import '@testing-library/jest-dom';
import { vi } from 'vitest';

if (typeof window !== 'undefined') {
  if (typeof window.URL.createObjectURL === 'undefined') {
    window.URL.createObjectURL = vi.fn(() => 'blob:mock-preview-url');
  }
  if (typeof window.URL.revokeObjectURL === 'undefined') {
    window.URL.revokeObjectURL = vi.fn();
  }
}
