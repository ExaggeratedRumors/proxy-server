package com.ertools.monitor

import dto.Message

interface MonitorListener {
    fun onRegisterTopic(topic: String)
    fun onReply(port: Int, message: Message)
    fun onProduce(topic: String, message: Message)
    fun onSubscription(topic: String, port: Int)
    fun onUnsubscription(topic: String, port: Int)
}