import type { ReactNode } from 'react';
import type { Page } from '../api/types';

export interface Column<T> {
  header: string;
  render: (row: T) => ReactNode;
  className?: string;
}

interface Props<T> {
  columns: Column<T>[];
  rows: T[];
  rowKey: (row: T) => string | number;
  onRowClick?: (row: T) => void;
  /** Optional coloured strip on the left of each row (e.g. IUCN colour). */
  stripe?: (row: T) => string | undefined;
  empty?: ReactNode;
}

export function DataTable<T>({ columns, rows, rowKey, onRowClick, stripe, empty }: Props<T>) {
  if (rows.length === 0) return <div className="table-wrap">{empty}</div>;
  return (
    <div className="table-wrap">
      <table className="data">
        <thead>
          <tr>
            {stripe && <th className="stripe-cell" aria-hidden="true" />}
            {columns.map((c) => <th key={c.header} className={c.className}>{c.header}</th>)}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr
              key={rowKey(row)}
              className={onRowClick ? 'clickable' : undefined}
              tabIndex={onRowClick ? 0 : undefined}
              onClick={onRowClick ? () => onRowClick(row) : undefined}
              onKeyDown={onRowClick ? (e) => { if (e.key === 'Enter') onRowClick(row); } : undefined}
            >
              {stripe && <td className="stripe-cell"><span className="stripe" style={{ background: stripe(row) }} /></td>}
              {columns.map((c) => <td key={c.header} className={c.className}>{c.render(row)}</td>)}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export function Pager<T>({ page, onPage }: { page: Page<T>; onPage: (p: number) => void }) {
  if (page.totalPages <= 1) return null;
  const from = page.page * page.size + 1;
  const to = from + page.content.length - 1;
  return (
    <div className="pager">
      <span>{from}–{to} of {page.totalElements}</span>
      <div>
        <button className="btn btn-secondary" disabled={page.page === 0} onClick={() => onPage(page.page - 1)}>Previous</button>
        <button className="btn btn-secondary" disabled={page.page + 1 >= page.totalPages} onClick={() => onPage(page.page + 1)}>Next</button>
      </div>
    </div>
  );
}
