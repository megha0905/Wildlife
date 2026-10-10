import { useState } from 'react';
import { resourceApi } from '../api/endpoints';
import { areaOptions, refreshLookups, useLookups } from '../api/lookups';
import type { Allocation, Equipment, Staff, Vehicle } from '../api/types';
import { useAuth } from '../auth/AuthContext';
import { DataTable } from '../components/DataTable';
import { FormModal } from '../components/Form';
import { useToast } from '../components/Toasts';
import { Chip, Empty, ErrorBanner, PageHeader, Tabs, fmtDate, fmtDateTime, label, nowLocalInput, useLoad } from '../components/ui';

const DESIGNATIONS = ['FOREST_OFFICER', 'RANGE_OFFICER', 'FOREST_GUARD', 'VETERINARIAN', 'DRIVER', 'TRACKER', 'RESEARCHER'] as const;
const STAFF_STATUS = ['ACTIVE', 'ON_LEAVE', 'INACTIVE'] as const;
const VEHICLE_TYPES = ['JEEP', 'PICKUP', 'MOTORCYCLE', 'BOAT', 'RESCUE_VAN', 'TRACTOR'] as const;
const EQUIPMENT_TYPES = ['CAMERA_TRAP', 'GPS_UNIT', 'RADIO', 'BINOCULARS', 'DRONE', 'TRANQUILIZER_GUN', 'FIRST_AID_KIT', 'RESCUE_CAGE', 'NIGHT_VISION'] as const;
const RESOURCE_STATUS = ['AVAILABLE', 'IN_USE', 'MAINTENANCE', 'RETIRED'] as const;
const PURPOSES = ['PATROL', 'INCIDENT', 'RESCUE', 'PROGRAMME', 'OTHER'] as const;

type TabId = 'staff' | 'vehicles' | 'equipment' | 'allocations';

export function ResourcesPage() {
  const [tab, setTab] = useState<TabId>('staff');
  return (
    <>
      <PageHeader title="Staff & resources" intro="Field personnel, vehicles and equipment, and who or what is reserved for which activity." />
      <Tabs value={tab} onChange={setTab} tabs={[
        { id: 'staff', label: 'Staff' }, { id: 'vehicles', label: 'Vehicles' },
        { id: 'equipment', label: 'Equipment' }, { id: 'allocations', label: 'Allocations' },
      ]} />
      {tab === 'staff' && <StaffTab />}
      {tab === 'vehicles' && <VehiclesTab />}
      {tab === 'equipment' && <EquipmentTab />}
      {tab === 'allocations' && <AllocationsTab />}
    </>
  );
}

function useCanEdit() {
  return useAuth().hasRole('ADMIN', 'FOREST_OFFICER');
}

function StaffTab() {
  const toast = useToast();
  const canEdit = useCanEdit();
  const { areas } = useLookups();
  const [q, setQ] = useState('');
  const [editing, setEditing] = useState<Staff | 'new' | null>(null);
  const list = useLoad(() => resourceApi.staff({ q }), [q]);
  return (
    <>
      <div className="toolbar">
        <input className="input search" placeholder="Search name or employee code" value={q} onChange={(e) => setQ(e.target.value)} aria-label="Search staff" />
        <div className="spacer" />
        {canEdit && <button className="btn btn-primary" onClick={() => setEditing('new')}>Add staff member</button>}
      </div>
      <ErrorBanner error={list.error} />
      <DataTable<Staff>
        rows={list.data?.content ?? []} rowKey={(s) => s.id} onRowClick={canEdit ? setEditing : undefined}
        empty={<Empty title="No staff found" />}
        columns={[
          { header: 'Name', render: (s) => <strong>{s.fullName}</strong> },
          { header: 'Code', render: (s) => s.employeeCode },
          { header: 'Designation', render: (s) => label(s.designation) },
          { header: 'Status', render: (s) => <Chip value={s.status} /> },
          { header: 'Home area', render: (s) => areas.find((a) => a.id === s.homeAreaId)?.name ?? '—' },
          { header: 'Phone', render: (s) => s.phone ?? '—' },
        ]}
      />
      <FormModal
        open={editing !== null} onClose={() => setEditing(null)}
        title={editing && editing !== 'new' ? `Edit ${editing.fullName}` : 'Add staff member'}
        submitLabel={editing && editing !== 'new' ? 'Save changes' : 'Add staff member'}
        initial={editing && editing !== 'new' ? editing : { designation: 'FOREST_GUARD', status: 'ACTIVE' }}
        fields={[
          { name: 'fullName', label: 'Full name', required: true },
          { name: 'employeeCode', label: 'Employee code', required: true, placeholder: 'EMP-009' },
          { name: 'designation', label: 'Designation', type: 'select', required: true, enumValues: DESIGNATIONS },
          { name: 'status', label: 'Status', type: 'select', required: true, enumValues: STAFF_STATUS },
          { name: 'homeAreaId', label: 'Home area', type: 'select', options: areaOptions(areas) },
          { name: 'joinedOn', label: 'Joined on', type: 'date' },
          { name: 'phone', label: 'Phone' },
          { name: 'email', label: 'Email' },
        ]}
        onSubmit={async (v) => {
          if (editing && editing !== 'new') await resourceApi.updateStaff(editing.id, v);
          else await resourceApi.createStaff(v);
          toast('Staff saved');
          refreshLookups();
          list.reload();
        }}
      />
    </>
  );
}

