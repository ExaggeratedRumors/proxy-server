package dto

data class Topic(
    val topicName: String,
    val producer: Int,
    val subscribers: MutableList<Int> = mutableListOf()
)