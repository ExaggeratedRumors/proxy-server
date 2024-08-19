package com.ertools.runtime

import com.ertools.dto.Response

interface ConnectionListener {
    fun onClientAccept(port: Int, ip: String)
    fun onClientDisconnect(port: Int)
    fun onServerBusy(port: Int)
    fun onMessageReceive(port: Int, size: Int, message: String)
    fun onMessageSend(response: Response)
}