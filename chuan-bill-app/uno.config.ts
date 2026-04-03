import { presetUni } from '@uni-helper/unocss-preset-uni'

import {
  defineConfig,
  presetIcons,
  transformerDirectives,
  transformerVariantGroup,
} from 'unocss'
import { safeIconList } from './safelist'

export default defineConfig({
  presets: [
    presetUni({
      attributify: false,
    }),
    presetIcons({
      warn: true,
      cdn: 'https://esm.sh',
      // HBuilderX 必须针对要使用的 Collections 做异步导入
      collections: {
        carbon: () => import('@iconify-json/carbon/icons.json').then(i => i.default),
        mingcute: () => import('@iconify-json/mingcute/icons.json').then(i => i.default),
        lucide: () => import('@iconify-json/lucide/icons.json').then(i => i.default),
        tabler: () => import('@iconify-json/tabler/icons.json').then(i => i.default),
        materialSymbols: () => import('@iconify-json/material-symbols/icons.json').then(i => i.default),
        iconParkOutline: () => import('@iconify-json/icon-park-outline/icons.json').then(i => i.default),
      },
    }),
  ],
  safelist: [
    ...safeIconList,
  ],
  transformers: [
    transformerDirectives(),
    transformerVariantGroup(),
  ],
  theme: {
    colors: {
      primary: 'rgb(var(--color-primary))',
    },
  },
})
