<script setup>
import { onMounted, ref } from 'vue'
import api from '../../../../util/api'

const users = ref([])
const totalRecords = ref(0)
const loading = ref(false)

const page = ref(0)
const size = ref(20)
const sortField = ref(null)
const sortOrder = ref(null)

const email = ref('')

const loadUsers = async () => {
  loading.value = true

  try {
    const response = await api.get('/user/list', {
      params: {
        page: page.value,
        size: size.value,
        sort: sortField.value ? `${sortField.value},${sortOrder.value === 1 ? 'asc' : 'desc'}` : null,
        email: email.value || undefined
      }
    })

    users.value = response.data.content
    totalRecords.value = response.data.totalElements
  } catch (err) {
    console.error('Erro ao carregar usuários', err)
  } finally {
    loading.value = false
  }
}

onMounted(loadUsers)

const onPage = (event) => {
  page.value = event.page
  size.value = event.rows
  loadUsers()
}

const onSort = (event) => {
  sortField.value = event.sortField
  sortOrder.value = event.sortOrder
  loadUsers()
}

const onFilter = () => {
  page.value = 0 // volta para primeira página
  loadUsers()
}

const formatDate = isoString => {
  if (!isoString) return ''

  const date = new Date(isoString)

  const data = date.toLocaleDateString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric'
  })

  const hora = date.toLocaleTimeString('pt-BR', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  })

  return `${data} ${hora}`
}

const editUser = (user) => {
  console.log('Editar usuário', user)
}

const deleteUser = async (user) => {
  if (!confirm(`Deseja realmente remover o usuário ${user.email}?`)) return

  try {
    await api.delete(`/user/${user.id}`)
    // recarregar tabela após remoção
    loadUsers()
  } catch (err) {
    console.error('Erro ao remover usuário', err)
  }
}

</script>

<template>
  <div class="card">
    <h2 class="mb-4">Lista de Usuários</h2>

    <div class="flex gap-4 mb-10">
      <InputText v-model="email" placeholder="Filtrar por email" class="p-inputtext-sm" />
      <Button label="Buscar" icon="pi pi-search" @click="onFilter" />
    </div>

    <DataTable
      :value="users"
      :lazy="true"
      :paginator="true"
      :rows="size"
      :totalRecords="totalRecords"
      :loading="loading"
      :first="page * size"
      @page="onPage"
      @sort="onSort"
      :sortField="sortField"
      :sortOrder="sortOrder"
      responsiveLayout="scroll"
      stripedRows
    >
      <Column field="id" header="Id" sortable />
      <Column field="email" header="Email" sortable />

      <Column header="Data de Criação" sortable>
        <template #body="slotProps">
          {{ formatDate(slotProps.data.dataCriacao) }}
        </template>
      </Column>

      <Column header="Data de Alteração" sortable>
        <template #body="slotProps">
          {{ formatDate(slotProps.data.dataAlteracao) }}
        </template>
      </Column>

      <Column :bodyStyle="{ textAlign: 'center' }">
        <template #header>
          <div style="width: 100%; display: flex; justify-content: center;">
            <Button
              icon="pi pi-plus"
              class="p-button-sm p-button-text p-mr-2"
              @click="editUser(null)"
              title="Novo Usuário"
            />
          </div>
        </template>

        <template #body="slotProps">
          <Button
            icon="pi pi-pencil"
            class="p-button-sm p-button-text p-mr-2"
            @click="editUser(slotProps.data)"
            title="Editar"
          />
          <Button
            icon="pi pi-trash"
            class="p-button-sm p-button-text p-button-danger"
            @click="deleteUser(slotProps.data)"
            title="Remover"
          />
        </template>
      </Column>
    </DataTable>
  </div>
</template>

<style scoped>
.card {
  padding: 1.5rem;
  background: var(--surface-card);
  border-radius: var(--border-radius);
  box-shadow: var(--card-shadow);
}
</style>
