// Staff & resources: field personnel, vehicles, equipment and their allocations.
import { resourceApi } from '../api/endpoints.js';
import { areaOptions, getLookups, refreshLookups } from '../api/lookups.js';
import { hasRole } from '../auth/auth.js';
import { h } from '../components/dom.js';
import { openFormModal } from '../components/form.js';
import { dataTable } from '../components/table.js';
import { toast } from '../components/toasts.js';
import {
  chip, empty, enumOptions, fmtDate, fmtDateTime, label, nowLocalInput, pageHeader, region, searchInput, selectInput, tabbed,
} from '../components/ui.js';

const DESIGNATIONS = ['FOREST_OFFICER', 'RANGE_OFFICER', 'FOREST_GUARD', 'VETERINARIAN', 'DRIVER', 'TRACKER', 'RESEARCHER'];
const STAFF_STATUS = ['ACTIVE', 'ON_LEAVE', 'INACTIVE'];
const VEHICLE_TYPES = ['JEEP', 'PICKUP', 'MOTORCYCLE', 'BOAT', 'RESCUE_VAN', 'TRACTOR'];
const EQUIPMENT_TYPES = ['CAMERA_TRAP', 'GPS_UNIT', 'RADIO', 'BINOCULARS', 'DRONE', 'TRANQUILIZER_GUN', 'FIRST_AID_KIT', 'RESCUE_CAGE', 'NIGHT_VISION'];
const RESOURCE_STATUS = ['AVAILABLE', 'IN_USE', 'MAINTENANCE', 'RETIRED'];
const PURPOSES = ['PATROL', 'INCIDENT', 'RESCUE', 'PROGRAMME', 'OTHER'];

const canEdit = () => hasRole('ADMIN', 'FOREST_OFFICER');
const areaName = (areas, id) => areas.find((a) => a.id === id)?.name ?? '—';

export function ResourcesPage() {
  return h('div', null,
    pageHeader('Staff & resources', 'Field personnel, vehicles and equipment, and who or what is reserved for which activity.'),
    tabbed([
      { id: 'staff', label: 'Staff', render: () => region(getLookups, staffTab).el },
      { id: 'vehicles', label: 'Vehicles', render: () => region(getLookups, vehiclesTab).el },
      { id: 'equipment', label: 'Equipment', render: () => region(getLookups, equipmentTab).el },
      { id: 'allocations', label: 'Allocations', render: allocationsTab },
    ]));
}

/** A list with an "add" button where clicking a row edits it (staff, vehicles, equipment). */
function catalogue({ load, columns, emptyTitle, addLabel, toolbar = () => [], form }) {
  const edit = canEdit();
  const list = region(load, (data) => dataTable({
    rows: data.content,
    onRowClick: edit ? (row) => form(row, list.reload) : undefined,
    empty: empty(emptyTitle),
    columns,
  }));
  return h('div', null,
    h('div', { class: 'toolbar' }, toolbar(list.reload), h('div', { class: 'spacer' }),
      edit && h('button', { class: 'btn btn-primary', onClick: () => form(null, list.reload) }, addLabel)),
    list.el);
}

function staffTab({ areas }) {
  let q = '';
  return catalogue({
    load: () => resourceApi.staff({ q }),
    emptyTitle: 'No staff found',
    addLabel: 'Add staff member',
    toolbar: (reload) => [searchInput({ placeholder: 'Search name or employee code', ariaLabel: 'Search staff', onSearch: (v) => { q = v; reload(); } })],
    columns: [
      { header: 'Name', render: (s) => h('strong', null, s.fullName) },
      { header: 'Code', render: (s) => s.employeeCode },
      { header: 'Designation', render: (s) => label(s.designation) },
      { header: 'Status', render: (s) => chip(s.status) },
      { header: 'Home area', render: (s) => areaName(areas, s.homeAreaId) },
      { header: 'Phone', render: (s) => s.phone ?? '—' },
    ],
    form: (s, reload) => openFormModal({
      title: s ? `Edit ${s.fullName}` : 'Add staff member',
      submitLabel: s ? 'Save changes' : 'Add staff member',
      initial: s ?? { designation: 'FOREST_GUARD', status: 'ACTIVE' },
      fields: [
        { name: 'fullName', label: 'Full name', required: true },
        { name: 'employeeCode', label: 'Employee code', required: true, placeholder: 'EMP-009' },
        { name: 'designation', label: 'Designation', type: 'select', required: true, enumValues: DESIGNATIONS },
        { name: 'status', label: 'Status', type: 'select', required: true, enumValues: STAFF_STATUS },
        { name: 'homeAreaId', label: 'Home area', type: 'select', options: areaOptions(areas) },
        { name: 'joinedOn', label: 'Joined on', type: 'date' },
        { name: 'phone', label: 'Phone' },
        { name: 'email', label: 'Email' },
      ],
      onSubmit: async (v) => {
        if (s) await resourceApi.updateStaff(s.id, v);
        else await resourceApi.createStaff(v);
        toast('Staff saved');
        refreshLookups();
        reload();
      },
    }),
  });
}

