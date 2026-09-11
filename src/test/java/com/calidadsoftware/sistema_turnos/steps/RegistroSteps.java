package com.calidadsoftware.sistema_turnos.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.calidadsoftware.sistema_turnos.entity.Servicio;
import com.calidadsoftware.sistema_turnos.entity.Usuario;
import com.calidadsoftware.sistema_turnos.enums.TipoUsuario;
import com.calidadsoftware.sistema_turnos.service.ServicioService;
import com.calidadsoftware.sistema_turnos.service.UsuarioService;

import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;

public class RegistroSteps {

    private final UsuarioService usuarioService = new UsuarioService();
    private final ServicioService servicioService = new ServicioService();

    private Usuario usuarioPorRegistrar;
    private Usuario usuarioRegistrado;
    private Servicio servicioPorRegistrar;
    private Servicio servicioRegistrado;

    @Dado("que deseo registrar un usuario llamado {string} con documento {string} y tipo {string}")
    public void prepararUsuario(String nombre, String documento, String tipoUsuario) {
        usuarioPorRegistrar = new Usuario(
                null,
                nombre,
                documento,
                TipoUsuario.valueOf(tipoUsuario));
    }

    @Cuando("registro el usuario")
    public void registrarUsuario() {
        usuarioRegistrado = usuarioService.registrar(usuarioPorRegistrar);
    }

    @Entonces("el usuario queda creado con nombre {string} y un identificador")
    public void verificarUsuarioRegistrado(String nombreEsperado) {
        assertNotNull(usuarioRegistrado);
        assertNotNull(usuarioRegistrado.getId());
        assertEquals(nombreEsperado, usuarioRegistrado.getNombre());
    }

    @Dado("que deseo registrar el servicio {string} con descripción {string} y estado {string}")
    public void prepararServicio(String nombre, String descripcion, String estado) {
        servicioPorRegistrar = new Servicio(null, nombre, descripcion, estado);
    }

    @Cuando("registro el servicio")
    public void registrarServicio() {
        servicioRegistrado = servicioService.registrar(servicioPorRegistrar);
    }

    @Entonces("el servicio queda creado con estado {string} y un identificador")
    public void verificarServicioRegistrado(String estadoEsperado) {
        assertNotNull(servicioRegistrado);
        assertNotNull(servicioRegistrado.getId());
        assertEquals(estadoEsperado, servicioRegistrado.getEstado());
    }
}
