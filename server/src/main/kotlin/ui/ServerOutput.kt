package com.ertools.ui

import dto.Topic

interface ServerOutput {
    fun updateLog(message: String)
    fun updateStatus(status: List<Topic>)
}