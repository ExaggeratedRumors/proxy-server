package com.ertools.utils

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

object Configuration {
    lateinit var serverId: String
        private set
    var listenPort: Int = 8080
        private set
    var timeout: Int = 20
        private set
    var sizeLimit: Long = 104576
        private set
    lateinit var listenAddresses: String
        private set
    lateinit var allowedIpAdresses: List<String>
        private set

    fun load() {
        try {
            val config: Config = ConfigFactory.load("application.conf")
            val serverConfig = config.getConfig("server")
            serverId = serverConfig.getString("server_id")
            listenPort = serverConfig.getInt("listen_port")
            timeout = serverConfig.getInt("timeout")
            sizeLimit = serverConfig.getLong("size_limit")
            listenAddresses = serverConfig.getString("listen_addresses")
            allowedIpAdresses = serverConfig.getStringList("allowed_ip_addresses")
            if(Constance.DEBUG_MODE) {
                println("#### DEBUG CONFIG:\n" +
                        "Server ID: $serverId\n" +
                        "Listen port: $listenPort\n" +
                        "TimeOut: $timeout\n" +
                        "Listen addresses: $listenAddresses\n" +
                        "Size limit: $sizeLimit\n" +
                        "Allowed IP addresses: $allowedIpAdresses")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            error("Configuration: Content of application.conf file is not valid.")
        }
    }
}