plugins {
    id("application")
    id("java")
}

application {
    mainClass.set("m_gcm")
}

val jar by tasks.getting(Jar::class) {
    manifest {
        attributes["Main-Class"] = "m_gcm"
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.bouncycastle:bc-fips:1.0.2.3")
    implementation("commons-io:commons-io:2.11.0")
}

tasks.withType<JavaExec>() {
    standardInput = System.`in`
}