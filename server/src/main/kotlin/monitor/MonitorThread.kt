package com.ertools.monitor

import dto.Request
import dto.Response
import com.ertools.utils.Constance
import com.ertools.utils.ObservableQueue

class MonitorThread(
    private val requestQueue: ObservableQueue<Request>,
    private val responseQueue: ObservableQueue<Response>
): Thread() {
    private var isRunning: Boolean = false

    override fun run() {
        isRunning = true
        while(isRunning) {
            if(requestQueue.isEmpty()) sleep(Constance.MONITOR_THREAD_SLEEP)

        }
    }

}