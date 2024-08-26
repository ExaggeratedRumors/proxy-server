package com.ertools.utils

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

object Configuration {
    lateinit var SERVER_ID: String
        private set
    var LISTEN_PORT: Int = 8080
        private set
    var TIMEOUT: Int = 20
        private set
    var SIZE_LIMIT: Int = 4096
        private set
    lateinit var LISTEN_ADDRESSES: List<String>
        private set
    lateinit var ALLOWED_IP_ADDRESSES: List<String>
        private set

    fun load() {
        try {
            val config: Config = ConfigFactory.load("application.conf")
            val serverConfig = config.getConfig("server")
            SERVER_ID = serverConfig.getString("server_id")
            LISTEN_PORT = serverConfig.getInt("listen_port")
            TIMEOUT = serverConfig.getInt("timeout")
            SIZE_LIMIT = serverConfig.getInt("size_limit")
            LISTEN_ADDRESSES = serverConfig.getStringList("listen_addresses")
            ALLOWED_IP_ADDRESSES = serverConfig.getStringList("allowed_ip_addresses")
            if(Constance.DEBUG_MODE) {
                println("#### DEBUG CONFIG:\n" +
                        "Server ID: $SERVER_ID\n" +
                        "Listen port: $LISTEN_PORT\n" +
                        "TimeOut: $TIMEOUT\n" +
                        "Listen addresses: $LISTEN_ADDRESSES\n" +
                        "Size limit: $SIZE_LIMIT\n" +
                        "Allowed IP addresses: $ALLOWED_IP_ADDRESSES")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            error("Configuration: Content of application.conf file is not valid.")
        }
    }
}