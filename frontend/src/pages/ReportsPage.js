// Reports: incident trends by month, and a combined profile of one zone drawn from several services.
import { reportApi } from '../api/endpoints.js';
import { getLookups } from '../api/lookups.js';
import { bars } from '../components/bars.js';
import { h } from '../components/dom.js';
import { mapView } from '../components/map.js';
import { chip, empty, label, pageHeader, region, warnings } from '../components/ui.js';

export function ReportsPage() {
  return h('div', null,
    pageHeader('Reports', 'Incident trends by month, and a combined profile of one zone drawn from several services.'),
    monthlyPanel(),
    region(getLookups, zonePanel).el);
}

function monthlyPanel() {
  const now = new Date();
  let year = now.getFullYear();
  let month = now.getMonth() + 1;

  const report = region(() => reportApi.monthlyIncidents(year, month), (m) => [
    warnings(m.warnings),
    m.totalThisMonth === undefined || m.totalThisMonth === null
      ? empty('No data for this month')
      : h('div', { class: 'grid-3' },
        h('div', null, h('div', { class: 'big-number' }, m.totalThisMonth), h('div', { class: 'muted' }, 'incidents that month')),
        h('div', null, h('h3', null, 'By status'), m.byStatus && bars(m.byStatus)),
        h('div', null, h('h3', null, 'By type'), m.byType && bars(m.byType))),
  ]);

  return h('section', { class: 'panel' },
    h('div', { class: 'panel-head' },
      h('h2', null, 'Incidents by month'),
      h('div', { class: 'toolbar compact' },
        h('select', { class: 'input', 'aria-label': 'Month', value: String(month), onChange: (e) => { month = Number(e.target.value); report.reload(); } },
          Array.from({ length: 12 }, (_, i) => h('option', { value: String(i + 1) },
            new Date(2000, i, 1).toLocaleString(undefined, { month: 'long' })))),
        h('input', { class: 'input year', type: 'number', min: 2000, max: 2100, 'aria-label': 'Year', value: String(year),
          onChange: (e) => { const y = Number(e.target.value); if (y >= 2000 && y <= 2100) { year = y; report.reload(); } } }))),
    report.el);
}

function zonePanel({ zones }) {
  if (zones.length === 0) {
    return h('section', { class: 'panel' }, h('h2', null, 'Zone profile'), empty('No zones available', 'The habitat service may be offline.'));
  }
  let zoneId = zones[0].id;

  const report = region(() => reportApi.zone(zoneId), (z) => {
    const water = z.waterResources ?? [];
    const incidents = z.incidentsThisMonth?.incidents ?? [];
    return [
      warnings(z.warnings),
      h('div', { class: 'kpis small' },
        kpi(z.animalsInZone, 'Registered animals'),
        kpi(z.incidentsThisMonth?.totalIncidents, 'Incidents this month'),
        kpi(z.waterResources?.length, 'Water sources'),
        kpi(z.waterResources?.filter((w) => w.status !== 'AVAILABLE').length, 'Water sources needing attention')),
      z.zone && mapView({
        height: 260,
        markers: [
          { lat: z.zone.centerLatitude, lng: z.zone.centerLongitude, label: z.zone.name, radius: 11 },
          ...water.map((w) => ({ lat: w.latitude, lng: w.longitude, label: `${w.name}: ${label(w.status)}`, radius: 5,
            color: w.status === 'AVAILABLE' ? '#3a7fc1' : '#a93a26' })),
          ...incidents.map((i) => ({ lat: i.latitude, lng: i.longitude, label: `${i.incidentCode}: ${i.title}`, color: '#d9a441' })),
        ],
      }),
      z.incidentsThisMonth && h('div', { class: 'grid-2' },
        h('div', null, h('h3', null, "This month's incidents by status"), bars(z.incidentsThisMonth.statusSummary)),
        h('div', null, h('h3', null, 'Water sources'),
          h('ul', { class: 'plain' }, water.map((w) => h('li', null, w.name, ' ', chip(w.status)))))),
    ];
  });

  return h('section', { class: 'panel' },
    h('div', { class: 'panel-head' },
      h('h2', null, 'Zone profile'),
      h('select', { class: 'input', 'aria-label': 'Zone', value: String(zoneId), onChange: (e) => { zoneId = Number(e.target.value); report.reload(); } },
        zones.map((z) => h('option', { value: String(z.id) }, `${z.name} (${z.areaName})`)))),
    report.el);
}

function kpi(n, text) {
  return h('div', { class: 'kpi' }, h('span', { class: 'kpi-n' }, n ?? '—'), h('span', { class: 'kpi-l' }, text));
}
