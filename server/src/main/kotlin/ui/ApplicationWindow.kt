package com.ertools.ui

import com.ertools.communication.ConnectionListener
import dto.ClientInfo
import dto.Request
import dto.Response
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.KeyEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JFrame
import javax.swing.JScrollPane
import javax.swing.JTextArea
import kotlin.system.exitProcess

class ApplicationWindow(
    private val shutdownCallback: () -> Unit
): JFrame(), ConnectionListener {

    private val height = 320
    private val width = 480
    private val connectionArea = JTextArea("Wait for clients...")
    private val messageArea = JTextArea("Server started. Press Q to shut down.")
    private val scrollPane1 = JScrollPane(messageArea)
    private val clients : ArrayList<ClientInfo> = ArrayList()
    private var clientsCounter = 0

    init {
        /** UI Layout **/
        title = "Server"
        defaultCloseOperation = EXIT_ON_CLOSE
        layout = BorderLayout()
        setSize(width, height)
        isResizable = false

        messageArea.lineWrap = true
        messageArea.wrapStyleWord = true
        messageArea.isEditable = false
        scrollPane1.preferredSize = Dimension((width * 0.8).toInt(), height)
        add(scrollPane1, BorderLayout.WEST)

        connectionArea.lineWrap = true
        connectionArea.wrapStyleWord = true
        connectionArea.isEditable = false
        connectionArea.margin = java.awt.Insets((height * 0.1).toInt(), 0, 0, 0)
        val scrollPane2 = JScrollPane(connectionArea)
        scrollPane2.preferredSize = Dimension((width * 0.2).toInt(), height)
        add(scrollPane2, BorderLayout.EAST)

        pack()
        setLocationRelativeTo(null)
        isVisible = true

        /** UI listeners **/
        addKeyListener(object : java.awt.event.KeyAdapter() {
            override fun keyTyped(e: KeyEvent) {
                val character = e.keyChar
                if(character == 'q') exitProcess(1)
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

    private fun revalidateClientsInfo() {
        connectionArea.text = ""
        clients.forEach {
            connectionArea.text = connectionArea.text.plus("#${it.id} ${it.ip}:${it.port}\n")
        }
    }

    private fun notifyMessage(message: String) {
        messageArea.append("\n$message")
        val scrollBar = scrollPane1.verticalScrollBar
        scrollBar.value = scrollBar.maximum
    }


    /** ServerListener methods **/
    override fun onClientAccept(port: Int, ip: String) {
        clients.add(ClientInfo(clientsCounter, port, ip))

        notifyMessage("#$clientsCounter joined to server.")
        clientsCounter += 1
        revalidateClientsInfo()
    }

    override fun onClientDisconnect(port: Int) {
        val client = clients.firstOrNull { it.port == port }
        if(client == null) return

        notifyMessage("#${client.id} has been disconnected.")
        clients.removeIf { it.port == port }
        revalidateClientsInfo()
    }

    override fun onMessageReceive(request: Request) {
        val client = clients.firstOrNull { it.port == request.clientPort }
        if(client == null) return
        client.messages.add(request.serializedMessage)

        notifyMessage("#${client.id} Received $size ${request.serializedMessage}")
    }

    override fun onMessageSend(response: Response) {
        notifyMessage("Reply to ${response.receivers}: ${response.message}")
    }
}