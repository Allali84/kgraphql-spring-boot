
dependencies {
	implementation(project(":kgraphql-spring-boot-autoconfigure"))
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
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
