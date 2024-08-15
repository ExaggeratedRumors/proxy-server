package com.ertools.dto

import com.ertools.runtime.ClientServiceThread
import java.net.ServerSocket

data class ClientConnection (
    val ip: String,
    val acceptThread: Thread,
    val serverSocket: ServerSocket,
    var clientServiceThread: ClientServiceThread? = null
)