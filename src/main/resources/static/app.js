"use strict";
const $ = id => document.getElementById(id);
const state = { view: "turnos", turnos: [], usuarios: [], servicios: [], funcionarios: [], cuentas: [], loaded: false, me: null };
const labels = { turnos: ["Centro de turnos", "Organiza la espera. Haz que la atención fluya.", "Nuevo turno", "Cola de atención"],
  usuarios: ["Usuarios", "Las personas en el centro de tu atención.", "Nuevo usuario", "Directorio de usuarios"],
  servicios: ["Servicios", "Organiza los motivos de visita a tu centro.", "Nuevo servicio", "Servicios disponibles"],
  funcionarios: ["Funcionarios", "Un equipo preparado para atender.", "Nuevo funcionario", "Equipo de atención"],
  cuentas: ["Cuentas de acceso", "Administra el acceso del personal de atención.", "Habilitar funcionario", "Cuentas registradas"] };
const escapeHtml = value => String(value ?? "").replace(/[&<>"']/g, c => ({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c]));
const textState = value => String(value).replaceAll("_", " ").toLowerCase();
const badge = value => '<span class="badge ' + escapeHtml(value) + '">' + escapeHtml(textState(value)) + '</span>';
let submitting = false, toastTimer, actionHandler;
async function api(path, method = "GET", body) {
  try { return await Auth.request(path, method, body); }
  catch (e) {
    if (e.status === 401) {
      document.body.classList.add("checking-session");
      location.replace(state.loaded ? "/login.html?expirada=1" : "/login.html");
    }
    throw e;
  }
}
async function load() {
  $("refresh").disabled = true;
  try {
    const paths = state.me.rol === "ADMIN"
      ? {turnos:"/turnos",usuarios:"/usuarios",servicios:"/servicios",funcionarios:"/funcionarios",cuentas:"/api/admin/cuentas"}
      : state.me.rol === "FUNCIONARIO"
        ? {turnos:"/turnos",servicios:"/servicios",funcionarios:"/funcionarios"}
        : {turnos:"/api/mi/turnos",servicios:"/servicios"};
    await Promise.all(Object.entries(paths).map(async ([key,path]) => state[key] = await api(path)));
    state.loaded = true; $("connection").hidden = true;
    $("updated").textContent = "Actualizado a las " + new Date().toLocaleTimeString("es", {hour:"2-digit",minute:"2-digit"});
    render();
  } catch (e) {
    $("connection").textContent = "No se pudieron actualizar los datos. Comprueba que la aplicación esté encendida. " + e.message;
    $("connection").hidden = false;
    if (!state.loaded) $("content").innerHTML = '<div class="empty"><strong>No pudimos conectar con la API</strong>Utiliza Actualizar para volver a intentarlo.</div>';
  } finally { $("refresh").disabled = false; }
}
function toast(message) {
  clearTimeout(toastTimer); $("toast").textContent = message; $("toast").hidden = false;
  toastTimer = setTimeout(() => $("toast").hidden = true, 4500);
}
function render() {
  const t = state.turnos;
  $("total").textContent = t.length;
  $("waiting").textContent = t.filter(x => ["GENERADO","EN_ESPERA","LLAMADO"].includes(x.estado)).length;
  $("serving").textContent = t.filter(x => x.estado === "EN_ATENCION").length;
  $("done").textContent = t.filter(x => x.estado === "FINALIZADO").length;
  const query = $("search").value.toLocaleLowerCase();
  const rows = state[state.view].filter(x => [x.numero,x.nombre,x.username,x.documento,x.descripcion,x.usuario?.nombre,x.servicio?.nombre]
    .some(v => String(v ?? "").toLocaleLowerCase().includes(query)))
    .filter(x => state.view !== "turnos" || !$("filter").value || x.estado === $("filter").value);
  $("count").textContent = rows.length + (rows.length === 1 ? " registro" : " registros");
  if (!rows.length) {
    $("content").innerHTML = '<div class="empty"><div class="empty-icon">◫</div><strong>' +
      (state[state.view].length ? "Sin coincidencias" : "Todo listo para comenzar") +
      '</strong>' + (state[state.view].length ? "Prueba con otra búsqueda o filtro."
        : state.me.rol === "FUNCIONARIO" ? "Todavía no hay turnos en esta vista. Pulsa Actualizar para consultar la cola."
        : "Usa el botón de arriba para crear tu primer registro.") + '</div>';
    return;
  }
  let headers, cells;
  if (state.view === "turnos") {
    headers = ["Turno","Usuario","Servicio","Estado","Funcionario","Acciones"];
    cells = x => ['<span class="ticket">'+escapeHtml(x.numero)+'</span>',
      escapeHtml(x.usuario.nombre)+(x.usuario.tipoUsuario === "PRIORITARIO" ? '<div class="priority">★ Prioritario</div>' : ''),
      escapeHtml(x.servicio.nombre),badge(x.estado),escapeHtml(x.funcionario?.nombre || "Sin asignar"),actions(x)];
  } else if (state.view === "usuarios") {
    headers = ["ID","Nombre","Documento","Tipo de usuario"];
    cells = x => [escapeHtml(x.id),escapeHtml(x.nombre),escapeHtml(x.documento),badge(x.tipoUsuario)];
  } else if (state.view === "servicios") {
    headers = ["ID","Servicio","Descripción","Estado"];
    cells = x => [escapeHtml(x.id),escapeHtml(x.nombre),escapeHtml(x.descripcion),badge(x.estado)];
  } else if (state.view === "cuentas") {
    headers = ["Usuario de acceso","Rol","Funcionario asociado"];
    cells = x => [escapeHtml(x.username),badge(x.rol),escapeHtml(state.funcionarios.find(f=>f.id===x.funcionarioId)?.nombre || "—")];
  } else {
    headers = ["ID","Nombre","Servicio","Estado"];
    cells = x => [escapeHtml(x.id),escapeHtml(x.nombre),escapeHtml(x.servicio.nombre),badge(x.estado)];
  }
  $("content").innerHTML = '<div class="table-wrap"><table><thead><tr>'+headers.map(h=>'<th scope="col">'+h+'</th>').join('')+
    '</tr></thead><tbody>'+rows.map(x=>'<tr>'+cells(x).map(c=>'<td>'+c+'</td>').join('')+'</tr>').join('')+'</tbody></table></div>';
}
function actions(t) {
  if (state.me.rol === "USUARIO") {
    return ["GENERADO","EN_ESPERA"].includes(t.estado)
      ? '<button class="action danger" data-id="'+t.id+'" data-action="cancelar">Cancelar mi turno</button>'
      : '<span style="color:#91a096">'+(["FINALIZADO","CANCELADO"].includes(t.estado)?"Cerrado":"Sigue las indicaciones del personal")+'</span>';
  }
  const next = {GENERADO:["esperar","Poner en espera"],EN_ESPERA:["llamar","Llamar"],LLAMADO:["iniciar","Iniciar"],EN_ATENCION:["finalizar","Finalizar"]}[t.estado];
  if (!next) return '<span style="color:#91a096">Cerrado</span>';
  return '<div class="actions"><button class="action" data-id="'+t.id+'" data-action="'+next[0]+'">'+next[1]+'</button><button class="action danger" data-id="'+t.id+'" data-action="cancelar">Cancelar</button></div>';
}
function field(name,label,type="text") {
  if (type === "password") return '<label class="field">'+label+'<input name="'+name+'" type="password" autocomplete="new-password" required minlength="12" maxlength="64"></label>';
  return '<label class="field">'+label+'<input name="'+name+'" type="'+type+'" required maxlength="160"></label>';
}
function select(name,label,values) {
  return '<label class="field">'+label+'<select name="'+name+'" required><option value="">Selecciona una opción</option>'+
    values.map(([id,title])=>'<option value="'+escapeHtml(id)+'">'+escapeHtml(title)+'</option>').join('')+'</select></label>';
}
function openDialog(title,fields,handler,button="Guardar") {
  $("form").reset(); $("dialog-title").textContent = title; $("fields").innerHTML = fields;
  $("form-error").hidden = true; $("submit").textContent = button; actionHandler = handler;
  $("dialog").showModal();
}
$("create").onclick = () => {
  if (!state.loaded) { toast("Primero conecta con la API usando Actualizar."); return; }
  const view = state.view;
  const services = state.servicios.filter(s=>s.estado==="ACTIVO").map(s=>[s.id,s.nombre]);
  let fields;
  if (view === "cuentas") {
    const pendientes = state.funcionarios.filter(f=>!state.cuentas.some(c=>c.funcionarioId===f.id));
    if (!pendientes.length) { toast("Registra un funcionario sin cuenta antes de habilitar su acceso."); return; }
    fields = select("funcionarioId","Funcionario",pendientes.map(f=>[f.id,f.nombre]))
      +field("username","Nombre de usuario")+field("password","Contraseña (12 a 64 caracteres)","password");
  }
  if (view === "usuarios") fields = field("nombre","Nombre completo")+field("documento","Documento")+select("tipoUsuario","Tipo de usuario",[["NORMAL","Normal"],["PRIORITARIO","Prioritario"]]);
  if (view === "servicios") fields = field("nombre","Nombre del servicio")+field("descripcion","Descripción")+select("estado","Estado",[["ACTIVO","Activo"],["INACTIVO","Inactivo"]]);
  if (view === "funcionarios") {
    if (!services.length) { toast("Registra primero un servicio activo."); return; }
    fields = field("nombre","Nombre del funcionario")+select("servicioId","Servicio asignado",services);
  }
  if (view === "turnos") {
    if (!services.length || (state.me.rol === "ADMIN" && !state.usuarios.length)) { toast("Necesitas un usuario y un servicio activo."); return; }
    fields = (state.me.rol === "USUARIO" ? "" : select("usuarioId","Usuario",state.usuarios.map(u=>[u.id,u.nombre+" · "+u.documento])))+select("servicioId","Servicio",services);
  }
  openDialog(labels[view][2],fields, async data => {
    for (const key of ["usuarioId","servicioId","funcionarioId"]) if (data[key]) data[key] = Number(data[key]);
    const path = view === "cuentas" ? "/api/admin/cuentas-funcionarios"
      : view === "turnos" && state.me.rol === "USUARIO" ? "/api/mi/turnos" : "/"+view;
    const result = await api(path,"POST",data);
    return view === "turnos" ? "Turno "+result.numero+" creado correctamente." : "Registro creado correctamente.";
  }, "Crear");
};
$("content").onclick = event => {
  const button = event.target.closest("[data-action]");
  if (!button) return;
  const t = state.turnos.find(t=>t.id === Number(button.dataset.id)), action = button.dataset.action;
  let fields = '<p>Confirma la operación para el turno <strong>'+escapeHtml(t.numero)+'</strong> de '+escapeHtml(t.usuario.nombre)+'.</p>';
  if (action === "llamar") {
    const available = state.funcionarios.filter(f=>f.estado==="ACTIVO" && f.servicio.id===t.servicio.id &&
      !state.turnos.some(other=>other.funcionario?.id===f.id && ["LLAMADO","EN_ATENCION"].includes(other.estado)));
    if (!available.length) { toast("No hay funcionarios disponibles para este servicio."); return; }
    fields += select("funcionarioId","Funcionario",available.map(f=>[f.id,f.nombre]));
  }
  openDialog(button.textContent+" · "+t.numero, fields, async data => {
    const base = state.me.rol === "USUARIO" ? "/api/mi/turnos/" : "/turnos/";
    await api(base+t.id+"/"+action,"PATCH",action==="llamar" ? {funcionarioId:Number(data.funcionarioId)} : undefined);
    return "Turno "+t.numero+" actualizado.";
  },"Confirmar");
};
$("form").onsubmit = async event => {
  event.preventDefault(); if (submitting) return;
  submitting = true; $("submit").disabled = true; $("cancel").disabled = true; $("close").disabled = true;
  $("form-error").hidden = true;
  try {
    const data = Object.fromEntries(new FormData($("form")));
    Object.keys(data).forEach(k=>{if(k!=="password")data[k]=data[k].trim();});
    if (Object.values(data).some(v=>!v.trim())) throw new Error("Completa los campos sin dejar solo espacios.");
    const message = await actionHandler(data); $("dialog").close(); toast(message); await load();
  } catch (e) { $("form-error").textContent = e.message; $("form-error").hidden = false; }
  finally { submitting=false; $("submit").disabled=false; $("cancel").disabled=false; $("close").disabled=false; }
};
$("dialog").addEventListener("cancel",e=>{if(submitting)e.preventDefault();});
$("close").onclick = $("cancel").onclick = () => $("dialog").close();
document.querySelectorAll("[data-view]").forEach(button => button.onclick = () => {
  state.view = button.dataset.view;
  document.querySelectorAll("[data-view]").forEach(b=>{b.classList.toggle("active",b===button);b.setAttribute("aria-current",b===button?"page":"false");});
  const info = labels[state.view];
  $("title").textContent = $("breadcrumb").textContent = info[0]; $("subtitle").textContent=info[1];
  $("create").textContent="＋ "+info[2]; $("list-title").textContent=info[3];
  $("create").hidden = state.me.rol !== "ADMIN" && !(state.me.rol === "USUARIO" && state.view === "turnos");
  $("list-description").textContent=state.view==="turnos"?"Una vista clara de cada paso del proceso.":"Registros del centro de atención.";
  $("filter").hidden = state.view !== "turnos"; $("search").value="";
  $("search").placeholder=state.view==="turnos"?"Buscar por número o usuario…":"Buscar registros…";
  render();
});
$("search").oninput = $("filter").onchange = render;
$("refresh").onclick = load;
$("date").textContent = new Date().toLocaleDateString("es",{day:"numeric",month:"long",year:"numeric"});
$("logout").onclick = async () => {
  const button = $("logout");
  button.disabled = true; button.textContent = "Cerrando sesión…";
  try {
    await Auth.logout();
    state.me = null;
    ["turnos","usuarios","servicios","funcionarios","cuentas"].forEach(key => state[key] = []);
    $("content").replaceChildren();
    document.body.classList.add("checking-session");
    location.replace("/login.html?salida=1");
  } catch(e) {
    if (e.status === 401) {
      document.body.classList.add("checking-session");
      location.replace("/login.html?expirada=1");
      return;
    }
    toast("No se pudo cerrar la sesión. Reintenta cuando tengas conexión.");
    button.disabled = false; button.textContent = "Cerrar sesión ↗";
  }
};
window.addEventListener("pageshow", event => {
  if (event.persisted) {
    document.body.classList.add("checking-session");
    location.reload();
  }
});
async function initialize() {
  try {
    state.me = await api("/api/auth/me");
    await Auth.token();
    $("account-name").textContent = state.me.username;
    $("account-role").textContent = {ADMIN:"Administrador",FUNCIONARIO:"Funcionario",USUARIO:"Usuario"}[state.me.rol];
    if (state.me.rol !== "ADMIN") {
      document.querySelectorAll("[data-view]").forEach(button => {
        button.hidden = !["turnos","servicios"].includes(button.dataset.view);
      });
    }
    if (state.me.rol === "USUARIO") {
      labels.turnos = ["Mis turnos","Elige un servicio y sigue el estado de tu atención.","Solicitar turno","Mis solicitudes"];
      document.querySelector('[data-view="turnos"]').textContent = "◎ Mis turnos";
    }
    document.querySelector('[data-view="turnos"]').click();
    await load();
  } catch(e) {
    if (e.status !== 401) {
      $("connection").textContent=e.message; $("connection").hidden=false;
    }
  }
}
initialize();
