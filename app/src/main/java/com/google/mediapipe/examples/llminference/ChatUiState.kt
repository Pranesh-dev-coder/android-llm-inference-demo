package com.google.mediapipe.examples.llminference

import androidx.compose.runtime.toMutableStateList

const val USER_PREFIX = "user"
const val MODEL_PREFIX = "model"
const val THINKING_MARKER_START = "<think>"
const val THINKING_MARKER_END = "</think>"

// Turn markers to stop hallucination
val STOP_MARKERS = listOf("<|im_end|>", "<|im_start|>", "<start_of_turn>", "<end_of_turn>", "###", "</s>", "<|user|>", "<|assistant|>")

/** Management of the message queue. */
class UiState(
    private val supportsThinking: Boolean = false,
    messages: List<ChatMessage> = emptyList()
)  {
    private val _messages: MutableList<ChatMessage> = messages.toMutableStateList()
    val messages: List<ChatMessage> = _messages.asReversed()
    private var _currentMessageId = ""
    
    // Accumulator for the current partial response to handle split markers
    private var currentResponseBuffer = ""

    /** Creates a new loading message. */
    fun createLoadingMessage() {
        currentResponseBuffer = ""
        val chatMessage = ChatMessage(
            author = MODEL_PREFIX, 
            isLoading = true, 
            isThinking = InferenceModel.model.thinking
        )
        _messages.add(chatMessage)
        _currentMessageId = chatMessage.id
    }

    /**
     * Appends the specified delta text to the current message.
     */
    fun appendMessage(delta: String) {
        var index = _messages.indexOfFirst { it.id == _currentMessageId }
        if (index == -1) return

        currentResponseBuffer += delta
        
        // Hallucination check: if the model starts generating user turns, we truncate it.
        for (stopMarker in STOP_MARKERS) {
            if (currentResponseBuffer.contains(stopMarker)) {
                currentResponseBuffer = currentResponseBuffer.substringBefore(stopMarker)
            }
        }

        // Auto-detect thinking state
        if (!_messages[index].isThinking && currentResponseBuffer.contains(THINKING_MARKER_START)) {
            _messages[index] = _messages[index].copy(isThinking = true)
        }

        if (_messages[index].isThinking) {
            if (currentResponseBuffer.contains(THINKING_MARKER_END)) {
                val splitIndex = currentResponseBuffer.indexOf(THINKING_MARKER_END)
                val thinkingText = currentResponseBuffer.substring(0, splitIndex)
                val answerText = currentResponseBuffer.substring(splitIndex + THINKING_MARKER_END.length)

                // Update thinking bubble and finish it
                updateMessageContent(index, cleanThinkingText(thinkingText), isLoading = false)

                // Create new answer bubble
                val answerMessage = ChatMessage(
                    rawMessage = answerText.trimStart(),
                    author = MODEL_PREFIX,
                    isLoading = true,
                    isThinking = false
                )
                _messages.add(answerMessage)
                _currentMessageId = answerMessage.id
                
                // Switch buffer and index for the answer part
                currentResponseBuffer = answerText
            } else {
                // Still thinking - preserve internal newlines
                updateMessageContent(index, cleanThinkingText(currentResponseBuffer), isLoading = true)
            }
        } else {
            // Updating the answer bubble
            updateMessageContent(index, currentResponseBuffer.trimStart(), isLoading = true)
        }
    }

    private fun updateMessageContent(index: Int, text: String, isLoading: Boolean) {
        _messages[index] = _messages[index].copy(
            rawMessage = text,
            isLoading = isLoading
        )
    }

    private fun cleanThinkingText(text: String): String {
        return text.replace(THINKING_MARKER_START, "")
            .replace("<think>", "")
            .trimStart()
    }

    /** Called when the model is done generating. */
    fun finishMessage() {
        val index = _messages.indexOfFirst { it.id == _currentMessageId }
        if (index != -1) {
            val msg = _messages[index]
            if (msg.isThinking) {
                // Fallback: If the model finishes generating but never outputted </think>,
                // treat the entire block as the final answer so it doesn't get stuck in the UI.
                _messages[index] = msg.copy(isLoading = false, isThinking = false)
            } else {
                _messages[index] = msg.copy(isLoading = false)
            }
        }
        currentResponseBuffer = ""
    }

    /** Creates a new message with the specified text and author. */
    fun addMessage(text: String, author: String) {
        val chatMessage = ChatMessage(
            rawMessage = text,
            author = author
        )
        _messages.add(chatMessage)
        _currentMessageId = chatMessage.id
    }

    /** Clear all messages. */
    fun clearMessages() {
        _messages.clear()
        currentResponseBuffer = ""
    }
}
