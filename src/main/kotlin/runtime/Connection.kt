package com.ertools.runtime

import com.ertools.utils.Constance
import java.net.Socket

class Connection(
    private val client: Socket,
    private val listener: ConnectionListener
): Thread() {
    var isConnected = false

    override fun run() {
        isConnected = true
        while(isConnected && client.isConnected) {
            try {
                sleep(Constance.CONNECTION_THREAD_SLEEP)
                recv() ?: continue
                send()
            } catch (e: OutOfMemoryError) {
                shutdown()
                e.printStackTrace()
                error("No enough memory to allocate received message.")
            }
        }
    }

    fun shutdown() {
        listener.onClientDisconnect(client.port)
        isConnected = false
        client.close()
    }

    fun refuseConnection() {
        listener.onServerBusy(client.port)
        isConnected = false
        client.close()
    }


    private fun recv(): String? {
        return null
    }

    private fun send() {

    }
}