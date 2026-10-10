// TypeScript mirrors of the DTOs in CONTRACTS.md. Field names must match the backend exactly.

export type Role = 'ADMIN' | 'FOREST_OFFICER' | 'RANGER' | 'VET' | 'ANALYST';

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ApiErrorBody {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  details: string[];
}

// ---------------------------------------------------------------- auth
export interface UserResponse {
  id: number;
  username: string;
  fullName: string;
  email: string;
  roles: Role[];
  staffId: number | null;
  active: boolean;
  createdAt: string;
  lastLoginAt: string | null;
}
export interface LoginResponse {
  accessToken: string;
  tokenType: 'Bearer';
  expiresIn: number;
  user: UserResponse;
}

// ---------------------------------------------------------------- habitat
export type AreaType = 'NATIONAL_PARK' | 'WILDLIFE_SANCTUARY' | 'TIGER_RESERVE' | 'CONSERVATION_RESERVE' | 'COMMUNITY_RESERVE' | 'BIOSPHERE_RESERVE';
export type ZoneType = 'CORE' | 'BUFFER' | 'ECO_SENSITIVE' | 'TOURISM';
export type HabitatType = 'GRASSLAND' | 'TROPICAL_DRY_FOREST' | 'TROPICAL_MOIST_FOREST' | 'WETLAND' | 'RIVERINE' | 'DESERT' | 'MONTANE' | 'MANGROVE' | 'SCRUBLAND';
export type HabitatCondition = 'GOOD' | 'FAIR' | 'DEGRADED' | 'CRITICAL';
export type WaterSourceType = 'RIVER' | 'STREAM' | 'LAKE' | 'POND' | 'WATERHOLE' | 'ARTIFICIAL_WATERHOLE' | 'SPRING';
export type WaterStatus = 'AVAILABLE' | 'LOW' | 'DRY' | 'CONTAMINATED';

export interface Area {
  id: number; name: string; code: string; areaType: AreaType; state: string; district: string | null;
  totalAreaSqKm: number; establishedYear: number | null; centerLatitude: number; centerLongitude: number;
  description: string | null; zoneCount: number;
}
export interface Zone {
  id: number; areaId: number; areaName: string; name: string; code: string; zoneType: ZoneType;
  areaSqKm: number | null; centerLatitude: number; centerLongitude: number; description: string | null;
}
export interface Habitat {
  id: number; zoneId: number; zoneName: string; areaId: number; name: string; habitatType: HabitatType;
  condition: HabitatCondition; areaSqKm: number | null; latitude: number | null; longitude: number | null;
  dominantVegetation: string | null; lastSurveyDate: string | null; notes: string | null;
}
export interface WaterResource {
  id: number; zoneId: number; zoneName: string; areaId: number; name: string; sourceType: WaterSourceType;
  status: WaterStatus; perennial: boolean; latitude: number; longitude: number; capacityCubicM: number | null;
  lastInspectedAt: string | null; notes: string | null;
}

// ---------------------------------------------------------------- wildlife
export type SpeciesCategory = 'MAMMAL' | 'BIRD' | 'REPTILE' | 'AMPHIBIAN' | 'FISH' | 'INVERTEBRATE';
export type ConservationStatus = 'LEAST_CONCERN' | 'NEAR_THREATENED' | 'VULNERABLE' | 'ENDANGERED' | 'CRITICALLY_ENDANGERED' | 'EXTINCT_IN_THE_WILD' | 'DATA_DEFICIENT' | 'NOT_EVALUATED';
export type Sex = 'MALE' | 'FEMALE' | 'UNKNOWN';
export type HealthStatus = 'HEALTHY' | 'INJURED' | 'SICK' | 'UNDER_TREATMENT' | 'RECOVERING' | 'DECEASED' | 'UNKNOWN';
export type AnimalStatus = 'IN_WILD' | 'IN_RESCUE' | 'TRANSFERRED' | 'MISSING' | 'DECEASED';
export type HealthRecordType = 'CHECKUP' | 'OBSERVATION' | 'INJURY_ASSESSMENT' | 'TREATMENT' | 'VACCINATION' | 'POST_MORTEM';

