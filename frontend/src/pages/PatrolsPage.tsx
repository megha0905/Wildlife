import { useMemo, useState } from 'react';
import { patrolApi, resourceApi } from '../api/endpoints';
import { areaOptions, staffOptions, useLookups, zoneOptions } from '../api/lookups';
import type { PatrolLog, PatrolRoute, Schedule, Team, Vehicle } from '../api/types';
import { useAuth } from '../auth/AuthContext';
import { DataTable } from '../components/DataTable';
import { FormModal } from '../components/Form';
import { Drawer } from '../components/Modal';
import { MapView } from '../components/MapView';
import { useToast } from '../components/Toasts';
import { Chip, Empty, ErrorBanner, Fact, Loading, PageHeader, Tabs, Warnings, fmtDate, fmtDateTime, label, nowLocalInput, todayIso, useLoad } from '../components/ui';

const SHIFTS = ['MORNING', 'EVENING', 'NIGHT'] as const;
const OBSERVATIONS = ['ROUTINE', 'ANIMAL_SIGHTING', 'SUSPICIOUS_ACTIVITY', 'SNARE_FOUND', 'INFRASTRUCTURE_DAMAGE', 'FIRE', 'OTHER'] as const;
const SEVERITY = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'] as const;
const INCIDENT_TYPES = ['WILDLIFE_INJURY', 'HUMAN_ANIMAL_CONFLICT', 'POACHING', 'ILLEGAL_LOGGING', 'ENCROACHMENT', 'FOREST_FIRE', 'OTHER_THREAT'] as const;
const DIFFICULTY = ['EASY', 'MODERATE', 'HARD'] as const;

function addDays(iso: string, n: number) {
  const d = new Date(iso + 'T00:00:00');
  d.setDate(d.getDate() + n);
  return todayIso(d);
}

export function PatrolsPage() {
  const [tab, setTab] = useState<'schedules' | 'teams' | 'routes'>('schedules');
  return (
    <>
      <PageHeader title="Patrol planner" intro="Plan patrols for teams and routes, then follow them in the field. Scheduling a patrol reserves the team and vehicle." />
      <Tabs value={tab} onChange={setTab} tabs={[
        { id: 'schedules', label: 'Schedule' }, { id: 'teams', label: 'Teams' }, { id: 'routes', label: 'Routes' },
      ]} />
      {tab === 'schedules' && <SchedulesTab />}
      {tab === 'teams' && <TeamsTab />}
      {tab === 'routes' && <RoutesTab />}
    </>
  );
}

// ==================================================================== schedules

