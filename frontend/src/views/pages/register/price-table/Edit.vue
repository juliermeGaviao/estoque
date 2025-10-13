<script setup>
import api from '@/util/api'
import { zodResolver } from '@primevue/forms/resolvers/zod'
import { useToast } from 'primevue/usetoast'
import { nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { z } from 'zod'

const router = useRouter()
const route = useRoute()
const toast = useToast()
const loading = ref(false)

const form = ref(null)
const formValues = ref({ nome: '' })

const formValidator = zodResolver(
  z.object({
    nome: z.string().min(1, { message: 'Nome do Tabela de Preços é obrigatório.' })
  })
)

const id = ref(route.query.id)

async function load() {
  try {
    const res = await api.get('/price-table', { params: { id: id.value } })

    if (form.value) {
      form.value.setValues({
        nome: res.data.nome
      })
    }
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Tabela de Preços', detail: 'Requisição de tabela de preços terminou com o erro: ' + error.response.data, life: 10000 })
  }
}

const save = async ({ valid, values }) => {
  if (!valid) return

  let params = { ... values }

  for (let param in params) {
    if (typeof params[param] === 'string') {
      params[param] = params[param].trim()
    }
  }

  params['id'] = parseInt(id.value)

  loading.value = true

  try {
    const response = await api.post('/price-table', params)

    if (response.status === 200) {
      id.value = response.data.id

      toast.add({ severity: 'success', summary: 'Sucesso', detail: 'Tabela de Preços atualizado com sucesso', life: 10000 })
    }
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Gravação de Tabela de Preços', detail: 'Requisição de alteração de tabela de preços terminou com o erro: ' + error.response.data, life: 10000 })
  } finally {
    loadProducts( { ...productFormValues.value } )
    loading.value = false
  }
}

onMounted(() => {
  loading.value = true

  if (id.value) {
    load()
    loadProducts( { ...productFormValues.value } )
  }

  loadProductTypes()
  loadProviders()

  loading.value = false
})

const data = ref([])
const totalRecords = ref(0)

const page = ref(0)
const size = ref(15)
const sortField = ref(null)
const sortOrder = ref(null)

async function loadProducts(params) {
  const query = {
    idTabelaPreco: id.value,
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

  try {
    const response = await api.get("/price-table-product/list-product", { params: query })

    totalRecords.value = response.data.totalElements
    data.value = response.data.content
  } catch (error) {
    toast.add({ severity: "error", summary: "Falha de Carga de Produtos", detail: "Requisição de lista de Produtos terminou com o erro: " + error.response.data, life: 10000 })
  }
}

function onPage(event) {
  page.value = event.page
  size.value = event.rows

  loading.value = true
  loadProducts( { ...filterValues.value } )
  loading.value = false
}

function onSort(event) {
  page.value = 0
  sortField.value = event.sortField
  sortOrder.value = event.sortOrder

  loading.value = true
  loadProducts( { ...filterValues.value } )
  loading.value = false
}

async function savePrices() {
  if (!id.value) return

  const payload = []

  data.value.forEach(line => {
    payload.push({
      id: line.id,
      produto: { id: line.produto.id },
      tabela: { id: parseInt(id.value) },
      preco: line.preco
    })
  })

  loading.value = true

  try {
    const response = await api.post('/price-table-product/save-prices', payload)

    if (response.status === 200) {
      toast.add({ severity: 'success', summary: 'Sucesso', detail: 'Lista de Preços atualizada com sucesso', life: 10000 })
    }
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Gravação de Preços', detail: 'Requisição de alteração de preços terminou com o erro: ' + error.response.data, life: 10000 })
  } finally {
    loadProducts( { ...filterValues.value } )
    loading.value = false
  }
}

function cleanPrices() {
  data.value.forEach(product => product.preco = null)
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

const productForm = ref(null)
const productFormValues = ref({ nome: null, idTipoProduto: null, idFornecedor: null, referencia: null, minPeso: null, maxPeso: null, minPreco: null, maxPreco: null })
const filterValues = ref({ ... productFormValues.value })

const filter = async ({ valid, values }) => {
  if (!valid) return

  filterValues.value = { ...values }
  page.value = 0

  loadProducts( { ...filterValues.value } )
}

function limpar() {
  nextTick(() => {
    page.value = 0
    filterValues.value = { ... productFormValues.value }
    sortField.value = null
    loadProducts( { ...filterValues.value } )
  })
}
</script>

<template>
  <ConfirmDialog :closable="false"></ConfirmDialog>
  <BlockUI :blocked="loading" fullScreen>
    <Card class="mb-4">
      <template #title>
        <div class="grid grid-cols-2">
          <h3>{{ id ? 'Editar' : 'Inserir' }} Tabela de Preços</h3>
          <div class="flex justify-end items-center">
            <Button icon="pi pi-replay" @click="router.push('/register/price-table')" class="p-button-text" v-tooltip.bottom="'Voltar'"/>
          </div>
        </div>
      </template>

      <template #content>
        <Form ref="form" :resolver="formValidator" :initialValues="formValues" @submit="save" class="grid flex flex-column gap-4">
          <FormField v-slot="$field" name="nome">
            <FloatLabel variant="on">
              <InputText id="nome" maxlength="255" autocomplete="off" fluid/>
              <label for="nome">Nome da Tabela de Preços</label>
            </FloatLabel>
            <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
          </FormField>

          <FormField class="flex justify-end gap-4">
            <Button label="Limpar" icon="pi pi-times" type="reset" severity="secondary" raised/>
            <Button label="Salvar" icon="pi pi-save" type="submit" raised/>
          </FormField>
        </Form>
      </template>
    </Card>
    <Card class="mb-4">
      <template #title>
        <div class="grid grid-cols-2">
          <h3>Lista de Produtos</h3>
          <div class="flex justify-end items-center">
            <Button icon="pi pi-replay" @click="router.push('/register/price-table')" class="p-button-text" v-tooltip.bottom="'Voltar'"/>
          </div>
        </div>
      </template>

      <template #content>
        <Form ref="productForm" :initialValues="productFormValues" @submit="filter" @reset="limpar" class="grid flex flex-column gap-4 mb-4">
          <div class="grid grid-cols-12 gap-4">
            <div class="col-span-3">
              <FormField name="nome">
                <FloatLabel variant="on">
                  <InputText id="nome" maxlength="255" autocomplete="off" fluid/>
                  <label for="nome">Nome</label>
                </FloatLabel>
              </FormField>
            </div>
            <div class="col-span-3">
              <FormField name="referencia">
                <FloatLabel variant="on">
                  <InputText id="referencia" maxlength="100" autocomplete="off" fluid/>
                  <label for="referencia">Referência</label>
                </FloatLabel>
              </FormField>
            </div>
            <div class="col-span-3">
              <FormField name="idTipoProduto">
                <FloatLabel variant="on">
                  <Select :options="tipos" optionLabel="nome" optionValue="id" fluid/>
                  <label for="idTipoProduto">Tipo do Produto</label>
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
          </div>
          <div class="grid grid-cols-12 gap-4">
            <div class="col-span-3">
              <FormField name="minPeso">
                <FloatLabel variant="on">
                  <InputNumber id="minPeso" :max="10000" fluid/>
                  <label for="minPeso">Peso Mínimo (g)</label>
                </FloatLabel>
              </FormField>
            </div>
            <div class="col-span-3">
              <FormField name="maxPeso">
                <FloatLabel variant="on">
                  <InputNumber id="maxPeso" :max="10000" fluid/>
                  <label for="maxPeso">Peso Máximo (g)</label>
                </FloatLabel>
              </FormField>
            </div>
            <div class="col-span-3">
              <FormField name="minPreco">
                <FloatLabel variant="on">
                  <InputNumber id="minPreco" :max="10000" fluid/>
                  <label for="minPreco">Preco Mínimo</label>
                </FloatLabel>
              </FormField>
            </div>
            <div class="col-span-3">
              <FormField name="maxPreco">
                <FloatLabel variant="on">
                  <InputNumber id="maxPreco" :max="10000" fluid/>
                  <label for="maxPreco">Preco Máximo</label>
                </FloatLabel>
              </FormField>
            </div>
          </div>
          <div class="grid grid-cols-12 gap-4">
            <div class="col-span-12">
              <FormField class="flex justify-end gap-4">
                <Button label="Limpar" icon="pi pi-times" type="reset" severity="secondary" raised/>
                <Button label="Buscar" icon="pi pi-search" type="submit" raised/>
              </FormField>
            </div>
          </div>
        </Form>

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
        <div class="flex justify-end gap-4 mt-4">
          <Button label="Limpar" icon="pi pi-times" @click="cleanPrices" severity="secondary" raised/>
          <Button label="Salvar" icon="pi pi-save" @click="savePrices" raised/>
        </div>
      </template>
    </Card>
  </BlockUI>
</template>
