<script setup>
import api from '@/util/api'
import { formatDate } from '@/util/util'
import { useToast } from 'primevue/usetoast'
import { nextTick, onMounted, ref } from 'vue'
import Edit from './Edit.vue'

const editRef = ref(null)

const toast = useToast()

const data = ref([])
const totalRecords = ref(0)

const page = ref(0)
const size = ref(20)
const sortField = ref(null)
const sortOrder = ref(null)

async function load(params) {
  const query = {
    ...params,
    page: page.value,
    size: size.value,
  }

  if (sortField?.value) {
    query.sort = sortField.value

    if (sortOrder?.value) {
      query.sort += sortOrder.value === 1 ? ",asc" : ",desc"
    }
  }

  if (query.minDataTransferencia) {
    query.minDataTransferencia = formatDate(query.minDataTransferencia)
  }

  if (query.maxDataTransferencia) {
    query.maxDataTransferencia = formatDate(query.maxDataTransferencia)
  }

  try {
    const response = await api.get('/stock-transfer/list', { params: query })

    data.value = response.data.content
    totalRecords.value = response.data.totalElements
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Transferências de Estoque', detail: 'Requisição de lista de transferências de estoque terminou com o erro: ' + error.response.data, life: 10000 })
  }
}

onMounted(async () => {
  load({})
  loadSalePoints()
})

function onPage(event) {
  page.value = event.page
  size.value = event.rows

  load( { ...filterValues.value } )
}

function onSort(event) {
  page.value = 0
  sortField.value = event.sortField
  sortOrder.value = event.sortOrder

  load( { ...filterValues.value } )
}

const form = ref(null)
const formValues = ref({ idPontoVendaOrigem: null, idPontoVendaDestino: null, minDataTransferencia: null, maxDataTransferencia: null })
const filterValues = ref({ ... formValues.value })

const filter = async ({ valid, values }) => {
  if (!valid) return

  filterValues.value = { ...values }
  page.value = 0

  load( { ...filterValues.value } )
}

function limpar() {
  nextTick(() => {
    page.value = 0
    filterValues.value = { ... formValues.value }
    sortField.value = null
    load( { ...filterValues.value } )
  })
}

const pontos = ref([])

async function loadSalePoints() {
  try {
    const response = await api.get('/sale-point/list', { params: { page: 0, size: 10000, sort: 'id,asc' } })

    pontos.value = response.data.content
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Pontos de Venda', detail: 'Requisição de lista de pontos de venda terminou com o erro: ' + error.response.data, life: 10000 })
  }
}

function newStockTransfer() {
  editRef.value?.newStockTransfer()
}

function view(purchaseOrder) {
  editRef.value?.view(purchaseOrder)
}

function reloadList() {
  load({ ...filterValues.value })
}
</script>

<template>
  <ConfirmDialog :closable="false"></ConfirmDialog>
  <Card>
    <template #title><h3>Lista de Transferências de Estoque</h3></template>
    <template #content>
      <Form ref="form" :initialValues="formValues" @submit="filter" @reset="limpar" class="grid flex flex-column gap-2 mb-4">
        <div class="grid grid-cols-12 gap-2">
          <div class="col-span-3">
            <FormField name="idPontoVendaOrigem">
              <FloatLabel variant="on">
                <Select :options="pontos" optionLabel="nome" optionValue="id" fluid/>
                <label for="idPontoVendaOrigem">Ponto de Venda Origem</label>
              </FloatLabel>
            </FormField>
          </div>
          <div class="col-span-3">
            <FormField name="idPontoVendaDestino">
              <FloatLabel variant="on">
                <Select :options="pontos" optionLabel="nome" optionValue="id" fluid/>
                <label for="idPontoVendaDestino">Ponto de Venda Destino</label>
              </FloatLabel>
            </FormField>
          </div>
          <div class="col-span-2">
            <FormField name="minDataTransferencia">
              <FloatLabel variant="on" class="flex-1">
                <DatePicker dateFormat="dd/mm/yy" showIcon :manualInput="false" fluid/>
                <label for="minDataTransferencia">Data de Transferência Mínima</label>
              </FloatLabel>
            </FormField>
          </div>
          <div class="col-span-2">
            <FormField name="maxDataTransferencia">
              <FloatLabel variant="on" class="flex-1">
                <DatePicker dateFormat="dd/mm/yy" showIcon :manualInput="false" fluid/>
                <label for="minDataTransferencia">Data de Transferência Máxima</label>
              </FloatLabel>
            </FormField>
          </div>
          <div class="col-span-2">
            <FormField class="flex justify-end gap-2">
              <Button label="Limpar" icon="pi pi-times" type="reset" severity="secondary" raised/>
              <Button label="Buscar" icon="pi pi-search" type="submit" raised/>
            </FormField>
          </div>
        </div>
      </Form>

      <DataTable :value="data" :lazy="true" :paginator="true" :rows="size" :totalRecords="totalRecords"
        :first="page * size" @page="onPage" @sort="onSort" :sortField="sortField" :sortOrder="sortOrder" responsiveLayout="scroll" stripedRows
        :rowsPerPageOptions="[20, 40, 60, 100]" size="small">

        <Column field="id" header="Id" sortable/>
        <Column field="pontoVendaOrigem.nome" header="Ponto de Venda Origem" sortable/>
        <Column field="pontoVendaDestino.nome" header="Ponto de Venda Destino" sortable/>
        <Column field="dataTransferencia" header="Data do Transferência" sortable>
          <template #body="slotProps">
            {{ formatDate(slotProps.data.dataTransferencia) }}
          </template>
        </Column>

        <Column headerClass="flex justify-center" bodyClass="flex justify-center">
          <template #header>
            <Button icon="pi pi-plus" class="p-button-sm p-button-text p-mr-2" @click="newStockTransfer" v-tooltip.bottom="'Nova Transferência de Estoque'"/>
          </template>

          <template #body="slotProps">
            <Button icon="pi pi-eye" class="p-button-sm p-button-text p-mr-2" @click="view(slotProps.data)" v-tooltip.bottom="'Visualizar'"/>
          </template>
        </Column>
      </DataTable>
    </template>
  </Card>
  <Edit ref="editRef" @saved="reloadList"/>
</template>
