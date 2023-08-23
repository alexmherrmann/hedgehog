plugins {
    kotlin("jvm") version "1.9.0"
}

group = "com.alexmherrmann.utils"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    // Bring in the aws sdk v2 for sqs
    implementation(platform("software.amazon.awssdk:bom:2.20.125"))
    implementation("software.amazon.awssdk:sqs")


    // Bring in testcontainers with localstack
    testImplementation(platform("org.testcontainers:testcontainers-bom:1.18.3"))
    testImplementation("org.testcontainers:localstack")


    testImplementation(kotlin("test"))

    implementation(rootProject)
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(11)
}