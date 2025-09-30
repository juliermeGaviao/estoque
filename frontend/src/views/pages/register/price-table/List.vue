<script setup>
import api from '@/util/api'
import { useConfirm } from "primevue/useconfirm"
import { useToast } from 'primevue/usetoast'
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const toast = useToast()
const confirm = useConfirm()

const data = ref([])
const totalRecords = ref(0)
const loading = ref(false)

const page = ref(0)
const size = ref(20)
const sortField = ref(null)
const sortOrder = ref(null)

const nome = ref('')

async function load() {
  loading.value = true
  
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

    data.value = response.data.content
    totalRecords.value = response.data.totalElements
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Tabelas de Preços', detail: 'Requisição de lista de tabelas de preços terminou com o erro: ' + error.response.data, life: 10000 })
  } finally {
    loading.value = false
  }
}

onMounted(load)

function onPage(event) {
  page.value = event.page
  size.value = event.rows
  load()
}

function onSort(event) {
  sortField.value = event.sortField
  sortOrder.value = event.sortOrder
  load()
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
  if (entity?.id) {
    router.push(`/register/price-table/edit?id=${entity.id}`)
  } else {
    router.push('/register/price-table/edit')
  }
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
</script>

<template>
  <ConfirmDialog :closable="false"></ConfirmDialog>
  <BlockUI :blocked="loading" fullScreen>
    <Card>
      <template #title><h3>Lista de Tabelas de Preços</h3></template>
      <template #content>
        <Form class="flex gap-4 mb-4" @submit="onFilter" @reset="onClear">
          <FloatLabel variant="on">
            <label for="nome">Nome</label>
            <InputText id="nome" v-model="nome" autocomplete="off" fluid/>
          </FloatLabel>

          <Button label="Limpar" icon="pi pi-times" severity="secondary" type="reset" raised/>
          <Button label="Buscar" type="submit" icon="pi pi-search" raised/>
        </Form>

        <DataTable :value="data" :lazy="true" :paginator="true" :rows="size" :totalRecords="totalRecords"
          :first="page * size" @page="onPage" @sort="onSort" :sortField="sortField" :sortOrder="sortOrder" responsiveLayout="scroll" stripedRows
          :rowsPerPageOptions="[10, 20, 50, 100]">

          <Column field="id" header="Id" sortable/>
          <Column field="nome" header="Nome" sortable/>

          <Column :bodyStyle="{ textAlign: 'center' }">
            <template #header>
              <div style="width: 100%; display: flex; justify-content: center;">
                <Button icon="pi pi-plus" class="p-button-sm p-button-text p-mr-2" @click="edit(null)" v-tooltip.bottom="'Nova Tabela de Preços'"/>
              </div>
            </template>

            <template #body="slotProps">
              <Button icon="pi pi-pencil" class="p-button-sm p-button-text p-mr-2" @click="edit(slotProps.data)" v-tooltip.bottom="'Editar'"/>
              <Button icon="pi pi-trash" class="p-button-sm p-button-text p-button-danger" @click="confirmDelete(slotProps.data)" v-tooltip.bottom="'Remover'"/>
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>
  </BlockUI>
</template>
