package communication

import utils.TimeConverter
import com.fasterxml.jackson.databind.ObjectMapper
import dto.*
import utils.ClientUtils
import utils.ObservableQueue
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.UnknownHostException
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class ClientConnection (
    private val port: Int = ClientUtils.DEFAULT_PORT,
    private val ip: String = ClientUtils.DEFAULT_IP,
) : Thread(), ClientAPI {
    /** Connection data **/
    private var clientID: String = "client$port"
    private var isInitialized = false
    private var socket: Socket? = null
    private var writer: ObjectOutputStream? = null
    private var reader: ObjectInputStream? = null
    private var timeConverter: TimeConverter = TimeConverter()
    private val mapper: ObjectMapper = ObjectMapper()
    private var configuration: Configuration? = null

    /** Communication sources **/
    private val topics: ArrayList<String> = ArrayList()
    private val subscriptions: ArrayList<String> = ArrayList()
    private val sentMessages: ArrayList<Message> = ArrayList()
    private val receivedMessages: ObservableQueue<Message> = ObservableQueue(::serviceMessage)

    /** Callbacks **/
    private val topicCallbacks: MutableMap<String, (Message) -> (Unit)> = HashMap()
    private var statusCallback: (String) -> (Unit) = { _ -> }
    private var replyCallback: (Message) -> (Unit) = { _ -> }

    /** Private **/
    private fun send(message: Message) {
        if(!isInitialized) throw IllegalStateException("ERROR: Connection has not been initialized.")
        try {
            writer!!.writeObject(message)
            sentMessages.add(message)
        } catch (e: Exception) {
            e.printStackTrace()
            throw(e)
        }
    }

    private fun serviceMessage(message: Message) {
        receivedMessages.add(message)

        when(message.type) {
            MessageType.Acknowledge, MessageType.Reject -> {
                replyCallback.invoke(message)
            }
            MessageType.Config -> {
                configuration = (message.payload as ConfigPayload).config
            }
            MessageType.Status -> {
                val statusPayload = message.payload as StatusPayload
                val statusData = mapper.writeValueAsString(statusPayload.data)
                statusCallback.invoke(statusData)
            }
            MessageType.Message -> {
                topicCallbacks[message.topic]?.invoke(message)
            }
            MessageType.File -> {
                val filePayload = message.payload as FilePayload
                val fileData = Base64.getDecoder().decode(filePayload.data)
                val file = File(filePayload.filename)
                file.writeBytes(fileData)
                topicCallbacks[message.topic]?.invoke(message)
            }
            else -> return
        }
    }

    /****************/
    /** Client API **/
    /****************/

    override fun start(serverIP: String, serverPort: Int, clientID: String) {
        try {
            if(this.isInitialized) stopConnection()
            this.socket = Socket(ip, port)
            this.socket!!.bind(InetSocketAddress(serverIP, serverPort))
            this.writer = ObjectOutputStream(socket!!.getOutputStream())
            this.reader = ObjectInputStream(socket!!.getInputStream())
            this.topics.clear()
            this.subscriptions.clear()
            this.isInitialized = true
            this.clientID = clientID
            if(ClientUtils.DEBUG_MODE) println("ENGINE: Client started: (#$clientID $serverIP:$serverPort).")
            start()
        } catch (e: IllegalArgumentException) {
            throw(IllegalStateException("ERROR: Port number must be between 0 and 65535", e))
        } catch (e: ConnectException) {
            throw(Exception("ERROR: Failed to connect to the server.", e))
        } catch (e: UnknownHostException) {
            throw(Exception("ERROR: Unknown host.", e))
        }
    }

    override fun run() {
        val configMessage = Message(
            type = MessageType.Config,
            id = clientID,
            topic = "logs",
            timestamp = timeConverter.getTimestamp(),
            mode = MessageMode.Subscriber,
            payload = null
        )
        send(configMessage)
        while (isInitialized) {
            try {
                sleep(ClientUtils.LISTENING_THREAD_SLEEP)
                val receivedMessage = reader!!.readObject() as Message
                receivedMessages.add(receivedMessage)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun isConnected(): Boolean {
        if(!this.isInitialized) return false
        return socket!!.isConnected
    }

    override fun getStatus(): String {
        if(!this.isInitialized) throw IllegalStateException("ERROR: Connection has not been initialized.")
        val communicationData = Pair(topics, subscriptions)
        return mapper.writeValueAsString(communicationData)
    }

    override fun getServerStatus(callback: (status: String) -> Unit) {
        if(!this.isInitialized) throw IllegalStateException("ERROR: Connection has not been initialized.")
        val message = Message(
            type = MessageType.Status,
            id = clientID,
            topic = "logs",
            timestamp = timeConverter.getTimestamp(),
            mode = MessageMode.Subscriber,
            payload = null
        )
        send(message)
        statusCallback = callback
    }

    override fun getServerLogs(callback: (info: String, success: Boolean) -> Unit) {
        if(!this.isInitialized) throw IllegalStateException("ERROR: Connection has not been initialized.")
        val nestedCallback = { message: Message ->
            if(message.type == MessageType.Acknowledge || message.type == MessageType.Reject) {
                val payload = message.payload as MessagePayload
                val responseTimestamp = message.timestamp
                val request = sentMessages.firstOrNull {
                    timeConverter.getTimestampDiffMs(it.timestamp, responseTimestamp) == 0L
                }
                if(request != null) {
                    if(message.type == MessageType.Acknowledge) {
                        if(request.mode == MessageMode.Producer) topics.add(payload.topicOfMessage)
                        else subscriptions.add(payload.topicOfMessage)
                    }
                    callback.invoke(payload.message, payload.success)
                }
            }
        }
        replyCallback = nestedCallback
    }

    override fun createProducer(topicName: String) {
        if(!this.isInitialized) throw IllegalStateException("ERROR: Connection has not been initialized.")
        val message = Message(
            type = MessageType.Register,
            id = clientID,
            topic = topicName,
            timestamp = timeConverter.getTimestamp(),
            mode = MessageMode.Producer,
            payload = null
        )
        send(message)
    }

    override fun produce(topicName: String, payload: Payload) {
        if(!this.isInitialized) throw IllegalStateException("ERROR: Connection has not been initialized.")
        val message = Message(
            type = MessageType.Message,
            id = clientID,
            topic = topicName,
            timestamp = timeConverter.getTimestamp(),
            mode = MessageMode.Producer,
            payload = payload
        )
        send(message)
    }

    override fun sendFile(topicName: String, filePath: Path) {
        if(!this.isInitialized) throw IllegalStateException("ERROR: Connection has not been initialized.")
        val payload = FilePayload(
            filename = filePath.fileName.toString(),
            data = Files.readString(filePath)
        )
        val message = Message(
            type = MessageType.File,
            id = clientID,
            topic = topicName,
            timestamp = timeConverter.getTimestamp(),
            mode = MessageMode.Producer,
            payload = payload
        )
        send(message)
    }

    override fun withdrawProducer(topicName: String) {
        if(!this.isInitialized) throw IllegalStateException("ERROR: Connection has not been initialized.")
        val message = Message(
            type = MessageType.Withdraw,
            id = clientID,
            topic = topicName,
            timestamp = timeConverter.getTimestamp(),
            mode = MessageMode.Producer,
            payload = null
        )
        send(message)
    }

    override fun createSubscriber(topicName: String, callback: (message: Message) -> Unit) {
        if(!this.isInitialized) throw IllegalStateException("ERROR: Connection has not been initialized.")
        val message = Message(
            type = MessageType.Register,
            id = clientID,
            topic = topicName,
            timestamp = timeConverter.getTimestamp(),
            mode = MessageMode.Subscriber,
            payload = null
        )
        topicCallbacks[topicName] = callback
        send(message)
    }

    override fun withdrawSubscriber(topicName: String) {
        if(!this.isInitialized) throw IllegalStateException("ERROR: Connection has not been initialized.")
        val message = Message(
            type = MessageType.Withdraw,
            id = clientID,
            topic = topicName,
            timestamp = timeConverter.getTimestamp(),
            mode = MessageMode.Subscriber,
            payload = null
        )
        topicCallbacks.remove(topicName)
        send(message)
    }

    override fun stopConnection() {
        if(isInitialized) {
            isInitialized = false
            writer!!.close()
            reader!!.close()
            socket!!.close()
            configuration = null
            topics.clear()
            subscriptions.clear()
            sentMessages.clear()
            receivedMessages.clear()
            topicCallbacks.clear()
            statusCallback = { _ -> }
            replyCallback = { _ -> }
        }
    }
}