export interface Species {
  id: number; commonName: string; scientificName: string; category: SpeciesCategory;
  conservationStatus: ConservationStatus; protectionSchedule: string | null; description: string | null; animalCount: number;
}
export interface Animal {
  id: number; tagId: string; name: string | null; speciesId: number; speciesCommonName: string; speciesScientificName: string;
  conservationStatus: ConservationStatus; sex: Sex; birthYear: number | null; birthYearEstimated: boolean; ageYears: number | null;
  healthStatus: HealthStatus; status: AnimalStatus; identificationMarks: string | null; areaId: number | null; zoneId: number | null;
  zoneName: string | null; lastSeenLatitude: number | null; lastSeenLongitude: number | null; lastSeenAt: string | null;
  notes: string | null; createdAt: string; updatedAt: string; warnings: string[];
}
export interface HealthRecord {
  id: number; animalId: number; recordDate: string; recordType: HealthRecordType; healthStatus: HealthStatus;
  weightKg: number | null; diagnosis: string | null; treatment: string | null; vetName: string | null;
  recordedByUserId: number | null; recordedByName: string | null; notes: string | null; createdAt: string;
}

// ---------------------------------------------------------------- resources
export type Designation = 'FOREST_OFFICER' | 'RANGE_OFFICER' | 'FOREST_GUARD' | 'VETERINARIAN' | 'DRIVER' | 'TRACKER' | 'RESEARCHER';
export type StaffStatus = 'ACTIVE' | 'ON_LEAVE' | 'INACTIVE';
export type VehicleType = 'JEEP' | 'PICKUP' | 'MOTORCYCLE' | 'BOAT' | 'RESCUE_VAN' | 'TRACTOR';
export type EquipmentType = 'CAMERA_TRAP' | 'GPS_UNIT' | 'RADIO' | 'BINOCULARS' | 'DRONE' | 'TRANQUILIZER_GUN' | 'FIRST_AID_KIT' | 'RESCUE_CAGE' | 'NIGHT_VISION';
export type ResourceStatus = 'AVAILABLE' | 'IN_USE' | 'MAINTENANCE' | 'RETIRED';
export type ResourceKind = 'STAFF' | 'VEHICLE' | 'EQUIPMENT';
export type AllocationPurpose = 'PATROL' | 'INCIDENT' | 'RESCUE' | 'PROGRAMME' | 'OTHER';
export type AllocationStatus = 'PLANNED' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED';

export interface Staff {
  id: number; employeeCode: string; fullName: string; designation: Designation; status: StaffStatus;
  phone: string | null; email: string | null; homeAreaId: number | null; joinedOn: string | null;
}
export interface StaffRef { id: number; employeeCode: string | null; fullName: string | null; designation: string | null; status: string | null; }
export interface Vehicle {
  id: number; registrationNumber: string; vehicleType: VehicleType; status: ResourceStatus; make: string | null;
  model: string | null; seatingCapacity: number | null; homeAreaId: number | null; lastServicedOn: string | null;
}
export interface Equipment {
  id: number; assetTag: string; name: string; equipmentType: EquipmentType; status: ResourceStatus;
  homeAreaId: number | null; purchasedOn: string | null; notes: string | null;
}
export interface Allocation {
  id: number; resourceKind: ResourceKind; resourceId: number; resourceLabel: string; purpose: AllocationPurpose;
  referenceId: number | null; referenceLabel: string | null; startAt: string; endAt: string; status: AllocationStatus;
  allocatedByUserId: number | null; allocatedByName: string | null; notes: string | null; createdAt: string;
}

// ---------------------------------------------------------------- patrols
export type Shift = 'MORNING' | 'EVENING' | 'NIGHT';
export type PatrolStatus = 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED' | 'MISSED';
export type RouteDifficulty = 'EASY' | 'MODERATE' | 'HARD';
export type ObservationType = 'ROUTINE' | 'ANIMAL_SIGHTING' | 'SUSPICIOUS_ACTIVITY' | 'SNARE_FOUND' | 'INFRASTRUCTURE_DAMAGE' | 'FIRE' | 'OTHER';
export type Severity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export interface Team {
  id: number; name: string; areaId: number; leaderStaffId: number; leaderName: string | null;
  members: StaffRef[]; active: boolean; warnings: string[];
}
export interface Waypoint { sequence: number; latitude: number; longitude: number; label: string | null; }
export interface PatrolRoute {
  id: number; name: string; areaId: number; zoneId: number; zoneName: string | null; distanceKm: number;
  estimatedHours: number | null; difficulty: RouteDifficulty; waypoints: Waypoint[]; description: string | null; warnings: string[];
}
export interface Schedule {
  id: number; code: string; teamId: number; teamName: string; routeId: number; routeName: string; areaId: number;
  zoneId: number; zoneName: string | null; patrolDate: string; shift: Shift; startTime: string; endTime: string;
  status: PatrolStatus; vehicleId: number | null; allocationIds: number[]; objective: string | null;
  startedAt: string | null; completedAt: string | null; warnings: string[];
}
export interface PatrolLog {
  id: number; scheduleId: number; loggedAt: string; observationType: ObservationType; latitude: number; longitude: number;
  description: string; animalTagId: string | null; severityHint: Severity | null; reportedByUserId: number | null;
  reportedByName: string | null; incidentId: number | null; createdAt: string; warnings: string[];
}
export interface ScheduleDetail extends Schedule { team: Team; route: PatrolRoute; logs: PatrolLog[]; }

