package communication

import ui.ClientWindow
import utils.Constance
import java.io.DataInputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.ConnectException
import java.net.Socket
import java.net.SocketException
import java.net.UnknownHostException

class ClientConnection  (
    private val port: Int = Constance.DEFAULT_PORT,
    private val ip: String = Constance.DEFAULT_IP
) : Thread() {
    private var isInitialized = false
    private lateinit var socket: Socket
    private lateinit var writer: OutputStream
    private lateinit var reader: InputStream
    private val buffer: ByteArray = ByteArray(Constance.MAX_BUFFER_SIZE)

    fun startConnection(): Boolean {
        try {
            socket = Socket(ip, port)
            writer = socket.getOutputStream()
            reader = DataInputStream(socket.getInputStream())
            start()
            isInitialized = true
            println("ENGINE: Client started.")
        } catch (e: IllegalArgumentException) {
            throw(IllegalStateException("ERROR: Port number must be between 0 and 65535"))
        } catch (e: ConnectException) {
            throw(ConnectException("ERROR: Failed to connect to the server."))
        } catch (e: UnknownHostException) {
            throw(UnknownHostException("ERROR: Unknown host."))
        } catch (e: Exception) {
            throw(e)
        }
        return isInitialized
    }

    fun sendAndReceive(message: Char): String? {
        if(!isInitialized) throw IllegalStateException()
        try {
            writer.write(ByteArray(1) { message.code.toByte() })
            val messageSize = reader.read(buffer)
            if(messageSize == -1) return null
            return buffer.copyOfRange(0, messageSize).toString(Charsets.UTF_8)
        } catch (e: SocketException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun shutdown() {
        isInitialized = false
    }
}