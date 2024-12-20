import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/authenticate': {
        target: 'http://localhost:8081/',
      },
      '/info': {
        target: 'http://localhost:8081/',
      },
      '/api': {
        target: 'http://localhost:8081/',
      },
      '/documentation': {
        target: 'http://localhost:8081/',
      },
      '/tools': {
        target: 'http://localhost:8081/',
      },
      '^/legacy-ui/.*': {
        target: 'http://localhost:8081/',
      }
    }
  }
})
