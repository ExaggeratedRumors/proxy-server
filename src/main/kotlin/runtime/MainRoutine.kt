package com.ertools.runtime

import com.ertools.communication.CommunicationThread
import com.ertools.communication.ConnectionListener
import com.ertools.dto.Request
import com.ertools.dto.Response
import com.ertools.ui.ApplicationWindow
import com.ertools.utils.Configuration
import com.ertools.utils.ObservableQueue

class MainRoutine: ConnectionListener {
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
        TODO("Not yet implemented")
    }

    override fun onClientDisconnect(port: Int) {
        TODO("Not yet implemented")
    }

    override fun onServerBusy(port: Int) {
        TODO("Not yet implemented")
    }

    override fun onMessageReceive(request: Request) {
        requestQueue.add(request)
    }

    override fun onMessageSend(response: Response) {
        responseQueue.add(response)
    }

}