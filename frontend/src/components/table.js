// Data table and pager used by the list screens.
import { h } from './dom.js';

/**
 * columns: [{ header, render(row), className }]
 * onRowClick(row): optional; makes rows clickable (and Enter-able).
 * stripe(row): optional colour for a strip on the left of each row (e.g. IUCN colour).
 * empty: element shown when there are no rows.
 */
export function dataTable({ columns, rows, onRowClick, stripe, empty }) {
  if (rows.length === 0) return h('div', { class: 'table-wrap' }, empty);
  return h('div', { class: 'table-wrap' },
    h('table', { class: 'data' },
      h('thead', null, h('tr', null,
        stripe && h('th', { class: 'stripe-cell', 'aria-hidden': 'true' }),
        columns.map((c) => h('th', { class: c.className }, c.header)))),
      h('tbody', null, rows.map((row) => h('tr', {
        class: onRowClick ? 'clickable' : undefined,
        tabindex: onRowClick ? 0 : undefined,
        onClick: onRowClick ? () => onRowClick(row) : undefined,
        onKeydown: onRowClick ? (e) => { if (e.key === 'Enter') onRowClick(row); } : undefined,
      },
        stripe && h('td', { class: 'stripe-cell' }, h('span', { class: 'stripe', style: { background: stripe(row) } })),
        columns.map((c) => h('td', { class: c.className }, c.render(row))))))));
}

/** "1–20 of 57" with Previous / Next. Hidden when everything fits on one page. */
export function pager(page, onPage) {
  if (!page || page.totalPages <= 1) return null;
  const from = page.page * page.size + 1;
  const to = from + page.content.length - 1;
  return h('div', { class: 'pager' },
    h('span', null, `${from}–${to} of ${page.totalElements}`),
    h('div', null,
      h('button', { class: 'btn btn-secondary', disabled: page.page === 0, onClick: () => onPage(page.page - 1) }, 'Previous'),
      h('button', { class: 'btn btn-secondary', disabled: page.page + 1 >= page.totalPages, onClick: () => onPage(page.page + 1) }, 'Next')));
}
