import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "2.7.4"
	id("io.spring.dependency-management") version "1.0.14.RELEASE"
	kotlin("jvm") version "1.6.21"
	kotlin("plugin.spring") version "1.6.21"
}

group = "de.johannes"
version = "0.1.0"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
	mavenCentral()
}

dependencies {
	implementation("com.amazonaws:aws-java-sdk:1.12.309")
	implementation("javax.xml.bind:jaxb-api:2.4.0-b180830.0359")
	implementation("com.jcraft:jsch:0.1.55")
	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "17"
	}
}

tasks.register<Copy>("copyResource") {
	from(layout.projectDirectory.dir("resources"))
	include("**")
	into (layout.buildDirectory.dir("libs/resources"))
}

tasks.build {
	dependsOn("copyResource")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
