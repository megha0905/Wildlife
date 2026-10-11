// Minimal fetch wrapper: adds the JWT, turns error bodies into ApiError, logs out on 401.

// Empty = same address as the page. server.js forwards /api/... to the gateway.
export const API_BASE_URL = '';

const TOKEN_KEY = 'wildguard.token';

export class ApiError extends Error {
  constructor(status, message, details = []) {
    super(message);
    this.status = status;
    this.details = details;
  }
}

export function getToken() {
  try {
    return localStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
}

export function setToken(token) {
  try {
    if (token) localStorage.setItem(TOKEN_KEY, token);
    else localStorage.removeItem(TOKEN_KEY);
  } catch {
    /* storage unavailable: token lives only for this page load */
  }
}

/** Fired when any call returns 401, so the app can return to the login screen. */
export const UNAUTHORIZED_EVENT = 'wildguard:unauthorized';

function buildUrl(path, query) {
  const url = new URL(API_BASE_URL + path, window.location.origin);
  if (query) {
    for (const [k, v] of Object.entries(query)) {
      if (v !== undefined && v !== null && v !== '') url.searchParams.set(k, String(v));
    }
  }
  return url.toString();
}

async function request(method, path, body, query) {
  const headers = {};
  const token = getToken();
  if (token) headers.Authorization = `Bearer ${token}`;
  if (body !== undefined) headers['Content-Type'] = 'application/json';

  let res;
  try {
    res = await fetch(buildUrl(path, query), {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError(0, "Can't reach the WildGuard server. Is it running?");
  }

  if (res.status === 204) return undefined;
  const text = await res.text();
  let data = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = null;
    }
  }

  if (!res.ok) {
    const err = data ?? {};
    if (res.status === 401 && path !== '/api/auth/login') {
      window.dispatchEvent(new Event(UNAUTHORIZED_EVENT));
    }
    throw new ApiError(res.status, err.message || `Request failed (${res.status})`, err.details ?? []);
  }
  return data;
}

export const http = {
  get: (path, query) => request('GET', path, undefined, query),
  post: (path, body) => request('POST', path, body ?? {}),
  put: (path, body) => request('PUT', path, body),
  patch: (path, body) => request('PATCH', path, body),
  del: (path) => request('DELETE', path),
};
