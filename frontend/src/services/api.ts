import {
  DetectImageResponse,
  DetectVideoAcceptedResponse,
  JobDetailResponse,
  ApiErrorResponse,
  DashboardStatsResponse,
  PotholeMapMarkerResponse,
  PotholeResponse,
  PotholeDetailResponse,
  PotholeStatusHistoryResponse,
  UpdatePotholeStatusRequest,
  PageResponse,
  PotholeFilterParams,
  BoundingBoxParams,
  AuthorityResponse,
  AuthResponse,
  User,
  OfficerProfileResponse,
  NotificationResponse
} from '../types';

export const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || '/api/v1';

export class ApiError extends Error {
  public status?: number;
  public details?: ApiErrorResponse;

  constructor(message: string, status?: number, details?: ApiErrorResponse) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.details = details;
  }
}

async function handleResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    let errorData: ApiErrorResponse | undefined;
    let message = `Request failed with status ${response.status}`;

    try {
      const contentType = response.headers.get('content-type');
      if (contentType && contentType.includes('application/json')) {
        errorData = await response.json();
        if (errorData?.detail) {
          message = errorData.detail;
        } else if (errorData?.message) {
          message = errorData.message;
        } else if (errorData?.title) {
          message = errorData.title;
        } else if (Array.isArray(errorData?.errors) && errorData.errors.length > 0) {
          message = errorData.errors.join(', ');
        }
      } else {
        const text = await response.text();
        if (text) message = text;
      }
    } catch {
      // Keep default message if parsing fails
    }

    throw new ApiError(message, response.status, errorData);
  }

  return response.json() as Promise<T>;
}

export interface DetectImageParams {
  file: File;
  latitude: number;
  longitude: number;
  capturedAt?: string;
  addressText?: string;
  confidenceThreshold?: number;
}

export interface DetectVideoParams {
  file: File;
  latitude: number;
  longitude: number;
  capturedAt?: string;
  addressText?: string;
}

function getAuthHeaders(headers: Record<string, string> = {}): Record<string, string> {
  const token = sessionStorage.getItem('authToken') || localStorage.getItem('authToken');
  if (token) {
    return {
      ...headers,
      Authorization: `Bearer ${token}`
    };
  }
  return headers;
}

