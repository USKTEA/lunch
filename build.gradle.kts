plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.spring") version "2.0.21"
    kotlin("plugin.jpa") version "2.0.21"

    id("org.springframework.boot") version "3.3.5"
    id("io.spring.dependency-management") version "1.1.6"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1"
    id("com.github.node-gradle.node") version "7.1.0"
    id("com.google.cloud.tools.jib") version "3.4.0"
}

group = "com.usktea"
version = "0.0.1-SNAPSHOT"
description = "Lunch project for Spring Boot"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

extra["springCloudVersion"] = "2023.0.3"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.11.0")
    implementation("org.postgresql:postgresql")
    implementation("org.hibernate.orm:hibernate-spatial")
    implementation("org.locationtech.jts:jts-core:1.19.0")

    implementation("com.nimbusds:nimbus-jose-jwt:10.5")
    implementation("com.uber:h3:4.3.1")
    implementation("net.javacrumbs.shedlock:shedlock-spring:6.10.0")

    implementation("software.amazon.awssdk:s3:2.37.5")
    implementation("software.amazon.awssdk:cloudfront:2.38.7")

    implementation("com.linecorp.kotlin-jdsl:jpql-dsl:3.6.1")
    implementation("com.linecorp.kotlin-jdsl:jpql-render:3.6.1")
    implementation("com.linecorp.kotlin-jdsl:spring-data-jpa-support:3.6.1")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("com.h2database:h2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

node {
    download = true
    version = "20.18.0"
    npmVersion = "10.8.2"
    workDir.set(file("${project.projectDir}/web"))
    npmWorkDir.set(file("${project.projectDir}/web"))
    nodeProjectDir.set(file("${project.projectDir}/web"))
}

tasks.register<com.github.gradle.node.npm.task.NpmTask>("buildReact") {
    group = "web"
    description = "Build the React application"

    dependsOn(tasks.npmInstall)
    npmCommand.set(listOf("run", "build"))
}

tasks.register<Copy>("copyReactBuild") {
    group = "web"
    description = "Copy React build output to Spring Boot static resources folder"

    dependsOn("buildReact")
    from("${project.projectDir}/web/build")
    into("${project.projectDir}/src/main/resources/static")
}

jib {
    from {
        image = "eclipse-temurin:21-jre"
        platforms {
            platform {
                architecture = "amd64"
                os = "linux"
            }
            platform {
                architecture = "arm64"
                os = "linux"
            }
        }
    }
    to {
        image = "suktaekim/lunch"
        tags = setOf("latest", version.toString())
    }
    container {
        jvmFlags =
            listOf(
                "-Xms512m",
                "-Xmx1024m",
                "-Dspring.profiles.active=prod",
            )
        ports = listOf("8080")
        creationTime = "USE_CURRENT_TIMESTAMP"
    }
}
