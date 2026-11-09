package definitions;

import io.cucumber.java.es.*;
import io.cucumber.java.After;
import io.cucumber.java.Scenario;
import net.serenitybdd.core.Serenity;
import net.thucydides.core.annotations.Steps;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import steps.TurnosSteps;

public class TurnosDefinitions {
    private static boolean controladorPreparado;
    @io.cucumber.java.Before(order = 0)
    public void prepararControlador() {
        if (!controladorPreparado) {
            io.github.bonigarcia.wdm.WebDriverManager.chromedriver().clearResolutionCache().setup();
            controladorPreparado = true;
        }
    }
    @Steps TurnosSteps pasos;
    @Dado("que abro el login del Sistema de Turnos") public void abrir() { pasos.abrir(); }
    @Cuando("inicio sesión como administrador de pruebas") public void entrar() { pasos.entrar(); }
    @Entonces("veo el centro de turnos y mi rol de administrador") public void panel() { pasos.panel(); }
    @Cuando("cierro sesión") public void salir() { pasos.salir(); }
    @Entonces("regreso al login con la confirmación de salida") public void salida() { pasos.salida(); }
    @Entonces("al intentar abrir el panel vuelvo al login") public void proteccion() { pasos.proteccion(); }
    @Cuando("ingreso una contraseña incorrecta") public void incorrecto() { pasos.loginIncorrecto(); }
    @Entonces("veo un error de autenticación y permanezco en el login") public void rechazo() { pasos.rechazo(); }
    @Cuando("registro un servicio activo, un usuario y un funcionario desde la interfaz") public void datos() { pasos.datos(); }
    @Cuando("creo un turno para ese usuario y servicio") public void crear() { pasos.crear(); }
    @Entonces("el turno aparece en estado {string}") public void estado(String estado) { pasos.estado(estado); }
    @Cuando("pongo el turno en espera") public void esperar() { pasos.avanzar("esperar"); }
    @Cuando("llamo el turno con el funcionario registrado") public void llamar() { pasos.avanzar("llamar"); }
    @Cuando("inicio la atención del turno") public void iniciar() { pasos.avanzar("iniciar"); }
    @Cuando("finalizo la atención del turno") public void finalizar() { pasos.avanzar("finalizar"); }
    @Entonces("después de recargar conserva el estado finalizado y el funcionario asignado") public void persistencia() { pasos.persistencia(); }

    @After
    public void evidencia(Scenario escenario) {
        // Adjunta el resultado visible al HTML de Cucumber, incluso si falla un paso.
        try {
            escenario.attach(((TakesScreenshot) Serenity.getDriver()).getScreenshotAs(OutputType.BYTES),
                "image/png", "Resultado del escenario");
        } catch (RuntimeException error) {
            escenario.log("No se pudo tomar captura: " + error.getClass().getSimpleName());
        }
    }
}