export const apiClient = {
  /**
   * Auth endpoints
   */
  async registerUser(data: { email: string; password: string; fullName: string; phone?: string }): Promise<AuthResponse> {
    const response = await fetch(`${API_BASE_URL}/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
      body: JSON.stringify(data)
    });
    return await handleResponse<AuthResponse>(response);
  },

  async registerOfficer(formData: FormData): Promise<AuthResponse> {
    const response = await fetch(`${API_BASE_URL}/auth/officer/register`, {
      method: 'POST',
      body: formData
    });
    return await handleResponse<AuthResponse>(response);
  },

  async login(data: { email: string; password: string }): Promise<AuthResponse> {
    const response = await fetch(`${API_BASE_URL}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
      body: JSON.stringify(data)
    });
    return await handleResponse<AuthResponse>(response);
  },

  async getCurrentUser(): Promise<User> {
    const response = await fetch(`${API_BASE_URL}/auth/me`, {
      method: 'GET',
      headers: getAuthHeaders({ Accept: 'application/json' })
    });
    return await handleResponse<User>(response);
  },

  /**
   * Officer endpoints
   */
  async getOfficerProfile(): Promise<OfficerProfileResponse> {
    const response = await fetch(`${API_BASE_URL}/officer/profile`, {
      method: 'GET',
      headers: getAuthHeaders({ Accept: 'application/json' })
    });
    return await handleResponse<OfficerProfileResponse>(response);
  },

  async getOfficerReports(): Promise<PotholeResponse[]> {
    const response = await fetch(`${API_BASE_URL}/officer/reports`, {
      method: 'GET',
      headers: getAuthHeaders({ Accept: 'application/json' })
    });
    return await handleResponse<PotholeResponse[]>(response);
  },

  async updateOfficerReportStatus(id: string, payload: UpdatePotholeStatusRequest): Promise<PotholeResponse> {
    const response = await fetch(`${API_BASE_URL}/officer/reports/${id}/status`, {
      method: 'PATCH',
      headers: getAuthHeaders({ 'Content-Type': 'application/json', Accept: 'application/json' }),
      body: JSON.stringify(payload)
    });
    return await handleResponse<PotholeResponse>(response);
  },

  async acceptReport(id: string, notes?: string): Promise<PotholeResponse> {
    const query = notes ? `?notes=${encodeURIComponent(notes)}` : '';
    const response = await fetch(`${API_BASE_URL}/officer/reports/${id}/accept${query}`, {
      method: 'POST',
      headers: getAuthHeaders({ Accept: 'application/json' })
    });
    return await handleResponse<PotholeResponse>(response);
  },

  async rejectReport(id: string, payload: { reason: string; notes?: string }): Promise<PotholeResponse> {
    const response = await fetch(`${API_BASE_URL}/officer/reports/${id}/reject`, {
      method: 'POST',
      headers: getAuthHeaders({ 'Content-Type': 'application/json', Accept: 'application/json' }),
      body: JSON.stringify(payload)
    });
    return await handleResponse<PotholeResponse>(response);
  },

  /**
   * Notification endpoints
   */
  async getNotifications(): Promise<PageResponse<NotificationResponse>> {
    const response = await fetch(`${API_BASE_URL}/notifications`, {
      method: 'GET',
      headers: getAuthHeaders({ Accept: 'application/json' })
    });
    return await handleResponse<PageResponse<NotificationResponse>>(response);
  },

  async getUnreadNotificationCount(): Promise<{ unreadCount: number }> {
    const response = await fetch(`${API_BASE_URL}/notifications/unread-count`, {
      method: 'GET',
      headers: getAuthHeaders({ Accept: 'application/json' })
    });
    return await handleResponse<{ unreadCount: number }>(response);
  },

  async markNotificationAsRead(id: number): Promise<void> {
    const response = await fetch(`${API_BASE_URL}/notifications/${id}/read`, {
      method: 'PATCH',
      headers: getAuthHeaders()
    });
    if (!response.ok) {
      throw new ApiError(`Failed to mark notification ${id} as read`, response.status);
    }
  },

  /**
   * Submit an image for immediate synchronous detection and spatial aggregation.
   */
  async detectImage(params: DetectImageParams): Promise<DetectImageResponse> {
    const formData = new FormData();
    formData.append('file', params.file);
    formData.append('latitude', params.latitude.toString());
    formData.append('longitude', params.longitude.toString());

    if (params.capturedAt) {
      formData.append('capturedAt', params.capturedAt);
    }
    if (params.addressText) {
      formData.append('addressText', params.addressText);
    }
    if (params.confidenceThreshold !== undefined && params.confidenceThreshold !== null) {
      formData.append('confidenceThreshold', params.confidenceThreshold.toString());
    }

    try {
      const response = await fetch(`${API_BASE_URL}/potholes/detect-image`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: formData
      });
      return await handleResponse<DetectImageResponse>(response);
    } catch (err: any) {
      if (err instanceof ApiError) throw err;
      throw new ApiError(
        err.message || 'Unable to connect to the pothole detection service.',
        0
      );
    }
  },

  /**
   * Submit a video stream for asynchronous frame sampling and Type A aggregation.
   */
  async detectVideo(params: DetectVideoParams): Promise<DetectVideoAcceptedResponse> {
    const formData = new FormData();
    formData.append('file', params.file);
    formData.append('latitude', params.latitude.toString());
    formData.append('longitude', params.longitude.toString());

    if (params.capturedAt) {
      formData.append('capturedAt', params.capturedAt);
    }
    if (params.addressText) {
      formData.append('addressText', params.addressText);
    }

    try {
      const response = await fetch(`${API_BASE_URL}/potholes/detect-video`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: formData
      });
      return await handleResponse<DetectVideoAcceptedResponse>(response);
    } catch (err: any) {
      if (err instanceof ApiError) throw err;
      throw new ApiError(
        err.message || 'Unable to connect to the video detection service.',
        0
      );
    }
  },

  /**
   * Query the status, progress, and result summary of an asynchronous detection job.
   */
  async getDetectionJob(jobId: string): Promise<JobDetailResponse> {
    try {
      const response = await fetch(`${API_BASE_URL}/detection-jobs/${jobId}`, {
        method: 'GET',
        headers: getAuthHeaders({ Accept: 'application/json' })
      });
      return await handleResponse<JobDetailResponse>(response);
    } catch (err: any) {
      if (err instanceof ApiError) throw err;
      throw new ApiError(
        err.message || 'Unable to fetch detection job status.',
        0
      );
    }
  },

  /**
   * Fetch aggregate operational statistics for top dashboard counter cards.
   */
  async getDashboardStats(): Promise<DashboardStatsResponse> {
    try {
      const response = await fetch(`${API_BASE_URL}/dashboard/stats`, {
        method: 'GET',
        headers: getAuthHeaders({ Accept: 'application/json' })
      });
      return await handleResponse<DashboardStatsResponse>(response);
    } catch (err: any) {
      if (err instanceof ApiError) throw err;
      throw new ApiError(
        err.message || 'Unable to load dashboard statistics.',
        0
      );
    }
  },

  /**
   * Query map markers within a specific geographic bounding box viewport.
   */
  async getPotholesMap(params: BoundingBoxParams, signal?: AbortSignal): Promise<PotholeMapMarkerResponse[]> {
    const search = new URLSearchParams({
      minLat: params.minLat.toString(),
      minLng: params.minLng.toString(),
      maxLat: params.maxLat.toString(),
      maxLng: params.maxLng.toString(),
      limit: (params.limit || 200).toString()
    });

    try {
      const response = await fetch(`${API_BASE_URL}/potholes/map?${search.toString()}`, {
        method: 'GET',
        headers: getAuthHeaders({ Accept: 'application/json' }),
        signal
      });
      return await handleResponse<PotholeMapMarkerResponse[]>(response);
    } catch (err: any) {
      if (err.name === 'AbortError') throw err;
      if (err instanceof ApiError) throw err;
      throw new ApiError(
        err.message || 'Unable to fetch map markers.',
        0
      );
    }
  },

  /**
   * Retrieve filtered, paginated list of registered potholes.
   */
  async getPotholes(params: PotholeFilterParams): Promise<PageResponse<PotholeResponse>> {
    const search = new URLSearchParams();
    if (params.status) search.append('status', params.status);
    if (params.severity) search.append('severity', params.severity);
    if (params.authority) search.append('authority', params.authority);
    if (params.fromDate) search.append('fromDate', params.fromDate);
    if (params.toDate) search.append('toDate', params.toDate);
    search.append('page', (params.page ?? 0).toString());
    search.append('size', (params.size ?? 10).toString());
    if (params.sort) search.append('sort', params.sort);

    try {
      const response = await fetch(`${API_BASE_URL}/potholes?${search.toString()}`, {
        method: 'GET',
        headers: getAuthHeaders({ Accept: 'application/json' })
      });
      return await handleResponse<PageResponse<PotholeResponse>>(response);
    } catch (err: any) {
      if (err instanceof ApiError) throw err;
      throw new ApiError(
        err.message || 'Unable to load pothole list.',
        0
      );
    }
  },

  /**
   * Retrieve detailed information, evidence URLs, and report status for a single pothole.
   */
  async getPothole(id: string): Promise<PotholeDetailResponse> {
    try {
      const response = await fetch(`${API_BASE_URL}/potholes/${id}`, {
        method: 'GET',
        headers: getAuthHeaders({ Accept: 'application/json' })
      });
      return await handleResponse<PotholeDetailResponse>(response);
    } catch (err: any) {
      if (err instanceof ApiError) throw err;
      throw new ApiError(
        err.message || `Unable to load pothole detail for ${id}.`,
        0
      );
    }
  },

  /**
   * Retrieve status transition history timeline for a single pothole.
   */
  async getPotholeHistory(id: string): Promise<PotholeStatusHistoryResponse[]> {
    try {
      const response = await fetch(`${API_BASE_URL}/potholes/${id}/history`, {
        method: 'GET',
        headers: getAuthHeaders({ Accept: 'application/json' })
      });
      return await handleResponse<PotholeStatusHistoryResponse[]>(response);
    } catch (err: any) {
      if (err instanceof ApiError) throw err;
      throw new ApiError(
        err.message || `Unable to load status history for ${id}.`,
        0
      );
    }
  },

  /**
   * Update the remediation status of a pothole (enforces state transition rules).
   */
  async updatePotholeStatus(id: string, payload: UpdatePotholeStatusRequest): Promise<PotholeDetailResponse> {
    try {
      const response = await fetch(`${API_BASE_URL}/potholes/${id}/status`, {
        method: 'PATCH',
        headers: getAuthHeaders({
          'Content-Type': 'application/json',
          Accept: 'application/json'
        }),
        body: JSON.stringify(payload)
      });
      return await handleResponse<PotholeDetailResponse>(response);
    } catch (err: any) {
      if (err instanceof ApiError) throw err;
      throw new ApiError(
        err.message || `Unable to update status for pothole ${id}.`,
        0
      );
    }
  },

  /**
   * Retrieve all civic authorities for filter dropdown selection.
   */
  async getAuthorities(): Promise<AuthorityResponse[]> {
    try {
      const response = await fetch(`${API_BASE_URL}/authorities`, {
        method: 'GET',
        headers: getAuthHeaders({ Accept: 'application/json' })
      });
      return await handleResponse<AuthorityResponse[]>(response);
    } catch (err: any) {
      if (err instanceof ApiError) throw err;
      throw new ApiError(
        err.message || 'Unable to load civic authorities.',
        0
      );
    }
  }
};
