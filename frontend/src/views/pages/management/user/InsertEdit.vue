<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../../../../util/api'

const router = useRouter()
const route = useRoute()
const userId = route.params.id

const user = ref({
  id: null,
  email: '',
  perfis: []
})

const profiles = ref([])

async function loadProfiles() {
  try {
    const res = await api.get('/user/profiles')
    profiles.value = res.data
  } catch (error) {
    console.error('Erro ao carregar perfis:', error)
  }
}

async function loadUser(id) {
  try {
    const res = await api.get('/user/get', { params: { id } })
    user.value.id = res.data.id
    user.value.email = res.data.email

    user.value.perfis = res.data.perfis.map(p => p.id)
  } catch (error) {
    console.error('Erro ao carregar usuário:', error)
  }
}

async function saveUser() {
  try {
    await api.post('/user', user.value)
    alert('Usuário salvo com sucesso!')
    clearForm()
  } catch (error) {
    console.error('Erro ao salvar usuário:', error)
    alert('Erro ao salvar usuário')
  }
}

function clearForm() {
  user.value.email = ''
  user.value.perfis = []
}

function cancel() {
  router.push('/management/user')
}

onMounted(() => {
  loadProfiles()

  if (userId) {
    loadUser(userId)
  }
})
</script>

<template>
  <div class="card">
    <h3 class="mb-4">{{ route.params.id ? 'Editar Usuário' : 'Inserir Usuário' }}</h3>

    <div class="flex items-center gap-2 mb-4">
      <label for="email" class="flex items-center">Email</label>
      <InputText id="email" v-model="user.email" placeholder="Digite o email" maxlength="255" style="width: 600px"/>
    </div>

    <div class="flex gap-2 mb-4">
      <label for="">Perfis</label>
      <div v-for="perfil in profiles" :key="perfil.id" class="flex gap-2 mr-1">
        <Checkbox v-model="user.perfis" :value="perfil.id" :inputId="'perfil' + perfil.id"/>
        <label :for="'perfil' + perfil.id">{{ perfil.nome }}</label>
      </div>
    </div>

    <div class="flex w-full mb-4">
      <div class="ml-auto flex gap-2">
        <Button label="Limpar" icon="pi pi-times" @click="clearForm" severity="secondary"/>
        <Button label="Cancelar" icon="pi pi-ban" @click="cancel" severity="secondary"/>
        <Button label="Salvar" icon="pi pi-save" @click="saveUser"/>
      </div>
    </div>
  </div>
</template>
