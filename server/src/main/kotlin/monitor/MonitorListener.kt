package com.ertools.monitor

import dto.Message

interface MonitorListener {
    fun onReply(port: Int, message: Message)
    fun onPublish(topicName: String, message: Message): Boolean
    fun onRegisterTopic(producerPort: Int, topicName: String): Boolean
    fun onWithdrawTopic(producerPort: Int, topicName: String): Boolean
    fun onSubscription(port: Int, topicName: String): Boolean
    fun onUnsubscription(port: Int, topicName: String): Boolean
}