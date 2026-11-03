package com.calidadsoftware.sistema_turnos;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
@AutoConfigureMockMvc
@org.springframework.security.test.context.support.WithMockUser(roles = "ADMIN")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class TurnosIntegrationTests {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private org.springframework.web.context.WebApplicationContext context;

    @BeforeEach
    void preparar() throws Exception {
        mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(context)
                .defaultRequest(get("/").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity()).build();
        mvc.perform(post("/usuarios").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nombre":"Ana","documento":"100","tipoUsuario":"NORMAL"}
                        """)).andExpect(status().isCreated());
        mvc.perform(post("/servicios").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nombre":"Pagos","descripcion":"Pago de facturas","estado":"ACTIVO"}
                        """)).andExpect(status().isCreated());
    }

    @Test
    void creaYConsultaTurno() throws Exception {
        mvc.perform(post("/turnos").contentType(MediaType.APPLICATION_JSON)
                .content(""" 
                        {"usuarioId":1,"servicioId":1}
                        """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/turnos/1"))
                .andExpect(jsonPath("$.numero").value("T001"))
                .andExpect(jsonPath("$.estado").value("GENERADO"))
                .andExpect(jsonPath("$.fechaCreacion").isNotEmpty())
                .andExpect(jsonPath("$.fechaInicio").value(nullValue()))
                .andExpect(jsonPath("$.usuario.documento").value("100"));
        for (String ruta : new String[]{"/turnos/1", "/turnos/numero/T001"}) {
            mvc.perform(get(ruta)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));
        }
        for (String ruta : new String[]{"/turnos", "/turnos/pendientes", "/turnos?servicioId=1"}) {
            mvc.perform(get(ruta)).andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }
    }

    @Test
    void rechazaDuplicadoPeroPermiteOtroServicio() throws Exception {
        String solicitud = "{\"usuarioId\":1,\"servicioId\":1}";
        mvc.perform(post("/turnos").contentType(MediaType.APPLICATION_JSON).content(solicitud))
                .andExpect(status().isCreated());
        mvc.perform(post("/turnos").contentType(MediaType.APPLICATION_JSON).content(solicitud))
                .andExpect(status().isConflict());
        mvc.perform(post("/servicios").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nombre":"Soporte","descripcion":"Ayuda","estado":"ACTIVO"}
                        """)).andExpect(status().isCreated());
        mvc.perform(post("/turnos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuarioId\":1,\"servicioId\":2}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.numero").value("T002"));
        mvc.perform(get("/turnos?servicioId=2")).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].numero").value("T002"));
    }

    @Test
    void rechazaDatosInvalidosYReferenciasInexistentes() throws Exception {
        for (String cuerpo : new String[]{"{}", "{\"usuarioId\":0,\"servicioId\":1}",
                "{\"usuarioId\":1,\"servicioId\":-1}"}) {
            mvc.perform(post("/turnos").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isBadRequest());
        }
        for (String cuerpo : new String[]{"{\"usuarioId\":99,\"servicioId\":1}",
                "{\"usuarioId\":1,\"servicioId\":99}"}) {
            mvc.perform(post("/turnos").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isNotFound());
        }
        for (String ruta : new String[]{"/turnos/99", "/turnos/numero/NOEXISTE", "/turnos?servicioId=99"}) {
            mvc.perform(get(ruta)).andExpect(status().isNotFound());
        }
        mvc.perform(get("/turnos")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void atiendeTurnoDesdeLaApi() throws Exception {
        mvc.perform(post("/funcionarios").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Laura\",\"servicioId\":1}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.estado").value("ACTIVO"));
        mvc.perform(get("/funcionarios")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/funcionarios/1")).andExpect(status().isOk());
        mvc.perform(get("/usuarios")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/usuarios/1")).andExpect(status().isOk());
        mvc.perform(get("/servicios/1")).andExpect(status().isOk());
        mvc.perform(post("/turnos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuarioId\":1,\"servicioId\":1}")).andExpect(status().isCreated());
        mvc.perform(patch("/turnos/1/finalizar")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").isNotEmpty());
        mvc.perform(patch("/turnos/1/esperar")).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_ESPERA"));
        mvc.perform(patch("/turnos/1/llamar").contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores").isArray());
        mvc.perform(patch("/turnos/1/llamar").contentType(MediaType.APPLICATION_JSON)
                .content("{\"funcionarioId\":1}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.funcionario.nombre").value("Laura"));
        mvc.perform(patch("/turnos/1/iniciar")).andExpect(status().isOk())
                .andExpect(jsonPath("$.fechaInicio").isNotEmpty());
        mvc.perform(patch("/turnos/1/finalizar")).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("FINALIZADO"));
        mvc.perform(get("/turnos/funcionario/1")).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(patch("/turnos/1/cancelar")).andExpect(status().isConflict());
        mvc.perform(post("/turnos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuarioId\":1,\"servicioId\":1}")).andExpect(status().isCreated());
        mvc.perform(patch("/turnos/2/cancelar")).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADO"));
    }

    @Test
    void rechazaServicioInactivo() throws Exception {
        mvc.perform(post("/servicios").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nombre":"Cerrado","descripcion":"No disponible","estado":"INACTIVO"}
                        """)).andExpect(status().isCreated());
        mvc.perform(post("/turnos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuarioId\":1,\"servicioId\":2}")).andExpect(status().isConflict());
    }
}