function vehiclesTab({ areas }) {
  return catalogue({
    load: () => resourceApi.vehicles(),
    emptyTitle: 'No vehicles',
    addLabel: 'Add vehicle',
    columns: [
      { header: 'Registration', render: (v) => h('strong', null, v.registrationNumber) },
      { header: 'Type', render: (v) => label(v.vehicleType) },
      { header: 'Make / model', render: (v) => [v.make, v.model].filter(Boolean).join(' ') || '—' },
      { header: 'Seats', render: (v) => v.seatingCapacity ?? '—', className: 'num' },
      { header: 'Status', render: (v) => chip(v.status) },
      { header: 'Last serviced', render: (v) => fmtDate(v.lastServicedOn) },
    ],
    form: (veh, reload) => openFormModal({
      title: veh ? `Edit ${veh.registrationNumber}` : 'Add vehicle',
      submitLabel: 'Save vehicle',
      initial: veh ?? { vehicleType: 'JEEP', status: 'AVAILABLE' },
      fields: [
        { name: 'registrationNumber', label: 'Registration', required: true, placeholder: 'MP-51-G-1021' },
        { name: 'vehicleType', label: 'Type', type: 'select', required: true, enumValues: VEHICLE_TYPES },
        { name: 'status', label: 'Status', type: 'select', required: true, enumValues: RESOURCE_STATUS },
        { name: 'make', label: 'Make' },
        { name: 'model', label: 'Model' },
        { name: 'seatingCapacity', label: 'Seats', type: 'number' },
        { name: 'homeAreaId', label: 'Home area', type: 'select', options: areaOptions(areas) },
        { name: 'lastServicedOn', label: 'Last serviced', type: 'date' },
      ],
      onSubmit: async (v) => {
        if (veh) await resourceApi.updateVehicle(veh.id, v);
        else await resourceApi.createVehicle(v);
        toast('Vehicle saved');
        reload();
      },
    }),
  });
}

function equipmentTab({ areas }) {
  return catalogue({
    load: () => resourceApi.equipment(),
    emptyTitle: 'No equipment',
    addLabel: 'Add equipment',
    columns: [
      { header: 'Asset tag', render: (e) => h('strong', null, e.assetTag) },
      { header: 'Name', render: (e) => e.name },
      { header: 'Type', render: (e) => label(e.equipmentType) },
      { header: 'Status', render: (e) => chip(e.status) },
      { header: 'Home area', render: (e) => areaName(areas, e.homeAreaId) },
      { header: 'Notes', render: (e) => e.notes ?? '—' },
    ],
    form: (eq, reload) => openFormModal({
      title: eq ? `Edit ${eq.assetTag}` : 'Add equipment',
      submitLabel: 'Save equipment',
      initial: eq ?? { equipmentType: 'CAMERA_TRAP', status: 'AVAILABLE' },
      fields: [
        { name: 'assetTag', label: 'Asset tag', required: true },
        { name: 'name', label: 'Name', required: true },
        { name: 'equipmentType', label: 'Type', type: 'select', required: true, enumValues: EQUIPMENT_TYPES },
        { name: 'status', label: 'Status', type: 'select', required: true, enumValues: RESOURCE_STATUS },
        { name: 'homeAreaId', label: 'Home area', type: 'select', options: areaOptions(areas) },
        { name: 'purchasedOn', label: 'Purchased on', type: 'date' },
        { name: 'notes', label: 'Notes', type: 'textarea', wide: true },
      ],
      onSubmit: async (v) => {
        if (eq) await resourceApi.updateEquipment(eq.id, v);
        else await resourceApi.createEquipment(v);
        toast('Equipment saved');
        reload();
      },
    }),
  });
}

