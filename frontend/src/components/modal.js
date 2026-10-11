// Native <dialog> modal and a slide-in drawer.
import { h } from './dom.js';

/**
 * Opens a modal. body and footer are elements. Returns { close }.
 * The dialog removes itself from the page when it closes (Escape, ✕ or close()).
 */
export function openModal({ title, body, footer, wide = false, onClose }) {
  const dlg = h('dialog', { class: wide ? 'modal wide' : 'modal' },
    h('div', { class: 'modal-head' },
      h('h2', null, title),
      h('button', { class: 'icon-btn', 'aria-label': 'Close', onClick: () => close() }, '✕')),
    h('div', { class: 'modal-body' }, body),
    footer && h('div', { class: 'modal-foot' }, footer));
  let closed = false;
  function close() {
    if (closed) return;
    closed = true;
    if (dlg.open) dlg.close();
    dlg.remove();
    onClose?.();
  }
  dlg.addEventListener('close', close);
  document.body.append(dlg);
  dlg.showModal();
  return { close };
}

/**
 * Opens the side panel used for record details. Returns { body, close }:
 * put the record's content into `body` (and replace it to refresh).
 */
export function openDrawer({ accent, onClose } = {}) {
  const body = h('div');
  const backdrop = h('div', { class: 'drawer-backdrop', onClick: () => close() });
  const panel = h('aside', { class: 'drawer', role: 'dialog', 'aria-modal': 'true' },
    accent && h('div', { class: 'drawer-accent', style: { background: accent } }),
    h('button', { class: 'icon-btn drawer-close', 'aria-label': 'Close', onClick: () => close() }, '✕'),
    body);
  const onKey = (e) => { if (e.key === 'Escape' && !document.querySelector('dialog[open]')) close(); };
  document.addEventListener('keydown', onKey);
  document.body.append(backdrop, panel);
  let closed = false;
  function close() {
    if (closed) return;
    closed = true;
    document.removeEventListener('keydown', onKey);
    backdrop.remove();
    panel.remove();
    onClose?.();
  }
  /** Changes the coloured strip at the top (e.g. once the record has loaded). */
  const setAccent = (color) => {
    let strip = panel.querySelector('.drawer-accent');
    if (!strip) { strip = h('div', { class: 'drawer-accent' }); panel.prepend(strip); }
    strip.style.background = color;
  };
  return { body, close, setAccent };
}
