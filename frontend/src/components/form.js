// Declarative form used by every "create / edit" modal. Keeps pages short and consistent.
//
// openFormModal({ title, fields, initial, submitLabel, intro, onSubmit })
//   fields: [{ name, label, type, required, options, enumValues, placeholder, hint, wide, step }]
//     type: text | number | date | datetime | time | select | textarea | checkbox | ids
//   onSubmit(values): return normally to close the modal; throw to keep it open and show the error.
import { ApiError } from '../api/client.js';
import { h, mount } from './dom.js';
import { openModal } from './modal.js';
import { label } from './ui.js';

function toInput(field, v) {
  if (field.type === 'checkbox') return v === true;
  if (v === null || v === undefined) return '';
  if (field.type === 'datetime' && typeof v === 'string') return v.slice(0, 16);
  if (field.type === 'time' && typeof v === 'string') return v.slice(0, 5);
  if (field.type === 'ids' && Array.isArray(v)) return v.join(', ');
  return String(v);
}

function fromInput(field, raw) {
  if (field.type === 'checkbox') return raw === true;
  const s = String(raw).trim();
  if (s === '') return field.type === 'ids' ? [] : null;
  switch (field.type) {
    case 'number': return Number(s);
    case 'datetime': return s.length === 16 ? `${s}:00` : s;
    case 'ids': return s.split(/[\s,]+/).filter(Boolean).map(Number).filter((n) => !isNaN(n));
    case 'select': {
      const opt = field.options?.find((o) => String(o.value) === s);
      return opt ? opt.value : s;
    }
    default: return s;
  }
}

export function openFormModal({ title, fields, initial = {}, submitLabel, intro, onSubmit }) {
  const inputs = {};
  const errorBox = h('div');

  const form = h('form', { id: 'form-modal', class: 'form-grid', novalidate: true },
    fields.map((f) => {
      const id = `f-${f.name}`;
      const value = toInput(f, initial[f.name]);
      if (f.type === 'checkbox') {
        const input = h('input', { type: 'checkbox', checked: value === true });
        inputs[f.name] = input;
        return h('label', { class: `check ${f.wide ? 'wide' : ''}` }, input, f.label);
      }
      const options = f.options ?? f.enumValues?.map((v) => ({ value: v, label: label(v) }));
      let input;
      if (f.type === 'select') {
        input = h('select', { id, class: 'input', required: f.required, value },
          !f.required && h('option', { value: '' }, f.placeholder ?? '—'),
          f.required && value === '' && h('option', { value: '', disabled: true }, 'Choose…'),
          (options ?? []).map((o) => h('option', { value: String(o.value) }, o.label)));
      } else if (f.type === 'textarea') {
        input = h('textarea', { id, class: 'input', rows: 3, value });
      } else {
        const type = f.type === 'datetime' ? 'datetime-local' : f.type === 'ids' ? 'text' : f.type ?? 'text';
        input = h('input', { id, class: 'input', type, step: f.step, value, placeholder: f.placeholder, required: f.required });
      }
      inputs[f.name] = input;
      return h('div', { class: `field ${f.wide ? 'wide' : ''}` },
        h('label', { for: id }, f.label + (f.required ? ' *' : '')),
        input,
        f.hint && h('span', { class: 'hint' }, f.hint));
    }));

  const submitBtn = h('button', { type: 'submit', form: 'form-modal', class: 'btn btn-primary' }, submitLabel);
  const modal = openModal({
    title,
    body: h('div', null, intro && h('p', { class: 'form-intro' }, intro), errorBox, form),
    footer: [
      h('button', { type: 'button', class: 'btn btn-secondary', onClick: () => modal.close() }, 'Cancel'),
      submitBtn,
    ],
  });

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    submitBtn.disabled = true;
    submitBtn.textContent = 'Saving…';
    mount(errorBox);
    try {
      const values = {};
      for (const f of fields) {
        const el = inputs[f.name];
        values[f.name] = fromInput(f, f.type === 'checkbox' ? el.checked : el.value);
      }
      await onSubmit(values);
      modal.close();
    } catch (err) {
      const details = err instanceof ApiError ? err.details : [];
      mount(errorBox, h('div', { class: 'error-banner', role: 'alert' },
        h('div', null, err instanceof Error ? err.message : String(err)),
        details.length > 0 && h('ul', null, details.map((d) => h('li', null, d)))));
      submitBtn.disabled = false;
      submitBtn.textContent = submitLabel;
    }
  });
  return modal;
}
