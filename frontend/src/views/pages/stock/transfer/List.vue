<script setup>
import api from '@/util/api'
import { formatDate, toDate } from '@/util/util'
import { zodResolver } from '@primevue/forms/resolvers/zod'
import { useToast } from 'primevue/usetoast'
import { nextTick, onMounted, ref } from 'vue'
import { z } from 'zod'

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

    if (sortOrder) {
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

let pontos = ref([])

async function loadSalePoints() {
  try {
    const response = await api.get('/sale-point/list', { params: { page: 0, size: 10000, sort: 'id,asc' } })

    pontos.value = response.data.content
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Pontos de Venda', detail: 'Requisição de lista de pontos de venda terminou com o erro: ' + error.response.data, life: 10000 })
  }
}

const products = ref([])

async function loadSalePointProducts(origem, destino) {
  try {
    const response = await api.get('/stock-transfer/list-products', { params: { idPontoVendaOrigem: origem, idPontoVendaDestingo: destino } })

    products.value = response.data.map(item => ({
      ...item,
      quantidade: 0
    }))
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Produtos', detail: 'Requisição de lista de produtos de ponto de venda terminou com o erro: ' + error.response.data, life: 10000 })
  }
}

async function loadStockTransferProducts(stockTransferId) {
  try {
    const response = await api.get('/stock-transfer/list-sale-point-products', { params: { idTransferenciaEstoque: stockTransferId } })

    products.value = response.data.map(item => ({
      ...item,
      quantidade: 0
    }))
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Produtos', detail: 'Requisição de lista de produtos de transferência de estoque terminou com o erro: ' + error.response.data, life: 10000 })
  }
}

const visible = ref(false)
const action = ref()
const stockTransferForm = ref(null)
const stockTransferFormValues = ref({ idPontoVendaOrigem: null, idPontoVendaDestino: null, dataTransferencia: new Date() })

const stockTransferFormValidator = zodResolver(
  z.object({
    idPontoVendaOrigem: z.number({ required_error: 'Ponto de Venda Origem é obrigatório.' }).nullable().refine((val) => val !== null && val !== undefined, { message: 'Ponto de Venda Origem é obrigatório.' }),
    idPontoVendaDestino: z.number({ required_error: 'Ponto de Venda Destino é obrigatório.' }).nullable().refine((val) => val !== null && val !== undefined, { message: 'Ponto de Venda Destino é obrigatório.' }),
    dataTransferencia: z.any().nullable().optional()
  })
)

const salePointChange = async (event) => {
  if (!stockTransferForm.value) return

  const states = stockTransferForm.value.states

  const origem = states.idPontoVendaOrigem.value
  const destino = states.idPontoVendaDestino.value

  if (origem && destino && origem !== destino) {
    await loadSalePointProducts(origem, destino)
  } else {
    products.value = []
  }
}

const saveStockTransfer = async ({ valid, values }) => {
  if (!valid) return

  const filtered = products.value.filter(item => item.quantidade !== 0)

  if (!filtered.length) {
    toast.add({ severity: 'error', summary: 'Falha de Transferência de Estoque', detail: 'Algum produto deve ter valor diferente de zero para transferir.', life: 10000 })
    return
  }

  const params = {
    ...values,
    pontoVendaOrigem: { id: values.idPontoVendaOrigem },
    pontoVendaDestino: { id: values.idPontoVendaDestino },
    estoque: filtered.map(item => ({ idProduto: item.id, quantidade: item.quantidade }) )
  }

  const [dia, mes, ano] = params.dataTransferencia.split('/')
  params.dataTransferencia = `${ano}-${mes}-${dia}`

  try {
    const response = await api.post('/stock-transfer', params)

    if (response.status === 200) {
      toast.add({ severity: 'success', summary: 'Sucesso', detail: 'Transferência de Estoque realizada com sucesso', life: 10000 })
    }
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Gravação do Transferência de Estoque', detail: 'Requisição de pedido de compra terminou com o erro: ' + error.response.data, life: 10000 })
  } finally {
    toggle()
    load( { ...filterValues.value } )
  }
}

function toggle() {
  products.value = []

  if (stockTransferForm.value) {
    stockTransferForm.value.reset()
  }

  visible.value = !visible.value
}

async function newStockTransfer() {
  action.value = 'criar'
  toggle()
  await nextTick()
  stockTransferForm.value.setFieldValue('dataTransferencia', formatDate(new Date()))
}

const view = async (stockTransfer) => {
  action.value = 'visualizar'
  visible.value = true

  await nextTick()

  stockTransferForm.value.setValues({ idPontoVendaOrigem: stockTransfer.pontoVendaOrigem.id, idPontoVendaDestino: stockTransfer.pontoVendaDestino.id, dataTransferencia: formatDate(toDate(stockTransfer.dataTransferencia)) })
  loadStockTransferProducts(stockTransfer.id)
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

  <Dialog v-model:visible="visible" modal closable header="Transferência de Estoque" style="width: 95%">

    <Form ref="stockTransferForm" :resolver="stockTransferFormValidator" :initialValues="stockTransferFormValues" @submit="saveStockTransfer" class="grid flex flex-column gap-2">
      <div class="grid grid-cols-12 gap-2">
        <div class="col-span-4 mt-2">
          <FormField name="idPontoVendaOrigem" v-slot="$field">
            <FloatLabel variant="on">
              <Select :options="pontos" optionLabel="nome" optionValue="id" @change="salePointChange" :disabled="action === 'visualizar'" fluid/>
              <label for="idPontoVendaOrigem">Ponto de Venda Origem</label>
            </FloatLabel>
            <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
          </FormField>
        </div>
        <div class="col-span-4 mt-2">
          <FormField name="idPontoVendaDestino" v-slot="$field">
            <FloatLabel variant="on">
              <Select :options="pontos" optionLabel="nome" optionValue="id" @change="salePointChange" :disabled="action === 'visualizar'" fluid/>
              <label for="idPontoVendaDestino">Ponto de Venda Destino</label>
            </FloatLabel>
            <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
          </FormField>
        </div>
        <div :class="action === 'criar' ? 'col-span-3 mt-2' : 'col-span-4 mt-2'">
          <FormField name="dataTransferencia" v-slot="$field">
            <FloatLabel variant="on" class="flex-1">
              <DatePicker dateFormat="dd/mm/yy" showIcon :manualInput="false" disabled fluid/>
              <label for="dataTransferencia">Data de Transferência</label>
            </FloatLabel>
            <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
          </FormField>
        </div>
        <div class="col-span-1 mt-2" v-show="action === 'criar'">
          <FormField class="flex justify-end gap-2">
            <Button label="Transferir" icon="pi pi-save" :disabled="!products.length" type="submit" raised/>
          </FormField>
        </div>
      </div>

      <DataTable :value="products" :lazy="true" responsiveLayout="scroll" stripedRows size="small" class="mt-4 mb-4">
        <Column field="id" header="Id"><template #body="slotProps">{{slotProps.data.id}}</template></Column>
        <Column field="nome" header="Nome"/>
        <Column field="referencia" header="Referência"/>
        <Column field="tipoProduto.nome" header="Tipo de Produto"/>
        <Column field="fornecedor.fantasia" header="Fornecedor"/>
        <Column field="peso" header="Peso (em gramas)"/>
        <Column field="estoque" :header="action === 'visualizar' ? 'Transferido' : 'Estoque Origem'"/>
        <Column field="estoqueDestino" header="Estoque Destino" v-if="action === 'criar'"/>

        <Column headerClass="flex justify-center" bodyClass="flex justify-center" v-if="action === 'criar'">
          <template #header>
            <b>Transferir</b>
          </template>

          <template #body="slotProps">
            <InputNumber v-model="slotProps.data.quantidade" :min="0" :max="slotProps.data.estoque" showButtons buttonLayout="horizontal" :step="1" fluid/>
          </template>
        </Column>
      </DataTable>

      <FormField class="flex justify-end gap-2" v-show="action === 'criar'">
        <Button label="Cancelar" icon="pi pi-times" @click="toggle" severity="secondary" raised/>
        <Button label="Transferir" icon="pi pi-save" :disabled="!products.length" type="submit" raised/>
      </FormField>
      <FormField class="flex justify-end gap-2" v-show="action === 'visualizar'">
        <Button label="Fechar" icon="pi pi-times" @click="toggle" severity="secondary" raised/>
      </FormField>
    </Form>
  </Dialog>
</template>
