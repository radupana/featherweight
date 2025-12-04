// @ts-check
import { defineConfig } from 'astro/config';
import tailwindcss from '@tailwindcss/vite';

export default defineConfig({
  site: 'https://featherweight.app',
  output: 'static',
  build: {
    assets: '_assets'
  },
  vite: {
    plugins: [tailwindcss()]
  }
});
