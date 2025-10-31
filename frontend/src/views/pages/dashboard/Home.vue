<script setup>
import api from '@/util/api'
import { formatNumber } from '@/util/util'
import { useToast } from 'primevue/usetoast'
import { onMounted, ref } from 'vue'

const toast = useToast()
const loading = ref(false)

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

const salesFrequency = ref(1)

const salesQuantityReport = ref({ labels: [], datasets: [] })
const salesAverageReport = ref({ labels: [], datasets: [] })
const salesTotalReport = ref({ labels: [], datasets: [] })

const salesQuantityReportOptions = ref({
  plugins: {
    legend: { position: 'right', labels: { usePointStyle: true, color: getComputedStyle(document.documentElement).getPropertyValue('--p-text-color') } },
    title: { display: true, text: 'Quantidade', font: { size: 18 }, fullSize: false },
    datalabels: { color: '#000', font: { weight: 'bold', size: 14 }, anchor: 'end', align: 'start', offset: 10 }
  },
})

const salesAverageReportOptions = ref({
  plugins: {
    legend: { position: 'right', labels: { usePointStyle: true, color: getComputedStyle(document.documentElement).getPropertyValue('--p-text-color') } },
    title: { display: true, text: 'Valor médio (R$)', font: { size: 18 }, fullSize: false },
    datalabels: { color: '#000', font: { weight: 'bold', size: 14 }, formatter: value =>  formatNumber(value), anchor: 'end', align: 'start', offset: 10 }
  }
})

const salesTotalReportOptions = ref({
  plugins: {
    legend: { position: 'right', labels: { usePointStyle: true, color: getComputedStyle(document.documentElement).getPropertyValue('--p-text-color') } },
    title: { display: true, text: 'Valor total (R$)', font: { size: 18 }, fullSize: false },
    datalabels: { color: '#000', font: { weight: 'bold', size: 14 }, formatter: value =>  formatNumber(value), anchor: 'end', align: 'start', offset: 10 }
  }
})

async function loadSalesReport() {
  try {
    const response = await api.get("/report/sale-report", { params: { frequency: salesFrequency.value } })

    salesQuantityReport.value = { labels: [], datasets: [] }
    salesAverageReport.value = { labels: [], datasets: [] }
    salesTotalReport.value = { labels: [], datasets: [] }

    salesQuantityReport.value.datasets.push({ data: [], backgroundColor: backgroundColorArray, hoverBackgroundColor: hoverBackgroundColorArray })
    salesAverageReport.value.datasets.push({ data: [], backgroundColor: backgroundColorArray, hoverBackgroundColor: hoverBackgroundColorArray })
    salesTotalReport.value.datasets.push({ data: [], backgroundColor: backgroundColorArray, hoverBackgroundColor: hoverBackgroundColorArray })

    for (let periodo of response.data) {
      salesQuantityReport.value.labels.push(periodo.grupo)
      salesQuantityReport.value.datasets[0].data.push(periodo.indicadores.quantidadeVendas)

      salesAverageReport.value.labels.push(periodo.grupo)
      salesAverageReport.value.datasets[0].data.push(periodo.indicadores.mediaTotal)

      salesTotalReport.value.labels.push(periodo.grupo)
      salesTotalReport.value.datasets[0].data.push(periodo.indicadores.somaTotal)
    }
  } catch (error) {
    toast.add({ severity: "error", summary: "Falha de Carga de Relatório de Vendas", detail: "Requisição de Relatório de Vendas terminou com o erro: " + error.response.data, life: 10000 })
  }
}

onMounted(() => {
  loading.value = true

  loadSalesReport()
  loadSalesmanReport()

  loading.value = false
})

const salesmanFrequency = ref(1)

const salesmanQuantityReport = ref({ labels: [], datasets: [] })
const salesmanAverageReport = ref({ labels: [], datasets: [] })
const salesmanTotalReport = ref({ labels: [], datasets: [] })

