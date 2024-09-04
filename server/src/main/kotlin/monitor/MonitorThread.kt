package com.ertools.monitor

import com.ertools.utils.Constance
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonMappingException
import dto.*
import utils.ObservableQueue
import utils.TimeConverter
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream

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
                replyError(request.client)
                e.printStackTrace()
                throw (Exception("ERROR: Incorrect JSON processing.", e))
            } catch (e: JsonMappingException) {
                replyError(request.client)
                e.printStackTrace()
                throw (Exception("ERROR: JSON deserialization failed.", e))
            }

            /* Validate message */
            val validateResult = validate(message)
            if(!validateResult) return replyError(request.client)

            /* Choose response */
            serviceMessage(message, request.client)
        }
    }

    private fun deserialize(request: Request): Message {
        val byteArrayInputStream = ByteArrayInputStream(request.serializedMessage)
        val objectInputStream = ObjectInputStream(byteArrayInputStream)
        val message = objectInputStream.readObject() as Message
        objectInputStream.close()
        byteArrayInputStream.close()
        return message
    }

    private fun validate(message: Message): Boolean {
        if(message.type == MessageType.Reject && message.payload == null) return false
        if(message.type == MessageType.Acknowledge && message.payload == null) return false
        return true
    }

    private fun serviceMessage(message: Message, client: ClientInfo) {
        val clientInfo = ClientInfo(
            message.id,
            client.port,
            client.ip,
            client.socket
        )
        when(message.type) {
            MessageType.Register -> {
                if(message.mode == MessageMode.Producer) return registerTopic(clientInfo, message)
                if(message.mode == MessageMode.Subscriber) return subscribeTopic(clientInfo, message)
            }
            MessageType.Withdraw -> {
                if(message.mode == MessageMode.Producer) return withdrawTopic(clientInfo, message)
                if(message.mode == MessageMode.Subscriber) return unsubscribeTopic(clientInfo, message)
            }
            MessageType.Message, MessageType.File -> {
                if(message.mode == MessageMode.Producer) return publishMessage(clientInfo, message)
            }
            MessageType.Config -> return replyConfiguration(clientInfo)
            MessageType.Status -> return replyStatus(clientInfo)
            else -> {
                return
            }
        }
    }

    /***************************/
    /** Response to listeners **/
    /***************************/

    private fun registerTopic(client: ClientInfo, message: Message) {
        val success = messageManager.onRegisterTopic(client, message.topic, message.id)
        when(success) {
            false -> replyRejection(client, message, "topic is already exist")
            true -> replyAcknowledge(client, message, "OK")
        }
    }

    private fun withdrawTopic(client: ClientInfo, message: Message) {
        val success = messageManager.onWithdrawTopic(client, message.topic)
        when(success) {
            false -> replyRejection(client, message, "no topic registered")
            true -> replyAcknowledge(client, message, "Producer withdrawed the topic")
        }
    }

    private fun subscribeTopic(client: ClientInfo, message: Message) {
        val success = messageManager.onSubscription(client, message.topic)
        when(success) {
            false -> replyRejection(client, message, "no such topic registered")
            true -> replyAcknowledge(client, message, "OK")
        }
    }

    private fun unsubscribeTopic(client: ClientInfo, message: Message) {
        val success = messageManager.onUnsubscription(client, message.topic)
        when(success) {
            false -> replyRejection(client, message, "no topic registered")
            true -> replyAcknowledge(client, message, "Subscription of the topic withdrawed")
        }
    }

    private fun publishMessage(client: ClientInfo, message: Message) {
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
            if(receiversAmount == 0) replyRejection(client, message, "Topic has not any listeners")
            else replyAcknowledge(client, message, "OK")
        } catch (e: IllegalStateException) {
            replyRejection(client, message, "No topic registered")
        }
    }

    private fun replyConfiguration(client: ClientInfo) {
        val configuration = messageManager.onConfigRequest(client)

        val responseMessage = Message(
            type = MessageType.Config,
            id = Configuration.SERVER_ID,
            topic = "logs",
            timestamp = timeConverter.getTimestamp(),
            mode = MessageMode.Producer,
            payload = ConfigPayload(configuration)
        )

        messageManager.onReply(client, responseMessage)
    }

    private fun replyStatus(client: ClientInfo) {
        val status = messageManager.onStatusRequest(client)

        val responseMessage = Message(
            type = MessageType.Status,
            id = Configuration.SERVER_ID,
            topic = "logs",
            timestamp = timeConverter.getTimestamp(),
            mode = MessageMode.Producer,
            payload = StatusPayload(status)
        )

        messageManager.onReply(client, responseMessage)
    }

    private fun replyError(client: ClientInfo) {
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

        messageManager.onReply(client, responseMessage)
    }

    private fun replyRejection(client: ClientInfo, message: Message, messageContent: String) {
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

        messageManager.onReply(client, responseMessage)
    }

    private fun replyAcknowledge(client: ClientInfo, message: Message, messageContent: String) {
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

        messageManager.onReply(client, responseMessage)
    }
}