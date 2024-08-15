package com.ertools.runtime

import com.ertools.utils.Configuration
import com.ertools.utils.Constance
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.SocketTimeoutException
import kotlin.concurrent.thread

class CommunicationThread(
    private val port: Int,
    private val listenAddresses: List<String>,
    private val connectionListener: ConnectionListener
) : Thread() {
    var connection: ClientServiceThread? = null
    private var pendingConnections: MutableMap<String, ServerSocket> = HashMap()
    private var connections: MutableMap<String, ClientServiceThread> = HashMap()

    override fun run() {
        listenAddresses.forEach { ip ->
            thread {
                try {
                    val serverSocket: ServerSocket
                    if(ip == "*") {
                        serverSocket = ServerSocket(port)
                    } else {
                        val inetAddress = InetAddress.getByName(ip)
                        serverSocket = ServerSocket()
                        //serverSocket.reuseAddress = true
                        serverSocket.soTimeout = Configuration.timeout * 1000
                        serverSocket.bind(InetSocketAddress(inetAddress, port))
                    }
                    pendingConnections[ip] = serverSocket
                    if (Constance.DEBUG_MODE) println("ENGINE: Bind $ip")

                    val clientSocket = serverSocket.accept()
                    if(Constance.DEBUG_MODE) println("ENGINE: Connect $ip")
                    pendingConnections.remove(ip)
                    serverSocket.close()


                    val connection = ClientServiceThread(clientSocket, connectionListener)
                    connections[ip] = connection
                    connection.start()
                } catch (e: SocketTimeoutException) {
                    if (Constance.DEBUG_MODE) println("ERROR: Socket timeout for $ip")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun shutdown() {
        pendingConnections.forEach{ it.value.close() }
        pendingConnections.clear()

        connections.forEach { it.value.shutdown() }
    }
}