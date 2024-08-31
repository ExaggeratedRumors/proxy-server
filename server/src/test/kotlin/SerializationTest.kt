import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dto.*
import dto.MessageType
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class SerializationTest {
    @Test
    fun testMessageSerialization() {

        val currentTime = Instant.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .withZone(ZoneOffset.UTC)
        val formattedTime = formatter.format(currentTime)


        val message = Message(
            type = MessageType.Message,
            id = "justAnObviousTest",
            topic = "testTopic",
            mode = MessageMode.Producer,
            timestamp = "2024-02-15T00:00:00.000Z",
            payload = MessagePayload(
                timestampOfMessage = formattedTime,
                topicOfMessage = "testTopic",
                success = false,
                message = "just a message"
            )
        )

        val serializedMessage = ObjectMapper().writeValueAsString(message)
        println("Serialized message:\n $serializedMessage")

        val deserializedMessage = jacksonObjectMapper().readValue(serializedMessage, Message::class.java)
        println("Deserialized message:\n $deserializedMessage")
    }
}