package communication

import dto.Message
import dto.Payload
import java.nio.file.Path


interface ClientAPI {
    fun start(serverIP: String, serverPort: Int, clientID: String)
    fun isConnected(): Boolean
    fun getStatus(): String
    fun getServerStatus(callback: (status: Map<String, String>) -> Unit)
    fun getServerLogs(callback: (info: String, success: Boolean) -> Unit)
    fun createProducer(topicName: String)
    fun produce(topicName: String, payload: Payload)
    fun sendFile(topicName: String, filePath: Path)
    fun withdrawProducer(topicName: String)
    fun createSubscriber(topicName: String, callback: (message: Message) -> Unit)
    fun withdrawSubscriber(topicName: String)
    fun stopConnection()
}
