package com.calidadsoftware.sistema_turnos;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ApiBasicaIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registraUnUsuario() throws Exception {
        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Ana Pérez",
                                  "documento": "100100100",
                                  "tipoUsuario": "PRIORITARIO"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nombre").value("Ana Pérez"))
                .andExpect(jsonPath("$.documento").value("100100100"))
                .andExpect(jsonPath("$.tipoUsuario").value("PRIORITARIO"));
    }

    @Test
    void registraUnServicio() throws Exception {
        mockMvc.perform(post("/servicios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Pagos",
                                  "descripcion": "Recepción de pagos presenciales",
                                  "estado": "activo"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nombre").value("Pagos"))
                .andExpect(jsonPath("$.estado").value("ACTIVO"));
    }

    @Test
    void listaLosServiciosRegistrados() throws Exception {
        String nombre = "Información general";

        mockMvc.perform(post("/servicios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Información general",
                                  "descripcion": "Orientación a usuarios",
                                  "estado": "ACTIVO"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/servicios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nombre", hasItem(nombre)));
    }
}
