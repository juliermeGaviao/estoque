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
    nome: z.string().min(1, { message: 'Nome do Tipo de Produto é obrigatório.' })
  })
)

const id = ref(route.query.id)

async function load() {
  loading.value = true

  try {
    const res = await api.get('/product-type', { params: { id: id.value } })

    if (productTypeForm.value) {
      productTypeForm.value.setValues({
        nome: res.data.nome
      })
    }
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Tipo de Produto', detail: 'Requisição de tipo de produto terminou com o erro: ' + error.response.data, life: 10000 })
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

  params['id'] = Number.parseInt(id.value)

  try {
    const response = await api.post('/product-type', params)

    if (response.status === 200) {
      id.value = response.data.id

      toast.add({ severity: 'success', summary: 'Sucesso', detail: 'Tipo de Produto atualizado com sucesso', life: 10000 })
    }
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Gravação de Tipo de Produto', detail: 'Requisição de alteração de tipo de produto terminou com o erro: ' + error.response.data, life: 10000 })
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
          <h3>{{ id ? 'Editar' : 'Inserir' }} Tipo de Produto</h3>
          <div class="flex justify-end items-center">
            <Button icon="pi pi-replay" @click="router.push('/register/product-type')" class="p-button-text" v-tooltip.bottom="'Voltar'"/>
          </div>
        </div>
      </template>

      <template #content>
        <Form ref="productTypeForm" :resolver="productTypeFormValidator" :initialValues="productTypeFormValues" @submit="save" class="grid flex flex-column gap-2">
          <FormField v-slot="$field" name="nome">
            <FloatLabel variant="on">
              <InputText id="nome" maxlength="255" autocomplete="off" fluid/>
              <label for="nome">Nome do Tipo de Produto</label>
            </FloatLabel>
            <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
          </FormField>

          <FormField class="flex justify-end gap-2">
            <Button label="Limpar" icon="pi pi-times" type="reset" severity="secondary" raised/>
            <Button label="Salvar" icon="pi pi-save" type="submit" raised/>
          </FormField>
        </Form>
      </template>
    </Card>
  </BlockUI>
</template>
