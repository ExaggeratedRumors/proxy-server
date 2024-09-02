package dto

data class Topic(
    val topicName: String,
    val producerPort: Int,
    val producerId: String,
    val subscribers: MutableList<Int> = mutableListOf()
)