package dto

class ClientInfo (
    val id: Int,
    val port: Int,
    val ip: String
) {
    val messages: ArrayList<String> = ArrayList()
}