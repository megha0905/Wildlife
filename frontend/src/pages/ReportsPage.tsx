import { useState } from 'react';
import { reportApi } from '../api/endpoints';
import { useLookups } from '../api/lookups';
import { Bars } from '../components/Bars';
import { MapView } from '../components/MapView';
import { Chip, Empty, ErrorBanner, Loading, PageHeader, Warnings, label, useLoad } from '../components/ui';

export function ReportsPage() {
  const { zones } = useLookups();
  const now = new Date();
  const [year, setYear] = useState(now.getFullYear());
  const [month, setMonth] = useState(now.getMonth() + 1);
  const [zoneId, setZoneId] = useState<number | null>(null);
  const zone = zoneId ?? zones[0]?.id ?? null;

  const monthly = useLoad(() => reportApi.monthlyIncidents(year, month), [year, month]);
  const zoneReport = useLoad(() => (zone == null ? Promise.resolve(null) : reportApi.zone(zone)), [zone]);
  const m = monthly.data;
  const z = zoneReport.data;

  return (
    <>
      <PageHeader title="Reports" intro="Incident trends by month, and a combined profile of one zone drawn from several services." />

      <section className="panel">
        <div className="panel-head">
          <h2>Incidents by month</h2>
          <div className="toolbar compact">
            <select className="input" value={month} onChange={(e) => setMonth(Number(e.target.value))} aria-label="Month">
              {Array.from({ length: 12 }, (_, i) => (
                <option key={i + 1} value={i + 1}>{new Date(2000, i, 1).toLocaleString(undefined, { month: 'long' })}</option>
              ))}
            </select>
            <input className="input year" type="number" value={year} min={2000} max={2100} onChange={(e) => setYear(Number(e.target.value))} aria-label="Year" />
          </div>
        </div>
        <ErrorBanner error={monthly.error} />
        {monthly.loading && !m ? <Loading /> : m && (
          <>
            <Warnings items={m.warnings} />
            {m.totalThisMonth === undefined ? <Empty title="No data for this month" /> : (
              <div className="grid-3">
                <div><div className="big-number">{m.totalThisMonth}</div><div className="muted">incidents that month</div></div>
                <div><h3>By status</h3>{m.byStatus && <Bars data={m.byStatus} />}</div>
                <div><h3>By type</h3>{m.byType && <Bars data={m.byType} />}</div>
              </div>
            )}
          </>
        )}
      </section>

      <section className="panel">
        <div className="panel-head">
          <h2>Zone profile</h2>
          <select className="input" value={zone ?? ''} onChange={(e) => setZoneId(Number(e.target.value))} aria-label="Zone">
            {zones.map((zz) => <option key={zz.id} value={zz.id}>{zz.name} ({zz.areaName})</option>)}
          </select>
        </div>
        <ErrorBanner error={zoneReport.error} />
        {zoneReport.loading && !z ? <Loading /> : z && (
          <>
            <Warnings items={z.warnings} />
            <div className="kpis small">
              <div className="kpi"><span className="kpi-n">{z.animalsInZone ?? '—'}</span><span className="kpi-l">Registered animals</span></div>
              <div className="kpi"><span className="kpi-n">{z.incidentsThisMonth?.totalIncidents ?? '—'}</span><span className="kpi-l">Incidents this month</span></div>
              <div className="kpi"><span className="kpi-n">{z.waterResources?.length ?? '—'}</span><span className="kpi-l">Water sources</span></div>
              <div className="kpi"><span className="kpi-n">{z.waterResources?.filter((w) => w.status !== 'AVAILABLE').length ?? '—'}</span><span className="kpi-l">Water sources needing attention</span></div>
            </div>
            {z.zone && (
              <MapView height={260} markers={[
                { id: 'zone', lat: z.zone.centerLatitude, lng: z.zone.centerLongitude, label: z.zone.name, radius: 11 },
                ...(z.waterResources ?? []).map((w) => ({ id: `w${w.id}`, lat: w.latitude, lng: w.longitude, label: `${w.name}: ${label(w.status)}`, radius: 5,
                  color: w.status === 'AVAILABLE' ? '#3a7fc1' : '#a93a26' })),
                ...(z.incidentsThisMonth?.incidents ?? []).map((i) => ({ id: `i${i.id}`, lat: i.latitude, lng: i.longitude, label: `${i.incidentCode}: ${i.title}`, color: '#d9a441' })),
              ]} />
            )}
            {z.incidentsThisMonth && (
              <div className="grid-2">
                <div><h3>This month's incidents by status</h3><Bars data={z.incidentsThisMonth.statusSummary} /></div>
                <div>
                  <h3>Water sources</h3>
                  <ul className="plain">
                    {(z.waterResources ?? []).map((w) => <li key={w.id}>{w.name} <Chip value={w.status} /></li>)}
                  </ul>
                </div>
              </div>
            )}
          </>
        )}
      </section>
    </>
  );
}
