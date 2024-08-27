package dto

class ClientInfo (
    val id: Int,
    val port: Int,
    val ip: String
) {
    val messages: ArrayList<Message> = ArrayList()
}