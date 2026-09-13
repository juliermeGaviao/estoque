<script setup>
import api from '@/util/api'
import { eAdmin } from '@/util/auth'
import { zodResolver } from '@primevue/forms/resolvers/zod'
import { useToast } from 'primevue/usetoast'
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { z } from 'zod'

const props = defineProps({
  userId: { type: [Number, String], default: null }
})

const router = useRouter()
const toast = useToast()

async function load() {
  try {
    const res = await api.get('/user/get', { params: { id: props.userId } })

    userProfiles.value = res.data.perfis.length
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Usuário', detail: 'Requisição de usuário terminou com o erro: ' + error?.response?.data, life: 10000 })
  }
}

onMounted(() => {
  load()

  if (eAdmin()) {
    loadPriceTables()
    loadUserPriceTables()
  }
})

const priceTables = ref([])

async function loadPriceTables() {
  try {
    const response = await api.get('/price-table/list', { params: { page: 0, size: 10000, sort: 'nome,asc' } })

    priceTables.value = response.data.content
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Tabelas de Preços', detail: 'Requisição de lista de tabelas de preços terminou com o erro: ' + error?.response?.data, life: 10000 })
  }
}

const userPriceTables = ref([])

async function loadUserPriceTables() {
  try {
    const response = await api.get('/user-price-table/list', { params: { idVendedor: props.userId, page: 0, size: 10000, sort: 'tabela.nome,asc' } })

    userPriceTables.value = response.data.content

    if (userProfiles.value === 1) {
      tableForm.value.setValues({ tabelas: [], tabela: userPriceTables.value[0].tabela.id })
    } else if (userProfiles.value > 1) {
      tableForm.value.setValues({ tabelas: userPriceTables.value.map(record => record.tabela.id), tabela: 0 })
    }
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Tabelas de Preços do Usuário', detail: 'Requisição de carga de tabelas de preços do usuário terminou com o erro: ' + error?.response?.data, life: 10000 })
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

const save = async ({ valid, values }) => {
  if (!valid) return

  if (userProfiles.value < 2) {
    const userPriceTable = userPriceTables.value.length ? userPriceTables.value[0] : { tabela: { id: null }, usuario: { id: props.userId } }

    userPriceTable.tabela.id = values.tabela

    try {
      const response = await api.post('/user-price-table', userPriceTable)

      if (response.status === 200) {
        userPriceTables.value = [response.data]

        toast.add({ severity: 'success', summary: 'Sucesso', detail: 'Seleção de Tabela de Preços salva com sucesso', life: 10000 })
      }
    } catch (error) {
      toast.add({ severity: 'error', summary: 'Falha de Gravação da seleção de Tabela de Preços', detail: 'Requisição de gravação da seleção de Tabela de Preços terminou com o erro: ' + error?.response?.data, life: 10000 })
    }
  } else {
    const tables = []

    for (let tabela of values.tabelas) {
      tables.push({ tabela: { id: tabela }, usuario: { id: props.userId } })
    }

    try {
      const response = await api.post('/user-price-table/save-tables', tables)

      if (response.status === 200) {
        toast.add({ severity: 'success', summary: 'Sucesso', detail: 'Seleção de Tabela de Preços salva com sucesso', life: 10000 })
        
        loadUserPriceTables()
      }
    } catch (error) {
      toast.add({ severity: 'error', summary: 'Falha de Gravação da seleção de Tabela de Preços', detail: 'Requisição de gravação da seleção de Tabela de Preços terminou com o erro: ' + error?.response?.data, life: 10000 })
    }
  }
}

</script>

<template>
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
      <Form ref="tableForm" :resolver="tableFormValidator" :initialValues="tableFormValues" @submit="save" class="grid flex flex-column gap-2">
        <FormField v-slot="$field" name="tabelas" v-show="userProfiles === 2">
          <div class="flex items-start gap-2">
            <div v-for="tabela in priceTables" :key="tabela.id" class="flex items-center gap-2 mb-2">
              <Checkbox v-model="$field.value" :value="tabela.id" :inputId="'checkbox_' + tabela.id"/>
              <label :for="'checkbox_' + tabela.id">{{ tabela.nome }}</label>
            </div>
          </div>
          <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
        </FormField>

        <FormField v-slot="$field" name="tabela" v-show="userProfiles < 2">
          <div class="flex items-start gap-2">
            <div v-for="tabela in priceTables" :key="tabela.id" class="flex items-center gap-2 mb-2">
              <RadioButton v-model="$field.value" :value="tabela.id" :inputId="'radiobutton_' + tabela.id"/>
              <label :for="'radiobutton_' + tabela.id">{{ tabela.nome }}</label>
            </div>
          </div>
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
