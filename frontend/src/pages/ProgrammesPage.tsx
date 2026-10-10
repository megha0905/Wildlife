import { useState } from 'react';
import { programmeApi } from '../api/endpoints';
import { staffOptions, useLookups } from '../api/lookups';
import type { Programme, ProgrammeActivity, ProgrammeStatus } from '../api/types';
import { useAuth } from '../auth/AuthContext';
import { Progress } from '../components/Bars';
import { DataTable } from '../components/DataTable';
import { FormModal } from '../components/Form';
import { Drawer } from '../components/Modal';
import { useToast } from '../components/Toasts';
import { Chip, Empty, ErrorBanner, Fact, Loading, PageHeader, Warnings, fmtDate, fmtInr, label, todayIso, useLoad } from '../components/ui';

const TYPES = ['SPECIES_RECOVERY', 'HABITAT_RESTORATION', 'ANTI_POACHING', 'COMMUNITY_OUTREACH', 'RESEARCH_MONITORING', 'WATER_MANAGEMENT'] as const;
const ACTIVITY_STATUS = ['NOT_STARTED', 'IN_PROGRESS', 'DONE', 'BLOCKED'] as const;

/** Programme status moves (CONTRACTS.md 6.5). */
const MOVES: Record<ProgrammeStatus, { to: ProgrammeStatus; action: string }[]> = {
  PLANNED: [{ to: 'ACTIVE', action: 'Start' }, { to: 'CANCELLED', action: 'Cancel' }],
  ACTIVE: [{ to: 'ON_HOLD', action: 'Put on hold' }, { to: 'COMPLETED', action: 'Complete' }, { to: 'CANCELLED', action: 'Cancel' }],
  ON_HOLD: [{ to: 'ACTIVE', action: 'Resume' }, { to: 'CANCELLED', action: 'Cancel' }],
  COMPLETED: [],
  CANCELLED: [],
};

export function ProgrammesPage() {
  const { hasRole } = useAuth();
  const toast = useToast();
  const { areas, species, staff } = useLookups();
  const [status, setStatus] = useState('');
  const [creating, setCreating] = useState(false);
  const [selected, setSelected] = useState<number | null>(null);
  const list = useLoad(() => programmeApi.list({ status, size: 50 }), [status]);

  return (
    <>
      <PageHeader
        title="Conservation programmes"
        intro="Programmes with their activities, measurable targets and progress updates."
        actions={hasRole('ADMIN', 'FOREST_OFFICER') && <button className="btn btn-primary" onClick={() => setCreating(true)}>New programme</button>}
      />
      <div className="toolbar">
        <select className="input" value={status} onChange={(e) => setStatus(e.target.value)} aria-label="Status">
          <option value="">Any status</option>
          {Object.keys(MOVES).map((s) => <option key={s} value={s}>{label(s)}</option>)}
        </select>
      </div>
      <ErrorBanner error={list.error} />
      {list.data && <Warnings items={list.data.content[0]?.warnings} />}
      {list.loading && !list.data ? <Loading /> : (
        <DataTable<Programme>
          rows={list.data?.content ?? []} rowKey={(p) => p.id} onRowClick={(p) => setSelected(p.id)}
          empty={<Empty title="No programmes" />}
          columns={[
            { header: 'Programme', render: (p) => <><strong>{p.name}</strong><div className="muted small">{p.code}</div></> },
            { header: 'Type', render: (p) => label(p.programmeType) },
            { header: 'Status', render: (p) => <Chip value={p.status} /> },
            { header: 'Progress', render: (p) => <Progress percent={p.progressPercent} /> },
            { header: 'Activities', render: (p) => `${p.activitiesDone} / ${p.activityCount} done` },
            { header: 'Lead', render: (p) => p.leadStaffName ?? '—' },
            { header: 'Period', render: (p) => `${fmtDate(p.startDate)} – ${fmtDate(p.endDate)}` },
          ]}
        />
      )}

      <FormModal
        open={creating} onClose={() => setCreating(false)} title="New programme" submitLabel="Create programme"
        initial={{ programmeType: 'ANTI_POACHING', startDate: todayIso() }}
        fields={[
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
        ]}
        onSubmit={async (v) => { const p = await programmeApi.create(v); toast(`${p.name} created`); list.reload(); }}
      />
      {selected !== null && <ProgrammeDrawer id={selected} onClose={() => setSelected(null)} onChanged={list.reload} />}
    </>
  );
}

