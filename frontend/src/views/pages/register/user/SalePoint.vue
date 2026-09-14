<script setup>
import api from '@/util/api'
import { eAdmin } from '@/util/auth'
import { zodResolver } from '@primevue/forms/resolvers/zod'
import { useToast } from 'primevue/usetoast'
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { createSalePointSchema } from './salePointSchema'

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
    loadSalePoints()
  }
})

const salePoints = ref([])

async function loadSalePoints() {
  try {
    const response = await api.get('/sale-point/list', { params: { page: 0, size: 10000, sort: 'nome,asc' } })

    salePoints.value = response.data.content
    loadUserSalePoints()
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Pontos de Venda', detail: 'Requisição de lista de pontos de venda terminou com o erro: ' + error?.response?.data, life: 10000 })
  }
}

const userSalePoints = ref([])

async function loadUserSalePoints() {
  try {
    const response = await api.get('/user-sale-point/list', { params: { idUsuario: props.userId, page: 0, size: 10000, sort: 'pontoVenda.nome,asc' } })

    userSalePoints.value = response.data.content

    if (userProfiles.value === 1) {
      form.value.setValues({ pontos: [], ponto: userSalePoints.value[0].pontoVenda.id })
    } else if (userProfiles.value > 1) {
      form.value.setValues({ pontos: userSalePoints.value.map(record => record.pontoVenda.id), ponto: 0 })
    }
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Pontos de Venda do Usuário', detail: 'Requisição de carga dos pontos de venda do usuário terminou com o erro: ' + error?.response?.data, life: 10000 })
  }
}

const form = ref(null)
const formValues = ref({ pontos: [], ponto: 0 })

const formSchema = createSalePointSchema(() => userProfiles.value)
const formValidator = zodResolver(formSchema)

const userProfiles = ref(0)

const save = async ({ valid, values }) => {
  if (!valid) return

  if (userProfiles.value < 2) {
    const userSalePoint = userSalePoints.value.length ? userSalePoints.value[0] : { pontoVenda: { id: null }, usuario: { id: props.userId } }

    userSalePoint.pontoVenda.id = values.ponto

    try {
      const response = await api.post('/user-sale-point', userSalePoint)

      if (response.status === 200) {
        userSalePoints.value = [response.data]

        toast.add({ severity: 'success', summary: 'Sucesso', detail: 'Seleção de Pontos de Venda salva com sucesso', life: 10000 })
      }
    } catch (error) {
      toast.add({ severity: 'error', summary: 'Falha de Gravação da seleção de Pontos de Venda', detail: 'Requisição de gravação da seleção de Pontos de Venda terminou com o erro: ' + error?.response?.data, life: 10000 })
    }
  } else {
    const points = []

    for (let ponto of values.pontos) {
      points.push({ pontoVenda: { id: ponto }, usuario: { id: props.userId } })
    }

    try {
      const response = await api.post('/user-sale-point/save-sale-points', points)

      if (response.status === 200) {
        toast.add({ severity: 'success', summary: 'Sucesso', detail: 'Seleção de Pontos de Venda salva com sucesso', life: 10000 })
        
        loadUserSalePoints()
      }
    } catch (error) {
      toast.add({ severity: 'error', summary: 'Falha de Gravação da seleção de Pontos de Venda', detail: 'Requisição de gravação da seleção de Pontos de Venda terminou com o erro: ' + error?.response?.data, life: 10000 })
    }
  }
}

</script>

<template>
  <Card class="mb-6" v-show="eAdmin()">
    <template #title>
      <div class="grid grid-cols-2">
        <h3>Pontos de Venda</h3>
        <div class="flex justify-end items-center">
          <Button icon="pi pi-replay" @click="router.back()" class="p-button-text" v-tooltip.bottom="'Voltar'"/>
        </div>
      </div>
    </template>
    <template #content>
      <Form ref="form" :resolver="formValidator" :initialValues="formValues" @submit="save" class="grid flex flex-column gap-2">
        <FormField v-slot="$field" name="pontos" v-show="userProfiles === 2">
          <div class="flex items-start gap-2">
            <div v-for="ponto in salePoints" :key="ponto.id" class="flex items-center gap-2 mb-2">
              <Checkbox v-model="$field.value" :value="ponto.id" :inputId="'checkbox_' + ponto.id"/>
              <label :for="'checkbox_' + ponto.id">{{ ponto.nome }}</label>
            </div>
          </div>
          <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
        </FormField>

        <FormField v-slot="$field" name="ponto" v-show="userProfiles < 2">
          <div class="flex items-start gap-2">
            <div v-for="ponto in salePoints" :key="ponto.id" class="flex items-center gap-2 mb-2">
              <RadioButton v-model="$field.value" :value="ponto.id" :inputId="'radiobutton_' + ponto.id"/>
              <label :for="'radiobutton_' + ponto.id">{{ ponto.nome }}</label>
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
