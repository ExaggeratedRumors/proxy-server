package com.ertools.communication

import com.ertools.dto.Request
import com.ertools.dto.Response

interface ConnectionListener {
    fun onClientAccept(port: Int, ip: String)
    fun onClientDisconnect(port: Int)
    fun onServerBusy(port: Int)
    fun onMessageReceive(request: Request)
    fun onMessageSend(response: Response)
}