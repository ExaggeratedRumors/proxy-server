package communication

import dto.Message


interface ClientAPI {
    fun start(serverIP: String, serverPort: Int, clientID: Int)
    fun isConnected(): Boolean
    fun getStatus(): String
    fun getServerStatus(callback: (status: String) -> Unit)
    fun getServerLogs(callback: (message: Message) -> Unit)
}
