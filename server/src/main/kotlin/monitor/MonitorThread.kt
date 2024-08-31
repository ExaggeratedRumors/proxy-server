package com.ertools.monitor

import dto.Request
import dto.Response
import com.ertools.utils.Constance
import com.ertools.utils.ObservableQueue
import dto.Message

class MonitorThread(
    private val requestQueue: ObservableQueue<Request>
): Thread() {
    private var isRunning: Boolean = false
    private val listeners: MutableList<MonitorListener> = mutableListOf()

    /** Public API **/

    fun addListener(listener: MonitorListener) {
        listeners.add(listener)
    }


    /** Private **/
    override fun run() {
        isRunning = true
        var request: Request? = null
        while(isRunning) {
            if(requestQueue.isEmpty()) sleep(Constance.MONITOR_THREAD_SLEEP)
            val request = requestQueue.poll()

        }
    }

    private fun validate(message: Message) {

    }

    private fun registerTopic(topic: String) {
        listeners.forEach { it.onRegisterTopic(topic) }
    }

    private fun registserResponse(response: Response) {
        listeners.forEach { it.onRegisterResponse(response) }
    }

}