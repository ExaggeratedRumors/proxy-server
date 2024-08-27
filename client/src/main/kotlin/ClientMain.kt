import communication.ClientConnection
import ui.ClientWindow
import utils.ClientUtils

fun main(args: Array<String>) {
    try {
        val ip = if(args.isEmpty()) ClientUtils.DEFAULT_IP else args[0]
        val port = if(args.size < 2) ClientUtils.DEFAULT_PORT else args[1].toInt()
        val connection = ClientConnection(port, ip)
        ClientWindow(connection).renewConnection()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}