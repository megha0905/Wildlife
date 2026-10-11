// Animal registry: tagged animals (with health records, sightings, status) and the species catalogue.
import { wildlifeApi } from '../api/endpoints.js';
import { getLookups, refreshLookups, speciesOptions, zoneOptions } from '../api/lookups.js';
import { hasRole } from '../auth/auth.js';
import { h, mount } from '../components/dom.js';
import { openFormModal } from '../components/form.js';
import { mapView } from '../components/map.js';
import { openDrawer } from '../components/modal.js';
import { dataTable, pager } from '../components/table.js';
import { toast } from '../components/toasts.js';
import {
  IUCN, chip, empty, enumOptions, errorBanner, fact, fmtAgo, fmtCoord, fmtDate, fmtDateTime, iucnBadge, label,
  loading, pageHeader, region, searchInput, selectInput, tabbed, todayIso, warnings,
} from '../components/ui.js';

const HEALTH = ['HEALTHY', 'INJURED', 'SICK', 'UNDER_TREATMENT', 'RECOVERING', 'DECEASED', 'UNKNOWN'];
const STATUS = ['IN_WILD', 'IN_RESCUE', 'TRANSFERRED', 'MISSING', 'DECEASED'];
const SEX = ['MALE', 'FEMALE', 'UNKNOWN'];
const RECORD_TYPES = ['CHECKUP', 'OBSERVATION', 'INJURY_ASSESSMENT', 'TREATMENT', 'VACCINATION', 'POST_MORTEM'];
const CATEGORIES = ['MAMMAL', 'BIRD', 'REPTILE', 'AMPHIBIAN', 'FISH', 'INVERTEBRATE'];
const IUCN_ORDER = ['CRITICALLY_ENDANGERED', 'ENDANGERED', 'VULNERABLE', 'NEAR_THREATENED', 'LEAST_CONCERN',
  'EXTINCT_IN_THE_WILD', 'DATA_DEFICIENT', 'NOT_EVALUATED'];

export function AnimalsPage() {
  return h('div', null,
    pageHeader('Animal registry',
      'Individually identified animals with their health, last known location and threat level, and the species catalogue.'),
    tabbed([
      { id: 'animals', label: 'Animals', render: () => region(getLookups, animalsTab).el },
      { id: 'species', label: 'Species catalogue', render: speciesTab },
    ]));
}

// ==================================================================== animals

function animalsTab(lookups) {
  const { species, zones } = lookups;
  const filters = { q: '', speciesId: '', healthStatus: '', status: '', zoneId: '' };
  let page = 0;
  const canEdit = hasRole('ADMIN', 'FOREST_OFFICER', 'RANGER', 'VET');

  const list = region(
    () => wildlifeApi.animals({ ...filters, page, size: 20, sort: 'tagId,asc' }),
    (data) => [
      dataTable({
        rows: data.content,
        onRowClick: (a) => openAnimalDrawer(a.id, lookups, list.reload),
        stripe: (a) => IUCN[a.conservationStatus]?.color,
        empty: empty('No animals match these filters', 'Clear a filter or register a new animal.'),
        columns: [
          { header: 'Tag', render: (a) => h('strong', { class: 'mono-ish' }, a.tagId) },
          { header: 'Name', render: (a) => a.name ?? h('span', { class: 'muted' }, '—') },
          { header: 'Species', render: (a) => a.speciesCommonName },
          { header: 'Sex', render: (a) => label(a.sex) },
          { header: 'Age', render: (a) => (a.ageYears == null ? '—' : `${a.birthYearEstimated ? '~' : ''}${a.ageYears} y`) },
          { header: 'Health', render: (a) => chip(a.healthStatus) },
          { header: 'Status', render: (a) => chip(a.status) },
          { header: 'Zone', render: (a) => a.zoneName ?? '—' },
          { header: 'Last seen', render: (a) => h('span', { title: fmtDateTime(a.lastSeenAt) }, fmtAgo(a.lastSeenAt)) },
        ],
      }),
      pager(data, (p) => { page = p; list.reload(); }),
    ]);

  const setFilter = (k) => (v) => { filters[k] = v; page = 0; list.reload(); };

  return h('div', null,
    h('div', { class: 'toolbar' },
      searchInput({ placeholder: 'Search tag, name or species', ariaLabel: 'Search animals', onSearch: setFilter('q') }),
      selectInput({ ariaLabel: 'Species', placeholder: 'All species', options: speciesOptions(species), onChange: setFilter('speciesId') }),
      selectInput({ ariaLabel: 'Health', placeholder: 'Any health', options: enumOptions(HEALTH), onChange: setFilter('healthStatus') }),
      selectInput({ ariaLabel: 'Status', placeholder: 'Any status', options: enumOptions(STATUS), onChange: setFilter('status') }),
      selectInput({ ariaLabel: 'Zone', placeholder: 'All zones', options: zones.map((z) => ({ value: z.id, label: z.name })), onChange: setFilter('zoneId') }),
      h('div', { class: 'spacer' }),
      canEdit && h('button', { class: 'btn btn-primary', onClick: () => openAnimalForm(lookups, null, (a) => {
        toast(`${a.tagId} registered`);
        a.warnings.forEach((w) => toast(w, 'warn'));
        list.reload();
      }) }, 'Register animal')),
    list.el);
}

