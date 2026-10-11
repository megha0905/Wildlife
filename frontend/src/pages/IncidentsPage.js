// Incident board: every incident from report to closure, one column per status.
import { incidentApi } from '../api/endpoints.js';
import { getLookups, staffOptions, zoneOptions } from '../api/lookups.js';
import { hasRole } from '../auth/auth.js';
import { h, mount } from '../components/dom.js';
import { openFormModal } from '../components/form.js';
import { mapView } from '../components/map.js';
import { openDrawer } from '../components/modal.js';
import { toast } from '../components/toasts.js';
import {
  chip, enumOptions, errorBanner, fact, fmtCoord, fmtDateTime, label, loading, nowLocalInput, pageHeader, region,
  selectInput, warnings,
} from '../components/ui.js';

export const INCIDENT_TYPES = ['WILDLIFE_INJURY', 'HUMAN_ANIMAL_CONFLICT', 'POACHING', 'ILLEGAL_LOGGING', 'ENCROACHMENT', 'FOREST_FIRE', 'OTHER_THREAT'];
export const SEVERITIES = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
export const STATUSES = ['REPORTED', 'VERIFIED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];
const ASSIGNMENT_ROLES = ['LEAD', 'INVESTIGATOR', 'SUPPORT', 'VETERINARY'];

/** Next step in the workflow and who may take it (CONTRACTS.md 6.1). */
export const NEXT_STEP = {
  REPORTED: { to: 'VERIFIED', action: 'Verify', roles: ['ADMIN', 'FOREST_OFFICER'] },
  VERIFIED: { to: 'IN_PROGRESS', action: 'Start work', roles: ['ADMIN', 'FOREST_OFFICER'] },
  IN_PROGRESS: { to: 'RESOLVED', action: 'Resolve', roles: ['ADMIN', 'FOREST_OFFICER', 'RANGER', 'VET'] },
  RESOLVED: { to: 'CLOSED', action: 'Close', roles: ['ADMIN', 'FOREST_OFFICER'] },
};

export function IncidentsPage() {
  return region(getLookups, board).el;
}

function board({ zones }) {
  const filters = { zoneId: '', incidentType: '', severity: '' };

  const list = region(
    () => incidentApi.list({ zoneId: filters.zoneId, type: filters.incidentType, severity: filters.severity, size: 100, sort: 'occurredAt,desc' }),
    (data) => h('div', { class: 'board' }, STATUSES.map((s) => {
      const items = data.content.filter((i) => i.status === s);
      return h('section', { class: 'board-col' },
        h('h3', null, label(s), ' ', h('span', { class: 'count' }, items.length)),
        items.map((i) => h('button', { class: `incident-card sev-${i.severity.toLowerCase()}`, onClick: () => openIncidentDrawer(i.id, list.reload) },
          h('span', { class: 'incident-code' }, i.incidentCode),
          h('strong', null, i.title),
          h('span', { class: 'muted' }, `${label(i.incidentType)} · ${i.zoneName ?? `Zone ${i.zoneId}`}`),
          h('span', { class: 'incident-foot' }, chip(i.severity), h('span', { class: 'muted' }, fmtDateTime(i.occurredAt))),
          i.assignedPersonnel.length > 0 && h('span', { class: 'muted small' },
            i.assignedPersonnel.map((p) => p.staffName ?? `Staff ${p.staffId}`).join(', ')))));
    })));

  const setFilter = (k) => (v) => { filters[k] = v; list.reload(); };

  const report = () => openFormModal({
    title: 'Report incident',
    submitLabel: 'Report incident',
    initial: { occurredAt: nowLocalInput(), incidentType: 'HUMAN_ANIMAL_CONFLICT', severity: 'MEDIUM' },
    fields: [
      { name: 'title', label: 'Title', required: true, wide: true },
      { name: 'incidentType', label: 'Type', type: 'select', required: true, enumValues: INCIDENT_TYPES },
      { name: 'severity', label: 'Severity', type: 'select', required: true, enumValues: SEVERITIES },
      { name: 'zoneId', label: 'Zone', type: 'select', required: true, options: zoneOptions(zones) },
      { name: 'occurredAt', label: 'Occurred at', type: 'datetime', required: true },
      { name: 'latitude', label: 'Latitude', type: 'number', step: 'any', required: true },
      { name: 'longitude', label: 'Longitude', type: 'number', step: 'any', required: true },
      { name: 'locationDescription', label: 'Location description', wide: true },
      { name: 'description', label: 'What happened', type: 'textarea', required: true, wide: true },
    ],
    onSubmit: async (v) => {
      const zone = zones.find((z) => z.id === v.zoneId);
      const created = await incidentApi.create({ ...v, areaId: zone?.areaId });
      toast(`${created.incidentCode} reported`);
      created.warnings.forEach((w) => toast(w, 'warn'));
      list.reload();
    },
  });

  return h('div', null,
    pageHeader('Incident board',
      h('span', null, 'Injuries, conflict, poaching and other threats from report to closure. For one zone\'s month at a glance, use the ',
        h('a', { href: '#/incidents/monthly' }, 'monthly zone review'), '.'),
      hasRole('ADMIN', 'FOREST_OFFICER', 'RANGER', 'VET') && h('button', { class: 'btn btn-primary', onClick: report }, 'Report incident')),
    h('div', { class: 'toolbar' },
      selectInput({ ariaLabel: 'Zone', placeholder: 'All zones', options: zones.map((z) => ({ value: z.id, label: z.name })), onChange: setFilter('zoneId') }),
      selectInput({ ariaLabel: 'Type', placeholder: 'All types', options: enumOptions(INCIDENT_TYPES), onChange: setFilter('incidentType') }),
      selectInput({ ariaLabel: 'Severity', placeholder: 'Any severity', options: enumOptions(SEVERITIES), onChange: setFilter('severity') })),
    list.el);
}

/** Side panel with one incident: workflow step, assignments, map and history. Used by the board and the monthly review. */
export function openIncidentDrawer(id, onChanged = () => {}) {
  const drawer = openDrawer();
  mount(drawer.body, loading());
  let extraWarnings = [];
  let staff = [];

  const refresh = () => { load(); onChanged(); };
  const load = () => Promise.all([incidentApi.get(id), getLookups()])
    .then(([i, lookups]) => { staff = lookups.staff; mount(drawer.body, render(i)); })
    .catch((err) => mount(drawer.body, h('div', { class: 'drawer-body' }, errorBanner(err))));

  const render = (i) => {
    const next = NEXT_STEP[i.status];
    const canAssign = hasRole('ADMIN', 'FOREST_OFFICER') && i.status !== 'CLOSED';
    return [
      h('div', { class: 'drawer-head' },
        h('h2', null, i.incidentCode),
        h('div', { class: 'sub' }, i.title),
        h('div', { class: 'actions' },
          next && hasRole(...next.roles) && h('button', { class: 'btn btn-primary', onClick: () => transition(i, next) }, next.action),
          canAssign && h('button', { class: 'btn btn-secondary', onClick: () => assign(i) }, 'Assign staff'))),
      h('div', { class: 'drawer-body' },
        warnings([...extraWarnings, ...i.warnings]),
        h('ol', { class: 'steps', 'aria-label': 'Workflow' },
          STATUSES.map((s, idx) => h('li', { class: STATUSES.indexOf(i.status) >= idx ? 'done' : '' }, label(s)))),
        h('dl', { class: 'facts panel' },
          fact('Type', label(i.incidentType)),
          fact('Severity', chip(i.severity)),
          fact('Zone', i.zoneName ?? i.zoneId),
          fact('Occurred', fmtDateTime(i.occurredAt)),
          fact('Location', `${fmtCoord(i.latitude, i.longitude)}${i.locationDescription ? `, ${i.locationDescription}` : ''}`),
          fact('Reported by', i.reportedByName ?? '—'),
          fact('Description', i.description, true),
          i.resolutionNotes && fact('Resolution', i.resolutionNotes, true),
          i.sourcePatrolLogId && fact('Source', `Patrol log #${i.sourcePatrolLogId}`)),
        mapView({ height: 200, markers: [{ lat: i.latitude, lng: i.longitude, label: i.title, color: '#a93a26' }] }),
        h('h3', null, 'Assigned personnel'),
        i.assignedPersonnel.length === 0
          ? h('p', { class: 'muted' }, "No one assigned yet. Work can't start until someone is.")
          : h('ul', { class: 'people' }, i.assignedPersonnel.map((p) => h('li', null,
            h('strong', null, p.staffName ?? `Staff ${p.staffId}`),
            h('span', { class: 'muted' }, ` ${label(p.assignmentRole)}${p.designation ? `, ${label(p.designation).toLowerCase()}` : ''}`),
            canAssign && h('button', { class: 'link-btn', onClick: async () => {
              try {
                await incidentApi.unassign(i.id, p.assignmentId);
                toast('Assignment removed');
                refresh();
              } catch (err) { toast(err.message, 'error'); }
            } }, 'Remove')))),
        h('h3', null, 'History'),
        h('ol', { class: 'timeline' }, i.history.map((e) => h('li', null,
          h('div', { class: 'when' }, fmtDateTime(e.changedAt)),
          h('div', { class: 'what' }, e.fromStatus ? `${label(e.fromStatus)} → ${label(e.toStatus)}` : label(e.toStatus)),
          e.note && h('div', null, e.note),
          h('div', { class: 'meta' }, h('span', null, e.changedByName)))))),
    ];
  };

  const transition = (i, next) => openFormModal({
    title: `${next.action}: ${i.incidentCode}`,
    submitLabel: next.action,
    intro: next.to === 'RESOLVED' ? 'Describe how the incident was resolved.' : undefined,
    fields: [{ name: 'note', label: next.to === 'RESOLVED' ? 'Resolution notes' : 'Note', type: 'textarea', wide: true, required: next.to === 'RESOLVED' }],
    onSubmit: async (v) => {
      if (next.to === 'RESOLVED' && !v.note) throw new Error('Resolution notes are required.');
      const r = await incidentApi.transition(i.id, next.to, v.note ?? undefined);
      toast(`${r.incidentCode} is now ${label(r.status).toLowerCase()}`);
      refresh();
    },
  });

  const assign = (i) => openFormModal({
    title: `Assign staff to ${i.incidentCode}`,
    submitLabel: 'Assign',
    intro: 'The person is also reserved for the next 24 hours in staff allocations.',
    initial: { assignmentRole: 'LEAD' },
    fields: [
      { name: 'staffId', label: 'Staff member', type: 'select', required: true, options: staffOptions(staff) },
      { name: 'assignmentRole', label: 'Role', type: 'select', required: true, enumValues: ASSIGNMENT_ROLES },
      { name: 'note', label: 'Note', type: 'textarea', wide: true },
    ],
    onSubmit: async (v) => {
      const a = await incidentApi.assign(i.id, v);
      toast('Staff assigned');
      extraWarnings = a.warnings ?? [];
      refresh();
    },
  });

  load();
  return drawer;
}
