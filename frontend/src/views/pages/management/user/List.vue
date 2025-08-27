<script setup>
import { useToast } from 'primevue/usetoast'
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../../../../util/api'

const router = useRouter()
const route = useRoute()

const toast = useToast()

const users = ref([])
const totalRecords = ref(0)
const loading = ref(false)

const page = ref(0)
const size = ref(20)
const sortField = ref(null)
const sortOrder = ref(null)

const email = ref('')

async function loadUsers() {
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

    if (email.value) {
      params.email = email.value
    }

    const response = await api.get('/user/list', { params: params })

    users.value = response.data.content
    totalRecords.value = response.data.totalElements
  } catch (error) {
    console.error('Erro ao carregar usuários', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadUsers()

  if (route.query.saved === 'true') {
    toast.add({ severity: 'success', summary: 'Sucesso', detail: 'Usuário cadastrado/atualizado com sucesso', life: 10000 })

    route.query.saved = undefined
  }
})

function onPage(event) {
  page.value = event.page
  size.value = event.rows
  loadUsers()
}

function onSort(event) {
  sortField.value = event.sortField
  sortOrder.value = event.sortOrder
  loadUsers()
}

function onFilter() {
  page.value = 0
  loadUsers()
}

function onClear() {
  email.value = null
  loadUsers()
}

function formatDate(isoString) {
  if (!isoString) return ''

  const date = new Date(isoString)

  const data = date.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' })

  const hora = date.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false })

  return `${data} ${hora}`
}

function editUser(user) {
  if (user && user.id) {
    router.push(`/management/user/edit?id=${user.id}`)
  } else {
    router.push('/management/user/edit')
  }
}

async function deleteUser(user) {
  if (!confirm(`Deseja realmente remover o usuário ${user.email}?`)) return

  try {
    await api.delete(`/user/${user.id}`)

    loadUsers()
  } catch (err) {
    console.error('Erro ao remover usuário', err)
  }
}
</script>

<template>
  <Card>
    <template #title><h3>Lista de Usuários</h3></template>
    <template #content>
      <Form class="flex gap-2 mb-4" @submit="onFilter" @reset="onClear">
        <FloatLabel variant="on">
          <label for="email">E-mail</label>
          <InputText id="email" v-model="email" autocomplete="off" fluid/>
        </FloatLabel>

        <Button label="Limpar" icon="pi pi-times" severity="secondary" type="reset" raised/>
        <Button label="Buscar" type="submit" icon="pi pi-search" raised/>
      </Form>

      <DataTable :value="users" :lazy="true" :paginator="true" :rows="size" :totalRecords="totalRecords" :loading="loading"
        :first="page * size" @page="onPage" @sort="onSort" :sortField="sortField" :sortOrder="sortOrder" responsiveLayout="scroll" stripedRows
        :rowsPerPageOptions="[10, 20, 50, 100]">

        <Column field="id" header="Id" sortable />
        <Column field="email" header="Email" sortable />

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
            <Button icon="pi pi-trash" class="p-button-sm p-button-text p-button-danger" @click="deleteUser(slotProps.data)" title="Remover"/>
          </template>
        </Column>
      </DataTable>
    </template>
  </Card>
</template>