const salesmanQuantityReportOptions = ref({
  plugins: {
    legend: { position: 'bottom', labels: { usePointStyle: true, color: getComputedStyle(document.documentElement).getPropertyValue('--p-text-color') } },
    title: { display: true, text: 'Quantidade', font: { size: 18 }, fullSize: false },
    datalabels: { color: '#000', font: { weight: 'bold', size: 12 }, rotation: -90 }
  },
})

const salesmanAverageReportOptions = ref({
  plugins: {
    legend: { position: 'bottom', labels: { usePointStyle: true, color: getComputedStyle(document.documentElement).getPropertyValue('--p-text-color') } },
    title: { display: true, text: 'Valor médio (R$)', font: { size: 18 }, fullSize: false },
    datalabels: { color: '#000', font: { weight: 'bold', size: 12 }, rotation: -90, formatter: value =>  formatNumber(value) }
  },
})

const salesmanTotalReportOptions = ref({
  plugins: {
    legend: { position: 'bottom', labels: { usePointStyle: true, color: getComputedStyle(document.documentElement).getPropertyValue('--p-text-color') } },
    title: { display: true, text: 'Valor total (R$)', font: { size: 18 }, fullSize: false },
    datalabels: { color: '#000', font: { weight: 'bold', size: 12 }, rotation: -90, formatter: value =>  formatNumber(value) }
  },
})

async function loadSalesmanReport() {
  try {
    const response = await api.get("/report/salesman-report", { params: { frequency: salesmanFrequency.value } })

    salesmanQuantityReport.value = getSalesmanReport(response.data, 'quantidadeVendas')
    salesmanAverageReport.value = getSalesmanReport(response.data, 'mediaTotal')
    salesmanTotalReport.value = getSalesmanReport(response.data, 'somaTotal')
  } catch (error) {
    toast.add({ severity: "error", summary: "Falha de Carga de Relatório de Vendedores", detail: "Requisição de Relatório de Vendedores terminou com o erro: " + error.response.data, life: 10000 })
  }
}

function getSalesmanReport(data, indicador) {
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
  <BlockUI :blocked="loading" fullScreen>
    <Card class="mb-2">
      <template #title>
        <div class="grid grid-cols-2">
          <h3>Relatório de Vendas</h3>
          <div class="flex justify-end items-center">
            <FloatLabel variant="on">
              <Select id="idSalesFrequency" v-model="salesFrequency" :options="frequencyOptions" @update:modelValue="loadSalesReport" optionLabel="label" optionValue="id" class="w-[150px] mr-10"/>
              <label for="idSalesFrequency">Periodicidade</label>
            </FloatLabel>
          </div>
        </div>
      </template>
      <template #content>
        <div class="flex justify-around">
          <Chart type="pie" :data="salesQuantityReport" :options="salesQuantityReportOptions" class="w-[450px] h-[450px]"/>
          <Chart type="pie" :data="salesAverageReport" :options="salesAverageReportOptions" class="w-[450px] h-[450px]"/>
          <Chart type="pie" :data="salesTotalReport" :options="salesTotalReportOptions" class="w-[450px] h-[450px]"/>
        </div>
      </template>
    </Card>
    <Card class="mb-2">
      <template #title>
        <div class="grid grid-cols-2">
          <h3>Relatório de Vendedores</h3>
          <div class="flex justify-end items-center">
            <FloatLabel variant="on">
              <Select id="idSalesmanFrequency" v-model="salesmanFrequency" :options="frequencyOptions" @update:modelValue="loadSalesmanReport" optionLabel="label" optionValue="id" class="w-[150px] mr-10"/>
              <label for="idSalesmanFrequency">Periodicidade</label>
            </FloatLabel>
          </div>
        </div>
      </template>
      <template #content>
        <div class="flex justify-around">
          <Chart type="bar" :data="salesmanQuantityReport" :options="salesmanQuantityReportOptions" class="w-[600px] h-[320px]"/>
          <Chart type="bar" :data="salesmanAverageReport" :options="salesmanAverageReportOptions" class="w-[600px] h-[320px]"/>
          <Chart type="bar" :data="salesmanTotalReport" :options="salesmanTotalReportOptions" class="w-[600px] h-[320px]"/>
        </div>
      </template>
    </Card>
  </BlockUI>
</template>
