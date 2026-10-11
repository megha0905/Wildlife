// Zones & map: protected areas, their zones, habitats and water sources.
import { habitatApi } from '../api/endpoints.js';
import { areaOptions, getLookups, refreshLookups, zoneOptions } from '../api/lookups.js';
import { hasRole } from '../auth/auth.js';
import { h, mount } from '../components/dom.js';
import { openFormModal } from '../components/form.js';
import { mapView } from '../components/map.js';
import { openDrawer } from '../components/modal.js';
import { dataTable } from '../components/table.js';
import { toast } from '../components/toasts.js';
import {
  chip, empty, errorBanner, fact, fmtCoord, fmtDate, fmtDateTime, label, loading, nowLocalInput, pageHeader, region, selectInput,
} from '../components/ui.js';

const ZONE_COLORS = { CORE: '#2c5246', BUFFER: '#9fbdb1', ECO_SENSITIVE: '#a8740f', TOURISM: '#6a7fa8' };
const WATER_COLORS = { AVAILABLE: '#3a7fc1', LOW: '#d9a441', DRY: '#a93a26', CONTAMINATED: '#7a3b8f' };
const AREA_TYPES = ['NATIONAL_PARK', 'WILDLIFE_SANCTUARY', 'TIGER_RESERVE', 'CONSERVATION_RESERVE', 'COMMUNITY_RESERVE', 'BIOSPHERE_RESERVE'];
const ZONE_TYPES = ['CORE', 'BUFFER', 'ECO_SENSITIVE', 'TOURISM'];
const HABITAT_TYPES = ['GRASSLAND', 'TROPICAL_DRY_FOREST', 'TROPICAL_MOIST_FOREST', 'WETLAND', 'RIVERINE', 'DESERT', 'MONTANE', 'MANGROVE', 'SCRUBLAND'];
const CONDITIONS = ['GOOD', 'FAIR', 'DEGRADED', 'CRITICAL'];
const WATER_TYPES = ['RIVER', 'STREAM', 'LAKE', 'POND', 'WATERHOLE', 'ARTIFICIAL_WATERHOLE', 'SPRING'];
const WATER_STATUS = ['AVAILABLE', 'LOW', 'DRY', 'CONTAMINATED'];

export function ZonesPage() {
  return region(getLookups, zonesPage).el;
}

