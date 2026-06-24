package com.adiidev.aichatbot

data class ChatSession(
    val id: String = "",
    val title: String = "",
    val timestamp: Long = 0,
    val messages: List<Message> = emptyList()
)