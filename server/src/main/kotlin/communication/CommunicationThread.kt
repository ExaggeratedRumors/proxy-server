package com.ertools.communication

import dto.Configuration
import com.ertools.utils.Constance
import dto.Response
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class CommunicationThread(
    private val port: Int,
    private val listenAddresses: List<String>,
    private val allowedAddresses: List<String>
) : Thread() {
    private var pendingConnections: MutableMap<String, ServerSocket> = HashMap()
    private var connections: MutableMap<Int, ClientServiceThread> = HashMap()
    private var stopServer: AtomicBoolean = AtomicBoolean(false)
    private var listeners: ArrayList<ConnectionListener> = ArrayList()

    /*************/
    /**** API ****/
    /*************/
    override fun run() {
        listenAddresses.forEach { ip ->
            thread {
                /** Create server socket **/
                val serverSocket = bindServerSocket(ip)
                if(Constance.DEBUG_MODE) println("ENGINE: Bind $ip")

                while(!stopServer.get()) {
                    try {
                        /** Wait for client connection **/
                        val clientSocket = serverSocket.accept()
                        val clientIp = clientSocket.inetAddress.hostAddress
                        if (Constance.DEBUG_MODE) println("ENGINE: Connect $ip")

                        /** Check client IP is allowed **/
                        if (!isIpAllowed(clientIp, allowedAddresses)) {
                            if (Constance.DEBUG_MODE) println("ENGINE: Client refused $clientIp")
                            clientSocket.close()
                            continue
                        }

                        /** Start client service thread **/
                        val connection = ClientServiceThread(clientSocket, listeners, 1000 * Configuration.TIMEOUT)
                        connections[clientSocket.port] = connection
                        connection.start()
                    } catch (e: SocketTimeoutException) {
                        continue
                    } catch (e: Exception) {
                        break
                    }
                    if (Constance.DEBUG_MODE) println("ERROR: Socket timeout for $ip")
                }
                closeServerSocket(serverSocket, ip)
            }
        }
    }

    fun send(response: Response) {
        response.receivers.forEach {
            connections[it.port]?.send(response)
        }
    }

    fun shutdown() {
        stopServer.set(true)
        pendingConnections.forEach{ it.value.close() }
        pendingConnections.clear()

        connections.forEach { it.value.shutdown() }
    }

    fun addListener(connectionListener: ConnectionListener) {
        listeners.add(connectionListener)
    }

    /*************/
    /** Private **/
    /*************/

    private fun bindServerSocket(ip: String): ServerSocket {
        val serverSocket: ServerSocket
        if(ip == "*") {
            serverSocket = ServerSocket(port)
        } else {
            val inetAddress = InetAddress.getByName(ip)
            serverSocket = ServerSocket()
            //serverSocket.reuseAddress = true
            serverSocket.soTimeout = Configuration.TIMEOUT * 1000
            serverSocket.bind(InetSocketAddress(inetAddress, port))
        }
        pendingConnections[ip] = serverSocket
        return serverSocket
    }

    private fun closeServerSocket(serverSocket: ServerSocket, ip: String) {
        pendingConnections.remove(ip)
        serverSocket.close()
    }

    private fun isIpAllowed(clientIp: String, allowedIpAddresses: List<String>): Boolean {
        if(Constance.DEBUG_MODE) println("ENGINE: Check allowed IP: $clientIp from $allowedIpAddresses")
        if("any" in allowedIpAddresses) return true
        if("*" in allowedIpAddresses) return true
        if(clientIp in allowedIpAddresses) return true
        if("localhost" in allowedIpAddresses && clientIp == "127.0.0.1") return true
        for(cidr in allowedIpAddresses) {
            if(!cidr.contains('/')) continue
            val parts = cidr.split("/")
            val network = InetAddress.getByName(parts[0]).address
            val mask = (0xFFFFFFFF shl (32 - parts[1].toInt())).toInt()

            val ipAddr = InetAddress.getByName(clientIp).address
            val networkAddr = network.fold(0) { acc, byte -> (acc shl 8) or (byte.toInt() and 0xFF) }
            val ipAddrInt = ipAddr.fold(0) { acc, byte -> (acc shl 8) or (byte.toInt() and 0xFF) }

            if((networkAddr and mask) == (ipAddrInt and mask)) return true
        }
        return false
    }
}