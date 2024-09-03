package com.ertools.monitor

import utils.TimeConverter
import com.ertools.utils.Constance
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dto.*
import utils.ObservableQueue

class MonitorThread(
    private val requestQueue: ObservableQueue<Request>,
    private val messageManager: MessageManager
): Thread() {
    private var isRunning: Boolean = false
    private var timeConverter: TimeConverter = TimeConverter()

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
            MessageType.Message, MessageType.File -> {
                if(message.mode == MessageMode.Producer) return publishMessage(port, message)
            }
            MessageType.Config -> return replyConfiguration(port)
            MessageType.Status -> return replyStatus(port)
            else -> {
                return
            }
        }
    }

    /***************************/
    /** Response to listeners **/
    /***************************/

    private fun registerTopic(port: Int, message: Message) {
        val success = messageManager.onRegisterTopic(port, message.topic, message.id)
        when(success) {
            false -> replyRejection(port, message, "topic is already exist")
            true -> replyAcknowledge(port, message, "OK")
        }
    }

    private fun withdrawTopic(port: Int, message: Message) {
        val success = messageManager.onWithdrawTopic(port, message.topic)
        when(success) {
            false -> replyRejection(port, message, "no topic registered")
            true -> replyAcknowledge(port, message, "Producer withdrawed the topic")
        }
    }

    private fun subscribeTopic(port: Int, message: Message) {
        val success = messageManager.onSubscription(port, message.topic)
        when(success) {
            false -> replyRejection(port, message, "no such topic registered")
            true -> replyAcknowledge(port, message, "OK")
        }
    }

    private fun unsubscribeTopic(port: Int, message: Message) {
        val success = messageManager.onUnsubscription(port, message.topic)
        when(success) {
            false -> replyRejection(port, message, "no topic registered")
            true -> replyAcknowledge(port, message, "Subscription of the topic withdrawed")
        }
    }

    private fun publishMessage(port: Int, message: Message) {
        val responseMessage = Message(
            type = message.type,
            id = Configuration.SERVER_ID,
            topic = message.topic,
            timestamp = timeConverter.getTimestamp(),
            mode = MessageMode.Producer,
            payload = message.payload as MessagePayload
        )

        var receiversAmount = 0
        try {
            receiversAmount += messageManager.onPublish(message.topic, responseMessage)
            if(receiversAmount == 0) replyRejection(port, message, "Topic has not any listeners")
            else replyAcknowledge(port, message, "OK")
        } catch (e: IllegalStateException) {
            replyRejection(port, message, "No topic registered")
        }
    }

    private fun replyConfiguration(port: Int) {
        val configuration = messageManager.onConfigRequest(port)

        val responseMessage = Message(
            type = MessageType.Config,
            id = Configuration.SERVER_ID,
            topic = "logs",
            timestamp = timeConverter.getTimestamp(),
            mode = MessageMode.Producer,
            payload = ConfigPayload(configuration)
        )

        messageManager.onReply(port, responseMessage)
    }

    private fun replyStatus(port: Int) {
        val status = messageManager.onStatusRequest(port)

        val responseMessage = Message(
            type = MessageType.Status,
            id = Configuration.SERVER_ID,
            topic = "logs",
            timestamp = timeConverter.getTimestamp(),
            mode = MessageMode.Producer,
            payload = StatusPayload(status)
        )

        messageManager.onReply(port, responseMessage)
    }

    private fun replyError(port: Int) {
        val payload = MessagePayload(
            timestampOfMessage = timeConverter.getTimestamp(),
            topicOfMessage = "logs",
            success = false,
            message = "The message is unpleasant."
        )
        val responseMessage = Message(
            type = MessageType.Reject,
            id = Configuration.SERVER_ID,
            topic = "logs",
            timestamp = timeConverter.getTimestamp(),
            mode = MessageMode.Producer,
            payload = payload
        )

        messageManager.onReply(port, responseMessage)
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
            timestamp = timeConverter.getTimestamp(),
            mode = MessageMode.Producer,
            payload = payload
        )

        messageManager.onReply(port, responseMessage)
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
            timestamp = timeConverter.getTimestamp(),
            mode = MessageMode.Producer,
            payload = payload
        )

        messageManager.onReply(port, responseMessage)
    }
}