import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue(),
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src')
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:9090',
        changeOrigin: true
      },
      '/ws': {
        target: 'ws://localhost:9090',
        ws: true
      }
    }
  },
  build: {
    // CSS 代码分割（按路由拆分 CSS chunk）
    cssCodeSplit: true,
    // 小于 10KB 的静态资源直接 base64 内联
    assetsInlineLimit: 10240,
    rollupOptions: {
      output: {
        // 将不常变更的依赖拆分为独立 chunk，利用浏览器长期缓存
        manualChunks: {
          'vendor-vue': ['vue', 'vue-router', 'pinia'],
          'vendor-http': ['axios'],
        }
      }
    }
  }
})

