// Mandatory example: a forest officer reviews all incidents in a protected zone for the current month.
// Uses GET /api/incidents/zone/{zoneId}/current-month?status= (CONTRACTS.md 7.6).
import { useState } from 'react';
import { incidentApi } from '../api/endpoints';
import { useLookups } from '../api/lookups';
import type { IncidentStatus, MonthlyIncidentItem } from '../api/types';
import { DataTable } from '../components/DataTable';
import { MapView } from '../components/MapView';
import { Chip, Empty, ErrorBanner, Loading, PageHeader, Warnings, fmtCoord, fmtDateTime, label, useLoad } from '../components/ui';
import { IncidentDrawer, STATUSES } from './IncidentsPage';

export function ZoneMonthlyPage() {
  const { zones, ready } = useLookups();
  const [zoneId, setZoneId] = useState<number | null>(null);
  const [status, setStatus] = useState<IncidentStatus | ''>('');
  const [selected, setSelected] = useState<number | null>(null);
  const effectiveZone = zoneId ?? zones[0]?.id ?? null;

  const report = useLoad(
    () => (effectiveZone == null ? Promise.resolve(null) : incidentApi.zoneCurrentMonth(effectiveZone, status)),
    [effectiveZone, status],
  );
  const r = report.data;
  const monthName = r ? new Date(r.periodStart).toLocaleDateString(undefined, { month: 'long', year: 'numeric' }) : '';

  return (
    <>
      <PageHeader
        title="Monthly zone review"
        intro="Every incident in one protected zone this month, with its status, location and the people assigned to it."
      />
      <div className="toolbar">
        <label className="inline-label" htmlFor="zone">Zone</label>
        <select id="zone" className="input" value={effectiveZone ?? ''} onChange={(e) => setZoneId(Number(e.target.value))} disabled={!ready}>
          {zones.map((z) => <option key={z.id} value={z.id}>{z.name} ({z.areaName})</option>)}
        </select>
      </div>

      <ErrorBanner error={report.error} />
      {report.loading && !r ? <Loading /> : r && (
        <>
          <Warnings items={r.warnings} />
          <div className="review-head">
            <div>
              <div className="review-zone">{r.zoneName ?? `Zone ${r.zoneId}`}</div>
              <div className="muted">{r.areaName ?? ''}{r.areaName ? ' · ' : ''}{monthName}</div>
            </div>
            <div className="review-total"><span>{r.totalIncidents}</span> incident{r.totalIncidents === 1 ? '' : 's'} this month</div>
          </div>

          {/* Status summary doubles as the filter: counts always cover the whole month */}
          <div className="status-filter" role="group" aria-label="Filter by status">
            <button className={status === '' ? 'active' : ''} onClick={() => setStatus('')}>
              <span className="sf-n">{r.totalIncidents}</span><span className="sf-l">All</span>
            </button>
            {STATUSES.map((s) => (
              <button key={s} className={status === s ? 'active' : ''} onClick={() => setStatus(s)} aria-pressed={status === s}>
                <span className="sf-n">{r.statusSummary[s] ?? 0}</span><span className="sf-l">{label(s)}</span>
              </button>
            ))}
          </div>

          {r.incidents.length > 0 && (
            <MapView height={260} markers={r.incidents.map((i) => ({
              id: i.id, lat: i.latitude, lng: i.longitude, label: `${i.incidentCode}: ${i.title}`,
              color: i.severity === 'CRITICAL' || i.severity === 'HIGH' ? '#a93a26' : '#d9a441', onClick: () => setSelected(i.id),
            }))} />
          )}

          <DataTable<MonthlyIncidentItem>
            rows={r.incidents}
            rowKey={(i) => i.id}
            onRowClick={(i) => setSelected(i.id)}
            empty={<Empty title={status ? `No ${label(status).toLowerCase()} incidents this month` : 'No incidents in this zone this month'}>
              {status ? 'Choose another status above.' : 'A quiet month so far.'}</Empty>}
            columns={[
              { header: 'Incident', render: (i) => <><strong>{i.incidentCode}</strong><div className="muted small">{i.title}</div></> },
              { header: 'Type', render: (i) => label(i.incidentType) },
              { header: 'Severity', render: (i) => <Chip value={i.severity} /> },
              { header: 'Status', render: (i) => <Chip value={i.status} /> },
              { header: 'Occurred', render: (i) => fmtDateTime(i.occurredAt) },
              { header: 'Location', render: (i) => <>{fmtCoord(i.latitude, i.longitude)}{i.locationDescription && <div className="muted small">{i.locationDescription}</div>}</> },
              { header: 'Assigned personnel', render: (i) => i.assignedPersonnel.length === 0
                  ? <span className="muted">Unassigned</span>
                  : i.assignedPersonnel.map((p) => (
                    <div key={p.assignmentId}>{p.staffName ?? `Staff ${p.staffId}`} <span className="muted small">({label(p.assignmentRole).toLowerCase()})</span></div>
                  )) },
            ]}
          />
        </>
      )}
      {selected !== null && <IncidentDrawer id={selected} onClose={() => setSelected(null)} onChanged={report.reload} />}
    </>
  );
}
