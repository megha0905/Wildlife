import { useMemo, useState } from 'react';
import { habitatApi } from '../api/endpoints';
import { areaOptions, refreshLookups, useLookups, zoneOptions } from '../api/lookups';
import type { WaterResource, Zone } from '../api/types';
import { useAuth } from '../auth/AuthContext';
import { DataTable } from '../components/DataTable';
import { FormModal } from '../components/Form';
import { Drawer } from '../components/Modal';
import { MapView, type MapMarker } from '../components/MapView';
import { useToast } from '../components/Toasts';
import { Chip, Empty, ErrorBanner, Fact, Loading, PageHeader, fmtCoord, fmtDate, fmtDateTime, label, nowLocalInput, useLoad } from '../components/ui';

const ZONE_COLORS: Record<string, string> = { CORE: '#2c5246', BUFFER: '#9fbdb1', ECO_SENSITIVE: '#a8740f', TOURISM: '#6a7fa8' };
const WATER_COLORS: Record<string, string> = { AVAILABLE: '#3a7fc1', LOW: '#d9a441', DRY: '#a93a26', CONTAMINATED: '#7a3b8f' };
const AREA_TYPES = ['NATIONAL_PARK', 'WILDLIFE_SANCTUARY', 'TIGER_RESERVE', 'CONSERVATION_RESERVE', 'COMMUNITY_RESERVE', 'BIOSPHERE_RESERVE'] as const;
const ZONE_TYPES = ['CORE', 'BUFFER', 'ECO_SENSITIVE', 'TOURISM'] as const;
const HABITAT_TYPES = ['GRASSLAND', 'TROPICAL_DRY_FOREST', 'TROPICAL_MOIST_FOREST', 'WETLAND', 'RIVERINE', 'DESERT', 'MONTANE', 'MANGROVE', 'SCRUBLAND'] as const;
const CONDITIONS = ['GOOD', 'FAIR', 'DEGRADED', 'CRITICAL'] as const;
const WATER_TYPES = ['RIVER', 'STREAM', 'LAKE', 'POND', 'WATERHOLE', 'ARTIFICIAL_WATERHOLE', 'SPRING'] as const;
const WATER_STATUS = ['AVAILABLE', 'LOW', 'DRY', 'CONTAMINATED'] as const;

