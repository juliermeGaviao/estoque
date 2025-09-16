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

const resolverUser = zodResolver(
  z.object({
    razaoSocial: z.string().min(1, { message: 'Razão Social é obrigatório.' }),
    fantasia: z.string().min(1, { message: 'Nome de Fantasia é obrigatório.' }),
    cnpj: z.string().min(1, { message: 'CNPJ é obrigatório.' }),
    fone: z.string().min(1, { message: 'Fone é obrigatório.' }),
    endereco: z.string().min(1, { message: 'Endereço é obrigatório.' }),
    bairro: z.string().min(1, { message: 'Bairro é obrigatório.' }),
    cep: z.string().min(1, { message: 'CEP é obrigatório.' }),
    cidade: z.string().min(1, { message: 'Cidade é obrigatório.' }),
    uf: z.string().min(1, { message: 'UF é obrigatório.' })
  })
)

const contacts = ref([])
const loading = ref(false)

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

async function load(id) {
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
    toast.add({ severity: 'error', summary: 'Falha de Carga de Usuário', detail: 'Requisição de usuário terminou com o erro: ' + error.response.data, life: 10000 })
  } finally {
    loading.value = false
  }
}

const save = async ({ valid, values }) => {
  if (!valid) return

  loading.value = true

  let params = { ... values }

  params['id'] = parseInt(route.query.id)

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

function clear(values) {
  values.razaoSocial = ''
  values.fantasia = ''
  values.cnpj = ''
  values.fone = ''
  values.endereco = ''
  values.bairro = ''
  values.cep = ''
  values.cidade = ''
  values.uf = ''
}

function cancel() {
  router.back()
}

onMounted(() => {
  loadProfiles()

  load(route.query.id)
})
</script>

<template>
  <BlockUI :blocked="loading" fullScreen>
    <Card class="mb-4">
      <template #title><h3>Editar Fornecedor</h3></template>

      <template #content>
        <Form ref="formRef" :resolver="resolverUser" :initialValues @submit="save" @reset="clear" class="grid flex flex-column gap-4">
          <FormField v-slot="$field" name="razaoSocial" initialValue="">
            <FloatLabel variant="on" class="flex-1">
              <InputText id="razaoSocial" maxlength="255" autocomplete="off" fluid/>
              <label for="razaoSocial">Razão Social</label>
            </FloatLabel>
            <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
          </FormField>

          <FormField v-slot="$field" name="fantasia" initialValue="">
            <FloatLabel variant="on" class="flex-1">
              <InputText id="fantasia" maxlength="255" autocomplete="off" fluid/>
              <label for="fantasia">Nome de Fantasia</label>
            </FloatLabel>
            <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
          </FormField>

          <div class="grid grid-cols-2 gap-4">
            <FormField v-slot="$field" name="cnpj" initialValue="">
              <FloatLabel variant="on" class="flex-1">
                <InputMask id="cnpj" mask="99.999.999/9999-99" autocomplete="off" fluid/>
                <label for="cnpj">CNPJ</label>
              </FloatLabel>
              <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
            </FormField>

            <FormField v-slot="$field" name="fone" initialValue="">
              <FloatLabel variant="on" class="flex-1">
                <InputMask id="fone" mask="(99) 99999-9999" autocomplete="off" fluid/>
                <label for="fone">Fone</label>
              </FloatLabel>
              <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
            </FormField>

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
