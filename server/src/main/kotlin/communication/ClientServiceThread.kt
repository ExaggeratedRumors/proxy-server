package com.ertools.communication

import dto.Response
import dto.Configuration
import com.ertools.utils.Constance
import com.fasterxml.jackson.databind.ObjectMapper
import dto.Request
import java.io.*
import java.net.Socket
import java.net.SocketTimeoutException

class ClientServiceThread(
    private val client: Socket,
    private val listeners: List<ConnectionListener>,
    timeout: Int = Configuration.TIMEOUT
): Thread() {
    private var stopClient = true
    private var messageSize: Int = 0
    private var buffer: ByteArray = ByteArray(Configuration.SIZE_LIMIT)
    private val writer: ObjectOutputStream = ObjectOutputStream(client.getOutputStream())
    private val reader: ObjectInputStream = ObjectInputStream(client.getInputStream())
    private val mapper: ObjectMapper = ObjectMapper()

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

    private fun recv(): String? {
        if(stopClient) throw IllegalStateException("ERROR: Client ${client.port} is stopped.")
        try {
            messageSize = reader.read(buffer)
            if (messageSize == -1) return null

            val rawMessage: String = buffer.copyOfRange(0, messageSize).toString(Charsets.UTF_8)
            listeners.forEach { it.onMessageReceive(Request(rawMessage, client.port)) }

            return rawMessage
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
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
            writer.writeObject(response.message)
            listeners.forEach { it.onMessageSend(response) }
        } catch (e: Exception) {
            e.printStackTrace()
            throw(e)
        }
    }


/*    fun refuseConnection() {
        writer.bufferedWriter().use {
            //it.write(Utils.BUSY_STATEMENT)
            it.flush()
        }
        listeners.forEach { it.onServerBusy(client.port) }
        stopClient = true
        client.close()
    }*/



    /*

    private fun send() {
        writer.write(buffer.copyOfRange(0, messageSize))
        val message = buffer.copyOfRange(0, messageSize).toString(Charsets.UTF_8)
        listener.onMessageSend(Response(message, listOf(client.port)))
    }*/
}