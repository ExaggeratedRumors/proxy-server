package com.ertools.monitor

import dto.Response

interface MonitorListener {
    fun onRegisterResponse(response: Response)
    fun onRegisterTopic(topic: String)
}