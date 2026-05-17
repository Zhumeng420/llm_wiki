import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'node:path'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        // 主动反推等长耗时接口可能超过 10 分钟，放宽到 30 分钟，避免被代理切断
        timeout: 30 * 60 * 1000,
        proxyTimeout: 30 * 60 * 1000
      }
    }
  }
})
