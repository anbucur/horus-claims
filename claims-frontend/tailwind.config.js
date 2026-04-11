/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // Google Material You — Dark theme tokens
        md: {
          bg:                 '#111319',
          surface:            '#111319',
          'container-lowest': '#0c0e14',
          'container-low':    '#191b22',
          container:          '#1e1f26',
          'container-high':   '#282a30',
          'container-highest':'#33343b',
          bright:             '#373940',

          primary:            '#c3c0ff',
          'primary-container':'#4b4dd8',
          'on-primary':       '#1d00a5',

          secondary:          '#c3c5dc',
          'secondary-container':'#434659',

          tertiary:           '#c0c1ff',
          'tertiary-container':'#4b4dd8',

          'on-surface':       '#e2e2eb',
          'on-surface-var':   '#c7c4d8',
          outline:            '#918fa1',
          'outline-var':      '#464555',

          error:              '#ffb4ab',
          'error-container':  '#93000a',
        },
        // Status — Google brand colors adapted for dark
        status: {
          critical: '#ff6b6b',
          warning:  '#fbbf24',
          verified: '#4ade80',
          info:     '#60a5fa',
        },
        // Keep legacy brand for any remaining refs
        brand: {
          primary:   '#c3c0ff',
          secondary: '#a5b4fc',
        },
        surface: {
          bg:    '#111319',
          panel: '#1e1f26',
        },
      },
      fontFamily: {
        sans:    ['Inter', 'system-ui', 'sans-serif'],
        display: ['Manrope', 'Inter', 'system-ui', 'sans-serif'],
        mono:    ['JetBrains Mono', 'Fira Code', 'monospace'],
      },
      fontVariantNumeric: {
        tabular: 'lining-nums tabular-nums',
      },
      boxShadow: {
        'md-1': '0 1px 2px rgba(0,0,0,0.3), 0 1px 3px rgba(0,0,0,0.15)',
        'md-2': '0 2px 6px rgba(0,0,0,0.35), 0 1px 4px rgba(0,0,0,0.2)',
        'md-3': '0 4px 16px rgba(0,0,0,0.4), 0 2px 6px rgba(0,0,0,0.25)',
        'glow-primary': '0 0 12px rgba(195,192,255,0.15)',
        'glow-success': '0 0 12px rgba(74,222,128,0.15)',
        'glow-warning': '0 0 12px rgba(251,191,36,0.15)',
        'glow-error':   '0 0 12px rgba(255,107,107,0.15)',
      },
    },
  },
  plugins: [],
}
