<script setup>
import { StateService } from '@/service/StateService'
import api from '@/util/api'
import { onlyDigits } from '@/util/util'
import { zodResolver } from '@primevue/forms/resolvers/zod'
import { useToast } from 'primevue/usetoast'
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { z } from 'zod'

const router = useRouter()
const route = useRoute()

const toast = useToast()

const formRef = ref(null)
const initialValues = ref({
  razaoSocial: '',
  fantasia: '',
  cnpj: '',
  fone: '',
  endereco: '',
  bairro: '',
  cep: '',
  cidade: '',
  uf: ''
})

const states = ref([])

const resolverUser = zodResolver(
  z.object({
    razaoSocial: z.string().min(1, { message: 'Razão Social é obrigatório.' }),
    fantasia: z.string().min(1, { message: 'Nome de Fantasia é obrigatório.' }),
    cnpj: z.string().length(18, { message: 'CNPJ é obrigatório.' }),
    fone: z.string().length(15, { message: 'Fone é obrigatório.' }),
    endereco: z.string().min(1, { message: 'Endereço é obrigatório.' }),
    bairro: z.string().min(1, { message: 'Bairro é obrigatório.' }),
    cep: z.string().length(9, { message: 'CEP é obrigatório.' }),
    cidade: z.string().min(1, { message: 'Cidade é obrigatório.' }),
    uf: z.string().length(2, { message: 'UF é obrigatório.' })
  })
)

const contacts = ref([])
const loading = ref(false)

let id = route.query.id

async function loadProfiles() {
  loading.value = true

  try {
    const res = await api.get('/user/profiles')

    contacts.value = res.data
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Perfis', detail: 'Requisição de perfis terminou com o erro: ' + error.response.data, life: 10000 })
  } finally {
    loading.value = false
  }
}

async function load() {
  loading.value = true

  try {
    const res = await api.get('/provider', { params: { id } })

    if (formRef.value) {
      formRef.value.setValues({
        razaoSocial: res.data.razaoSocial,
        fantasia: res.data.fantasia,
        cnpj: res.data.cnpj,
        fone: res.data.fone,
        endereco: res.data.endereco,
        bairro: res.data.bairro,
        cep: res.data.cep,
        cidade: res.data.cidade,
        uf: res.data.uf
      })
    }

    initialValues.value = {
      razaoSocial: res.data.razaoSocial,
      fantasia: res.data.fantasia,
      cnpj: res.data.cnpj,
      fone: res.data.fone,
      endereco: res.data.endereco,
      bairro: res.data.bairro,
      cep: res.data.cep,
      cidade: res.data.cidade,
      uf: res.data.uf
    }
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Fornecedor', detail: 'Requisição de fornecedor terminou com o erro: ' + error.response.data, life: 10000 })
  } finally {
    loading.value = false
  }
}

const save = async ({ valid, values }) => {
  if (!valid) return

  loading.value = true

  let params = { ... values }

  params['id'] = parseInt(id)

  for (let field of ['cnpj', 'fone', 'cep']) {
    params[field] = onlyDigits(params[field])
  }

  try {
    const response = await api.post('/provider', params)

    if (response.status === 200) {
      toast.add({ severity: 'success', summary: 'Sucesso', detail: 'Fornecedor atualizado com sucesso', life: 10000 })
    }
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Gravação de Fornecedor', detail: 'Requisição de alteração de fornecedor terminou com o erro: ' + error.response.data, life: 10000 })
  } finally {
    loading.value = false
  }
}

function cancel() {
  router.back()
}

onMounted(() => {
  StateService.getStates().then(data => states.value = data)

  if (id) {
    loadProfiles()
    load()
  }
})
</script>

<template>
  <BlockUI :blocked="loading" fullScreen>
    <Card class="mb-4">
      <template #title><h3>Editar Fornecedor</h3></template>

      <template #content>
        <Form ref="formRef" :resolver="resolverUser" :initialValues @submit="save" class="grid flex flex-column gap-4">
          <FormField v-slot="$field" name="razaoSocial" initialValue="">
            <FloatLabel variant="on">
              <InputText id="razaoSocial" maxlength="255" autocomplete="off" fluid/>
              <label for="razaoSocial">Razão Social</label>
            </FloatLabel>
            <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
          </FormField>

          <FormField v-slot="$field" name="fantasia" initialValue="">
            <FloatLabel variant="on">
              <InputText id="fantasia" maxlength="255" autocomplete="off" fluid/>
              <label for="fantasia">Nome de Fantasia</label>
            </FloatLabel>
            <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
          </FormField>

          <div class="grid grid-cols-2 gap-4">
            <FormField v-slot="$field" name="cnpj" initialValue="">
              <FloatLabel variant="on">
                <InputMask id="cnpj" v-model="$field.value" mask="99.999.999/9999-99" autocomplete="off" fluid/>
                <label for="cnpj">CNPJ</label>
              </FloatLabel>
              <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
            </FormField>

            <FormField v-slot="$field" name="fone" initialValue="">
              <FloatLabel variant="on">
                <InputMask id="fone" mask="(99) 99999-9999" autocomplete="off" fluid/>
                <label for="fone">Fone</label>
              </FloatLabel>
              <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
            </FormField>
          </div>

          <FormField v-slot="$field" name="endereco" initialValue="">
            <FloatLabel variant="on">
              <InputText id="endereco" maxlength="255" autocomplete="off" fluid/>
              <label for="endereco">Endereço</label>
            </FloatLabel>
            <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
          </FormField>

          <div class="grid grid-cols-12 gap-4">
            <div class="col-span-10">
              <FormField v-slot="$field" name="bairro" initialValue="">
                <FloatLabel variant="on">
                  <InputText id="bairro" maxlength="100" autocomplete="off" fluid/>
                  <label for="bairro">Bairro</label>
                </FloatLabel>
                <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
              </FormField>
            </div>

            <div class="col-span-2">
              <FormField v-slot="$field" name="cep" initialValue="">
                <FloatLabel variant="on">
                  <InputMask id="cep" mask="99999-999" autocomplete="off" fluid/>
                  <label for="cep">CEP</label>
                </FloatLabel>
                <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
              </FormField>
            </div>
          </div>

          <div class="grid grid-cols-12 gap-4">
            <div class="col-span-10">
              <FormField v-slot="$field" name="cidade" initialValue="">
                <FloatLabel variant="on">
                  <InputText id="cidade" maxlength="100" autocomplete="off" fluid/>
                  <label for="cidade">Cidade</label>
                </FloatLabel>
                <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
              </FormField>
            </div>

            <div class="col-span-2">
              <FormField v-slot="$field" name="uf" initialValue="">
                <FloatLabel variant="on">
                  <Select id="uf" :options="states" optionLabel="name" optionValue="code" fluid/>
                  <label for="uf">Estado</label>
                </FloatLabel>
                <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
              </FormField>
            </div>
          </div>

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
  </BlockUI>
</template>
