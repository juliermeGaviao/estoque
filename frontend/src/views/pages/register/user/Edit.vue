<script setup>
import api from '@/util/api'
import { eAdmin, sha256Hex } from '@/util/auth'
import { zodResolver } from '@primevue/forms/resolvers/zod'
import { useToast } from 'primevue/usetoast'
import { nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { z } from 'zod'

const router = useRouter()
const route = useRoute()
const toast = useToast()
const loading = ref(false)
const userId = Number.parseInt(route.query.id)

const form = ref(null)
const initialFormValues = ref({ email: '', perfis: [] })

const formValidator = zodResolver(
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

  loading.value = true

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
  } finally {
    loading.value = false
  }
}

const changePassword = async ({ valid, values }) => {
  if (!valid) return

  loading.value = true

  let params = { ... values }

  params['id'] = userId

  delete params.confirmarSenha

  try {
    params.senha = await sha256Hex(params.senha)

    const response = await api.post('/user/password', params)

    if (response.status === 200) {
      toast.add({ severity: 'success', summary: 'Sucesso', detail: 'Senha alterada com sucesso', life: 10000 })
    }
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Gravação de Usuário', detail: 'Requisição de troca de senha terminou com o erro: ' + error.response.data, life: 10000 })
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loading.value = true

  load()

  if (eAdmin()) {
    loadProfiles()
    loadPriceTables()
    loadUserPriceTables()
  }

  loading.value = false
})

const priceTables = ref([])

async function loadPriceTables() {
  try {
    const response = await api.get('/price-table/list', { params: { page: 0, size: 10000, sort: 'nome,asc' } })

    priceTables.value = response.data.content
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Tabelas de Preços', detail: 'Requisição de lista de tabelas de preços terminou com o erro: ' + error.response.data, life: 10000 })
  }
}

const userPriceTables = ref([])

async function loadUserPriceTables() {
  try {
    const response = await api.get('/user-price-table/list', { params: { idVendedor: userId, page: 0, size: 10000, sort: 'tabela.nome,asc' } })

    userPriceTables.value = response.data.content

    if (userProfiles.value === 1) {
      tableForm.value.setValues({ tabelas: [], tabela: userPriceTables.value[0].tabela.id })
    } else if (userProfiles.value > 1) {
      tableForm.value.setValues({ tabelas: userPriceTables.value.map(record => record.tabela.id), tabela: 0 })
    }
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Usuário', detail: 'Requisição de usuário terminou com o erro: ' + error.response.data, life: 10000 })
  }
}

const tableForm = ref(null)
const tableFormValues = ref({ tabelas: [], tabela: 0 })

const tableFormValidator = zodResolver(
  z.object({
    tabelas: z.array(z.number()).refine(data => userProfiles.value === 1 || data.length, { message: 'É necessário marcar ao menos uma tabela de preços.' }),
    tabela: z.number().refine(data => userProfiles.value === 2 || data > 0, { message: 'Uma Tabela de Preços deve ser escolhida.' })
  })
)

const userProfiles = ref(0)

const savePriceTables = async ({ valid, values }) => {
  if (!valid) return

  if (userProfiles.value < 2) {
    const userPriceTable = userPriceTables.value.length ? userPriceTables.value[0] : { tabela: { id: null }, usuario: { id: userId } }

    userPriceTable.tabela.id = values.tabela

    loading.value = true

    try {
      const response = await api.post('/user-price-table', userPriceTable)

      if (response.status === 200) {
        userPriceTables.value = [response.data]

        toast.add({ severity: 'success', summary: 'Sucesso', detail: 'Seleção de Tabela de Preços salva com sucesso', life: 10000 })
      }
    } catch (error) {
      toast.add({ severity: 'error', summary: 'Falha de Gravação da seleção de Tabela de Preços', detail: 'Requisição de gravação da seleção de Tabela de Preços terminou com o erro: ' + error.response.data, life: 10000 })
    } finally {
      loading.value = false
    }
  } else {
    const tables = []

    values.tabelas.forEach(tabela => tables.push({ tabela: { id: tabela }, usuario: { id: userId } }))

    loading.value = true

    try {
      const response = await api.post('/user-price-table/save-tables', tables)

      if (response.status === 200) {
        toast.add({ severity: 'success', summary: 'Sucesso', detail: 'Seleção de Tabela de Preços salva com sucesso', life: 10000 })
        
        loadUserPriceTables()
      }
    } catch (error) {
      toast.add({ severity: 'error', summary: 'Falha de Gravação da seleção de Tabela de Preços', detail: 'Requisição de gravação da seleção de Tabela de Preços terminou com o erro: ' + error.response.data, life: 10000 })
    } finally {
      loading.value = false
    }
  }
}

