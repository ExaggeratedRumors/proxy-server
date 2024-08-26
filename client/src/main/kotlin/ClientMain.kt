import java.net.ConnectException
import java.net.Socket
import java.net.UnknownHostException
import utils.Constance
import communication.ClientConnection
import ui.ClientWindow

fun main(args: Array<String>) {
    val ip = if(args.isEmpty()) Constance.DEFAULT_IP else args[0]
    val port = if(args.size < 2) Constance.DEFAULT_PORT else args[1].toInt()

    try {
        val socket = Socket(ip, port)
        val connection = ClientConnection(socket)
        connection.start()
        println("ENGINE: Client started.")
        ClientWindow(connection)
    } catch (e: IllegalArgumentException) {
        e.printStackTrace()
        println("ERROR: Port number must be between 0 and 65535")
    } catch (e: ConnectException) {
        e.printStackTrace()
        println("ERROR: Cannot connect to the server.")
    } catch (e: UnknownHostException) {
        e.printStackTrace()
        println("ERROR: Unknown host.")
    } catch (e: Exception) {
        e.printStackTrace()
    }
}