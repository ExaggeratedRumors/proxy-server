package com.ertools.utils

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

object Configuration {
    var serverId: Int = 0
        private set
    var listenPort: Int = 8080
        private set
    var timeout: Int = 20
        private set
    lateinit var listenAddresses: String
        private set
    lateinit var allowedIpAdresses: List<String>
        private set

    fun load() {
        try {
            val config: Config = ConfigFactory.load("application.conf")
            val serverConfig = config.getConfig("server")
            serverId = serverConfig.getInt("server_id")
            listenPort = serverConfig.getInt("listen_port")
            timeout = serverConfig.getInt("time_out")
            listenAddresses = serverConfig.getString("listen_addresses")
            allowedIpAdresses = serverConfig.getStringList("allowed_ip_addresses")
        } catch (e: Exception) {
            e.printStackTrace()
            error("Configuration: Content of application.conf file is not valid.")
        }
    }

    data class CommandEntry(
        val call: String,
        val enabled: Boolean,
        val reqAdmin: Boolean
    )
}