package com.google.mediapipe.examples.llminference.ui.chat

import java.util.UUID

/**
 * Used to represent a ChatMessage
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val rawMessage: String = "",
    val thinkingText: String = "",
    val answerText: String = "",
    val author: String,
    val isLoading: Boolean = false,
    val isThinking: Boolean = false,
) {
    val isEmpty: Boolean
        get() = answerText.trim().isEmpty() && thinkingText.trim().isEmpty()
    val isFromUser: Boolean
        get() = author == USER_PREFIX
    val isSystem: Boolean
        get() = author == "system"
    val message: String
        get() = if (answerText.isNotEmpty()) answerText.trim() else rawMessage.trim()
}
