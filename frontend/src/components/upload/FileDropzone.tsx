import React, { useState, useRef, useEffect } from 'react';
import { formatBytes } from '../../utils/formatters';
import {
  validateImageFile,
  validateVideoFile,
  SUPPORTED_IMAGE_EXTENSIONS,
  SUPPORTED_VIDEO_EXTENSIONS
} from '../../utils/validation';

export interface FileDropzoneProps {
  mode: 'IMAGE' | 'VIDEO';
  file: File | null;
  onFileSelect: (file: File | null) => void;
  onError: (error: string | null) => void;
  disabled?: boolean;
}

export const FileDropzone: React.FC<FileDropzoneProps> = ({
  mode,
  file,
  onFileSelect,
  onError,
  disabled = false
}) => {
  const [isDragOver, setIsDragOver] = useState(false);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const acceptedExtensions =
    mode === 'IMAGE'
      ? SUPPORTED_IMAGE_EXTENSIONS.join(',')
      : SUPPORTED_VIDEO_EXTENSIONS.join(',');

  useEffect(() => {
    if (file && mode === 'IMAGE' && typeof URL.createObjectURL === 'function') {
      const url = URL.createObjectURL(file);
      setPreviewUrl(url);
      return () => {
        if (typeof URL.revokeObjectURL === 'function') {
          URL.revokeObjectURL(url);
        }
      };
    } else {
      setPreviewUrl(null);
    }
  }, [file, mode]);

  const processFile = (selectedFile: File) => {
    const validation =
      mode === 'IMAGE'
        ? validateImageFile(selectedFile)
        : validateVideoFile(selectedFile);

    if (!validation.valid) {
      onError(validation.error || 'Invalid file');
      onFileSelect(null);
      return;
    }

    onError(null);
    onFileSelect(selectedFile);
  };

  const handleDrop = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    setIsDragOver(false);
    if (disabled) return;

    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      processFile(e.dataTransfer.files[0]);
    }
  };

  const handleDragOver = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    if (!disabled) setIsDragOver(true);
  };

  const handleDragLeave = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    setIsDragOver(false);
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      processFile(e.target.files[0]);
    }
  };

  const clearFile = (e: React.MouseEvent) => {
    e.stopPropagation();
    onFileSelect(null);
    onError(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  return (
    <div className="form-group">
      <label className="form-label">
        {mode === 'IMAGE' ? 'Road-Facing Image' : 'Road Dashcam Video Stream'} *
      </label>

      <div
        onClick={() => !disabled && fileInputRef.current?.click()}
        onDrop={handleDrop}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        style={{
          border: `2px dashed ${isDragOver ? 'var(--brand-accent)' : file ? 'var(--brand-accent-border)' : 'var(--border)'}`,
          borderRadius: '12px',
          padding: file ? '1.5rem' : '2.75rem 1.5rem',
          textAlign: 'center',
          backgroundColor: isDragOver ? 'var(--brand-accent-subtle)' : file ? 'var(--surface-elevated)' : 'var(--surface)',
          cursor: disabled ? 'not-allowed' : 'pointer',
          transition: 'all 0.15s ease',
          position: 'relative'
        }}
        data-testid="file-dropzone"
      >
        <input
          ref={fileInputRef}
          type="file"
          accept={acceptedExtensions}
          onChange={handleInputChange}
          style={{ display: 'none' }}
          disabled={disabled}
          data-testid="file-input"
        />

        {file ? (
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '0.85rem' }}>
            {previewUrl && (
              <div
                style={{
                  position: 'relative',
                  borderRadius: '8px',
                  overflow: 'hidden',
                  border: '1px solid var(--border)',
                  boxShadow: 'var(--shadow-md)',
                  maxHeight: '190px'
                }}
              >
                <img
                  src={previewUrl}
                  alt="Selected road frame preview"
                  style={{
                    maxHeight: '190px',
                    maxWidth: '100%',
                    display: 'block',
                    objectFit: 'cover'
                  }}
                />
              </div>
            )}

            <div
              style={{
                backgroundColor: 'var(--surface)',
                border: '1px solid var(--border)',
                borderRadius: '8px',
                padding: '0.65rem 1rem',
                display: 'inline-flex',
                alignItems: 'center',
                gap: '0.75rem',
                boxShadow: 'var(--shadow-sm)'
              }}
            >
              <span style={{ fontSize: '1.25rem' }}>{mode === 'IMAGE' ? '📷' : '🎥'}</span>
              <div style={{ textAlign: 'left' }}>
                <div style={{ fontWeight: 700, fontSize: '0.875rem', color: 'var(--text-primary)', wordBreak: 'break-all' }}>
                  {file.name}
                </div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                  {formatBytes(file.size)} &bull; {file.type || (mode === 'IMAGE' ? 'image/jpeg' : 'video/mp4')}
                </div>
              </div>
            </div>

            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={clearFile}
              disabled={disabled}
            >
              Remove &amp; Choose Another File
            </button>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '0.75rem' }}>
            <div
              style={{
                width: '56px',
                height: '56px',
                borderRadius: '50%',
                backgroundColor: 'var(--surface-elevated)',
                border: '1px solid var(--border)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: 'var(--brand-accent)'
              }}
            >
              <svg
                width="26"
                height="26"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                <polyline points="17 8 12 3 7 8" />
                <line x1="12" y1="3" x2="12" y2="15" />
              </svg>
            </div>

            <div>
              <div style={{ fontWeight: 700, fontSize: '1.05rem', color: 'var(--text-primary)' }}>
                Drag &amp; drop {mode === 'IMAGE' ? 'road photo' : 'dashcam footage'} here, or{' '}
                <span style={{ color: 'var(--brand-accent)', textDecoration: 'underline' }}>browse</span>
              </div>
              <div className="form-help" style={{ marginTop: '0.35rem' }}>
                {mode === 'IMAGE'
                  ? 'Supported formats: JPEG, PNG, WebP (Max 10 MB)'
                  : 'Supported formats: MP4, WebM, MOV (Max 200 MB)'}
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
