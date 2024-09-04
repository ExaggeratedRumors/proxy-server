package com.ertools.communication

import com.ertools.utils.Constance
import dto.ClientInfo
import dto.Configuration
import dto.Request
import dto.Response
import java.io.*
import java.net.Socket
import java.net.SocketTimeoutException

class ClientServiceThread(
    private val client: Socket,
    private val listeners: List<ConnectionListener>,
    timeout: Int = Configuration.TIMEOUT
): Thread() {
    private var stopClient = true
    private val writer: OutputStream = client.getOutputStream()
    private val reader: InputStream = client.getInputStream()

    init {
        client.soTimeout = timeout
        listeners.forEach { it.onClientAccept(port = client.port, ip = client.inetAddress.hostAddress) }
    }

    override fun run() {
        stopClient = false
        while (!stopClient && client.isConnected) {
            try {
                sleep(Constance.CONNECTION_THREAD_SLEEP)
                recv() ?: continue
            } catch (e: SocketTimeoutException) {
                e.printStackTrace()
                if (Constance.DEBUG_MODE) error("ENGINE: Socket timeout ${client.inetAddress.hostAddress}")
            } catch (e: OutOfMemoryError) {
                shutdown()
                e.printStackTrace()
                if (Constance.DEBUG_MODE) error("ERROR: No enough memory to allocate received message.")
            } catch (e: IOException) {
                shutdown()
                e.printStackTrace()
                if (Constance.DEBUG_MODE) error("ERROR: IO error occurs.")
            } catch (e: Exception) {
                shutdown()
                e.printStackTrace()
            }
        }
    }

    private fun recv(): ByteArray? {
        if(stopClient) throw IllegalStateException("ERROR: Client ${client.port} is stopped.")
        val buffer = ByteArray(Configuration.SIZE_LIMIT)
        val messageSize = reader.read(buffer, 0, Configuration.SIZE_LIMIT)
        if(messageSize == -1) return null
        val rawMessage = buffer.copyOfRange(0, messageSize)
        val clientInfo = ClientInfo("", port = client.port, ip = client.inetAddress.hostAddress, socket = client)
        listeners.forEach { it.onMessageReceive(Request(rawMessage, clientInfo)) }
        return rawMessage
    }

    /*********/
    /** API **/
    /*********/
    fun shutdown() {
        if(stopClient) return
        listeners.forEach { it.onClientDisconnect(client.port) }
        stopClient = true
        writer.close()
        reader.close()
        client.close()
    }

    fun send(response: Response) {
        if(stopClient) throw IllegalStateException("ERROR: Client ${client.port} is stopped.")
        try {
            val bufferStream = ByteArrayOutputStream(Configuration.SIZE_LIMIT)
            val objectOutputStream = ObjectOutputStream(bufferStream)
            objectOutputStream.writeObject(response.message)
            objectOutputStream.flush()
            val byteArray = bufferStream.toByteArray()
            val bufferStreamSize = bufferStream.size()
            objectOutputStream.close()
            bufferStream.close()
            if(bufferStreamSize > Configuration.SIZE_LIMIT) throw IllegalArgumentException()
            writer.write(byteArray)
            writer.flush()
            listeners.forEach { it.onMessageSend(response) }
        } catch (e: Exception) {
            e.printStackTrace()
            throw(e)
        }
    }
}