// One function per endpoint the UI uses. Paths and parameters follow CONTRACTS.md section 7.
import { http } from './client';
import type {
  Allocation, Animal, Area, Dashboard, Equipment, HealthRecord, Incident, IncidentDetail, IncidentStatus,
  IncidentSummary, LoginResponse, Page, PatrolLog, PatrolRoute, Programme, ProgrammeActivity, ProgrammeDetail,
  ProgrammeTarget, ProgressUpdate, RehabProgress, RescueCase, RescueDetail, Schedule, ScheduleDetail, Species,
  Staff, StaffRef, Team, Treatment, UserResponse, Vehicle, WaterResource, Habitat, Zone, ZoneMonthlyIncidentReport,
  ZoneReport, AssignedPersonnel,
} from './types';

type Body = Record<string, unknown>;
type Params = Record<string, string | number | boolean | null | undefined>;

// ---------------------------------------------------------------- auth (8081)
export const authApi = {
  login: (username: string, password: string) => http.post<LoginResponse>('/api/auth/login', { username, password }),
  me: () => http.get<UserResponse>('/api/auth/me'),
  changePassword: (currentPassword: string, newPassword: string) =>
    http.post<void>('/api/auth/change-password', { currentPassword, newPassword }),
  users: (params: Params) => http.get<Page<UserResponse>>('/api/users', params),
  createUser: (body: Body) => http.post<UserResponse>('/api/users', body),
  updateUser: (id: number, body: Body) => http.put<UserResponse>(`/api/users/${id}`, body),
  setActive: (id: number, active: boolean) => http.patch<UserResponse>(`/api/users/${id}/active`, { active }),
  resetPassword: (id: number, newPassword: string) => http.put<void>(`/api/users/${id}/password`, { newPassword }),
};

// ---------------------------------------------------------------- habitat (8082)
export const habitatApi = {
  areas: (params: Params = {}) => http.get<Page<Area>>('/api/areas', { size: 100, ...params }),
  createArea: (body: Body) => http.post<Area>('/api/areas', body),
  zones: (params: Params = {}) => http.get<Page<Zone>>('/api/zones', { size: 100, ...params }),
  zone: (id: number) => http.get<Zone>(`/api/zones/${id}`),
  createZone: (body: Body) => http.post<Zone>('/api/zones', body),
  habitats: (params: Params = {}) => http.get<Page<Habitat>>('/api/habitats', { size: 100, ...params }),
  createHabitat: (body: Body) => http.post<Habitat>('/api/habitats', body),
  water: (params: Params = {}) => http.get<Page<WaterResource>>('/api/water-resources', { size: 100, ...params }),
  createWater: (body: Body) => http.post<WaterResource>('/api/water-resources', body),
  setWaterStatus: (id: number, body: Body) => http.patch<WaterResource>(`/api/water-resources/${id}/status`, body),
};

// ---------------------------------------------------------------- wildlife (8083)
export const wildlifeApi = {
  species: (params: Params = {}) => http.get<Page<Species>>('/api/species', { size: 100, ...params }),
  createSpecies: (body: Body) => http.post<Species>('/api/species', body),
  updateSpecies: (id: number, body: Body) => http.put<Species>(`/api/species/${id}`, body),
  animals: (params: Params) => http.get<Page<Animal>>('/api/animals', params),
  animal: (id: number) => http.get<Animal>(`/api/animals/${id}`),
  createAnimal: (body: Body) => http.post<Animal>('/api/animals', body),
  updateAnimal: (id: number, body: Body) => http.put<Animal>(`/api/animals/${id}`, body),
  setAnimalStatus: (id: number, body: Body) => http.patch<Animal>(`/api/animals/${id}/status`, body),
  healthRecords: (id: number) => http.get<HealthRecord[]>(`/api/animals/${id}/health-records`),
  addHealthRecord: (id: number, body: Body) => http.post<HealthRecord>(`/api/animals/${id}/health-records`, body),
};

// ---------------------------------------------------------------- resources (8084)
export const resourceApi = {
  staff: (params: Params = {}) => http.get<Page<Staff>>('/api/staff', { size: 100, ...params }),
  staffLookup: (ids: number[]) => http.get<StaffRef[]>('/api/staff/lookup', { ids: ids.join(',') }),
  createStaff: (body: Body) => http.post<Staff>('/api/staff', body),
  updateStaff: (id: number, body: Body) => http.put<Staff>(`/api/staff/${id}`, body),
  vehicles: (params: Params = {}) => http.get<Page<Vehicle>>('/api/vehicles', { size: 100, ...params }),
  createVehicle: (body: Body) => http.post<Vehicle>('/api/vehicles', body),
  updateVehicle: (id: number, body: Body) => http.put<Vehicle>(`/api/vehicles/${id}`, body),
  equipment: (params: Params = {}) => http.get<Page<Equipment>>('/api/equipment', { size: 100, ...params }),
  createEquipment: (body: Body) => http.post<Equipment>('/api/equipment', body),
  updateEquipment: (id: number, body: Body) => http.put<Equipment>(`/api/equipment/${id}`, body),
  allocations: (params: Params) => http.get<Page<Allocation>>('/api/allocations', params),
  createAllocation: (body: Body) => http.post<Allocation>('/api/allocations', body),
  setAllocationStatus: (id: number, status: string) => http.patch<Allocation>(`/api/allocations/${id}/status`, { status }),
};

