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

const form = ref(null)
const formValues = ref({ nome: '', idEmpresa: null, cracha: null, limite: null, fone: '', dataAniversario: null, endereco: '', bairro: '', cep: '', cidade: '', uf: '' })

const formValidator = zodResolver(
  z.object({
    nome: z.string().trim().min(1, { message: 'Nome é obrigatório.' }),
    idEmpresa: z.number().nullable().optional(),
    cracha: z.string().trim().nullable().optional(),
    limite: z.number().nullable().optional(),
    fone: z.string().optional(),
    dataAniversario: z.any()
      .refine(val => {
        if (val === '' || !val) return false

        if (val instanceof Date && !Number.isNaN(val.getTime())) return true

        if (typeof val === 'string') {
          const date = new Date(val)
          return !Number.isNaN(date.getTime())
        }

        return false
        }, { message: 'Data de aniversário é obrigatória.' }
      ),
    endereco: z.string().trim().optional(),
    bairro: z.string().trim().optional(),
    cep: z.string().optional(),
    cidade: z.string().trim().optional(),
    uf: z.string().optional()
  })
)

const id = ref(route.query.id)

async function load() {
  try {
    const res = await api.get('/client', { params: { id: id.value } })

    if (form.value) {
      form.value.setValues({
        nome: res.data.nome,
        idEmpresa: res.data.empresa?.id,
        cracha: res.data.cracha,
        limite: res.data.limite,
        fone: res.data.fone,
        dataAniversario: new Date(res.data.dataAniversario),
        endereco: res.data.endereco,
        bairro: res.data.bairro,
        cep: res.data.cep,
        cidade: res.data.cidade,
        uf: res.data.uf
      })
    }
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Pessoa Cliente', detail: 'Requisição de pessoa cliente terminou com o erro: ' + error.response.data, life: 10000 })
  }
}

const save = async ({ valid, values }) => {
  if (!valid) return

  let params = { ... values }

  for (let field of ['fone', 'cep']) {
    params[field] = onlyDigits(params[field])
  }

  for (let param in params) {
    if (typeof params[param] === 'string') {
      params[param] = params[param].trim()
    }
  }

  if (params.idEmpresa) {
    params.empresa = { id: params.idEmpresa }
  }

  params['id'] = Number.parseInt(id.value)

  try {
    const response = await api.post('/client', params)

    if (response.status === 200) {
      id.value = response.data.id

      toast.add({ severity: 'success', summary: 'Sucesso', detail: 'Pessoa Cliente atualizada com sucesso', life: 10000 })
    }
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Gravação de Pessoa Cliente', detail: 'Requisição de alteração de pessoa cliente terminou com o erro: ' + error.response.data, life: 10000 })
  }
}

onMounted(() => {
  StateService.getStates().then(data => states.value = data)

  if (id.value) {
    load()
  }

  loadCompanies()
})

const companies = ref([])

async function loadCompanies() {
  try {
    const response = await api.get('/client/list-companies', { params: { page: 0, size: 10000, sort: 'nome,asc' } })

    companies.value = response.data.content
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Empresas', detail: 'Requisição de lista de Empresas terminou com o erro: ' + error.response.data, life: 10000 })
  }

}
</script>

<template>
  <ConfirmDialog :closable="false"></ConfirmDialog>
  <Card class="mb-4">
    <template #title>
      <div class="grid grid-cols-2">
        <h3>{{ id ? 'Editar' : 'Inserir' }} Pessoa Cliente</h3>
        <div class="flex justify-end items-center">
          <Button icon="pi pi-replay" @click="router.push('/register/person')" class="p-button-text" v-tooltip.bottom="'Voltar'"/>
        </div>
      </div>
    </template>

    <template #content>
      <Form ref="form" :resolver="formValidator" :initialValues="formValues" @submit="save" class="grid flex flex-column gap-2">
        <div class="grid grid-cols-12 gap-2">
          <div class="col-span-8">
            <FormField v-slot="$field" name="nome">
              <FloatLabel variant="on">
                <InputText id="nome" maxlength="255" autocomplete="off" fluid/>
                <label for="nome">Nome</label>
              </FloatLabel>
              <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
            </FormField>
          </div>

          <div class="col-span-4">
            <FormField name="idEmpresa">
              <FloatLabel variant="on">
                <Select id="idEmpresa" :options="companies" optionLabel="nome" optionValue="id" fluid/>
                <label for="idEmpresa">Empresa</label>
              </FloatLabel>
            </FormField>
          </div>
        </div>
        <div class="grid grid-cols-12 gap-2">
          <div class="col-span-3">
            <FormField name="cracha">
              <FloatLabel variant="on">
                <InputText id="cracha" maxlength="50" autocomplete="off" fluid/>
                <label for="cracha">Crachá</label>
              </FloatLabel>
            </FormField>
          </div>
          <div class="col-span-3">
            <FormField name="limite">
              <FloatLabel variant="on">
                <InputNumber id="limite" :max="10000" :minFractionDigits="2" :maxFractionDigits="2" fluid/>
                <label for="limite">Limite (R$)</label>
              </FloatLabel>
            </FormField>
          </div>
          <div class="col-span-3">
            <FormField v-slot="$field" name="fone">
              <FloatLabel variant="on">
                <InputMask id="fone" mask="(99) 99999-9999" autocomplete="off" fluid/>
                <label for="fone">Fone</label>
              </FloatLabel>
              <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
            </FormField>
          </div>

          <div class="col-span-3">
            <FormField v-slot="$field" name="dataAniversario" initialValue="">
              <FloatLabel variant="on" class="flex-1">
                <DatePicker dateFormat="dd/mm/yy" showIcon :manualInput="false" fluid/>
                <label for="dataAniversario">Data de Aniversário</label>
              </FloatLabel>
              <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
            </FormField>
          </div>
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
