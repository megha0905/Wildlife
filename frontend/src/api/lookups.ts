// Reference lists used by many dropdowns. Loaded once per session and shared between pages.
import { useEffect, useState } from 'react';
import { habitatApi, resourceApi, wildlifeApi } from './endpoints';
import type { Area, Species, Staff, Zone } from './types';

interface Lookups {
  areas: Area[];
  zones: Zone[];
  species: Species[];
  staff: Staff[];
}

let cache: Promise<Lookups> | null = null;

function load(): Promise<Lookups> {
  // each list degrades to [] if its service is down, so one outage doesn't empty every dropdown
  const safe = <T,>(p: Promise<{ content: T[] }>) => p.then((r) => r.content).catch(() => [] as T[]);
  return Promise.all([
    safe<Area>(habitatApi.areas()),
    safe<Zone>(habitatApi.zones()),
    safe<Species>(wildlifeApi.species()),
    safe<Staff>(resourceApi.staff()),
  ]).then(([areas, zones, species, staff]) => ({ areas, zones, species, staff }));
}

/** Clears the cache, e.g. after creating a zone or species. */
const REFRESH_EVENT = 'wildguard:lookups';

export function refreshLookups() {
  cache = null;
  window.dispatchEvent(new Event(REFRESH_EVENT));
}

export function useLookups(): Lookups & { ready: boolean } {
  const [data, setData] = useState<Lookups>({ areas: [], zones: [], species: [], staff: [] });
  const [ready, setReady] = useState(false);
  const [version, setVersion] = useState(0);
  useEffect(() => {
    const onRefresh = () => setVersion((v) => v + 1);
    window.addEventListener(REFRESH_EVENT, onRefresh);
    return () => window.removeEventListener(REFRESH_EVENT, onRefresh);
  }, []);
  useEffect(() => {
    if (!cache) cache = load();
    let alive = true;
    cache.then((d) => { if (alive) { setData(d); setReady(true); } });
    return () => { alive = false; };
  }, [version]);
  return { ...data, ready };
}

export const zoneOptions = (zones: Zone[]) =>
  zones.map((z) => ({ value: z.id, label: `${z.name} (${z.areaName})` }));
export const areaOptions = (areas: Area[]) => areas.map((a) => ({ value: a.id, label: a.name }));
export const staffOptions = (staff: Staff[]) =>
  staff.filter((s) => s.status === 'ACTIVE').map((s) => ({ value: s.id, label: `${s.fullName} (${s.designation.replace(/_/g, ' ').toLowerCase()})` }));
export const speciesOptions = (species: Species[]) => species.map((s) => ({ value: s.id, label: s.commonName }));
