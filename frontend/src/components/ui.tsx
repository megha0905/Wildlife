// Small presentational helpers shared by every page.
import { useEffect, useState, type ReactNode } from 'react';
import { ApiError } from '../api/client';

// ---------------------------------------------------------------- formatting

/** "UNDER_TREATMENT" -> "Under treatment" */
export function label(value: string | null | undefined): string {
  if (!value) return '—';
  const s = value.replace(/_/g, ' ').toLowerCase();
  return s.charAt(0).toUpperCase() + s.slice(1);
}

export function fmtDate(iso: string | null | undefined): string {
  if (!iso) return '—';
  const d = new Date(iso);
  return isNaN(d.getTime()) ? iso : d.toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' });
}

export function fmtDateTime(iso: string | null | undefined): string {
  if (!iso) return '—';
  const d = new Date(iso);
  return isNaN(d.getTime()) ? iso : d.toLocaleString(undefined, { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' });
}

export function fmtAgo(iso: string | null | undefined): string {
  if (!iso) return 'never';
  const days = Math.floor((Date.now() - new Date(iso).getTime()) / 86_400_000);
  if (days <= 0) return 'today';
  if (days === 1) return 'yesterday';
  if (days < 60) return `${days} days ago`;
  return `${Math.round(days / 30)} months ago`;
}

export function fmtCoord(lat: number | null | undefined, lng: number | null | undefined): string {
  if (lat == null || lng == null) return '—';
  return `${lat.toFixed(4)}, ${lng.toFixed(4)}`;
}

export function fmtInr(n: number | null | undefined): string {
  if (n == null) return '—';
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(n);
}

/** Local "yyyy-MM-ddTHH:mm" for datetime-local inputs. */
export function nowLocalInput(date = new Date()): string {
  const p = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${p(date.getMonth() + 1)}-${p(date.getDate())}T${p(date.getHours())}:${p(date.getMinutes())}`;
}

export function todayIso(date = new Date()): string {
  return nowLocalInput(date).slice(0, 10);
}

// ---------------------------------------------------------------- colours

/** Official IUCN Red List category colours. */
export const IUCN: Record<string, { code: string; color: string; ink: string }> = {
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
const TONE: Record<string, string> = {
  // health
  HEALTHY: 'good', RECOVERING: 'good', INJURED: 'bad', SICK: 'warn', UNDER_TREATMENT: 'info', DECEASED: 'dark', UNKNOWN: 'muted',
  // animals
  IN_WILD: 'good', IN_RESCUE: 'info', TRANSFERRED: 'muted', MISSING: 'warn',
  // incidents
  REPORTED: 'warn', VERIFIED: 'info', IN_PROGRESS: 'info', RESOLVED: 'good', CLOSED: 'muted',
  // severity
  LOW: 'muted', MEDIUM: 'warn', HIGH: 'bad', CRITICAL: 'bad',
  // rescue
  REGISTERED: 'warn', REHABILITATING: 'info', RELEASED: 'good',
  // patrol / allocations / programmes
  SCHEDULED: 'info', COMPLETED: 'good', CANCELLED: 'muted', MISSED: 'warn', PLANNED: 'info', ACTIVE: 'good',
  ON_HOLD: 'warn', NOT_STARTED: 'muted', DONE: 'good', BLOCKED: 'bad',
  // resources
  AVAILABLE: 'good', IN_USE: 'info', MAINTENANCE: 'warn', RETIRED: 'muted', ON_LEAVE: 'warn', INACTIVE: 'muted',
  // habitat
  GOOD: 'good', FAIR: 'warn', DEGRADED: 'bad', DRY: 'bad', CONTAMINATED: 'bad',
};

export function Chip({ value }: { value: string | null | undefined }) {
  if (!value) return <span className="muted">—</span>;
  const tone = TONE[value] ?? 'muted';
  return <span className={`chip tone-${tone}`}>{label(value)}</span>;
}

export function IucnBadge({ status, short = false }: { status: string; short?: boolean }) {
  const m = IUCN[status] ?? IUCN.NOT_EVALUATED;
  return (
    <span className="iucn" title={label(status)}>
      <span className="iucn-code" style={{ background: m.color, color: m.ink }}>{m.code}</span>
      {!short && label(status)}
    </span>
  );
}

// ---------------------------------------------------------------- layout bits

export function PageHeader({ title, intro, actions }: { title: string; intro?: ReactNode; actions?: ReactNode }) {
  return (
    <div className="page-head">
      <div>
        <h1>{title}</h1>
        {intro && <p>{intro}</p>}
      </div>
      {actions && <div className="page-actions">{actions}</div>}
    </div>
  );
}

export function Tabs<T extends string>({ tabs, value, onChange }: {
  tabs: { id: T; label: string }[]; value: T; onChange: (id: T) => void;
}) {
  return (
    <div className="tabs" role="tablist">
      {tabs.map((t) => (
        <button key={t.id} role="tab" aria-selected={value === t.id} onClick={() => onChange(t.id)}>{t.label}</button>
      ))}
    </div>
  );
}

export function Warnings({ items }: { items?: string[] | null }) {
  if (!items || items.length === 0) return null;
  return (
    <div className="warnings" role="status">
      {items.map((w) => <div key={w}>{w}</div>)}
    </div>
  );
}

export function ErrorBanner({ error }: { error: unknown }) {
  if (!error) return null;
  const message = error instanceof Error ? error.message : String(error);
  const details = error instanceof ApiError ? error.details : [];
  return (
    <div className="error-banner" role="alert">
      <div>{message}</div>
      {details.length > 0 && <ul>{details.map((d) => <li key={d}>{d}</li>)}</ul>}
    </div>
  );
}

export function Empty({ title, children }: { title: string; children?: ReactNode }) {
  return (
    <div className="empty">
      <strong>{title}</strong>
      {children}
    </div>
  );
}

export function Loading({ text = 'Loading…' }: { text?: string }) {
  return <div className="loading">{text}</div>;
}

export function Fact({ term, children, wide }: { term: string; children: ReactNode; wide?: boolean }) {
  return (
    <div className={wide ? 'fact wide' : 'fact'}>
      <dt>{term}</dt>
      <dd>{children}</dd>
    </div>
  );
}

// ---------------------------------------------------------------- data loading hook

/** Runs an async loader and tracks loading/error. Call reload() after a change. */
export function useLoad<T>(loader: () => Promise<T>, deps: unknown[]) {
  const [data, setData] = useState<T | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [loading, setLoading] = useState(true);
  const [tick, setTick] = useState(0);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    loader()
      .then((d) => { if (!cancelled) setData(d); })
      .catch((e) => { if (!cancelled) setError(e); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, tick]);

  return { data, error, loading, reload: () => setTick((t) => t + 1), setData };
}
