<script setup>
import api from '@/util/api'
import { zodResolver } from '@primevue/forms/resolvers/zod'
import { useToast } from 'primevue/usetoast'
import { nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { z } from 'zod'

const router = useRouter()
const route = useRoute()
const toast = useToast()
const loading = ref(false)

const form = ref(null)
const formValues = ref({ nome: '', idTipoProduto: null, idFornecedor: null, referencia: '', peso: '', ativo: true })

const validade = [ { value: true, label: 'Sim' }, { value: false, label: 'Não' } ]

const formValidator = zodResolver(
  z.object({
    nome: z.string().min(1, { message: 'Nome do Produto é obrigatório.' }),
    idTipoProduto: z.int().positive({ message: 'Tipo de Produto é obrigatório.' }),
    idFornecedor: z.int().positive({ message: 'Tipo de Produto é obrigatório.' }),
    referencia: z.string().min(1, { message: 'Código de Referência é obrigatório.' }),
    peso: z.int().min(1, { message: 'Peso do Produto é obrigatório.' }).max(10000, 'Limite de peso é 10kg'),
    ativo: z.boolean( { required_error: 'O campo Ativo é obrigatório.' } )
  })
)

const id = ref(route.query.id)

async function load() {
  loading.value = true

  try {
    const res = await api.get('/product', { params: { id: id.value } })

    if (form.value) {
      form.value.setValues({
        nome: res.data.nome,
        idTipoProduto: res.data.tipoProduto.id,
        idFornecedor: res.data.fornecedor.id,
        referencia: res.data.referencia,
        peso: res.data.peso,
        ativo: res.data.ativo
      })
    }
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Produto', detail: 'Requisição de produto terminou com o erro: ' + error.response.data, life: 10000 })
  } finally {
    loading.value = false
  }
}

let tipos = ref([])

async function loadProductTypes() {
  loading.value = true

  try {
    const response = await api.get('/product-type/list', { params: { page: 0, size: 10000, sort: 'nome,asc' } })

    tipos.value = response.data.content
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Tipos de Produto', detail: 'Requisição de lista de Tipos de Produto terminou com o erro: ' + error.response.data, life: 10000 })
  } finally {
    loading.value = false
  }
}

const submitAction = ref('')
const formKey = ref(0)

const save = async ({ valid, values }) => {
  if (!valid) return

  loading.value = true

  let params = { ... values }

  delete params.idTipoProduto
  delete params.idFornecedor

  for (let param in params) {
    if (typeof params[param] === 'string') {
      params[param] = params[param].trim()
    }
  }

  params.tipoProduto = { id: values.idTipoProduto }
  params.fornecedor = { id: values.idFornecedor }

  params['id'] = Number.parseInt(id.value)

  try {
    const response = await api.post('/product', params)

    if (response.status === 200) {
      id.value = response.data.id

      toast.add({ severity: 'success', summary: 'Sucesso', detail: 'Produto atualizado com sucesso', life: 10000 })
    }
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Gravação de Produto', detail: 'Requisição de alteração de produto terminou com o erro: ' + error.response.data, life: 10000 })
  } finally {
    loading.value = false
  }

  if (submitAction.value === 'saveNew') {
    nextTick(() => {
      if (form.value) {
        form.value.setValues({ ...formValues.value })
        id.value = null
        formKey.value++
      }
    })
  }
}

onMounted(() => {
  if (id.value) {
    load()
  }

  loadProductTypes()
  loadProviders()
})

const providers = ref([])

async function loadProviders() {
  loading.value = true

  try {
    const response = await api.get('/provider/list', { params: { page: 0, size: 10000, sort: 'fantasia,asc' } })

    providers.value = response.data.content
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Fornecedores', detail: 'Requisição de lista de Fornecedores terminou com o erro: ' + error.response.data, life: 10000 })
  } finally {
    loading.value = false
  }
}

</script>

<template>
  <ConfirmDialog :closable="false"></ConfirmDialog>
  <BlockUI :blocked="loading" fullScreen>
    <Card class="mb-4">
      <template #title>
        <div class="grid grid-cols-2">
          <h3>{{ id ? 'Editar' : 'Inserir' }} Produto</h3>
          <div class="flex justify-end items-center">
            <Button icon="pi pi-replay" @click="router.push('/register/product')" class="p-button-text" v-tooltip.bottom="'Voltar'"/>
          </div>
        </div>
      </template>

      <template #content>
        <Form ref="form" :key="formKey" :resolver="formValidator" :initialValues="formValues" @submit="save" class="grid flex flex-column gap-4">
          <div class="grid grid-cols-12 gap-4">
            <div class="col-span-8">
              <FormField v-slot="$field" name="nome">
                <FloatLabel variant="on">
                  <InputText id="nome" maxlength="255" autocomplete="off" fluid/>
                  <label for="nome">Nome</label>
                </FloatLabel>
                <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
              </FormField>
            </div>
            <div class="col-span-2">
              <FormField v-slot="$field" name="idTipoProduto">
                <FloatLabel variant="on">
                  <Select v-bind="$field" :options="tipos" optionLabel="nome" optionValue="id" fluid/>
                  <label for="idTipoProduto">Tipo do Produto</label>
                </FloatLabel>
                <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
              </FormField>
            </div>
            <div class="col-span-2">
              <FormField v-slot="$field" name="idFornecedor">
                <FloatLabel variant="on">
                  <Select v-bind="$field" :options="providers" optionLabel="fantasia" optionValue="id" fluid/>
                  <label for="idFornecedor">Fornecedor</label>
                </FloatLabel>
                <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
              </FormField>
            </div>
          </div>

          <div class="grid grid-cols-12 gap-4">
            <div class="col-span-4">
              <FormField v-slot="$field" name="referencia">
                <FloatLabel variant="on">
                  <InputText id="referencia" maxlength="100" autocomplete="off" fluid/>
                  <label for="referencia">Código de Referência</label>
                </FloatLabel>
                <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
              </FormField>
            </div>

            <div class="col-span-4">
              <FormField v-slot="$field" name="peso">
                <FloatLabel variant="on">
                  <InputNumber name="peso" :max="10000" fluid/>
                  <label for="peso">Peso (em gramas)</label>
                </FloatLabel>
                <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
              </FormField>
            </div>

            <div class="col-span-4">
              <FormField v-slot="$field" name="ativo">
                <FloatLabel variant="on">
                  <Select v-bind="$field" :options="validade" optionLabel="label" optionValue="value" fluid/>
                  <label for="ativo">Ativo</label>
                </FloatLabel>
                <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
              </FormField>
            </div>
          </div>

          <FormField class="flex justify-end gap-4">
            <Button label="Limpar" icon="pi pi-times" type="reset" severity="secondary" raised/>
            <Button label="Salvar & Novo" icon="pi pi-plus" type="submit" iconPos="left" raised @click="submitAction = 'saveNew'"/>
            <Button label="Salvar" icon="pi pi-save" type="submit" raised @click="submitAction = 'save'"/>
          </FormField>
        </Form>
      </template>
    </Card>
  </BlockUI>
</template>
