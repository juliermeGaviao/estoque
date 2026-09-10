<script setup>
import api from '@/util/api'
import { formatPhone, onlyDigits } from '@/util/util'
import { zodResolver } from '@primevue/forms/resolvers/zod'
import { useConfirm } from "primevue/useconfirm"
import { useToast } from 'primevue/usetoast'
import { nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { z } from 'zod'

const props = defineProps({
  id: { type: [Number, String], default: null }
})

const router = useRouter()
const route = useRoute()
const toast = useToast()
const confirm = useConfirm()

const id = ref(route.query.id)

const data = ref([])
const totalRecords = ref(0)
const page = ref(0)
const size = ref(20)
const sortField = ref(null)
const sortOrder = ref(null)

const form = ref(null)
const formValues = ref({ nome: '', cargo: '', celular: '' })

const formValidator = zodResolver(
  z.object({
    nome: z.string().trim().min(1, { message: 'Nome do Contato é obrigatório.' }),
    cargo: z.string().trim().min(1, { message: 'Cargo é obrigatório.' }),
    celular: z.string().length(15, { message: 'Número do Celular é obrigatório.' })
  })
)

let idContact
const visible = ref(false)

async function load() {
  try {
    let params = {
      idFornecedor: id.value,
      page: page.value,
      size: size.value
    }

    if (sortField?.value) {
      params.sort = sortField.value
  
      if (sortOrder?.value) {
        params.sort += sortOrder.value === 1 ? ',asc' : ',desc'
      }
    }

    const response = await api.get('/provider-contact/list', { params: params })

    data.value = response.data.content
    totalRecords.value = response.data.totalElements
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Contatos do Fornecedor', detail: 'Requisição de lista de contatos do fornecedor terminou com o erro: ' + error?.response?.data, life: 10000 })
  }
}

function onPage(event) {
  page.value = event.page
  size.value = event.rows
  load()
}

function onSort(event) {
  page.value = 0
  sortField.value = event.sortField
  sortOrder.value = event.sortOrder

  load()
}

function edit(contact) {
  visible.value = true

  nextTick(() => {
    if (contact) {
      idContact = contact.id
      form.value.setValues({ nome: contact.nome, cargo: contact.cargo, celular: contact.celular })
    } else {
      form.value.setValues(formValues.value)
    }
  })
}

const save = async ({ valid, values }) => {
  if (!valid) return

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
    toast.add({ severity: 'error', summary: 'Falha de Gravação de Contato de Fornecedor', detail: 'Requisição de alteração de contato de fornecedor terminou com o erro: ' + error?.response?.data, life: 10000 })
  } finally {
    visible.value = false
    load()
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
      try {
        await api.delete(`/provider-contact?id=${entity.id}`)

        toast.add({ severity: 'success', summary: 'Sucesso', detail: 'Contato de Fornecedor removido com sucesso', life: 10000 })
      } catch (error) {
        toast.add({ severity: 'error', summary: 'Falha de Remoção de Contato de Fornecedor', detail: 'Requisição de remoção de contato de fornecedor terminou com o erro: ' + error?.response?.data, life: 10000 })
      } finally {
        load()
      }
    }
  })
}

onMounted(() => {
  if (id.value) {
    load()
  }
})
</script>

<template>
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
        :rowsPerPageOptions="[20, 40, 60, 100]" size="small">

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
    <Form ref="form" :resolver="formValidator" :initialValues="formValues" @submit="save" class="grid flex flex-column gap-2">
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

      <FormField class="flex justify-end gap-2">
        <Button label="Cancelar" icon="pi pi-ban" @click="visible = false" severity="secondary" raised/>
        <Button label="Salvar" icon="pi pi-save" type="submit" raised/>
      </FormField>
    </Form>
  </Dialog>
</template>
