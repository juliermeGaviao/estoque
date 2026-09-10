<script setup>
import { StateService } from '@/service/StateService'
import api from '@/util/api'
import { onlyDigits } from '@/util/util'
import { zodResolver } from '@primevue/forms/resolvers/zod'
import { useToast } from 'primevue/usetoast'
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { z } from 'zod'
import Contact from './Contact.vue'

const router = useRouter()
const route = useRoute()
const toast = useToast()
const states = ref([])

const providerForm = ref(null)
const providerFormValues = ref({ razaoSocial: '', fantasia: '', cnpj: '', fone: '', endereco: '', bairro: '', cep: '', cidade: '', uf: '' })

const providerFormValidator = zodResolver(
  z.object({
    razaoSocial: z.string().trim().min(1, { message: 'Razão Social é obrigatório.' }),
    fantasia: z.string().trim().min(1, { message: 'Nome de Fantasia é obrigatório.' }),
    cnpj: z.string().length(18, { message: 'CNPJ é obrigatório.' }),
    fone: z.string().length(15, { message: 'Fone é obrigatório.' }),
    endereco: z.string().trim().min(1, { message: 'Endereço é obrigatório.' }),
    bairro: z.string().trim().min(1, { message: 'Bairro é obrigatório.' }),
    cep: z.string().trim().length(9, { message: 'CEP é obrigatório.' }),
    cidade: z.string().trim().min(1, { message: 'Cidade é obrigatório.' }),
    uf: z.string().length(2, { message: 'UF é obrigatório.' })
  })
)

const id = ref(route.query.id)

async function load() {
  try {
    const res = await api.get('/provider', { params: { id: id.value } })

    if (providerForm.value) {
      providerForm.value.setValues({
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
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Fornecedor', detail: 'Requisição de fornecedor terminou com o erro: ' + error?.response?.data, life: 10000 })
  }
}

const save = async ({ valid, values }) => {
  if (!valid) return

  let params = { ... values }

  for (let field of ['cnpj', 'fone', 'cep']) {
    params[field] = onlyDigits(params[field])
  }

  for (let param in params) {
    if (typeof params[param] === 'string') {
      params[param] = params[param].trim()
    }
  }

  params['id'] = Number.parseInt(id.value)

  try {
    const response = await api.post('/provider', params)

    if (response.status === 200) {
      id.value = response.data.id

      toast.add({ severity: 'success', summary: 'Sucesso', detail: 'Fornecedor atualizado com sucesso', life: 10000 })
    }
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Gravação de Fornecedor', detail: 'Requisição de alteração de fornecedor terminou com o erro: ' + error?.response?.data, life: 10000 })
  }
}

onMounted(() => {
  StateService.getStates().then(data => states.value = data)

  if (id.value) {
    load()
  }
})
</script>

<template>
  <ConfirmDialog :closable="false"></ConfirmDialog>
  <Card class="mb-4">
    <template #title>
      <div class="grid grid-cols-2">
        <h3>{{ id ? 'Editar' : 'Inserir' }} Fornecedor</h3>
        <div class="flex justify-end items-center">
          <Button icon="pi pi-replay" @click="router.push('/register/provider')" class="p-button-text" v-tooltip.bottom="'Voltar'"/>
        </div>
      </div>
    </template>

    <template #content>
      <Form ref="providerForm" :resolver="providerFormValidator" :initialValues="providerFormValues" @submit="save" class="grid flex flex-column gap-2">
        <FormField v-slot="$field" name="razaoSocial">
          <FloatLabel variant="on">
            <InputText id="razaoSocial" maxlength="255" autocomplete="off" fluid/>
            <label for="razaoSocial">Razão Social</label>
          </FloatLabel>
          <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
        </FormField>

        <FormField v-slot="$field" name="fantasia">
          <FloatLabel variant="on">
            <InputText id="fantasia" maxlength="255" autocomplete="off" fluid/>
            <label for="fantasia">Nome de Fantasia</label>
          </FloatLabel>
          <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
        </FormField>

        <div class="grid grid-cols-2 gap-2">
          <FormField v-slot="$field" name="cnpj">
            <FloatLabel variant="on">
              <InputMask id="cnpj" v-model="$field.value" mask="99.999.999/9999-99" autocomplete="off" fluid/>
              <label for="cnpj">CNPJ</label>
            </FloatLabel>
            <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
          </FormField>

          <FormField v-slot="$field" name="fone">
            <FloatLabel variant="on">
              <InputMask id="fone" mask="(99) 99999-9999" autocomplete="off" fluid/>
              <label for="fone">Fone</label>
            </FloatLabel>
            <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
          </FormField>
        </div>

        <FormField v-slot="$field" name="endereco">
          <FloatLabel variant="on">
            <InputText id="endereco" maxlength="255" autocomplete="off" fluid/>
            <label for="endereco">Endereço</label>
          </FloatLabel>
          <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
        </FormField>

        <div class="grid grid-cols-12 gap-2">
          <div class="col-span-10">
            <FormField v-slot="$field" name="bairro">
              <FloatLabel variant="on">
                <InputText id="bairro" maxlength="100" autocomplete="off" fluid/>
                <label for="bairro">Bairro</label>
              </FloatLabel>
              <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
            </FormField>
          </div>

          <div class="col-span-2">
            <FormField v-slot="$field" name="cep">
              <FloatLabel variant="on">
                <InputMask id="cep" mask="99999-999" autocomplete="off" fluid/>
                <label for="cep">CEP</label>
              </FloatLabel>
              <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
            </FormField>
          </div>
        </div>

        <div class="grid grid-cols-12 gap-2">
          <div class="col-span-10">
            <FormField v-slot="$field" name="cidade">
              <FloatLabel variant="on">
                <InputText id="cidade" maxlength="100" autocomplete="off" fluid/>
                <label for="cidade">Cidade</label>
              </FloatLabel>
              <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
            </FormField>
          </div>

          <div class="col-span-2">
            <FormField v-slot="$field" name="uf">
              <FloatLabel variant="on">
                <Select id="uf" :options="states" optionLabel="name" optionValue="code" fluid/>
                <label for="uf">Estado</label>
              </FloatLabel>
              <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
            </FormField>
          </div>
        </div>

        <FormField class="flex justify-end gap-2">
          <Button label="Limpar" icon="pi pi-times" type="reset" severity="secondary" raised/>
          <Button label="Salvar" icon="pi pi-save" type="submit" raised/>
        </FormField>
      </Form>
    </template>
  </Card>
  <Contact :id="id"/>
</template>
