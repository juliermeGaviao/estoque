<script setup>
import ProductPriceListComponent from '@/components/ProductPriceListComponent.vue'
import api from '@/util/api'
import { useConfirm } from "primevue/useconfirm"
import { useToast } from 'primevue/usetoast'
import { nextTick, onMounted, ref } from 'vue'

const toast = useToast()
const confirm = useConfirm()

const data = ref([])
const totalRecords = ref(0)

const page = ref(0)
const size = ref(15)
const first = ref(0)
const sortField = ref(null)
const sortOrder = ref(null)

const nome = ref('')

async function load() {
  let params = {
    page: page.value,
    size: size.value
  }

  if (sortField.value) {
    params.sort = sortField.value

    if (sortOrder) {
      params.sort += sortOrder.value === 1 ? ',asc' : ',desc'
    }
  }

  if (nome.value) {
    params.nome = nome.value
  }

  try {
    const response = await api.get('/price-table/list', { params: params })

    data.value = response.data.content.map(item => ({
      ...item,
      editando: false,
      edicao: { nome: item.nome }
    }))

    data.value = response.data.content
    totalRecords.value = response.data.totalElements
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Tabelas de Preços', detail: 'Requisição de lista de tabelas de preços terminou com o erro: ' + error.response.data, life: 10000 })
  }
}

onMounted(async () => {
  load()
})

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

function onFilter() {
  page.value = 0
  load()
}

function onClear() {
  nome.value = null
  load()
}

function edit(entity) {
  entity.editando = true

  entity.edicao = { nome: entity.nome }
}

const confirmDelete = entity => {
  confirm.require({
    message: 'Deseja remover a tabela de preços?',
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
        await api.delete(`/price-table?id=${entity.id}`)

        toast.add({ severity: 'success', summary: 'Sucesso', detail: 'Tabela de Preços removida com sucesso', life: 10000 })

        load()
      } catch (error) {
        toast.add({ severity: 'error', summary: 'Falha de Remoção de Tabela de Preços', detail: 'Requisição de remoção de tabela de preços terminou com o erro: ' + error.response.data, life: 10000 })
      }
    }
  })
}

function addItem() {
  data.value.push({
    id: null,
    nome: null,
    edicao: { nome: null },
    editando: true
  })
}

async function commit(item) {
  if (!item.edicao.nome || !item.edicao.nome.trim().length) {
    toast.add({ severity: 'error', summary: 'Dados Insuficientes', detail: 'Nome é obrigatório.', life: 10000 })
    return
  }

  item.editando = false
  item.nome = item.edicao.nome

  try {
    const response = await api.post('/price-table', item)

    if (response.status === 200) {
      toast.add({ severity: 'success', summary: 'Sucesso', detail: `Tabela de preços ${item.id ? 'atualizada' : 'criada'} com sucesso`, life: 10000 })

      load()
    }
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Gravação de Produto', detail: `Requisição de ${item.id ? 'alteração' : 'criação'} de tabela de preços terminou com o erro: ` + error.response.data, life: 10000 })
  }
}

function cancel(item) {
  item.editando = false

  if (!item.id) {
    data.value.splice(data.value.indexOf(item), 1)
  }
}

const visible = ref(false)
const idTabelaPreco = ref(null)
const nomeSelecionado = ref('')

function openTable(item) {
  idTabelaPreco.value = item.id
  nomeSelecionado.value = item.nome

  visible.value = true
}

