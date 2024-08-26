package com.ertools.communication

import com.ertools.dto.Request
import com.ertools.dto.Response
import com.ertools.utils.Configuration
import com.ertools.utils.Constance
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
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
    private val writer: OutputStream = client.getOutputStream()
    private val reader: InputStream = DataInputStream(client.getInputStream())
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
        listeners.forEach { it.onClientDisconnect(client.port) }
        stopClient = true
        client.close()
    }

    fun refuseConnection() {
        writer.bufferedWriter().use {
            //it.write(Utils.BUSY_STATEMENT)
            it.flush()
        }
        listeners.forEach { it.onServerBusy(client.port) }
        stopClient = true
        client.close()
    }

    /*
        public String receiveMessage() throws IOException {
        if (clientSocket == null || !clientSocket.isConnected())
            throw new IOException(CLIENT_NOT_CONNECTED_MSG);

        byte[] bytes = new byte[sizeLimit];
        int bytesRead = inputStream.read(bytes);

        if (bytesRead == -1)
            throw new IOException("Client communication error");

        return new String(bytes, 0, bytesRead);
    }





        private Message mapReceivedMessage(String receivedMessage) {
        if (receivedMessage == null || receivedMessage.isEmpty())
            return null;

        try {
            String modifiedJson = insertTypeToPayload(receivedMessage);
            Message message = mapper.readValue(modifiedJson, Message.class);

            Set<ConstraintViolation<Message>> violations = Validator.validateJsonObject(message);
            if (violations == null || !violations.isEmpty())
                return null;

            return message;
        } catch (JsonProcessingException e) {
//            e.printStackTrace();
            return null;
        }

    }

     */

    private fun recv(): String? {
        messageSize = reader.read(buffer)
        if(messageSize == -1) return null

        val message: String = buffer.copyOfRange(0, messageSize).toString(Charsets.UTF_8)
        val request: Request = mapper.readValue(message, Request::class.java)

        if(Constance.DEBUG_MODE) println("ENGINE: Add request: $request")
        listeners.forEach { it.onMessageReceive(request) }

        return message
    }

    /*private fun send() {
        writer.write(buffer.copyOfRange(0, messageSize))
        val message = buffer.copyOfRange(0, messageSize).toString(Charsets.UTF_8)
        listener.onMessageSend(Response(message, listOf(client.port)))
    }*/
}