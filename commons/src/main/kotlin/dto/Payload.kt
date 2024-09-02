package dto

import javax.xml.crypto.Data

interface Payload

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