async function saveAll(emitirMensagem) {
  for (const item of data.value) {
    if (item.editando) {
      if (!item.edicao.nome || !item.edicao.nome.trim().length) {
        toast.add({ severity: 'error', summary: 'Dados Insuficientes', detail: 'Nome é obrigatório.', life: 10000 })
        return false
      }
    }
  }

  data.value.forEach(item => {
    if (item.editando) {
      item.nome = item.edicao.nome
      item.editando = false
    }
  })

  try {
    const response = await api.post('/price-table/save-all', data.value)

    if (response.status === 200) {
      if (emitirMensagem) {
        toast.add({ severity: 'success', summary: 'Sucesso', detail: `Tabelas de preços salvas com sucesso`, life: 10000 })
      }
    }
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Gravação de Produto', detail: `Requisição de salvamento de tabelas de preços terminou com o erro: ` + error.response.data, life: 10000 })

    return false
  }

  return true
}

async function clickAndSaveAll() {
  const result = await saveAll(true)

  if (result) {
    load()
  }
}

</script>

<template>
  <ConfirmDialog :closable="false"></ConfirmDialog>
  <Card>
    <template #title><h3>Lista de Tabelas de Preços</h3></template>
    <template #content>
      <Form class="flex gap-2 mb-4" @submit="onFilter" @reset="onClear">
        <FloatLabel variant="on">
          <label for="nome">Nome</label>
          <InputText id="nome" v-model="nome" autocomplete="off" fluid/>
        </FloatLabel>

        <Button label="Limpar" icon="pi pi-times" severity="secondary" type="reset" raised/>
        <Button label="Buscar" type="submit" icon="pi pi-search" raised/>
      </Form>

      <DataTable :value="data" :lazy="true" :paginator="true" :rows="size" :totalRecords="totalRecords"
        :first="first" @page="onPage" @sort="onSort" :sortField="sortField" :sortOrder="sortOrder" responsiveLayout="scroll" stripedRows
        :rowsPerPageOptions="[15, 30, 60, 100]" size="small">

        <Column field="id" header="Id" sortable/>
        <Column field="nome" header="Nome" sortable>
          <template #body="slotProps">
            <div v-if="!slotProps.data.editando">{{slotProps.data.nome}}</div>
            <div v-if="slotProps.data.editando">
              <InputText v-model="slotProps.data.edicao.nome" maxlength="255" autocomplete="off" fluid/>
            </div>
          </template>
        </Column>

        <Column headerClass="flex justify-center" bodyClass="flex justify-center">
          <template #header>
            <Button icon="pi pi-plus" class="p-button-sm p-button-text p-mr-2" @click="addItem" v-tooltip.bottom="'Nova Tabela de Preços'"/>
          </template>

          <template #body="slotProps">
            <Button icon="pi pi-dollar" class="p-button-sm p-button-text p-mr-2" @click="openTable(slotProps.data)" v-tooltip.bottom="'Preencher Preços'" v-if="slotProps.data.id"/>
            <Button icon="pi pi-pencil" class="p-button-sm p-button-text p-mr-2" @click="edit(slotProps.data)" v-tooltip.bottom="'Editar'" v-if="!slotProps.data.editando"/>
            <Button icon="pi pi-trash" class="p-button-sm p-button-text p-button-danger" @click="confirmDelete(slotProps.data)" v-tooltip.bottom="'Remover'" v-if="!slotProps.data.editando"/>
            <Button icon="pi pi-check" class="p-button-sm p-button-text p-mr-2" @click="commit(slotProps.data)" v-tooltip.bottom="'Consolidar'" v-if="slotProps.data.editando"/>
            <Button icon="pi pi-times" class="p-button-sm p-button-text p-mr-2" @click="cancel(slotProps.data)" v-tooltip.bottom="'Cancelar'" v-if="slotProps.data.editando"/>
          </template>
        </Column>
      </DataTable>

      <div class="flex justify-end mt-4">
        <Button label="Salvar" icon="pi pi-save" raised @click="clickAndSaveAll"/>
      </div>
    </template>
  </Card>
  <Dialog v-model:visible="visible" modal :closable="false" style="width: 90%">
    <ProductPriceListComponent v-if="visible" :id="idTabelaPreco" :nomeTabelaPreco="nomeSelecionado" @close="visible = false"/>
  </Dialog>
</template>
