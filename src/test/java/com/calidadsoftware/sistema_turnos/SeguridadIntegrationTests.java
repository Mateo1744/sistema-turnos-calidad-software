package com.calidadsoftware.sistema_turnos;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.calidadsoftware.sistema_turnos.security.*;
import com.calidadsoftware.sistema_turnos.service.*;
import com.calidadsoftware.sistema_turnos.entity.Servicio;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class SeguridadIntegrationTests {
    private static final String CLAVE = "SoloPruebas-2026!";
    @Autowired private MockMvc mvc;
    @Autowired private CuentaService cuentas;
    @Autowired private CuentaRepository repository;
    @Autowired private ServicioService servicios;
    @Autowired private FuncionarioService funcionarios;
    @Autowired private PasswordEncoder encoder;

    private MockHttpSession login(String username) throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/auth/login").with(csrf())
                .param("username", username).param("password", CLAVE))
                .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
    }

    @Test
    void restableceAdminInvalidaSesionYRechazaClaveAnterior() throws Exception {
        cuentas.crearAdministradorInicial("admin-test", CLAVE);
        var session = login("admin-test");
        Long id = repository.findByUsername("admin-test").orElseThrow().getId();
        cuentas.restablecerAdministrador(" ADMIN-TEST ", "NuevaSoloPruebas-2026!");
        assertEquals(id, repository.findByUsername("admin-test").orElseThrow().getId());
        assertEquals(1, repository.count());
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
        assertTrue(session.isInvalid());
        mvc.perform(post("/api/auth/login").with(csrf()).param("username","admin-test")
                .param("password",CLAVE)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").with(csrf()).param("username","admin-test")
                .param("password","NuevaSoloPruebas-2026!")).andExpect(status().isOk());
    }

    @Test
    void restablecimientoNoCreaCuentasNiModificaUsuariosNormales() {
        cuentas.registrarUsuario("persona",CLAVE,"Ana","100");
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> cuentas.restablecerAdministrador("persona","NuevaSoloPruebas-2026!"));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> cuentas.restablecerAdministrador("inexistente","NuevaSoloPruebas-2026!"));
        assertEquals(1, repository.count());
        assertTrue(encoder.matches(CLAVE, repository.findByUsername("persona").orElseThrow().getPasswordHash()));
    }

    @Test
    void aceptaTokenRealDelNavegadorYRenuevaSesion() throws Exception {
        cuentas.crearAdministradorInicial("admin-test", CLAVE);
        var tokenResult = mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        var session = (MockHttpSession)tokenResult.getRequest().getSession(false);
        var token = (org.springframework.security.web.csrf.CsrfToken)
                tokenResult.getRequest().getAttribute(org.springframework.security.web.csrf.CsrfToken.class.getName());
        String anterior = session.getId();
        mvc.perform(post("/api/auth/login").session(session)
                .header(token.getHeaderName(), token.getToken())
                .param("username","admin-test").param("password",CLAVE)).andExpect(status().isOk());
        assertNotEquals(anterior, session.getId());
        var nuevoResult = mvc.perform(get("/api/auth/csrf").session(session)).andExpect(status().isOk()).andReturn();
        var nuevo = (org.springframework.security.web.csrf.CsrfToken)
                nuevoResult.getRequest().getAttribute(org.springframework.security.web.csrf.CsrfToken.class.getName());
        mvc.perform(post("/api/auth/logout").session(session)
                .header(nuevo.getHeaderName(),nuevo.getToken())).andExpect(status().isNoContent());
    }

    @Test
    void autenticaCifraYPermiteCerrarSesion() throws Exception {
        cuentas.crearAdministradorInicial("admin-test", CLAVE);
        var hash = repository.findByUsername("admin-test").orElseThrow().getPasswordHash();
        assertNotEquals(CLAVE, hash);
        assertTrue(encoder.matches(CLAVE, hash));
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").with(csrf()).param("username","admin-test")
                .param("password","incorrecta")).andExpect(status().isUnauthorized());
        var session = login("admin-test");
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("ADMIN"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        mvc.perform(get("/api/admin/cuentas").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
        mvc.perform(post("/api/auth/logout").session(session).with(csrf()))
                .andExpect(status().isNoContent());
        assertTrue(session.isInvalid());
        mvc.perform(get("/usuarios")).andExpect(status().isUnauthorized());
    }

    @Test
    void registroPublicoSiempreEsUsuarioYRequiereCsrf() throws Exception {
        String registro = """
                {"username":"persona","password":"SoloPruebas-2026!","nombre":"Ana","documento":"100","rol":"ADMIN"}
                """;
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(registro))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/registro").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(registro)).andExpect(status().isCreated());
        var session = login("persona");
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("USUARIO"));
        for (String path : new String[]{"/usuarios","/turnos","/funcionarios","/api/admin/cuentas"}) {
            mvc.perform(get(path).session(session)).andExpect(status().isForbidden());
        }
        mvc.perform(post("/servicios").session(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"No autorizado\",\"descripcion\":\"Prueba\",\"estado\":\"ACTIVO\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/logout").session(session)).andExpect(status().isForbidden());
    }

    @Test
    void usuarioSoloVeYCancelaSusTurnos() throws Exception {
        cuentas.registrarUsuario("persona1", CLAVE, "Ana", "100");
        cuentas.registrarUsuario("persona2", CLAVE, "Luis", "200");
        var servicio = servicios.registrar(new Servicio(null,"Pagos","Pagos","ACTIVO"));
        var uno = login("persona1");
        var dos = login("persona2");
        mvc.perform(post("/api/mi/turnos").session(uno).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"servicioId\":" + servicio.getId() + "}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.estado").value("EN_ESPERA"));
        mvc.perform(get("/api/mi/turnos").session(uno)).andExpect(jsonPath("$",hasSize(1)));
        mvc.perform(get("/api/mi/turnos").session(dos)).andExpect(jsonPath("$",hasSize(0)));
        mvc.perform(patch("/api/mi/turnos/1/cancelar").session(dos).with(csrf()))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/api/mi/turnos/1/cancelar").session(uno).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("CANCELADO"));
    }

    @Test
    void funcionarioSoloAtiendeSuServicioYSuAsignacion() throws Exception {
        var servicio1 = servicios.registrar(new Servicio(null,"Pagos","Pagos","ACTIVO"));
        var servicio2 = servicios.registrar(new Servicio(null,"Soporte","Ayuda","ACTIVO"));
        var f1 = funcionarios.registrar("Laura", servicio1.getId());
        var f2 = funcionarios.registrar("Pedro", servicio2.getId());
        cuentas.registrarFuncionario("laura",CLAVE,f1.getId());
        cuentas.registrarFuncionario("pedro",CLAVE,f2.getId());
        cuentas.registrarUsuario("persona",CLAVE,"Ana","100");
        var persona=login("persona");
        mvc.perform(post("/api/mi/turnos").session(persona).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"servicioId\":1}")).andExpect(status().isCreated());
        var laura=login("laura"); var pedro=login("pedro");
        mvc.perform(get("/turnos").session(pedro)).andExpect(status().isOk()).andExpect(jsonPath("$",hasSize(0)));
        mvc.perform(get("/turnos/1").session(pedro)).andExpect(status().isForbidden());
        mvc.perform(patch("/turnos/1/llamar").session(laura).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"funcionarioId\":2}")).andExpect(status().isForbidden());
        mvc.perform(patch("/turnos/1/llamar").session(laura).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"funcionarioId\":1}")).andExpect(status().isOk());
        mvc.perform(patch("/turnos/1/iniciar").session(pedro).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(patch("/turnos/1/iniciar").session(laura).with(csrf())).andExpect(status().isOk());
        mvc.perform(get("/usuarios").session(laura)).andExpect(status().isForbidden());
    }

    @Test
    void soloAdministradorHabilitaFuncionarios() throws Exception {
        cuentas.crearAdministradorInicial("admin-test",CLAVE);
        var servicio=servicios.registrar(new Servicio(null,"Pagos","Pagos","ACTIVO"));
        funcionarios.registrar("Laura",servicio.getId());
        var session=login("admin-test");
        mvc.perform(post("/api/admin/cuentas-funcionarios").session(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"laura\",\"password\":\"SoloPruebas-2026!\",\"funcionarioId\":1}"))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/auth/me").session(login("laura"))).andExpect(jsonPath("$.rol").value("FUNCIONARIO"));
        cuentas.crearAdministradorInicial("otro-admin",CLAVE);
        assertFalse(repository.existsByUsername("otro-admin"));
    }
}