function VehiclesTab() {
  const toast = useToast();
  const canEdit = useCanEdit();
  const { areas } = useLookups();
  const [editing, setEditing] = useState<Vehicle | 'new' | null>(null);
  const list = useLoad(() => resourceApi.vehicles(), []);
  return (
    <>
      <div className="toolbar"><div className="spacer" />
        {canEdit && <button className="btn btn-primary" onClick={() => setEditing('new')}>Add vehicle</button>}
      </div>
      <ErrorBanner error={list.error} />
      <DataTable<Vehicle>
        rows={list.data?.content ?? []} rowKey={(v) => v.id} onRowClick={canEdit ? setEditing : undefined}
        empty={<Empty title="No vehicles" />}
        columns={[
          { header: 'Registration', render: (v) => <strong>{v.registrationNumber}</strong> },
          { header: 'Type', render: (v) => label(v.vehicleType) },
          { header: 'Make / model', render: (v) => [v.make, v.model].filter(Boolean).join(' ') || '—' },
          { header: 'Seats', render: (v) => v.seatingCapacity ?? '—', className: 'num' },
          { header: 'Status', render: (v) => <Chip value={v.status} /> },
          { header: 'Last serviced', render: (v) => fmtDate(v.lastServicedOn) },
        ]}
      />
      <FormModal
        open={editing !== null} onClose={() => setEditing(null)}
        title={editing && editing !== 'new' ? `Edit ${editing.registrationNumber}` : 'Add vehicle'} submitLabel="Save vehicle"
        initial={editing && editing !== 'new' ? editing : { vehicleType: 'JEEP', status: 'AVAILABLE' }}
        fields={[
          { name: 'registrationNumber', label: 'Registration', required: true, placeholder: 'MP-51-G-1021' },
          { name: 'vehicleType', label: 'Type', type: 'select', required: true, enumValues: VEHICLE_TYPES },
          { name: 'status', label: 'Status', type: 'select', required: true, enumValues: RESOURCE_STATUS },
          { name: 'make', label: 'Make' },
          { name: 'model', label: 'Model' },
          { name: 'seatingCapacity', label: 'Seats', type: 'number' },
          { name: 'homeAreaId', label: 'Home area', type: 'select', options: areaOptions(areas) },
          { name: 'lastServicedOn', label: 'Last serviced', type: 'date' },
        ]}
        onSubmit={async (v) => {
          if (editing && editing !== 'new') await resourceApi.updateVehicle(editing.id, v);
          else await resourceApi.createVehicle(v);
          toast('Vehicle saved');
          list.reload();
        }}
      />
    </>
  );
}

function EquipmentTab() {
  const toast = useToast();
  const canEdit = useCanEdit();
  const { areas } = useLookups();
  const [editing, setEditing] = useState<Equipment | 'new' | null>(null);
  const list = useLoad(() => resourceApi.equipment(), []);
  return (
    <>
      <div className="toolbar"><div className="spacer" />
        {canEdit && <button className="btn btn-primary" onClick={() => setEditing('new')}>Add equipment</button>}
      </div>
      <ErrorBanner error={list.error} />
      <DataTable<Equipment>
        rows={list.data?.content ?? []} rowKey={(e) => e.id} onRowClick={canEdit ? setEditing : undefined}
        empty={<Empty title="No equipment" />}
        columns={[
          { header: 'Asset tag', render: (e) => <strong>{e.assetTag}</strong> },
          { header: 'Name', render: (e) => e.name },
          { header: 'Type', render: (e) => label(e.equipmentType) },
          { header: 'Status', render: (e) => <Chip value={e.status} /> },
          { header: 'Home area', render: (e) => areas.find((a) => a.id === e.homeAreaId)?.name ?? '—' },
          { header: 'Notes', render: (e) => e.notes ?? '—' },
        ]}
      />
      <FormModal
        open={editing !== null} onClose={() => setEditing(null)}
        title={editing && editing !== 'new' ? `Edit ${editing.assetTag}` : 'Add equipment'} submitLabel="Save equipment"
        initial={editing && editing !== 'new' ? editing : { equipmentType: 'CAMERA_TRAP', status: 'AVAILABLE' }}
        fields={[
          { name: 'assetTag', label: 'Asset tag', required: true },
          { name: 'name', label: 'Name', required: true },
          { name: 'equipmentType', label: 'Type', type: 'select', required: true, enumValues: EQUIPMENT_TYPES },
          { name: 'status', label: 'Status', type: 'select', required: true, enumValues: RESOURCE_STATUS },
          { name: 'homeAreaId', label: 'Home area', type: 'select', options: areaOptions(areas) },
          { name: 'purchasedOn', label: 'Purchased on', type: 'date' },
          { name: 'notes', label: 'Notes', type: 'textarea', wide: true },
        ]}
        onSubmit={async (v) => {
          if (editing && editing !== 'new') await resourceApi.updateEquipment(editing.id, v);
          else await resourceApi.createEquipment(v);
          toast('Equipment saved');
          list.reload();
        }}
      />
    </>
  );
}

