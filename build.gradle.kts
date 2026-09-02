plugins {
    java
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"

    // Pruebas de mutación con PIT
    id("info.solidsoft.pitest") version "1.19.0"

    // Análisis de calidad con SonarQube
    id("org.sonarqube") version "7.3.1.8318"
}

group = "com.calidadsoftware"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

/*
 * ---------------------------------------------------------
 * SOURCE SET PARA PRUEBAS DE ACEPTACIÓN
 * ---------------------------------------------------------
 */

val acceptanceTestSourceSet = sourceSets.create("acceptanceTest") {
    java.srcDir("src/acceptanceTest/java")
    resources.srcDir("src/acceptanceTest/resources")

    compileClasspath += sourceSets["main"].output
    runtimeClasspath += output + compileClasspath
}

configurations[acceptanceTestSourceSet.implementationConfigurationName]
    .extendsFrom(configurations.testImplementation.get())

configurations[acceptanceTestSourceSet.runtimeOnlyConfigurationName]
    .extendsFrom(configurations.testRuntimeOnly.get())

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")

    // Pruebas unitarias
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")

    // Cucumber para pruebas de aceptación
    add(
        acceptanceTestSourceSet.implementationConfigurationName,
        "io.cucumber:cucumber-java:7.34.7"
    )

    // JUnit Platform utilizado por PIT con Spring Boot
    add(
        "pitest",
        "org.junit.platform:junit-platform-launcher:6.0.3"
    )
}

/*
 * ---------------------------------------------------------
 * PRUEBAS UNITARIAS
 * ---------------------------------------------------------
 */

tasks.withType<Test> {
    useJUnitPlatform()
}

/*
 * ---------------------------------------------------------
 * PRUEBAS DE ACEPTACIÓN - CUCUMBER
 * ---------------------------------------------------------
 */

tasks.register<JavaExec>("acceptanceTest") {
    description = "Ejecuta las pruebas de aceptación con Cucumber"
    group = "verification"

    dependsOn(acceptanceTestSourceSet.classesTaskName)

    classpath = acceptanceTestSourceSet.runtimeClasspath

    mainClass.set("io.cucumber.core.cli.Main")

    doFirst {
        layout.buildDirectory
            .dir("test-results/acceptanceTest")
            .get()
            .asFile
            .mkdirs()
    }

    args(
        "--plugin", "pretty",
        "--plugin", "junit:build/test-results/acceptanceTest/TEST-cucumber.xml",
        "--glue", "com.calidadsoftware",
        "src/acceptanceTest/resources"
    )
}

/*
 * ---------------------------------------------------------
 * PRUEBAS DE MUTACIÓN - PITEST
 * ---------------------------------------------------------
 */

pitest {
    targetClasses.set(
        listOf("com.calidadsoftware.*")
    )

    junit5PluginVersion.set("1.2.3")

    outputFormats.set(
        listOf("HTML", "XML")
    )

    timestampedReports.set(false)
}