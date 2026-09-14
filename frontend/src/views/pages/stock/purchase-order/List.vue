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

  if (sortField.value) {
    query.sort = sortField.value

    if (sortOrder.value) {
      query.sort += sortOrder.value === 1 ? ",asc" : ",desc"
    }
  }

  if (query.minDataPedido) {
    query.minDataPedido = formatDate(query.minDataPedido)
  }

  if (query.maxDataPedido) {
    query.maxDataPedido = formatDate(query.maxDataPedido)
  }

  try {
    const response = await api.get('/purchase-order/list', { params: query })

    data.value = response.data.content
    totalRecords.value = response.data.totalElements
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Pedidos de Compra', detail: 'Requisição de lista de pedidos de compra terminou com o erro: ' + error.response.data, life: 10000 })
  }
}

onMounted(async () => {
  load({})
  loadProviders()
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
const formValues = ref({ numeroPedido: null, idFornecedor: null, minDataPedido: null, maxDataPedido: null })
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

let fornecedores = ref([])

async function loadProviders() {
  try {
    const response = await api.get('/provider/list', { params: { page: 0, size: 10000, sort: 'fantasia,asc' } })

    fornecedores.value = response.data.content
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Fornecedores', detail: 'Requisição de lista de Fornecedores terminou com o erro: ' + error.response.data, life: 10000 })
  }
}

function newOrder() {
  editRef.value?.newOrder()
}

function viewOrder(purchaseOrder) {
  editRef.value?.view(purchaseOrder)
}

function reloadList() {
  load({ ...filterValues.value })
}
</script>

<template>
  <ConfirmDialog :closable="false"></ConfirmDialog>
  <Card>
    <template #title><h3>Lista de Pedidos de Compra</h3></template>
    <template #content>
      <Form ref="form" :initialValues="formValues" @submit="filter" @reset="limpar" class="grid flex flex-column gap-2 mb-4">
        <div class="grid grid-cols-12 gap-2">
          <div class="col-span-3">
            <FormField name="numeroPedido">
              <FloatLabel variant="on">
                <InputText id="numeroPedido" maxlength="255" autocomplete="off" fluid/>
                <label for="numeroPedido">Número do Pedido</label>
              </FloatLabel>
            </FormField>
          </div>
          <div class="col-span-3">
            <FormField name="idFornecedor">
              <FloatLabel variant="on">
                <Select :options="fornecedores" optionLabel="fantasia" optionValue="id" fluid/>
                <label for="idFornecedor">Fornecedores</label>
              </FloatLabel>
            </FormField>
          </div>
          <div class="col-span-2">
            <FormField name="minDataPedido">
              <FloatLabel variant="on" class="flex-1">
                <DatePicker dateFormat="dd/mm/yy" showIcon :manualInput="false" fluid/>
                <label for="minDataPedido">Data de Pedido Mínima</label>
              </FloatLabel>
            </FormField>
          </div>
          <div class="col-span-2">
            <FormField name="maxDataPedido">
              <FloatLabel variant="on" class="flex-1">
                <DatePicker dateFormat="dd/mm/yy" showIcon :manualInput="false" fluid/>
                <label for="minDataPedido">Data de Pedido Máxima</label>
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
        <Column field="numeroPedido" header="Número do Pedido" sortable/>
        <Column field="fornecedor.fantasia" header="Fornecedor" sortable/>
        <Column field="dataPedido" header="Data do Pedido" sortable>
          <template #body="slotProps">
            {{ formatDate(slotProps.data.dataPedido) }}
          </template>
        </Column>

        <Column headerClass="flex justify-center" bodyClass="flex justify-center">
          <template #header>
            <Button icon="pi pi-plus" class="p-button-sm p-button-text p-mr-2" @click="newOrder" v-tooltip.bottom="'Novo Pedido de Compra'"/>
          </template>

          <template #body="slotProps">
            <Button icon="pi pi-eye" class="p-button-sm p-button-text p-mr-2" @click="viewOrder(slotProps.data)" v-tooltip.bottom="'Visualizar'"/>
          </template>
        </Column>
      </DataTable>
    </template>
  </Card>
  <Edit ref="editRef" @saved="reloadList"/>
</template>
