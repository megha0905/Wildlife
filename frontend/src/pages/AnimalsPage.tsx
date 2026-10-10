import { useState } from 'react';
import { wildlifeApi } from '../api/endpoints';
import { refreshLookups, speciesOptions, useLookups, zoneOptions } from '../api/lookups';
import type { Animal, Species } from '../api/types';
import { useAuth } from '../auth/AuthContext';
import { DataTable, Pager } from '../components/DataTable';
import { FormModal, type Field } from '../components/Form';
import { Drawer } from '../components/Modal';
import { MapView } from '../components/MapView';
import { useToast } from '../components/Toasts';
import {
  Chip, Empty, ErrorBanner, Fact, IUCN, IucnBadge, Loading, PageHeader, Tabs, Warnings,
  fmtAgo, fmtCoord, fmtDate, fmtDateTime, label, todayIso, useLoad,
} from '../components/ui';

const HEALTH = ['HEALTHY', 'INJURED', 'SICK', 'UNDER_TREATMENT', 'RECOVERING', 'DECEASED', 'UNKNOWN'] as const;
const STATUS = ['IN_WILD', 'IN_RESCUE', 'TRANSFERRED', 'MISSING', 'DECEASED'] as const;
const SEX = ['MALE', 'FEMALE', 'UNKNOWN'] as const;
const RECORD_TYPES = ['CHECKUP', 'OBSERVATION', 'INJURY_ASSESSMENT', 'TREATMENT', 'VACCINATION', 'POST_MORTEM'] as const;
const CATEGORIES = ['MAMMAL', 'BIRD', 'REPTILE', 'AMPHIBIAN', 'FISH', 'INVERTEBRATE'] as const;
const IUCN_ORDER = ['CRITICALLY_ENDANGERED', 'ENDANGERED', 'VULNERABLE', 'NEAR_THREATENED', 'LEAST_CONCERN',
  'EXTINCT_IN_THE_WILD', 'DATA_DEFICIENT', 'NOT_EVALUATED'] as const;

export function AnimalsPage() {
  const [tab, setTab] = useState<'animals' | 'species'>('animals');
  return (
    <>
      <PageHeader title="Animal registry"
        intro="Individually identified animals with their health, last known location and threat level, and the species catalogue." />
      <Tabs value={tab} onChange={setTab} tabs={[{ id: 'animals', label: 'Animals' }, { id: 'species', label: 'Species catalogue' }]} />
      {tab === 'animals' ? <AnimalsTab /> : <SpeciesTab />}
    </>
  );
}

// ==================================================================== animals

