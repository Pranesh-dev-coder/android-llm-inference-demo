package com.google.mediapipe.examples.llminference.ui.chat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.google.mediapipe.examples.llminference.ui.theme.TextChunker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max
import com.google.mediapipe.examples.llminference.data.db.MedicalDatabase
import com.google.mediapipe.examples.llminference.data.db.GraphDao
import com.google.mediapipe.examples.llminference.data.db.GraphRelationResult
import com.google.mediapipe.examples.llminference.model.InferenceModel
import com.google.mediapipe.examples.llminference.model.Model

class ChatViewModel(
    private var inferenceModel: InferenceModel,
    private val graphDao: GraphDao
) : ViewModel() {

    private val _isContextLoaded = MutableStateFlow(false)
    val isContextLoaded: StateFlow<Boolean> = _isContextLoaded.asStateFlow()

    private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(inferenceModel.uiState)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _tokensRemaining = MutableStateFlow(-1)
    val tokensRemaining: StateFlow<Int> = _tokensRemaining.asStateFlow()

    private val _textInputEnabled: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val isTextInputEnabled: StateFlow<Boolean> = _textInputEnabled.asStateFlow()

    private var textChunks: List<String> = emptyList()

    private var currentInferenceFuture: com.google.common.util.concurrent.ListenableFuture<String>? = null
    @Volatile private var isCancelled = false

    fun resetInferenceModel(newModel: InferenceModel) {
        inferenceModel = newModel
        _uiState.value = inferenceModel.uiState
    }

    fun sendMessage(userMessage: String) {
        if (userMessage.startsWith("Context from file: ")) {
            val fileAndContent = userMessage.removePrefix("Context from file: ")
            val firstNewline = fileAndContent.indexOf('\n')
            val fileName = if (firstNewline != -1) fileAndContent.substring(0, firstNewline) else "Document"
            val rawText = if (firstNewline != -1) fileAndContent.substring(firstNewline + 1) else fileAndContent
            
            textChunks = TextChunker.chunkText(rawText)
            _isContextLoaded.value = true
            _uiState.value.addMessage("📄 Loaded: $fileName", "system")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            isCancelled = false
            _uiState.value.addMessage(userMessage, USER_PREFIX)
            _uiState.value.createLoadingMessage()
            setInputEnabled(false)
            try {
                // 1. Flush the C++ KV Cache to prevent ekv1280 overflow crashes
                inferenceModel.resetSession()

                // 2. Perform G-RAG database query (Feature 5)
                val graphContext = lookupGraphRelationships(userMessage)

                // 3. Perform Smart Compressed Vector Chunks Query (Feature 1)
                val vectorContext = if (textChunks.isNotEmpty()) {
                    TextChunker.findBestContextChunks(userMessage, textChunks)
                } else ""

                // 4. Combine both clinical context streams cleanly
                val combinedContext = StringBuilder().apply {
                    if (graphContext.isNotEmpty()) {
                        append("VERIFIED MEDICAL KNOWLEDGE GRAPH FACTS (ZERO-HALLUCINATION TRUTH):\n")
                        append(graphContext)
                        append("\n\n")
                    }
                    if (vectorContext.isNotEmpty()) {
                        append("RELEVANT UPLOADED PROTOCOL CONTEXT:\n")
                        append(vectorContext)
                        append("\n\n")
                    }
                }.toString()

                // 5. Generate a syntactically pristine standalone ChatML sequence wrapper
                val finalPrompt = buildSlidingWindowPrompt(userMessage, combinedContext)

                val asyncInference = inferenceModel.generateResponseAsync(finalPrompt, { partialResult: String, done: Boolean ->
                    if (isCancelled) return@generateResponseAsync
                    _uiState.value.appendMessage(partialResult)
                    if (done) {
                        _uiState.value.finishMessage()
                        setInputEnabled(true)  // Re-enable text input
                    } else {
                        // Reduce current token count (estimate only). sizeInTokens() will be used
                        // when computation is done
                        _tokensRemaining.update { max(0, it - 1) }
                    }
                })
                currentInferenceFuture = asyncInference
                // Once the inference is done, recompute the remaining size in tokens
                asyncInference.addListener({
                    if (!isCancelled) {
                        viewModelScope.launch(Dispatchers.IO) {
                            recomputeSizeInTokens(userMessage)
                        }
                    }
                }, Dispatchers.Main.asExecutor())
            } catch (e: Exception) {
                _uiState.value.addMessage(e.localizedMessage ?: "Unknown Error", MODEL_PREFIX)
                setInputEnabled(true)
            }
        }
    }

    /**
     * Searches the local database for terms found in the user's question,
     * finds explicit relationships between them, and formats them for prompt grounding.
     */
    private fun lookupGraphRelationships(query: String): String {
        val cleanQuery = query.lowercase()
        val words = cleanQuery.split(Regex("\\W+")).filter { it.length > 3 }
        val detectedEntities = mutableListOf<String>()

        for (word in words) {
            val match = graphDao.findEntityByName("%$word%")
            if (match != null) {
                detectedEntities.add(match.name)
            }
        }

        val uniqueEntities = detectedEntities.distinct()
        if (uniqueEntities.size < 2) return "" // Need at least two entities to construct path relationships!

        val relationshipsFound = mutableListOf<GraphRelationResult>()

        // Cross-check all detected entities in pairs to find direct relationships
        for (i in 0 until uniqueEntities.size) {
            for (j in i + 1 until uniqueEntities.size) {
                val relations = graphDao.getDirectRelationships(uniqueEntities[i], uniqueEntities[j])
                relationshipsFound.addAll(relations)
            }
        }

        if (relationshipsFound.isEmpty()) return ""

        // Format relation paths into a deterministic facts block
        return relationshipsFound.joinToString("\n") { rel ->
            "- ${rel.sourceName} (${rel.sourceDomain}) -> [${rel.relationType}] -> ${rel.targetName} (${rel.targetDomain})${if (rel.notes != null) " [Details: ${rel.notes}]" else ""}"
        }
    }

    private fun setInputEnabled(isEnabled: Boolean) {
        _textInputEnabled.value = isEnabled
    }

    fun cancelGeneration() {
        isCancelled = true
        currentInferenceFuture?.cancel(true)
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                inferenceModel.resetSession()
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "Failed to reset session on cancel", e)
            }
        }
        
        _uiState.value.finishMessage()
        setInputEnabled(true)
    }

    fun clearContext() {
        // 1. Clear the list of chunks
        textChunks = emptyList()
        // 2. Hide the indicator
        _isContextLoaded.value = false
    }

    fun resetChat() {
        inferenceModel.resetSession()
        _uiState.value.clearMessages()
        _tokensRemaining.value = -1
        clearContext()
    }

    fun recomputeSizeInTokens(message: String) {
        try {
            val remainingTokens = inferenceModel.estimateTokensRemaining(message)
            _tokensRemaining.value = remainingTokens
        } catch (e: IllegalStateException) {
            android.util.Log.w("ChatViewModel", "Model busy, skipping token calculation.")
        } catch (e: Exception) {
            android.util.Log.e("ChatViewModel", "Error computing tokens", e)
        }
    }

    private fun buildSlidingWindowPrompt(newQuery: String, documentContext: String): String {
        // uiState.messages is reversed (newest first). 
        // We reverse it back to chronological order (oldest first).
        val chronologicalMessages = _uiState.value.messages.reversed()
        
        val history = chronologicalMessages.filter {
            !it.isLoading && it.rawMessage.isNotBlank() && !it.isThinking && !it.isSystem
        }

        // The very last message in the chronological list is newQuery itself, so we drop it to prevent duplication.
        val historyWithoutCurrentQuery = history.dropLast(1)

        val maxHistoryTurns = 6
        val recentHistory = historyWithoutCurrentQuery.takeLast(maxHistoryTurns)

        val formattedContext = if (documentContext.isNotEmpty()) {
            "Relevant Context:\n$documentContext\n\n"
        } else ""

        val historyPairs = recentHistory.map { msg ->
            val cleanMsg = msg.rawMessage.replace(Regex("<think>.*?</think>", RegexOption.DOT_MATCHES_ALL), "").trim()
            Pair(msg.isFromUser, cleanMsg)
        }

        return InferenceModel.model.generateSlidingWindowPrompt(historyPairs, newQuery, formattedContext)
    }

    companion object {
        fun getFactory(context: Context) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val inferenceModel = InferenceModel.getInstance(context)
                
                // Initialize SQLite Room Graph database safely
                val db = MedicalDatabase.getDatabase(context.applicationContext)
                val graphDao = db.graphDao()
                
                // Initialize Semantic RAG Embedder (silently falls back to Keyword search if model is missing)
                try {
                    TextChunker.initEmbedder(context, "universal_sentence_encoder.tflite")
                } catch (e: Exception) {
                    android.util.Log.e("ChatViewModel", "TextEmbedder model missing, using keyword fallback", e)
                }
                
                return ChatViewModel(inferenceModel, graphDao) as T
            }
        }
    }
}


