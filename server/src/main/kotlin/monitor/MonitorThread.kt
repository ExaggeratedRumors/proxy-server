package com.ertools.monitor

import com.ertools.utils.Configuration
import com.ertools.utils.Constance
import com.ertools.utils.ObservableQueue
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dto.*
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class MonitorThread(
    private val requestQueue: ObservableQueue<Request>
): Thread() {
    private var isRunning: Boolean = false
    private val listeners: MutableList<MonitorListener> = mutableListOf()

    /****************/
    /** Public API **/
    /****************/

    fun addListener(listener: MonitorListener) {
        listeners.add(listener)
    }

    /*********************/
    /** Private service **/
    /*********************/

    override fun run() {
        isRunning = true

        /* Start thread */
        while(isRunning) {
            if (requestQueue.isEmpty()) {
                sleep(Constance.MONITOR_THREAD_SLEEP)
                continue
            }

            /* Fetch and deserialize message */
            val request = requestQueue.poll()
            val message: Message
            try {
                message = deserialize(request)
            } catch (e: JsonProcessingException) {
                replyError(request.clientPort)
                throw (Exception("ERROR: Incorrect JSON processing.", e))
            } catch (e: JsonMappingException) {
                replyError(request.clientPort)
                throw (Exception("ERROR: JSON deserialization failed.", e))
            }

            /* Validate message */
            val validateResult = validate(message)
            if(!validateResult) return replyError(request.clientPort)

            /* Choose response */
            serviceMessage(message, request.clientPort)
        }
    }

    private fun deserialize(request: Request): Message {
        return jacksonObjectMapper().readValue(request.serializedMessage, Message::class.java)
    }

    private fun validate(message: Message): Boolean {
        if(message.type == MessageType.Reject && message.payload == null) return false
        if(message.type == MessageType.Acknowledge && message.payload == null) return false
        if(message.type == MessageType.Register && message.mode != MessageMode.Producer) return false
        return true
    }

    private fun getTimestamp(): String {
        val currentTime = Instant.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .withZone(ZoneOffset.UTC)
        val formattedTime = formatter.format(currentTime)
        return formattedTime
    }

    private fun serviceMessage(message: Message, port: Int) {
        when(message.type) {
            MessageType.Register -> {
                if(message.mode == MessageMode.Producer) return registerTopic(port, message)
                if(message.mode == MessageMode.Subscriber) return subscribeTopic(port, message)
            }
            MessageType.Withdraw -> {
                if(message.mode == MessageMode.Producer) return withdrawTopic(port, message)
                if(message.mode == MessageMode.Subscriber) return unsubscribeTopic(port, message)
            }
            else -> {
                return
            }
        }
    }

    /***************************/
    /** Response to listeners **/
    /***************************/

    private fun registerTopic(port: Int, message: Message) {
        val success = listeners.map { it.onRegisterTopic(port, message.topic) }.contains(false)
        when(success) {
            false -> replyRejection(port, message, "topic is already exist")
            true -> replyAcknowledge(port, message, "OK")
        }
    }

    private fun withdrawTopic(port: Int, message: Message) {
        val success = listeners.map { it.onWithdrawTopic(port, message.topic) }.contains(false)
        when(success) {
            false -> replyRejection(port, message, "no topic registered")
            true -> replyAcknowledge(port, message, "Producer withdrawed the topic")
        }
    }

    private fun subscribeTopic(port: Int, message: Message) {
        val success = listeners.map { it.onSubscription(port, message.topic) }.contains(false)
        when(success) {
            false -> replyRejection(port, message, "no such topic registered")
            true -> replyAcknowledge(port, message, "OK")
        }
    }

    private fun unsubscribeTopic(port: Int, message: Message) {
        val success = listeners.map { it.onUnsubscription(port, message.topic) }.contains(false)
        when(success) {
            false -> replyRejection(port, message, "no topic registered")
            true -> replyAcknowledge(port, message, "Subscription of the topic withdrawed")
        }
    }

    private fun replyError(port: Int) {
        val payload = MessagePayload(
            timestampOfMessage = getTimestamp(),
            topicOfMessage = "logs",
            success = false,
            message = "The message is unpleasant."
        )
        val responseMessage = Message(
            type = MessageType.Reject,
            id = Configuration.SERVER_ID,
            topic = "logs",
            timestamp = getTimestamp(),
            mode = MessageMode.Producer,
            payload = payload
        )

        listeners.forEach { it.onReply(port, responseMessage) }
    }

    private fun replyRejection(port: Int, message: Message, messageContent: String) {
        val payload = MessagePayload(
            timestampOfMessage = message.timestamp,
            topicOfMessage = message.topic,
            success = false,
            message = messageContent
        )
        val responseMessage = Message(
            type = MessageType.Reject,
            id = Configuration.SERVER_ID,
            topic = "logs",
            timestamp = getTimestamp(),
            mode = MessageMode.Producer,
            payload = payload
        )

        listeners.forEach { it.onReply(port, responseMessage) }
    }

    private fun replyAcknowledge(port: Int, message: Message, messageContent: String) {
        val payload = MessagePayload(
            timestampOfMessage = message.timestamp,
            topicOfMessage = message.topic,
            success = true,
            message = messageContent
        )
        val responseMessage = Message(
            type = MessageType.Acknowledge,
            id = Configuration.SERVER_ID,
            topic = "logs",
            timestamp = getTimestamp(),
            mode = MessageMode.Producer,
            payload = payload
        )

        listeners.forEach { it.onReply(port, responseMessage) }
    }
}