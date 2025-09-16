<script setup>
import api from '@/util/api'
import { formatCpfCnpj, formatDate, formatPhone } from '@/util/util'
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

async function load() {
  loading.value = true

  try {
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

    const response = await api.get('/provider/list', { params: params })

    data.value = response.data.content
    totalRecords.value = response.data.totalElements
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Fornecedores', detail: 'Requisição de lista de fornecedores terminou com o erro: ' + error.response.data, life: 10000 })
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
  load()
}

function editUser(user) {
  if (user?.id) {
    router.push(`/register/provider/edit?id=${user.id}`)
  } else {
    router.push('/register/provider/edit')
  }
}

const confirmDelete = user => {
  confirm.require({
    message: 'Deseja remover o usuário?',
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
        await api.delete(`/user?id=${user.id}`)

        toast.add({ severity: 'success', summary: 'Sucesso', detail: 'Usuário removido com sucesso', life: 10000 })

        load()
      } catch (error) {
        toast.add({ severity: 'error', summary: 'Falha de Remoção de Usuário', detail: 'Requisição de remoção de usuário terminou com o erro: ' + error.response.data, life: 10000 })
      }
    }
  })
}
</script>

<template>
  <ConfirmDialog></ConfirmDialog>
  <BlockUI :blocked="loading" fullScreen>
    <Card>
      <template #title><h3>Lista de Fornecedores</h3></template>
      <template #content>
        <Form class="flex gap-4 mb-4" @submit="onFilter" @reset="onClear">
          <Button label="Buscar" type="submit" icon="pi pi-search" raised/>
        </Form>

        <DataTable :value="data" :lazy="true" :paginator="true" :rows="size" :totalRecords="totalRecords"
          :first="page * size" @page="onPage" @sort="onSort" :sortField="sortField" :sortOrder="sortOrder" responsiveLayout="scroll" stripedRows
          :rowsPerPageOptions="[10, 20, 50, 100]">

          <Column field="id" header="Id" sortable/>
          <Column field="razaoSocial" header="Razão Social" sortable/>
          <Column field="fantasia" header="Nome de Fantasia" sortable/>
          <Column field="cnpj" header="CNPJ" sortable>
            <template #body="slotProps">
              {{ formatCpfCnpj(slotProps.data.cnpj) }}
            </template>
          </Column>
          <Column field="fone" header="Fone">
            <template #body="slotProps">
              {{ formatPhone(slotProps.data.fone) }}
            </template>
          </Column>

          <Column header="Data de Criação" field="dataCriacao" sortable>
            <template #body="slotProps">
              {{ formatDate(slotProps.data.dataCriacao) }}
            </template>
          </Column>

          <Column header="Data de Alteração" field="dataAlteracao" sortable>
            <template #body="slotProps">
              {{ formatDate(slotProps.data.dataAlteracao) }}
            </template>
          </Column>

          <Column :bodyStyle="{ textAlign: 'center' }">
            <template #header>
              <div style="width: 100%; display: flex; justify-content: center;">
                <Button icon="pi pi-plus" class="p-button-sm p-button-text p-mr-2" @click="editUser(null)" title="Novo Usuário"/>
              </div>
            </template>

            <template #body="slotProps">
              <Button icon="pi pi-pencil" class="p-button-sm p-button-text p-mr-2" @click="editUser(slotProps.data)" title="Editar"/>
              <Button icon="pi pi-trash" class="p-button-sm p-button-text p-button-danger" @click="confirmDelete(slotProps.data)" title="Remover"/>
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>
  </BlockUI>
</template>
