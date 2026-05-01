<script setup>
import api from '@/util/api'
import { useToast } from 'primevue/usetoast'
import { defineEmits, defineProps, onMounted, ref, watch } from 'vue'

const toast = useToast()
const loading = ref(false)

const page = ref(0)
const size = ref(15)
const sortField = ref(null)
const sortOrder = ref(null)

const props = defineProps({
  id: Number,
  nomeTabelaPreco: { type: String, default: '' }
})

watch(() => props.id, () => {
  load()
}, { immediate: true })

onMounted(async () => {
  loading.value = true

  loadProductTypes()
  loadProviders()

  loading.value = false
})

const data = ref([])
const totalRecords = ref(0)

async function load() {
  const query = {
    idTabelaPreco: props.id,
    page: page.value,
    size: size.value,
  }

  if (sortField.value) {
    query.sort = sortField.value

    if (sortOrder) {
      query.sort += sortOrder.value === 1 ? ",asc" : ",desc"
    }
  }

  loading.value = true

  try {
    const response = await api.get("/price-table-product/list-product", { params: query })

    totalRecords.value = response.data.totalElements
    data.value = response.data.content
  } catch (error) {
    toast.add({ severity: "error", summary: "Falha de Carga de Produtos", detail: "Requisição de lista de Produtos terminou com o erro: " + error.response.data, life: 10000 })
  } finally {
    loading.value = false
  }
}

function onPage(event) {
  page.value = event.page
  size.value = event.rows

  loading.value = true
  load()
  loading.value = false
}

function onSort(event) {
  page.value = 0
  sortField.value = event.sortField
  sortOrder.value = event.sortOrder

  loading.value = true
  load()
  loading.value = false
}

async function savePrices() {
  if (!id.value) return

  const payload = []

  for (let line of data.value) {
    payload.push({
      id: line.id,
      produto: { id: line.produto.id },
      tabela: { id: Number.parseInt(id.value) },
      preco: line.preco
    })
  }

  loading.value = true

  try {
    const response = await api.post('/price-table-product/save-prices', payload)

    if (response.status === 200) {
      toast.add({ severity: 'success', summary: 'Sucesso', detail: 'Lista de Preços atualizada com sucesso', life: 10000 })
    }
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Gravação de Preços', detail: 'Requisição de alteração de preços terminou com o erro: ' + error.response.data, life: 10000 })
  } finally {
    load()
    loading.value = false
  }
}

function cleanPrices() {
  for (let product of data.value) {
    product.preco = null
  }
}

let tipos = ref([])

async function loadProductTypes() {
  try {
    const response = await api.get('/product-type/list', { params: { page: 0, size: 10000, sort: 'nome,asc' } })

    tipos.value = response.data.content
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Tipos de Produto', detail: 'Requisição de lista de Tipos de Produto terminou com o erro: ' + error.response.data, life: 10000 })
  }
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

const emit = defineEmits(['close'])

function closeDialog() {
  emit('close')
}
</script>

<template>
  <BlockUI :blocked="loading" fullScreen>
    <Card>
      <template #title>
        <div class="flex justify-between items-center w-full">
          <h3>Lista de Tabelas de Preços{{ (nomeTabelaPreco.length ? ' - ' : '' ) + nomeTabelaPreco }}</h3>
          <Button icon="pi pi-times" severity="secondary" rounded text @click="closeDialog"/>
        </div>
      </template>
      <template #content>
        <DataTable :value="data" :lazy="true" :paginator="true" :rows="size" :totalRecords="totalRecords"
          :first="page * size" @page="onPage" @sort="onSort" :sortField="sortField" :sortOrder="sortOrder" responsiveLayout="scroll" stripedRows
          :rowsPerPageOptions="[15, 30, 60, 100]" size="small">

          <Column field="produto.id" header="Id" sortable/>
          <Column field="produto.nome" header="Nome" sortable/>
          <Column field="produto.referencia" header="Referência" sortable/>
          <Column field="produto.tipoProduto.nome" header="Tipo de Produto" sortable/>
          <Column field="produto.fornecedor.fantasia" header="Fornecedor" sortable/>
          <Column field="produto.peso" header="Peso (em gramas)" sortable/>
          <Column field="preco" header="Preço (R$)" headerClass="flex justify-center" bodyClass="flex justify-center" sortable>
            <template #body="slotProps">
              <InputNumber v-model="slotProps.data.preco" :minFractionDigits="2" :maxFractionDigits="2" :max="10000" size="small" :inputStyle="{'text-align': 'right'}"/>
            </template>
          </Column>
        </DataTable>
        <div class="flex justify-end gap-2 mt-4">
          <Button label="Limpar" icon="pi pi-times" @click="cleanPrices" severity="secondary" raised/>
          <Button label="Salvar" icon="pi pi-save" @click="savePrices" raised/>
        </div>
      </template>
    </Card>
  </BlockUI>
</template>
