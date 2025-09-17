<script setup>
import api from '@/util/api'
import { sha256Hex } from '@/util/auth'
import { zodResolver } from '@primevue/forms/resolvers/zod'
import { useToast } from 'primevue/usetoast'
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { z } from 'zod'

const router = useRouter()
const toast = useToast()

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
const loading = ref(false)

async function loadProfiles() {
  loading.value = true

  try {
    const res = await api.get('/user/profiles')

    profiles.value = res.data
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Perfis', detail: 'Requisição de perfis terminou com o erro: ' + error.response.data, life: 10000 })
  } finally {
    loading.value = false
  }
}

const save = async ({ valid, values }) => {
  if (!valid) return

  loading.value = true

  let params = { ... values }

  delete params.confirmarSenha

  try {
    params.senha = await sha256Hex(params.senha)
    const response = await api.post('/user', params)

    if (response.status === 200) {
      toast.add({ severity: 'success', summary: 'Sucesso', detail: 'Usuário cadastrado com sucesso', life: 10000 })
      cancel()
    }
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Gravação de Usuário', detail: 'Requisição de carga de usuário terminou com o erro: ' + error.response.data, life: 10000 })
  } finally {
    loading.value = false
  }
}

function cancel() {
  router.push('/management/user')
}

onMounted(() => {
  loadProfiles()
})
</script>

<template>
  <BlockUI :blocked="loading" fullScreen>
    <Card>
      <template #title><h3>Inserir Usuário</h3></template>

      <template #content>
        <Form :resolver :initialValues @submit="save" class="grid flex flex-column gap-4">
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

          <FormField class="flex justify-end gap-4">
            <Button label="Limpar" icon="pi pi-times" type="reset" severity="secondary" raised/>
            <Button label="Cancelar" icon="pi pi-ban" @click="cancel" severity="secondary" raised/>
            <Button label="Salvar" icon="pi pi-save" type="submit" raised/>
          </FormField>

        </Form>
      </template>
    </Card>
  </BlockUI>
</template>
