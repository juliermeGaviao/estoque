<script setup>
import { StateService } from '@/service/StateService'
import api from '@/util/api'
import { formatPhone, onlyDigits } from '@/util/util'
import { zodResolver } from '@primevue/forms/resolvers/zod'
import { useConfirm } from "primevue/useconfirm"
import { useToast } from 'primevue/usetoast'
import { nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { z } from 'zod'

const router = useRouter()
const route = useRoute()
const toast = useToast()
const states = ref([])
const loading = ref(false)
const confirm = useConfirm()

const providerForm = ref(null)
const providerFormValues = ref({ razaoSocial: '', fantasia: '', cnpj: '', fone: '', endereco: '', bairro: '', cep: '', cidade: '', uf: '' })

const providerFormValidator = zodResolver(
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

const id = ref(route.query.id)

const data = ref([])
const totalRecords = ref(0)
const page = ref(0)
const size = ref(20)
const sortField = ref(null)
const sortOrder = ref(null)

const contactForm = ref(null)
const contactFormValues = ref({ nome: '', cargo: '', celular: '' })

const contactFormValidator = zodResolver(
  z.object({
    nome: z.string().min(1, { message: 'Nome do Contato é obrigatório.' }),
    cargo: z.string().min(1, { message: 'Cargo é obrigatório.' }),
    celular: z.string().length(15, { message: 'Número do Celular é obrigatório.' })
  })
)

let idContact
const visible = ref(false)

async function load() {
  loading.value = true

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
    toast.add({ severity: 'error', summary: 'Falha de Carga de Fornecedor', detail: 'Requisição de fornecedor terminou com o erro: ' + error.response.data, life: 10000 })
  } finally {
    loading.value = false
  }

  loadContacts()
}

async function loadContacts() {
  loading.value = true

  try {
    let params = {
      idFornecedor: id.value,
      page: page.value,
      size: size.value
    }

    if (sortField.value) {
      params.sort = sortField.value
  
      if (sortOrder) {
        params.sort += sortOrder.value === 1 ? ',asc' : ',desc'
      }
    }

    const response = await api.get('/provider-contact/list', { params: params })

    data.value = response.data.content
    totalRecords.value = response.data.totalElements
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Contatos do Fornecedor', detail: 'Requisição de lista de contatos do fornecedor terminou com o erro: ' + error.response.data, life: 10000 })
  } finally {
    loading.value = false
  }
}

const save = async ({ valid, values }) => {
  if (!valid) return

  loading.value = true

  let params = { ... values }

  for (let field of ['cnpj', 'fone', 'cep']) {
    params[field] = onlyDigits(params[field])
  }

  for (let param in params) {
    if (typeof params[param] === 'string') {
      params[param] = params[param].trim()
    }
  }

  params['id'] = parseInt(id.value)

  try {
    const response = await api.post('/provider', params)

    if (response.status === 200) {
      id.value = response.data.id

      toast.add({ severity: 'success', summary: 'Sucesso', detail: 'Fornecedor atualizado com sucesso', life: 10000 })
    }
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Gravação de Fornecedor', detail: 'Requisição de alteração de fornecedor terminou com o erro: ' + error.response.data, life: 10000 })
  } finally {
    loading.value = false
  }
}

function onPage(event) {
  page.value = event.page
  size.value = event.rows
  loadContacts()
}

function onSort(event) {
  sortField.value = event.sortField
  sortOrder.value = event.sortOrder
  loadContacts()
}

function edit(contact) {
  visible.value = true

  nextTick(() => {
    if (contact) {
      idContact = contact.id
      contactForm.value.setValues({ nome: contact.nome, cargo: contact.cargo, celular: contact.celular })
    } else {
      contactForm.value.setValues(contactFormValues.value)
    }
  })
}

const saveContact = async ({ valid, values }) => {
  if (!valid) return

  loading.value = true

  let params = { ... values }

  params['fornecedor'] = { "id": id.value }

  if (idContact) {
    params['id'] = idContact
  }

  for (let field of ['celular']) {
    params[field] = onlyDigits(params[field])
  }

  for (let param in params) {
    if (typeof params[param] === 'string') {
      params[param] = params[param].trim()
    }
  }

  try {
    const response = await api.post('/provider-contact', params)

    if (response.status === 200) {
      toast.add({ severity: 'success', summary: 'Sucesso', detail: 'Contato de Fornecedor atualizado com sucesso', life: 10000 })
    }
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Gravação de Contato de Fornecedor', detail: 'Requisição de alteração de contato de fornecedor terminou com o erro: ' + error.response.data, life: 10000 })
  } finally {
    visible.value = false
    loading.value = false
    loadContacts()
  }
}

const confirmDelete = entity => {
  confirm.require({
    message: 'Deseja remover o contato?',
    header: "Alerta",
    icon: 'pi pi-info-circle',
    rejectProps: {
      label: 'Cancelar',
      severity: 'secondary',
      raised: true
    },
    acceptProps: {
      label: 'Remover',
      severity: 'danger',
      raised: true
    },
    accept: async () => {
      loading.value = true

      try {
        await api.delete(`/provider-contact?id=${entity.id}`)

        toast.add({ severity: 'success', summary: 'Sucesso', detail: 'Contato de Fornecedor removido com sucesso', life: 10000 })
      } catch (error) {
        toast.add({ severity: 'error', summary: 'Falha de Remoção de Contato de Fornecedor', detail: 'Requisição de remoção de contato de fornecedor terminou com o erro: ' + error.response.data, life: 10000 })
      } finally {
        loading.value = false
        loadContacts()
      }
    }
  })
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
  <BlockUI :blocked="loading" fullScreen>
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
        <Form ref="providerForm" :resolver="providerFormValidator" :initialValues="providerFormValues" @submit="save" class="grid flex flex-column gap-4">
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

          <div class="grid grid-cols-2 gap-4">
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

          <div class="grid grid-cols-12 gap-4">
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

          <div class="grid grid-cols-12 gap-4">
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

          <FormField class="flex justify-end gap-4">
            <Button label="Limpar" icon="pi pi-times" type="reset" severity="secondary" raised/>
            <Button label="Salvar" icon="pi pi-save" type="submit" raised/>
          </FormField>
        </Form>
      </template>
    </Card>
    <Card>
      <template #title>
        <div class="grid grid-cols-2">
          <h3>Lista de Contatos do Fornecedor</h3>
          <div class="flex justify-end items-center">
            <Button icon="pi pi-replay" @click="router.push('/register/provider')" class="p-button-text" v-tooltip.bottom="'Voltar'"/>
          </div>
        </div>
      </template>
      <template #content>
        <DataTable :value="data" :lazy="true" :paginator="true" :rows="size" :totalRecords="totalRecords"
          :first="page * size" @page="onPage" @sort="onSort" :sortField="sortField" :sortOrder="sortOrder" responsiveLayout="scroll" stripedRows
          :rowsPerPageOptions="[10, 20, 50, 100]">

          <Column field="id" header="Id" sortable/>
          <Column field="nome" header="Nome" sortable/>
          <Column field="cargo" header="Cargo" sortable/>
          <Column field="celular" header="Celular">
            <template #body="slotProps">
              {{ formatPhone(slotProps.data.celular) }}
            </template>
          </Column>

          <Column headerClass="flex justify-center" bodyClass="flex justify-center">
            <template #header>
              <Button icon="pi pi-plus" class="p-button-sm p-button-text p-mr-2" @click="edit(null)" :disabled="!id" v-tooltip.bottom="'Novo Contato'"/>
            </template>

            <template #body="slotProps">
              <Button icon="pi pi-pencil" class="p-button-sm p-button-text p-mr-2" @click="edit(slotProps.data)" v-tooltip.bottom="'Editar'"/>
              <Button icon="pi pi-trash" class="p-button-sm p-button-text p-button-danger" @click="confirmDelete(slotProps.data)" v-tooltip.bottom="'Remover'"/>
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>
    <Dialog v-model:visible="visible" modal :closable="false" :header="idContact ? 'Editar Contato' : 'Inserir Contato'" style="width: 40%">
      <Form ref="contactForm" :resolver="contactFormValidator" :initialValues="contactFormValues" @submit="saveContact" class="grid flex flex-column gap-4">
        <FormField v-slot="$field" name="nome" class="mt-1">
          <FloatLabel variant="on">
            <InputText id="nome" maxlength="255" autocomplete="off" fluid/>
            <label for="nome">Nome</label>
          </FloatLabel>
          <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
        </FormField>

        <FormField v-slot="$field" name="cargo">
          <FloatLabel variant="on">
            <InputText id="cargo" maxlength="255" autocomplete="off" fluid/>
            <label for="cargo">Cargo</label>
          </FloatLabel>
          <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
        </FormField>

        <FormField v-slot="$field" name="celular">
          <FloatLabel variant="on">
            <InputMask id="celular" mask="(99) 99999-9999" autocomplete="off" fluid/>
            <label for="celular">Celular</label>
          </FloatLabel>
          <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
        </FormField>

        <FormField class="flex justify-end gap-4">
          <Button label="Cancelar" icon="pi pi-ban" @click="visible = false" severity="secondary" raised/>
          <Button label="Salvar" icon="pi pi-save" type="submit" raised/>
        </FormField>
      </Form>
    </Dialog>
  </BlockUI>
</template>
