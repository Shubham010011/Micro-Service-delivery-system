// Base URL of the API Gateway - the ONLY backend origin the frontend ever talks to.
const API_BASE = window.API_BASE || 'http://localhost:8080';

const Auth = {
  getToken() { return localStorage.getItem('rt_token'); },
  getUser() {
    const raw = localStorage.getItem('rt_user');
    return raw ? JSON.parse(raw) : null;
  },
  setSession(token, user) {
    localStorage.setItem('rt_token', token);
    localStorage.setItem('rt_user', JSON.stringify(user));
  },
  clear() {
    localStorage.removeItem('rt_token');
    localStorage.removeItem('rt_user');
  },
  requireRole(role) {
    const user = this.getUser();
    if (!this.getToken() || !user || user.role !== role) {
      window.location.href = 'index.html';
    }
    return user;
  }
};

async function api(path, { method = 'GET', body, auth = true } = {}) {
  const headers = { 'Content-Type': 'application/json' };
  if (auth && Auth.getToken()) headers['Authorization'] = `Bearer ${Auth.getToken()}`;

  const res = await fetch(`${API_BASE}${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined
  });

  let payload;
  try { payload = await res.json(); } catch { payload = null; }

  if (!res.ok || (payload && payload.success === false)) {
    const message = (payload && payload.message) || `Request failed (${res.status})`;
    throw new Error(message);
  }
  return payload ? payload.data : null;
}

function toast(message, type = 'info') {
  const el = document.createElement('div');
  el.className = `toast ${type}`;
  el.textContent = message;
  document.body.appendChild(el);
  setTimeout(() => el.remove(), 3800);
}

function logout() {
  Auth.clear();
  window.location.href = 'index.html';
}

function statusBadge(status) {
  const s = (status || '').toLowerCase();
  return `<span class="badge badge-${s}">${(status || '').replace(/_/g, ' ')}</span>`;
}

function timeAgo(iso) {
  if (!iso) return '';
  const diff = (Date.now() - new Date(iso).getTime()) / 1000;
  if (diff < 60) return 'just now';
  if (diff < 3600) return `${Math.floor(diff / 60)}m ago`;
  if (diff < 86400) return `${Math.floor(diff / 3600)}h ago`;
  return new Date(iso).toLocaleDateString();
}
