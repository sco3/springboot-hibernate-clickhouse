plugins {
    java
    id("org.springframework.boot") version "4.1.2"
    id("eclipse")
}

group = "com.example"
version = "0.1.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:4.1.2"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // ClickHouse official JDBC driver (talks to the HTTP interface on port 8123)
    implementation("com.clickhouse:clickhouse-jdbc:0.11.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-resttestclient") // TestRestTemplate lives here in Boot 4
    testImplementation("org.springframework.boot:spring-boot-restclient") // RestTemplateBuilder, needed by TestRestTemplate
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
