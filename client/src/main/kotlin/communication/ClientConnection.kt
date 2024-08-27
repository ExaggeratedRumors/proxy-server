package communication

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
    private var socket: Socket? = null
    private var writer: OutputStream? = null
    private var reader: InputStream? = null
    private val buffer: ByteArray = ByteArray(Constance.MAX_BUFFER_SIZE)

    fun startConnection() {
        try {
            if(isInitialized) shutdown()
            socket = Socket(ip, port)
            writer = socket!!.getOutputStream()
            reader = DataInputStream(socket!!.getInputStream())
            isInitialized = true
            println("ENGINE: Client started.")
        } catch (e: IllegalArgumentException) {
            throw(IllegalStateException("ERROR: Port number must be between 0 and 65535", e))
        } catch (e: ConnectException) {
            throw(Exception("ERROR: Failed to connect to the server.", e))
        } catch (e: UnknownHostException) {
            e.addSuppressed(Exception("ERROR: Unknown host.", e))
            throw(e)
        }
    }

    fun sendAndReceive(message: Char): String? {
        if(!isInitialized) throw IllegalStateException("ERROR: Connection has not been initialized.")
        try {
            writer!!.write(ByteArray(1) { message.code.toByte() })
            val messageSize = reader!!.read(buffer)
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
        if(isInitialized) {
            isInitialized = false
            writer!!.close()
            reader!!.close()
            socket!!.close()
        }
    }
}