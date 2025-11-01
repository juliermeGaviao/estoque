<script setup>
import api from '@/util/api'
import { formatNumber } from '@/util/util'
import { useToast } from 'primevue/usetoast'
import { onMounted, ref } from 'vue'

const props = defineProps({
  title: { type: [String], default: null },
  endpoint: { type: [String], default: null }
})

const toast = useToast()

const backgroundColorArray = [
  getComputedStyle(document.body).getPropertyValue('--p-orange-500'),
  getComputedStyle(document.body).getPropertyValue('--p-blue-500'),
  getComputedStyle(document.body).getPropertyValue('--p-green-500'),
  getComputedStyle(document.body).getPropertyValue('--p-yellow-500'),
  getComputedStyle(document.body).getPropertyValue('--p-red-500'),
  getComputedStyle(document.body).getPropertyValue('--p-cyan-500'),
  getComputedStyle(document.body).getPropertyValue('--p-gray-500')
]

const hoverBackgroundColorArray = [
  getComputedStyle(document.body).getPropertyValue('--p-orange-400'),
  getComputedStyle(document.body).getPropertyValue('--p-blue-400'),
  getComputedStyle(document.body).getPropertyValue('--p-green-400'),
  getComputedStyle(document.body).getPropertyValue('--p-yellow-400'),
  getComputedStyle(document.body).getPropertyValue('--p-red-400'),
  getComputedStyle(document.body).getPropertyValue('--p-cyan-400'),
  getComputedStyle(document.body).getPropertyValue('--p-gray-400'),
]

const frequencyOptions = ref([
  { id: 1, label: "Diária" },
  { id: 2, label: "Semanal" },
  { id: 3, label: "Mensal" },
])

onMounted(() => {
  loadReport()
})

const frequency = ref(1)

const quantityReport = ref({ labels: [], datasets: [] })
const averageReport = ref({ labels: [], datasets: [] })
const totalReport = ref({ labels: [], datasets: [] })

const quantityReportOptions = ref({
  plugins: {
    legend: { position: 'bottom', labels: { usePointStyle: true, color: getComputedStyle(document.documentElement).getPropertyValue('--p-text-color') } },
    title: { display: true, text: 'Quantidade', font: { size: 18 }, fullSize: false },
    datalabels: { color: '#000', font: { weight: 'bold', size: 12 }, rotation: -90 }
  },
})

const averageReportOptions = ref({
  plugins: {
    legend: { position: 'bottom', labels: { usePointStyle: true, color: getComputedStyle(document.documentElement).getPropertyValue('--p-text-color') } },
    title: { display: true, text: 'Valor médio (R$)', font: { size: 18 }, fullSize: false },
    datalabels: { color: '#000', font: { weight: 'bold', size: 12 }, rotation: -90, formatter: value =>  formatNumber(value) }
  },
})

const totalReportOptions = ref({
  plugins: {
    legend: { position: 'bottom', labels: { usePointStyle: true, color: getComputedStyle(document.documentElement).getPropertyValue('--p-text-color') } },
    title: { display: true, text: 'Valor total (R$)', font: { size: 18 }, fullSize: false },
    datalabels: { color: '#000', font: { weight: 'bold', size: 12 }, rotation: -90, formatter: value =>  formatNumber(value) }
  },
})

async function loadReport() {
  try {
    const response = await api.get('/report/' + props.endpoint, { params: { frequency: frequency.value } })

    quantityReport.value = getDataReport(response.data, 'quantidadeVendas')
    averageReport.value = getDataReport(response.data, 'mediaTotal')
    totalReport.value = getDataReport(response.data, 'somaTotal')
  } catch (error) {
    toast.add({ severity: "error", summary: "Falha de Carga de Relatório de Vendedores", detail: "Requisição de Relatório de Vendedores terminou com o erro: " + error.response.data, life: 10000 })
  }
}

function getDataReport(data, indicador) {
  const result = { labels: [], datasets: [] }
  const datasets = {}

  for (let periodo of data) {
    result.labels.push(periodo.grupo)

    for (let vendedor of periodo.subGrupos) {
      if (!datasets[vendedor.grupo]) {
        datasets[vendedor.grupo] = { label: vendedor.grupo, data: [], backgroundColor: backgroundColorArray[Object.keys(datasets).length], hoverBackgroundColor: hoverBackgroundColorArray[Object.keys(datasets).length] }
      }
    }
  }

  for (let periodo of data) {
    for (let dataset in datasets) {
      const grupo = periodo.subGrupos.find(subGrupo => subGrupo.grupo === dataset)

      datasets[dataset].data.push(grupo ? grupo.indicadores[indicador] : null)
    }
  }

  for (let dataset in datasets) {
    result.datasets.push(datasets[dataset])
  }

  return result
}
</script>

<template>
  <Card class="mb-2">
    <template #title>
      <div class="grid grid-cols-2">
        <h3>{{ props.title }}</h3>
        <div class="flex justify-end items-center">
          <FloatLabel variant="on">
            <Select id="idSalesmanFrequency" v-model="frequency" :options="frequencyOptions" @update:modelValue="loadReport" optionLabel="label" optionValue="id" class="w-[150px] mr-10"/>
            <label for="idSalesmanFrequency">Periodicidade</label>
          </FloatLabel>
        </div>
      </div>
    </template>
    <template #content>
      <div class="flex justify-around">
        <Chart type="bar" :data="quantityReport" :options="quantityReportOptions" class="w-[600px] h-[320px]"/>
        <Chart type="bar" :data="averageReport" :options="averageReportOptions" class="w-[600px] h-[320px]"/>
        <Chart type="bar" :data="totalReport" :options="totalReportOptions" class="w-[600px] h-[320px]"/>
      </div>
    </template>
  </Card>
</template>
