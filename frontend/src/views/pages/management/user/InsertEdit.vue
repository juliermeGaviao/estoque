<script setup>
import { zodResolver } from '@primevue/forms/resolvers/zod'
import { useToast } from 'primevue/usetoast'
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { z } from 'zod'
import api from '../../../../util/api'
import { sha256Hex } from '../../../../util/auth'

const router = useRouter()
const route = useRoute()
const userId = route.query.id

const toast = useToast()

const formRef = ref(null)
const initialValues = ref({
  email: '',
  senha: '',
  confirmarSenha: '',
  perfis: [2]
})

const resolver = zodResolver(
  z.object({
    email: z.string().min(1, { message: 'E-mail é obrigatório.' }).email({ message: 'E-mail inválido.' }),
    senha: z.string().min(1, { message: 'Senha é obrigatória.' }).min(8, { message: 'Senha deve ter no mínimo 8 caracteres.' }),
    confirmarSenha: z.string().min(1, { message: 'Confirmação de senha é obrigatória.' }),
    perfis: z.array(z.number()).optional()
  }).refine(data => data.senha === data.confirmarSenha, {
    message: 'As senhas não coincidem.',
    path: ['confirmarSenha']
  })
)

const profiles = ref([])

async function loadProfiles() {
  try {
    const res = await api.get('/user/profiles')

    profiles.value = res.data
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Perfis', detail: 'Requisição de perfis terminou com o erro: ' + error.response.data, life: 10000 })
  }
}

async function loadUser(id) {
  try {
    const res = await api.get('/user/get', { params: { id } })

    if (formRef.value) {
      formRef.value.setValues({
        email: res.data.email,
        senha: '',
        confirmarSenha: '',
        perfis: res.data.perfis.map(p => p.id)
      })
    }

    initialValues.value = {
      email: res.data.email,
      senha: '',
      confirmarSenha: '',
      perfis: res.data.perfis.map(p => p.id)
    }
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Usuário', detail: 'Requisição de usuário terminou com o erro: ' + error.response.data, life: 10000 })
  }
}

const save = async ({ valid, values }) => {
  if (!valid) return

  let params = { ... values }

  if (userId) {
    params['id'] = parseInt(userId)
  }

  delete params.confirmarSenha

  try {
    params.senha = await sha256Hex(params.senha)
    const response = await api.post('/user', params)

    if (response.status === 200) {
      router.push({ path: '/management/user', query: { saved: 'true' } })
    }
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Gravação de Usuário', detail: 'Requisição de carga de usuário terminou com o erro: ' + error.response.data, life: 10000 })
  }
}

function clear(values) {
  values.email = ''
  values.senha = ''
  values.confirmarSenha = ''
  values.perfis = []
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
    <template #title>
      <h3>{{ route.params.id ? 'Editar Usuário' : 'Inserir Usuário' }}</h3>
    </template>

    <template #content>
      <Form ref="formRef" :resolver :initialValues @submit="save" @reset="clear" class="grid flex flex-column gap-4">
        <FormField v-slot="$field" name="email" initialValue="">
          <FloatLabel variant="on" class="flex-1">
            <InputText id="email" maxlength="255" autocomplete="off" fluid/>
            <label for="email">E-mail</label>
          </FloatLabel>
          <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
        </FormField>

        <FormField v-slot="$field" name="senha" initialValue="">
          <FloatLabel variant="on" class="flex-1">
            <Password inputId="senha" toggleMask fluid :feedback="false"/>
            <label for="senha">Senha</label>
          </FloatLabel>
          <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
        </FormField>

        <FormField v-slot="$field" name="confirmarSenha" initialValue="">
          <FloatLabel variant="on" class="flex-1">
            <Password inputId="confirmarSenha" toggleMask fluid :feedback="false"/>
            <label for="confirmarSenha">Confirmação da senha</label>
          </FloatLabel>
          <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
        </FormField>

        <FormField name="perfis" class="flex items-start gap-4">
          <div classes="label">Perfis:</div>
          <div v-for="perfil in profiles" :key="perfil.id" class="flex items-center gap-2">
            <Checkbox :value="perfil.id" :inputId="'perfil' + perfil.id" :disabled="perfil.id === 2"/>
            <label :for="'perfil' + perfil.id">{{ perfil.nome }}</label>
          </div>
        </FormField>

        <FormField class="flex justify-end gap-2">
          <Button label="Limpar" icon="pi pi-times" type="reset" severity="secondary" raised/>
          <Button label="Cancelar" icon="pi pi-ban" @click="cancel" severity="secondary" raised/>
          <Button label="Salvar" icon="pi pi-save" type="submit" raised/>
        </FormField>

      </Form>
    </template>
  </Card>
</template>
