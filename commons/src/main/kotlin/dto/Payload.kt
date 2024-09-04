package dto

import java.io.Serializable

interface Payload : Serializable

data class MessagePayload(
    val timestampOfMessage: String,
    val topicOfMessage: String,
    val success: Boolean,
    val message: String
) : Payload

data class ConfigPayload(
    val config: Configuration
) : Payload

data class StatusPayload(
    val data: Map<String, String>
) : Payload

data class FilePayload(
    val filename: String,
    val data: String
) : Payload