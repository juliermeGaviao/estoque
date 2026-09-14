<script setup>
import api from '@/util/api'
import { sha256Hex } from '@/util/auth'
import { zodResolver } from '@primevue/forms/resolvers/zod'
import { useToast } from 'primevue/usetoast'
import { useRouter } from 'vue-router'
import { passwordSchema } from './passwordSchema'

const props = defineProps({
  userId: { type: [Number, String], default: null }
})

const router = useRouter()
const toast = useToast()

const resolver = zodResolver(passwordSchema)

const changePassword = async ({ valid, values }) => {
  if (!valid) return

  let params = { ... values }

  params['id'] = props.userId

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
</script>

<template>
  <Card class="mb-6">
    <template #title>
      <div class="grid grid-cols-2">
        <h3>Senha de acesso</h3>
        <div class="flex justify-end items-center">
          <Button icon="pi pi-replay" @click="router.back()" class="p-button-text" v-tooltip.bottom="'Voltar'"/>
        </div>
      </div>
    </template>
    <template #content>
      <Form :resolver="resolver" @submit="changePassword" class="grid flex flex-column gap-2">
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

        <div class="flex justify-end gap-2">
          <Button label="Limpar" icon="pi pi-times" type="reset" severity="secondary" raised/>
          <Button label="Salvar" icon="pi pi-save" type="submit" raised/>
        </div>
      </Form>
    </template>
  </Card>
</template>
