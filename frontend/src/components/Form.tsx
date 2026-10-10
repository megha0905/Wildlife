// Declarative form used by every "create / edit" modal. Keeps pages short and consistent.
import { useState, type FormEvent } from 'react';
import { ApiError } from '../api/client';
import { Modal } from './Modal';
import { label } from './ui';

export type FieldType = 'text' | 'number' | 'date' | 'datetime' | 'time' | 'select' | 'textarea' | 'checkbox' | 'ids';

export interface Field {
  name: string;
  label: string;
  type?: FieldType;
  required?: boolean;
  options?: { value: string | number; label: string }[];
  /** Shorthand: enum values become options with readable labels. */
  enumValues?: readonly string[];
  placeholder?: string;
  hint?: string;
  wide?: boolean;
  step?: string;
}

export type FormValues = Record<string, unknown>;

function toInput(field: Field, v: unknown): string | boolean {
  if (field.type === 'checkbox') return v === true;
  if (v === null || v === undefined) return '';
  if (field.type === 'datetime' && typeof v === 'string') return v.slice(0, 16);
  if (field.type === 'time' && typeof v === 'string') return v.slice(0, 5);
  if (field.type === 'ids' && Array.isArray(v)) return v.join(', ');
  return String(v);
}

function fromInput(field: Field, raw: string | boolean): unknown {
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

interface FormModalProps {
  title: string;
  open: boolean;
  fields: Field[];
  /** Starting values; any object whose keys match field names (e.g. an existing record). */
  initial?: object;
  submitLabel: string;
  onClose: () => void;
  /** Return normally to close the modal; throw to keep it open and show the error. */
  onSubmit: (values: FormValues) => Promise<void>;
  intro?: string;
}

export function FormModal({ title, open, fields, initial, submitLabel, onClose, onSubmit, intro }: FormModalProps) {
  const start = (initial ?? {}) as Record<string, unknown>;
  const initialState = () => Object.fromEntries(fields.map((f) => [f.name, toInput(f, start[f.name])]));
  const [state, setState] = useState<Record<string, string | boolean>>(initialState);
  const [error, setError] = useState<ApiError | Error | null>(null);
  const [saving, setSaving] = useState(false);
  const [lastOpen, setLastOpen] = useState(false);

  // reset each time the modal opens
  if (open && !lastOpen) {
    setLastOpen(true);
    setState(initialState());
    setError(null);
  } else if (!open && lastOpen) {
    setLastOpen(false);
  }

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      const values: FormValues = {};
      for (const f of fields) values[f.name] = fromInput(f, state[f.name]);
      await onSubmit(values);
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err : new Error(String(err)));
    } finally {
      setSaving(false);
    }
  };

  const fieldErrors = error instanceof ApiError ? error.details : [];

  return (
    <Modal
      title={title}
      open={open}
      onClose={onClose}
      footer={
        <>
          <button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button>
          <button type="submit" form="form-modal" className="btn btn-primary" disabled={saving}>
            {saving ? 'Saving…' : submitLabel}
          </button>
        </>
      }
    >
      {intro && <p className="form-intro">{intro}</p>}
      {error && (
        <div className="error-banner" role="alert">
          <div>{error.message}</div>
          {fieldErrors.length > 0 && <ul>{fieldErrors.map((d) => <li key={d}>{d}</li>)}</ul>}
        </div>
      )}
      <form id="form-modal" className="form-grid" onSubmit={submit} noValidate>
        {fields.map((f) => {
          const id = `f-${f.name}`;
          const value = state[f.name];
          const set = (v: string | boolean) => setState((s) => ({ ...s, [f.name]: v }));
          const options = f.options ?? f.enumValues?.map((v) => ({ value: v, label: label(v) }));
          if (f.type === 'checkbox') {
            return (
              <label key={f.name} className={`check ${f.wide ? 'wide' : ''}`}>
                <input type="checkbox" checked={value === true} onChange={(e) => set(e.target.checked)} />
                {f.label}
              </label>
            );
          }
          return (
            <div key={f.name} className={`field ${f.wide ? 'wide' : ''}`}>
              <label htmlFor={id}>{f.label}{f.required ? ' *' : ''}</label>
              {f.type === 'select' ? (
                <select id={id} className="input" value={String(value)} onChange={(e) => set(e.target.value)} required={f.required}>
                  {!f.required && <option value="">{f.placeholder ?? '—'}</option>}
                  {f.required && value === '' && <option value="" disabled>Choose…</option>}
                  {options?.map((o) => <option key={String(o.value)} value={String(o.value)}>{o.label}</option>)}
                </select>
              ) : f.type === 'textarea' ? (
                <textarea id={id} className="input" rows={3} value={String(value)} onChange={(e) => set(e.target.value)} />
              ) : (
                <input
                  id={id}
                  className="input"
                  type={f.type === 'datetime' ? 'datetime-local' : f.type === 'ids' ? 'text' : f.type ?? 'text'}
                  step={f.step}
                  value={String(value)}
                  placeholder={f.placeholder}
                  required={f.required}
                  onChange={(e) => set(e.target.value)}
                />
              )}
              {f.hint && <span className="hint">{f.hint}</span>}
            </div>
          );
        })}
      </form>
    </Modal>
  );
}
