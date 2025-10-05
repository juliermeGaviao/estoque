<script setup>
import api from '@/util/api'
import { zodResolver } from '@primevue/forms/resolvers/zod'
import { useToast } from 'primevue/usetoast'
import { onMounted, ref } from 'vue'
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
  loading.value = true

  try {
    const res = await api.get('/price-table', { params: { id: id.value } })

    if (form.value) {
      form.value.setValues({
        nome: res.data.nome
      })
    }
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Tabela de Preços', detail: 'Requisição de tabela de preços terminou com o erro: ' + error.response.data, life: 10000 })
  } finally {
    loading.value = false
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
    loading.value = false
  }
}

onMounted(() => {
  if (id.value) {
    load()
    loadProducts()
  }
})

const data = ref([])
const totalRecords = ref(0)

const page = ref(0)
const size = ref(20)
const sortField = ref(null)
const sortOrder = ref(null)

async function loadProducts() {
  const query = {
    idTabelaPreco: id.value,
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

  loadProducts()
}

function onSort(event) {
  sortField.value = event.sortField
  sortOrder.value = event.sortOrder

  loadProducts()
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
    loading.value = false
    loadProducts()
  }
}

function cleanPrices() {
  data.value.forEach(product => product.preco = null)
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
        <DataTable :value="data" :lazy="true" :paginator="true" :rows="size" :totalRecords="totalRecords"
          :first="page * size" @page="onPage" @sort="onSort" :sortField="sortField" :sortOrder="sortOrder" responsiveLayout="scroll" stripedRows
          :rowsPerPageOptions="[10, 20, 50, 100]">

          <Column field="produto.id" header="Id" sortable/>
          <Column field="produto.nome" header="Nome" sortable/>
          <Column field="produto.referencia" header="Referência" sortable/>
          <Column field="produto.tipoProduto.nome" header="Tipo de Produto" sortable/>
          <Column field="produto.fornecedor.fantasia" header="Fornecedor" sortable/>
          <Column field="produto.peso" header="Peso (em gramas)" sortable/>
          <Column field="preco" header="Preço" headerClass="flex justify-center" bodyClass="flex justify-center" sortable>
            <template #body="slotProps">
              <InputNumber v-model="slotProps.data.preco" :minFractionDigits="2" :maxFractionDigits="2" :max="10000" class="w-1/3"/>
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
