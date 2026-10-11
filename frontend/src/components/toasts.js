// Short pop-up messages in the corner: toast('Saved'), toast(message, 'error'), toast(warning, 'warn').
import { h } from './dom.js';

let box = null;

export function toast(text, kind = 'info') {
  if (!box || !box.isConnected) {
    box = h('div', { class: 'toasts', role: 'status', 'aria-live': 'polite' });
    document.body.append(box);
  }
  const item = h('div', { class: `toast toast-${kind}` }, text);
  box.append(item);
  setTimeout(() => item.remove(), kind === 'info' ? 3500 : 7000);
}
