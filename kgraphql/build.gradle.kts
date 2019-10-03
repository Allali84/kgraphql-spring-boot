
dependencies {

	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.0.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.9.7")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.7")

	implementation("com.github.ben-manes.caffeine:caffeine:1.0.0")

	testImplementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.6.3")
	testImplementation("io.netty:netty-all:4.1.9.Final")

	testImplementation("junit:junit:4.12")
	testImplementation("org.hamcrest:hamcrest-all:1.3")
	testImplementation("org.amshove.kluent:kluent:1.53")
}

tasks.bootRun {
	enabled = false
}
tasks.bootJar {
	enabled = false
}
tasks.jar {
	enabled = true
}