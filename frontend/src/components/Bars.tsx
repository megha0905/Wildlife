import { label } from './ui';

/** Horizontal bar list for count maps (e.g. incidents by type). Plain CSS, no chart library. */
export function Bars({ data, colors, max }: { data: Record<string, number>; colors?: Record<string, string>; max?: number }) {
  const entries = Object.entries(data);
  const top = max ?? Math.max(1, ...entries.map(([, v]) => v));
  return (
    <ul className="bars">
      {entries.map(([k, v]) => (
        <li key={k}>
          <span className="bar-label">{label(k)}</span>
          <span className="bar-track">
            <span className="bar-fill" style={{ width: `${(v / top) * 100}%`, background: colors?.[k] }} />
          </span>
          <span className="bar-value">{v}</span>
        </li>
      ))}
    </ul>
  );
}

/** Thin progress bar with a percentage. */
export function Progress({ percent }: { percent: number }) {
  const p = Math.max(0, Math.min(100, percent));
  return (
    <span className="progress" title={`${p}%`}>
      <span className="progress-track"><span className="progress-fill" style={{ width: `${p}%` }} /></span>
      <span className="progress-value">{p.toFixed(0)}%</span>
    </span>
  );
}
