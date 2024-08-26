package com.ertools.dto

data class Response (
    val message: Message,
    val receivers: List<Int> /** Ports **/
)