// ---------------------------------------------------------------- incidents
export type IncidentType = 'WILDLIFE_INJURY' | 'HUMAN_ANIMAL_CONFLICT' | 'POACHING' | 'ILLEGAL_LOGGING' | 'ENCROACHMENT' | 'FOREST_FIRE' | 'OTHER_THREAT';
export type IncidentStatus = 'REPORTED' | 'VERIFIED' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED';
export type AssignmentRole = 'LEAD' | 'INVESTIGATOR' | 'SUPPORT' | 'VETERINARY';

export interface AssignedPersonnel {
  assignmentId: number; staffId: number; staffName: string | null; designation: string | null;
  assignmentRole: AssignmentRole; assignedAt: string;
}
export interface Incident {
  id: number; incidentCode: string; title: string; incidentType: IncidentType; severity: Severity; status: IncidentStatus;
  description: string; areaId: number; zoneId: number; zoneName: string | null; latitude: number; longitude: number;
  locationDescription: string | null; occurredAt: string; reportedAt: string; reportedByUserId: number | null;
  reportedByName: string | null; verifiedAt: string | null; resolvedAt: string | null; closedAt: string | null;
  resolutionNotes: string | null; animalId: number | null; speciesId: number | null; sourcePatrolLogId: number | null;
  assignedPersonnel: AssignedPersonnel[]; warnings: string[];
}
export interface StatusHistory {
  id: number; fromStatus: string | null; toStatus: string; changedByUserId: number | null;
  changedByName: string | null; changedAt: string; note: string | null;
}
export interface IncidentDetail extends Incident { history: StatusHistory[]; }
export interface MonthlyIncidentItem {
  id: number; incidentCode: string; title: string; incidentType: IncidentType; severity: Severity; status: IncidentStatus;
  occurredAt: string; latitude: number; longitude: number; locationDescription: string | null; assignedPersonnel: AssignedPersonnel[];
}
export interface ZoneMonthlyIncidentReport {
  zoneId: number; zoneName: string | null; areaId: number | null; areaName: string | null; month: string;
  periodStart: string; periodEnd: string; statusFilter: IncidentStatus | null; totalIncidents: number;
  statusSummary: Record<IncidentStatus, number>; incidents: MonthlyIncidentItem[]; warnings: string[];
}

// ---------------------------------------------------------------- rescue
export type RescueCause = 'INJURY' | 'ORPHANED' | 'HUMAN_CONFLICT' | 'SNARE' | 'ROAD_ACCIDENT' | 'ILLNESS' | 'DISPLACED' | 'OTHER';
export type RescueStatus = 'REGISTERED' | 'UNDER_TREATMENT' | 'REHABILITATING' | 'TRANSFERRED' | 'RELEASED';

export interface TransferInfo { destinationFacility: string; reason: string; transferDate: string; }
export interface ReleaseInfo { releaseDate: string; releaseAreaId: number; releaseZoneId: number; latitude: number; longitude: number; notes: string | null; }
export interface RescueCase {
  id: number; caseNumber: string; animalId: number | null; animalTagId: string | null; speciesId: number | null;
  speciesName: string; animalDescription: string | null; cause: RescueCause; intakeCondition: HealthStatus; status: RescueStatus;
  rescuedAt: string; latitude: number; longitude: number; areaId: number | null; zoneId: number | null; incidentId: number | null;
  facilityName: string; registeredByName: string | null; notes: string | null; transfer: TransferInfo | null;
  release: ReleaseInfo | null; createdAt: string; updatedAt: string; warnings: string[];
}
export interface Treatment {
  id: number; rescueId: number; treatmentDate: string; diagnosis: string; procedure: string; medications: string | null;
  vetStaffId: number | null; vetName: string; notes: string | null; createdAt: string;
}
export interface RehabProgress {
  id: number; rescueId: number; assessedOn: string; enclosure: string | null; behaviourNotes: string;
  weightKg: number | null; fitForRelease: boolean; assessedBy: string; createdAt: string;
}
export interface RescueDetail extends RescueCase { treatments: Treatment[]; rehabProgress: RehabProgress[]; history: StatusHistory[]; }

