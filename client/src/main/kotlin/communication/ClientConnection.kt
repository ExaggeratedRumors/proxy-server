package communication

import utils.TimeConverter
import com.fasterxml.jackson.databind.ObjectMapper
import dto.*
import utils.ClientUtils
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.UnknownHostException

class ClientConnection (
    private val port: Int = ClientUtils.DEFAULT_PORT,
    private val ip: String = ClientUtils.DEFAULT_IP
) : Thread(), ClientAPI {
    /** Connection data **/
    private var clientID: String = "client$port"
    private var isInitialized = false
    private var socket: Socket? = null
    private var writer: ObjectOutputStream? = null
    private var reader: ObjectInputStream? = null
    private var timeConverter: TimeConverter = TimeConverter()

    /** Communication sources **/
    private val topics: ArrayList<String> = ArrayList()
    private val feed: ArrayList<String> = ArrayList()
    private val mapper: ObjectMapper = ObjectMapper()
    private val exchange: ArrayList<Pair<Message, Message>> = ArrayList()


    private fun sendAndReceive(message: Message): Message {
        if(!isInitialized) throw IllegalStateException("ERROR: Connection has not been initialized.")
        try {
            writer!!.writeObject(message)
            val receivedMessage = reader!!.readObject() as Message
            return receivedMessage
        } catch (e: Exception) {
            e.printStackTrace()
            throw(e)
        }
    }

    /** Client API **/
    override fun start(serverIP: String, serverPort: Int, clientID: String) {
        try {
            if(this.isInitialized) stopConnection()
            this.socket = Socket(ip, port)
            this.socket!!.bind(InetSocketAddress(serverIP, serverPort))
            this.writer = ObjectOutputStream(socket!!.getOutputStream())
            this.reader = ObjectInputStream(socket!!.getInputStream())
            this.topics.clear()
            this.feed.clear()
            this.isInitialized = true
            this.clientID = clientID
            if(ClientUtils.DEBUG_MODE) println("ENGINE: Client started: (#$clientID $serverIP:$serverPort).")
        } catch (e: IllegalArgumentException) {
            throw(IllegalStateException("ERROR: Port number must be between 0 and 65535", e))
        } catch (e: ConnectException) {
            throw(Exception("ERROR: Failed to connect to the server.", e))
        } catch (e: UnknownHostException) {
            throw(Exception("ERROR: Unknown host.", e))
        }
    }

    override fun isConnected(): Boolean {
        if(!this.isInitialized) return false
        return socket!!.isConnected
    }

    override fun getStatus(): String {
        if(!this.isInitialized) throw IllegalStateException("ERROR: Connection has not been initialized.")
        val communicationData = Pair(topics, feed)
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
        val response = sendAndReceive(message)
        val statusPayload = response.payload as StatusPayload
        val statusData = mapper.writeValueAsString(statusPayload.data)
        exchange.add(Pair(message, response))
        callback.invoke(statusData)
    }

    override fun getServerLogs(callback: (message: Message) -> Unit) {
        if(!this.isInitialized) throw IllegalStateException("ERROR: Connection has not been initialized.")
        val output = StringBuilder()
        exchange.forEach {
            val responseType = it.second.type
            if(responseType != MessageType.Acknowledge && responseType != MessageType.Reject) return@forEach
            val responsePayload = it.second.payload ?: return@forEach
            val timeDiff = timeConverter.getTimestampDiffMs(
                it.first.timestamp,
                (responsePayload as MessagePayload).timestampOfMessage
            )
            val successMessage = if(responsePayload.success) "success" else "failed"
            output.append("[${timeDiff}ms] ($successMessage) ${responsePayload.message}")
        }
    }

    override fun createProducer(topicName: String) {
        if(!this.isInitialized) throw IllegalStateException("ERROR: Connection has not been initialized.")

    }

    override fun produce(topicName: String, payload: MessagePayload) {
        if(!this.isInitialized) throw IllegalStateException("ERROR: Connection has not been initialized.")

    }

    override fun sendFile(topicName: String, file: File) {
        if(!this.isInitialized) throw IllegalStateException("ERROR: Connection has not been initialized.")

    }

    override fun withdrawProducer(topicName: String) {
        if(!this.isInitialized) throw IllegalStateException("ERROR: Connection has not been initialized.")

    }

    override fun createSubscriber(topicName: String, callback: (message: Message) -> Unit) {
        if(!this.isInitialized) throw IllegalStateException("ERROR: Connection has not been initialized.")

    }

    override fun withdrawSubscriber(topicName: String) {
        if(!this.isInitialized) throw IllegalStateException("ERROR: Connection has not been initialized.")

    }

    override fun stopConnection() {
        if(isInitialized) {
            isInitialized = false
            writer!!.close()
            reader!!.close()
            socket!!.close()
        }
    }
}