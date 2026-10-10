import { useState } from 'react';
import { rescueApi } from '../api/endpoints';
import { areaOptions, speciesOptions, useLookups, zoneOptions } from '../api/lookups';
import type { RescueCase, RescueStatus, Role } from '../api/types';
import { useAuth } from '../auth/AuthContext';
import { DataTable, Pager } from '../components/DataTable';
import { FormModal, type Field } from '../components/Form';
import { Drawer } from '../components/Modal';
import { useToast } from '../components/Toasts';
import { Chip, Empty, ErrorBanner, Fact, Loading, PageHeader, Warnings, fmtCoord, fmtDate, fmtDateTime, label, nowLocalInput, todayIso, useLoad } from '../components/ui';

const STAGES: RescueStatus[] = ['REGISTERED', 'UNDER_TREATMENT', 'REHABILITATING', 'RELEASED'];
const CAUSES = ['INJURY', 'ORPHANED', 'HUMAN_CONFLICT', 'SNARE', 'ROAD_ACCIDENT', 'ILLNESS', 'DISPLACED', 'OTHER'] as const;
const CONDITIONS = ['HEALTHY', 'INJURED', 'SICK', 'UNDER_TREATMENT', 'RECOVERING', 'UNKNOWN'] as const;

/** Moves allowed from each stage and who may make them (CONTRACTS.md 6.2). */
const MOVES: Partial<Record<RescueStatus, { to: RescueStatus; action: string; roles: Role[] }[]>> = {
  REGISTERED: [{ to: 'UNDER_TREATMENT', action: 'Start treatment', roles: ['ADMIN', 'FOREST_OFFICER', 'VET'] }],
  UNDER_TREATMENT: [{ to: 'REHABILITATING', action: 'Move to rehabilitation', roles: ['ADMIN', 'VET'] }],
  REHABILITATING: [
    { to: 'RELEASED', action: 'Release', roles: ['ADMIN', 'FOREST_OFFICER'] },
    { to: 'TRANSFERRED', action: 'Transfer', roles: ['ADMIN', 'FOREST_OFFICER', 'VET'] },
  ],
};

export function RescuesPage() {
  const { hasRole } = useAuth();
  const toast = useToast();
  const { species, zones } = useLookups();
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);
  const [creating, setCreating] = useState(false);
  const [selected, setSelected] = useState<number | null>(null);
  const list = useLoad(() => rescueApi.list({ status, page, size: 20 }), [status, page]);

  return (
    <>
      <PageHeader
        title="Rescue tracker"
        intro="Rescued animals from intake through treatment and rehabilitation to release or transfer. Linked animal records update automatically."
        actions={hasRole('ADMIN', 'FOREST_OFFICER', 'RANGER', 'VET') && <button className="btn btn-primary" onClick={() => setCreating(true)}>Register rescue</button>}
      />
      <div className="toolbar">
        <select className="input" value={status} onChange={(e) => { setStatus(e.target.value); setPage(0); }} aria-label="Stage">
          <option value="">All stages</option>
          {[...STAGES, 'TRANSFERRED'].map((s) => <option key={s} value={s}>{label(s)}</option>)}
        </select>
      </div>
      <ErrorBanner error={list.error} />
      {list.loading && !list.data ? <Loading /> : list.data && (
        <>
          <DataTable<RescueCase>
            rows={list.data.content}
            rowKey={(r) => r.id}
            onRowClick={(r) => setSelected(r.id)}
            empty={<Empty title="No rescue cases" />}
            columns={[
              { header: 'Case', render: (r) => <strong>{r.caseNumber}</strong> },
              { header: 'Animal', render: (r) => <>{r.speciesName}{r.animalTagId && <div className="muted small">{r.animalTagId}</div>}</> },
              { header: 'Cause', render: (r) => label(r.cause) },
              { header: 'Rescued', render: (r) => fmtDate(r.rescuedAt) },
              { header: 'Facility', render: (r) => r.facilityName },
              { header: 'Progress', render: (r) => <StageBar status={r.status} /> },
            ]}
          />
          <Pager page={list.data} onPage={setPage} />
        </>
      )}

      <FormModal
        open={creating} onClose={() => setCreating(false)} title="Register rescue" submitLabel="Register rescue"
        intro="If the animal is already in the registry, pick it by ID; its status will change to 'in rescue'."
        initial={{ rescuedAt: nowLocalInput(), cause: 'INJURY', intakeCondition: 'INJURED' }}
        fields={[
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
        ]}
        onSubmit={async (v) => {
          const zone = zones.find((z) => z.id === v.zoneId);
          const r = await rescueApi.create({ ...v, areaId: zone?.areaId ?? null });
          toast(`${r.caseNumber} registered`);
          r.warnings.forEach((w) => toast(w, 'warn'));
          list.reload();
        }}
      />
      {selected !== null && <RescueDrawer id={selected} onClose={() => setSelected(null)} onChanged={list.reload} />}
    </>
  );
}

