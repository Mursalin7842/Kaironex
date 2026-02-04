import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  base: './', // CRITICAL for Android WebView file:/// loading
  plugins: [react()],
  build: {
    target: 'es2015'
  },
  define: {
    'process.env.VITE_API_KEY': JSON.stringify(process.env.VITE_API_KEY)
  }
})
