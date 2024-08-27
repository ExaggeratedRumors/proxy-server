package ui

import communication.ClientConnection
import utils.Constance
import java.awt.*
import java.awt.event.KeyEvent
import javax.swing.*
import kotlin.concurrent.thread

class ClientWindow(private val connection: ClientConnection) : JFrame() {
    private val connectionLabel: JLabel = JLabel("Disconnected", SwingConstants.CENTER)

    init {
        /** Windows bar **/
        title = "Client"
        defaultCloseOperation = EXIT_ON_CLOSE
        isResizable = false
        setSize(Constance.WINDOW_WIDTH, Constance.WINDOW_HEIGHT)

        /** Layout **/
        layout = GridBagLayout()
        val gbc = GridBagConstraints()
        gbc.fill = GridBagConstraints.BOTH

        /** Input area **/
        val textArea = JTextArea()
        textArea.lineWrap = true
        textArea.wrapStyleWord = true
        textArea.isEditable = false

        textArea.addKeyListener(object : java.awt.event.KeyAdapter() {
            override fun keyTyped(e: KeyEvent) {
                val character = e.keyChar
                thread {
                    val reply = connection.sendAndReceive(character)
                    /*if(reply == null) shutdownClient()
                    else textArea.append(reply.toString())*/
                    textArea.append(reply.toString())
                }
            }
        })

        val scrollPane = JScrollPane(textArea)
        scrollPane.preferredSize = Dimension(Constance.WINDOW_WIDTH, Constance.WINDOW_HEIGHT)
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.weightx = 0.8
        gbc.weighty = 1.0
        gbc.gridheight = 1
        add(scrollPane, gbc)

        /** Right panel **/
        val rightPanel = JPanel()
        rightPanel.border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY), "Connection"
        )
        rightPanel.layout = GridBagLayout()
        gbc.gridx = 1
        gbc.weightx = 0.2
        add(rightPanel, gbc)

        val rightGbc = GridBagConstraints()

        /** Connection label **/
        rightGbc.gridx = 0
        rightGbc.gridy = 0
        rightGbc.gridwidth = GridBagConstraints.REMAINDER
        rightGbc.fill = GridBagConstraints.HORIZONTAL
        rightGbc.insets = Insets(4, 25, 4, 25)
        rightPanel.add(connectionLabel, rightGbc)

        /** Renew connection button **/
        val button = JButton("Renew connection")
        button.addActionListener { renewConnection() }
        rightGbc.gridy = 1
        rightGbc.weighty = 1.0
        rightGbc.anchor = GridBagConstraints.SOUTH
        rightPanel.add(button, rightGbc)

        /** Pack UI **/
        pack()
        setLocationRelativeTo(null)
        isVisible = true
    }

    fun renewConnection() {
        try {
            connection.startConnection()
            connectionLabel.text = "Connected"
        } catch (e: Exception) {
            e.printStackTrace()
            connectionLabel.text = "Disconnected"
        }
    }

    fun shutdownClient() {
        connection.shutdown()
        this.dispose()
    }
}