function AnimalsTab() {
  const { hasRole } = useAuth();
  const toast = useToast();
  const { species, zones } = useLookups();
  const [filters, setFilters] = useState({ q: '', speciesId: '', healthStatus: '', status: '', zoneId: '' });
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState<number | null>(null);
  const [creating, setCreating] = useState(false);
  const list = useLoad(() => wildlifeApi.animals({ ...filters, page, size: 20, sort: 'tagId,asc' }), [JSON.stringify(filters), page]);

  const canEdit = hasRole('ADMIN', 'FOREST_OFFICER', 'RANGER', 'VET');
  const setFilter = (k: keyof typeof filters, v: string) => { setFilters((f) => ({ ...f, [k]: v })); setPage(0); };

  return (
    <>
      <div className="toolbar">
        <input className="input search" placeholder="Search tag, name or species" value={filters.q}
          onChange={(e) => setFilter('q', e.target.value)} aria-label="Search animals" />
        <select className="input" value={filters.speciesId} onChange={(e) => setFilter('speciesId', e.target.value)} aria-label="Species">
          <option value="">All species</option>
          {species.map((s) => <option key={s.id} value={s.id}>{s.commonName}</option>)}
        </select>
        <select className="input" value={filters.healthStatus} onChange={(e) => setFilter('healthStatus', e.target.value)} aria-label="Health">
          <option value="">Any health</option>
          {HEALTH.map((h) => <option key={h} value={h}>{label(h)}</option>)}
        </select>
        <select className="input" value={filters.status} onChange={(e) => setFilter('status', e.target.value)} aria-label="Status">
          <option value="">Any status</option>
          {STATUS.map((s) => <option key={s} value={s}>{label(s)}</option>)}
        </select>
        <select className="input" value={filters.zoneId} onChange={(e) => setFilter('zoneId', e.target.value)} aria-label="Zone">
          <option value="">All zones</option>
          {zones.map((z) => <option key={z.id} value={z.id}>{z.name}</option>)}
        </select>
        <div className="spacer" />
        {canEdit && <button className="btn btn-primary" onClick={() => setCreating(true)}>Register animal</button>}
      </div>

      <ErrorBanner error={list.error} />
      {list.loading && !list.data ? <Loading /> : list.data && (
        <>
          <DataTable<Animal>
            rows={list.data.content}
            rowKey={(a) => a.id}
            onRowClick={(a) => setSelected(a.id)}
            stripe={(a) => IUCN[a.conservationStatus]?.color}
            empty={<Empty title="No animals match these filters">Clear a filter or register a new animal.</Empty>}
            columns={[
              { header: 'Tag', render: (a) => <strong className="mono-ish">{a.tagId}</strong> },
              { header: 'Name', render: (a) => a.name ?? <span className="muted">—</span> },
              { header: 'Species', render: (a) => a.speciesCommonName },
              { header: 'Sex', render: (a) => label(a.sex) },
              { header: 'Age', render: (a) => a.ageYears == null ? '—' : `${a.birthYearEstimated ? '~' : ''}${a.ageYears} y` },
              { header: 'Health', render: (a) => <Chip value={a.healthStatus} /> },
              { header: 'Status', render: (a) => <Chip value={a.status} /> },
              { header: 'Zone', render: (a) => a.zoneName ?? '—' },
              { header: 'Last seen', render: (a) => <span title={fmtDateTime(a.lastSeenAt)}>{fmtAgo(a.lastSeenAt)}</span> },
            ]}
          />
          <Pager page={list.data} onPage={setPage} />
        </>
      )}

      <AnimalForm open={creating} onClose={() => setCreating(false)} species={species} zones={zones}
        onSaved={(a) => { toast(`${a.tagId} registered`); a.warnings.forEach((w) => toast(w, 'warn')); list.reload(); }} />
      {selected !== null && (
        <AnimalDrawer id={selected} onClose={() => setSelected(null)} onChanged={list.reload} />
      )}
    </>
  );
}

