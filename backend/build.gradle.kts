plugins {
    java
    id("org.springframework.boot") version "3.4.1"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "dev.neuralforge"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot - Core (NO WEB!)
    implementation("org.springframework.boot:spring-boot-starter")
    
    // JSON Processing for IPC
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
    
    // ONNX Runtime for AI model inference
    implementation("com.microsoft.onnxruntime:onnxruntime:1.19.2")
    
    // Logging
    implementation("ch.qos.logback:logback-classic")
    
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

// Create executable JAR
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("neuralforge-backend.jar")
    mainClass.set("dev.neuralforge.Application")
}

// Task to print classpath for testing
tasks.register("printClasspath") {
    doLast {
        println(configurations.runtimeClasspath.get().asPath)
    }
}
