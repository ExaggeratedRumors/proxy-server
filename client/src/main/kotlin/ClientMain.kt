import communication.ClientConnection
import ui.ClientWindow
import utils.Constance

fun main(args: Array<String>) {
    try {
        val ip = if(args.isEmpty()) Constance.DEFAULT_IP else args[0]
        val port = if(args.size < 2) Constance.DEFAULT_PORT else args[1].toInt()
        val connection = ClientConnection(port, ip)
        ClientWindow(connection).renewConnection()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}