plugins {
    java
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.sonarqube") version "6.2.0.5505"
    id("info.solidsoft.pitest") version "1.19.0"
     
}

group = "com.calidadsoftware"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

sourceSets {
    test {
        resources.srcDir(
            "src/test/java/com/calidadsoftware/sistema_turnos/resources"
        )
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")

    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    

        // PIT Mutation Testing
    pitest ("org.pitest:pitest-junit5-plugin:1.19.0")

    // Cucumber
    testImplementation("io.cucumber:cucumber-java:7.34.8")
    testImplementation("io.cucumber:cucumber-junit-platform-engine:7.34.8")
    testImplementation("org.junit.platform:junit-platform-suite")
}

configurations.named("testRuntimeClasspath") {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.junit.platform" &&
            requested.name == "junit-platform-launcher"
        ) {
            useVersion("6.0.3")
            because("Spring Boot 4.1.1 utiliza JUnit Platform 6.0.3")
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<Test>("acceptanceTest") {
    description = "Runs Cucumber acceptance tests."
    group = "verification"

    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath

    useJUnitPlatform {
        includeEngines("cucumber")
    }
}


sonar {
    properties {

        property(
            "sonar.projectKey",
            "Sistema-de-Turnos"
        )

        property(
            "sonar.projectName",
            "Sistema de Turnos"
        )

        property(
            "sonar.login", 
            "sqp_52b521d8e18db47ccc1adcf2726e0fbe1fd0f141"
        )

        property(
            "sonar.host.url", 
            "http://localhost:9000"
        )
    }
}

pitest {
    pitestVersion.set("1.19.0")
    junit5PluginVersion.set("1.2.1")
    targetClasses.set(listOf("com.calidadsoftware.sistema_turnos.*")) // <--- cambia aquí targetTests.set(listOf("org.example.*"))
    threads.set(4)
    outputFormats.set(listOf("HTML"))
    timestampedReports.set(false)
}
