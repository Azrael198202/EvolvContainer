import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173, // 可自调
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        // 如果后端实际路径前缀是 /api，则不需要 rewrite；若后端没有 /api 前缀可启用：
        //rewrite: (path) => path.replace(/^\/api/, '')
      },
    },
  },
})
