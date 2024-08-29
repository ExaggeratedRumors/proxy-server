package communication

import dto.Message
import dto.MessagePayload
import java.io.File


interface ClientAPI {
    fun start(serverIP: String, serverPort: Int, clientID: Int)
    fun isConnected(): Boolean
    fun getStatus(): String
    fun getServerStatus(callback: (status: String) -> Unit)
    fun getServerLogs(callback: (message: Message) -> Unit)
    fun createProducer(topicName: String)
    fun produce(topicName: String, payload: MessagePayload)
    fun sendFile(topicName: String, file: File)
    fun withdrawProducer(topicName: String)
    fun createSubscriber(topicName: String, callback: (message: Message) -> Unit)
    fun withdrawSubscriber(topicName: String)
    fun stopConnection()
}
