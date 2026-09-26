/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class', 
  theme: {
    fontFamily: {
      sans: ['Inter', 'SF Pro Display', 'system-ui', 'sans-serif'],
      mono: ['JetBrains Mono', 'Fira Code', 'Menlo', 'monospace'],
    },
    extend: {
      colors: {
        background: 'var(--background)',
        foreground: 'var(--foreground)',
        surface: 'var(--surface)',
        'surface-hover': 'var(--surface-hover)',
        border: 'var(--border)',
        'border-strong': 'var(--border-strong)',
        brand: {
          DEFAULT: 'var(--brand)',
          hover: 'var(--brand-hover)',
          text: 'var(--brand-text)',
        },
        status: {
          active: 'var(--status-active)',
          warning: 'var(--status-warning)',
          error: 'var(--status-error)',
          disabled: 'var(--status-disabled)',
        }
      },
      boxShadow: {
        'sm-subtle': '0 1px 2px 0 rgba(0, 0, 0, 0.05)',
        'subtle': '0 4px 12px rgba(0, 0, 0, 0.03), 0 1px 3px rgba(0,0,0,0.04)',
      },
      transitionDuration: {
        DEFAULT: '150ms',
      },
      borderRadius: {
        sm: '6px',
        md: '8px',
        lg: '12px',
      }
    },
  },
  plugins: [],
}
