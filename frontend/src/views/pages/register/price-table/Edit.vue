<script setup>
import api from '@/util/api'
import { zodResolver } from '@primevue/forms/resolvers/zod'
import { useToast } from 'primevue/usetoast'
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { z } from 'zod'

const router = useRouter()
const route = useRoute()
const toast = useToast()
const loading = ref(false)

const productTypeForm = ref(null)
const productTypeFormValues = ref({ nome: '' })

const productTypeFormValidator = zodResolver(
  z.object({
    nome: z.string().min(1, { message: 'Nome do Tabela de Preços é obrigatório.' })
  })
)

const id = ref(route.query.id)

async function load() {
  loading.value = true

  try {
    const res = await api.get('/price-table', { params: { id: id.value } })

    if (productTypeForm.value) {
      productTypeForm.value.setValues({
        nome: res.data.nome
      })
    }
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Tabela de Preços', detail: 'Requisição de tabela de preços terminou com o erro: ' + error.response.data, life: 10000 })
  } finally {
    loading.value = false
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

  params['id'] = parseInt(id.value)

  try {
    const response = await api.post('/price-table', params)

    if (response.status === 200) {
      id.value = response.data.id

      toast.add({ severity: 'success', summary: 'Sucesso', detail: 'Tabela de Preços atualizado com sucesso', life: 10000 })
    }
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Gravação de Tabela de Preços', detail: 'Requisição de alteração de tabela de preços terminou com o erro: ' + error.response.data, life: 10000 })
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (id.value) {
    load()
  }
})
</script>

<template>
  <ConfirmDialog :closable="false"></ConfirmDialog>
  <BlockUI :blocked="loading" fullScreen>
    <Card class="mb-4">
      <template #title>
        <div class="grid grid-cols-2">
          <h3>{{ id ? 'Editar' : 'Inserir' }} Tabela de Preços</h3>
          <div class="flex justify-end items-center">
            <Button icon="pi pi-replay" @click="router.push('/register/price-table')" class="p-button-text" v-tooltip.bottom="'Voltar'"/>
          </div>
        </div>
      </template>

      <template #content>
        <Form ref="productTypeForm" :resolver="productTypeFormValidator" :initialValues="productTypeFormValues" @submit="save" class="grid flex flex-column gap-4">
          <FormField v-slot="$field" name="nome">
            <FloatLabel variant="on">
              <InputText id="nome" maxlength="255" autocomplete="off" fluid/>
              <label for="nome">Nome da Tabela de Preços</label>
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
  </BlockUI>
</template>
