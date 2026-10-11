// Mandatory example: a forest officer reviews all incidents in a protected zone for the current month.
// Uses GET /api/incidents/zone/{zoneId}/current-month?status= (CONTRACTS.md 7.6).
import { incidentApi } from '../api/endpoints.js';
import { getLookups } from '../api/lookups.js';
import { h } from '../components/dom.js';
import { mapView } from '../components/map.js';
import { dataTable } from '../components/table.js';
import { chip, empty, fmtCoord, fmtDateTime, label, pageHeader, region, warnings } from '../components/ui.js';
import { STATUSES, openIncidentDrawer } from './IncidentsPage.js';

export function ZoneMonthlyPage() {
  return h('div', null,
    pageHeader('Monthly zone review',
      'Every incident in one protected zone this month, with its status, location and the people assigned to it.'),
    region(getLookups, review).el);
}

function review({ zones }) {
  if (zones.length === 0) return empty('No zones available', 'The habitat service may be offline.');
  let zoneId = zones[0].id;
  let status = '';

  const report = region(() => incidentApi.zoneCurrentMonth(zoneId, status), (r) => {
    const monthName = new Date(r.periodStart).toLocaleDateString(undefined, { month: 'long', year: 'numeric' });
    const open = (i) => openIncidentDrawer(i.id, report.reload);
    const pick = (s) => () => { status = s; report.reload(); };
    return [
      warnings(r.warnings),
      h('div', { class: 'review-head' },
        h('div', null,
          h('div', { class: 'review-zone' }, r.zoneName ?? `Zone ${r.zoneId}`),
          h('div', { class: 'muted' }, `${r.areaName ?? ''}${r.areaName ? ' · ' : ''}${monthName}`)),
        h('div', { class: 'review-total' }, h('span', null, r.totalIncidents), ` incident${r.totalIncidents === 1 ? '' : 's'} this month`)),

      // Status summary doubles as the filter: counts always cover the whole month
      h('div', { class: 'status-filter', role: 'group', 'aria-label': 'Filter by status' },
        h('button', { class: status === '' ? 'active' : '', onClick: pick('') },
          h('span', { class: 'sf-n' }, r.totalIncidents), h('span', { class: 'sf-l' }, 'All')),
        STATUSES.map((s) => h('button', { class: status === s ? 'active' : '', 'aria-pressed': String(status === s), onClick: pick(s) },
          h('span', { class: 'sf-n' }, r.statusSummary[s] ?? 0), h('span', { class: 'sf-l' }, label(s))))),

      r.incidents.length > 0 && mapView({
        height: 260,
        markers: r.incidents.map((i) => ({
          lat: i.latitude, lng: i.longitude, label: `${i.incidentCode}: ${i.title}`,
          color: i.severity === 'CRITICAL' || i.severity === 'HIGH' ? '#a93a26' : '#d9a441', onClick: () => open(i),
        })),
      }),

      dataTable({
        rows: r.incidents,
        onRowClick: open,
        empty: empty(status ? `No ${label(status).toLowerCase()} incidents this month` : 'No incidents in this zone this month',
          status ? 'Choose another status above.' : 'A quiet month so far.'),
        columns: [
          { header: 'Incident', render: (i) => [h('strong', null, i.incidentCode), h('div', { class: 'muted small' }, i.title)] },
          { header: 'Type', render: (i) => label(i.incidentType) },
          { header: 'Severity', render: (i) => chip(i.severity) },
          { header: 'Status', render: (i) => chip(i.status) },
          { header: 'Occurred', render: (i) => fmtDateTime(i.occurredAt) },
          { header: 'Location', render: (i) => [fmtCoord(i.latitude, i.longitude),
            i.locationDescription && h('div', { class: 'muted small' }, i.locationDescription)] },
          { header: 'Assigned personnel', render: (i) => (i.assignedPersonnel.length === 0
            ? h('span', { class: 'muted' }, 'Unassigned')
            : i.assignedPersonnel.map((p) => h('div', null, p.staffName ?? `Staff ${p.staffId}`, ' ',
              h('span', { class: 'muted small' }, `(${label(p.assignmentRole).toLowerCase()})`)))) },
        ],
      }),
    ];
  });

  return h('div', null,
    h('div', { class: 'toolbar' },
      h('label', { class: 'inline-label', for: 'zone' }, 'Zone'),
      h('select', { id: 'zone', class: 'input', value: String(zoneId), onChange: (e) => { zoneId = Number(e.target.value); report.reload(); } },
        zones.map((z) => h('option', { value: String(z.id) }, `${z.name} (${z.areaName})`)))),
    report.el);
}
