package com.ertools.runtime

interface ConnectionListener {
    fun onClientAccept(port: Int, ip: String)
    fun onClientDisconnect(port: Int)
    fun onServerBusy(port: Int)
    fun onMessageReceive(port: Int, size: Int, message: String)
    fun onMessageSend(port: Int, size: Int, message: String)
}