function clearTables() {
  tableForm.value.setValues(tableFormValues.value)
}
</script>

<template>
  <BlockUI :blocked="loading" fullScreen>
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
        <Form ref="form" :resolver="formValidator" :initialValues="initialFormValues" @submit="save" class="grid flex flex-column gap-4">
          <FormField v-slot="$field" name="email">
            <FloatLabel variant="on" class="flex-1">
              <InputText id="email" maxlength="255" autocomplete="off" fluid/>
              <label for="email">E-mail</label>
            </FloatLabel>
            <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
          </FormField>

          <FormField v-slot="$field" name="perfis" class="flex items-start gap-4" v-show="eAdmin()">
            <div classes="label">Perfis:</div>
            <div v-for="perfil in profiles" :key="perfil.id" class="flex items-center gap-2">
              <Checkbox :value="perfil.id" :inputId="'perfil_' + perfil.id" :disabled="perfil.id === 2"
                @change="clearTables"
                :modelValue="$field.value"
                @update:modelValue="val => {
                  $field.value = val
                  userProfiles = val.length
                }"
              />
              <label :for="'perfil_' + perfil.id">{{ perfil.nome }}</label>
            </div>
          </FormField>

          <div class="flex justify-end gap-4">
            <Button label="Limpar" icon="pi pi-times" type="reset" severity="secondary" raised/>
            <Button label="Salvar" icon="pi pi-save" type="submit" raised/>
          </div>
        </Form>
      </template>
    </Card>
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
        <Form :resolver="resolverPassword" @submit="changePassword" class="grid flex flex-column gap-4">
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

          <div class="flex justify-end gap-4">
            <Button label="Limpar" icon="pi pi-times" type="reset" severity="secondary" raised/>
            <Button label="Salvar" icon="pi pi-save" type="submit" raised/>
          </div>
        </Form>
      </template>
    </Card>
    <Card class="mb-6" v-show="eAdmin()">
      <template #title>
        <div class="grid grid-cols-2">
          <h3>Tabelas de Preços</h3>
          <div class="flex justify-end items-center">
            <Button icon="pi pi-replay" @click="router.back()" class="p-button-text" v-tooltip.bottom="'Voltar'"/>
          </div>
        </div>
      </template>
      <template #content>
        <Form ref="tableForm" :resolver="tableFormValidator" :initialValues="tableFormValues" @submit="savePriceTables" class="grid flex flex-column gap-4">
          <FormField v-slot="$field" name="tabelas" v-show="userProfiles === 2">
            <div class="flex items-start gap-4">
              <div v-for="tabela in priceTables" :key="tabela.id" class="flex items-center gap-2 mb-2">
                <Checkbox v-model="$field.value" :value="tabela.id" :inputId="'checkbox_' + tabela.id"/>
                <label :for="'checkbox_' + tabela.id">{{ tabela.nome }}</label>
              </div>
            </div>
            <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
          </FormField>

          <FormField v-slot="$field" name="tabela" v-show="userProfiles < 2">
            <div class="flex items-start gap-4">
              <div v-for="tabela in priceTables" :key="tabela.id" class="flex items-center gap-2 mb-2">
                <RadioButton v-model="$field.value" :value="tabela.id" :inputId="'radiobutton_' + tabela.id"/>
                <label :for="'radiobutton_' + tabela.id">{{ tabela.nome }}</label>
              </div>
            </div>
            <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
          </FormField>

          <div class="flex justify-end gap-4">
            <Button label="Limpar" icon="pi pi-times" type="reset" severity="secondary" raised/>
            <Button label="Salvar" icon="pi pi-save" type="submit" raised/>
          </div>
        </Form>
      </template>
    </Card>
  </BlockUI>
</template>
