"use strict";
const Auth = {
  csrf: null,
  async token() {
    const response = await fetch("/api/auth/csrf", {cache:"no-store"});
    if (!response.ok) {
      const error = new Error("No se pudo actualizar la sesión.");
      error.status = response.status;
      throw error;
    }
    this.csrf = await response.json();
  },
  async request(path, method = "GET", body, form = false) {
    if (method !== "GET" && !this.csrf) await this.token();
    const headers = {};
    if (method !== "GET") headers[this.csrf.headerName] = this.csrf.token;
    if (body !== undefined) headers["Content-Type"] = form ? "application/x-www-form-urlencoded" : "application/json";
    const response = await fetch(path, {method, headers, cache:"no-store",
      body: body === undefined ? undefined : form ? new URLSearchParams(body) : JSON.stringify(body)});
    const data = await response.json().catch(() => ({}));
    if (!response.ok) {
      const error = new Error(data.errores?.join(". ") || data.detail || "No se pudo completar la operación.");
      error.status = response.status; throw error;
    }
    return data;
  },
  async logout() {
    // Obtener un token actual permite cerrar incluso si la sesión anterior expiró.
    await this.token();
    try { await this.request("/api/auth/logout", "POST"); }
    catch (error) {
      if (error.status !== 403) throw error;
      await this.token();
      await this.request("/api/auth/logout", "POST");
    }
    this.csrf = null;
  }
};
