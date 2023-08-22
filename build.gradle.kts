plugins {
	kotlin("jvm") version "1.9.0"
}

group = "com.alexmherrmann.utils"
version = "1.0-SNAPSHOT"

repositories {
	mavenCentral()
}

dependencies {
	"2.14.2".also { jacksonVersion ->
		implementation("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
		implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
		implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
	}

	testImplementation(kotlin("test"))

	testImplementation(platform("org.junit:junit-bom:5.9.1"))
	testImplementation("org.junit.jupiter:junit-jupiter")
	testImplementation("org.junit.jupiter:junit-jupiter-api")
	testImplementation("org.junit.jupiter:junit-jupiter-params")

	//awaitility
	testImplementation("org.awaitility:awaitility:4.1.0")

	// kotest assertions
	testImplementation("io.kotest:kotest-assertions-core-jvm:5.6.2")
	// kotlin coroutines
	testImplementation("io.kotest:kotest-assertions-core:5.6.2")
	testImplementation("io.kotest:kotest-assertions-json:5.6.2")

	testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
	// kotlin coroutines jvm
	testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk9:1.6.4")

	testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
}

tasks.test {
	useJUnitPlatform()
}

kotlin {
	jvmToolchain(11)
}