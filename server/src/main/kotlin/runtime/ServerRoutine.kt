package com.ertools.runtime

import com.ertools.communication.CommunicationThread
import com.ertools.communication.ConnectionListener
import com.ertools.dto.Request
import com.ertools.dto.Response
import com.ertools.ui.ApplicationWindow
import com.ertools.utils.Configuration
import com.ertools.utils.Constance
import com.ertools.utils.ObservableQueue

class ServerRoutine: ConnectionListener {
    private lateinit var topicList: ArrayList<String>
    private lateinit var requestQueue: ObservableQueue<Request>
    private lateinit var responseQueue: ObservableQueue<Response>

    private lateinit var communicationThread: CommunicationThread

    /** API **/
    fun start() {
        loadConfiguration()
        buildResources()
        runCommunication()
        runMonitor()
        runUserInterface()
    }

    fun shutdown() {
        communicationThread.shutdown()
    }

    /** Private **/
    private fun loadConfiguration() {
        Configuration.load()
    }

    private fun buildResources() {
        topicList = ArrayList()
        requestQueue = ObservableQueue(::sendMessage)
        responseQueue = ObservableQueue(::sendMessage)
    }

    private fun runCommunication() {
        communicationThread = CommunicationThread(
            Configuration.LISTEN_PORT,
            Configuration.LISTEN_ADDRESSES,
            Configuration.ALLOWED_IP_ADDRESSES
        )
        communicationThread.addListener(this)
        communicationThread.start()
    }

    private fun runMonitor() {

    }

    private fun runUserInterface() {
        val applicationWindow = ApplicationWindow(::shutdown)
        communicationThread.addListener(applicationWindow)
    }

    private fun sendMessage() {

    }


    /** Connection listening **/

    override fun onClientAccept(port: Int, ip: String) {
        if(Constance.DEBUG_MODE) println("ENGINE: $port ($ip) joined to server.")
    }

    override fun onClientDisconnect(port: Int) {
        if(Constance.DEBUG_MODE) println("ENGINE: $port has been disconnected.")
    }

    override fun onServerBusy(port: Int) {
        if(Constance.DEBUG_MODE) println("ENGINE: $port was rejected: server is busy.")
    }

    override fun onMessageReceive(request: Request) {
        requestQueue.add(request)
        if(Constance.DEBUG_MODE) println("ENGINE: Received from ${request.clientPort}: ${request.message}")
    }

    override fun onMessageSend(response: Response) {
        responseQueue.add(response)
        if(Constance.DEBUG_MODE) println("ENGINE: Reply to ${response.receivers}: ${response.message}")
    }

}