import React from 'react';
import { SeverityClass, DetectionJobStatus, PotholeStatus } from '../../types';

interface SeverityBadgeProps {
  severity: SeverityClass;
  score?: number;
}

export const SeverityBadge: React.FC<SeverityBadgeProps> = ({ severity, score }) => {
  let icon = '●';
  let badgeClass = 'badge badge-low';
  let label = 'Low Severity';

  if (severity === 'HIGH') {
    icon = '▲';
    badgeClass = 'badge badge-high';
    label = 'High Severity';
  } else if (severity === 'MEDIUM') {
    icon = '■';
    badgeClass = 'badge badge-medium';
    label = 'Medium Severity';
  }

  return (
    <span className={badgeClass} title={`Severity: ${label}`}>
      <span aria-hidden="true" style={{ fontSize: '0.65rem' }}>{icon} </span>
      <span>{severity}{score !== undefined ? ` (${score.toFixed(1)})` : ''}</span>
    </span>
  );
};

interface JobStatusBadgeProps {
  status: DetectionJobStatus;
}

export const JobStatusBadge: React.FC<JobStatusBadgeProps> = ({ status }) => {
  let bg = '#f1f5f9';
  let color = '#475569';
  let border = '#cbd5e1';

  if (status === 'COMPLETED') {
    bg = '#ecfdf5';
    color = '#065f46';
    border = '#a7f3d0';
  } else if (status === 'PROCESSING') {
    bg = '#eff6ff';
    color = '#1d4ed8';
    border = '#bfdbfe';
  } else if (status === 'FAILED') {
    bg = '#fef2f2';
    color = '#991b1b';
    border = '#fecaca';
  }

  return (
    <span
      className="badge"
      style={{
        backgroundColor: bg,
        color,
        border: `1px solid ${border}`
      }}
    >
      {status}
    </span>
  );
};

interface PotholeStatusBadgeProps {
  status: PotholeStatus;
}

export const PotholeStatusBadge: React.FC<PotholeStatusBadgeProps> = ({ status }) => {
  let bg = '#fee2e2';
  let color = '#991b1b';
  let border = '#fca5a5';
  let icon = '🚨';

  switch (status) {
    case 'REPORTED':
      bg = '#fee2e2';
      color = '#991b1b';
      border = '#fca5a5';
      icon = '🚨';
      break;
    case 'ACKNOWLEDGED':
      bg = '#fef3c7';
      color = '#92400e';
      border = '#fde68a';
      icon = '📋';
      break;
    case 'IN_PROGRESS':
      bg = '#e0e7ff';
      color = '#3730a3';
      border = '#c7d2fe';
      icon = '🛠️';
      break;
    case 'RESOLVED':
      bg = '#dcfce7';
      color = '#166534';
      border = '#bbf7d0';
      icon = '✓';
      break;
  }

  return (
    <span
      className="badge"
      style={{
        backgroundColor: bg,
        color,
        border: `1px solid ${border}`,
        padding: '0.2rem 0.55rem',
        fontSize: '0.72rem',
        letterSpacing: '0.03em'
      }}
    >
      <span aria-hidden="true" style={{ fontSize: '0.65rem' }}>{icon} </span>
      <span>{status.replace('_', ' ')}</span>
    </span>
  );
};

interface DuplicateBadgeProps {
  isDuplicate: boolean;
  duplicateOfId?: string | null;
}

export const DuplicateBadge: React.FC<DuplicateBadgeProps> = ({ isDuplicate, duplicateOfId }) => {
  if (isDuplicate) {
    return (
      <span
        className="badge"
        style={{ background: '#fffbeb', color: '#b45309', border: '1px solid #fde68a' }}
        title={duplicateOfId ? `Duplicate of #${duplicateOfId.slice(0, 8)}` : 'Duplicate defect report'}
      >
        <span aria-hidden="true">🔁 </span>DUPLICATE
      </span>
    );
  }
  return (
    <span
      className="badge"
      style={{ background: '#ecfdf5', color: '#047857', border: '1px solid #a7f3d0' }}
    >
      <span aria-hidden="true">✦ </span>UNIQUE DEFECT
    </span>
  );
};