function AllocationsTab() {
  const toast = useToast();
  const canEdit = useCanEdit();
  const [status, setStatus] = useState('');
  const [creating, setCreating] = useState(false);
  const list = useLoad(() => resourceApi.allocations({ status, size: 50, sort: 'startAt,desc' }), [status]);
  const staff = useLoad(() => resourceApi.staff({ status: 'ACTIVE' }), []);
  const vehicles = useLoad(() => resourceApi.vehicles(), []);
  const equipment = useLoad(() => resourceApi.equipment(), []);

  const resourceOptions = [
    ...(staff.data?.content ?? []).map((s) => ({ value: `STAFF:${s.id}`, label: `Staff: ${s.fullName}` })),
    ...(vehicles.data?.content ?? []).map((v) => ({ value: `VEHICLE:${v.id}`, label: `Vehicle: ${v.registrationNumber}` })),
    ...(equipment.data?.content ?? []).map((e) => ({ value: `EQUIPMENT:${e.id}`, label: `Equipment: ${e.assetTag} ${e.name}` })),
  ];

  const move = async (a: Allocation, to: string) => {
    try {
      await resourceApi.setAllocationStatus(a.id, to);
      toast(`Allocation ${label(to).toLowerCase()}`);
      list.reload();
    } catch (e) {
      toast(e instanceof Error ? e.message : 'Failed', 'error');
    }
  };

  return (
    <>
      <div className="toolbar">
        <select className="input" value={status} onChange={(e) => setStatus(e.target.value)} aria-label="Status">
          <option value="">Any status</option>
          {['PLANNED', 'ACTIVE', 'COMPLETED', 'CANCELLED'].map((s) => <option key={s} value={s}>{label(s)}</option>)}
        </select>
        <div className="spacer" />
        {canEdit && <button className="btn btn-primary" onClick={() => setCreating(true)}>Reserve resource</button>}
      </div>
      <ErrorBanner error={list.error} />
      <DataTable<Allocation>
        rows={list.data?.content ?? []} rowKey={(a) => a.id}
        empty={<Empty title="No allocations" />}
        columns={[
          { header: 'Resource', render: (a) => <><strong>{a.resourceLabel}</strong><div className="muted small">{label(a.resourceKind)}</div></> },
          { header: 'For', render: (a) => <>{label(a.purpose)}{a.referenceLabel && <div className="muted small">{a.referenceLabel}</div>}</> },
          { header: 'From', render: (a) => fmtDateTime(a.startAt) },
          { header: 'To', render: (a) => fmtDateTime(a.endAt) },
          { header: 'Status', render: (a) => <Chip value={a.status} /> },
          { header: '', render: (a) => canEdit && (
            <span className="row-actions">
              {a.status === 'PLANNED' && <button className="link-btn" onClick={() => move(a, 'ACTIVE')}>Activate</button>}
              {a.status === 'ACTIVE' && <button className="link-btn" onClick={() => move(a, 'COMPLETED')}>Complete</button>}
              {(a.status === 'PLANNED' || a.status === 'ACTIVE') && <button className="link-btn danger" onClick={() => move(a, 'CANCELLED')}>Cancel</button>}
            </span>
          ) },
        ]}
      />
      <FormModal
        open={creating} onClose={() => setCreating(false)} title="Reserve resource" submitLabel="Reserve"
        intro="A resource can't be reserved twice for overlapping times; you'll be told which booking clashes."
        initial={{ purpose: 'OTHER', startAt: nowLocalInput(), endAt: nowLocalInput(new Date(Date.now() + 4 * 3600_000)) }}
        fields={[
          { name: 'resource', label: 'Resource', type: 'select', required: true, options: resourceOptions, wide: true },
          { name: 'purpose', label: 'Purpose', type: 'select', required: true, enumValues: PURPOSES },
          { name: 'referenceLabel', label: 'For (description)' },
          { name: 'startAt', label: 'From', type: 'datetime', required: true },
          { name: 'endAt', label: 'To', type: 'datetime', required: true },
          { name: 'notes', label: 'Notes', type: 'textarea', wide: true },
        ]}
        onSubmit={async (v) => {
          const [kind, rid] = String(v.resource ?? '').split(':');
          const { resource: _ignored, ...rest } = v;
          void _ignored;
          await resourceApi.createAllocation({ ...rest, resourceKind: kind, resourceId: Number(rid) });
          toast('Resource reserved');
          list.reload();
        }}
      />
    </>
  );
}
