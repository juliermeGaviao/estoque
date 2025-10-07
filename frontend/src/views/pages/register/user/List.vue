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
const size = ref(15)
const sortField = ref(null)
const sortOrder = ref(null)

const email = ref('')

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

  if (email.value) {
    params.email = email.value
  }

  try {
    const response = await api.get('/user/list', { params: params })

    data.value = response.data.content
    totalRecords.value = response.data.totalElements
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Usuários', detail: 'Requisição de lista de usuários terminou com o erro: ' + error.response.data, life: 10000 })
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
  page.value = 0
  sortField.value = event.sortField
  sortOrder.value = event.sortOrder

  load()
}

function onFilter() {
  page.value = 0
  load()
}

function onClear() {
  email.value = null
  load()
}

function edit(user) {
  if (user?.id) {
    router.push(`/register/user/edit?id=${user.id}`)
  } else {
    router.push('/register/user/insert')
  }
}

const confirmDelete = entity => {
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
        await api.delete(`/user?id=${entity.id}`)

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
  <ConfirmDialog :closable="false"></ConfirmDialog>
  <BlockUI :blocked="loading" fullScreen>
    <Card>
      <template #title><h3>Lista de Usuários</h3></template>
      <template #content>
        <Form class="flex gap-4 mb-4" @submit="onFilter" @reset="onClear">
          <FloatLabel variant="on">
            <label for="email">E-mail</label>
            <InputText id="email" v-model="email" autocomplete="off" fluid/>
          </FloatLabel>

          <Button label="Limpar" icon="pi pi-times" severity="secondary" type="reset" raised/>
          <Button label="Buscar" type="submit" icon="pi pi-search" raised/>
        </Form>

        <DataTable :value="data" :lazy="true" :paginator="true" :rows="size" :totalRecords="totalRecords"
          :first="page * size" @page="onPage" @sort="onSort" :sortField="sortField" :sortOrder="sortOrder" responsiveLayout="scroll" stripedRows
          :rowsPerPageOptions="[15, 30, 60, 100]" size="small">

          <Column field="id" header="Id" sortable/>
          <Column field="email" header="Email" sortable/>
          <Column field="perfis" header="Perfis"/>

          <Column headerClass="flex justify-center" bodyClass="flex justify-center">
            <template #header>
              <Button icon="pi pi-plus" class="p-button-sm p-button-text p-mr-2" @click="edit(null)" v-tooltip.bottom="'Novo Usuário'"/>
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
