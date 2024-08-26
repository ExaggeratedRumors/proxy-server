package communication

import utils.Constance
import java.io.DataInputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.net.SocketException

class ClientConnection  (
    socket: Socket
) : Thread() {
    private val writer: OutputStream = socket.getOutputStream()
    private val reader: InputStream = DataInputStream(socket.getInputStream())
    private val buffer =  ByteArray(Constance.MAX_BUFFER_SIZE)

    fun sendAndReceive(message: Char): String? {
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
}