export function ZonesPage() {
  const { hasRole } = useAuth();
  const toast = useToast();
  const lookups = useLookups();
  const [areaId, setAreaId] = useState('');
  const [selected, setSelected] = useState<Zone | null>(null);
  const [modal, setModal] = useState<'area' | 'zone' | null>(null);
  const zones = useLoad(() => habitatApi.zones({ areaId }), [areaId]);
  const water = useLoad(() => habitatApi.water({ areaId }), [areaId]);
  const canEdit = hasRole('ADMIN', 'FOREST_OFFICER');

  const markers = useMemo<MapMarker[]>(() => [
    ...(zones.data?.content ?? []).map((z) => ({
      id: `z${z.id}`, lat: z.centerLatitude, lng: z.centerLongitude, radius: 11,
      label: `${z.name} (${label(z.zoneType)})`, color: ZONE_COLORS[z.zoneType], onClick: () => setSelected(z),
    })),
    ...(water.data?.content ?? []).map((w) => ({
      id: `w${w.id}`, lat: w.latitude, lng: w.longitude, radius: 5,
      label: `${w.name}: ${label(w.status)}`, color: WATER_COLORS[w.status],
    })),
  ], [zones.data, water.data]);

  return (
    <>
      <PageHeader
        title="Zones & map"
        intro="Protected areas, their management zones, habitats and water sources. Large dots are zones; small dots are water sources coloured by status."
        actions={canEdit && <>
          <button className="btn btn-secondary" onClick={() => setModal('area')}>Add area</button>
          <button className="btn btn-primary" onClick={() => setModal('zone')}>Add zone</button>
        </>}
      />
      <div className="toolbar">
        <select className="input" value={areaId} onChange={(e) => setAreaId(e.target.value)} aria-label="Protected area">
          <option value="">All protected areas</option>
          {lookups.areas.map((a) => <option key={a.id} value={a.id}>{a.name}</option>)}
        </select>
        <div className="legend">
          {Object.entries(WATER_COLORS).map(([k, c]) => <span key={k}><i style={{ background: c }} />{label(k)}</span>)}
        </div>
      </div>
      <ErrorBanner error={zones.error} />
      <MapView markers={markers} height={380} />

      <h2 className="section-title">Zones</h2>
      {zones.loading && !zones.data ? <Loading /> : (
        <DataTable<Zone>
          rows={zones.data?.content ?? []}
          rowKey={(z) => z.id}
          onRowClick={setSelected}
          stripe={(z) => ZONE_COLORS[z.zoneType]}
          empty={<Empty title="No zones yet" />}
          columns={[
            { header: 'Zone', render: (z) => <strong>{z.name}</strong> },
            { header: 'Code', render: (z) => z.code },
            { header: 'Protected area', render: (z) => z.areaName },
            { header: 'Type', render: (z) => label(z.zoneType) },
            { header: 'Area (km²)', render: (z) => z.areaSqKm ?? '—', className: 'num' },
            { header: 'Centre', render: (z) => fmtCoord(z.centerLatitude, z.centerLongitude) },
          ]}
        />
      )}

      {selected && <ZoneDrawer zone={selected} onClose={() => setSelected(null)} onChanged={() => water.reload()} />}

      <FormModal
        open={modal === 'area'} onClose={() => setModal(null)} title="Add protected area" submitLabel="Add area"
        initial={{ areaType: 'TIGER_RESERVE' }}
        fields={[
          { name: 'name', label: 'Name', required: true },
          { name: 'code', label: 'Code', required: true, placeholder: 'KNH' },
          { name: 'areaType', label: 'Type', type: 'select', required: true, enumValues: AREA_TYPES },
          { name: 'state', label: 'State', required: true },
          { name: 'district', label: 'District' },
          { name: 'totalAreaSqKm', label: 'Total area (km²)', type: 'number', step: 'any', required: true },
          { name: 'establishedYear', label: 'Established', type: 'number' },
          { name: 'centerLatitude', label: 'Centre latitude', type: 'number', step: 'any', required: true },
          { name: 'centerLongitude', label: 'Centre longitude', type: 'number', step: 'any', required: true },
          { name: 'description', label: 'Description', type: 'textarea', wide: true },
        ]}
        onSubmit={async (v) => { await habitatApi.createArea(v); toast('Area added'); refreshLookups(); }}
      />
      <FormModal
        open={modal === 'zone'} onClose={() => setModal(null)} title="Add zone" submitLabel="Add zone"
        initial={{ zoneType: 'CORE', areaId: areaId || undefined }}
        fields={[
          { name: 'areaId', label: 'Protected area', type: 'select', required: true, options: areaOptions(lookups.areas) },
          { name: 'name', label: 'Name', required: true },
          { name: 'code', label: 'Code', required: true, placeholder: 'KNH-MUK' },
          { name: 'zoneType', label: 'Type', type: 'select', required: true, enumValues: ZONE_TYPES },
          { name: 'areaSqKm', label: 'Area (km²)', type: 'number', step: 'any' },
          { name: 'centerLatitude', label: 'Centre latitude', type: 'number', step: 'any', required: true },
          { name: 'centerLongitude', label: 'Centre longitude', type: 'number', step: 'any', required: true },
          { name: 'description', label: 'Description', type: 'textarea', wide: true },
        ]}
        onSubmit={async (v) => { await habitatApi.createZone(v); toast('Zone added'); refreshLookups(); zones.reload(); }}
      />
    </>
  );
}

