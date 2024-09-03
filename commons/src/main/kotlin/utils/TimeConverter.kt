package utils

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class TimeConverter {
    fun getTimestamp(): String {
        val currentTime = Instant.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .withZone(ZoneOffset.UTC)
        val formattedTime = formatter.format(currentTime)
        return formattedTime
    }

    fun getTimestampDiffMs(timestamp: String, timestampOfMessage: String): Long {
        val formatter = DateTimeFormatter.ISO_INSTANT
        val time = Instant.from(formatter.parse(timestamp))
        val timeOfMessage = Instant.from(formatter.parse(timestampOfMessage))
        return time.toEpochMilli() - timeOfMessage.toEpochMilli()
    }

}