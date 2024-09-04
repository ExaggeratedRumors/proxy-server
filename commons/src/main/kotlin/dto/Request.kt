package dto

class Request (
    val serializedMessage: ByteArray,
    val client: ClientInfo
)