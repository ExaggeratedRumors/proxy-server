package com.ertools.runtime

import com.ertools.dto.Request
import com.ertools.dto.Response
import com.ertools.utils.Configuration

class MainRoutine {
    private lateinit var topicList: ArrayList<String>
    private lateinit var requestQueue: ArrayList<Request>
    private lateinit var responseQueue: ArrayList<Response>

    fun start() {
        loadConfiguration()
        buildResources()
        runCommunication()
        runMonitor()
        runUserInterface()
    }

    private fun loadConfiguration() {
        Configuration.load()
    }

    private fun buildResources() {
        topicList = ArrayList()
        requestQueue = ArrayList()
        responseQueue = ArrayList()
    }

    private fun runCommunication() {

    }

    private fun runMonitor() {

    }

    private fun runUserInterface() {

    }

}