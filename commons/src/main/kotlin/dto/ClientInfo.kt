package dto

import java.net.Socket

class ClientInfo (
    val id: String,
    val port: Int,
    val ip: String,
    val socket: Socket
)