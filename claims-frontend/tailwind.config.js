/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        brand: {
          primary: '#0033A0',
          secondary: '#00A3E0',
        },
        surface: {
          bg: '#F4F7F9',
          panel: '#FFFFFF',
        },
        status: {
          critical: '#DC2626',
          warning: '#F59E0B',
          verified: '#16A34A',
        }
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
      },
      fontVariantNumeric: {
        tabular: 'lining-nums tabular-nums',
      },
      spacing: {
        '4': '4px',
        '8': '8px',
        '12': '12px',
      }
    },
  },
  plugins: [],
}
