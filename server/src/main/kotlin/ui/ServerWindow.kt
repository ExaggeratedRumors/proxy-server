package com.ertools.ui

import dto.Configuration
import dto.Topic
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.KeyEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JFrame
import javax.swing.JScrollPane
import javax.swing.JTextArea
import kotlin.system.exitProcess

class ServerWindow(
    private val shutdownCallback: () -> Unit
): JFrame(), ServerOutput {

    private val height = 320
    private val width = 480
    private val statusArea = JTextArea("Wait for clients...")
    private val logArea = JTextArea("Server started. Press Q to shut down.")
    private val logScroll = JScrollPane(logArea)

    init {
        /** UI Layout **/
        title = Configuration.SERVER_ID
        defaultCloseOperation = EXIT_ON_CLOSE
        layout = BorderLayout()
        setSize(width, height)
        isResizable = false

        /** Log area **/
        logArea.lineWrap = true
        logArea.wrapStyleWord = true
        logArea.isEditable = false
        logScroll.preferredSize = Dimension((width * 0.8).toInt(), height)
        add(logScroll, BorderLayout.WEST)

        /** Status area **/
        statusArea.lineWrap = true
        statusArea.wrapStyleWord = true
        statusArea.isEditable = false
        statusArea.margin = java.awt.Insets((height * 0.1).toInt(), 0, 0, 0)
        val statusScroll = JScrollPane(statusArea)
        statusScroll.preferredSize = Dimension((width * 0.2).toInt(), height)
        add(statusScroll, BorderLayout.EAST)

        pack()
        setLocationRelativeTo(null)
        isVisible = true

        /** UI listeners **/
        addKeyListener(object : java.awt.event.KeyAdapter() {
            override fun keyTyped(e: KeyEvent) {
                val character = e.keyChar
                if(character != 'q') return
                shutdownCallback.invoke()
                dispose()
                exitProcess(0)
            }
        })

        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                shutdownCallback.invoke()
                dispose()
                exitProcess(0)
            }
        })
    }

    /**********************************/
    /** Server output implementation **/
    /**********************************/

    override fun updateLog(message: String) {
        logArea.append("\n$message")
        val scrollBar = logScroll.verticalScrollBar
        scrollBar.value = scrollBar.maximum
    }

    override fun updateStatus(status: List<Topic>) {
        statusArea.text = ""
        status.forEach {
            statusArea.text = statusArea.text.plus("${it.topicName} [${it.producerId}]: ${it.subscribers}\n")
        }
    }
}