// ---------------------------------------------------------------- programmes
export type ProgrammeType = 'SPECIES_RECOVERY' | 'HABITAT_RESTORATION' | 'ANTI_POACHING' | 'COMMUNITY_OUTREACH' | 'RESEARCH_MONITORING' | 'WATER_MANAGEMENT';
export type ProgrammeStatus = 'PLANNED' | 'ACTIVE' | 'ON_HOLD' | 'COMPLETED' | 'CANCELLED';
export type ActivityStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'DONE' | 'BLOCKED';

export interface Programme {
  id: number; code: string; name: string; programmeType: ProgrammeType; status: ProgrammeStatus; objective: string;
  description: string | null; startDate: string; endDate: string; budgetInr: number | null; leadStaffId: number | null;
  leadStaffName: string | null; areaIds: number[]; speciesIds: number[]; progressPercent: number; activityCount: number;
  activitiesDone: number; targetCount: number; warnings: string[];
}
export interface ProgrammeActivity {
  id: number; programmeId: number; title: string; description: string | null; plannedStart: string; plannedEnd: string;
  status: ActivityStatus; assignedStaffId: number | null; assignedStaffName: string | null; areaId: number | null; overdue: boolean;
}
export interface ProgrammeTarget {
  id: number; programmeId: number; metricName: string; unit: string; baselineValue: number; targetValue: number;
  currentValue: number; dueDate: string | null; progressPercent: number; lastUpdatedAt: string | null;
}
export interface ProgressUpdate {
  id: number; programmeId: number; targetId: number | null; targetMetricName: string | null; updateDate: string;
  previousValue: number | null; newValue: number | null; note: string; reportedByUserId: number | null;
  reportedByName: string | null; createdAt: string;
}
export interface ProgrammeDetail extends Programme {
  activities: ProgrammeActivity[]; targets: ProgrammeTarget[]; recentProgress: ProgressUpdate[];
}

// ---------------------------------------------------------------- summaries and reports
export type CountMap = Record<string, number>;
export interface HabitatSummary { totalAreas: number; totalZones: number; totalAreaSqKm: number; habitatsByCondition: CountMap; waterByStatus: CountMap; waterSourcesNeedingAttention: number; }
export interface WildlifeSummary { totalSpecies: number; totalAnimals: number; byHealthStatus: CountMap; byStatus: CountMap; byConservationStatus: CountMap; needingAttention: number; notSeenFor30Days: number; }
export interface ResourceSummary { totalStaff: number; staffByStatus: CountMap; staffByDesignation: CountMap; vehiclesByStatus: CountMap; equipmentByStatus: CountMap; activeAllocations: number; allocationsToday: number; }
export interface PatrolSummary { schedulesToday: number; schedulesThisMonthByStatus: CountMap; logsThisMonth: number; suspiciousActivityThisMonth: number; distancePatrolledKmThisMonth: number; }
export interface IncidentSummary { year: number; month: number; totalThisMonth: number; openIncidents: number; byStatus: CountMap; byType: CountMap; bySeverity: CountMap; criticalOpen: number; warnings?: string[]; }
export interface RescueSummary { activeCases: number; byStatus: CountMap; byCause: CountMap; releasedThisYear: number; transferredThisYear: number; admittedThisMonth: number; }
export interface ProgrammeSummary { byStatus: CountMap; byType: CountMap; activeProgrammes: number; averageProgressPercent: number; overdueActivities: number; totalBudgetInrActive: number; }

export interface Dashboard {
  generatedAt: string;
  habitat: HabitatSummary | null;
  wildlife: WildlifeSummary | null;
  resources: ResourceSummary | null;
  patrols: PatrolSummary | null;
  incidents: IncidentSummary | null;
  rescues: RescueSummary | null;
  programmes: ProgrammeSummary | null;
  servicesOnline: Record<string, boolean>;
  warnings: string[];
}
export interface ZoneReport {
  zone: Zone | null;
  animalsInZone: number | null;
  incidentsThisMonth: ZoneMonthlyIncidentReport | null;
  waterResources: WaterResource[] | null;
  warnings: string[];
}
