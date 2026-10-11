// Who is logged in. Keeps the token in localStorage so a page refresh keeps you logged in.
import { authApi } from '../api/endpoints.js';
import { getToken, setToken, UNAUTHORIZED_EVENT } from '../api/client.js';

let currentUser = null;
const listeners = new Set();

function notify() {
  for (const fn of listeners) fn(currentUser);
}

/** Calls fn(user) whenever someone logs in or out. */
export function onAuthChange(fn) {
  listeners.add(fn);
}

export function getUser() {
  return currentUser;
}

/** True if the logged-in user has any of the given roles. */
export function hasRole(...roles) {
  return !!currentUser && currentUser.roles.some((r) => roles.includes(r));
}

export async function login(username, password) {
  const res = await authApi.login(username, password);
  setToken(res.accessToken);
  currentUser = res.user;
  notify();
}

export function logout() {
  setToken(null);
  currentUser = null;
  notify();
}

/** On page load: if a token is saved, ask the server who it belongs to. */
export async function restoreSession() {
  if (!getToken()) return;
  try {
    currentUser = await authApi.me();
  } catch {
    setToken(null);
    currentUser = null;
  }
}

// Any 401 from the API ends the session
window.addEventListener(UNAUTHORIZED_EVENT, () => { if (currentUser) logout(); });
