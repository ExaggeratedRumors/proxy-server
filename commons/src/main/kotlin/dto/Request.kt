package dto

data class Request (
    val serializedMessage: String,
    val clientPort: Int
)