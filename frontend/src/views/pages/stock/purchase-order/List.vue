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
const loading = ref(false)

const page = ref(0)
const size = ref(15)
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

  if (query.minDataPedido) {
    query.minDataPedido = formatDate(query.minDataPedido)
  }

  if (query.maxDataPedido) {
    query.maxDataPedido = formatDate(query.maxDataPedido)
  }

  loading.value = true

  try {
    const response = await api.get('/purchase-order/list', { params: query })

    data.value = response.data.content
    totalRecords.value = response.data.totalElements
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Pedidos de Compra', detail: 'Requisição de lista de pedidos de compra terminou com o erro: ' + error.response.data, life: 10000 })
  } finally {
    loading.value = false
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
  loading.value = true

  try {
    const response = await api.get('/provider/list', { params: { page: 0, size: 10000, sort: 'fantasia,asc' } })

    fornecedores.value = response.data.content
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Fornecedores', detail: 'Requisição de lista de Fornecedores terminou com o erro: ' + error.response.data, life: 10000 })
  } finally {
    loading.value = true
  }
}

const products = ref([])

async function loadProviderProducts(providerId) {
  loading.value = true

  try {
    const response = await api.get('/purchase-order/list-products', { params: { idFornecedor: providerId } })

    products.value = response.data.map(item => ({
      ...item,
      quantidade: 0
    }))
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Produtos', detail: 'Requisição de lista de produtos de fornecedor terminou com o erro: ' + error.response.data, life: 10000 })
  } finally {
    loading.value = true
  }
}

async function loadPurchaseOrderProducts(purchaseOrderId) {
  loading.value = true

  try {
    const response = await api.get('/purchase-order/list-purchase-order-products', { params: { idPedidoCompra: purchaseOrderId } })

    products.value = response.data.map(item => ({
      ...item,
      quantidade: 0
    }))
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Produtos', detail: 'Requisição de lista de produtos de pedido de compra terminou com o erro: ' + error.response.data, life: 10000 })
  } finally {
    loading.value = true
  }
}

const visible = ref(false)
const action = ref()
const purchaseOrderForm = ref(null)
const purchaseOrderFormValues = ref({ numeroPedido: null, idFornecedor: null, dataPedido: null })

const purchaseOrderFormValidator = zodResolver(
  z.object({
    numeroPedido: z.string().min(1).nullish().refine(val => val && val.trim().length > 0, { message: 'Número do Pedido é obrigatório.' }),
    idFornecedor: z.number({ required_error: 'Fornecedor é obrigatório.' }),
    dataPedido: z.any()
      .refine(val => {
        if (val === '' || !val) return false

        if (val instanceof Date && !Number.isNaN(val.getTime())) return true

        if (typeof val === 'string') {
          const date = new Date(val)
          return !Number.isNaN(date.getTime())
        }

        return false
        }, { message: 'Data do Pedido é obrigatória.' }
      )
  })
)

const providerChange = async (event) => {
  loadProviderProducts(event.value)
}

const savePurchaseOrder = async ({ valid, values }) => {
  if (!valid) return

  const filtered = products.value.filter(item => item.quantidade !== 0)

  if (!filtered.length) {
    toast.add({ severity: 'error', summary: 'Falha de Pedido de Compra', detail: 'Algum produto deve ter valor diferente de zero.', life: 10000 })
    return
  }

  const params = {
    ...values,
    fornecedor: { id: values.idFornecedor },
    estoque: filtered.map(item => ({ idProduto: item.id, quantidade: item.quantidade }) )
  }

  loading.value = true

  try {
    const response = await api.post('/purchase-order', params)

    if (response.status === 200) {
      toast.add({ severity: 'success', summary: 'Sucesso', detail: 'Pedido de Compra criado com sucesso', life: 10000 })
    }
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Gravação do Pedido de Compra', detail: 'Requisição de pedido de compra terminou com o erro: ' + error.response.data, life: 10000 })
  } finally {
    loading.value = true
    toggle()
    load( { ...filterValues.value } )
  }
}

function toggle() {
  products.value = []

  if (purchaseOrderForm.value) {
    purchaseOrderForm.value.reset()
  }

  visible.value = !visible.value
}

function newPurchaseOrder() {
  action.value = 'criar'
  toggle()
}

