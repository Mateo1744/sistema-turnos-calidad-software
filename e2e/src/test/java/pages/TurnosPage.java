package pages;

import java.time.Duration;
import java.util.UUID;
import net.serenitybdd.core.pages.PageObject;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;
import static org.junit.Assert.*;

/** Interacciones y validaciones de la interfaz del Sistema de Turnos. */
public class TurnosPage extends PageObject {
    private String servicio, usuario, funcionario, numero;

    private WebDriverWait espera() { return new WebDriverWait(getDriver(), Duration.ofSeconds(15)); }
    private WebElement visible(String css) {
        return espera().until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(css)));
    }
    private void click(String css) {
        espera().until(ExpectedConditions.elementToBeClickable(By.cssSelector(css))).click();
    }
    private void escribir(String css, String valor) {
        WebElement campo = visible(css); campo.clear(); campo.sendKeys(valor);
    }
    private void seleccionar(String nombre, String texto) {
        new Select(visible("#fields [name='" + nombre + "']")).selectByVisibleText(texto);
    }
    private void texto(String css, String esperado) {
        espera().until(ExpectedConditions.textToBe(By.cssSelector(css), esperado));
        assertEquals(esperado, visible(css).getText());
    }
    private void cargado() {
        espera().until(d -> !d.findElement(By.id("refresh")).isEnabled() ? false
            : d.findElement(By.id("updated")).getText().startsWith("Actualizado a las"));
        assertFalse("No debe haber error de conexión", getDriver().findElement(By.id("connection")).isDisplayed());
    }
    public void abrirLogin() { openUrl("http://127.0.0.1:8081/login.html"); visible("#login-submit"); }
    public void login(boolean correcto) {
        escribir("#login-form [name='username']", "admin.e2e");
        escribir("#login-form [name='password']", correcto ? "DemoTurnos2026!" : "ClaveIncorrecta!");
        click("#login-submit");
    }
    public void verificarPanel() {
        texto("#account-name", "admin.e2e"); texto("#account-role", "Administrador");
        texto("#title", "Centro de turnos"); cargado();
    }
    public void salir() { click("#logout"); }
    public void verificarSalida() { texto("#login-notice", "Has cerrado sesión correctamente."); visible("#login-submit"); }
    public void verificarProteccion() {
        openUrl("http://127.0.0.1:8081/");
        espera().until(ExpectedConditions.urlContains("/login.html")); visible("#login-submit");
    }
    public void verificarRechazo() {
        texto("#login-error", "Usuario o contraseña incorrectos");
        assertTrue(getDriver().getCurrentUrl().contains("/login.html")); visible("#login-submit");
        verificarProteccion();
    }
    private void vista(String nombre) { cargado(); click("[data-view='" + nombre + "']"); }
    private void nuevo() { click("#create"); visible("#dialog[open]"); }
    private void guardar() {
        click("#submit");
        espera().until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("#dialog[open]")));
        cargado();
    }
    private void filaContiene(String valor) {
        espera().until(d -> d.findElements(By.cssSelector("#content tbody tr")).stream()
            .anyMatch(f -> f.getText().contains(valor)));
    }
    public void registrarDatos() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        servicio = "Servicio E2E " + sufijo; usuario = "Usuario E2E " + sufijo;
        funcionario = "Funcionario E2E " + sufijo;
        vista("servicios"); nuevo();
        escribir("#fields [name='nombre']", servicio);
        escribir("#fields [name='descripcion']", "Atención creada desde el navegador");
        seleccionar("estado", "Activo"); guardar(); filaContiene(servicio);
        vista("usuarios"); nuevo();
        escribir("#fields [name='nombre']", usuario);
        escribir("#fields [name='documento']", "E2E-" + sufijo);
        seleccionar("tipoUsuario", "Normal"); guardar(); filaContiene(usuario);
        vista("funcionarios"); nuevo();
        escribir("#fields [name='nombre']", funcionario);
        seleccionar("servicioId", servicio); guardar(); filaContiene(funcionario);
    }
    public void crearTurno() {
        vista("turnos"); nuevo();
        Select usuarios = new Select(visible("#fields [name='usuarioId']"));
        String opcion = usuarios.getOptions().stream().map(WebElement::getText)
            .filter(t -> t.startsWith(usuario + " · ")).findFirst().orElseThrow();
        usuarios.selectByVisibleText(opcion);
        seleccionar("servicioId", servicio); guardar(); filaContiene(usuario);
        numero = fila().findElement(By.cssSelector(".ticket")).getText();
        assertTrue("El turno debe tener un número generado", numero.matches("T\\d+"));
    }
    private WebElement fila() {
        return espera().until(d -> d.findElements(By.cssSelector("#content tbody tr")).stream()
            .filter(f -> f.getText().contains(usuario) && f.getText().contains(servicio))
            .findFirst().orElse(null));
    }
    public void verificarEstado(String estado) {
        espera().ignoring(StaleElementReferenceException.class).until(d ->
            fila().findElement(By.cssSelector(".badge")).getText().equalsIgnoreCase(estado));
        assertEquals(numero, fila().findElement(By.cssSelector(".ticket")).getText());
        assertEquals(estado, fila().findElement(By.cssSelector(".badge")).getText().toLowerCase(java.util.Locale.ROOT));
    }
    public void transicion(String accion) {
        // El aviso de éxito puede cubrir los botones de la fila mientras está visible.
        espera().until(ExpectedConditions.invisibilityOfElementLocated(By.id("toast")));
        fila().findElement(By.cssSelector("[data-action='" + accion + "']")).click();
        visible("#dialog[open]");
        if (accion.equals("llamar")) seleccionar("funcionarioId", funcionario);
        guardar();
    }
    public void verificarPersistencia() {
        getDriver().navigate().refresh(); verificarPanel();
        verificarEstado("finalizado");
        assertTrue(fila().getText().contains(funcionario));
        assertTrue("Un turno finalizado no permite más transiciones", fila().findElements(By.cssSelector("[data-action]")).isEmpty());
    }
}
