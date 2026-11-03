import groovy.json.JsonSlurper
import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    java
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.sonarqube") version "6.2.0.5505"
    id("info.solidsoft.pitest") version "1.19.0"
     
}

group = "com.calidadsoftware"
version = "1.0-SNAPSHOT"

springBoot {
    mainClass.set("com.calidadsoftware.sistema_turnos.SistemaTurnosApplication")
}

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
    implementation("org.springframework.boot:spring-boot-starter-security")
    testImplementation("org.springframework.security:spring-security-test")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("com.h2database:h2")
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
    systemProperty("spring.profiles.active", "test")
    useJUnitPlatform()
}

fun JavaExec.cargarConexionLocal() {
        if (environment["DB_PASSWORD"]?.toString().isNullOrBlank()) {
            val conexion = project.file(".local/postgres.clixml")
            if (!conexion.exists()) {
                throw GradleException("Configura la conexión una vez con: powershell -ExecutionPolicy Bypass -File .\\scripts\\configurar-conexion.ps1; o define DB_PASSWORD.")
            }
            if (!System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) {
                throw GradleException("La conexión local cifrada solo funciona en Windows. Define DB_URL, DB_USERNAME y DB_PASSWORD en este equipo.")
            }
            val proceso = ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive",
                "-ExecutionPolicy", "Bypass", "-File",
                project.file("scripts/leer-conexion.ps1").absolutePath,
                "-Archivo", conexion.absolutePath).redirectErrorStream(true).start()
            val salida = proceso.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            if (proceso.waitFor() != 0) {
                throw GradleException("No se pudo leer la conexión cifrada. Ejecuta configurar-conexion.ps1 con tu usuario de Windows.")
            }
            val datos = JsonSlurper().parseText(salida) as Map<*, *>
            for (clave in listOf("DB_URL", "DB_USERNAME", "DB_PASSWORD")) {
                if (environment[clave]?.toString().isNullOrBlank()) {
                    environment(clave, datos[clave]?.toString()
                        ?: throw GradleException("Configuración local incompleta. Ejecuta configurar-conexion.ps1."))
                }
            }
        }
}

tasks.named<BootRun>("bootRun") {
    doFirst { cargarConexionLocal() }
}

tasks.register<JavaExec>("restablecerAdmin") {
    group = "application"
    description = "Restablece localmente la contraseña de una cuenta administradora existente."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.calidadsoftware.sistema_turnos.security.RestablecerAdminCommand")
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(17))
    })
    doFirst { cargarConexionLocal() }
}

tasks.register<Test>("acceptanceTest") {
    systemProperty("spring.profiles.active", "test")
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
