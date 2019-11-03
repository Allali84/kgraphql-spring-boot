import com.jfrog.bintray.gradle.BintrayExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "2.1.9.RELEASE" apply false
	id("io.spring.dependency-management") version "1.0.8.RELEASE"
	kotlin("jvm") version "1.3.50" apply false
	kotlin("plugin.spring") version "1.3.50" apply false
	id("com.jfrog.bintray") version "1.8.3"
	java
	`maven-publish`
}

allprojects{
	group = "com.github"
	version = "0.0.3"
	repositories {
		mavenCentral()
		jcenter()
	}
}

java {
	sourceCompatibility = JavaVersion.VERSION_11
	targetCompatibility = JavaVersion.VERSION_11

}

subprojects {
	apply(plugin = "io.spring.dependency-management")
	apply(plugin = "kotlin")
	apply(plugin = "org.springframework.boot")
	apply(plugin = "org.jetbrains.kotlin.plugin.spring")
	apply(plugin = "maven-publish")
	apply(plugin = "com.jfrog.bintray")


	dependencyManagement {
		dependencies {
			dependency("com.apurebase:kgraphql:0.8.0")
		}
	}

	if (name != "example") {

		val sourcesJar by tasks.registering(Jar::class) {
			archiveClassifier.set("sources")
			from(sourceSets.main.get().allSource)
		}

		artifacts.add("archives", sourcesJar)

		bintray {
			user = System.getenv("BINTRAY_USER")
			key = System.getenv("BINTRAY_KEY")
			publish = true
			override = true
			setPublications("MyPublication")
			setConfigurations("archives")
			pkg(delegateClosureOf<BintrayExtension.PackageConfig> {
				repo = project.group.toString()
				name = project.name
				vcsUrl = "https://github.com/Allali84/kgraphql-spring-boot.git"
				setLicenses("Apache-2.0")
				version = versionConfig(
						project.version
				)
			})

			publishing {
				publications {
					register("MyPublication", MavenPublication::class) {
						groupId = "com.github"
						from(components["java"])
						artifactId = project.name
						artifact(sourcesJar.get())
						version = version
					}
				}
			}
		}
	}

	tasks.withType<KotlinCompile> {
		kotlinOptions {
			freeCompilerArgs = listOf("-Xjsr305=strict")
			jvmTarget = "11"
		}
	}
}


fun versionConfig(version: Any): BintrayExtension.VersionConfig {
	val versionConfig = BintrayExtension(project).VersionConfig()
	versionConfig.name = version.toString()
	return versionConfig
}
