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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.google.mediapipe.examples.llminference.data.db.MedicalDatabase
import com.google.mediapipe.examples.llminference.data.db.GraphDao
import com.google.mediapipe.examples.llminference.data.db.GraphRelationResult
import com.google.mediapipe.examples.llminference.data.db.MedicalEntity
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
    @Volatile private var generationId = 0
    // True while the native C++ engine is actively generating tokens.
    // Unlike Future.isDone, this is NOT set to true by cancel() — only by the real done=true callback.
    @Volatile private var engineBusy = false
    private val inferenceMutex = Mutex()

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
            val myGenerationId = ++generationId
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                _uiState.value.addMessage(userMessage, USER_PREFIX)
                _uiState.value.createLoadingMessage()
                setInputEnabled(false)
            }
            try {
                // 1. Wait for the C++ engine to truly become idle before resetting the session.
                //    We use the engineBusy flag (set by the real done=true callback) because
                //    ListenableFuture.isDone() returns true immediately after cancel(true),
                //    even though the native engine is still generating tokens underneath.
                inferenceMutex.withLock {
                    var waited = 0
                    while (engineBusy && waited < 60) { // max ~9s wait
                        kotlinx.coroutines.delay(150)
                        waited++
                    }
                    if (engineBusy) {
                        android.util.Log.w("ChatViewModel", "Engine still busy after timeout — forcing session reset")
                    }
                    inferenceModel.resetSession()
                }

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

                engineBusy = true
                val asyncInference = inferenceModel.generateResponseAsync(finalPrompt, { partialResult: String, done: Boolean ->
                    // Clear engineBusy BEFORE the generationId guard so cancelled generations
                    // still unblock the next sendMessage() that may be spin-waiting.
                    if (done) engineBusy = false
                    if (myGenerationId != generationId) return@generateResponseAsync
                    viewModelScope.launch(Dispatchers.Main) {
                        _uiState.value.appendMessage(partialResult)
                        if (done) {
                            _uiState.value.finishMessage()
                            setInputEnabled(true)
                        }
                    }
                    if (!done) {
                        _tokensRemaining.update { max(0, it - 1) }
                    }
                })
                currentInferenceFuture = asyncInference
                // Once the inference is done, recompute the remaining size in tokens
                asyncInference.addListener({
                    if (myGenerationId == generationId) {
                        viewModelScope.launch(Dispatchers.IO) {
                            recomputeSizeInTokens(userMessage)
                        }
                    }
                }, Dispatchers.Main.asExecutor())
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    if (e !is java.util.concurrent.CancellationException) {
                        android.util.Log.e("ChatViewModel", "Inference failed", e)
                        _uiState.value.removeCurrentMessage()
                        if (e is IllegalStateException) {
                            _uiState.value.addMessage("⚠️ The model is busy. Please wait a moment and try again.", "system")
                        } else {
                            _uiState.value.addMessage("⚠️ Error: ${e.localizedMessage ?: "Please try again."}", "system")
                        }
                    }
                    setInputEnabled(true)
                }
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
        val detectedEntities = mutableListOf<MedicalEntity>()

        for (word in words) {
            val match = graphDao.findEntityByName("%$word%")
            if (match != null) {
                detectedEntities.add(match)
            }
        }

        val uniqueEntities = detectedEntities.distinctBy { it.id }
        if (uniqueEntities.isEmpty()) return ""

        val sb = java.lang.StringBuilder()

        // 1. Single Entity Facts
        uniqueEntities.forEach { entity ->
            sb.append("[Fact: ${entity.name} (${entity.domain}) - ${entity.description ?: ""}]\n")
        }

        // 2. Relationship Checks
        if (uniqueEntities.size >= 2) {
            val relationshipsFound = mutableListOf<GraphRelationResult>()
            for (i in 0 until uniqueEntities.size) {
                for (j in i + 1 until uniqueEntities.size) {
                    val relations = graphDao.getDirectRelationships(uniqueEntities[i].name, uniqueEntities[j].name)
                    relationshipsFound.addAll(relations)
                }
            }
            relationshipsFound.distinct().forEach { rel ->
                sb.append("[Relation: ${rel.sourceName} ${rel.relationType} ${rel.targetName} - ${rel.notes ?: ""}]\n")
            }
        }

        return sb.toString().trim()
    }

    private fun setInputEnabled(isEnabled: Boolean) {
        _textInputEnabled.value = isEnabled
    }

    fun cancelGeneration() {
        // Increment generationId to instantly cut off UI updates from the old generation.
        generationId++
        // Use the native MediaPipe cancellation API. This safely stops the C++ engine
        // and triggers the done=true callback cleanly, setting engineBusy = false.
        inferenceModel.cancelGeneration()

        viewModelScope.launch(Dispatchers.Main) {
            _uiState.value.finishMessage()
            setInputEnabled(true)
        }
    }

    fun clearContext() {
        // 1. Clear the list of chunks
        textChunks = emptyList()
        // 2. Hide the indicator
        _isContextLoaded.value = false
    }

    fun resetChat() {
        viewModelScope.launch(Dispatchers.IO) {
            inferenceMutex.withLock {
                inferenceModel.resetSession()
            }
        }
        _uiState.value.clearMessages()
        _tokensRemaining.value = -1
        clearContext()
    }

    fun rewindToMessage(messageId: String): String? {
        return _uiState.value.rewindToMessage(messageId)
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
            Pair(msg.isFromUser, msg.message)
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


