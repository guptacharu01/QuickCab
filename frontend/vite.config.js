import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// Local dev server on :5173 (must match the backend's CORS setting).
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: { port: 5173 },
})
