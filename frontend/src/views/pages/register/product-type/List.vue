<script setup>
import api from '@/util/api'
import { useConfirm } from "primevue/useconfirm"
import { useToast } from 'primevue/usetoast'
import { nextTick, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const toast = useToast()
const confirm = useConfirm()

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

  loading.value = true

  try {
    const response = await api.get('/product-type/list', { params: query })

    data.value = response.data.content.map(item => ({
      ...item,
      editando: false,
      edicao: { ...item }
    }))

    totalRecords.value = response.data.totalElements
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Tipos de Produtos', detail: 'Requisição de lista de tipos de produtos terminou com o erro: ' + error.response.data, life: 10000 })
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  load({})
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

function edit(entity) {
  entity.edicao.nome = entity.nome
  entity.editando = true
}

const confirmDelete = entity => {
  confirm.require({
    message: 'Deseja remover o tipo de produto?',
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
        await api.delete(`/product-type?id=${entity.id}`)

        toast.add({ severity: 'success', summary: 'Sucesso', detail: 'Tipo de Produto removida com sucesso', life: 10000 })

        load()
      } catch (error) {
        toast.add({ severity: 'error', summary: 'Falha de Remoção de Tipo de Produto', detail: 'Requisição de remoção de tipo de produto terminou com o erro: ' + error.response.data, life: 10000 })
      }
    }
  })
}

const form = ref(null)
const formValues = ref({ nome: null })
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
  data.value.push({
    id: null,
    nome: '',
    edicao: { id: null, rnome: '' },
    editando: true
  })
}

async function commit(item) {
  if (!item.edicao.nome || !item.edicao.nome.trim().length) {
    toast.add({ severity: 'error', summary: 'Dados Insuficientes', detail: 'Nome de tipo de produto é obrigatório.', life: 10000 })
    return
  }

  item.nome = item.edicao.nome
  item.editando = false

  try {
    const response = await api.post('/product-type', { id: item.id, nome: item.nome})

    if (response.status === 200) {
      toast.add({ severity: 'success', summary: 'Sucesso', detail: `Tipo de Produto ${item.id ? 'atualizado' : 'criado'} com sucesso`, life: 10000 })
      item.id = response.data.id
    }
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Gravação de Tipo de Produto', detail: 'Requisição de alteração de tipo de produto terminou com o erro: ' + error.response.data, life: 10000 })
  } finally {
    loading.value = false
  }
}

function cancel(item) {
  item.editando = false

  if (!item.id) {
    data.value.splice(data.value.indexOf(item), 1)
  }
}

</script>

<template>
  <ConfirmDialog :closable="false"></ConfirmDialog>
  <BlockUI :blocked="loading" fullScreen>
    <Card>
      <template #title><h3>Lista de Tipos de Produtos</h3></template>
      <template #content>
        <Form ref="form" :initialValues="formValues" @submit="filter" @reset="limpar" class="grid flex flex-column gap-2 mb-4">
          <div class="grid grid-cols-12 gap-2">
            <div class="col-span-10">
              <FormField name="nome">
                <FloatLabel variant="on">
                  <InputText id="nome" maxlength="255" autocomplete="off" fluid/>
                  <label for="nome">Nome</label>
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
          <Column field="nome" header="Nome">
            <template #body="slotProps">
              <div v-show="!slotProps.data.editando">{{slotProps.data.nome}}</div>
              <div v-show="slotProps.data.editando">
                <InputText v-model="slotProps.data.edicao.nome" maxlength="255" autocomplete="off" fluid/>
              </div>
            </template>
          </Column>

          <Column headerClass="flex justify-center" bodyClass="flex justify-center">
            <template #header>
              <Button icon="pi pi-plus" class="p-button-sm p-button-text p-mr-2" @click="addItem" v-tooltip.bottom="'Novo Tipo de Produto'"/>
            </template>

            <template #body="slotProps">
              <Button icon="pi pi-pencil" class="p-button-sm p-button-text p-mr-2" @click="edit(slotProps.data)" v-tooltip.bottom="'Editar'" v-show="!slotProps.data.editando"/>
              <Button icon="pi pi-trash" class="p-button-sm p-button-text p-button-danger" @click="confirmDelete(slotProps.data)" v-tooltip.bottom="'Remover'" v-show="!slotProps.data.editando"/>
              <Button icon="pi pi-check" class="p-button-sm p-button-text p-mr-2" @click="commit(slotProps.data)" v-tooltip.bottom="'Consolidar'" v-show="slotProps.data.editando"/>
              <Button icon="pi pi-times" class="p-button-sm p-button-text p-mr-2" @click="cancel(slotProps.data)" v-tooltip.bottom="'Cancelar'" v-show="slotProps.data.editando"/>
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>
  </BlockUI>
</template>