function StageBar({ status }: { status: RescueStatus }) {
  if (status === 'TRANSFERRED') return <Chip value="TRANSFERRED" />;
  const idx = STAGES.indexOf(status);
  return (
    <span className="stage-bar" title={label(status)}>
      {STAGES.map((s, i) => <span key={s} className={i <= idx ? 'on' : ''} />)}
      <span className="stage-label">{label(status)}</span>
    </span>
  );
}

function RescueDrawer({ id, onClose, onChanged }: { id: number; onClose: () => void; onChanged: () => void }) {
  const { hasRole } = useAuth();
  const toast = useToast();
  const { areas, zones } = useLookups();
  const detail = useLoad(() => rescueApi.get(id), [id]);
  const [modal, setModal] = useState<'treatment' | 'rehab' | RescueStatus | null>(null);
  const [warnings, setWarnings] = useState<string[]>([]);
  const r = detail.data;
  const refresh = () => { detail.reload(); onChanged(); };
  const moves = r ? MOVES[r.status] ?? [] : [];
  const open = r && r.status !== 'RELEASED' && r.status !== 'TRANSFERRED';

  const transitionFields = (to: RescueStatus): Field[] => {
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

  return (
    <Drawer open onClose={onClose}>
      {!r ? <Loading /> : (
        <>
          <div className="drawer-head">
            <h2>{r.caseNumber}<span className="alias">{r.speciesName}</span></h2>
            <div className="sub"><StageBar status={r.status} /></div>
            <div className="actions">
              {moves.filter((m) => hasRole(...m.roles)).map((m) =>
                <button key={m.to} className="btn btn-primary" onClick={() => setModal(m.to)}>{m.action}</button>)}
              {open && hasRole('ADMIN', 'VET') && <button className="btn btn-secondary" onClick={() => setModal('treatment')}>Add treatment</button>}
              {(r.status === 'UNDER_TREATMENT' || r.status === 'REHABILITATING') && hasRole('ADMIN', 'FOREST_OFFICER', 'VET') &&
                <button className="btn btn-secondary" onClick={() => setModal('rehab')}>Add assessment</button>}
            </div>
          </div>
          <div className="drawer-body">
            <Warnings items={[...warnings, ...r.warnings]} />
            <dl className="facts panel">
              <Fact term="Cause">{label(r.cause)}</Fact>
              <Fact term="Condition at intake"><Chip value={r.intakeCondition} /></Fact>
              <Fact term="Registry animal">{r.animalTagId ?? (r.animalId ? `#${r.animalId}` : 'Not in registry')}</Fact>
              <Fact term="Rescued">{fmtDateTime(r.rescuedAt)}</Fact>
              <Fact term="Location">{fmtCoord(r.latitude, r.longitude)}</Fact>
              <Fact term="Facility">{r.facilityName}</Fact>
              {r.incidentId && <Fact term="Linked incident">#{r.incidentId}</Fact>}
              {r.animalDescription && <Fact term="Description" wide>{r.animalDescription}</Fact>}
              {r.transfer && <Fact term="Transferred" wide>{fmtDate(r.transfer.transferDate)} to {r.transfer.destinationFacility}: {r.transfer.reason}</Fact>}
              {r.release && <Fact term="Released" wide>{fmtDate(r.release.releaseDate)} at {fmtCoord(r.release.latitude, r.release.longitude)}{r.release.notes ? `: ${r.release.notes}` : ''}</Fact>}
            </dl>
            <h3>Treatments ({r.treatments.length})</h3>
            {r.treatments.length === 0 && <p className="muted">No treatment recorded yet. At least one is needed before rehabilitation.</p>}
            <ol className="timeline">
              {r.treatments.map((t) => (
                <li key={t.id}>
                  <div className="when">{fmtDate(t.treatmentDate)}</div>
                  <div className="what">{t.diagnosis}</div>
                  <div>{t.procedure}</div>
                  <div className="meta"><span>{t.vetName}</span>{t.medications && <span>{t.medications}</span>}</div>
                </li>
              ))}
            </ol>
            <h3>Rehabilitation assessments ({r.rehabProgress.length})</h3>
            {r.rehabProgress.length === 0 && <p className="muted">The latest assessment must say "fit for release" before the animal can be released.</p>}
            <ol className="timeline">
              {r.rehabProgress.map((p) => (
                <li key={p.id}>
                  <div className="when">{fmtDate(p.assessedOn)}</div>
                  <div className="what">{p.fitForRelease ? 'Fit for release' : 'Not yet fit for release'}{p.weightKg != null ? `, ${p.weightKg} kg` : ''}</div>
                  <div>{p.behaviourNotes}</div>
                  <div className="meta"><span>{p.assessedBy}</span>{p.enclosure && <span>{p.enclosure}</span>}</div>
                </li>
              ))}
            </ol>
            <h3>History</h3>
            <ol className="timeline">
              {r.history.map((h) => (
                <li key={h.id}>
                  <div className="when">{fmtDateTime(h.changedAt)}</div>
                  <div className="what">{h.fromStatus ? `${label(h.fromStatus)} → ${label(h.toStatus)}` : label(h.toStatus)}</div>
                  {h.note && <div>{h.note}</div>}
                  <div className="meta"><span>{h.changedByName}</span></div>
                </li>
              ))}
            </ol>
          </div>

          {moves.map((m) => (
            <FormModal
              key={m.to}
              open={modal === m.to} onClose={() => setModal(null)} title={`${m.action}: ${r.caseNumber}`} submitLabel={m.action}
              initial={{ releaseDate: todayIso(), transferDate: todayIso(), releaseAreaId: r.areaId ?? undefined, releaseZoneId: r.zoneId ?? undefined,
                latitude: r.latitude, longitude: r.longitude }}
              fields={transitionFields(m.to)}
              onSubmit={async (v) => {
                const body: Record<string, unknown> = { toStatus: m.to, note: v.note ?? null };
                if (m.to === 'RELEASED') body.release = v;
                if (m.to === 'TRANSFERRED') body.transfer = v;
                const res = await rescueApi.transition(r.id, body);
                toast(`${res.caseNumber} is now ${label(res.status).toLowerCase()}`);
                setWarnings(res.warnings);
                refresh();
              }}
            />
          ))}
          <FormModal
            open={modal === 'treatment'} onClose={() => setModal(null)} title={`Treatment for ${r.caseNumber}`} submitLabel="Add treatment"
            initial={{ treatmentDate: todayIso() }}
            fields={[
              { name: 'treatmentDate', label: 'Date', type: 'date', required: true },
              { name: 'vetName', label: 'Vet', required: true },
              { name: 'diagnosis', label: 'Diagnosis', required: true, wide: true },
              { name: 'procedure', label: 'Procedure', type: 'textarea', required: true, wide: true },
              { name: 'medications', label: 'Medications', wide: true },
              { name: 'notes', label: 'Notes', type: 'textarea', wide: true },
            ]}
            onSubmit={async (v) => { await rescueApi.addTreatment(r.id, v); toast('Treatment added'); refresh(); }}
          />
          <FormModal
            open={modal === 'rehab'} onClose={() => setModal(null)} title={`Assessment for ${r.caseNumber}`} submitLabel="Add assessment"
            initial={{ assessedOn: todayIso(), fitForRelease: false }}
            fields={[
              { name: 'assessedOn', label: 'Date', type: 'date', required: true },
              { name: 'assessedBy', label: 'Assessed by', required: true },
              { name: 'enclosure', label: 'Enclosure' },
              { name: 'weightKg', label: 'Weight (kg)', type: 'number', step: 'any' },
              { name: 'fitForRelease', label: 'Fit for release', type: 'checkbox', wide: true },
              { name: 'behaviourNotes', label: 'Behaviour and condition', type: 'textarea', required: true, wide: true },
            ]}
            onSubmit={async (v) => { await rescueApi.addRehab(r.id, v); toast('Assessment added'); refresh(); }}
          />
        </>
      )}
    </Drawer>
  );
}
