package com.ertools.runtime

import com.ertools.utils.Constance
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

class CommunicationThread(
    private val port: Int,
    private val listenAddresses: List<String>,
    private val connectionListener: ConnectionListener
) : Thread() {
    private val sockets: List<Socket> = ArrayList()
    private var connections: MutableMap<String, Connection> = HashMap()

    override fun run() {
        listenAddresses.map { ip ->
            thread {
                try {
                    val inetAddress = InetAddress.getByName(ip)
                    val serverSocket = ServerSocket()
                    serverSocket.bind(InetSocketAddress(inetAddress, port))
                    if(Constance.DEBUG_MODE) println("ENGINE: Bind $ip")

                    while(true) {
                        val clientSocket = serverSocket.accept()
                        connections[ip] = Connection(clientSocket, connectionListener)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }


    }
}