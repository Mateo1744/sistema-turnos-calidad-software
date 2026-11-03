package com.calidadsoftware.sistema_turnos.controller;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.MethodArgumentNotValidException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> conflicto() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, "El registro está duplicado o entra en conflicto con los datos existentes"));
    }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> negocio(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode())
                .body(ProblemDetail.forStatusAndDetail(error.getStatusCode(), error.getReason()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> validacion(MethodArgumentNotValidException error) {
        var problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Revisa los datos enviados");
        problema.setProperty("errores", error.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage()).toList());
        return ResponseEntity.badRequest().body(problema);
    }
}
