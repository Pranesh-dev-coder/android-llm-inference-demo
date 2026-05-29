package com.google.mediapipe.examples.llminference.ui.chat

import androidx.compose.runtime.toMutableStateList
import com.google.mediapipe.examples.llminference.model.InferenceModel


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
    private var thinkingContent = ""
    private var mainContent = ""
    private var tagBuffer = ""
    private var insideThinkBlock = false

    /** Creates a new loading message. */
    fun createLoadingMessage() {
        currentResponseBuffer = ""
        thinkingContent = ""
        mainContent = ""
        tagBuffer = ""
        insideThinkBlock = InferenceModel.model.thinking
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
        val index = _messages.indexOfFirst { it.id == _currentMessageId }
        if (index == -1) return

        currentResponseBuffer += delta
        val msg = _messages[index]

        // Process char by char for a robust state machine to handle broken streaming chunks
        for (char in delta) {
            if (char == '<' && tagBuffer.isEmpty()) {
                tagBuffer += char
            } else if (tagBuffer.isNotEmpty()) {
                tagBuffer += char
                if (tagBuffer == "<think>") {
                    insideThinkBlock = true
                    tagBuffer = ""
                } else if (tagBuffer == "</think>") {
                    insideThinkBlock = false
                    tagBuffer = ""
                } else if (!"<think>".startsWith(tagBuffer) && !"</think>".startsWith(tagBuffer)) {
                    // Not a tag, flush buffer
                    if (insideThinkBlock) {
                        thinkingContent += tagBuffer
                    } else {
                        mainContent += tagBuffer
                    }
                    tagBuffer = ""
                }
            } else {
                if (insideThinkBlock) {
                    thinkingContent += char
                } else {
                    mainContent += char
                }
            }
        }
        
        // Hallucination check
        for (stopMarker in STOP_MARKERS) {
            if (mainContent.contains(stopMarker)) {
                mainContent = mainContent.substringBefore(stopMarker)
            }
        }

        // Update the single answer bubble with separated logic
        _messages[index] = msg.copy(
            rawMessage = currentResponseBuffer,
            thinkingText = thinkingContent.trimStart(),
            answerText = mainContent.trimStart(),
            isLoading = true
        )
    }

    /** Called when the model is done generating. */
    fun finishMessage() {
        val index = _messages.indexOfFirst { it.id == _currentMessageId }
        if (index != -1) {
            // Flush any remaining tag buffer just in case
            if (tagBuffer.isNotEmpty()) {
                if (insideThinkBlock) {
                    thinkingContent += tagBuffer
                } else {
                    mainContent += tagBuffer
                }
                tagBuffer = ""
            }
            val msg = _messages[index]
            _messages[index] = msg.copy(
                thinkingText = thinkingContent.trimStart(),
                answerText = mainContent.trimStart(),
                isLoading = false
            )
        }
        currentResponseBuffer = ""
        thinkingContent = ""
        mainContent = ""
        tagBuffer = ""
        insideThinkBlock = false
        removeEmptyMessages()
    }

    fun removeEmptyMessages() {
        _messages.removeAll { it.isEmpty }
    }

    fun rewindToMessage(messageId: String): String? {
        val index = _messages.indexOfFirst { it.id == messageId }
        if (index != -1) {
            val raw = _messages[index].rawMessage
            while (_messages.size > index) {
                _messages.removeAt(_messages.size - 1)
            }
            
            // Cleanly reset parser states when rewinding history
            currentResponseBuffer = ""
            thinkingContent = ""
            mainContent = ""
            tagBuffer = ""
            insideThinkBlock = false
            
            return raw
        }
        return null
    }

    fun removeCurrentMessage() {
        val index = _messages.indexOfFirst { it.id == _currentMessageId }
        if (index != -1) {
            _messages.removeAt(index)
        }
    }

    /** Creates a new message with the specified text and author. */
    fun addMessage(text: String, author: String) {
        val chatMessage = ChatMessage(
            rawMessage = text,
            answerText = text,
            author = author
        )
        _messages.add(chatMessage)
        _currentMessageId = chatMessage.id
    }

    /** Clear all messages. */
    fun clearMessages() {
        _messages.clear()
        currentResponseBuffer = ""
        thinkingContent = ""
        mainContent = ""
        tagBuffer = ""
        insideThinkBlock = false
    }
}
