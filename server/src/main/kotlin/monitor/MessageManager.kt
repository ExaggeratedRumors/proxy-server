package com.ertools.monitor

import dto.Configuration
import dto.Message

interface MessageManager {
    fun onReply(port: Int, message: Message)
    fun onPublish(topicName: String, message: Message): Int
    fun onRegisterTopic(producerPort: Int, topicName: String, producerId: String): Boolean
    fun onWithdrawTopic(producerPort: Int, topicName: String): Boolean
    fun onSubscription(port: Int, topicName: String): Boolean
    fun onUnsubscription(port: Int, topicName: String): Boolean
    fun onStatusRequest(port: Int): Map<String, String>
    fun onConfigRequest(port: Int): Configuration
}