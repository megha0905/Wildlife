// Dashboard: greeting, headline numbers and charts from every service (via reporting-service).
import { reportApi } from '../api/endpoints.js';
import { getUser } from '../auth/auth.js';
import { bars, progress } from '../components/bars.js';
import { h } from '../components/dom.js';
import { IUCN, fact, fmtDateTime, fmtInr, label, region, warnings } from '../components/ui.js';

const SEVERITY_COLORS = { LOW: '#9fbdb1', MEDIUM: '#d9a441', HIGH: '#c2603d', CRITICAL: '#a93a26' };
const IUCN_COLORS = Object.fromEntries(Object.entries(IUCN).map(([k, v]) => [k, v.color]));

export function DashboardPage() {
  return region(() => reportApi.dashboard(), renderDashboard).el;
}

function renderDashboard(d) {
  const user = getUser();
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
  const offline = (name) => h('p', { class: 'muted' }, `${name} service offline.`);

  return h('div', null,
    welcomeBanner(user?.fullName, user?.roles.map(label).join(', '), d.generatedAt),
    warnings(d.warnings),

    h('div', { class: 'kpis' }, tiles.map((t) => h('a', { href: `#${t.to}`, class: `kpi ${t.alert ? 'kpi-alert' : ''}` },
      h('span', { class: 'kpi-n' }, t.n ?? '—'),
      h('span', { class: 'kpi-l' }, t.l),
      t.sub && h('span', { class: 'kpi-sub' }, t.sub)))),

    h('div', { class: 'grid-2' },
      h('section', { class: 'panel' }, h('h2', null, 'Animals by threat level'),
        d.wildlife ? bars(d.wildlife.byConservationStatus, IUCN_COLORS) : offline('Wildlife')),
      h('section', { class: 'panel' }, h('h2', null, 'Incidents this month by type'),
        d.incidents ? bars(d.incidents.byType) : offline('Incident')),
      h('section', { class: 'panel' }, h('h2', null, 'Incidents this month by severity'),
        d.incidents ? bars(d.incidents.bySeverity, SEVERITY_COLORS) : offline('Incident')),
      h('section', { class: 'panel' }, h('h2', null, 'Rescue cases by stage'),
        d.rescues ? bars(d.rescues.byStatus) : offline('Rescue')),
      h('section', { class: 'panel' }, h('h2', null, 'Field resources'),
        d.resources
          ? h('dl', { class: 'facts' },
            fact('Staff', `${d.resources.totalStaff} (${d.resources.staffByStatus.ACTIVE ?? 0} active)`),
            fact('Active allocations', d.resources.activeAllocations),
            fact('Vehicles available', d.resources.vehiclesByStatus.AVAILABLE ?? 0),
            fact('Vehicles in maintenance', d.resources.vehiclesByStatus.MAINTENANCE ?? 0))
          : offline('Resource')),
      h('section', { class: 'panel' }, h('h2', null, 'Patrols and programmes this month'),
        d.patrols && h('dl', { class: 'facts' },
          fact('Distance patrolled', `${d.patrols.distancePatrolledKmThisMonth.toFixed(1)} km`),
          fact('Suspicious activity logged', d.patrols.suspiciousActivityThisMonth)),
        d.programmes && [
          h('div', { class: 'fact-line' }, 'Average programme progress ', progress(d.programmes.averageProgressPercent)),
          h('div', { class: 'fact-line' }, 'Budget of active programmes ', h('strong', null, fmtInr(d.programmes.totalBudgetInrActive))),
        ])));
}

/** Greeting at the top of the dashboard: the user's first name, today's date and the most-used screens. */
function welcomeBanner(name, roles, generatedAt) {
  const first = name ? name.replace(/^Dr\.\s*/, '').split(' ')[0] : '';
  const hour = new Date().getHours();
  const greeting = hour < 12 ? 'Good morning' : hour < 17 ? 'Good afternoon' : 'Good evening';
  const today = new Date().toLocaleDateString(undefined, { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
  return h('div', { class: 'welcome' },
    h('div', { class: 'welcome-text' },
      h('p', { class: 'welcome-date' }, today),
      h('h1', null, `${greeting}${first ? `, ${first}` : ''}`),
      h('p', null, `${roles ? `${roles} · ` : ''}Here is what is happening across your protected areas. Updated ${fmtDateTime(generatedAt)}.`)),
    h('div', { class: 'welcome-actions' },
      h('a', { class: 'btn welcome-btn-main', href: '#/incidents' }, 'Incident board'),
      h('a', { class: 'btn welcome-btn', href: '#/incidents/monthly' }, 'Monthly zone review'),
      h('a', { class: 'btn welcome-btn', href: '#/patrols' }, 'Patrol planner')));
}