function ProgrammeDrawer({ id, onClose, onChanged }: { id: number; onClose: () => void; onChanged: () => void }) {
  const { hasRole } = useAuth();
  const toast = useToast();
  const { staff } = useLookups();
  const detail = useLoad(() => programmeApi.get(id), [id]);
  const [modal, setModal] = useState<'activity' | 'target' | 'progress' | null>(null);
  const [editingActivity, setEditingActivity] = useState<ProgrammeActivity | null>(null);
  const p = detail.data;
  const refresh = () => { detail.reload(); onChanged(); };
  const officer = hasRole('ADMIN', 'FOREST_OFFICER');
  const readOnly = p?.status === 'COMPLETED' || p?.status === 'CANCELLED';

  const move = async (to: ProgrammeStatus) => {
    try { await programmeApi.setStatus(id, to); toast(`Programme ${label(to).toLowerCase()}`); refresh(); }
    catch (e) { toast(e instanceof Error ? e.message : 'Failed', 'error'); }
  };

  return (
    <Drawer open onClose={onClose}>
      {!p ? <Loading /> : (
        <>
          <div className="drawer-head">
            <h2>{p.name}</h2>
            <div className="sub">{p.code} · {label(p.programmeType)} · <Chip value={p.status} /></div>
            <div className="actions">
              {!readOnly && hasRole('ADMIN', 'FOREST_OFFICER', 'RANGER', 'VET') &&
                <button className="btn btn-primary" onClick={() => setModal('progress')}>Post progress</button>}
              {officer && !readOnly && <>
                <button className="btn btn-secondary" onClick={() => setModal('target')}>Add target</button>
                <button className="btn btn-secondary" onClick={() => setModal('activity')}>Add activity</button>
              </>}
              {officer && MOVES[p.status].map((m) =>
                <button key={m.to} className={m.to === 'CANCELLED' ? 'btn btn-danger' : 'btn btn-ghost'} onClick={() => move(m.to)}>{m.action}</button>)}
            </div>
          </div>
          <div className="drawer-body">
            <Warnings items={p.warnings} />
            <div className="big-progress"><Progress percent={p.progressPercent} /><span className="muted">overall, averaged over targets</span></div>
            <dl className="facts panel">
              <Fact term="Objective" wide>{p.objective}</Fact>
              <Fact term="Period">{fmtDate(p.startDate)} – {fmtDate(p.endDate)}</Fact>
              <Fact term="Budget">{fmtInr(p.budgetInr)}</Fact>
              <Fact term="Lead">{p.leadStaffName ?? '—'}</Fact>
              <Fact term="Areas / species">{p.areaIds.length} / {p.speciesIds.length}</Fact>
            </dl>
            <h3>Targets</h3>
            {p.targets.length === 0 && <p className="muted">No targets yet. Progress is the average of target progress.</p>}
            <ul className="cards">
              {p.targets.map((t) => (
                <li key={t.id} className="card">
                  <div className="card-title">{t.metricName}</div>
                  <Progress percent={t.progressPercent} />
                  <div className="muted small">From {t.baselineValue} to {t.targetValue} {t.unit}; now {t.currentValue}{t.dueDate ? `, due ${fmtDate(t.dueDate)}` : ''}</div>
                </li>
              ))}
            </ul>
            <h3>Activities</h3>
            <DataTable<ProgrammeActivity>
              rows={p.activities} rowKey={(a) => a.id}
              onRowClick={!readOnly && hasRole('ADMIN', 'FOREST_OFFICER', 'RANGER') ? setEditingActivity : undefined}
              empty={<Empty title="No activities yet" />}
              columns={[
                { header: 'Activity', render: (a) => <><strong>{a.title}</strong>{a.overdue && <span className="overdue">Overdue</span>}</> },
                { header: 'When', render: (a) => `${fmtDate(a.plannedStart)} – ${fmtDate(a.plannedEnd)}` },
                { header: 'Status', render: (a) => <Chip value={a.status} /> },
                { header: 'Assigned', render: (a) => a.assignedStaffName ?? '—' },
              ]}
            />
            <h3>Recent progress</h3>
            <ol className="timeline">
              {p.recentProgress.map((u) => (
                <li key={u.id}>
                  <div className="when">{fmtDate(u.updateDate)}</div>
                  <div className="what">{u.targetMetricName ? `${u.targetMetricName}: ${u.previousValue} → ${u.newValue}` : 'General update'}</div>
                  <div>{u.note}</div>
                  <div className="meta"><span>{u.reportedByName}</span></div>
                </li>
              ))}
            </ol>
          </div>

          <FormModal
            open={modal === 'progress'} onClose={() => setModal(null)} title="Post progress" submitLabel="Post progress"
            intro="Choose a target to record its new value, or leave it empty for a general update."
            initial={{ updateDate: todayIso() }}
            fields={[
              { name: 'targetId', label: 'Target', type: 'select', placeholder: 'General update', options: p.targets.map((t) => ({ value: t.id, label: `${t.metricName} (now ${t.currentValue} ${t.unit})` })) },
              { name: 'newValue', label: 'New value', type: 'number', step: 'any' },
              { name: 'updateDate', label: 'Date', type: 'date', required: true },
              { name: 'note', label: 'Note', type: 'textarea', required: true, wide: true },
            ]}
            onSubmit={async (v) => { await programmeApi.addProgress(id, v); toast('Progress posted'); refresh(); }}
          />
          <FormModal
            open={modal === 'target'} onClose={() => setModal(null)} title="Add target" submitLabel="Add target"
            fields={[
              { name: 'metricName', label: 'What is measured', required: true, wide: true },
              { name: 'unit', label: 'Unit', required: true, placeholder: 'snares' },
              { name: 'dueDate', label: 'Due', type: 'date' },
              { name: 'baselineValue', label: 'Baseline', type: 'number', step: 'any', required: true },
              { name: 'targetValue', label: 'Target', type: 'number', step: 'any', required: true, hint: 'Can be lower than the baseline (e.g. 40 → 0).' },
              { name: 'currentValue', label: 'Current value', type: 'number', step: 'any', hint: 'Defaults to the baseline.' },
            ]}
            onSubmit={async (v) => { await programmeApi.addTarget(id, v); toast('Target added'); refresh(); }}
          />
          <FormModal
            open={modal === 'activity' || editingActivity !== null}
            onClose={() => { setModal(null); setEditingActivity(null); }}
            title={editingActivity ? `Edit ${editingActivity.title}` : 'Add activity'}
            submitLabel={editingActivity ? 'Save changes' : 'Add activity'}
            initial={editingActivity ?? { status: 'NOT_STARTED', plannedStart: todayIso() }}
            fields={[
              { name: 'title', label: 'Title', required: true, wide: true },
              { name: 'plannedStart', label: 'Start', type: 'date', required: true },
              { name: 'plannedEnd', label: 'End', type: 'date', required: true },
              { name: 'status', label: 'Status', type: 'select', required: true, enumValues: ACTIVITY_STATUS },
              { name: 'assignedStaffId', label: 'Assigned to', type: 'select', options: staffOptions(staff) },
              { name: 'description', label: 'Description', type: 'textarea', wide: true },
            ]}
            onSubmit={async (v) => {
              if (editingActivity) await programmeApi.updateActivity(id, editingActivity.id, { ...v, areaId: editingActivity.areaId });
              else await programmeApi.addActivity(id, v);
              toast('Activity saved');
              refresh();
            }}
          />
        </>
      )}
    </Drawer>
  );
}
