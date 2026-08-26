# Sistema de Turnos de Atención

Proyecto desarrollado para la materia **Calidad de Software**.

El objetivo es crear una API REST para gestionar:

* Usuarios
* Servicios
* Funcionarios
* Turnos de atención

## Tecnologías

* Java 17
* Spring Boot
* Maven
* PostgreSQL
* Git y GitHub

## Estado actual

Actualmente el proyecto cuenta con:

* Estructura inicial de Spring Boot
* Java 17 configurado
* Paquetes organizados
* Modelos iniciales
* Enums para tipo de usuario y estado del turno

## Ejecutar el proyecto

En Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

La aplicación se ejecuta en:

```text
http://localhost:8080
```

## Próximos pasos

* Configurar JPA
* Conectar PostgreSQL
* Crear repositorios
* Crear servicios
* Crear controladores
* Implementar reglas de negocio
* Agregar pruebas
* Realizar pruebas de carga con JMeter
