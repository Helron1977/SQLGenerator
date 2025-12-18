import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  // Configuration pour l'intégration dans un projet parent
  build: {
    // Génère un bundle qui peut être importé comme module
    lib: {
      entry: './src/SqlGeneratorApp.tsx',
      name: 'SqlGeneratorApp',
      fileName: 'sql-generator-app',
      formats: ['es', 'umd']
    },
    rollupOptions: {
      // Externaliser React pour éviter la duplication
      external: ['react', 'react-dom'],
      output: {
        globals: {
          react: 'React',
          'react-dom': 'ReactDOM'
        }
      }
    }
  },
  // Proxy pour le développement (optionnel)
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})

