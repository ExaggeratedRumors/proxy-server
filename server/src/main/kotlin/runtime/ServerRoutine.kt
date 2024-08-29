package com.ertools.runtime

import com.ertools.communication.CommunicationThread
import com.ertools.communication.ConnectionListener
import com.ertools.monitor.MonitorThread
import dto.Request
import dto.Response
import com.ertools.ui.ApplicationWindow
import com.ertools.utils.Configuration
import com.ertools.utils.Constance
import com.ertools.utils.ObservableQueue

class ServerRoutine: ConnectionListener {
    /** Connection service **/
    private lateinit var topicList: ArrayList<String>               /** Topics **/
    private lateinit var requestQueue: ObservableQueue<Request>     /*** KKO  ***/
    private lateinit var responseQueue: ObservableQueue<Response>   /*** KKW  ***/

    /** Threads **/
    private lateinit var communicationThread: CommunicationThread
    private lateinit var monitorThread: MonitorThread


    /*************/
    /**   API   **/
    /*************/
    fun start() {
        loadConfiguration()
        buildResources()
        runCommunication()
        runMonitor()
        runUserInterface()
    }

    private fun shutdown() {
        communicationThread.shutdown()
    }

    /*************/
    /** Private **/
    /*************/
    private fun loadConfiguration() {
        Configuration.load()
    }

    private fun buildResources() {
        topicList = ArrayList()
        requestQueue = ObservableQueue()
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
        monitorThread = MonitorThread(
            requestQueue,
            responseQueue
        )
        monitorThread.start()
    }

    private fun runUserInterface() {
        val applicationWindow = ApplicationWindow(::shutdown)
        communicationThread.addListener(applicationWindow)
    }

    private fun sendMessage() {

    }



    /**************************/
    /** Connection listening **/
    /**************************/

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