function ZoneDrawer({ zone, onClose, onChanged }: { zone: Zone; onClose: () => void; onChanged: () => void }) {
  const { hasRole } = useAuth();
  const toast = useToast();
  const { zones } = useLookups();
  const habitats = useLoad(() => habitatApi.habitats({ zoneId: zone.id }), [zone.id]);
  const water = useLoad(() => habitatApi.water({ zoneId: zone.id }), [zone.id]);
  const [modal, setModal] = useState<'habitat' | 'water' | null>(null);
  const [inspecting, setInspecting] = useState<WaterResource | null>(null);
  const canEdit = hasRole('ADMIN', 'FOREST_OFFICER');

  return (
    <Drawer open onClose={onClose} accent={ZONE_COLORS[zone.zoneType]}>
      <div className="drawer-head">
        <h2>{zone.name}</h2>
        <div className="sub">{zone.areaName} · {label(zone.zoneType)} zone · {zone.code}</div>
        {canEdit && (
          <div className="actions">
            <button className="btn btn-secondary" onClick={() => setModal('habitat')}>Add habitat</button>
            <button className="btn btn-secondary" onClick={() => setModal('water')}>Add water source</button>
          </div>
        )}
      </div>
      <div className="drawer-body">
        <dl className="facts panel">
          <Fact term="Area">{zone.areaSqKm ? `${zone.areaSqKm} km²` : '—'}</Fact>
          <Fact term="Centre">{fmtCoord(zone.centerLatitude, zone.centerLongitude)}</Fact>
          {zone.description && <Fact term="Description" wide>{zone.description}</Fact>}
        </dl>
        <h3>Habitats</h3>
        {habitats.data?.content.length === 0 && <Empty title="No habitats recorded" />}
        <ul className="cards">
          {habitats.data?.content.map((h) => (
            <li key={h.id} className="card">
              <div className="card-title">{h.name} <Chip value={h.condition} /></div>
              <div className="muted">{label(h.habitatType)}{h.areaSqKm ? ` · ${h.areaSqKm} km²` : ''}</div>
              {h.dominantVegetation && <div>Vegetation: {h.dominantVegetation}</div>}
              <div className="meta"><span>Last survey {fmtDate(h.lastSurveyDate)}</span></div>
              {h.notes && <div className="muted">{h.notes}</div>}
            </li>
          ))}
        </ul>
        <h3>Water sources</h3>
        {water.data?.content.length === 0 && <Empty title="No water sources recorded" />}
        <ul className="cards">
          {water.data?.content.map((w) => (
            <li key={w.id} className="card">
              <div className="card-title">{w.name} <Chip value={w.status} /></div>
              <div className="muted">{label(w.sourceType)}{w.perennial ? ', perennial' : ', seasonal'}</div>
              <div className="meta"><span>Inspected {fmtDateTime(w.lastInspectedAt)}</span></div>
              {w.notes && <div className="muted">{w.notes}</div>}
              {hasRole('ADMIN', 'FOREST_OFFICER', 'RANGER') &&
                <button className="link-btn" onClick={() => setInspecting(w)}>Record inspection</button>}
            </li>
          ))}
        </ul>
      </div>

      <FormModal
        open={modal === 'habitat'} onClose={() => setModal(null)} title={`Add habitat in ${zone.name}`} submitLabel="Add habitat"
        initial={{ zoneId: zone.id, habitatType: 'GRASSLAND', condition: 'GOOD' }}
        fields={[
          { name: 'zoneId', label: 'Zone', type: 'select', required: true, options: zoneOptions(zones) },
          { name: 'name', label: 'Name', required: true },
          { name: 'habitatType', label: 'Type', type: 'select', required: true, enumValues: HABITAT_TYPES },
          { name: 'condition', label: 'Condition', type: 'select', required: true, enumValues: CONDITIONS },
          { name: 'areaSqKm', label: 'Area (km²)', type: 'number', step: 'any' },
          { name: 'lastSurveyDate', label: 'Last survey', type: 'date' },
          { name: 'latitude', label: 'Latitude', type: 'number', step: 'any' },
          { name: 'longitude', label: 'Longitude', type: 'number', step: 'any' },
          { name: 'dominantVegetation', label: 'Dominant vegetation', wide: true },
          { name: 'notes', label: 'Notes', type: 'textarea', wide: true },
        ]}
        onSubmit={async (v) => { await habitatApi.createHabitat(v); toast('Habitat added'); habitats.reload(); }}
      />
      <FormModal
        open={modal === 'water'} onClose={() => setModal(null)} title={`Add water source in ${zone.name}`} submitLabel="Add water source"
        initial={{ zoneId: zone.id, sourceType: 'WATERHOLE', status: 'AVAILABLE', perennial: false }}
        fields={[
          { name: 'zoneId', label: 'Zone', type: 'select', required: true, options: zoneOptions(zones) },
          { name: 'name', label: 'Name', required: true },
          { name: 'sourceType', label: 'Type', type: 'select', required: true, enumValues: WATER_TYPES },
          { name: 'status', label: 'Status', type: 'select', required: true, enumValues: WATER_STATUS },
          { name: 'perennial', label: 'Holds water all year', type: 'checkbox' },
          { name: 'capacityCubicM', label: 'Capacity (m³)', type: 'number', step: 'any' },
          { name: 'latitude', label: 'Latitude', type: 'number', step: 'any', required: true },
          { name: 'longitude', label: 'Longitude', type: 'number', step: 'any', required: true },
          { name: 'notes', label: 'Notes', type: 'textarea', wide: true },
        ]}
        onSubmit={async (v) => { await habitatApi.createWater(v); toast('Water source added'); water.reload(); onChanged(); }}
      />
      <FormModal
        open={inspecting !== null} onClose={() => setInspecting(null)}
        title={inspecting ? `Inspection: ${inspecting.name}` : ''} submitLabel="Save inspection"
        initial={{ status: inspecting?.status, inspectedAt: nowLocalInput() }}
        fields={[
          { name: 'status', label: 'Status found', type: 'select', required: true, enumValues: WATER_STATUS },
          { name: 'inspectedAt', label: 'Inspected at', type: 'datetime' },
          { name: 'notes', label: 'Notes', type: 'textarea', wide: true },
        ]}
        onSubmit={async (v) => { await habitatApi.setWaterStatus(inspecting!.id, v); toast('Inspection saved'); water.reload(); onChanged(); }}
      />
    </Drawer>
  );
}
