/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // Здесь мы определим палитру в стиле Cloudflare/Vercel (воздушная, контрастная)
        background: '#ffffff',
        foreground: '#171717',
        primary: {
          DEFAULT: '#000000',
          foreground: '#ffffff',
        },
        muted: {
          DEFAULT: '#fafafa',
          foreground: '#737373',
        },
        border: '#e5e5e5'
      },
      boxShadow: {
        'soft': '0 4px 14px 0 rgba(0, 0, 0, 0.05)',
      }
    },
  },
  plugins: [],
}