function animalFields({ species, zones }) {
  return [
    { name: 'tagId', label: 'Tag ID', required: true, placeholder: 'TGR-KNH-021', hint: 'Unique; letters, digits and hyphens. Saved in capitals.' },
    { name: 'name', label: 'Name' },
    { name: 'speciesId', label: 'Species', type: 'select', required: true, options: speciesOptions(species) },
    { name: 'sex', label: 'Sex', type: 'select', required: true, enumValues: SEX },
    { name: 'birthYear', label: 'Birth year', type: 'number' },
    { name: 'birthYearEstimated', label: 'Birth year is an estimate', type: 'checkbox' },
    { name: 'healthStatus', label: 'Health', type: 'select', required: true, enumValues: HEALTH },
    { name: 'status', label: 'Status', type: 'select', required: true, enumValues: STATUS },
    { name: 'zoneId', label: 'Zone', type: 'select', options: zoneOptions(zones), placeholder: 'Not assigned' },
    { name: 'lastSeenAt', label: 'Last seen', type: 'datetime' },
    { name: 'lastSeenLatitude', label: 'Last seen latitude', type: 'number', step: 'any' },
    { name: 'lastSeenLongitude', label: 'Last seen longitude', type: 'number', step: 'any' },
    { name: 'identificationMarks', label: 'Identification marks', type: 'textarea', wide: true },
    { name: 'notes', label: 'Notes', type: 'textarea', wide: true },
  ];
}

/** Register (animal = null) or edit an animal. */
function openAnimalForm(lookups, animal, onSaved) {
  openFormModal({
    title: animal ? `Edit ${animal.tagId}` : 'Register animal',
    submitLabel: animal ? 'Save changes' : 'Register animal',
    fields: animalFields(lookups),
    initial: animal ?? { sex: 'UNKNOWN', healthStatus: 'HEALTHY', status: 'IN_WILD', birthYearEstimated: true },
    onSubmit: async (v) => {
      const saved = animal ? await wildlifeApi.updateAnimal(animal.id, v) : await wildlifeApi.createAnimal(v);
      onSaved(saved);
    },
  });
}

