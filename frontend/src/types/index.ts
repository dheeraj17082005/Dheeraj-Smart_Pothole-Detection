export type SeverityClass = 'LOW' | 'MEDIUM' | 'HIGH';

export type PotholeStatus =
  | 'SUBMITTED'
  | 'PENDING_OFFICER_REVIEW'
  | 'ACCEPTED'
  | 'REJECTED'
  | 'IN_PROGRESS'
  | 'RESOLVED'
  | 'REPORTED'
  | 'ACKNOWLEDGED';

export type RejectionReason =
  | 'NO_POTHOLE'
  | 'DUPLICATE'
  | 'WRONG_LOCATION'
  | 'INSUFFICIENT_EVIDENCE'
  | 'OUTSIDE_JURISDICTION'
  | 'INVALID_REPORT';

export type DetectionJobStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';

export interface BoundingBox {
  xmin: number;
  ymin: number;
  xmax: number;
  ymax: number;
}

export interface DetectionResponse {
  id: string;
  box: BoundingBox;
  confidence: number;
  visual_area_ratio: number;
  frame_index?: number | null;
  frame_timestamp_sec?: number | null;
}

export interface AuthorityResponse {
  id: string;
  name: string;
  code: string;
  department_type: string;
}

export interface PotholeResponse {
  id: string;
  latitude: number;
  longitude: number;
  address_text?: string | null;
  first_detected_at?: string;
  severity_score: number;
  severity_class: SeverityClass;
  max_confidence: number;
  status: PotholeStatus;
  is_duplicate: boolean;
  duplicate_of_id?: string | null;
  rejection_reason?: RejectionReason | null;
  rejection_note?: string | null;
  accepted_at?: string | null;
  rejected_at?: string | null;
  authority?: AuthorityResponse | null;
  authority_code?: string | null;
  representative_key?: string | null;
  representative_image_url?: string | null;
  detections: DetectionResponse[];
  created_at?: string;
  updated_at?: string;
}

export interface JobResponse {
  id: string;
  status: DetectionJobStatus;
  started_at?: string | null;
  completed_at?: string | null;
  error_summary?: string | null;
}

export interface DetectImageResponse {
  job: JobResponse;
  media_asset_id: string;
  pothole_count: number;
  pothole_created: boolean;
  pothole?: PotholeResponse | null;
  message?: string | null;
}

export interface DetectVideoAcceptedResponse {
  jobId: string;
  status: DetectionJobStatus;
  mediaAssetId: string;
  pollUrl: string;
}

export interface JobResultSummary {
  totalFramesSampled: number;
  framesWithPotholes: number;
  potholesCreated: number;
  duplicatesDetected: number;
}

export interface JobDetailResponse {
  jobId: string;
  status: DetectionJobStatus;
  startedAt?: string | null;
  completedAt?: string | null;
  progress?: number | null;
  result?: JobResultSummary | null;
  error?: string | null;
}

export interface DashboardStatsResponse {
  totalPotholes: number;
  reportedCount: number;
  acknowledgedCount: number;
  inProgressCount: number;
  resolvedCount: number;
  highSeverityCount: number;
}

export interface PotholeMapMarkerResponse {
  id: string;
  latitude: number;
  longitude: number;
  severity_class: SeverityClass;
  severity_score: number;
  status: PotholeStatus;
  confidence: number;
  first_detected_at?: string;
  authority_name?: string | null;
  authority_code?: string | null;
  is_duplicate: boolean;
}

export interface ReportAttemptResponse {
  id: string;
  attempt_number: number;
  channel: string;
  idempotency_key: string;
  attempt_timestamp: string;
  status: string;
  response_summary?: string | null;
}

export interface ReportResponse {
  id: string;
  pothole_id: string;
  authority?: AuthorityResponse | null;
  status: 'PENDING' | 'DISPATCHED' | 'FAILED';
  idempotency_key: string;
  created_timestamp: string;
  external_reference?: string | null;
  attempts: ReportAttemptResponse[];
}

export interface PotholeEvidence {
  representativeImageUrl?: string | null;
  representativeKey?: string | null;
  rawMediaUrl?: string | null;
  rawKey?: string | null;
  mediaType?: string | null;
}

export interface PotholeDetailResponse {
  id: string;
  latitude: number;
  longitude: number;
  addressText?: string | null;
  firstDetectedAt?: string;
  severityScore: number;
  severityClass: SeverityClass;
  maxConfidence: number;
  status: PotholeStatus;
  isDuplicate: boolean;
  duplicateOfId?: string | null;
  rejectionReason?: RejectionReason | null;
  rejectionNote?: string | null;
  rejectedAt?: string | null;
  acceptedAt?: string | null;
  authority?: AuthorityResponse | null;
  authorityCode?: string | null;
  evidence?: PotholeEvidence | null;
  report?: ReportResponse | null;
  detections: DetectionResponse[];
  createdAt?: string;
  updatedAt?: string;
}

export interface PotholeStatusHistoryResponse {
  id: string;
  pothole_id: string;
  previous_status?: PotholeStatus | null;
  new_status: PotholeStatus;
  changed_at: string;
  changed_by?: string | null;
  notes?: string | null;
}

export interface UpdatePotholeStatusRequest {
  newStatus: PotholeStatus;
  changedBy?: string;
  notes?: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface PotholeFilterParams {
  status?: PotholeStatus | '';
  severity?: SeverityClass | '';
  authority?: string;
  fromDate?: string;
  toDate?: string;
  page?: number;
  size?: number;
  sort?: string;
}

export interface BoundingBoxParams {
  minLat: number;
  minLng: number;
  maxLat: number;
  maxLng: number;
  limit?: number;
}

export type Role = 'ROLE_USER' | 'ROLE_OFFICER';

export type VerificationStatus = 'PENDING_VERIFICATION' | 'VERIFIED' | 'REJECTED' | 'SUSPENDED';

export type NotificationType = 'STATUS_CHANGE' | 'REPORT_RECEIVED' | 'OFFICER_VERIFIED';

export interface User {
  id: number;
  email: string;
  fullName: string;
  phone?: string | null;
  role: Role;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  id: number;
  email: string;
  fullName: string;
  role: Role;
  verificationStatus?: VerificationStatus | null;
}

export interface OfficerProfileResponse {
  id: number;
  userId: number;
  email: string;
  fullName: string;
  phone?: string | null;
  department: string;
  officerIdCode: string;
  verificationStatus: VerificationStatus;
  jurisdictionName?: string | null;
  officeLatitude?: number | null;
  officeLongitude?: number | null;
  radiusKm?: number | null;
  verifiedAt?: string | null;
  verifiedBy?: string | null;
}

export interface NotificationResponse {
  id: number;
  potholeId?: string | null;
  title: string;
  message: string;
  readStatus: boolean;
  notificationType: NotificationType;
  createdAt: string;
}

export interface ApiErrorResponse {
  title?: string;
  status?: number;
  detail?: string;
  message?: string;
  errors?: string[];
}
