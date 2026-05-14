package com.google.mediapipe.examples.llminference

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

class ChatViewModel(
    private var inferenceModel: InferenceModel
) : ViewModel() {

    private val _isContextLoaded = MutableStateFlow(false)
    val isContextLoaded: StateFlow<Boolean> = _isContextLoaded.asStateFlow()

    private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(inferenceModel.uiState)
    val uiState: StateFlow<UiState> =_uiState.asStateFlow()

    private val _tokensRemaining = MutableStateFlow(-1)
    val tokensRemaining: StateFlow<Int> = _tokensRemaining.asStateFlow()

    private val _textInputEnabled: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val isTextInputEnabled: StateFlow<Boolean> = _textInputEnabled.asStateFlow()

    private var textChunks: List<String> = emptyList()


    fun resetInferenceModel(newModel: InferenceModel) {
        inferenceModel = newModel
        _uiState.value = inferenceModel.uiState
    }

    fun sendMessage(userMessage: String) {
        if (userMessage.startsWith("Context from file: ")) {
            val rawText = userMessage.removePrefix("Context from file: ")
            textChunks = TextChunker.chunkText(rawText)
            _isContextLoaded.value = true
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value.addMessage(userMessage, USER_PREFIX)
            _uiState.value.createLoadingMessage()
            setInputEnabled(false)
            try {
                // 1. Flush the C++ KV Cache to prevent ekv1280 overflow crashes
                inferenceModel.resetSession()

                // 2. Format any loaded document context cleanly
                val contextString = if (textChunks.isNotEmpty()){
                    val bestChunk = TextChunker.findBestChunk(userMessage, textChunks)
                    "Relevant Document Context:\n$bestChunk\n\n"
                } else ""

                // 3. Generate a syntactically pristine standalone ChatML sequence wrapper
                val finalPrompt = buildSlidingWindowPrompt(userMessage, contextString)

                val asyncInference =  inferenceModel.generateResponseAsync(finalPrompt, { partialResult: String, done: Boolean ->
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
                // Once the inference is done, recompute the remaining size in tokens
                asyncInference.addListener({
                    viewModelScope.launch(Dispatchers.IO) {
                        recomputeSizeInTokens(userMessage)
                    }
                }, Dispatchers.Main.asExecutor())
            } catch (e: Exception) {
                _uiState.value.addMessage(e.localizedMessage ?: "Unknown Error", MODEL_PREFIX)
                setInputEnabled(true)
            }
        }
    }

    private fun setInputEnabled(isEnabled: Boolean) {
        _textInputEnabled.value = isEnabled
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
    }

    fun recomputeSizeInTokens(message: String) {
        val remainingTokens = inferenceModel.estimateTokensRemaining(message)
        _tokensRemaining.value = remainingTokens
    }

    private fun buildSlidingWindowPrompt(newQuery: String, documentContext: String): String {
        // uiState.messages is reversed (newest first). 
        // We reverse it back to chronological order (oldest first).
        val chronologicalMessages = _uiState.value.messages.reversed()
        
        val history = chronologicalMessages.filter {
            !it.isLoading && it.rawMessage.isNotBlank() && !it.isThinking
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
                
                // Initialize Semantic RAG Embedder (silently falls back to Keyword search if model is missing)
                try {
                    TextChunker.initEmbedder(context, "universal_sentence_encoder.tflite")
                } catch (e: Exception) {
                    android.util.Log.e("ChatViewModel", "TextEmbedder model missing, using keyword fallback", e)
                }
                
                return ChatViewModel(inferenceModel) as T
            }
        }
    }
}

