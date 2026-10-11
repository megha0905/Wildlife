// Conservation programmes: activities, measurable targets and progress updates.
import { programmeApi } from '../api/endpoints.js';
import { getLookups, staffOptions } from '../api/lookups.js';
import { hasRole } from '../auth/auth.js';
import { progress } from '../components/bars.js';
import { h, mount } from '../components/dom.js';
import { openFormModal } from '../components/form.js';
import { openDrawer } from '../components/modal.js';
import { dataTable } from '../components/table.js';
import { toast } from '../components/toasts.js';
import {
  chip, empty, enumOptions, errorBanner, fact, fmtDate, fmtInr, label, loading, pageHeader, region, selectInput, todayIso, warnings,
} from '../components/ui.js';

const TYPES = ['SPECIES_RECOVERY', 'HABITAT_RESTORATION', 'ANTI_POACHING', 'COMMUNITY_OUTREACH', 'RESEARCH_MONITORING', 'WATER_MANAGEMENT'];
const ACTIVITY_STATUS = ['NOT_STARTED', 'IN_PROGRESS', 'DONE', 'BLOCKED'];

/** Programme status moves (CONTRACTS.md 6.5). */
const MOVES = {
  PLANNED: [{ to: 'ACTIVE', action: 'Start' }, { to: 'CANCELLED', action: 'Cancel' }],
  ACTIVE: [{ to: 'ON_HOLD', action: 'Put on hold' }, { to: 'COMPLETED', action: 'Complete' }, { to: 'CANCELLED', action: 'Cancel' }],
  ON_HOLD: [{ to: 'ACTIVE', action: 'Resume' }, { to: 'CANCELLED', action: 'Cancel' }],
  COMPLETED: [],
  CANCELLED: [],
};

export function ProgrammesPage() {
  return region(getLookups, programmes).el;
}

function programmes(lookups) {
  const { areas, species, staff } = lookups;
  let status = '';

  const list = region(() => programmeApi.list({ status, size: 50 }), (data) => [
    warnings(data.content[0]?.warnings),
    dataTable({
      rows: data.content,
      onRowClick: (p) => openProgrammeDrawer(p.id, lookups, list.reload),
      empty: empty('No programmes'),
      columns: [
        { header: 'Programme', render: (p) => [h('strong', null, p.name), h('div', { class: 'muted small' }, p.code)] },
        { header: 'Type', render: (p) => label(p.programmeType) },
        { header: 'Status', render: (p) => chip(p.status) },
        { header: 'Progress', render: (p) => progress(p.progressPercent) },
        { header: 'Activities', render: (p) => `${p.activitiesDone} / ${p.activityCount} done` },
        { header: 'Lead', render: (p) => p.leadStaffName ?? '—' },
        { header: 'Period', render: (p) => `${fmtDate(p.startDate)} – ${fmtDate(p.endDate)}` },
      ],
    }),
  ]);

  const create = () => openFormModal({
    title: 'New programme',
    submitLabel: 'Create programme',
    initial: { programmeType: 'ANTI_POACHING', startDate: todayIso() },
    fields: [
      { name: 'name', label: 'Name', required: true, wide: true },
      { name: 'code', label: 'Code', required: true, placeholder: 'KNH-ASD-2027' },
      { name: 'programmeType', label: 'Type', type: 'select', required: true, enumValues: TYPES },
      { name: 'startDate', label: 'Start', type: 'date', required: true },
      { name: 'endDate', label: 'End', type: 'date', required: true },
      { name: 'budgetInr', label: 'Budget (INR)', type: 'number', step: 'any' },
      { name: 'leadStaffId', label: 'Lead', type: 'select', options: staffOptions(staff) },
      { name: 'areaIds', label: 'Area IDs', type: 'ids', hint: areas.map((a) => `${a.id} ${a.name}`).join(', ') },
      { name: 'speciesIds', label: 'Species IDs', type: 'ids', hint: species.map((s) => `${s.id} ${s.commonName}`).join(', ') },
      { name: 'objective', label: 'Objective', type: 'textarea', required: true, wide: true },
      { name: 'description', label: 'Description', type: 'textarea', wide: true },
    ],
    onSubmit: async (v) => { const p = await programmeApi.create(v); toast(`${p.name} created`); list.reload(); },
  });

  return h('div', null,
    pageHeader('Conservation programmes', 'Programmes with their activities, measurable targets and progress updates.',
      hasRole('ADMIN', 'FOREST_OFFICER') && h('button', { class: 'btn btn-primary', onClick: create }, 'New programme')),
    h('div', { class: 'toolbar' },
      selectInput({ ariaLabel: 'Status', placeholder: 'Any status', options: enumOptions(Object.keys(MOVES)),
        onChange: (v) => { status = v; list.reload(); } })),
    list.el);
}

