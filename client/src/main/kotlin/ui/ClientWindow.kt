package ui

import communication.ClientConnection
import java.awt.Dimension
import java.awt.event.KeyEvent
import javax.swing.JFrame
import javax.swing.JScrollPane
import javax.swing.JTextArea
import kotlin.concurrent.thread

class ClientWindow(private val connection: ClientConnection) : JFrame() {
    private val textArea = JTextArea()
    init {
        title = "Client"
        defaultCloseOperation = EXIT_ON_CLOSE

        textArea.lineWrap = true
        textArea.wrapStyleWord = true
        textArea.isEditable = false

        textArea.addKeyListener(object : java.awt.event.KeyAdapter() {
            override fun keyTyped(e: KeyEvent) {
                val character = e.keyChar
                thread {
                    val reply = connection.sendAndReceive(character)
                    if(reply == null) shutdownClient()
                    else textArea.append(reply.toString())
                }
            }
        })

        val scrollPane = JScrollPane(textArea)
        scrollPane.preferredSize = Dimension(400, 300)
        add(scrollPane)
        pack()
        setLocationRelativeTo(null)
        isVisible = true
    }

    fun shutdownClient() {
        this.dispose()
    }
}