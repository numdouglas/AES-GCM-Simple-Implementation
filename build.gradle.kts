plugins {
    id("application")
    id("java")
}

application {
    mainClass.set("m_gcm")
}



repositories {
    mavenCentral()
}

dependencies {
    implementation("org.bouncycastle:bc-fips:1.0.2.3")
    implementation("commons-io:commons-io:2.11.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.1")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.jar {
    manifest.attributes["Main-Class"] = "m_gcm"

    archiveBaseName.set("aes_gcm_impl")

    manifest.attributes["Class-Path"] = configurations
        .runtimeClasspath
        .get()
        .joinToString(separator = " ") { file ->
            "ext_dependencies/${file.name}"
        }
}

tasks.withType<JavaExec>() {
    standardInput = System.`in`
}