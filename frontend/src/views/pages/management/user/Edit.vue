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

const toast = useToast()

const formRef = ref(null)
const initialValues = ref({
  email: '',
  perfis: [2]
})

const resolverUser = zodResolver(
  z.object({
    email: z.string().min(1, { message: 'E-mail é obrigatório.' }).email({ message: 'E-mail inválido.' }),
    perfis: z.array(z.number()).optional()
  })
)

const resolverPassword = zodResolver(
  z.object({
    senha: z.string().min(1, { message: 'Senha é obrigatória.' }).min(8, { message: 'Senha deve ter no mínimo 8 caracteres.' }),
    confirmarSenha: z.string().min(1, { message: 'Confirmação de senha é obrigatória.' })
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
        perfis: res.data.perfis.map(p => p.id)
      })
    }

    initialValues.value = {
      email: res.data.email,
      perfis: res.data.perfis.map(p => p.id)
    }
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Usuário', detail: 'Requisição de usuário terminou com o erro: ' + error.response.data, life: 10000 })
  }
}

const save = async ({ valid, values }) => {
  if (!valid) return

  let params = { ... values }

  params['id'] = parseInt(route.query.id)

  try {
    const response = await api.post('/user', params)

    if (response.status === 200) {
      toast.add({ severity: 'success', summary: 'Sucesso', detail: 'Usuário atualizado com sucesso', life: 10000 })
    }
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Gravação de Usuário', detail: 'Requisição de alteração de usuário terminou com o erro: ' + error.response.data, life: 10000 })
  }
}

const changePassword = async ({ valid, values }) => {
  if (!valid) return

  let params = { ... values }

  params['id'] = parseInt(route.query.id)

  delete params.confirmarSenha

  try {
    params.senha = await sha256Hex(params.senha)
    const response = await api.post('/user/password', params)

    if (response.status === 200) {
      toast.add({ severity: 'success', summary: 'Sucesso', detail: 'Senha alterada com sucesso', life: 10000 })
    }
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Gravação de Usuário', detail: 'Requisição de troca de senha terminou com o erro: ' + error.response.data, life: 10000 })
  }
}

function clearUser(values) {
  values.email = ''
  values.perfis = []
}

function clearPassword(values) {
  values.senha = ''
  values.confirmarSenha = ''
}

function cancel() {
  router.back()
}

onMounted(() => {
  loadProfiles()

  loadUser(route.query.id)
})
</script>

<template>
  <Card class="mb-4">
    <template #title><h3>Editar Usuário</h3></template>

    <template #content>
      <Form ref="formRef" :resolver="resolverUser" :initialValues @submit="save" @reset="clearUser" class="grid flex flex-column gap-4">
        <FormField v-slot="$field" name="email" initialValue="">
          <FloatLabel variant="on" class="flex-1">
            <InputText id="email" maxlength="255" autocomplete="off" fluid/>
            <label for="email">E-mail</label>
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

        <FormField class="flex justify-end gap-4">
          <Button label="Limpar" icon="pi pi-times" type="reset" severity="secondary" raised/>
          <Button label="Salvar" icon="pi pi-save" type="submit" raised/>
        </FormField>
      </Form>
    </template>
  </Card>
  <Card class="mb-6">
    <template #title><h3>Senha de acesso</h3></template>
    <template #content>
      <Form :resolver="resolverPassword" @submit="changePassword" @reset="clearPassword" class="grid flex flex-column gap-4">
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

        <FormField class="flex justify-end gap-4">
          <Button label="Limpar" icon="pi pi-times" type="reset" severity="secondary" raised/>
          <Button label="Salvar" icon="pi pi-save" type="submit" raised/>
        </FormField>
      </Form>
    </template>
  </Card>
  <div class="flex justify-end mt-4">
    <Button label="Voltar" icon="pi pi-replay" @click="cancel" severity="secondary" raised/>
  </div>
</template>
