<script setup>
import api from '@/util/api'
import { useConfirm } from "primevue/useconfirm"
import { useToast } from 'primevue/usetoast'
import { nextTick, onMounted, ref } from 'vue'

const toast = useToast()
const confirm = useConfirm()

const data = ref([])
const totalRecords = ref(0)
const loading = ref(false)

const page = ref(0)
const size = ref(15)
const first = ref(0)
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

  loading.value = true

  try {
    const response = await api.get("/product/list", { params: query })

    data.value = response.data.content.map(item => ({
      ...item,
      editando: false,
      edicao: { nome: item.nome, idTipoProduto: item.tipoProduto.id, idFornecedor: item.fornecedor.id, referencia: item.referencia, peso: item.peso, estoque: item.estoque }
    }))

    totalRecords.value = response.data.totalElements
  } catch (error) {
    toast.add({ severity: "error", summary: "Falha de Carga de Produtos", detail: "Requisição de lista de Produtos terminou com o erro: " + error.response.data, life: 10000 })
  } finally {
    await nextTick()
    setTimeout(() => loading.value = false, 50)
  }
}

onMounted(async () => {
  load({})
  loadProductTypes()
  loadProviders()
})

async function onPage(event) {
  const result = await saveAll(false)

  if (result) {
    page.value = event.page
    size.value = event.rows
    first.value = event.first

    load( { ...filterValues.value } )
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
    first.value = 0
    page.value = 0
    sortField.value = event.sortField
    sortOrder.value = event.sortOrder

    load( { ...filterValues.value } )
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

function edit(entity) {
  entity.editando = true

  entity.edicao.nome = entity.nome
  entity.edicao.referencia = entity.referencia
  entity.edicao.idTipoProduto = entity.tipoProduto.id
  entity.edicao.idFornecedor = entity.fornecedor.id
  entity.edicao.peso = entity.peso
  entity.edicao.estoque = entity.estoque
}

const confirmDelete = entity => {
  confirm.require({
    message: 'Deseja remover o produto?',
    header: "Alerta",
    icon: 'pi pi-info-circle',
    rejectProps: {
      label: 'Cancelar',
      severity: 'secondary',
      raised: true
    },
    acceptProps: {
      label: 'Remover',
      severity: 'danger',
      raised: true
    },
    accept: async () => {
      try {
        await api.delete(`/product?id=${entity.id}`)

        toast.add({ severity: 'success', summary: 'Sucesso', detail: 'Produto removido com sucesso', life: 10000 })

        page.value = 0
        load( { ...filterValues.value } )
      } catch (error) {
        toast.add({ severity: 'error', summary: 'Falha de Remoção de Produto', detail: 'Requisição de remoção de produto terminou com o erro: ' + error.response.data, life: 10000 })
      }
    }
  })
}

let tipos = ref([])

async function loadProductTypes() {
  loading.value = true

  try {
    const response = await api.get('/product-type/list', { params: { page: 0, size: 10000, sort: 'nome,asc' } })

    tipos.value = response.data.content
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Tipos de Produto', detail: 'Requisição de lista de Tipos de Produto terminou com o erro: ' + error.response.data, life: 10000 })
  } finally {
    await nextTick()
    setTimeout(() => loading.value = false, 50)
  }
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
    await nextTick()
    setTimeout(() => loading.value = false, 50)
  }
}

const form = ref(null)
const formValues = ref({ nome: null, idTipoProduto: null, idFornecedor: null, referencia: null, minPeso: null, maxPeso: null })
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

function addItem() {
  data.value.unshift({
    id: null,
    nome: null,
    tipoProduto: { id: null },
    fornecedor: { id: null },
    referencia: null,
    peso: null,
    estoque: null,
    edicao: { nome: null, idTipoProduto: null, idFornecedor: null, referencia: null, peso: null, estoque: null },
    editando: true
  })
}

async function commit(item) {
  if (!item.edicao.nome || !item.edicao.nome.trim().length || !item.edicao.idTipoProduto || !item.edicao.idFornecedor
      || !item.edicao.referencia || !item.edicao.referencia.trim().length || !item.edicao.peso || item.edicao.peso <= 0) {
    toast.add({ severity: 'error', summary: 'Dados Insuficientes', detail: 'Nome, tipo de produto, fornecedor, referência e peso são obrigatórios.', life: 10000 })
    return
  }

  item.editando = false
  const { editando, edicao, ...atributos } = item

  atributos.nome = edicao.nome
  atributos.tipoProduto.id = edicao.idTipoProduto
  atributos.fornecedor.id = edicao.idFornecedor
  atributos.referencia = edicao.referencia
  atributos.peso = edicao.peso
  atributos.estoque = edicao.estoque

  loading.value = true

  try {
    const response = await api.post('/product', atributos)

    if (response.status === 200) {
      toast.add({ severity: 'success', summary: 'Sucesso', detail: `Produto ${item.id ? 'atualizado' : 'criado'} com sucesso`, life: 10000 })

      load( { ...filterValues.value } )
    }
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Gravação de Produto', detail: `Requisição de ${item.id ? 'alteração' : 'criação'} de produto terminou com o erro: ` + error.response.data, life: 10000 })
  } finally {
    await nextTick()
    setTimeout(() => loading.value = false, 50)
  }
}

function cancel(item) {
  item.editando = false

  if (!item.id) {
    data.value.splice(data.value.indexOf(item), 1)
  }
}

async function saveAll(emitirMensagem) {
  for (const item of data.value) {
    if (item.editando) {
      if (!item.edicao.nome || !item.edicao.nome.trim().length || !item.edicao.idTipoProduto || !item.edicao.idFornecedor
          || !item.edicao.referencia || !item.edicao.referencia.trim().length || !item.edicao.peso || !item.edicao.peso > 0) {
        toast.add({ severity: 'error', summary: 'Dados Insuficientes', detail: 'Nome, tipo de produto, fornecedor, referência e peso são obrigatórios.', life: 10000 })
        return false
      }
    } else if (item.edicao.estoque === null || item.edicao.estoque < 0) {
      toast.add({ severity: 'error', summary: 'Dados Insuficientes', detail: 'Unidades em estoque é obrigatório.', life: 10000 })
      return false
    }
  }

  data.value.forEach(item => {
    if (item.editando) {
      item.nome = item.edicao.nome
      item.tipoProduto.id = item.edicao.idTipoProduto
      item.fornecedor.id = item.edicao.idFornecedor
      item.referencia = item.edicao.referencia
      item.peso = item.edicao.peso
      item.editando = false
    }

    item.estoque = item.edicao.estoque - item.estoque
  })

  loading.value = true

  try {
    const response = await api.post('/product/save-all', data.value)

    if (response.status === 200) {
      if (emitirMensagem) {
        toast.add({ severity: 'success', summary: 'Sucesso', detail: `Produtos salvos com sucesso`, life: 10000 })
      }
    }
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Gravação de Produto', detail: `Requisição de salvamento de produtos terminou com o erro: ` + error.response.data, life: 10000 })

    return false
  } finally {
    await nextTick()
    setTimeout(() => loading.value = false, 50)
  }

  return true
}

async function clickAndSaveAll() {
  const result = await saveAll(true)

  if (result) {
    load( { ...filterValues.value } )
  }
}

const pop = ref()
const salePoints = ref()

const togglePopover = async (event, product) => {
  pop.value.toggle(event)

  try {
    const response = await api.get('/product/stock-product-sale-point', { params: { idProduto: product.id } })

    if (response.status === 200) {
      salePoints.value = response.data
    }
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Estoque', detail: `Requisição de obtenção do estoque detalhado do produto terminou com o erro: ` + error.response.data, life: 10000 })
  }
}

</script>

<template>
  <ConfirmDialog :closable="false"></ConfirmDialog>
  <BlockUI :blocked="loading" fullScreen>
    <Card>
      <template #title><h3>Lista de Produtos</h3></template>
      <template #content>
        <Form ref="form" :initialValues="formValues" @submit="filter" @reset="limpar" class="grid flex flex-column gap-2 mb-4">
          <div class="grid grid-cols-12 gap-2">
            <div class="col-span-4">
              <FormField name="nome">
                <FloatLabel variant="on">
                  <InputText id="nome" maxlength="255" autocomplete="off" fluid/>
                  <label for="nome">Nome</label>
                </FloatLabel>
              </FormField>
            </div>
            <div class="col-span-4">
              <FormField name="idTipoProduto">
                <FloatLabel variant="on">
                  <Select :options="tipos" optionLabel="nome" optionValue="id" fluid/>
                  <label for="idTipoProduto">Tipo do Produto</label>
                </FloatLabel>
              </FormField>
            </div>
            <div class="col-span-4">
              <FormField name="idFornecedor">
                <FloatLabel variant="on">
                  <Select :options="fornecedores" optionLabel="fantasia" optionValue="id" fluid/>
                  <label for="idFornecedor">Fornecedores</label>
                </FloatLabel>
              </FormField>
            </div>
          </div>
          <div class="grid grid-cols-12 gap-2">
            <div class="col-span-4">
              <FormField name="referencia">
                <FloatLabel variant="on">
                  <InputText id="referencia" maxlength="100" autocomplete="off" fluid/>
                  <label for="referencia">Referência</label>
                </FloatLabel>
              </FormField>
            </div>
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
            <div class="col-span-2">
              <FormField class="flex justify-end gap-2">
                <Button label="Limpar" icon="pi pi-times" type="reset" severity="secondary" raised/>
                <Button label="Buscar" icon="pi pi-search" type="submit" raised/>
              </FormField>
            </div>
          </div>
        </Form>

        <DataTable :value="data" :lazy="true" :paginator="true" :rows="size" :totalRecords="totalRecords"
          :first="first" @page="onPage" @sort="onSort" :sortField="sortField" :sortOrder="sortOrder" responsiveLayout="scroll" stripedRows
          :rowsPerPageOptions="[15, 30, 60, 100]" size="small" class="mt-6">

          <Column field="id" header="Id" sortable><template #body="slotProps">{{slotProps.data.id}}</template></Column>
          <Column field="nome" header="Nome" sortable>
            <template #body="slotProps">
              <div v-if="!slotProps.data.editando">{{slotProps.data.nome}}</div>
              <div v-if="slotProps.data.editando">
                <InputText v-model="slotProps.data.edicao.nome" maxlength="255" autocomplete="off" fluid/>
              </div>
            </template>
          </Column>
          <Column field="referencia" header="Referência" sortable>
            <template #body="slotProps">
              <div v-if="!slotProps.data.editando">{{slotProps.data.referencia}}</div>
              <div v-if="slotProps.data.editando">
                <InputText v-model="slotProps.data.edicao.referencia" maxlength="100" autocomplete="off" fluid/>
              </div>
            </template>
          </Column>
          <Column field="tipoProduto.nome" header="Tipo de Produto" sortable>
            <template #body="slotProps">
              <div v-if="!slotProps.data.editando">{{slotProps.data.tipoProduto.nome}}</div>
              <div v-if="slotProps.data.editando">
                <Select v-model="slotProps.data.edicao.idTipoProduto" :options="tipos" optionLabel="nome" optionValue="id" fluid/>
              </div>
            </template>
          </Column>
          <Column field="fornecedor.fantasia" header="Fornecedor" sortable>
            <template #body="slotProps">
              <div v-if="!slotProps.data.editando">{{slotProps.data.fornecedor.fantasia}}</div>
              <div v-if="slotProps.data.editando">
                <Select v-model="slotProps.data.edicao.idFornecedor" :options="fornecedores" optionLabel="fantasia" optionValue="id" fluid/>
              </div>
            </template>
          </Column>
          <Column field="peso" header="Peso (em gramas)" sortable>
            <template #body="slotProps">
              <div v-if="!slotProps.data.editando">{{slotProps.data.peso}}</div>
              <div v-if="slotProps.data.editando">
                <InputNumber v-model="slotProps.data.edicao.peso" :min="1" :max="10000" fluid/>
              </div>
            </template>
          </Column>
          <Column field="estoque" header="Estoque">
            <template #body="slotProps">
              {{ slotProps.data.estoque }}&nbsp;
              <i ref="infoIcon" class="pi pi-info-circle" @click="togglePopover($event, slotProps.data)" style="cursor: pointer; color: black;"/>
            </template>
          </Column>

          <Column headerClass="flex justify-center" bodyClass="flex justify-center">
            <template #header>
              <Button icon="pi pi-plus" class="p-button-sm p-button-text p-mr-2" @click="addItem" v-tooltip.bottom="'Novo Produto'"/>
            </template>

            <template #body="slotProps">
              <Button icon="pi pi-pencil" class="p-button-sm p-button-text p-mr-2" @click="edit(slotProps.data)" v-tooltip.bottom="'Editar'" v-if="!slotProps.data.editando"/>
              <Button icon="pi pi-trash" class="p-button-sm p-button-text p-button-danger" @click="confirmDelete(slotProps.data)" v-tooltip.bottom="'Remover'" v-if="!slotProps.data.editando"/>
              <Button icon="pi pi-check" class="p-button-sm p-button-text p-mr-2" @click="commit(slotProps.data)" v-tooltip.bottom="'Consolidar'" v-if="slotProps.data.editando"/>
              <Button icon="pi pi-times" class="p-button-sm p-button-text p-mr-2" @click="cancel(slotProps.data)" v-tooltip.bottom="'Cancelar'" v-if="slotProps.data.editando"/>
            </template>
          </Column>
        </DataTable>

        <Popover ref="pop">
          <div v-if="salePoints && salePoints.length">
            <div v-for="item in salePoints" :key="item.id">
              <b>{{ item.pontoVenda?.nome }}</b>: {{ item.saldo }}
            </div>
          </div>

          <div v-else-if="salePoints && salePoints.length === 0">
            <span>Produto sem estoque.</span>
          </div>

          <div v-else class="p-3 text-center text-gray-400 flex flex-column items-center justify-center gap-2">
            <i class="pi pi-spin pi-spinner text-2xl"></i>
            <span class="text-xs font-medium">Buscando estoques...</span>
          </div>
        </Popover>

        <div class="flex justify-end mt-4">
          <Button label="Salvar" icon="pi pi-save" raised @click="clickAndSaveAll"/>
        </div>
      </template>
    </Card>
  </BlockUI>
</template>
