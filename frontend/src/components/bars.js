// Horizontal bar list and progress bar. Plain HTML and CSS, no chart library.
import { h } from './dom.js';
import { label } from './ui.js';

/** Bars for a count map, e.g. { POACHING: 3, FOREST_FIRE: 1 }. */
export function bars(data, colors, max) {
  const entries = Object.entries(data ?? {});
  const top = max ?? Math.max(1, ...entries.map(([, v]) => v));
  return h('ul', { class: 'bars' }, entries.map(([k, v]) => h('li', null,
    h('span', { class: 'bar-label' }, label(k)),
    h('span', { class: 'bar-track' },
      h('span', { class: 'bar-fill', style: { width: `${(v / top) * 100}%`, background: colors?.[k] ?? '' } })),
    h('span', { class: 'bar-value' }, v))));
}

/** Thin progress bar with a percentage. */
export function progress(percent) {
  const p = Math.max(0, Math.min(100, percent ?? 0));
  return h('span', { class: 'progress', title: `${p}%` },
    h('span', { class: 'progress-track' }, h('span', { class: 'progress-fill', style: { width: `${p}%` } })),
    h('span', { class: 'progress-value' }, `${p.toFixed(0)}%`));
}
