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
                replyRejection(request)
                throw (Exception("ERROR: Incorrect JSON processing.", e))
            } catch (e: JsonMappingException) {
                replyRejection(request)
                throw (Exception("ERROR: JSON deserialization failed.", e))
            }

            /* Validate message */
            val validateResult = validate(message)
            if(!validateResult) return replyRejection(request)

            /* Choose response */

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

    /***************************/
    /** Response to listeners **/
    /***************************/

    private fun registerTopic(topic: String) {
        listeners.forEach { it.onRegisterTopic(topic) }
    }

    private fun replyRejection(request: Request) {
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

        listeners.forEach { it.onReply(request.clientPort, responseMessage) }
    }
}