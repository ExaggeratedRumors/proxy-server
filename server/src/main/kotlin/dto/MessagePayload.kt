package com.ertools.dto

data class MessagePayload (
    val timestampOfMessage: String,
    val topicOfMessage: String,
    val success: Boolean,
    val message: String
)