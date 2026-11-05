import { defineConfig } from 'vite';

export default defineConfig({
  // Caminhos relativos: o build em /dist funciona em qualquer pasta do servidor.
  base: './',
  server: {
    port: 5173,
    open: true,
  },
});