function openAnimalDrawer(id, lookups, onChanged) {
  const drawer = openDrawer();
  mount(drawer.body, loading());

  const refresh = () => { load(); onChanged(); };
  const load = () => Promise.all([wildlifeApi.animal(id), wildlifeApi.healthRecords(id)])
    .then(([a, records]) => { drawer.setAccent(IUCN[a.conservationStatus]?.color); mount(drawer.body, render(a, records)); })
    .catch((err) => mount(drawer.body, h('div', { class: 'drawer-body' }, errorBanner(err))));

  const render = (a, records) => [
    h('div', { class: 'drawer-head' },
      h('h2', null, a.tagId, a.name && h('span', { class: 'alias' }, a.name)),
      h('div', { class: 'sub' }, a.speciesCommonName, ' ', h('em', { class: 'muted' }, a.speciesScientificName)),
      h('div', { class: 'actions' },
        hasRole('ADMIN', 'FOREST_OFFICER', 'VET') && h('button', { class: 'btn btn-primary', onClick: () => openRecordForm(a, refresh) }, 'Add health record'),
        hasRole('ADMIN', 'FOREST_OFFICER', 'RANGER', 'VET') && [
          h('button', { class: 'btn btn-secondary', onClick: () => openStatusForm(a, refresh) }, 'Change status'),
          h('button', { class: 'btn btn-secondary', onClick: () => openAnimalForm(lookups, a, (s) => {
            toast(`${s.tagId} updated`);
            s.warnings.forEach((w) => toast(w, 'warn'));
            refresh();
          }) }, 'Edit'),
        ])),
    h('div', { class: 'drawer-body' },
      warnings(a.warnings),
      h('dl', { class: 'facts panel' },
        fact('Health', chip(a.healthStatus)),
        fact('Status', chip(a.status)),
        fact('Threat level', iucnBadge(a.conservationStatus)),
        fact('Sex', label(a.sex)),
        fact('Age', a.ageYears == null ? 'Unknown' : `${a.birthYearEstimated ? 'About ' : ''}${a.ageYears} years (born ${a.birthYear})`),
        fact('Zone', a.zoneName ?? (a.zoneId ? `Zone ${a.zoneId}` : '—')),
        fact('Last seen', a.lastSeenAt ? `${fmtDateTime(a.lastSeenAt)} (${fmtAgo(a.lastSeenAt)})` : 'Never'),
        fact('Last known location', fmtCoord(a.lastSeenLatitude, a.lastSeenLongitude)),
        a.identificationMarks && fact('Identification marks', a.identificationMarks, true),
        a.notes && fact('Notes', h('span', { class: 'pre' }, a.notes), true)),
      a.lastSeenLatitude != null && a.lastSeenLongitude != null && mapView({
        height: 200,
        markers: [{ lat: a.lastSeenLatitude, lng: a.lastSeenLongitude, label: `${a.tagId} last seen`, color: IUCN[a.conservationStatus]?.color }],
      }),
      h('h3', null, 'Health records'),
      records.length === 0 && empty('No health records yet'),
      h('ol', { class: 'timeline' }, records.map((r) => h('li', null,
        h('div', { class: 'when' }, fmtDate(r.recordDate)),
        h('div', { class: 'what' }, label(r.recordType), ' ', chip(r.healthStatus)),
        r.diagnosis && h('div', null, r.diagnosis),
        r.treatment && h('div', { class: 'muted' }, `Treatment: ${r.treatment}`),
        h('div', { class: 'meta' },
          r.vetName && h('span', null, `Vet: ${r.vetName}`),
          r.weightKg != null && h('span', null, `${r.weightKg} kg`),
          r.recordedByName && h('span', null, `Recorded by ${r.recordedByName}`)))))),
  ];

  load();
}