function zonesPage(lookups) {
  let areaId = '';
  const canEdit = hasRole('ADMIN', 'FOREST_OFFICER');

  const content = region(
    () => Promise.all([habitatApi.zones({ areaId }), habitatApi.water({ areaId })]),
    ([zones, water]) => {
      const markers = [
        ...zones.content.map((z) => ({
          lat: z.centerLatitude, lng: z.centerLongitude, radius: 11,
          label: `${z.name} (${label(z.zoneType)})`, color: ZONE_COLORS[z.zoneType], onClick: () => openZoneDrawer(z, lookups),
        })),
        ...water.content.map((w) => ({
          lat: w.latitude, lng: w.longitude, radius: 5, label: `${w.name}: ${label(w.status)}`, color: WATER_COLORS[w.status],
        })),
      ];
      return [
        mapView({ markers, height: 380 }),
        h('h2', { class: 'section-title' }, 'Zones'),
        dataTable({
          rows: zones.content,
          onRowClick: (z) => openZoneDrawer(z, lookups, content.reload),
          stripe: (z) => ZONE_COLORS[z.zoneType],
          empty: empty('No zones yet'),
          columns: [
            { header: 'Zone', render: (z) => h('strong', null, z.name) },
            { header: 'Code', render: (z) => z.code },
            { header: 'Protected area', render: (z) => z.areaName },
            { header: 'Type', render: (z) => label(z.zoneType) },
            { header: 'Area (km²)', render: (z) => z.areaSqKm ?? '—', className: 'num' },
            { header: 'Centre', render: (z) => fmtCoord(z.centerLatitude, z.centerLongitude) },
          ],
        }),
      ];
    });

  const addArea = () => openFormModal({
    title: 'Add protected area', submitLabel: 'Add area', initial: { areaType: 'TIGER_RESERVE' },
    fields: [
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
    ],
    onSubmit: async (v) => { await habitatApi.createArea(v); toast('Area added'); refreshLookups(); },
  });

  const addZone = () => openFormModal({
    title: 'Add zone', submitLabel: 'Add zone', initial: { zoneType: 'CORE', areaId: areaId || undefined },
    fields: [
      { name: 'areaId', label: 'Protected area', type: 'select', required: true, options: areaOptions(lookups.areas) },
      { name: 'name', label: 'Name', required: true },
      { name: 'code', label: 'Code', required: true, placeholder: 'KNH-MUK' },
      { name: 'zoneType', label: 'Type', type: 'select', required: true, enumValues: ZONE_TYPES },
      { name: 'areaSqKm', label: 'Area (km²)', type: 'number', step: 'any' },
      { name: 'centerLatitude', label: 'Centre latitude', type: 'number', step: 'any', required: true },
      { name: 'centerLongitude', label: 'Centre longitude', type: 'number', step: 'any', required: true },
      { name: 'description', label: 'Description', type: 'textarea', wide: true },
    ],
    onSubmit: async (v) => { await habitatApi.createZone(v); toast('Zone added'); refreshLookups(); content.reload(); },
  });

  return h('div', null,
    pageHeader('Zones & map',
      'Protected areas, their management zones, habitats and water sources. Large dots are zones; small dots are water sources coloured by status.',
      canEdit && [
        h('button', { class: 'btn btn-secondary', onClick: addArea }, 'Add area'),
        h('button', { class: 'btn btn-primary', onClick: addZone }, 'Add zone'),
      ]),
    h('div', { class: 'toolbar' },
      selectInput({ ariaLabel: 'Protected area', placeholder: 'All protected areas', options: areaOptions(lookups.areas),
        onChange: (v) => { areaId = v; content.reload(); } }),
      h('div', { class: 'legend' }, Object.entries(WATER_COLORS).map(([k, c]) => h('span', null, h('i', { style: { background: c } }), label(k))))),
    content.el);
}

