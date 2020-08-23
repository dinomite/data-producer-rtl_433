package data.producer.rtl_433

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
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
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CompletableFuture

object Rtl433 {
    private val logger = LoggerFactory.getLogger(Rtl433::class.java.name)

    @JvmStatic
    fun main(args: Array<String>) {
        val start = Instant.now()

        val jacksonFuture = CompletableFuture.supplyAsync { jacksonObjectMapper() }

        val server = embeddedServer(Netty, 8080) {
            install(CallLogging) { level = Level.INFO }

            install(Routing) {
                get("/") {
                    call.respond("Foobar")
                }
            }

            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter(jacksonFuture.get()))
            }
        }

        server.start(wait = false)

        logger.info("Startup time: ${Duration.between(start, Instant.now())}")
    }
}
