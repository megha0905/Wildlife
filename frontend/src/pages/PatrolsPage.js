// Patrol planner: schedules by day (start, log, complete, escalate), teams and routes.
import { patrolApi, resourceApi } from '../api/endpoints.js';
import { areaOptions, getLookups, staffOptions, zoneOptions } from '../api/lookups.js';
import { hasRole } from '../auth/auth.js';
import { h, mount } from '../components/dom.js';
import { openFormModal } from '../components/form.js';
import { mapView } from '../components/map.js';
import { openDrawer } from '../components/modal.js';
import { dataTable } from '../components/table.js';
import { toast } from '../components/toasts.js';
import {
  chip, empty, enumOptions, errorBanner, fact, fmtDate, fmtDateTime, label, loading, nowLocalInput, pageHeader, region,
  selectInput, tabbed, todayIso, warnings,
} from '../components/ui.js';

const SHIFTS = ['MORNING', 'EVENING', 'NIGHT'];
const OBSERVATIONS = ['ROUTINE', 'ANIMAL_SIGHTING', 'SUSPICIOUS_ACTIVITY', 'SNARE_FOUND', 'INFRASTRUCTURE_DAMAGE', 'FIRE', 'OTHER'];
const SEVERITY = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
const INCIDENT_TYPES = ['WILDLIFE_INJURY', 'HUMAN_ANIMAL_CONFLICT', 'POACHING', 'ILLEGAL_LOGGING', 'ENCROACHMENT', 'FOREST_FIRE', 'OTHER_THREAT'];
const DIFFICULTY = ['EASY', 'MODERATE', 'HARD'];
const PATROL_STATUS = ['SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'MISSED'];

function addDays(iso, n) {
  const d = new Date(`${iso}T00:00:00`);
  d.setDate(d.getDate() + n);
  return todayIso(d);
}

export function PatrolsPage() {
  return h('div', null,
    pageHeader('Patrol planner', 'Plan patrols for teams and routes, then follow them in the field. Scheduling a patrol reserves the team and vehicle.'),
    tabbed([
      { id: 'schedules', label: 'Schedule', render: schedulesTab },
      { id: 'teams', label: 'Teams', render: () => region(getLookups, teamsTab).el },
      { id: 'routes', label: 'Routes', render: () => region(getLookups, routesTab).el },
    ]));
}

// ==================================================================== schedules

function schedulesTab() {
  let from = addDays(todayIso(), -3);
  let status = '';
  const to = () => addDays(from, 10);

  const fromInput = h('input', { class: 'input', type: 'date', value: from, 'aria-label': 'From date',
    onChange: (e) => { from = e.target.value; refresh(); } });
  const rangeText = h('p', { class: 'muted small' });

  const list = region(
    () => patrolApi.schedules({ from, to: to(), status, size: 100, sort: 'patrolDate,asc' }),
    (data) => {
      const groups = new Map();
      for (const s of data.content) groups.set(s.patrolDate, [...(groups.get(s.patrolDate) ?? []), s]);
      const byDay = [...groups.entries()].sort(([a], [b]) => a.localeCompare(b));
      if (byDay.length === 0) return empty('No patrols in this period', 'Pick another week or schedule a patrol.');
      return h('div', { class: 'day-list' }, byDay.map(([day, items]) => h('section', { class: `day ${day === todayIso() ? 'today' : ''}` },
        h('h3', null, fmtDate(day), day === todayIso() && h('span', { class: 'today-tag' }, 'Today')),
        items.map((s) => h('button', { class: 'patrol-card', onClick: () => openScheduleDrawer(s.id, list.reload) },
          h('span', { class: 'patrol-time' }, `${s.startTime}–${s.endTime}`),
          h('span', { class: 'patrol-main' },
            h('strong', null, s.teamName),
            h('span', { class: 'muted' }, s.routeName + (s.zoneName ? ` · ${s.zoneName}` : ''))),
          h('span', { class: 'patrol-meta' },
            h('span', { class: 'muted' }, `${s.code} · ${label(s.shift)}`),
            chip(s.status)))))));
    });

  function refresh() {
    fromInput.value = from;
    mount(rangeText, `Showing ${fmtDate(from)} to ${fmtDate(to())}.`);
    list.reload();
  }
  mount(rangeText, `Showing ${fmtDate(from)} to ${fmtDate(to())}.`);

  async function schedulePatrol() {
    // the dropdowns need teams, routes and available vehicles
    const [teams, routes, vehicles] = await Promise.all([
      patrolApi.teams({ active: true }).then((r) => r.content).catch(() => []),
      patrolApi.routes().then((r) => r.content).catch(() => []),
      resourceApi.vehicles({ status: 'AVAILABLE' }).then((r) => r.content).catch(() => []),
    ]);
    openFormModal({
      title: 'Schedule patrol', submitLabel: 'Schedule patrol',
      intro: "Every team member and the vehicle are reserved for this time. If one is already booked, you'll see which booking clashes.",
      initial: { patrolDate: todayIso(), shift: 'MORNING', startTime: '06:00', endTime: '12:00' },
      fields: [
        { name: 'teamId', label: 'Team', type: 'select', required: true, options: teams.map((t) => ({ value: t.id, label: t.name })) },
        { name: 'routeId', label: 'Route', type: 'select', required: true, options: routes.map((r) => ({ value: r.id, label: `${r.name} (${r.zoneName ?? `zone ${r.zoneId}`})` })) },
        { name: 'patrolDate', label: 'Date', type: 'date', required: true },
        { name: 'shift', label: 'Shift', type: 'select', required: true, enumValues: SHIFTS },
        { name: 'startTime', label: 'Start', type: 'time', required: true },
        { name: 'endTime', label: 'End', type: 'time', required: true, hint: 'Night patrols may end after midnight.' },
        { name: 'vehicleId', label: 'Vehicle', type: 'select', placeholder: 'No vehicle', options: vehicles.map((v) => ({ value: v.id, label: `${v.registrationNumber} (${label(v.vehicleType)})` })) },
        { name: 'objective', label: 'Objective', type: 'textarea', wide: true },
      ],
      onSubmit: async (v) => {
        const s = await patrolApi.createSchedule(v);
        toast(`${s.code} scheduled`);
        s.warnings.forEach((w) => toast(w, 'warn'));
        list.reload();
      },
    });
  }

  return h('div', null,
    h('div', { class: 'toolbar' },
      h('button', { class: 'btn btn-secondary', onClick: () => { from = addDays(from, -7); refresh(); } }, 'Previous week'),
      fromInput,
      h('button', { class: 'btn btn-secondary', onClick: () => { from = addDays(from, 7); refresh(); } }, 'Next week'),
      selectInput({ ariaLabel: 'Status', placeholder: 'Any status', options: enumOptions(PATROL_STATUS), onChange: (v) => { status = v; list.reload(); } }),
      h('div', { class: 'spacer' }),
      hasRole('ADMIN', 'FOREST_OFFICER') && h('button', { class: 'btn btn-primary', onClick: schedulePatrol }, 'Schedule patrol')),
    rangeText,
    list.el);
}

function openScheduleDrawer(id, onChanged) {
  const drawer = openDrawer();
  const fieldRole = hasRole('ADMIN', 'FOREST_OFFICER', 'RANGER');
  let lastWarnings = [];
  mount(drawer.body, loading());

  const load = () => patrolApi.schedule(id)
    .then((s) => mount(drawer.body, render(s)))
    .catch((err) => mount(drawer.body, h('div', { class: 'drawer-body' }, errorBanner(err))));

  const move = async (status) => {
    try {
      const r = await patrolApi.setScheduleStatus(id, status);
      toast(`${r.code} is now ${label(r.status).toLowerCase()}`);
      lastWarnings = r.warnings;
      load();
      onChanged();
    } catch (e) {
      toast(e instanceof Error ? e.message : 'Failed', 'error');
    }
  };

  const render = (s) => [
    h('div', { class: 'drawer-head' },
      h('h2', null, s.code, h('span', { class: 'alias' }, s.teamName)),
      h('div', { class: 'sub' }, `${fmtDate(s.patrolDate)}, ${s.startTime}–${s.endTime} (${label(s.shift).toLowerCase()}) · `, chip(s.status)),
      fieldRole && h('div', { class: 'actions' },
        s.status === 'SCHEDULED' && [
          h('button', { class: 'btn btn-primary', onClick: () => move('IN_PROGRESS') }, 'Start patrol'),
          h('button', { class: 'btn btn-secondary', onClick: () => move('MISSED') }, 'Mark missed'),
          h('button', { class: 'btn btn-danger', onClick: () => move('CANCELLED') }, 'Cancel'),
        ],
        s.status === 'IN_PROGRESS' && [
          h('button', { class: 'btn btn-primary', onClick: () => addLog(s) }, 'Add log'),
          h('button', { class: 'btn btn-secondary', onClick: () => move('COMPLETED') }, 'Complete patrol'),
        ],
        s.status === 'COMPLETED' && h('button', { class: 'btn btn-secondary', onClick: () => addLog(s) }, 'Add log'))),
    h('div', { class: 'drawer-body' },
      warnings(lastWarnings),
      h('dl', { class: 'facts panel' },
        fact('Route', `${s.route.name} (${s.route.distanceKm} km, ${label(s.route.difficulty).toLowerCase()})`),
        fact('Zone', s.zoneName ?? s.zoneId),
        fact('Team', s.team.members.map((m) => m.fullName ?? `Staff ${m.id}`).join(', ')),
        fact('Leader', s.team.leaderName ?? s.team.leaderStaffId),
        fact('Vehicle', s.vehicleId ? `Vehicle ${s.vehicleId}` : 'None'),
        fact('Reserved', `${s.allocationIds.length} allocation(s)`),
        s.objective && fact('Objective', s.objective, true)),
      mapView({
        height: 220,
        path: s.route.waypoints.map((w) => [w.latitude, w.longitude]),
        markers: s.logs.map((l) => ({
          lat: l.latitude, lng: l.longitude, label: `${label(l.observationType)}: ${l.description}`,
          color: l.observationType === 'ROUTINE' || l.observationType === 'ANIMAL_SIGHTING' ? '#2c5246' : '#a93a26',
        })),
      }),
      h('h3', null, `Patrol log (${s.logs.length})`),
      s.logs.length === 0 && empty('Nothing logged yet', 'Logs can be added once the patrol has started.'),
      h('ol', { class: 'timeline' }, s.logs.map((l) => h('li', null,
        h('div', { class: 'when' }, fmtDateTime(l.loggedAt)),
        h('div', { class: 'what' }, label(l.observationType), ' ', l.severityHint && chip(l.severityHint)),
        h('div', null, l.description),
        h('div', { class: 'meta' },
          l.animalTagId && h('span', null, `Animal ${l.animalTagId}`),
          l.reportedByName && h('span', null, `By ${l.reportedByName}`),
          l.incidentId && h('span', null, `Escalated to incident #${l.incidentId}`)),
        !l.incidentId && fieldRole && l.observationType !== 'ROUTINE' && l.observationType !== 'ANIMAL_SIGHTING'
          && h('button', { class: 'link-btn', onClick: () => escalate(l) }, 'Escalate to incident'))))),
  ];

  function addLog(s) {
    openFormModal({
      title: `Log for ${s.code}`, submitLabel: 'Add log',
      intro: "Mention an animal tag to update that animal's last sighting automatically.",
      initial: { loggedAt: nowLocalInput(), observationType: 'ROUTINE', latitude: s.route.waypoints[0]?.latitude, longitude: s.route.waypoints[0]?.longitude },
      fields: [
        { name: 'loggedAt', label: 'Time', type: 'datetime', required: true },
        { name: 'observationType', label: 'Observation', type: 'select', required: true, enumValues: OBSERVATIONS },
        { name: 'latitude', label: 'Latitude', type: 'number', step: 'any', required: true },
        { name: 'longitude', label: 'Longitude', type: 'number', step: 'any', required: true },
        { name: 'animalTagId', label: 'Animal tag', placeholder: 'TGR-KNH-001' },
        { name: 'severityHint', label: 'Severity', type: 'select', enumValues: SEVERITY, placeholder: 'Not applicable' },
        { name: 'description', label: 'What was seen', type: 'textarea', required: true, wide: true },
      ],
      onSubmit: async (v) => {
        const l = await patrolApi.addLog(id, v);
        toast('Log added');
        l.warnings.forEach((w) => toast(w, 'warn'));
        load();
      },
    });
  }

  function escalate(log) {
    const type = log.observationType === 'SNARE_FOUND' || log.observationType === 'SUSPICIOUS_ACTIVITY' ? 'POACHING'
      : log.observationType === 'FIRE' ? 'FOREST_FIRE' : 'OTHER_THREAT';
    openFormModal({
      title: 'Escalate to incident', submitLabel: 'Create incident',
      intro: "Creates an incident in this zone with the log's location and time.",
      initial: { title: log.description.slice(0, 120), severity: log.severityHint ?? 'MEDIUM', incidentType: type },
      fields: [
        { name: 'title', label: 'Title', required: true, wide: true },
        { name: 'incidentType', label: 'Type', type: 'select', required: true, enumValues: INCIDENT_TYPES },
        { name: 'severity', label: 'Severity', type: 'select', required: true, enumValues: SEVERITY },
      ],
      onSubmit: async (v) => {
        const l = await patrolApi.escalate(log.id, v);
        toast(`Incident #${l.incidentId} created`);
        load();
      },
    });
  }

  load();
}

// ==================================================================== teams & routes

function teamsTab({ areas, staff }) {
  const list = region(() => patrolApi.teams(), (data) => dataTable({
    rows: data.content,
    empty: empty('No patrol teams yet'),
    columns: [
      { header: 'Team', render: (t) => h('strong', null, t.name) },
      { header: 'Area', render: (t) => areas.find((a) => a.id === t.areaId)?.name ?? t.areaId },
      { header: 'Leader', render: (t) => t.leaderName ?? t.leaderStaffId },
      { header: 'Members', render: (t) => t.members.map((m) => m.fullName ?? m.id).join(', ') },
      { header: 'Active', render: (t) => (t.active ? 'Yes' : 'No') },
    ],
  }));

  const addTeam = () => openFormModal({
    title: 'Add patrol team', submitLabel: 'Add team',
    fields: [
      { name: 'name', label: 'Team name', required: true },
      { name: 'areaId', label: 'Protected area', type: 'select', required: true, options: areaOptions(areas) },
      { name: 'leaderStaffId', label: 'Leader', type: 'select', required: true, options: staffOptions(staff) },
      { name: 'memberStaffIds', label: 'Member staff IDs', type: 'ids', required: true, wide: true,
        hint: `Comma-separated. Active staff: ${staff.filter((s) => s.status === 'ACTIVE').map((s) => `${s.id} ${s.fullName}`).join(', ')}` },
    ],
    onSubmit: async (v) => {
      const t = await patrolApi.createTeam(v);
      toast(`${t.name} added`);
      t.warnings.forEach((w) => toast(w, 'warn'));
      list.reload();
    },
  });

  return h('div', null,
    h('div', { class: 'toolbar' },
      h('div', { class: 'spacer' }),
      hasRole('ADMIN', 'FOREST_OFFICER') && h('button', { class: 'btn btn-primary', onClick: addTeam }, 'Add team')),
    list.el);
}

function routesTab({ areas, zones }) {
  const preview = h('div');
  const list = region(() => patrolApi.routes(), (data) => dataTable({
    rows: data.content,
    onRowClick: (r) => mount(preview, r.waypoints.length > 0 && h('div', { class: 'panel' },
      h('h3', null, r.name),
      mapView({
        height: 260,
        path: r.waypoints.map((w) => [w.latitude, w.longitude]),
        markers: r.waypoints.map((w) => ({ lat: w.latitude, lng: w.longitude, label: `${w.sequence}. ${w.label ?? ''}` })),
      }))),
    empty: empty('No routes yet'),
    columns: [
      { header: 'Route', render: (r) => h('strong', null, r.name) },
      { header: 'Zone', render: (r) => r.zoneName ?? r.zoneId },
      { header: 'Distance', render: (r) => `${r.distanceKm} km`, className: 'num' },
      { header: 'Hours', render: (r) => r.estimatedHours ?? '—', className: 'num' },
      { header: 'Difficulty', render: (r) => label(r.difficulty) },
      { header: 'Waypoints', render: (r) => r.waypoints.length, className: 'num' },
    ],
  }));

  const addRoute = () => openFormModal({
    title: 'Add patrol route', submitLabel: 'Add route', initial: { difficulty: 'MODERATE' },
    fields: [
      { name: 'name', label: 'Route name', required: true },
      { name: 'areaId', label: 'Protected area', type: 'select', required: true, options: areaOptions(areas) },
      { name: 'zoneId', label: 'Zone', type: 'select', required: true, options: zoneOptions(zones) },
      { name: 'distanceKm', label: 'Distance (km)', type: 'number', step: 'any', required: true },
      { name: 'estimatedHours', label: 'Estimated hours', type: 'number', step: 'any' },
      { name: 'difficulty', label: 'Difficulty', type: 'select', required: true, enumValues: DIFFICULTY },
      { name: 'description', label: 'Description', type: 'textarea', wide: true },
    ],
    onSubmit: async (v) => {
      const r = await patrolApi.createRoute({ ...v, waypoints: [] });
      toast(`${r.name} added`);
      r.warnings.forEach((w) => toast(w, 'warn'));
      list.reload();
    },
  });

  return h('div', null,
    h('div', { class: 'toolbar' },
      h('div', { class: 'spacer' }),
      hasRole('ADMIN', 'FOREST_OFFICER') && h('button', { class: 'btn btn-primary', onClick: addRoute }, 'Add route')),
    list.el,
    preview);
}
