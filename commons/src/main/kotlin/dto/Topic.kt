package dto

data class Topic(
    val topicName: String,
    val producerId: String,
    val producer: ClientInfo?,
    val subscribers: MutableList<ClientInfo> = mutableListOf()
)