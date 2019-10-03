
dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web-services")
	implementation("org.springframework.boot:spring-boot-starter-aop")
	implementation(project(":kgraphql"))
	annotationProcessor("org.springframework.boot:spring-boot-autoconfigure-processor")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
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