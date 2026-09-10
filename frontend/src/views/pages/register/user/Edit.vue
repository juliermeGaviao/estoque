<script setup>
import api from '@/util/api'
import { eAdmin } from '@/util/auth'
import { zodResolver } from '@primevue/forms/resolvers/zod'
import { useToast } from 'primevue/usetoast'
import { nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { z } from 'zod'
import ChangePassword from './ChangePassword.vue'
import PriceTable from './PriceTable.vue'
import SalePoint from './SalePoint.vue'

const router = useRouter()
const route = useRoute()
const toast = useToast()
const userId = Number.parseInt(route.query.id)

const form = ref(null)
const initialFormValues = ref({ email: '', perfis: [] })

const formValidator = zodResolver(
  z.object({
    email: z.string().min(1, { message: 'E-mail é obrigatório.' }).email({ message: 'E-mail inválido.' }),
    perfis: z.array(z.number()).optional()
  })
)

async function load() {
  try {
    const res = await api.get('/user/get', { params: { id: userId } })

    nextTick(() => {
      form.value.setValues({
        email: res.data.email,
        perfis: res.data.perfis.map(p => p.id)
      })

      userProfiles.value = res.data.perfis.length
    })
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Usuário', detail: 'Requisição de usuário terminou com o erro: ' + error.response.data, life: 10000 })
  }
}

const profiles = ref([])

async function loadProfiles() {
  try {
    const res = await api.get('/user/profiles')

    profiles.value = res.data
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Perfis', detail: 'Requisição de perfis terminou com o erro: ' + error.response.data, life: 10000 })
  }
}

const save = async ({ valid, values }) => {
  if (!valid) return

  let params = { ... values }

  for (let param in params) {
    if (typeof params[param] === 'string') {
      params[param] = params[param].trim()
    }
  }

  params['id'] = userId

  try {
    const response = await api.post('/user', params)

    if (response.status === 200) {
      toast.add({ severity: 'success', summary: 'Sucesso', detail: 'Usuário atualizado com sucesso', life: 10000 })
    }
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Gravação de Usuário', detail: 'Requisição de alteração de usuário terminou com o erro: ' + error.response.data, life: 10000 })
  }
}

onMounted(() => {
  load()

  if (eAdmin()) {
    loadProfiles()
  }
})

const userProfiles = ref(0)

</script>

<template>
  <Card class="mb-4">
    <template #title>
      <div class="grid grid-cols-2">
        <h3>Editar Usuário</h3>
        <div class="flex justify-end items-center">
          <Button icon="pi pi-replay" @click="router.back()" class="p-button-text" v-tooltip.bottom="'Voltar'"/>
        </div>
      </div>
    </template>

    <template #content>
      <Form ref="form" :resolver="formValidator" :initialValues="initialFormValues" @submit="save" class="grid flex flex-column gap-2">
        <FormField v-slot="$field" name="email">
          <FloatLabel variant="on" class="flex-1">
            <InputText id="email" maxlength="255" autocomplete="off" fluid/>
            <label for="email">E-mail</label>
          </FloatLabel>
          <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
        </FormField>

        <FormField v-slot="$field" name="perfis" class="flex items-start gap-2" v-show="eAdmin()">
          <div classes="label">Perfis:</div>
          <div v-for="perfil in profiles" :key="perfil.id" class="flex items-center gap-2">
            <Checkbox :value="perfil.id" :inputId="'perfil_' + perfil.id" :disabled="perfil.id === 2"
              :modelValue="$field.value"
              @update:modelValue="val => {
                $field.value = val
                userProfiles = val.length
              }"
            />
            <label :for="'perfil_' + perfil.id">{{ perfil.nome }}</label>
          </div>
        </FormField>

        <div class="flex justify-end gap-2">
          <Button label="Limpar" icon="pi pi-times" type="reset" severity="secondary" raised/>
          <Button label="Salvar" icon="pi pi-save" type="submit" raised/>
        </div>
      </Form>
    </template>
  </Card>
  <ChangePassword :userId="userId"/>
  <PriceTable :userId="userId"/>
  <SalePoint :userId="userId"/>
</template>
