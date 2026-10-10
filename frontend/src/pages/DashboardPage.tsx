import { Link } from 'react-router-dom';
import { reportApi } from '../api/endpoints';
import { Bars, Progress } from '../components/Bars';
import { ErrorBanner, IUCN, Loading, PageHeader, Warnings, fmtDateTime, fmtInr, useLoad } from '../components/ui';

const SEVERITY_COLORS = { LOW: '#9fbdb1', MEDIUM: '#d9a441', HIGH: '#c2603d', CRITICAL: '#a93a26' };
const IUCN_COLORS = Object.fromEntries(Object.entries(IUCN).map(([k, v]) => [k, v.color]));

export function DashboardPage() {
  const { data: d, error, loading } = useLoad(() => reportApi.dashboard(), []);

  if (loading && !d) return <Loading />;
  if (error) return <ErrorBanner error={error} />;
  if (!d) return null;

  const tiles = [
    { n: d.incidents?.openIncidents, l: 'Open incidents', to: '/incidents', alert: (d.incidents?.criticalOpen ?? 0) > 0,
      sub: d.incidents ? `${d.incidents.criticalOpen} critical` : undefined },
    { n: d.wildlife?.needingAttention, l: 'Animals injured, sick or in treatment', to: '/animals' },
    { n: d.rescues?.activeCases, l: 'Active rescue cases', to: '/rescues' },
    { n: d.patrols?.schedulesToday, l: 'Patrols today', to: '/patrols' },
    { n: d.habitat?.waterSourcesNeedingAttention, l: 'Water sources low, dry or contaminated', to: '/zones' },
    { n: d.programmes?.activeProgrammes, l: 'Active programmes', to: '/programmes',
      sub: d.programmes ? `${d.programmes.averageProgressPercent.toFixed(0)}% average progress` : undefined },
  ];

  return (
    <>
      <PageHeader title="Dashboard" intro={`Consolidated picture across all services, generated ${fmtDateTime(d.generatedAt)}.`} />
      <Warnings items={d.warnings} />

      <div className="kpis">
        {tiles.map((t) => (
          <Link key={t.l} to={t.to} className={`kpi ${t.alert ? 'kpi-alert' : ''}`}>
            <span className="kpi-n">{t.n ?? '—'}</span>
            <span className="kpi-l">{t.l}</span>
            {t.sub && <span className="kpi-sub">{t.sub}</span>}
          </Link>
        ))}
      </div>

      <div className="grid-2">
        <section className="panel">
          <h2>Animals by threat level</h2>
          {d.wildlife ? <Bars data={d.wildlife.byConservationStatus} colors={IUCN_COLORS} /> : <p className="muted">Wildlife service offline.</p>}
        </section>
        <section className="panel">
          <h2>Incidents this month by type</h2>
          {d.incidents ? <Bars data={d.incidents.byType} /> : <p className="muted">Incident service offline.</p>}
        </section>
        <section className="panel">
          <h2>Incidents this month by severity</h2>
          {d.incidents ? <Bars data={d.incidents.bySeverity} colors={SEVERITY_COLORS} /> : <p className="muted">Incident service offline.</p>}
        </section>
        <section className="panel">
          <h2>Rescue cases by stage</h2>
          {d.rescues ? <Bars data={d.rescues.byStatus} /> : <p className="muted">Rescue service offline.</p>}
        </section>
        <section className="panel">
          <h2>Field resources</h2>
          {d.resources ? (
            <dl className="facts">
              <div className="fact"><dt>Staff</dt><dd>{d.resources.totalStaff} ({d.resources.staffByStatus.ACTIVE ?? 0} active)</dd></div>
              <div className="fact"><dt>Active allocations</dt><dd>{d.resources.activeAllocations}</dd></div>
              <div className="fact"><dt>Vehicles available</dt><dd>{d.resources.vehiclesByStatus.AVAILABLE ?? 0}</dd></div>
              <div className="fact"><dt>Vehicles in maintenance</dt><dd>{d.resources.vehiclesByStatus.MAINTENANCE ?? 0}</dd></div>
            </dl>
          ) : <p className="muted">Resource service offline.</p>}
        </section>
        <section className="panel">
          <h2>Patrols and programmes this month</h2>
          {d.patrols && (
            <dl className="facts">
              <div className="fact"><dt>Distance patrolled</dt><dd>{d.patrols.distancePatrolledKmThisMonth.toFixed(1)} km</dd></div>
              <div className="fact"><dt>Suspicious activity logged</dt><dd>{d.patrols.suspiciousActivityThisMonth}</dd></div>
            </dl>
          )}
          {d.programmes && (
            <>
              <div className="fact-line">Average programme progress <Progress percent={d.programmes.averageProgressPercent} /></div>
              <div className="fact-line">Budget of active programmes <strong>{fmtInr(d.programmes.totalBudgetInrActive)}</strong></div>
            </>
          )}
        </section>
      </div>
    </>
  );
}
