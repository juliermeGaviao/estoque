import { createApp } from 'vue'
import App from './App.vue'
import router from './router'

import Aura from '@primeuix/themes/aura'
import PrimeVue from 'primevue/config'
import ConfirmationService from 'primevue/confirmationservice'
import ToastService from 'primevue/toastservice'

import api from './util/api'

import '@/assets/styles.scss'
import '@/assets/tailwind.css'

import { Chart as ChartJS } from 'chart.js'
import ChartDataLabels from 'chartjs-plugin-datalabels'


ChartJS.register(ChartDataLabels)

const token = localStorage.getItem('token')
if (token) {
  api.defaults.headers.common['Authorization'] = `Bearer ${token}`
}

const app = createApp(App)

app.use(router)
app.use(PrimeVue, {
  theme: {
    preset: Aura,
    options: {
      darkModeSelector: '.app-dark'
    }
  },
  locale: {
    choose: 'Selecionar',
    upload: 'Enviar',
    cancel: 'Cancelar',
    fileSizeTypes: ['B', 'KB', 'MB', 'GB', 'TB'],
    invalidFileSizeMessage: 'Arquivo não pode ultrapassar 10MB',
    noFileChosenMessage: 'Nenhum arquivo selecionado',

    dayNames: ["Domingo", "Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado"],
    dayNamesShort: ["Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb"],
    dayNamesMin: ["D", "S", "T", "Q", "Q", "S", "S"],
    
    monthNames: ["Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"],
    monthNamesShort: ["Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez"],
    
    today: 'Hoje',
    clear: 'Limpar',
    dateFormat: 'dd/mm/yy',
    firstDayOfWeek: 0
  }
})
app.use(ToastService)
app.use(ConfirmationService)

app.mount('#app')
