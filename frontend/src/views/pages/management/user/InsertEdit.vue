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
  senha: '',
  confirmarSenha: '',
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

async function save() {
  try {
    await api.post('/user', user.value)
    alert('Usuário salvo com sucesso!')
    clearForm()
  } catch (error) {
    console.error('Erro ao salvar usuário:', error)
    alert('Erro ao salvar usuário')
  }
}

function clear() {
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
  <Card>
    <template #title><h3>{{ route.params.id ? 'Editar Usuário' : 'Inserir Usuário' }}</h3></template>
    <template #content>
      <Form class="grid flex flex-column gap-4" @submit="save" @reset="clear">
        <FormField class="flex items-center gap-4">
          <label class="w-36" for="email">Email</label>
          <InputText id="email" v-model="user.email" placeholder="Digite o email" maxlength="255" class="flex-1" fluid/>
        </FormField>
        <FormField class="flex items-center gap-4">
          <label class="w-36" for="senha">Senha</label>
          <Password id="senha" v-model="user.senha" placeholder="Digite a senha" toggleMask class="flex-1" fluid/>
        </FormField>
        <FormField class="flex items-center gap-4">
          <label class="w-36" for="confirmarSenha">Confirmação</label>
          <Password id="confirmarSenha" v-model="user.confirmarSenha" placeholder="Confirme a senha" toggleMask class="flex-1" fluid/>
        </FormField>
        <FormField class="flex items-start gap-4">
          <label class="w-36" for="">Perfis</label>
          <div class="flex flex-wrap gap-4 flex-1">
            <div v-for="perfil in profiles" :key="perfil.id" class="flex items-center gap-2">
              <Checkbox v-model="user.perfis" :value="perfil.id" :inputId="'perfil' + perfil.id" />
              <label :for="'perfil' + perfil.id">{{ perfil.nome }}</label>
            </div>
          </div>
        </FormField>
        <FormField class="flex justify-end gap-2">
          <Button label="Limpar" icon="pi pi-times" severity="secondary" />
          <Button label="Cancelar" icon="pi pi-ban" @click="cancel" severity="secondary" />
          <Button label="Salvar" icon="pi pi-save" type="submit"/>
        </FormField>
      </Form>
    </template>
  </Card>
</template>
