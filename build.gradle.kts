import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.72"
    application
    java
    id("dev.jacomet.logging-capabilities") version "0.9.0"
}

loggingCapabilities {
    enforceLogback()
}

apply(plugin = "org.jetbrains.kotlin.jvm")

repositories {
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    implementation("io.ktor", "ktor-server-core", "1.4.0")
    implementation("io.ktor", "ktor-server-netty", "1.4.0")
    implementation("io.ktor", "ktor-jackson", "1.4.0")

    runtimeOnly("ch.qos.logback", "logback-classic", "1.2.3")

    implementation("com.fasterxml.jackson.core", "jackson-core", "2.11.2")
    implementation("com.fasterxml.jackson.core", "jackson-databind", "2.11.2")
    implementation("com.fasterxml.jackson.datatype", "jackson-datatype-jsr310", "2.11.2")
    implementation("com.fasterxml.jackson.module", "jackson-module-kotlin", "2.11.2")

    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", "5.6.2")
    testImplementation("org.junit.jupiter", "junit-jupiter-api", "5.6.2")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

application {
    mainClassName = "data.producer.rtl_433.AppKt"
}
