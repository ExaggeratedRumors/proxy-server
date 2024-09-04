package ui

import communication.ClientConnection
import dto.MessagePayload
import utils.ClientUtils
import utils.TimeConverter
import java.awt.*
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.*
import kotlin.io.path.Path
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


        /** Start **/
        val startConnectionIp = JTextField(ClientUtils.DEFAULT_IP)
        panelGbc.gridx = 1
        panelGbc.gridy = 0
        userPanel.add(startConnectionIp, panelGbc)

        val startConnectionPort = JTextField(ClientUtils.DEFAULT_PORT.toString())
        panelGbc.gridx = 2
        panelGbc.gridy = 0
        userPanel.add(startConnectionPort, panelGbc)

        val startConnectionId = JTextField("client${ClientUtils.DEFAULT_PORT}")
        panelGbc.gridx = 3
        panelGbc.gridy = 0
        userPanel.add(startConnectionId, panelGbc)

        val startConnectionButton = JButton("Start")
        startConnectionButton.addActionListener {
            connection.start(startConnectionIp.text, startConnectionPort.text.toInt(), startConnectionId.text)
        }
        panelGbc.gridx = 0
        panelGbc.gridy = 0
        userPanel.add(startConnectionButton, panelGbc)

        /** Check connection **/
        val isConnectedLabel = JLabel("Disconnected")
        panelGbc.gridx = 1
        panelGbc.gridy = 1
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
        panelGbc.gridy = 2
        userPanel.add(getStatusButton, panelGbc)

        /** Server Status **/
        val getServerStatusButton = JButton("Get server status")
        getServerStatusButton.addActionListener {
            connection.getServerStatus { status ->
                notifyMessage("#STATUS:")
                status.forEach {
                    notifyMessage("topic: ${it.key}, producer: ${it.value}")
                }
            }
        }
        panelGbc.gridx = 0
        panelGbc.gridy = 3
        userPanel.add(getServerStatusButton, panelGbc)

        /** Get Server Logs **/
        val getServerLogsButton = JButton("Get server logs")
        getServerLogsButton.addActionListener {
            connection.getServerLogs { info, success ->
                if(success) notifyMessage("SUCCESS: $info")
                else notifyMessage("FAILURE: $info")
            }
        }
        panelGbc.gridx = 0
        panelGbc.gridy = 4
        userPanel.add(getServerLogsButton, panelGbc)

        /** Create Producer **/
        val createProducerTopic = JTextField("Topic")
        panelGbc.gridx = 1
        panelGbc.gridy = 5
        userPanel.add(createProducerTopic, panelGbc)

        val createProducerButton = JButton("Create producer")
        createProducerButton.addActionListener { connection.createProducer(createProducerTopic.text) }
        panelGbc.gridx = 0
        panelGbc.gridy = 5
        userPanel.add(createProducerButton, panelGbc)

        /** Produce **/
        val produceTopic = JTextField("Topic")
        panelGbc.gridx = 1
        panelGbc.gridy = 6
        userPanel.add(produceTopic, panelGbc)

        val produceMessage = JTextField("Message")
        panelGbc.gridx = 2
        panelGbc.gridy = 6
        panelGbc.gridwidth = 2
        userPanel.add(produceMessage, panelGbc)

        val produceButton = JButton("Produce")
        produceButton.addActionListener {
            connection.produce(
                produceTopic.text,
                MessagePayload(TimeConverter().getTimestamp(), produceTopic.text, true, produceMessage.text)
            )
        }
        panelGbc.gridx = 0
        panelGbc.gridy = 6
        panelGbc.gridwidth = 1
        userPanel.add(produceButton, panelGbc)

        /** Send File **/
        val sendFileTopic = JTextField("Topic")
        panelGbc.gridx = 1
        panelGbc.gridy = 7
        userPanel.add(sendFileTopic, panelGbc)

        val sendFileFilename = JTextField("Filename")
        panelGbc.gridx = 2
        panelGbc.gridy = 7
        panelGbc.gridwidth = 2
        userPanel.add(sendFileFilename, panelGbc)

        val sendFileButton = JButton("Send file")
        sendFileButton.addActionListener { connection.sendFile(sendFileTopic.text, Path(sendFileFilename.text)) }
        panelGbc.gridx = 0
        panelGbc.gridy = 7
        panelGbc.gridwidth = 1
        userPanel.add(sendFileButton, panelGbc)

        /** Withdraw Producer **/
        val withdrawProducerTopic = JTextField("Topic")
        panelGbc.gridx = 1
        panelGbc.gridy = 8
        userPanel.add(withdrawProducerTopic, panelGbc)

        val withdrawProducerButton = JButton("Withdraw producer")
        withdrawProducerButton.addActionListener { connection.withdrawProducer(withdrawProducerTopic.text) }
        panelGbc.gridx = 0
        panelGbc.gridy = 8
        userPanel.add(withdrawProducerButton, panelGbc)

        /** Create Subscriber **/
        val createSubscriberTopic = JTextField("Topic")
        panelGbc.gridx = 1
        panelGbc.gridy = 9
        userPanel.add(createSubscriberTopic, panelGbc)

        val createSubscriberButton = JButton("Create subscriber")
        createSubscriberButton.addActionListener {
            connection.createSubscriber(createSubscriberTopic.text) { message ->
                notifyMessage("#CREATE SUBSCRIBER: $message")
            }
        }
        panelGbc.gridx = 0
        panelGbc.gridy = 9
        userPanel.add(createSubscriberButton, panelGbc)

        /** Withdraw Subscriber **/
        val withdrawSubscriberTopic = JTextField("Topic")
        panelGbc.gridx = 1
        panelGbc.gridy = 10
        userPanel.add(withdrawSubscriberTopic, panelGbc)

        val withdrawSubscriberButton = JButton("Withdraw subscriber")
        withdrawSubscriberButton.addActionListener { connection.withdrawProducer(withdrawSubscriberTopic.text) }
        panelGbc.gridx = 0
        panelGbc.gridy = 10
        userPanel.add(withdrawSubscriberButton, panelGbc)

        /** Stop **/
        val stopButton = JButton("Stop")
        stopButton.addActionListener { connection.stopConnection() }
        panelGbc.gridx = 0
        panelGbc.gridy = 11
        panelGbc.anchor = GridBagConstraints.SOUTH
        userPanel.add(stopButton, panelGbc)
    }

}