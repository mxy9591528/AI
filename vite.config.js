import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

// https://vitejs.dev/config/
export default defineConfig({
    plugins: [vue()],
    resolve: {
        alias: {
            '@': path.resolve(__dirname, 'src')
        }
    },
    build: {
        chunkSizeWarningLimit: 600,
        rollupOptions: {
            output: {
                // 大体积第三方库拆分为独立 chunk，提升缓存命中与首屏加载
                manualChunks: {
                    'vendor-element-plus': ['element-plus', '@element-plus/icons-vue'],
                    'vendor-echarts': ['echarts'],
                    'vendor-editor': ['@wangeditor/editor', '@wangeditor/editor-for-vue'],
                    'vendor-vue': ['vue', 'vue-router', 'pinia', 'axios']
                }
            }
        }
    },
    server: {
        port: 5173,
        proxy: {
            '/api': {
                target: 'http://localhost:1236',
                changeOrigin: true
            },
            '/upload': {
                target: 'http://localhost:1236',
                changeOrigin: true
            }
        }
    }
})
