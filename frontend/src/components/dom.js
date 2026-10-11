// Two small helpers that replace JSX: h() builds an element, mount() replaces an element's contents.
//
//   h('button', { class: 'btn', onClick: save }, 'Save')
//   h('p', null, 'Hello ', h('b', null, name))
//
// attrs: class, style (object), on<Event> handlers, value/checked, any other HTML attribute.
// children: strings, numbers, elements or arrays of them; null/false/true are skipped.

export function h(tag, attrs, ...children) {
  const el = document.createElement(tag);
  let value;
  if (attrs) {
    for (const [k, v] of Object.entries(attrs)) {
      if (v == null || v === false) continue;
      if (k === 'class') el.className = v;
      else if (k === 'style' && typeof v === 'object') Object.assign(el.style, v);
      else if (k.startsWith('on') && typeof v === 'function') el.addEventListener(k.slice(2).toLowerCase(), v);
      else if (k === 'value') value = v; // set after children, so <select> options exist first
      else if (k === 'checked') el.checked = Boolean(v);
      else if (v === true) el.setAttribute(k, '');
      else el.setAttribute(k, String(v));
    }
  }
  append(el, children);
  if (value !== undefined) el.value = value;
  return el;
}

function append(el, children) {
  for (const c of children.flat(Infinity)) {
    if (c == null || c === false || c === true) continue;
    el.append(c instanceof Node ? c : document.createTextNode(String(c)));
  }
}

/** Replaces everything inside el with the given children. */
export function mount(el, ...children) {
  el.replaceChildren();
  append(el, children);
  return el;
}

/** An inline SVG icon from one path (used by the sidebar). */
export function icon(d) {
  const ns = 'http://www.w3.org/2000/svg';
  const svg = document.createElementNS(ns, 'svg');
  svg.setAttribute('viewBox', '0 0 24 24');
  svg.setAttribute('aria-hidden', 'true');
  const p = document.createElementNS(ns, 'path');
  p.setAttribute('d', d);
  svg.append(p);
  return svg;
}