function openZoneDrawer(zone, lookups, onChanged = () => {}) {
  const drawer = openDrawer({ accent: ZONE_COLORS[zone.zoneType] });
  const canEdit = hasRole('ADMIN', 'FOREST_OFFICER');
  mount(drawer.body, loading());

  const load = () => Promise.all([habitatApi.habitats({ zoneId: zone.id }), habitatApi.water({ zoneId: zone.id })])
    .then(([habitats, water]) => mount(drawer.body, render(habitats.content, water.content)))
    .catch((err) => mount(drawer.body, h('div', { class: 'drawer-body' }, errorBanner(err))));

  const render = (habitats, water) => [
    h('div', { class: 'drawer-head' },
      h('h2', null, zone.name),
      h('div', { class: 'sub' }, `${zone.areaName} · ${label(zone.zoneType)} zone · ${zone.code}`),
      canEdit && h('div', { class: 'actions' },
        h('button', { class: 'btn btn-secondary', onClick: addHabitat }, 'Add habitat'),
        h('button', { class: 'btn btn-secondary', onClick: addWater }, 'Add water source'))),
    h('div', { class: 'drawer-body' },
      h('dl', { class: 'facts panel' },
        fact('Area', zone.areaSqKm ? `${zone.areaSqKm} km²` : '—'),
        fact('Centre', fmtCoord(zone.centerLatitude, zone.centerLongitude)),
        zone.description && fact('Description', zone.description, true)),
      h('h3', null, 'Habitats'),
      habitats.length === 0 && empty('No habitats recorded'),
      h('ul', { class: 'cards' }, habitats.map((x) => h('li', { class: 'card' },
        h('div', { class: 'card-title' }, x.name, ' ', chip(x.condition)),
        h('div', { class: 'muted' }, label(x.habitatType) + (x.areaSqKm ? ` · ${x.areaSqKm} km²` : '')),
        x.dominantVegetation && h('div', null, `Vegetation: ${x.dominantVegetation}`),
        h('div', { class: 'meta' }, h('span', null, `Last survey ${fmtDate(x.lastSurveyDate)}`)),
        x.notes && h('div', { class: 'muted' }, x.notes)))),
      h('h3', null, 'Water sources'),
      water.length === 0 && empty('No water sources recorded'),
      h('ul', { class: 'cards' }, water.map((w) => h('li', { class: 'card' },
        h('div', { class: 'card-title' }, w.name, ' ', chip(w.status)),
        h('div', { class: 'muted' }, label(w.sourceType) + (w.perennial ? ', perennial' : ', seasonal')),
        h('div', { class: 'meta' }, h('span', null, `Inspected ${fmtDateTime(w.lastInspectedAt)}`)),
        w.notes && h('div', { class: 'muted' }, w.notes),
        hasRole('ADMIN', 'FOREST_OFFICER', 'RANGER') && h('button', { class: 'link-btn', onClick: () => inspect(w) }, 'Record inspection'))))),
  ];

  function addHabitat() {
    openFormModal({
      title: `Add habitat in ${zone.name}`, submitLabel: 'Add habitat',
      initial: { zoneId: zone.id, habitatType: 'GRASSLAND', condition: 'GOOD' },
      fields: [
        { name: 'zoneId', label: 'Zone', type: 'select', required: true, options: zoneOptions(lookups.zones) },
        { name: 'name', label: 'Name', required: true },
        { name: 'habitatType', label: 'Type', type: 'select', required: true, enumValues: HABITAT_TYPES },
        { name: 'condition', label: 'Condition', type: 'select', required: true, enumValues: CONDITIONS },
        { name: 'areaSqKm', label: 'Area (km²)', type: 'number', step: 'any' },
        { name: 'lastSurveyDate', label: 'Last survey', type: 'date' },
        { name: 'latitude', label: 'Latitude', type: 'number', step: 'any' },
        { name: 'longitude', label: 'Longitude', type: 'number', step: 'any' },
        { name: 'dominantVegetation', label: 'Dominant vegetation', wide: true },
        { name: 'notes', label: 'Notes', type: 'textarea', wide: true },
      ],
      onSubmit: async (v) => { await habitatApi.createHabitat(v); toast('Habitat added'); load(); },
    });
  }

  function addWater() {
    openFormModal({
      title: `Add water source in ${zone.name}`, submitLabel: 'Add water source',
      initial: { zoneId: zone.id, sourceType: 'WATERHOLE', status: 'AVAILABLE', perennial: false },
      fields: [
        { name: 'zoneId', label: 'Zone', type: 'select', required: true, options: zoneOptions(lookups.zones) },
        { name: 'name', label: 'Name', required: true },
        { name: 'sourceType', label: 'Type', type: 'select', required: true, enumValues: WATER_TYPES },
        { name: 'status', label: 'Status', type: 'select', required: true, enumValues: WATER_STATUS },
        { name: 'perennial', label: 'Holds water all year', type: 'checkbox' },
        { name: 'capacityCubicM', label: 'Capacity (m³)', type: 'number', step: 'any' },
        { name: 'latitude', label: 'Latitude', type: 'number', step: 'any', required: true },
        { name: 'longitude', label: 'Longitude', type: 'number', step: 'any', required: true },
        { name: 'notes', label: 'Notes', type: 'textarea', wide: true },
      ],
      onSubmit: async (v) => { await habitatApi.createWater(v); toast('Water source added'); load(); onChanged(); },
    });
  }

  function inspect(w) {
    openFormModal({
      title: `Inspection: ${w.name}`, submitLabel: 'Save inspection',
      initial: { status: w.status, inspectedAt: nowLocalInput() },
      fields: [
        { name: 'status', label: 'Status found', type: 'select', required: true, enumValues: WATER_STATUS },
        { name: 'inspectedAt', label: 'Inspected at', type: 'datetime' },
        { name: 'notes', label: 'Notes', type: 'textarea', wide: true },
      ],
      onSubmit: async (v) => { await habitatApi.setWaterStatus(w.id, v); toast('Inspection saved'); load(); onChanged(); },
    });
  }

  load();
}