function animalFields(species: Species[], zones: ReturnType<typeof useLookups>['zones']): Field[] {
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

function AnimalForm({ open, onClose, species, zones, animal, onSaved }: {
  open: boolean; onClose: () => void; species: Species[]; zones: ReturnType<typeof useLookups>['zones'];
  animal?: Animal; onSaved: (a: Animal) => void;
}) {
  return (
    <FormModal
      open={open}
      onClose={onClose}
      title={animal ? `Edit ${animal.tagId}` : 'Register animal'}
      submitLabel={animal ? 'Save changes' : 'Register animal'}
      fields={animalFields(species, zones)}
      initial={animal ?? { sex: 'UNKNOWN', healthStatus: 'HEALTHY', status: 'IN_WILD', birthYearEstimated: true }}
      onSubmit={async (v) => {
        const saved = animal ? await wildlifeApi.updateAnimal(animal.id, v) : await wildlifeApi.createAnimal(v);
        onSaved(saved);
      }}
    />
  );
}

function AnimalDrawer({ id, onClose, onChanged }: { id: number; onClose: () => void; onChanged: () => void }) {
  const { hasRole } = useAuth();
  const toast = useToast();
  const { species, zones } = useLookups();
  const animal = useLoad(() => wildlifeApi.animal(id), [id]);
  const records = useLoad(() => wildlifeApi.healthRecords(id), [id]);
  const [modal, setModal] = useState<'edit' | 'record' | 'status' | null>(null);

  const a = animal.data;
  const refresh = () => { animal.reload(); records.reload(); onChanged(); };

  return (
    <Drawer open onClose={onClose} accent={a ? IUCN[a.conservationStatus]?.color : undefined}>
      {!a ? <Loading /> : (
        <>
          <div className="drawer-head">
            <h2>{a.tagId}{a.name && <span className="alias">{a.name}</span>}</h2>
            <div className="sub">{a.speciesCommonName} <em className="muted">{a.speciesScientificName}</em></div>
            <div className="actions">
              {hasRole('ADMIN', 'FOREST_OFFICER', 'VET') &&
                <button className="btn btn-primary" onClick={() => setModal('record')}>Add health record</button>}
              {hasRole('ADMIN', 'FOREST_OFFICER', 'RANGER', 'VET') && <>
                <button className="btn btn-secondary" onClick={() => setModal('status')}>Change status</button>
                <button className="btn btn-secondary" onClick={() => setModal('edit')}>Edit</button>
              </>}
            </div>
          </div>
          <div className="drawer-body">
            <Warnings items={a.warnings} />
            <dl className="facts panel">
              <Fact term="Health"><Chip value={a.healthStatus} /></Fact>
              <Fact term="Status"><Chip value={a.status} /></Fact>
              <Fact term="Threat level"><IucnBadge status={a.conservationStatus} /></Fact>
              <Fact term="Sex">{label(a.sex)}</Fact>
              <Fact term="Age">{a.ageYears == null ? 'Unknown' : `${a.birthYearEstimated ? 'About ' : ''}${a.ageYears} years (born ${a.birthYear})`}</Fact>
              <Fact term="Zone">{a.zoneName ?? (a.zoneId ? `Zone ${a.zoneId}` : '—')}</Fact>
              <Fact term="Last seen">{a.lastSeenAt ? `${fmtDateTime(a.lastSeenAt)} (${fmtAgo(a.lastSeenAt)})` : 'Never'}</Fact>
              <Fact term="Last known location">{fmtCoord(a.lastSeenLatitude, a.lastSeenLongitude)}</Fact>
              {a.identificationMarks && <Fact term="Identification marks" wide>{a.identificationMarks}</Fact>}
              {a.notes && <Fact term="Notes" wide><span className="pre">{a.notes}</span></Fact>}
            </dl>
            {a.lastSeenLatitude != null && a.lastSeenLongitude != null && (
              <MapView height={200} markers={[{ id: a.id, lat: a.lastSeenLatitude, lng: a.lastSeenLongitude, label: `${a.tagId} last seen`, color: IUCN[a.conservationStatus]?.color }]} />
            )}
            <h3>Health records</h3>
            {records.data && records.data.length === 0 && <Empty title="No health records yet" />}
            <ol className="timeline">
              {records.data?.map((r) => (
                <li key={r.id}>
                  <div className="when">{fmtDate(r.recordDate)}</div>
                  <div className="what">{label(r.recordType)} <Chip value={r.healthStatus} /></div>
                  {r.diagnosis && <div>{r.diagnosis}</div>}
                  {r.treatment && <div className="muted">Treatment: {r.treatment}</div>}
                  <div className="meta">
                    {r.vetName && <span>Vet: {r.vetName}</span>}
                    {r.weightKg != null && <span>{r.weightKg} kg</span>}
                    {r.recordedByName && <span>Recorded by {r.recordedByName}</span>}
                  </div>
                </li>
              ))}
            </ol>
          </div>

          <AnimalForm open={modal === 'edit'} onClose={() => setModal(null)} species={species} zones={zones} animal={a}
            onSaved={(s) => { toast(`${s.tagId} updated`); s.warnings.forEach((w) => toast(w, 'warn')); refresh(); }} />
          <FormModal
            open={modal === 'record'} onClose={() => setModal(null)}
            title={`Health record for ${a.tagId}`} submitLabel="Add record"
            intro="If this is the newest record, the animal's current health changes to the status you choose."
            initial={{ recordDate: todayIso(), recordType: 'CHECKUP', healthStatus: a.healthStatus }}
            fields={[
              { name: 'recordDate', label: 'Date', type: 'date', required: true },
              { name: 'recordType', label: 'Type', type: 'select', required: true, enumValues: RECORD_TYPES },
              { name: 'healthStatus', label: 'Health observed', type: 'select', required: true, enumValues: HEALTH },
              { name: 'weightKg', label: 'Weight (kg)', type: 'number', step: 'any' },
              { name: 'vetName', label: 'Vet' },
              { name: 'diagnosis', label: 'Diagnosis', wide: true },
              { name: 'treatment', label: 'Treatment', type: 'textarea', wide: true },
              { name: 'notes', label: 'Notes', type: 'textarea', wide: true },
            ]}
            onSubmit={async (v) => { await wildlifeApi.addHealthRecord(a.id, v); toast('Health record added'); refresh(); }}
          />
          <FormModal
            open={modal === 'status'} onClose={() => setModal(null)}
            title={`Change status of ${a.tagId}`} submitLabel="Change status"
            initial={{ status: a.status }}
            fields={[
              { name: 'status', label: 'New status', type: 'select', required: true, enumValues: STATUS },
              { name: 'healthStatus', label: 'Health', type: 'select', enumValues: HEALTH, placeholder: 'Leave unchanged' },
              { name: 'note', label: 'Reason', type: 'textarea', wide: true },
            ]}
            onSubmit={async (v) => { const s = await wildlifeApi.setAnimalStatus(a.id, v); toast(`${s.tagId} is now ${label(s.status).toLowerCase()}`); refresh(); }}
          />
        </>
      )}
    </Drawer>
  );
}

// ==================================================================== species

function SpeciesTab() {
  const { hasRole } = useAuth();
  const toast = useToast();
  const [q, setQ] = useState('');
  const [editing, setEditing] = useState<Species | 'new' | null>(null);
  const list = useLoad(() => wildlifeApi.species({ q }), [q]);
  const canEdit = hasRole('ADMIN', 'FOREST_OFFICER');

  return (
    <>
      <div className="toolbar">
        <input className="input search" placeholder="Search common or scientific name" value={q} onChange={(e) => setQ(e.target.value)} aria-label="Search species" />
        <div className="spacer" />
        {canEdit && <button className="btn btn-primary" onClick={() => setEditing('new')}>Add species</button>}
      </div>
      <ErrorBanner error={list.error} />
      {list.data && (
        <DataTable<Species>
          rows={list.data.content}
          rowKey={(s) => s.id}
          onRowClick={canEdit ? (s) => setEditing(s) : undefined}
          stripe={(s) => IUCN[s.conservationStatus]?.color}
          empty={<Empty title="No species found" />}
          columns={[
            { header: 'Common name', render: (s) => <strong>{s.commonName}</strong> },
            { header: 'Scientific name', render: (s) => <em>{s.scientificName}</em> },
            { header: 'Category', render: (s) => label(s.category) },
            { header: 'IUCN status', render: (s) => <IucnBadge status={s.conservationStatus} /> },
            { header: 'Legal protection', render: (s) => s.protectionSchedule ?? '—' },
            { header: 'Animals', render: (s) => s.animalCount, className: 'num' },
          ]}
        />
      )}
      <FormModal
        open={editing !== null}
        onClose={() => setEditing(null)}
        title={editing && editing !== 'new' ? `Edit ${editing.commonName}` : 'Add species'}
        submitLabel={editing && editing !== 'new' ? 'Save changes' : 'Add species'}
        initial={editing && editing !== 'new' ? editing : { category: 'MAMMAL', conservationStatus: 'LEAST_CONCERN' }}
        fields={[
          { name: 'commonName', label: 'Common name', required: true },
          { name: 'scientificName', label: 'Scientific name', required: true },
          { name: 'category', label: 'Category', type: 'select', required: true, enumValues: CATEGORIES },
          { name: 'conservationStatus', label: 'IUCN Red List status', type: 'select', required: true,
            options: IUCN_ORDER.map((s) => ({ value: s, label: `${label(s)} (${IUCN[s].code})` })) },
          { name: 'protectionSchedule', label: 'Legal protection', placeholder: 'Schedule I', wide: true },
          { name: 'description', label: 'Description', type: 'textarea', wide: true },
        ]}
        onSubmit={async (v) => {
          if (editing && editing !== 'new') await wildlifeApi.updateSpecies(editing.id, v);
          else await wildlifeApi.createSpecies(v);
          toast('Species saved');
          refreshLookups();
          list.reload();
        }}
      />
    </>
  );
}
