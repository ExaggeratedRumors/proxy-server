package com.ertools.dto

data class Response (
    val message: String,
    val receivers: List<Int> /** Ports **/
)