const view = async (purchaseOrder) => {
  action.value = 'visualizar'
  visible.value = true

  await nextTick()
  purchaseOrderForm.value.setValues({ numeroPedido: purchaseOrder.numeroPedido, idFornecedor: purchaseOrder.fornecedor.id, dataPedido: toDate(purchaseOrder.dataPedido) })
  loadPurchaseOrderProducts(purchaseOrder.id)
}

</script>

<template>
  <ConfirmDialog :closable="false"></ConfirmDialog>
  <BlockUI :blocked="loading" fullScreen>
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
          :rowsPerPageOptions="[15, 30, 60, 100]" size="small">

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
              <Button icon="pi pi-plus" class="p-button-sm p-button-text p-mr-2" @click="newPurchaseOrder" v-tooltip.bottom="'Novo Pedido de Compra'"/>
            </template>

            <template #body="slotProps">
              <Button icon="pi pi-eye" class="p-button-sm p-button-text p-mr-2" @click="view(slotProps.data)" v-tooltip.bottom="'Visualizar'"/>
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>

    <Dialog v-model:visible="visible" modal closable header="Pedido de Compra" style="width: 95%">

      <Form ref="purchaseOrderForm" :resolver="purchaseOrderFormValidator" :initialValues="purchaseOrderFormValues" @submit="savePurchaseOrder" class="grid flex flex-column gap-2">
        <div class="grid grid-cols-12 gap-2">
          <div class="col-span-4 mt-2">
            <FormField name="numeroPedido" v-slot="$field">
              <FloatLabel variant="on">
                <InputText maxlength="255" autocomplete="off" :disabled="action === 'visualizar'" fluid/>
                <label for="numeroPedido">Número do Pedido</label>
              </FloatLabel>
              <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
            </FormField>
          </div>
          <div class="col-span-4 mt-2">
            <FormField name="idFornecedor">
              <FloatLabel variant="on">
                <Select :options="fornecedores" optionLabel="fantasia" optionValue="id" @change="providerChange" :disabled="action === 'visualizar'" fluid/>
                <label for="idFornecedor">Fornecedores</label>
              </FloatLabel>
            </FormField>
          </div>
          <div :class="action === 'criar' ? 'col-span-3 mt-2' : 'col-span-4 mt-2'">
            <FormField name="dataPedido" v-slot="$field">
              <FloatLabel variant="on" class="flex-1">
                <DatePicker dateFormat="dd/mm/yy" showIcon :manualInput="false" :disabled="action === 'visualizar'" fluid/>
                <label for="dataPedido">Data de Pedido</label>
              </FloatLabel>
              <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
            </FormField>
          </div>
          <div class="col-span-1 mt-2" v-show="action === 'criar'">
            <FormField class="flex justify-end gap-2">
              <Button label="Pedir" icon="pi pi-save" :disabled="!products.length" type="submit" raised/>
            </FormField>
          </div>
        </div>

        <DataTable :value="products" :lazy="true" responsiveLayout="scroll" stripedRows size="small" class="mt-4 mb-4">
          <Column field="id" header="Id"><template #body="slotProps">{{slotProps.data.id}}</template></Column>
          <Column field="nome" header="Nome"/>
          <Column field="referencia" header="Referência"/>
          <Column field="tipoProduto.nome" header="Tipo de Produto"/>
          <Column field="peso" header="Peso (em gramas)"/>
          <Column field="estoque" :header="action === 'visualizar' ? 'Estocado' : 'Em Estoque'"/>

          <Column headerClass="flex justify-center" bodyClass="flex justify-center" v-if="action === 'criar'">
            <template #header>
              <b>Adicionar</b>
            </template>

            <template #body="slotProps">
              <InputNumber v-model="slotProps.data.quantidade" :min="-slotProps.data.estoque" :max="10000" showButtons buttonLayout="horizontal" :step="1" fluid/>
            </template>
          </Column>
        </DataTable>

        <FormField class="flex justify-end gap-2" v-show="action === 'criar'">
          <Button label="Cancelar" icon="pi pi-times" @click="toggle" severity="secondary" raised/>
          <Button label="Pedir" icon="pi pi-save" :disabled="!products.length" type="submit" raised/>
        </FormField>
        <FormField class="flex justify-end gap-2" v-show="action === 'visualizar'">
          <Button label="Fechar" icon="pi pi-times" @click="toggle" severity="secondary" raised/>
        </FormField>
      </Form>
    </Dialog>
  </BlockUI>
</template>
