package com.ertools.runtime

import com.ertools.communication.CommunicationThread
import com.ertools.communication.ConnectionListener
import com.ertools.monitor.MessageManager
import com.ertools.monitor.MonitorThread
import com.ertools.ui.ServerOutput
import com.ertools.ui.ServerWindow
import com.ertools.utils.Constance
import dto.*
import utils.ObservableQueue

class ServerRoutine: ConnectionListener, MessageManager {
    /** Connection service **/
    private lateinit var topics: MutableList<Topic>   /** Topics **/
    private lateinit var requestQueue: ObservableQueue<Request>

    /*** KKO  ***/
    private lateinit var responseQueue: ObservableQueue<Response>
    /*** KKW  ***/

    /** Threads **/
    private lateinit var communicationThread: CommunicationThread
    private lateinit var monitorThread: MonitorThread
    private lateinit var serverOutput: ServerOutput

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
        topics = mutableListOf(Topic("logs", Configuration.SERVER_ID, null))
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
            requestQueue,
            this
        )
        monitorThread.start()
    }

    private fun runUserInterface() {
        val applicationWindow = ServerWindow(::shutdown)
        serverOutput = applicationWindow
    }

    private fun serviceResponse(response: Response) {
        communicationThread.send(response)
        responseQueue.remove(response)
    }


    /**************************/
    /** Connection listening **/
    /**************************/

    override fun onClientAccept(port: Int, ip: String) {
        serverOutput.updateLog("#ACCEPT: $ip:$port")
        if(Constance.DEBUG_MODE) println("ENGINE: $port ($ip) joined to server.")
    }

    override fun onClientDisconnect(port: Int) {
        topics.forEach { it.subscribers.removeIf { client-> client.port == port} }
        serverOutput.updateLog("#DISCONNECT: $port)")
        serverOutput.updateStatus(topics)
        if(Constance.DEBUG_MODE) println("ENGINE: $port has been disconnected.")
    }

    override fun onMessageReceive(request: Request) {
        requestQueue.add(request)
        serverOutput.updateLog("#RECEIVED: from ${request.client.port}")
        if(Constance.DEBUG_MODE) println("ENGINE: Received from ${request.client.port}")
    }

    override fun onMessageSend(response: Response) {
        serverOutput.updateLog("#SEND: to ${response.receivers}")
        if(Constance.DEBUG_MODE) println("ENGINE: Reply to ${response.receivers}: ${response.message}")
    }

    /**************************/
    /*** Monitor listening  ***/
    /**************************/

    override fun onReply(client: ClientInfo, message: Message) {
        responseQueue.add(Response(
            message = message,
            receivers = listOf(client)
        ))
        serverOutput.updateLog("#QUEUE REPLY: to ${client.port}")
    }

    override fun onPublish(topicName: String, message: Message): Int {
        val topic = topics.firstOrNull { it.topicName == topicName }
        if(topic == null) throw IllegalStateException("No topic $topicName")
        val receivers = topic.subscribers
        if(receivers.size == 0) return 0
        responseQueue.add(Response(
            message = message,
            receivers = receivers
        ))
        serverOutput.updateLog("#QUEUE PUBLISH: to $receivers")
        return receivers.size
    }

    override fun onRegisterTopic(producer: ClientInfo, topicName: String, producerId: String): Boolean {
        if(topics.firstOrNull { it.topicName == topicName } != null) return false
        val newTopic = Topic(
            topicName = topicName,
            producer = producer,
            producerId = producerId
        )
        topics.add(newTopic)
        serverOutput.updateLog("#REGISTER: topic $topicName [$producerId: ${producer.port}]")
        serverOutput.updateStatus(topics)
        return true
    }

    override fun onWithdrawTopic(producer: ClientInfo, topicName: String): Boolean {
        val topic = topics.firstOrNull { it.topicName == topicName }
        if(topic == null) return false
        if(topic.producer != producer) return false
        topics.remove(topic)
        serverOutput.updateLog("#WITHDRAW: topic $topicName")
        serverOutput.updateStatus(topics)
        return true
    }

    override fun onSubscription(client: ClientInfo, topicName: String): Boolean {
        val topic = topics.firstOrNull { it.topicName == topicName }
        if(topic == null) return false
        topic.subscribers.add(client)
        serverOutput.updateLog("#REGISTER: subscription to $topicName [${client.port}]")
        serverOutput.updateStatus(topics)
        return true
    }

    override fun onUnsubscription(client: ClientInfo, topicName: String): Boolean {
        val topic = topics.firstOrNull { it.topicName == topicName }
        if(topic == null) return false
        if(!topic.subscribers.remove(client)) return false
        serverOutput.updateLog("#WITHDRAW: subscription $topicName [${client.port}]")
        serverOutput.updateStatus(topics)
        return true
    }

    override fun onStatusRequest(client: ClientInfo): Map<String, String> {
        serverOutput.updateLog("#STATUS: given [${client.port}]")
        return topics.associate { it.topicName to it.producerId }
    }

    override fun onConfigRequest(client: ClientInfo): Configuration {
        serverOutput.updateLog("#CONFIG: given [${client.port}]")
        return Configuration
    }
}