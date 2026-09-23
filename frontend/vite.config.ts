import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  base: '/ui/', // Указываем базовый путь для генерации ассетов
  server: {
    port: 5173,
    proxy: {
      // Проксируем API запросы локального dev-сервера фронтенда на Spring Boot
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      }
    }
  }
})
