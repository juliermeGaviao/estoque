<script setup>
import api from '@/util/api'
import { useToast } from 'primevue/usetoast'
import { defineEmits, defineProps, nextTick, onMounted, ref, watch } from 'vue'

const toast = useToast()

const page = ref(0)
const size = ref(15)
const first = ref(0)
const sortField = ref(null)
const sortOrder = ref(null)

const props = defineProps({
  id: Number,
  nomeTabelaPreco: { type: String, default: '' }
})

watch(() => props.id, async () => {
  load()
}, { immediate: true })

onMounted(async () => {
  loadProductTypes()
  loadProviders()
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

  try {
    const response = await api.get("/price-table-product/list-product", { params: query })

    totalRecords.value = response.data.totalElements
    data.value = response.data.content
  } catch (error) {
    toast.add({ severity: "error", summary: "Falha de Carga de Produtos", detail: "Requisição de lista de Produtos terminou com o erro: " + error.response.data, life: 10000 })
  }
}

async function onPage(event) {
  const result = await saveAll(false)

  if (result) {
    page.value = event.page
    size.value = event.rows
    first.value = event.first

    load()
  } else {
    const currentFirst = page.value * size.value

    first.value = -1
    await nextTick()
    first.value = currentFirst
  }
}

async function onSort(event) {
  const result = await saveAll(false)

  if (result) {
    page.value = 0
    sortField.value = event.sortField
    sortOrder.value = event.sortOrder

    load()
  } else {
    const oldField = sortField.value
    const oldOrder = sortOrder.value
    const oldFirst = page.value * size.value

    sortField.value = oldField === null ? undefined : null 
    sortOrder.value = 0 
    first.value = -1

    await nextTick()

    sortField.value = oldField
    sortOrder.value = oldOrder
    first.value = oldFirst

    if (event.originalEvent) {
      event.originalEvent.preventDefault();
    }
  }
}

async function saveAll(emitirAviso) {
  for (const item of data.value) {
    if (item.preco === null || item.preco < 0) {
      toast.add({ severity: 'error', summary: 'Dados Insuficientes', detail: 'Preço é obrigatório.', life: 10000 })
      return false
    }
  }

  const payload = data.value.map(item => ({
      id: item.id,
      produto: { id: item.produto.id },
      tabela: { id: props.id },
      preco: item.preco
  }))

  try {
    const response = await api.post('/price-table-product/save-prices', payload)

    if (response.status === 200 && emitirAviso === true) {
      toast.add({ severity: 'success', summary: 'Sucesso', detail: 'Lista de Preços atualizada com sucesso', life: 10000 })
    }
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Gravação de Preços', detail: 'Requisição de alteração de preços terminou com o erro: ' + error.response.data, life: 10000 })
  }

  return true
}

async function clickAndSaveAll() {
  const result = await saveAll(true)

  if (result) {
    load()
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
  <Card>
    <template #title>
      <div class="flex justify-between items-center w-full">
        <h3>Lista de Tabelas de Preços{{ (nomeTabelaPreco.length ? ' - ' : '' ) + nomeTabelaPreco }}</h3>
        <Button icon="pi pi-times" severity="secondary" rounded text @click="closeDialog"/>
      </div>
    </template>
    <template #content>
      <DataTable :value="data" :lazy="true" :paginator="true" :rows="size" :totalRecords="totalRecords"
        :first="first" @page="onPage" @sort="onSort" :sortField="sortField" :sortOrder="sortOrder" responsiveLayout="scroll" stripedRows
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
        <Button label="Fechar" icon="pi pi-times-circle" @click="closeDialog" severity="secondary" raised/>
        <Button label="Limpar" icon="pi pi-times" @click="cleanPrices" severity="secondary" raised/>
        <Button label="Salvar" icon="pi pi-save" @click="clickAndSaveAll" raised/>
      </div>
    </template>
  </Card>
</template>
