package com.ertools.runtime

import com.ertools.communication.CommunicationThread
import com.ertools.communication.ConnectionListener
import com.ertools.monitor.MonitorListener
import com.ertools.monitor.MonitorThread
import com.ertools.ui.ApplicationWindow
import com.ertools.utils.Configuration
import com.ertools.utils.Constance
import com.ertools.utils.ObservableQueue
import dto.Message
import dto.Request
import dto.Response
import dto.Topic

class ServerRoutine: ConnectionListener, MonitorListener {
    /** Connection service **/
    private lateinit var topics: MutableList<Topic>   /** Topics **/
    private lateinit var requestQueue: ObservableQueue<Request>         /*** KKO  ***/
    private lateinit var responseQueue: ObservableQueue<Response>       /*** KKW  ***/

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
        topics = mutableListOf(Topic("logs", Configuration.LISTEN_PORT))
        requestQueue = ObservableQueue()
        responseQueue = ObservableQueue(::serviceResponse)
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
            requestQueue
        )
        monitorThread.addListener(this)
        monitorThread.start()
    }

    private fun runUserInterface() {
        val applicationWindow = ApplicationWindow(::shutdown)
        communicationThread.addListener(applicationWindow)
    }

    private fun serviceResponse(response: Response) {
        communicationThread.send(response)
        responseQueue.remove(response)
    }


    /**************************/
    /** Connection listening **/
    /**************************/

    override fun onClientAccept(port: Int, ip: String) {
        if(Constance.DEBUG_MODE) println("ENGINE: $port ($ip) joined to server.")
    }

    override fun onClientDisconnect(port: Int) {
        topics.forEach { it.subscribers.remove(port) }
        if(Constance.DEBUG_MODE) println("ENGINE: $port has been disconnected.")
    }

    override fun onServerBusy(port: Int) {
        if(Constance.DEBUG_MODE) println("ENGINE: $port was rejected: server is busy.")
    }

    override fun onMessageReceive(request: Request) {
        requestQueue.add(request)
        if(Constance.DEBUG_MODE) println("ENGINE: Received from ${request.clientPort}: ${request.serializedMessage}")
    }

    override fun onMessageSend(response: Response) {
        responseQueue.add(response)
        if(Constance.DEBUG_MODE) println("ENGINE: Reply to ${response.receivers}: ${response.message}")
    }

    /**************************/
    /*** Monitor listening  ***/
    /**************************/

    override fun onReply(port: Int, message: Message) {
        responseQueue.add(Response(
            message = message,
            receivers = listOf(port)
        ))
    }

    override fun onPublish(topicName: String, message: Message): Boolean {
        val topic = topics.firstOrNull { it.topicName == topicName }
        if(topic == null) return false
        responseQueue.add(Response(
            message = message,
            receivers = topic.subscribers
        ))
        return true
    }

    override fun onRegisterTopic(producerPort: Int, topicName: String): Boolean {
        if(topics.firstOrNull { it.topicName == topicName } != null) return false
        val newTopic = Topic(
            topicName = topicName,
            producer = producerPort
        )
        topics.add(newTopic)
        return true
    }

    override fun onWithdrawTopic(producerPort: Int, topicName: String): Boolean {
        val topic = topics.firstOrNull { it.topicName == topicName }
        if(topic == null) return false
        if(topic.producer != producerPort) return false
        topics.remove(topic)
        return true
    }

    override fun onSubscription(port: Int, topicName: String): Boolean {
        val topic = topics.firstOrNull { it.topicName == topicName }
        if(topic == null) return false
        topic.subscribers.add(port)
        return true
    }

    override fun onUnsubscription(port: Int, topicName: String): Boolean {
        val topic = topics.firstOrNull { it.topicName == topicName }
        if(topic == null) return false
        return topic.subscribers.remove(port)
    }
}