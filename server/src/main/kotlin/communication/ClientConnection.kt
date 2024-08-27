package com.ertools.communication

import java.net.ServerSocket

data class ClientConnection (
    val ip: String,
    val acceptThread: Thread,
    val serverSocket: ServerSocket,
    var clientServiceThread: ClientServiceThread? = null
)