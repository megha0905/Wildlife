// Small presentational helpers shared by every page.
import { ApiError } from '../api/client.js';
import { h, mount } from './dom.js';

// ---------------------------------------------------------------- formatting

/** "UNDER_TREATMENT" -> "Under treatment" */
export function label(value) {
  if (!value) return '—';
  const s = String(value).replace(/_/g, ' ').toLowerCase();
  return s.charAt(0).toUpperCase() + s.slice(1);
}

export function fmtDate(iso) {
  if (!iso) return '—';
  const d = new Date(iso);
  return isNaN(d.getTime()) ? iso : d.toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' });
}

export function fmtDateTime(iso) {
  if (!iso) return '—';
  const d = new Date(iso);
  return isNaN(d.getTime()) ? iso : d.toLocaleString(undefined, { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' });
}

export function fmtAgo(iso) {
  if (!iso) return 'never';
  const days = Math.floor((Date.now() - new Date(iso).getTime()) / 86_400_000);
  if (days <= 0) return 'today';
  if (days === 1) return 'yesterday';
  if (days < 60) return `${days} days ago`;
  return `${Math.round(days / 30)} months ago`;
}

export function fmtCoord(lat, lng) {
  if (lat == null || lng == null) return '—';
  return `${lat.toFixed(4)}, ${lng.toFixed(4)}`;
}

export function fmtInr(n) {
  if (n == null) return '—';
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(n);
}

/** Local "yyyy-MM-ddTHH:mm" for datetime-local inputs. */
export function nowLocalInput(date = new Date()) {
  const p = (n) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${p(date.getMonth() + 1)}-${p(date.getDate())}T${p(date.getHours())}:${p(date.getMinutes())}`;
}

export function todayIso(date = new Date()) {
  return nowLocalInput(date).slice(0, 10);
}

// ---------------------------------------------------------------- colours

/** Official IUCN Red List category colours. */
export const IUCN = {
  CRITICALLY_ENDANGERED: { code: 'CR', color: '#d81e05', ink: '#fff' },
  ENDANGERED: { code: 'EN', color: '#fc7f3f', ink: '#fff' },
  VULNERABLE: { code: 'VU', color: '#f9e814', ink: '#3a3300' },
  NEAR_THREATENED: { code: 'NT', color: '#cce226', ink: '#2e3800' },
  LEAST_CONCERN: { code: 'LC', color: '#60c659', ink: '#fff' },
  EXTINCT_IN_THE_WILD: { code: 'EW', color: '#542344', ink: '#fff' },
  DATA_DEFICIENT: { code: 'DD', color: '#a3a3a3', ink: '#fff' },
  NOT_EVALUATED: { code: 'NE', color: '#d4d4d4', ink: '#333' },
};

/** Muted semantic colours for statuses so they never compete with the IUCN strip. */
const TONE = {
  HEALTHY: 'good', RECOVERING: 'good', INJURED: 'bad', SICK: 'warn', UNDER_TREATMENT: 'info', DECEASED: 'dark', UNKNOWN: 'muted',
  IN_WILD: 'good', IN_RESCUE: 'info', TRANSFERRED: 'muted', MISSING: 'warn',
  REPORTED: 'warn', VERIFIED: 'info', IN_PROGRESS: 'info', RESOLVED: 'good', CLOSED: 'muted',
  LOW: 'muted', MEDIUM: 'warn', HIGH: 'bad', CRITICAL: 'bad',
  REGISTERED: 'warn', REHABILITATING: 'info', RELEASED: 'good',
  SCHEDULED: 'info', COMPLETED: 'good', CANCELLED: 'muted', MISSED: 'warn', PLANNED: 'info', ACTIVE: 'good',
  ON_HOLD: 'warn', NOT_STARTED: 'muted', DONE: 'good', BLOCKED: 'bad',
  AVAILABLE: 'good', IN_USE: 'info', MAINTENANCE: 'warn', RETIRED: 'muted', ON_LEAVE: 'warn', INACTIVE: 'muted',
  GOOD: 'good', FAIR: 'warn', DEGRADED: 'bad', DRY: 'bad', CONTAMINATED: 'bad',
};

export function chip(value) {
  if (!value) return h('span', { class: 'muted' }, '—');
  return h('span', { class: `chip tone-${TONE[value] ?? 'muted'}` }, label(value));
}

export function iucnBadge(status, short = false) {
  const m = IUCN[status] ?? IUCN.NOT_EVALUATED;
  return h('span', { class: 'iucn', title: label(status) },
    h('span', { class: 'iucn-code', style: { background: m.color, color: m.ink } }, m.code),
    !short && label(status));
}

// ---------------------------------------------------------------- layout bits

export function pageHeader(title, intro, actions) {
  return h('div', { class: 'page-head' },
    h('div', null, h('h1', null, title), intro && h('p', null, intro)),
    actions && h('div', { class: 'page-actions' }, actions));
}

/** Tab strip. onChange(id) is called when a tab is clicked. */
export function tabs(items, value, onChange) {
  return h('div', { class: 'tabs', role: 'tablist' },
    items.map((t) => h('button', { role: 'tab', 'aria-selected': String(value === t.id), onClick: () => onChange(t.id) }, t.label)));
}

export function warnings(items) {
  if (!items || items.length === 0) return null;
  return h('div', { class: 'warnings', role: 'status' }, items.map((w) => h('div', null, w)));
}

export function errorBanner(error) {
  if (!error) return null;
  const message = error instanceof Error ? error.message : String(error);
  const details = error instanceof ApiError ? error.details : [];
  return h('div', { class: 'error-banner', role: 'alert' },
    h('div', null, message),
    details.length > 0 && h('ul', null, details.map((d) => h('li', null, d))));
}

export function empty(title, ...children) {
  return h('div', { class: 'empty' }, h('strong', null, title), ...children);
}

export function loading(text = 'Loading…') {
  return h('div', { class: 'loading' }, text);
}

export function fact(term, content, wide = false) {
  return h('div', { class: wide ? 'fact wide' : 'fact' }, h('dt', null, term), h('dd', null, content));
}

// ---------------------------------------------------------------- data loading

/**
 * A piece of the page that shows data from the server.
 * loader() returns a promise; render(data, reload) returns the content to show.
 * The old content stays visible while reloading. Returns { el, reload }.
 */
export function region(loader, render) {
  const el = h('div', { class: 'region' });
  let token = 0;
  const reload = () => {
    const mine = ++token;
    if (!el.hasChildNodes()) mount(el, loading());
    loader()
      .then((data) => { if (mine === token) mount(el, render(data, reload)); })
      .catch((err) => { if (mine === token) mount(el, errorBanner(err)); });
  };
  reload();
  return { el, reload };
}

/** Calls fn at most once per `ms` while the user types (used by search boxes). */
export function debounce(fn, ms = 300) {
  let t;
  return (...args) => { clearTimeout(t); t = setTimeout(() => fn(...args), ms); };
}

// ---------------------------------------------------------------- toolbar inputs

/**
 * A <select> for toolbars. options: [{ value, label }]. placeholder: text of the "all" option ('' value).
 * onChange(value) gets the chosen value as a string.
 */
export function selectInput({ value = '', options, placeholder, ariaLabel, onChange }) {
  return h('select', { class: 'input', 'aria-label': ariaLabel, value: String(value), onChange: (e) => onChange(e.target.value) },
    placeholder != null && h('option', { value: '' }, placeholder),
    options.map((o) => h('option', { value: String(o.value) }, o.label)));
}

/** Enum values -> select options with readable labels. */
export const enumOptions = (values) => values.map((v) => ({ value: v, label: label(v) }));

/** A search box that calls onSearch(text) shortly after the user stops typing. */
export function searchInput({ placeholder, ariaLabel, value = '', onSearch }) {
  const fire = debounce(onSearch, 300);
  return h('input', { class: 'input search', placeholder, 'aria-label': ariaLabel, value, onInput: (e) => fire(e.target.value) });
}

/** Page with tabs: items [{ id, label, render }]. Returns the tab strip + the active tab's content. */
export function tabbed(items, startId = items[0].id) {
  const bar = h('div');
  const body = h('div');
  const show = (id) => {
    mount(bar, tabs(items, id, show));
    mount(body, items.find((t) => t.id === id).render());
  };
  show(startId);
  return [bar, body];
}
