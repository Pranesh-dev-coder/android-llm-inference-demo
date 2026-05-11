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
                val systemRules = "You are an Apollo Hospitals Medical Assistant. You give short, safe medical advice. If the user asks non-medical questions, politely refuse.\n\n"
                
                var finalPrompt = systemRules
                if (textChunks.isNotEmpty()) {
                    val bestChunk = TextChunker.findBestChunk(userMessage, textChunks)
                    finalPrompt += "Context: $bestChunk\n\n"
                }

                finalPrompt += "<start_of_turn>user\n$userMessage<end_of_turn>\n<start_of_turn>model\n"

                val asyncInference =  inferenceModel.generateResponseAsync(finalPrompt, { partialResult, done ->
                    _uiState.value.appendMessage(partialResult)
                    if (done) {
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

    fun recomputeSizeInTokens(message: String) {
        val remainingTokens = inferenceModel.estimateTokensRemaining(message)
        _tokensRemaining.value = remainingTokens
    }

    companion object {
        fun getFactory(context: Context) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val inferenceModel = InferenceModel.getInstance(context)
                return ChatViewModel(inferenceModel) as T
            }
        }
    }
}

