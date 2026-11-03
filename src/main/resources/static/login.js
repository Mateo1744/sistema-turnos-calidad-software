"use strict";
const form = document.getElementById("login-form");
const params = new URLSearchParams(location.search);
const notice = document.getElementById("login-notice");
if (params.has("salida") || params.has("expirada")) {
  notice.textContent = params.has("salida") ? "Has cerrado sesión correctamente." : "Tu sesión terminó. Ingresa nuevamente.";
  notice.hidden = false;
  history.replaceState(null, "", "/login.html");
}
window.addEventListener("pageshow", () => { form.elements.password.value = ""; });
let registration = false;
document.getElementById("toggle-mode").onclick = () => {
  registration = !registration;
  document.getElementById("registration-fields").hidden = !registration;
  form.elements.nombre.required = form.elements.documento.required = registration;
  form.elements.password.minLength = registration ? 12 : 1;
  form.elements.password.autocomplete = registration ? "new-password" : "current-password";
  document.getElementById("password-help").hidden = !registration;
  document.getElementById("login-title").textContent = registration ? "Crea tu cuenta" : "Inicia sesión";
  document.getElementById("login-description").textContent = registration ? "Regístrate para solicitar tus turnos." : "Ingresa con tu cuenta para continuar.";
  document.getElementById("login-submit").textContent = registration ? "Crear cuenta" : "Ingresar";
  document.getElementById("toggle-label").textContent = registration ? "¿Ya tienes cuenta?" : "¿Aún no tienes cuenta?";
  document.getElementById("toggle-mode").textContent = registration ? "Inicia sesión" : "Regístrate";
  document.getElementById("login-error").hidden = true;
};
form.onsubmit = async event => {
  event.preventDefault();
  const button = document.getElementById("login-submit"), error = document.getElementById("login-error");
  button.disabled = true; document.getElementById("toggle-mode").disabled = true; error.hidden = true;
  try {
    const data = Object.fromEntries(new FormData(form));
    data.username = data.username.trim();
    if (registration) await Auth.request("/api/auth/registro", "POST", data);
    await Auth.request("/api/auth/login", "POST", {username:data.username,password:data.password}, true);
    form.elements.password.value = "";
    location.replace("/");
  } catch (e) {
    error.textContent = e.message; error.hidden = false;
    // Renovar el token permite reintentar tras una sesión expirada.
    Auth.csrf = null;
  } finally { button.disabled = false; document.getElementById("toggle-mode").disabled = false; }
};
