import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "com.github.Lipen"

plugins {
    kotlin("jvm") version "1.7.20"
    id("fr.brouillard.oss.gradle.jgitver") version "0.9.1"
    id("com.github.ben-manes.versions") version "0.44.0"
    `maven-publish`
}

repositories {
    mavenCentral()
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    // Kotlin
    implementation(platform(kotlin("bom")))
    implementation(kotlin("stdlib-jdk8"))
    // implementation(kotlin("reflect"))

    // Dependencies
    implementation("com.github.Lipen:kotlin-toposort:0.1.0")
    implementation("com.soywiz.korlibs.klock:klock-jvm:3.3.1")
    implementation("com.squareup.okio:okio:3.2.0")

    // Logging
    implementation("io.github.microutils:kotlin-logging:3.0.4")
    runtimeOnly("org.fusesource.jansi:jansi:1.18")

    // Test
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

java {
    withSourcesJar()
    withJavadocJar()
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

jgitver {
    strategy("MAVEN")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
    repositories {
        maven(url = "$buildDir/repository")
    }
}

tasks.wrapper {
    gradleVersion = "7.6"
    distributionType = Wrapper.DistributionType.ALL
}

defaultTasks("clean", "build")
