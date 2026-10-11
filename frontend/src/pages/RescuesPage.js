// Rescue tracker: rescued animals from intake through treatment and rehabilitation to release or transfer.
import { rescueApi } from '../api/endpoints.js';
import { areaOptions, getLookups, speciesOptions, zoneOptions } from '../api/lookups.js';
import { hasRole } from '../auth/auth.js';
import { h, mount } from '../components/dom.js';
import { openFormModal } from '../components/form.js';
import { openDrawer } from '../components/modal.js';
import { dataTable, pager } from '../components/table.js';
import { toast } from '../components/toasts.js';
import {
  chip, empty, enumOptions, errorBanner, fact, fmtCoord, fmtDate, fmtDateTime, label, loading, nowLocalInput,
  pageHeader, region, selectInput, todayIso, warnings,
} from '../components/ui.js';

const STAGES = ['REGISTERED', 'UNDER_TREATMENT', 'REHABILITATING', 'RELEASED'];
const CAUSES = ['INJURY', 'ORPHANED', 'HUMAN_CONFLICT', 'SNARE', 'ROAD_ACCIDENT', 'ILLNESS', 'DISPLACED', 'OTHER'];
const CONDITIONS = ['HEALTHY', 'INJURED', 'SICK', 'UNDER_TREATMENT', 'RECOVERING', 'UNKNOWN'];

/** Moves allowed from each stage and who may make them (CONTRACTS.md 6.2). */
const MOVES = {
  REGISTERED: [{ to: 'UNDER_TREATMENT', action: 'Start treatment', roles: ['ADMIN', 'FOREST_OFFICER', 'VET'] }],
  UNDER_TREATMENT: [{ to: 'REHABILITATING', action: 'Move to rehabilitation', roles: ['ADMIN', 'VET'] }],
  REHABILITATING: [
    { to: 'RELEASED', action: 'Release', roles: ['ADMIN', 'FOREST_OFFICER'] },
    { to: 'TRANSFERRED', action: 'Transfer', roles: ['ADMIN', 'FOREST_OFFICER', 'VET'] },
  ],
};

export function RescuesPage() {
  return region(getLookups, tracker).el;
}

function tracker(lookups) {
  const { species, zones } = lookups;
  let status = '';
  let page = 0;

  const list = region(() => rescueApi.list({ status, page, size: 20 }), (data) => [
    dataTable({
      rows: data.content,
      onRowClick: (r) => openRescueDrawer(r.id, lookups, list.reload),
      empty: empty('No rescue cases'),
      columns: [
        { header: 'Case', render: (r) => h('strong', null, r.caseNumber) },
        { header: 'Animal', render: (r) => [r.speciesName, r.animalTagId && h('div', { class: 'muted small' }, r.animalTagId)] },
        { header: 'Cause', render: (r) => label(r.cause) },
        { header: 'Rescued', render: (r) => fmtDate(r.rescuedAt) },
        { header: 'Facility', render: (r) => r.facilityName },
        { header: 'Progress', render: (r) => stageBar(r.status) },
      ],
    }),
    pager(data, (p) => { page = p; list.reload(); }),
  ]);

  const register = () => openFormModal({
    title: 'Register rescue',
    submitLabel: 'Register rescue',
    intro: "If the animal is already in the registry, pick it by ID; its status will change to 'in rescue'.",
    initial: { rescuedAt: nowLocalInput(), cause: 'INJURY', intakeCondition: 'INJURED' },
    fields: [
      { name: 'speciesId', label: 'Species', type: 'select', options: speciesOptions(species), placeholder: 'Not in catalogue' },
      { name: 'speciesName', label: 'Species name', required: true, hint: 'As shown on the case, e.g. "Indian Leopard".' },
      { name: 'animalId', label: 'Registry animal ID', type: 'number' },
      { name: 'animalDescription', label: 'Animal description' },
      { name: 'cause', label: 'Cause', type: 'select', required: true, enumValues: CAUSES },
      { name: 'intakeCondition', label: 'Condition at intake', type: 'select', required: true, enumValues: CONDITIONS },
      { name: 'rescuedAt', label: 'Rescued at', type: 'datetime', required: true },
      { name: 'zoneId', label: 'Zone', type: 'select', options: zoneOptions(zones) },
      { name: 'latitude', label: 'Latitude', type: 'number', step: 'any', required: true },
      { name: 'longitude', label: 'Longitude', type: 'number', step: 'any', required: true },
      { name: 'incidentId', label: 'Linked incident ID', type: 'number' },
      { name: 'facilityName', label: 'Rescue facility', required: true },
      { name: 'notes', label: 'Notes', type: 'textarea', wide: true },
    ],
    onSubmit: async (v) => {
      const zone = zones.find((z) => z.id === v.zoneId);
      const r = await rescueApi.create({ ...v, areaId: zone?.areaId ?? null });
      toast(`${r.caseNumber} registered`);
      r.warnings.forEach((w) => toast(w, 'warn'));
      list.reload();
    },
  });

  return h('div', null,
    pageHeader('Rescue tracker',
      'Rescued animals from intake through treatment and rehabilitation to release or transfer. Linked animal records update automatically.',
      hasRole('ADMIN', 'FOREST_OFFICER', 'RANGER', 'VET') && h('button', { class: 'btn btn-primary', onClick: register }, 'Register rescue')),
    h('div', { class: 'toolbar' },
      selectInput({ ariaLabel: 'Stage', placeholder: 'All stages', options: enumOptions([...STAGES, 'TRANSFERRED']),
        onChange: (v) => { status = v; page = 0; list.reload(); } })),
    list.el);
}