function openRecordForm(a, refresh) {
  openFormModal({
    title: `Health record for ${a.tagId}`,
    submitLabel: 'Add record',
    intro: "If this is the newest record, the animal's current health changes to the status you choose.",
    initial: { recordDate: todayIso(), recordType: 'CHECKUP', healthStatus: a.healthStatus },
    fields: [
      { name: 'recordDate', label: 'Date', type: 'date', required: true },
      { name: 'recordType', label: 'Type', type: 'select', required: true, enumValues: RECORD_TYPES },
      { name: 'healthStatus', label: 'Health observed', type: 'select', required: true, enumValues: HEALTH },
      { name: 'weightKg', label: 'Weight (kg)', type: 'number', step: 'any' },
      { name: 'vetName', label: 'Vet' },
      { name: 'diagnosis', label: 'Diagnosis', wide: true },
      { name: 'treatment', label: 'Treatment', type: 'textarea', wide: true },
      { name: 'notes', label: 'Notes', type: 'textarea', wide: true },
    ],
    onSubmit: async (v) => { await wildlifeApi.addHealthRecord(a.id, v); toast('Health record added'); refresh(); },
  });
}

function openStatusForm(a, refresh) {
  openFormModal({
    title: `Change status of ${a.tagId}`,
    submitLabel: 'Change status',
    initial: { status: a.status },
    fields: [
      { name: 'status', label: 'New status', type: 'select', required: true, enumValues: STATUS },
      { name: 'healthStatus', label: 'Health', type: 'select', enumValues: HEALTH, placeholder: 'Leave unchanged' },
      { name: 'note', label: 'Reason', type: 'textarea', wide: true },
    ],
    onSubmit: async (v) => {
      const s = await wildlifeApi.setAnimalStatus(a.id, v);
      toast(`${s.tagId} is now ${label(s.status).toLowerCase()}`);
      refresh();
    },
  });
}

// ==================================================================== species

function speciesTab() {
  let q = '';
  const canEdit = hasRole('ADMIN', 'FOREST_OFFICER');

  const list = region(() => wildlifeApi.species({ q }), (data) => dataTable({
    rows: data.content,
    onRowClick: canEdit ? (s) => openSpeciesForm(s) : undefined,
    stripe: (s) => IUCN[s.conservationStatus]?.color,
    empty: empty('No species found'),
    columns: [
      { header: 'Common name', render: (s) => h('strong', null, s.commonName) },
      { header: 'Scientific name', render: (s) => h('em', null, s.scientificName) },
      { header: 'Category', render: (s) => label(s.category) },
      { header: 'IUCN status', render: (s) => iucnBadge(s.conservationStatus) },
      { header: 'Legal protection', render: (s) => s.protectionSchedule ?? '—' },
      { header: 'Animals', render: (s) => s.animalCount, className: 'num' },
    ],
  }));

  function openSpeciesForm(species) {
    openFormModal({
      title: species ? `Edit ${species.commonName}` : 'Add species',
      submitLabel: species ? 'Save changes' : 'Add species',
      initial: species ?? { category: 'MAMMAL', conservationStatus: 'LEAST_CONCERN' },
      fields: [
        { name: 'commonName', label: 'Common name', required: true },
        { name: 'scientificName', label: 'Scientific name', required: true },
        { name: 'category', label: 'Category', type: 'select', required: true, enumValues: CATEGORIES },
        { name: 'conservationStatus', label: 'IUCN Red List status', type: 'select', required: true,
          options: IUCN_ORDER.map((s) => ({ value: s, label: `${label(s)} (${IUCN[s].code})` })) },
        { name: 'protectionSchedule', label: 'Legal protection', placeholder: 'Schedule I', wide: true },
        { name: 'description', label: 'Description', type: 'textarea', wide: true },
      ],
      onSubmit: async (v) => {
        if (species) await wildlifeApi.updateSpecies(species.id, v);
        else await wildlifeApi.createSpecies(v);
        toast('Species saved');
        refreshLookups();
        list.reload();
      },
    });
  }

  return h('div', null,
    h('div', { class: 'toolbar' },
      searchInput({ placeholder: 'Search common or scientific name', ariaLabel: 'Search species', onSearch: (v) => { q = v; list.reload(); } }),
      h('div', { class: 'spacer' }),
      canEdit && h('button', { class: 'btn btn-primary', onClick: () => openSpeciesForm(null) }, 'Add species')),
    list.el);
}
