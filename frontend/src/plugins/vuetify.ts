import { createVuetify } from 'vuetify'

// Light mode: pastel bg + dark text
const lightSeverityColors = {
  'severity-low':           '#dcfce7',
  'on-severity-low':        '#14532d',
  'severity-medium':        '#fef3c7',
  'on-severity-medium':     '#78350f',
  'severity-high':          '#fee2e2',
  'on-severity-high':       '#7f1d1d',
  'severity-critical':      '#fecaca',
  'on-severity-critical':   '#450a0a',
}

// Dark mode: rich bg + light text (inverted pattern)
const darkSeverityColors = {
  'severity-low':           '#14532d',
  'on-severity-low':        '#bbf7d0',
  'severity-medium':        '#78350f',
  'on-severity-medium':     '#fde68a',
  'severity-high':          '#7f1d1d',
  'on-severity-high':       '#fecaca',
  'severity-critical':      '#450a0a',
  'on-severity-critical':   '#fca5a5',
}

export const vuetify = createVuetify({
  theme: {
    defaultTheme: 'light',
    themes: {
      light: {
        dark: false,
        colors: {
          primary: '#3f51b5',
          secondary: '#6b7280',
          error: '#dc2626',
          success: '#16a34a',
          info: '#2563eb',
          warning: '#d97706',
          background: '#f8fafc',
          surface: '#ffffff',
          'surface-light': '#f3f4f6',
          'surface-variant': '#f9fafb',
          'on-background': '#111827',
          'on-surface': '#111827',
          'on-surface-variant': '#374151',
          outline: '#e5e7eb',
          avatar: '#3b82f6',
          'nav-action': '#010107',
          leave: '#555555',
          'status-in-progress': '#d97706',
          'status-patched': '#16a34a',
          'status-verified': '#7c3aed',
          ...lightSeverityColors,
          nav: '#e7e7e7',
        },
      },
      dark: {
        dark: true,
        colors: {
          primary: '#7986cb',
          secondary: '#9ca3af',
          error: '#f87171',
          success: '#4ade80',
          info: '#60a5fa',
          warning: '#fbbf24',
          background: '#0b1220',
          grey: '#334155',
          'on-grey': '#e5e7eb',
          surface: '#1e293b',
          'surface-light': '#172032',
          'surface-variant': '#172032',
          'on-background': '#e5e7eb',
          'on-surface': '#e5e7eb',
          'on-surface-variant': '#9ca3af',
          outline: '#334155',
          avatar: '#60a5fa',
          'nav-action': '#a2a7c1',
          leave: '#9ca3af',
          'status-in-progress': '#fbbf24',
          'status-patched': '#4ade80',
          'status-verified': '#a78bfa',
          ...darkSeverityColors,
          nav: '#1f2933',
        },
      },
    },
  },
})