function allocationsTab() {
  const edit = canEdit();
  let status = '';

  const move = async (a, to) => {
    try {
      await resourceApi.setAllocationStatus(a.id, to);
      toast(`Allocation ${label(to).toLowerCase()}`);
      list.reload();
    } catch (e) {
      toast(e instanceof Error ? e.message : 'Failed', 'error');
    }
  };

  const list = region(() => resourceApi.allocations({ status, size: 50, sort: 'startAt,desc' }), (data) => dataTable({
    rows: data.content,
    empty: empty('No allocations'),
    columns: [
      { header: 'Resource', render: (a) => [h('strong', null, a.resourceLabel), h('div', { class: 'muted small' }, label(a.resourceKind))] },
      { header: 'For', render: (a) => [label(a.purpose), a.referenceLabel && h('div', { class: 'muted small' }, a.referenceLabel)] },
      { header: 'From', render: (a) => fmtDateTime(a.startAt) },
      { header: 'To', render: (a) => fmtDateTime(a.endAt) },
      { header: 'Status', render: (a) => chip(a.status) },
      { header: '', render: (a) => edit && h('span', { class: 'row-actions' },
        a.status === 'PLANNED' && h('button', { class: 'link-btn', onClick: () => move(a, 'ACTIVE') }, 'Activate'),
        a.status === 'ACTIVE' && h('button', { class: 'link-btn', onClick: () => move(a, 'COMPLETED') }, 'Complete'),
        (a.status === 'PLANNED' || a.status === 'ACTIVE') && h('button', { class: 'link-btn danger', onClick: () => move(a, 'CANCELLED') }, 'Cancel')) },
    ],
  }));

  const reserve = async () => {
    const safe = (p) => p.then((r) => r.content).catch(() => []);
    const [staff, vehicles, equipment] = await Promise.all([
      safe(resourceApi.staff({ status: 'ACTIVE' })), safe(resourceApi.vehicles()), safe(resourceApi.equipment()),
    ]);
    const resourceOptions = [
      ...staff.map((s) => ({ value: `STAFF:${s.id}`, label: `Staff: ${s.fullName}` })),
      ...vehicles.map((v) => ({ value: `VEHICLE:${v.id}`, label: `Vehicle: ${v.registrationNumber}` })),
      ...equipment.map((e) => ({ value: `EQUIPMENT:${e.id}`, label: `Equipment: ${e.assetTag} ${e.name}` })),
    ];
    openFormModal({
      title: 'Reserve resource',
      submitLabel: 'Reserve',
      intro: "A resource can't be reserved twice for overlapping times; you'll be told which booking clashes.",
      initial: { purpose: 'OTHER', startAt: nowLocalInput(), endAt: nowLocalInput(new Date(Date.now() + 4 * 3600_000)) },
      fields: [
        { name: 'resource', label: 'Resource', type: 'select', required: true, options: resourceOptions, wide: true },
        { name: 'purpose', label: 'Purpose', type: 'select', required: true, enumValues: PURPOSES },
        { name: 'referenceLabel', label: 'For (description)' },
        { name: 'startAt', label: 'From', type: 'datetime', required: true },
        { name: 'endAt', label: 'To', type: 'datetime', required: true },
        { name: 'notes', label: 'Notes', type: 'textarea', wide: true },
      ],
      onSubmit: async (v) => {
        const { resource, ...rest } = v;
        const [kind, rid] = String(resource ?? '').split(':');
        await resourceApi.createAllocation({ ...rest, resourceKind: kind, resourceId: Number(rid) });
        toast('Resource reserved');
        list.reload();
      },
    });
  };

  return h('div', null,
    h('div', { class: 'toolbar' },
      selectInput({ ariaLabel: 'Status', placeholder: 'Any status', options: enumOptions(['PLANNED', 'ACTIVE', 'COMPLETED', 'CANCELLED']),
        onChange: (v) => { status = v; list.reload(); } }),
      h('div', { class: 'spacer' }),
      edit && h('button', { class: 'btn btn-primary', onClick: reserve }, 'Reserve resource')),
    list.el);
}
