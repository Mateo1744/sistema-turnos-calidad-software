package steps;

import net.thucydides.core.annotations.Step;
import pages.TurnosPage;

/** Pasos de negocio de la suite E2E. */
public class TurnosSteps {
    TurnosPage pagina;
    @Step("Abrir el login") public void abrir() { pagina.abrirLogin(); }
    @Step("Autenticar al administrador de demostración") public void entrar() { pagina.login(true); }
    @Step("Comprobar identidad, rol y carga del panel") public void panel() { pagina.verificarPanel(); }
    @Step("Cerrar sesión") public void salir() { pagina.salir(); }
    @Step("Comprobar confirmación de salida") public void salida() { pagina.verificarSalida(); }
    @Step("Comprobar protección del panel") public void proteccion() { pagina.verificarProteccion(); }
    @Step("Intentar autenticación con contraseña incorrecta") public void loginIncorrecto() { pagina.login(false); }
    @Step("Comprobar rechazo del acceso") public void rechazo() { pagina.verificarRechazo(); }
    @Step("Registrar servicio, usuario y funcionario por la interfaz") public void datos() { pagina.registrarDatos(); }
    @Step("Crear el turno") public void crear() { pagina.crearTurno(); }
    @Step("Comprobar estado: {0}") public void estado(String estado) { pagina.verificarEstado(estado); }
    @Step("Aplicar transición: {0}") public void avanzar(String accion) { pagina.transicion(accion); }
    @Step("Recargar y comprobar persistencia del resultado") public void persistencia() { pagina.verificarPersistencia(); }
}
