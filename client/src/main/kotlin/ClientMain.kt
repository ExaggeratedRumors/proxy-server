import communication.ClientConnection
import ui.ClientWindow

fun main() {
    try {
        val connection = ClientConnection()
        ClientWindow(connection)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}