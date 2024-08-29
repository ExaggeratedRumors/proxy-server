package ui

import communication.ClientConnection
import dto.MessagePayload
import utils.ClientUtils
import java.awt.*
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import javax.swing.*
import kotlin.system.exitProcess

class ClientWindow(private val connection: ClientConnection) : JFrame() {
    private val messageArea = JTextArea("Client started.")
    private val scrollPane = JScrollPane(messageArea)

    init {
        /** Windows bar **/
        title = "Client"
        defaultCloseOperation = EXIT_ON_CLOSE
        isResizable = false
        setSize(ClientUtils.WINDOW_WIDTH, ClientUtils.WINDOW_HEIGHT)

        /** Layout **/
        layout = GridBagLayout()
        val gbc = GridBagConstraints()
        gbc.fill = GridBagConstraints.BOTH

        /** UI **/
        buildTextArea(gbc)
        buildUserPanel(gbc)
        addWindowListeners()
        pack()
        setLocationRelativeTo(null)
        isVisible = true
    }


    /*************/
    /** Private **/
    /*************/

    private fun addWindowListeners() {
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                connection.stopConnection()
                dispose()
                exitProcess(0)
            }
        })
    }

    private fun notifyMessage(message: String) {
        messageArea.append("\n$message")
        val scrollBar = scrollPane.verticalScrollBar
        scrollBar.value = scrollBar.maximum
    }


    private fun buildTextArea(gbc: GridBagConstraints) {
        messageArea.lineWrap = true
        messageArea.wrapStyleWord = true
        messageArea.isEditable = false

        val scrollPane = JScrollPane(messageArea)
        scrollPane.preferredSize = Dimension(ClientUtils.WINDOW_WIDTH, ClientUtils.WINDOW_HEIGHT)
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.weightx = 0.8
        gbc.weighty = 1.0
        gbc.gridheight = 1
        add(scrollPane, gbc)



        /*        textArea.addKeyListener(object : java.awt.event.KeyAdapter() {
            override fun keyTyped(e: KeyEvent) {
                val character = e.keyChar
                thread {
                    val reply = connection.sendAndReceive(character)
                    *//*if(reply == null) shutdownClient()
                    else textArea.append(reply.toString())*//*
                    textArea.append(reply.toString())
                }
            }
        })*/
    }

    private fun buildUserPanel(gbc: GridBagConstraints) {
        /** Panel **/
        val userPanel = JPanel()
        userPanel.border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY), "Manage connection"
        )
        userPanel.layout = GridBagLayout()
        gbc.gridx = 1
        gbc.weightx = 0.2
        add(userPanel, gbc)

        /** Grid bag constraints **/
        val panelGbc = GridBagConstraints()
        panelGbc.gridwidth = GridBagConstraints.REMAINDER
        panelGbc.fill = GridBagConstraints.HORIZONTAL
        panelGbc.insets = Insets(4, 4, 4, 4)
        panelGbc.gridx = 0
        panelGbc.weightx = 0.25
        panelGbc.weighty = 0.25
        panelGbc.gridheight = 1
        panelGbc.gridwidth = 1

        /** Connection **/
        val isConnectedLabel = JLabel("Disconnected")
        panelGbc.gridx = 1
        panelGbc.gridy = 0
        userPanel.add(isConnectedLabel, panelGbc)

        val isConnectedButton = JButton("Check connection")
        isConnectedButton.addActionListener {
            if(connection.isConnected()) isConnectedLabel.text = "Connected"
            else isConnectedLabel.text = "Disconnected"
        }
        panelGbc.gridx = 0
        userPanel.add(isConnectedButton, panelGbc)

        /** Status **/
        val getStatusButton = JButton("Get status")
        getStatusButton.addActionListener { notifyMessage(connection.getStatus()) }
        panelGbc.gridx = 0
        panelGbc.gridy = 1
        userPanel.add(getStatusButton, panelGbc)

        /** Server Status **/
        val getServerStatusButton = JButton("Get server status")
        getServerStatusButton.addActionListener { connection.getServerStatus { Unit } }
        panelGbc.gridx = 0
        panelGbc.gridy = 2
        userPanel.add(getServerStatusButton, panelGbc)

        /** Get Server Logs **/
        val getServerLogsButton = JButton("Get server logs")
        getServerLogsButton.addActionListener { connection.getServerLogs { Unit } }
        panelGbc.gridx = 0
        panelGbc.gridy = 3
        userPanel.add(getServerLogsButton, panelGbc)

        /** Create Producer **/
        val createProducerTopic = JTextField("Topic")
        panelGbc.gridx = 1
        panelGbc.gridy = 4
        userPanel.add(createProducerTopic, panelGbc)

        val createProducerButton = JButton("Create producer")
        createProducerButton.addActionListener { connection.createProducer(createProducerTopic.text) }
        panelGbc.gridx = 0
        panelGbc.gridy = 4
        userPanel.add(createProducerButton, panelGbc)

        /** Produce **/
        val produceTopic = JTextField("Topic")
        panelGbc.gridx = 1
        panelGbc.gridy = 5
        userPanel.add(produceTopic, panelGbc)

        val produceButton = JButton("Produce")
        produceButton.addActionListener { connection.produce(produceTopic.text, MessagePayload("", "", true, "")) }
        panelGbc.gridx = 0
        panelGbc.gridy = 5
        userPanel.add(produceButton, panelGbc)

        /** Send File **/
        val sendFileTopic = JTextField("Topic")
        panelGbc.gridx = 1
        panelGbc.gridy = 6
        userPanel.add(sendFileTopic, panelGbc)

        val sendFileFilename = JTextField("Filename")
        panelGbc.gridx = 2
        panelGbc.gridy = 6
        userPanel.add(sendFileFilename, panelGbc)

        val sendFileButton = JButton("Send file")
        sendFileButton.addActionListener { connection.sendFile(sendFileTopic.text, File(sendFileFilename.text)) }
        panelGbc.gridx = 0
        panelGbc.gridy = 6
        userPanel.add(sendFileButton, panelGbc)

        /** Withdraw Producer **/
        val withdrawProducerTopic = JTextField("Topic")
        panelGbc.gridx = 1
        panelGbc.gridy = 7
        userPanel.add(withdrawProducerTopic, panelGbc)

        val withdrawProducerButton = JButton("Withdraw producer")
        withdrawProducerButton.addActionListener { connection.withdrawProducer(withdrawProducerTopic.text) }
        panelGbc.gridx = 0
        panelGbc.gridy = 7
        userPanel.add(withdrawProducerButton, panelGbc)

        /** Create Subscriber **/
        val createSubscriberTopic = JTextField("Topic")
        panelGbc.gridx = 1
        panelGbc.gridy = 8
        userPanel.add(createSubscriberTopic, panelGbc)

        val createSubscriberButton = JButton("Create subscriber")
        createSubscriberButton.addActionListener { connection.createSubscriber(createSubscriberTopic.text) { Unit } }
        panelGbc.gridx = 0
        panelGbc.gridy = 8
        userPanel.add(createSubscriberButton, panelGbc)

        /** Withdraw Subscriber **/
        val withdrawSubscriberTopic = JTextField("Topic")
        panelGbc.gridx = 1
        panelGbc.gridy = 9
        userPanel.add(withdrawSubscriberTopic, panelGbc)

        val withdrawSubscriberButton = JButton("Withdraw subscriber")
        withdrawSubscriberButton.addActionListener { connection.withdrawProducer(withdrawSubscriberTopic.text) }
        panelGbc.gridx = 0
        panelGbc.gridy = 9
        userPanel.add(withdrawSubscriberButton, panelGbc)

        /** Stop **/
        val stopButton = JButton("Stop")
        stopButton.addActionListener { connection.stopConnection() }
        panelGbc.gridx = 0
        panelGbc.gridy = 10
        panelGbc.anchor = GridBagConstraints.SOUTH
        userPanel.add(stopButton, panelGbc)
    }

}