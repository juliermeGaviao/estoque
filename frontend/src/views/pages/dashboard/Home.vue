<script setup>
import api from '@/util/api'
import { useToast } from 'primevue/usetoast'
import { onMounted, ref } from 'vue'

const toast = useToast()
const loading = ref(false)

const documentStyle = getComputedStyle(document.body)

const backgroundColorArray = [
  documentStyle.getPropertyValue('--p-orange-500'),
  documentStyle.getPropertyValue('--p-blue-500'),
  documentStyle.getPropertyValue('--p-green-500'),
  documentStyle.getPropertyValue('--p-yellow-500'),
  documentStyle.getPropertyValue('--p-red-500'),
  documentStyle.getPropertyValue('--p-cyan-500'),
  documentStyle.getPropertyValue('--p-gray-500')
]

const hoverBackgroundColorArray = [
  documentStyle.getPropertyValue('--p-orange-400'),
  documentStyle.getPropertyValue('--p-blue-400'),
  documentStyle.getPropertyValue('--p-green-400'),
  documentStyle.getPropertyValue('--p-yellow-400'),
  documentStyle.getPropertyValue('--p-red-400'),
  documentStyle.getPropertyValue('--p-cyan-400'),
  documentStyle.getPropertyValue('--p-gray-400'),
]

const salesQuantityReport = ref({ labels: [], datasets: [] })
const salesAverageReport = ref({ labels: [], datasets: [] })
const salesTotalReport = ref({ labels: [], datasets: [] })

const salesQuantityReportOptions = ref({
  plugins: {
    legend: { position: 'right', labels: { usePointStyle: true, color: getComputedStyle(document.documentElement).getPropertyValue('--p-text-color') } },
    title: { display: true, text: 'Quantidade de Vendas', font: { size: 18 }, fullSize: false }
  },
})

const salesAverageReportOptions = ref({
  plugins: {
    legend: { position: 'right', labels: { usePointStyle: true, color: getComputedStyle(document.documentElement).getPropertyValue('--p-text-color') } },
    title: { display: true, text: 'Valor médio das Vendas', font: { size: 18 }, fullSize: false }
  }
})

const salesTotalReportOptions = ref({
  plugins: {
    legend: { position: 'right', labels: { usePointStyle: true, color: getComputedStyle(document.documentElement).getPropertyValue('--p-text-color') } },
    title: { display: true, text: 'Valor total das Vendas', font: { size: 18 }, fullSize: false }
  }
})

async function loadSaleReport() {
  try {
    const response = await api.get("/report/sale-report", { params: { frequency: 1 } })

    salesQuantityReport.value.datasets.push({ data: [], backgroundColor: backgroundColorArray, hoverBackgroundColor: hoverBackgroundColorArray })
    salesAverageReport.value.datasets.push({ data: [], backgroundColor: backgroundColorArray, hoverBackgroundColor: hoverBackgroundColorArray })
    salesTotalReport.value.datasets.push({ data: [], backgroundColor: backgroundColorArray, hoverBackgroundColor: hoverBackgroundColorArray })

    for (let periodo of response.data) {
      salesQuantityReport.value.labels.push(periodo.grupo)
      salesQuantityReport.value.datasets[0].data.push(periodo.indicadores[0].quantidadeVendas)

      salesAverageReport.value.labels.push(periodo.grupo)
      salesAverageReport.value.datasets[0].data.push(periodo.indicadores[0].mediaTotal)

      salesTotalReport.value.labels.push(periodo.grupo)
      salesTotalReport.value.datasets[0].data.push(periodo.indicadores[0].somaTotal)
    }
  } catch (error) {
    toast.add({ severity: "error", summary: "Falha de Carga de Relatório de Vendas", detail: "Requisição de Relatório de Vendas terminou com o erro: " + error.response.data, life: 10000 })
  }
}

onMounted(() => {
  loading.value = true

  loadSaleReport()

  loading.value = false
})

</script>

<template>
  <BlockUI :blocked="loading" fullScreen>
    <Card>
      <template #title><h3>Relatório de Vendas</h3></template>
      <template #content>
        <div class="flex justify-around">
          <Chart type="pie" :data="salesQuantityReport" :options="salesQuantityReportOptions" class="w-[450px] h-[450px]"/>
          <Chart type="pie" :data="salesAverageReport" :options="salesAverageReportOptions" class="w-[450px] h-[450px]"/>
          <Chart type="pie" :data="salesTotalReport" :options="salesTotalReportOptions" class="w-[450px] h-[450px]"/>
        </div>
      </template>
    </Card>
  </BlockUI>
</template>
