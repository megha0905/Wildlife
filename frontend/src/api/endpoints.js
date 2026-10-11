// One function per endpoint the UI uses. Paths and parameters follow CONTRACTS.md section 7.
import { http } from './client.js';

// ---------------------------------------------------------------- auth (8081)
export const authApi = {
  login: (username, password) => http.post('/api/auth/login', { username, password }),
  me: () => http.get('/api/auth/me'),
  changePassword: (currentPassword, newPassword) =>
    http.post('/api/auth/change-password', { currentPassword, newPassword }),
  users: (params) => http.get('/api/users', params),
  createUser: (body) => http.post('/api/users', body),
  updateUser: (id, body) => http.put(`/api/users/${id}`, body),
  setActive: (id, active) => http.patch(`/api/users/${id}/active`, { active }),
  resetPassword: (id, newPassword) => http.put(`/api/users/${id}/password`, { newPassword }),
};

// ---------------------------------------------------------------- habitat (8082)
export const habitatApi = {
  areas: (params = {}) => http.get('/api/areas', { size: 100, ...params }),
  createArea: (body) => http.post('/api/areas', body),
  zones: (params = {}) => http.get('/api/zones', { size: 100, ...params }),
  zone: (id) => http.get(`/api/zones/${id}`),
  createZone: (body) => http.post('/api/zones', body),
  habitats: (params = {}) => http.get('/api/habitats', { size: 100, ...params }),
  createHabitat: (body) => http.post('/api/habitats', body),
  water: (params = {}) => http.get('/api/water-resources', { size: 100, ...params }),
  createWater: (body) => http.post('/api/water-resources', body),
  setWaterStatus: (id, body) => http.patch(`/api/water-resources/${id}/status`, body),
};

// ---------------------------------------------------------------- wildlife (8083)
export const wildlifeApi = {
  species: (params = {}) => http.get('/api/species', { size: 100, ...params }),
  createSpecies: (body) => http.post('/api/species', body),
  updateSpecies: (id, body) => http.put(`/api/species/${id}`, body),
  animals: (params) => http.get('/api/animals', params),
  animal: (id) => http.get(`/api/animals/${id}`),
  createAnimal: (body) => http.post('/api/animals', body),
  updateAnimal: (id, body) => http.put(`/api/animals/${id}`, body),
  setAnimalStatus: (id, body) => http.patch(`/api/animals/${id}/status`, body),
  healthRecords: (id) => http.get(`/api/animals/${id}/health-records`),
  addHealthRecord: (id, body) => http.post(`/api/animals/${id}/health-records`, body),
};

// ---------------------------------------------------------------- resources (8084)
export const resourceApi = {
  staff: (params = {}) => http.get('/api/staff', { size: 100, ...params }),
  staffLookup: (ids) => http.get('/api/staff/lookup', { ids: ids.join(',') }),
  createStaff: (body) => http.post('/api/staff', body),
  updateStaff: (id, body) => http.put(`/api/staff/${id}`, body),
  vehicles: (params = {}) => http.get('/api/vehicles', { size: 100, ...params }),
  createVehicle: (body) => http.post('/api/vehicles', body),
  updateVehicle: (id, body) => http.put(`/api/vehicles/${id}`, body),
  equipment: (params = {}) => http.get('/api/equipment', { size: 100, ...params }),
  createEquipment: (body) => http.post('/api/equipment', body),
  updateEquipment: (id, body) => http.put(`/api/equipment/${id}`, body),
  allocations: (params) => http.get('/api/allocations', params),
  createAllocation: (body) => http.post('/api/allocations', body),
  setAllocationStatus: (id, status) => http.patch(`/api/allocations/${id}/status`, { status }),
};

// ---------------------------------------------------------------- patrols (8085)
export const patrolApi = {
  teams: (params = {}) => http.get('/api/patrols/teams', { size: 100, ...params }),
  createTeam: (body) => http.post('/api/patrols/teams', body),
  routes: (params = {}) => http.get('/api/patrols/routes', { size: 100, ...params }),
  createRoute: (body) => http.post('/api/patrols/routes', body),
  schedules: (params) => http.get('/api/patrols/schedules', params),
  schedule: (id) => http.get(`/api/patrols/schedules/${id}`),
  createSchedule: (body) => http.post('/api/patrols/schedules', body),
  setScheduleStatus: (id, status, note) =>
    http.patch(`/api/patrols/schedules/${id}/status`, { status, note }),
  addLog: (id, body) => http.post(`/api/patrols/schedules/${id}/logs`, body),
  escalate: (logId, body) => http.post(`/api/patrols/logs/${logId}/escalate`, body),
};

// ---------------------------------------------------------------- incidents (8086)
export const incidentApi = {
  list: (params) => http.get('/api/incidents', params),
  get: (id) => http.get(`/api/incidents/${id}`),
  create: (body) => http.post('/api/incidents', body),
  transition: (id, toStatus, note) =>
    http.post(`/api/incidents/${id}/transition`, { toStatus, note }),
  assign: (id, body) => http.post(`/api/incidents/${id}/assignments`, body),
  unassign: (id, assignmentId) => http.del(`/api/incidents/${id}/assignments/${assignmentId}`),
  /** Mandatory example: incidents in a zone for the current month, optional status filter. */
  zoneCurrentMonth: (zoneId, status) =>
    http.get(`/api/incidents/zone/${zoneId}/current-month`, { status: status || undefined }),
  summary: (year, month) => http.get('/api/incidents/summary', { year, month }),
};

// ---------------------------------------------------------------- rescues (8087)
export const rescueApi = {
  list: (params) => http.get('/api/rescues', params),
  get: (id) => http.get(`/api/rescues/${id}`),
  create: (body) => http.post('/api/rescues', body),
  transition: (id, body) => http.post(`/api/rescues/${id}/transition`, body),
  addTreatment: (id, body) => http.post(`/api/rescues/${id}/treatments`, body),
  addRehab: (id, body) => http.post(`/api/rescues/${id}/rehab-progress`, body),
};

// ---------------------------------------------------------------- programmes (8088)
export const programmeApi = {
  list: (params) => http.get('/api/programmes', params),
  get: (id) => http.get(`/api/programmes/${id}`),
  create: (body) => http.post('/api/programmes', body),
  setStatus: (id, status) => http.patch(`/api/programmes/${id}/status`, { status }),
  addActivity: (id, body) => http.post(`/api/programmes/${id}/activities`, body),
  updateActivity: (id, activityId, body) =>
    http.put(`/api/programmes/${id}/activities/${activityId}`, body),
  addTarget: (id, body) => http.post(`/api/programmes/${id}/targets`, body),
  addProgress: (id, body) => http.post(`/api/programmes/${id}/progress`, body),
};

// ---------------------------------------------------------------- reporting (8089) and gateway
export const reportApi = {
  dashboard: () => http.get('/api/reports/dashboard'),
  monthlyIncidents: (year, month) => http.get('/api/reports/incidents/monthly', { year, month }),
  zone: (zoneId) => http.get(`/api/reports/zones/${zoneId}`),
  gatewayHealth: () => http.get('/api/gateway/health'),
};
