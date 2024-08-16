package com.ertools.runtime

import com.ertools.utils.Configuration
import com.ertools.utils.Constance
import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.net.SocketTimeoutException

class ClientServiceThread(
    private val client: Socket,
    private val listener: ConnectionListener,
    timeout: Int = Configuration.TIMEOUT
): Thread() {
    private var stopClient = true
    private var messageSize: Int = 0
    private var buffer: ByteArray = ByteArray(Configuration.SIZE_LIMIT)
    private val writer: OutputStream = client.getOutputStream()
    private val reader: InputStream = DataInputStream(client.getInputStream())

    init {
        client.soTimeout = timeout
    }

    override fun run() {
        stopClient = false
        while (!stopClient && client.isConnected) {
            try {
                sleep(Constance.CONNECTION_THREAD_SLEEP)
                recv() ?: continue
                send()
            } catch (e: SocketTimeoutException) {
                if (Constance.DEBUG_MODE) println("ENGINE: Socket timeout ${client.inetAddress.hostAddress}")
            } catch (e: OutOfMemoryError) {
                shutdown()
                e.printStackTrace()
                error("ERROR: No enough memory to allocate received message.")
            } catch (e: IOException) {
                shutdown()
                e.printStackTrace()
                error("ERROR: IO error occurs.")
            } catch (e: Exception) {
                shutdown()
                e.printStackTrace()
            }
        }
    }

    fun shutdown() {
        writer.bufferedWriter().use {
            //it.write(Utils.DISCONNECT_STATEMENT)
            it.flush()
        }
        listener.onClientDisconnect(client.port)
        stopClient = true
        client.close()
    }

    fun refuseConnection() {
        writer.bufferedWriter().use {
            //it.write(Utils.BUSY_STATEMENT)
            it.flush()
        }
        listener.onServerBusy(client.port)
        stopClient = true
        client.close()
    }


    private fun recv(): String? {
        messageSize = reader.read(buffer)
        if(messageSize == -1) return null
        val message = buffer.copyOfRange(0, messageSize).toString(Charsets.UTF_8)
        println(message)
        listener.onMessageReceive(client.port, messageSize, message)
        return message
    }

    private fun send() {
        writer.write(buffer.copyOfRange(0, messageSize))
        val message = buffer.copyOfRange(0, messageSize).toString(Charsets.UTF_8)
        listener.onMessageSend(client.port, messageSize, message)
    }
}