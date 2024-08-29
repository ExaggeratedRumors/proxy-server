package communication

import com.fasterxml.jackson.databind.ObjectMapper
import dto.Message
import dto.MessagePayload
import utils.ClientUtils
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.ConnectException
import java.net.Socket
import java.net.UnknownHostException

class ClientConnection (
    private val port: Int = ClientUtils.DEFAULT_PORT,
    private val ip: String = ClientUtils.DEFAULT_IP
) : Thread(), ClientAPI {
    /** Connection data **/
    private var clientID: Int = 0
    private var isInitialized = false
    private var socket: Socket? = null
    private var writer: ObjectOutputStream? = null
    private var reader: ObjectInputStream? = null

    /** Communication sources **/
    private val topics: ArrayList<String> = ArrayList()
    private val feed: ArrayList<String> = ArrayList()
    private val mapper: ObjectMapper = ObjectMapper()


    fun sendAndReceive(message: Char): Message {
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
    override fun start(serverIP: String, serverPort: Int, clientID: Int) {
        try {
            if(this.isInitialized) stopConnection()
            this.socket = Socket(ip, port)
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

    }

    override fun getServerLogs(callback: (message: Message) -> Unit) {

    }

    override fun createProducer(topicName: String) {

    }

    override fun produce(topicName: String, payload: MessagePayload) {

    }

    override fun sendFile(topicName: String, file: File) {

    }

    override fun withdrawProducer(topicName: String) {

    }

    override fun createSubscriber(topicName: String, callback: (message: Message) -> Unit) {

    }

    override fun withdrawSubscriber(topicName: String) {

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