function SchedulesTab() {
  const { hasRole } = useAuth();
  const toast = useToast();
  const [from, setFrom] = useState(addDays(todayIso(), -3));
  const [status, setStatus] = useState('');
  const to = addDays(from, 10);
  const list = useLoad(() => patrolApi.schedules({ from, to, status, size: 100, sort: 'patrolDate,asc' }), [from, status]);
  const teams = useLoad(() => patrolApi.teams({ active: true }), []);
  const routes = useLoad(() => patrolApi.routes(), []);
  const vehicles = useLoad(() => resourceApi.vehicles({ status: 'AVAILABLE' }), []);
  const [creating, setCreating] = useState(false);
  const [selected, setSelected] = useState<number | null>(null);

  const byDay = useMemo(() => {
    const groups = new Map<string, Schedule[]>();
    for (const s of list.data?.content ?? []) {
      groups.set(s.patrolDate, [...(groups.get(s.patrolDate) ?? []), s]);
    }
    return [...groups.entries()].sort(([a], [b]) => a.localeCompare(b));
  }, [list.data]);

  return (
    <>
      <div className="toolbar">
        <button className="btn btn-secondary" onClick={() => setFrom(addDays(from, -7))}>Previous week</button>
        <input className="input" type="date" value={from} onChange={(e) => setFrom(e.target.value)} aria-label="From date" />
        <button className="btn btn-secondary" onClick={() => setFrom(addDays(from, 7))}>Next week</button>
        <select className="input" value={status} onChange={(e) => setStatus(e.target.value)} aria-label="Status">
          <option value="">Any status</option>
          {['SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'MISSED'].map((s) => <option key={s} value={s}>{label(s)}</option>)}
        </select>
        <div className="spacer" />
        {hasRole('ADMIN', 'FOREST_OFFICER') && <button className="btn btn-primary" onClick={() => setCreating(true)}>Schedule patrol</button>}
      </div>
      <p className="muted small">Showing {fmtDate(from)} to {fmtDate(to)}.</p>
      <ErrorBanner error={list.error} />
      {list.loading && !list.data ? <Loading /> : byDay.length === 0 ? (
        <Empty title="No patrols in this period">Pick another week or schedule a patrol.</Empty>
      ) : (
        <div className="day-list">
          {byDay.map(([day, items]) => (
            <section key={day} className={`day ${day === todayIso() ? 'today' : ''}`}>
              <h3>{fmtDate(day)}{day === todayIso() && <span className="today-tag">Today</span>}</h3>
              {items.map((s) => (
                <button key={s.id} className="patrol-card" onClick={() => setSelected(s.id)}>
                  <span className="patrol-time">{s.startTime}–{s.endTime}</span>
                  <span className="patrol-main">
                    <strong>{s.teamName}</strong>
                    <span className="muted">{s.routeName}{s.zoneName ? ` · ${s.zoneName}` : ''}</span>
                  </span>
                  <span className="patrol-meta">
                    <span className="muted">{s.code} · {label(s.shift)}</span>
                    <Chip value={s.status} />
                  </span>
                </button>
              ))}
            </section>
          ))}
        </div>
      )}

      <FormModal
        open={creating} onClose={() => setCreating(false)} title="Schedule patrol" submitLabel="Schedule patrol"
        intro="Every team member and the vehicle are reserved for this time. If one is already booked, you'll see which booking clashes."
        initial={{ patrolDate: todayIso(), shift: 'MORNING', startTime: '06:00', endTime: '12:00' }}
        fields={[
          { name: 'teamId', label: 'Team', type: 'select', required: true, options: (teams.data?.content ?? []).map((t: Team) => ({ value: t.id, label: t.name })) },
          { name: 'routeId', label: 'Route', type: 'select', required: true, options: (routes.data?.content ?? []).map((r: PatrolRoute) => ({ value: r.id, label: `${r.name} (${r.zoneName ?? 'zone ' + r.zoneId})` })) },
          { name: 'patrolDate', label: 'Date', type: 'date', required: true },
          { name: 'shift', label: 'Shift', type: 'select', required: true, enumValues: SHIFTS },
          { name: 'startTime', label: 'Start', type: 'time', required: true },
          { name: 'endTime', label: 'End', type: 'time', required: true, hint: 'Night patrols may end after midnight.' },
          { name: 'vehicleId', label: 'Vehicle', type: 'select', placeholder: 'No vehicle', options: (vehicles.data?.content ?? []).map((v: Vehicle) => ({ value: v.id, label: `${v.registrationNumber} (${label(v.vehicleType)})` })) },
          { name: 'objective', label: 'Objective', type: 'textarea', wide: true },
        ]}
        onSubmit={async (v) => {
          const s = await patrolApi.createSchedule(v);
          toast(`${s.code} scheduled`);
          s.warnings.forEach((w) => toast(w, 'warn'));
          list.reload();
        }}
      />
      {selected !== null && <ScheduleDrawer id={selected} onClose={() => setSelected(null)} onChanged={list.reload} />}
    </>
  );
}

function ScheduleDrawer({ id, onClose, onChanged }: { id: number; onClose: () => void; onChanged: () => void }) {
  const { hasRole } = useAuth();
  const toast = useToast();
  const detail = useLoad(() => patrolApi.schedule(id), [id]);
  const [logging, setLogging] = useState(false);
  const [escalating, setEscalating] = useState<PatrolLog | null>(null);
  const [warnings, setWarnings] = useState<string[]>([]);
  const s = detail.data;
  const fieldRole = hasRole('ADMIN', 'FOREST_OFFICER', 'RANGER');

  const move = async (status: string) => {
    try {
      const r = await patrolApi.setScheduleStatus(id, status);
      toast(`${r.code} is now ${label(r.status).toLowerCase()}`);
      setWarnings(r.warnings);
      detail.reload();
      onChanged();
    } catch (e) {
      toast(e instanceof Error ? e.message : 'Failed', 'error');
    }
  };

  return (
    <Drawer open onClose={onClose}>
      {!s ? <Loading /> : (
        <>
          <div className="drawer-head">
            <h2>{s.code}<span className="alias">{s.teamName}</span></h2>
            <div className="sub">{fmtDate(s.patrolDate)}, {s.startTime}–{s.endTime} ({label(s.shift).toLowerCase()}) · <Chip value={s.status} /></div>
            {fieldRole && (
              <div className="actions">
                {s.status === 'SCHEDULED' && <>
                  <button className="btn btn-primary" onClick={() => move('IN_PROGRESS')}>Start patrol</button>
                  <button className="btn btn-secondary" onClick={() => move('MISSED')}>Mark missed</button>
                  <button className="btn btn-danger" onClick={() => move('CANCELLED')}>Cancel</button>
                </>}
                {s.status === 'IN_PROGRESS' && <>
                  <button className="btn btn-primary" onClick={() => setLogging(true)}>Add log</button>
                  <button className="btn btn-secondary" onClick={() => move('COMPLETED')}>Complete patrol</button>
                </>}
                {s.status === 'COMPLETED' && <button className="btn btn-secondary" onClick={() => setLogging(true)}>Add log</button>}
              </div>
            )}
          </div>
          <div className="drawer-body">
            <Warnings items={warnings} />
            <dl className="facts panel">
              <Fact term="Route">{s.route.name} ({s.route.distanceKm} km, {label(s.route.difficulty).toLowerCase()})</Fact>
              <Fact term="Zone">{s.zoneName ?? s.zoneId}</Fact>
              <Fact term="Team">{s.team.members.map((m) => m.fullName ?? `Staff ${m.id}`).join(', ')}</Fact>
              <Fact term="Leader">{s.team.leaderName ?? s.team.leaderStaffId}</Fact>
              <Fact term="Vehicle">{s.vehicleId ? `Vehicle ${s.vehicleId}` : 'None'}</Fact>
              <Fact term="Reserved">{s.allocationIds.length} allocation(s)</Fact>
              {s.objective && <Fact term="Objective" wide>{s.objective}</Fact>}
            </dl>
            <MapView height={220}
              path={s.route.waypoints.map((w) => [w.latitude, w.longitude] as [number, number])}
              markers={s.logs.map((l) => ({ id: l.id, lat: l.latitude, lng: l.longitude, label: `${label(l.observationType)}: ${l.description}`,
                color: l.observationType === 'ROUTINE' || l.observationType === 'ANIMAL_SIGHTING' ? '#2c5246' : '#a93a26' }))} />
            <h3>Patrol log ({s.logs.length})</h3>
            {s.logs.length === 0 && <Empty title="Nothing logged yet">Logs can be added once the patrol has started.</Empty>}
            <ol className="timeline">
              {s.logs.map((l) => (
                <li key={l.id}>
                  <div className="when">{fmtDateTime(l.loggedAt)}</div>
                  <div className="what">{label(l.observationType)} {l.severityHint && <Chip value={l.severityHint} />}</div>
                  <div>{l.description}</div>
                  <div className="meta">
                    {l.animalTagId && <span>Animal {l.animalTagId}</span>}
                    {l.reportedByName && <span>By {l.reportedByName}</span>}
                    {l.incidentId && <span>Escalated to incident #{l.incidentId}</span>}
                  </div>
                  {!l.incidentId && fieldRole && l.observationType !== 'ROUTINE' && l.observationType !== 'ANIMAL_SIGHTING' &&
                    <button className="link-btn" onClick={() => setEscalating(l)}>Escalate to incident</button>}
                </li>
              ))}
            </ol>
          </div>

          <FormModal
            open={logging} onClose={() => setLogging(false)} title={`Log for ${s.code}`} submitLabel="Add log"
            intro="Mention an animal tag to update that animal's last sighting automatically."
            initial={{ loggedAt: nowLocalInput(), observationType: 'ROUTINE', latitude: s.route.waypoints[0]?.latitude, longitude: s.route.waypoints[0]?.longitude }}
            fields={[
              { name: 'loggedAt', label: 'Time', type: 'datetime', required: true },
              { name: 'observationType', label: 'Observation', type: 'select', required: true, enumValues: OBSERVATIONS },
              { name: 'latitude', label: 'Latitude', type: 'number', step: 'any', required: true },
              { name: 'longitude', label: 'Longitude', type: 'number', step: 'any', required: true },
              { name: 'animalTagId', label: 'Animal tag', placeholder: 'TGR-KNH-001' },
              { name: 'severityHint', label: 'Severity', type: 'select', enumValues: SEVERITY, placeholder: 'Not applicable' },
              { name: 'description', label: 'What was seen', type: 'textarea', required: true, wide: true },
            ]}
            onSubmit={async (v) => {
              const l = await patrolApi.addLog(id, v);
              toast('Log added');
              l.warnings.forEach((w) => toast(w, 'warn'));
              detail.reload();
            }}
          />
          <FormModal
            open={escalating !== null} onClose={() => setEscalating(null)} title="Escalate to incident" submitLabel="Create incident"
            intro="Creates an incident in this zone with the log's location and time."
            initial={{ title: escalating?.description.slice(0, 120), severity: escalating?.severityHint ?? 'MEDIUM',
              incidentType: escalating?.observationType === 'SNARE_FOUND' || escalating?.observationType === 'SUSPICIOUS_ACTIVITY' ? 'POACHING'
                : escalating?.observationType === 'FIRE' ? 'FOREST_FIRE' : 'OTHER_THREAT' }}
            fields={[
              { name: 'title', label: 'Title', required: true, wide: true },
              { name: 'incidentType', label: 'Type', type: 'select', required: true, enumValues: INCIDENT_TYPES },
              { name: 'severity', label: 'Severity', type: 'select', required: true, enumValues: SEVERITY },
            ]}
            onSubmit={async (v) => {
              const l = await patrolApi.escalate(escalating!.id, v);
              toast(`Incident #${l.incidentId} created`);
              detail.reload();
            }}
          />
        </>
      )}
    </Drawer>
  );
}

// ==================================================================== teams & routes

function TeamsTab() {
  const { hasRole } = useAuth();
  const toast = useToast();
  const { areas, staff } = useLookups();
  const list = useLoad(() => patrolApi.teams(), []);
  const [creating, setCreating] = useState(false);
  return (
    <>
      <div className="toolbar">
        <div className="spacer" />
        {hasRole('ADMIN', 'FOREST_OFFICER') && <button className="btn btn-primary" onClick={() => setCreating(true)}>Add team</button>}
      </div>
      <ErrorBanner error={list.error} />
      <DataTable<Team>
        rows={list.data?.content ?? []}
        rowKey={(t) => t.id}
        empty={<Empty title="No patrol teams yet" />}
        columns={[
          { header: 'Team', render: (t) => <strong>{t.name}</strong> },
          { header: 'Area', render: (t) => areas.find((a) => a.id === t.areaId)?.name ?? t.areaId },
          { header: 'Leader', render: (t) => t.leaderName ?? t.leaderStaffId },
          { header: 'Members', render: (t) => t.members.map((m) => m.fullName ?? m.id).join(', ') },
          { header: 'Active', render: (t) => (t.active ? 'Yes' : 'No') },
        ]}
      />
      <FormModal
        open={creating} onClose={() => setCreating(false)} title="Add patrol team" submitLabel="Add team"
        fields={[
          { name: 'name', label: 'Team name', required: true },
          { name: 'areaId', label: 'Protected area', type: 'select', required: true, options: areaOptions(areas) },
          { name: 'leaderStaffId', label: 'Leader', type: 'select', required: true, options: staffOptions(staff) },
          { name: 'memberStaffIds', label: 'Member staff IDs', type: 'ids', required: true, wide: true,
            hint: `Comma-separated. Active staff: ${staff.filter((s) => s.status === 'ACTIVE').map((s) => `${s.id} ${s.fullName}`).join(', ')}` },
        ]}
        onSubmit={async (v) => {
          const t = await patrolApi.createTeam(v);
          toast(`${t.name} added`);
          t.warnings.forEach((w) => toast(w, 'warn'));
          list.reload();
        }}
      />
    </>
  );
}

function RoutesTab() {
  const { hasRole } = useAuth();
  const toast = useToast();
  const { areas, zones } = useLookups();
  const list = useLoad(() => patrolApi.routes(), []);
  const [creating, setCreating] = useState(false);
  const [selected, setSelected] = useState<PatrolRoute | null>(null);
  return (
    <>
      <div className="toolbar">
        <div className="spacer" />
        {hasRole('ADMIN', 'FOREST_OFFICER') && <button className="btn btn-primary" onClick={() => setCreating(true)}>Add route</button>}
      </div>
      <ErrorBanner error={list.error} />
      <DataTable<PatrolRoute>
        rows={list.data?.content ?? []}
        rowKey={(r) => r.id}
        onRowClick={setSelected}
        empty={<Empty title="No routes yet" />}
        columns={[
          { header: 'Route', render: (r) => <strong>{r.name}</strong> },
          { header: 'Zone', render: (r) => r.zoneName ?? r.zoneId },
          { header: 'Distance', render: (r) => `${r.distanceKm} km`, className: 'num' },
          { header: 'Hours', render: (r) => r.estimatedHours ?? '—', className: 'num' },
          { header: 'Difficulty', render: (r) => label(r.difficulty) },
          { header: 'Waypoints', render: (r) => r.waypoints.length, className: 'num' },
        ]}
      />
      {selected && selected.waypoints.length > 0 && (
        <div className="panel">
          <h3>{selected.name}</h3>
          <MapView height={260} path={selected.waypoints.map((w) => [w.latitude, w.longitude] as [number, number])}
            markers={selected.waypoints.map((w) => ({ id: w.sequence, lat: w.latitude, lng: w.longitude, label: `${w.sequence}. ${w.label ?? ''}` }))} />
        </div>
      )}
      <FormModal
        open={creating} onClose={() => setCreating(false)} title="Add patrol route" submitLabel="Add route"
        initial={{ difficulty: 'MODERATE' }}
        fields={[
          { name: 'name', label: 'Route name', required: true },
          { name: 'areaId', label: 'Protected area', type: 'select', required: true, options: areaOptions(areas) },
          { name: 'zoneId', label: 'Zone', type: 'select', required: true, options: zoneOptions(zones) },
          { name: 'distanceKm', label: 'Distance (km)', type: 'number', step: 'any', required: true },
          { name: 'estimatedHours', label: 'Estimated hours', type: 'number', step: 'any' },
          { name: 'difficulty', label: 'Difficulty', type: 'select', required: true, enumValues: DIFFICULTY },
          { name: 'description', label: 'Description', type: 'textarea', wide: true },
        ]}
        onSubmit={async (v) => {
          const r = await patrolApi.createRoute({ ...v, waypoints: [] });
          toast(`${r.name} added`);
          r.warnings.forEach((w) => toast(w, 'warn'));
          list.reload();
        }}
      />
    </>
  );
}
