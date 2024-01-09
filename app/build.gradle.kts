import org.gradle.jvm.tasks.Jar


plugins {
    id("groovy")
    id("application")
    id("org.jetbrains.kotlin.jvm") version "1.7.20"
    id("org.graalvm.buildtools.native") version "0.9.28"
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

group = "com.stm.evernote.rsync"
version = "0.7.0"

dependencies {
    implementation("com.evernote:evernote-api:1.25.1")
    implementation("commons-io:commons-io:2.11.0")
    implementation("commons-codec:commons-codec:1.15")
    implementation("info.picocli:picocli:4.6.3")
    annotationProcessor("info.picocli:picocli-codegen:4.6.3")

    implementation("org.slf4j:slf4j-api:1.7.36")
    implementation("ch.qos.logback:logback-classic:1.2.11")

    testImplementation("org.codehaus.groovy:groovy:3.0.9")

    testImplementation("org.spockframework:spock-core:2.2-groovy-3.0")
    testImplementation("junit:junit:4.13.2")

    implementation("com.google.guava:guava:30.1.1-jre")
}

tasks.register<Jar>("fatjar") {
    group = "Build"
    description = "Build a jar with all dependencies"
    manifest {
        attributes["Main-Class"] = "com.stm.evenote.rsync.EvernoteSync"
    }

    archiveBaseName.set("evernote-rsync")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.jar.get())
}

application {
    mainClass.set("com.stm.evenote.rsync.EvernoteSync")
    applicationName = "EvernoteRsync"
}

tasks.test {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("evernote-rsync")
            verbose.set(true)
            buildArgs.addAll("--enable-url-protocols=https")
        }
    }
}
