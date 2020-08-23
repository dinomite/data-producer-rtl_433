package data.producer.rtl_433

import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.jackson.JacksonConverter
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.io.BufferedReader
import java.io.File
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CompletableFuture

object Rtl433 {
    private val logger = LoggerFactory.getLogger(Rtl433::class.java.name)

    lateinit var objectMapper: ObjectMapper

    private val nodeData = mutableMapOf<String, Sensor>()

    @JvmStatic
    fun main(args: Array<String>) {
        val start = Instant.now()

        val jacksonFuture = CompletableFuture.supplyAsync {
            jacksonObjectMapper()
                    .registerModule(JavaTimeModule())
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }

        val server = embeddedServer(Netty, 8080) {
            install(CallLogging) { level = Level.INFO }

            install(Routing) {
                get("/") {
                    call.respond(mapOf(EmonNode("environment") to nodeData))
                }
            }

            install(ContentNegotiation) {
                objectMapper = jacksonFuture.get()
                register(ContentType.Application.Json, JacksonConverter(objectMapper))
            }
        }

        server.start(wait = false)

        logger.info("Startup time: ${Duration.between(start, Instant.now())}")

        val input = File("/Users/dinomite/code/mine/data-producer-rtl_433").runCommand("bin/fake-rtl.sh")
        input.forEachLine { line ->
            objectMapper.readValue<RtlData>(line)
                    .toSensors()
                    .forEach {
                        val foo = nodeData.getOrDefault(it.name(), it)
                        nodeData[it.name()] = foo
                    }
            println(nodeData)
        }
    }
}

fun File.runCommand(command: String): BufferedReader {
    return ProcessBuilder(*command.split("\\s".toRegex()).toTypedArray())
            .directory(this)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()
            .inputStream
            .bufferedReader()
}

data class RtlData(
        @JsonProperty("brand") val brand: String?,
        @JsonProperty("model") val model: String,
        @JsonProperty("id") val id: Int,
        @JsonProperty("battery_ok") val batteryOk: Int,
        @JsonProperty("temperature_C") val temperatureC: Double?,
        @JsonProperty("humidity") val humidity: Int?
) {
    private fun sensorKey(subSensor: String) = if (brand != null) {
        "$brand-$model-$id-$subSensor"
    } else {
        "$model-$id-$subSensor"
    }

    fun toSensors(): List<Sensor> {
        val sensors = mutableListOf<Sensor>()
        if (temperatureC != null) {
            sensors.add(DoubleSensor(sensorKey("temperature"), temperatureC))
        }
        if (humidity != null) {
            sensors.add(IntSensor(sensorKey("humidity"), humidity))
        }
        return sensors
    }
}


data class EmonNode @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(@JsonValue val value: String)
data class NodeData @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(@JsonValue val sensors: List<Sensor>)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
        JsonSubTypes.Type(IntSensor::class, name = "IntSensor"),
        JsonSubTypes.Type(DoubleSensor::class, name = "DoubleSensor")
)
interface Sensor {
    fun stringValue(): String
    fun name(): String
}

data class IntSensor(val name: String, private val value: Int) : Sensor {
    override fun name(): String = name

    override fun stringValue(): String = "$value"

}

data class DoubleSensor(val name: String, private val value: Double) : Sensor {
    override fun name(): String = name

    override fun stringValue(): String = "$value"
}
