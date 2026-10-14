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
        bg: 'var(--bg)',
        background: 'var(--bg)',
        text: {
          DEFAULT: 'var(--text)',
          muted: 'var(--text-muted)',
          'on-tint': 'var(--text-on-tint)',
        },
        foreground: 'var(--text)',
        'text-muted': 'var(--text-muted)',
        'text-on-tint': 'var(--text-on-tint)',
        surface: {
          DEFAULT: 'var(--surface)',
          2: 'var(--surface-2)',
          tint: 'var(--surface-tint)',
          hover: 'var(--surface-2)',
        },
        'surface-2': 'var(--surface-2)',
        'surface-tint': 'var(--surface-tint)',
        'surface-hover': 'var(--surface-2)',
        border: {
          DEFAULT: 'var(--border)',
          strong: 'var(--border-strong)',
        },
        'border-strong': 'var(--border-strong)',
        track: 'var(--track)',
        accent: {
          DEFAULT: 'var(--accent)',
          text: 'var(--accent-text)',
        },
        'accent-text': 'var(--accent-text)',
        'on-accent': 'var(--on-accent)',
        inverse: 'var(--inverse)',
        brand: {
          DEFAULT: 'var(--brand)',
          hover: 'var(--brand-hover)',
          text: 'var(--brand-text)',
        },
        status: {
          active: 'var(--status-active)',
          warning: 'var(--status-warning)',
          error: 'var(--status-error)',
          disabled: 'var(--text-muted)',
        }
      },
      boxShadow: {
        'sm-subtle': 'none',
        'subtle': 'none',
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
