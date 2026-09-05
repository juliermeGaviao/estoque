import pluginVue from 'eslint-plugin-vue'
import skipFormatting from '@vue/eslint-config-prettier/skip-formatting'

export default [
  // Arquivos analisados
  {
    name: 'app/files-to-lint',
    files: ['**/*.{js,mjs,jsx,vue}'],
  },

  // Diretórios ignorados
  {
    name: 'app/files-to-ignore',
    ignores: ['**/dist/**', '**/dist-ssr/**', '**/coverage/**'],
  },

  // Configurações recomendadas para Vue 3
  ...pluginVue.configs['flat/essential'],

  // Integração com Prettier
  skipFormatting,

  // Regras customizadas trazidas do seu .eslintrc.js antigo
  {
    rules: {
      'vue/multi-word-component-names': 'off',
      'vue/no-reserved-component-names': 'off',
      'vue/block-order': [
        'error',
        {
          order: ['script', 'template', 'style']
        }
      ]
    }
  }
]