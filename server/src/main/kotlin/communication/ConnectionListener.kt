package com.ertools.communication

import dto.Request
import dto.Response


interface ConnectionListener {
    fun onClientAccept(port: Int, ip: String)
    fun onClientDisconnect(port: Int)
    fun onMessageReceive(request: Request)
    fun onMessageSend(response: Response)
}