// ---------------------------------------------------------------- patrols (8085)
export const patrolApi = {
  teams: (params: Params = {}) => http.get<Page<Team>>('/api/patrols/teams', { size: 100, ...params }),
  createTeam: (body: Body) => http.post<Team>('/api/patrols/teams', body),
  routes: (params: Params = {}) => http.get<Page<PatrolRoute>>('/api/patrols/routes', { size: 100, ...params }),
  createRoute: (body: Body) => http.post<PatrolRoute>('/api/patrols/routes', body),
  schedules: (params: Params) => http.get<Page<Schedule>>('/api/patrols/schedules', params),
  schedule: (id: number) => http.get<ScheduleDetail>(`/api/patrols/schedules/${id}`),
  createSchedule: (body: Body) => http.post<Schedule>('/api/patrols/schedules', body),
  setScheduleStatus: (id: number, status: string, note?: string) =>
    http.patch<Schedule>(`/api/patrols/schedules/${id}/status`, { status, note }),
  addLog: (id: number, body: Body) => http.post<PatrolLog>(`/api/patrols/schedules/${id}/logs`, body),
  escalate: (logId: number, body: Body) => http.post<PatrolLog>(`/api/patrols/logs/${logId}/escalate`, body),
};

// ---------------------------------------------------------------- incidents (8086)
export const incidentApi = {
  list: (params: Params) => http.get<Page<Incident>>('/api/incidents', params),
  get: (id: number) => http.get<IncidentDetail>(`/api/incidents/${id}`),
  create: (body: Body) => http.post<Incident>('/api/incidents', body),
  transition: (id: number, toStatus: IncidentStatus, note?: string) =>
    http.post<Incident>(`/api/incidents/${id}/transition`, { toStatus, note }),
  assign: (id: number, body: Body) => http.post<AssignedPersonnel & { warnings: string[] }>(`/api/incidents/${id}/assignments`, body),
  unassign: (id: number, assignmentId: number) => http.del(`/api/incidents/${id}/assignments/${assignmentId}`),
  /** Mandatory example: incidents in a zone for the current month, optional status filter. */
  zoneCurrentMonth: (zoneId: number, status?: IncidentStatus | '') =>
    http.get<ZoneMonthlyIncidentReport>(`/api/incidents/zone/${zoneId}/current-month`, { status: status || undefined }),
  summary: (year?: number, month?: number) => http.get<IncidentSummary>('/api/incidents/summary', { year, month }),
};

// ---------------------------------------------------------------- rescues (8087)
export const rescueApi = {
  list: (params: Params) => http.get<Page<RescueCase>>('/api/rescues', params),
  get: (id: number) => http.get<RescueDetail>(`/api/rescues/${id}`),
  create: (body: Body) => http.post<RescueCase>('/api/rescues', body),
  transition: (id: number, body: Body) => http.post<RescueCase>(`/api/rescues/${id}/transition`, body),
  addTreatment: (id: number, body: Body) => http.post<Treatment>(`/api/rescues/${id}/treatments`, body),
  addRehab: (id: number, body: Body) => http.post<RehabProgress>(`/api/rescues/${id}/rehab-progress`, body),
};

// ---------------------------------------------------------------- programmes (8088)
export const programmeApi = {
  list: (params: Params) => http.get<Page<Programme>>('/api/programmes', params),
  get: (id: number) => http.get<ProgrammeDetail>(`/api/programmes/${id}`),
  create: (body: Body) => http.post<Programme>('/api/programmes', body),
  setStatus: (id: number, status: string) => http.patch<Programme>(`/api/programmes/${id}/status`, { status }),
  addActivity: (id: number, body: Body) => http.post<ProgrammeActivity>(`/api/programmes/${id}/activities`, body),
  updateActivity: (id: number, activityId: number, body: Body) =>
    http.put<ProgrammeActivity>(`/api/programmes/${id}/activities/${activityId}`, body),
  addTarget: (id: number, body: Body) => http.post<ProgrammeTarget>(`/api/programmes/${id}/targets`, body),
  addProgress: (id: number, body: Body) => http.post<ProgressUpdate>(`/api/programmes/${id}/progress`, body),
};

// ---------------------------------------------------------------- reporting (8089) and gateway
export const reportApi = {
  dashboard: () => http.get<Dashboard>('/api/reports/dashboard'),
  monthlyIncidents: (year: number, month: number) => http.get<IncidentSummary>('/api/reports/incidents/monthly', { year, month }),
  zone: (zoneId: number) => http.get<ZoneReport>(`/api/reports/zones/${zoneId}`),
  gatewayHealth: () => http.get<{ services: Record<string, 'UP' | 'DOWN'> }>('/api/gateway/health'),
};
