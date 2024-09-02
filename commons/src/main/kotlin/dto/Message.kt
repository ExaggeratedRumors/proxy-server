package dto

data class Message (
    val type: MessageType,
    val id: String,
    val topic: String,
    val mode: MessageMode?,
    val timestamp: String, /* ISO 8601 */
    val payload: Payload?
)