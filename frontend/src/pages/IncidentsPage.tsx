import { useState } from 'react';
import { Link } from 'react-router-dom';
import { incidentApi } from '../api/endpoints';
import { staffOptions, useLookups, zoneOptions } from '../api/lookups';
import type { IncidentStatus, Role } from '../api/types';
import { useAuth } from '../auth/AuthContext';
import { FormModal } from '../components/Form';
import { Drawer } from '../components/Modal';
import { MapView } from '../components/MapView';
import { useToast } from '../components/Toasts';
import { Chip, ErrorBanner, Fact, Loading, PageHeader, Warnings, fmtCoord, fmtDateTime, label, nowLocalInput, useLoad } from '../components/ui';

export const INCIDENT_TYPES = ['WILDLIFE_INJURY', 'HUMAN_ANIMAL_CONFLICT', 'POACHING', 'ILLEGAL_LOGGING', 'ENCROACHMENT', 'FOREST_FIRE', 'OTHER_THREAT'] as const;
export const SEVERITIES = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'] as const;
export const STATUSES: IncidentStatus[] = ['REPORTED', 'VERIFIED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];
const ASSIGNMENT_ROLES = ['LEAD', 'INVESTIGATOR', 'SUPPORT', 'VETERINARY'] as const;

/** Next step in the workflow and who may take it (CONTRACTS.md 6.1). */
export const NEXT_STEP: Partial<Record<IncidentStatus, { to: IncidentStatus; action: string; roles: Role[] }>> = {
  REPORTED: { to: 'VERIFIED', action: 'Verify', roles: ['ADMIN', 'FOREST_OFFICER'] },
  VERIFIED: { to: 'IN_PROGRESS', action: 'Start work', roles: ['ADMIN', 'FOREST_OFFICER'] },
  IN_PROGRESS: { to: 'RESOLVED', action: 'Resolve', roles: ['ADMIN', 'FOREST_OFFICER', 'RANGER', 'VET'] },
  RESOLVED: { to: 'CLOSED', action: 'Close', roles: ['ADMIN', 'FOREST_OFFICER'] },
};

export function IncidentsPage() {
  const { hasRole } = useAuth();
  const toast = useToast();
  const { zones } = useLookups();
  const [filters, setFilters] = useState({ zoneId: '', incidentType: '', severity: '' });
  const [creating, setCreating] = useState(false);
  const [selected, setSelected] = useState<number | null>(null);
  const list = useLoad(
    () => incidentApi.list({ zoneId: filters.zoneId, type: filters.incidentType, severity: filters.severity, size: 100, sort: 'occurredAt,desc' }),
    [JSON.stringify(filters)],
  );

  const columns = STATUSES.map((s) => ({ status: s, items: (list.data?.content ?? []).filter((i) => i.status === s) }));

  return (
    <>
      <PageHeader
        title="Incident board"
        intro={<>Injuries, conflict, poaching and other threats from report to closure. For one zone's month at a glance, use the <Link to="/incidents/monthly">monthly zone review</Link>.</>}
        actions={hasRole('ADMIN', 'FOREST_OFFICER', 'RANGER', 'VET') && <button className="btn btn-primary" onClick={() => setCreating(true)}>Report incident</button>}
      />
      <div className="toolbar">
        <select className="input" value={filters.zoneId} onChange={(e) => setFilters({ ...filters, zoneId: e.target.value })} aria-label="Zone">
          <option value="">All zones</option>
          {zones.map((z) => <option key={z.id} value={z.id}>{z.name}</option>)}
        </select>
        <select className="input" value={filters.incidentType} onChange={(e) => setFilters({ ...filters, incidentType: e.target.value })} aria-label="Type">
          <option value="">All types</option>
          {INCIDENT_TYPES.map((t) => <option key={t} value={t}>{label(t)}</option>)}
        </select>
        <select className="input" value={filters.severity} onChange={(e) => setFilters({ ...filters, severity: e.target.value })} aria-label="Severity">
          <option value="">Any severity</option>
          {SEVERITIES.map((s) => <option key={s} value={s}>{label(s)}</option>)}
        </select>
      </div>
      <ErrorBanner error={list.error} />
      {list.loading && !list.data ? <Loading /> : (
        <div className="board">
          {columns.map((c) => (
            <section key={c.status} className="board-col">
              <h3>{label(c.status)} <span className="count">{c.items.length}</span></h3>
              {c.items.map((i) => (
                <button key={i.id} className={`incident-card sev-${i.severity.toLowerCase()}`} onClick={() => setSelected(i.id)}>
                  <span className="incident-code">{i.incidentCode}</span>
                  <strong>{i.title}</strong>
                  <span className="muted">{label(i.incidentType)} · {i.zoneName ?? `Zone ${i.zoneId}`}</span>
                  <span className="incident-foot">
                    <Chip value={i.severity} />
                    <span className="muted">{fmtDateTime(i.occurredAt)}</span>
                  </span>
                  {i.assignedPersonnel.length > 0 &&
                    <span className="muted small">{i.assignedPersonnel.map((p) => p.staffName ?? `Staff ${p.staffId}`).join(', ')}</span>}
                </button>
              ))}
            </section>
          ))}
        </div>
      )}

      <FormModal
        open={creating} onClose={() => setCreating(false)} title="Report incident" submitLabel="Report incident"
        initial={{ occurredAt: nowLocalInput(), incidentType: 'HUMAN_ANIMAL_CONFLICT', severity: 'MEDIUM' }}
        fields={[
          { name: 'title', label: 'Title', required: true, wide: true },
          { name: 'incidentType', label: 'Type', type: 'select', required: true, enumValues: INCIDENT_TYPES },
          { name: 'severity', label: 'Severity', type: 'select', required: true, enumValues: SEVERITIES },
          { name: 'zoneId', label: 'Zone', type: 'select', required: true, options: zoneOptions(zones) },
          { name: 'occurredAt', label: 'Occurred at', type: 'datetime', required: true },
          { name: 'latitude', label: 'Latitude', type: 'number', step: 'any', required: true },
          { name: 'longitude', label: 'Longitude', type: 'number', step: 'any', required: true },
          { name: 'locationDescription', label: 'Location description', wide: true },
          { name: 'description', label: 'What happened', type: 'textarea', required: true, wide: true },
        ]}
        onSubmit={async (v) => {
          const zone = zones.find((z) => z.id === v.zoneId);
          const created = await incidentApi.create({ ...v, areaId: zone?.areaId });
          toast(`${created.incidentCode} reported`);
          created.warnings.forEach((w) => toast(w, 'warn'));
          list.reload();
        }}
      />
      {selected !== null && <IncidentDrawer id={selected} onClose={() => setSelected(null)} onChanged={list.reload} />}
    </>
  );
}

export function IncidentDrawer({ id, onClose, onChanged }: { id: number; onClose: () => void; onChanged: () => void }) {
  const { hasRole } = useAuth();
  const toast = useToast();
  const { staff } = useLookups();
  const detail = useLoad(() => incidentApi.get(id), [id]);
  const [modal, setModal] = useState<'transition' | 'assign' | null>(null);
  const [warnings, setWarnings] = useState<string[]>([]);
  const i = detail.data;
  const next = i ? NEXT_STEP[i.status] : undefined;
  const refresh = () => { detail.reload(); onChanged(); };

  return (
    <Drawer open onClose={onClose}>
      {!i ? <Loading /> : (
        <>
          <div className="drawer-head">
            <h2>{i.incidentCode}</h2>
            <div className="sub">{i.title}</div>
            <div className="actions">
              {next && hasRole(...next.roles) && <button className="btn btn-primary" onClick={() => setModal('transition')}>{next.action}</button>}
              {i.status !== 'CLOSED' && hasRole('ADMIN', 'FOREST_OFFICER') &&
                <button className="btn btn-secondary" onClick={() => setModal('assign')}>Assign staff</button>}
            </div>
          </div>
          <div className="drawer-body">
            <Warnings items={[...warnings, ...i.warnings]} />
            <ol className="steps" aria-label="Workflow">
              {STATUSES.map((s, idx) => (
                <li key={s} className={STATUSES.indexOf(i.status) >= idx ? 'done' : ''}>{label(s)}</li>
              ))}
            </ol>
            <dl className="facts panel">
              <Fact term="Type">{label(i.incidentType)}</Fact>
              <Fact term="Severity"><Chip value={i.severity} /></Fact>
              <Fact term="Zone">{i.zoneName ?? i.zoneId}</Fact>
              <Fact term="Occurred">{fmtDateTime(i.occurredAt)}</Fact>
              <Fact term="Location">{fmtCoord(i.latitude, i.longitude)}{i.locationDescription ? `, ${i.locationDescription}` : ''}</Fact>
              <Fact term="Reported by">{i.reportedByName ?? '—'}</Fact>
              <Fact term="Description" wide>{i.description}</Fact>
              {i.resolutionNotes && <Fact term="Resolution" wide>{i.resolutionNotes}</Fact>}
              {i.sourcePatrolLogId && <Fact term="Source">Patrol log #{i.sourcePatrolLogId}</Fact>}
            </dl>
            <MapView height={200} markers={[{ id: i.id, lat: i.latitude, lng: i.longitude, label: i.title, color: '#a93a26' }]} />
            <h3>Assigned personnel</h3>
            {i.assignedPersonnel.length === 0 ? <p className="muted">No one assigned yet. Work can't start until someone is.</p> : (
              <ul className="people">
                {i.assignedPersonnel.map((p) => (
                  <li key={p.assignmentId}>
                    <strong>{p.staffName ?? `Staff ${p.staffId}`}</strong>
                    <span className="muted"> {label(p.assignmentRole)}{p.designation ? `, ${label(p.designation).toLowerCase()}` : ''}</span>
                    {hasRole('ADMIN', 'FOREST_OFFICER') && i.status !== 'CLOSED' && (
                      <button className="link-btn" onClick={async () => {
                        await incidentApi.unassign(i.id, p.assignmentId); toast('Assignment removed'); refresh();
                      }}>Remove</button>
                    )}
                  </li>
                ))}
              </ul>
            )}
            <h3>History</h3>
            <ol className="timeline">
              {i.history.map((h) => (
                <li key={h.id}>
                  <div className="when">{fmtDateTime(h.changedAt)}</div>
                  <div className="what">{h.fromStatus ? `${label(h.fromStatus)} → ${label(h.toStatus)}` : label(h.toStatus)}</div>
                  {h.note && <div>{h.note}</div>}
                  <div className="meta"><span>{h.changedByName}</span></div>
                </li>
              ))}
            </ol>
          </div>

          {next && (
            <FormModal
              open={modal === 'transition'} onClose={() => setModal(null)}
              title={`${next.action}: ${i.incidentCode}`} submitLabel={next.action}
              intro={next.to === 'RESOLVED' ? 'Describe how the incident was resolved.' : undefined}
              fields={[{ name: 'note', label: next.to === 'RESOLVED' ? 'Resolution notes' : 'Note', type: 'textarea', wide: true, required: next.to === 'RESOLVED' }]}
              onSubmit={async (v) => {
                const r = await incidentApi.transition(i.id, next.to, (v.note as string | null) ?? undefined);
                toast(`${r.incidentCode} is now ${label(r.status).toLowerCase()}`);
                refresh();
              }}
            />
          )}
          <FormModal
            open={modal === 'assign'} onClose={() => setModal(null)} title={`Assign staff to ${i.incidentCode}`} submitLabel="Assign"
            intro="The person is also reserved for the next 24 hours in staff allocations."
            initial={{ assignmentRole: 'LEAD' }}
            fields={[
              { name: 'staffId', label: 'Staff member', type: 'select', required: true, options: staffOptions(staff) },
              { name: 'assignmentRole', label: 'Role', type: 'select', required: true, enumValues: ASSIGNMENT_ROLES },
              { name: 'note', label: 'Note', type: 'textarea', wide: true },
            ]}
            onSubmit={async (v) => {
              const a = await incidentApi.assign(i.id, v);
              toast('Staff assigned');
              setWarnings(a.warnings ?? []);
              refresh();
            }}
          />
        </>
      )}
    </Drawer>
  );
}

