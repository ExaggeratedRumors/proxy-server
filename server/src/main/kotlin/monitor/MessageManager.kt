package com.ertools.monitor

import dto.ClientInfo
import dto.Configuration
import dto.Message

interface MessageManager {
    fun onReply(client: ClientInfo, message: Message)
    fun onPublish(topicName: String, message: Message): Int
    fun onRegisterTopic(producer: ClientInfo, topicName: String, producerId: String): Boolean
    fun onWithdrawTopic(producer: ClientInfo, topicName: String): Boolean
    fun onSubscription(client: ClientInfo, topicName: String): Boolean
    fun onUnsubscription(client: ClientInfo, topicName: String): Boolean
    fun onStatusRequest(client: ClientInfo): Map<String, String>
    fun onConfigRequest(client: ClientInfo): Configuration
}