function stageBar(status) {
  if (status === 'TRANSFERRED') return chip('TRANSFERRED');
  const idx = STAGES.indexOf(status);
  return h('span', { class: 'stage-bar', title: label(status) },
    STAGES.map((s, i) => h('span', { class: i <= idx ? 'on' : '' })),
    h('span', { class: 'stage-label' }, label(status)));
}

function timelineHistory(history) {
  return h('ol', { class: 'timeline' }, history.map((e) => h('li', null,
    h('div', { class: 'when' }, fmtDateTime(e.changedAt)),
    h('div', { class: 'what' }, e.fromStatus ? `${label(e.fromStatus)} → ${label(e.toStatus)}` : label(e.toStatus)),
    e.note && h('div', null, e.note),
    h('div', { class: 'meta' }, h('span', null, e.changedByName)))));
}

function openRescueDrawer(id, lookups, onChanged) {
  const { areas, zones } = lookups;
  const drawer = openDrawer();
  mount(drawer.body, loading());
  let extraWarnings = [];

  const refresh = () => { load(); onChanged(); };
  const load = () => rescueApi.get(id)
    .then((r) => mount(drawer.body, render(r)))
    .catch((err) => mount(drawer.body, h('div', { class: 'drawer-body' }, errorBanner(err))));

  const render = (r) => {
    const moves = (MOVES[r.status] ?? []).filter((m) => hasRole(...m.roles));
    const open = r.status !== 'RELEASED' && r.status !== 'TRANSFERRED';
    return [
      h('div', { class: 'drawer-head' },
        h('h2', null, r.caseNumber, h('span', { class: 'alias' }, r.speciesName)),
        h('div', { class: 'sub' }, stageBar(r.status)),
        h('div', { class: 'actions' },
          moves.map((m) => h('button', { class: 'btn btn-primary', onClick: () => move(r, m) }, m.action)),
          open && hasRole('ADMIN', 'VET') && h('button', { class: 'btn btn-secondary', onClick: () => addTreatment(r) }, 'Add treatment'),
          (r.status === 'UNDER_TREATMENT' || r.status === 'REHABILITATING') && hasRole('ADMIN', 'FOREST_OFFICER', 'VET')
            && h('button', { class: 'btn btn-secondary', onClick: () => addRehab(r) }, 'Add assessment'))),
      h('div', { class: 'drawer-body' },
        warnings([...extraWarnings, ...r.warnings]),
        h('dl', { class: 'facts panel' },
          fact('Cause', label(r.cause)),
          fact('Condition at intake', chip(r.intakeCondition)),
          fact('Registry animal', r.animalTagId ?? (r.animalId ? `#${r.animalId}` : 'Not in registry')),
          fact('Rescued', fmtDateTime(r.rescuedAt)),
          fact('Location', fmtCoord(r.latitude, r.longitude)),
          fact('Facility', r.facilityName),
          r.incidentId && fact('Linked incident', `#${r.incidentId}`),
          r.animalDescription && fact('Description', r.animalDescription, true),
          r.transfer && fact('Transferred', `${fmtDate(r.transfer.transferDate)} to ${r.transfer.destinationFacility}: ${r.transfer.reason}`, true),
          r.release && fact('Released',
            `${fmtDate(r.release.releaseDate)} at ${fmtCoord(r.release.latitude, r.release.longitude)}${r.release.notes ? `: ${r.release.notes}` : ''}`, true)),
        h('h3', null, `Treatments (${r.treatments.length})`),
        r.treatments.length === 0 && h('p', { class: 'muted' }, 'No treatment recorded yet. At least one is needed before rehabilitation.'),
        h('ol', { class: 'timeline' }, r.treatments.map((t) => h('li', null,
          h('div', { class: 'when' }, fmtDate(t.treatmentDate)),
          h('div', { class: 'what' }, t.diagnosis),
          h('div', null, t.procedure),
          h('div', { class: 'meta' }, h('span', null, t.vetName), t.medications && h('span', null, t.medications))))),
        h('h3', null, `Rehabilitation assessments (${r.rehabProgress.length})`),
        r.rehabProgress.length === 0 && h('p', { class: 'muted' }, 'The latest assessment must say "fit for release" before the animal can be released.'),
        h('ol', { class: 'timeline' }, r.rehabProgress.map((p) => h('li', null,
          h('div', { class: 'when' }, fmtDate(p.assessedOn)),
          h('div', { class: 'what' }, `${p.fitForRelease ? 'Fit for release' : 'Not yet fit for release'}${p.weightKg != null ? `, ${p.weightKg} kg` : ''}`),
          h('div', null, p.behaviourNotes),
          h('div', { class: 'meta' }, h('span', null, p.assessedBy), p.enclosure && h('span', null, p.enclosure))))),
        h('h3', null, 'History'),
        timelineHistory(r.history)),
    ];
  };

  const transitionFields = (to) => {
    if (to === 'RELEASED') return [
      { name: 'releaseDate', label: 'Release date', type: 'date', required: true },
      { name: 'releaseAreaId', label: 'Protected area', type: 'select', required: true, options: areaOptions(areas) },
      { name: 'releaseZoneId', label: 'Zone', type: 'select', required: true, options: zoneOptions(zones) },
      { name: 'latitude', label: 'Latitude', type: 'number', step: 'any', required: true },
      { name: 'longitude', label: 'Longitude', type: 'number', step: 'any', required: true },
      { name: 'notes', label: 'Release notes', type: 'textarea', wide: true },
    ];
    if (to === 'TRANSFERRED') return [
      { name: 'destinationFacility', label: 'Destination facility', required: true, wide: true },
      { name: 'transferDate', label: 'Transfer date', type: 'date', required: true },
      { name: 'reason', label: 'Reason', type: 'textarea', required: true, wide: true },
    ];
    return [{ name: 'note', label: 'Note', type: 'textarea', wide: true }];
  };

  const move = (r, m) => openFormModal({
    title: `${m.action}: ${r.caseNumber}`,
    submitLabel: m.action,
    initial: { releaseDate: todayIso(), transferDate: todayIso(), releaseAreaId: r.areaId ?? undefined,
      releaseZoneId: r.zoneId ?? undefined, latitude: r.latitude, longitude: r.longitude },
    fields: transitionFields(m.to),
    onSubmit: async (v) => {
      const body = { toStatus: m.to, note: v.note ?? null };
      if (m.to === 'RELEASED') body.release = v;
      if (m.to === 'TRANSFERRED') body.transfer = v;
      const res = await rescueApi.transition(r.id, body);
      toast(`${res.caseNumber} is now ${label(res.status).toLowerCase()}`);
      extraWarnings = res.warnings ?? [];
      refresh();
    },
  });

  const addTreatment = (r) => openFormModal({
    title: `Treatment for ${r.caseNumber}`,
    submitLabel: 'Add treatment',
    initial: { treatmentDate: todayIso() },
    fields: [
      { name: 'treatmentDate', label: 'Date', type: 'date', required: true },
      { name: 'vetName', label: 'Vet', required: true },
      { name: 'diagnosis', label: 'Diagnosis', required: true, wide: true },
      { name: 'procedure', label: 'Procedure', type: 'textarea', required: true, wide: true },
      { name: 'medications', label: 'Medications', wide: true },
      { name: 'notes', label: 'Notes', type: 'textarea', wide: true },
    ],
    onSubmit: async (v) => { await rescueApi.addTreatment(r.id, v); toast('Treatment added'); refresh(); },
  });

  const addRehab = (r) => openFormModal({
    title: `Assessment for ${r.caseNumber}`,
    submitLabel: 'Add assessment',
    initial: { assessedOn: todayIso(), fitForRelease: false },
    fields: [
      { name: 'assessedOn', label: 'Date', type: 'date', required: true },
      { name: 'assessedBy', label: 'Assessed by', required: true },
      { name: 'enclosure', label: 'Enclosure' },
      { name: 'weightKg', label: 'Weight (kg)', type: 'number', step: 'any' },
      { name: 'fitForRelease', label: 'Fit for release', type: 'checkbox', wide: true },
      { name: 'behaviourNotes', label: 'Behaviour and condition', type: 'textarea', required: true, wide: true },
    ],
    onSubmit: async (v) => { await rescueApi.addRehab(r.id, v); toast('Assessment added'); refresh(); },
  });

  load();
}
