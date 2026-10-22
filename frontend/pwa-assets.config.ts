import { defineConfig } from '@vite-pwa/assets-generator/config'

// O fundo do anel é o --ink do tokens.css. Vai nas bordas e no padding também,
// para o ícone não ter margem clara ao redor.
const FUNDO = '#17160f'

// Gera os ícones a partir de public/anel.svg com `npm run icones`. Os PNGs e o
// favicon.ico gerados ficam em public/ e são commitados.
export default defineConfig({
  preset: {
    transparent: {
      sizes: [64, 192, 512],
      padding: 0.1,
      resizeOptions: { background: FUNDO },
    },
    // Maskable: o sistema recorta em círculo ou squircle. O anel fica dentro
    // dos 80% centrais, que são a zona segura.
    maskable: {
      sizes: [512],
      padding: 0.2,
      resizeOptions: { background: FUNDO },
    },
    apple: {
      sizes: [180],
      padding: 0.1,
      resizeOptions: { background: FUNDO },
    },
  },
  images: ['public/anel.svg'],
})