function openProgrammeDrawer(id, { staff }, onChanged) {
  const drawer = openDrawer();
  mount(drawer.body, loading());
  const officer = hasRole('ADMIN', 'FOREST_OFFICER');

  const refresh = () => { load(); onChanged(); };
  const load = () => programmeApi.get(id)
    .then((p) => mount(drawer.body, render(p)))
    .catch((err) => mount(drawer.body, h('div', { class: 'drawer-body' }, errorBanner(err))));

  const move = async (to) => {
    try { await programmeApi.setStatus(id, to); toast(`Programme ${label(to).toLowerCase()}`); refresh(); }
    catch (e) { toast(e instanceof Error ? e.message : 'Failed', 'error'); }
  };

  const render = (p) => {
    const readOnly = p.status === 'COMPLETED' || p.status === 'CANCELLED';
    return [
      h('div', { class: 'drawer-head' },
        h('h2', null, p.name),
        h('div', { class: 'sub' }, `${p.code} · ${label(p.programmeType)} · `, chip(p.status)),
        h('div', { class: 'actions' },
          !readOnly && hasRole('ADMIN', 'FOREST_OFFICER', 'RANGER', 'VET')
            && h('button', { class: 'btn btn-primary', onClick: () => postProgress(p) }, 'Post progress'),
          officer && !readOnly && [
            h('button', { class: 'btn btn-secondary', onClick: addTarget }, 'Add target'),
            h('button', { class: 'btn btn-secondary', onClick: () => activityForm(null) }, 'Add activity'),
          ],
          officer && MOVES[p.status].map((m) =>
            h('button', { class: m.to === 'CANCELLED' ? 'btn btn-danger' : 'btn btn-ghost', onClick: () => move(m.to) }, m.action)))),
      h('div', { class: 'drawer-body' },
        warnings(p.warnings),
        h('div', { class: 'big-progress' }, progress(p.progressPercent), h('span', { class: 'muted' }, 'overall, averaged over targets')),
        h('dl', { class: 'facts panel' },
          fact('Objective', p.objective, true),
          fact('Period', `${fmtDate(p.startDate)} – ${fmtDate(p.endDate)}`),
          fact('Budget', fmtInr(p.budgetInr)),
          fact('Lead', p.leadStaffName ?? '—'),
          fact('Areas / species', `${p.areaIds.length} / ${p.speciesIds.length}`)),
        h('h3', null, 'Targets'),
        p.targets.length === 0 && h('p', { class: 'muted' }, 'No targets yet. Progress is the average of target progress.'),
        h('ul', { class: 'cards' }, p.targets.map((t) => h('li', { class: 'card' },
          h('div', { class: 'card-title' }, t.metricName),
          progress(t.progressPercent),
          h('div', { class: 'muted small' },
            `From ${t.baselineValue} to ${t.targetValue} ${t.unit}; now ${t.currentValue}${t.dueDate ? `, due ${fmtDate(t.dueDate)}` : ''}`)))),
        h('h3', null, 'Activities'),
        dataTable({
          rows: p.activities,
          onRowClick: !readOnly && hasRole('ADMIN', 'FOREST_OFFICER', 'RANGER') ? (a) => activityForm(a) : undefined,
          empty: empty('No activities yet'),
          columns: [
            { header: 'Activity', render: (a) => [h('strong', null, a.title), a.overdue && h('span', { class: 'overdue' }, 'Overdue')] },
            { header: 'When', render: (a) => `${fmtDate(a.plannedStart)} – ${fmtDate(a.plannedEnd)}` },
            { header: 'Status', render: (a) => chip(a.status) },
            { header: 'Assigned', render: (a) => a.assignedStaffName ?? '—' },
          ],
        }),
        h('h3', null, 'Recent progress'),
        h('ol', { class: 'timeline' }, p.recentProgress.map((u) => h('li', null,
          h('div', { class: 'when' }, fmtDate(u.updateDate)),
          h('div', { class: 'what' }, u.targetMetricName ? `${u.targetMetricName}: ${u.previousValue} → ${u.newValue}` : 'General update'),
          h('div', null, u.note),
          h('div', { class: 'meta' }, h('span', null, u.reportedByName)))))),
    ];
  };

  const postProgress = (p) => openFormModal({
    title: 'Post progress',
    submitLabel: 'Post progress',
    intro: 'Choose a target to record its new value, or leave it empty for a general update.',
    initial: { updateDate: todayIso() },
    fields: [
      { name: 'targetId', label: 'Target', type: 'select', placeholder: 'General update',
        options: p.targets.map((t) => ({ value: t.id, label: `${t.metricName} (now ${t.currentValue} ${t.unit})` })) },
      { name: 'newValue', label: 'New value', type: 'number', step: 'any' },
      { name: 'updateDate', label: 'Date', type: 'date', required: true },
      { name: 'note', label: 'Note', type: 'textarea', required: true, wide: true },
    ],
    onSubmit: async (v) => { await programmeApi.addProgress(id, v); toast('Progress posted'); refresh(); },
  });

  const addTarget = () => openFormModal({
    title: 'Add target',
    submitLabel: 'Add target',
    fields: [
      { name: 'metricName', label: 'What is measured', required: true, wide: true },
      { name: 'unit', label: 'Unit', required: true, placeholder: 'snares' },
      { name: 'dueDate', label: 'Due', type: 'date' },
      { name: 'baselineValue', label: 'Baseline', type: 'number', step: 'any', required: true },
      { name: 'targetValue', label: 'Target', type: 'number', step: 'any', required: true, hint: 'Can be lower than the baseline (e.g. 40 → 0).' },
      { name: 'currentValue', label: 'Current value', type: 'number', step: 'any', hint: 'Defaults to the baseline.' },
    ],
    onSubmit: async (v) => { await programmeApi.addTarget(id, v); toast('Target added'); refresh(); },
  });

  const activityForm = (activity) => openFormModal({
    title: activity ? `Edit ${activity.title}` : 'Add activity',
    submitLabel: activity ? 'Save changes' : 'Add activity',
    initial: activity ?? { status: 'NOT_STARTED', plannedStart: todayIso() },
    fields: [
      { name: 'title', label: 'Title', required: true, wide: true },
      { name: 'plannedStart', label: 'Start', type: 'date', required: true },
      { name: 'plannedEnd', label: 'End', type: 'date', required: true },
      { name: 'status', label: 'Status', type: 'select', required: true, enumValues: ACTIVITY_STATUS },
      { name: 'assignedStaffId', label: 'Assigned to', type: 'select', options: staffOptions(staff) },
      { name: 'description', label: 'Description', type: 'textarea', wide: true },
    ],
    onSubmit: async (v) => {
      if (activity) await programmeApi.updateActivity(id, activity.id, { ...v, areaId: activity.areaId });
      else await programmeApi.addActivity(id, v);
      toast('Activity saved');
      refresh();
    },
  });

  load();
}
