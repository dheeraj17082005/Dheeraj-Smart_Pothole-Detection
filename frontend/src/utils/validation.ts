export const SUPPORTED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp'];
export const SUPPORTED_IMAGE_EXTENSIONS = ['.jpg', '.jpeg', '.png', '.webp'];
export const MAX_IMAGE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB

export const SUPPORTED_VIDEO_TYPES = ['video/mp4', 'video/webm', 'video/quicktime'];
export const SUPPORTED_VIDEO_EXTENSIONS = ['.mp4', '.webm', '.mov'];
export const MAX_VIDEO_SIZE_BYTES = 200 * 1024 * 1024; // 200 MB

export interface ValidationResult {
  valid: boolean;
  error?: string;
}

export function validateImageFile(file: File): ValidationResult {
  if (!file) {
    return { valid: false, error: 'Please select an image file.' };
  }

  const fileNameLower = file.name.toLowerCase();
  const hasValidExt = SUPPORTED_IMAGE_EXTENSIONS.some((ext) => fileNameLower.endsWith(ext));
  const hasValidMime = SUPPORTED_IMAGE_TYPES.includes(file.type) || file.type === '';

  if (!hasValidExt && !hasValidMime) {
    return {
      valid: false,
      error: `Unsupported image format. Allowed formats: JPEG, PNG, WebP.`
    };
  }

  if (file.size > MAX_IMAGE_SIZE_BYTES) {
    return {
      valid: false,
      error: `Image file size exceeds maximum limit of 10 MB (Selected: ${(file.size / (1024 * 1024)).toFixed(1)} MB).`
    };
  }

  return { valid: true };
}

export function validateVideoFile(file: File): ValidationResult {
  if (!file) {
    return { valid: false, error: 'Please select a video file.' };
  }

  const fileNameLower = file.name.toLowerCase();
  const hasValidExt = SUPPORTED_VIDEO_EXTENSIONS.some((ext) => fileNameLower.endsWith(ext));
  const hasValidMime = SUPPORTED_VIDEO_TYPES.includes(file.type) || file.type === '';

  if (!hasValidExt && !hasValidMime) {
    return {
      valid: false,
      error: `Unsupported video format. Allowed formats: MP4, WebM, QuickTime (MOV).`
    };
  }

  if (file.size > MAX_VIDEO_SIZE_BYTES) {
    return {
      valid: false,
      error: `Video file size exceeds maximum limit of 200 MB (Selected: ${(file.size / (1024 * 1024)).toFixed(1)} MB).`
    };
  }

  return { valid: true };
}

export function validateCoordinates(lat: number | null | undefined, lon: number | null | undefined): ValidationResult {
  if (lat === null || lat === undefined || isNaN(lat)) {
    return { valid: false, error: 'Latitude is required.' };
  }
  if (lon === null || lon === undefined || isNaN(lon)) {
    return { valid: false, error: 'Longitude is required.' };
  }
  if (lat < -90 || lat > 90) {
    return { valid: false, error: 'Latitude must be between -90 and 90 degrees.' };
  }
  if (lon < -180 || lon > 180) {
    return { valid: false, error: 'Longitude must be between -180 and 180 degrees.' };
  }

  return { valid: true };
}
