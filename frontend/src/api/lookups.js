// Reference lists used by many dropdowns (areas, zones, species, staff). Loaded once and shared.
import { habitatApi, resourceApi, wildlifeApi } from './endpoints.js';

let cache = null;

function load() {
  // each list degrades to [] if its service is down, so one outage doesn't empty every dropdown
  const safe = (p) => p.then((r) => r.content).catch(() => []);
  return Promise.all([
    safe(habitatApi.areas()),
    safe(habitatApi.zones()),
    safe(wildlifeApi.species()),
    safe(resourceApi.staff()),
  ]).then(([areas, zones, species, staff]) => ({ areas, zones, species, staff }));
}

/** Promise of { areas, zones, species, staff }. */
export function getLookups() {
  if (!cache) cache = load();
  return cache;
}

/** Clears the cache, e.g. after creating a zone or species. */
export function refreshLookups() {
  cache = null;
}

export const zoneOptions = (zones) => zones.map((z) => ({ value: z.id, label: `${z.name} (${z.areaName})` }));
export const areaOptions = (areas) => areas.map((a) => ({ value: a.id, label: a.name }));
export const staffOptions = (staff) =>
  staff.filter((s) => s.status === 'ACTIVE').map((s) => ({ value: s.id, label: `${s.fullName} (${s.designation.replace(/_/g, ' ').toLowerCase()})` }));
export const speciesOptions = (species) => species.map((s) => ({ value: s.id, label: s.commonName }));
