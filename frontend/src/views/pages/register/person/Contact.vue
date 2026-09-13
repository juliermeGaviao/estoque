<script setup>
import { StateService } from '@/service/StateService'
import api from '@/util/api'
import { formatPhone, onlyDigits } from '@/util/util'
import { zodResolver } from '@primevue/forms/resolvers/zod'
import { useConfirm } from "primevue/useconfirm"
import { useToast } from 'primevue/usetoast'
import { nextTick, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { z } from 'zod'

const props = defineProps({
  id: { type: [Number, String], default: null }
})

const router = useRouter()
const toast = useToast()
const states = ref([])
const confirm = useConfirm()

const id = ref(props.id)

const data = ref([])
const totalRecords = ref(0)
const page = ref(0)
const size = ref(20)
const sortField = ref(null)
const sortOrder = ref(null)

const contactForm = ref(null)
const contactFormValues = ref({ whatsapp: '', email: '', observacoes: '' })

const contactFormValidator = zodResolver(
  z.object({
    whatsapp: z.string().min(15, { message: 'Whatsapp é obrigatório.' }),
    email: z.string().trim().optional().refine(val => !val || val.trim() === '' || z.string().email().safeParse(val).success, { message: 'E-mail inválido ou vazio.' }),
    observacoes: z.string().trim().optional()
  })
)

let idContact
const visible = ref(false)

async function load() {
  try {
    let params = {
      idPessoa: id.value,
      page: page.value,
      size: size.value
    }

    if (sortField?.value) {
      params.sort = sortField.value
  
      if (sortOrder?.value) {
        params.sort += sortOrder.value === 1 ? ',asc' : ',desc'
      }
    }

    const response = await api.get('/person-client-contact/list', { params: params })

    data.value = response.data.content
    totalRecords.value = response.data.totalElements
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Contatos da Pessoa Cliente', detail: 'Requisição de lista de contatos da pessoa cliente terminou com o erro: ' + error.response.data, life: 10000 })
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

      contactForm.value.setValues({
        nome: contact.nome,
        whatsapp: contact.whatsapp,
        email: contact.email,
        dataAniversario: new Date(contact.dataAniversario),
        observacoes: contact.observacoes
      })
    } else {
      contactForm.value.setValues(contactFormValues.value)
    }
  })
}

const save = async ({ valid, values }) => {
  if (!valid) return

  let params = { ... values }

  params['cliente'] = { "id": id.value }

  if (idContact) {
    params['id'] = idContact
  }

  for (let field of ['whatsapp']) {
    params[field] = onlyDigits(params[field])
  }

  for (let param in params) {
    if (typeof params[param] === 'string') {
      params[param] = params[param].trim()
    }
  }

  try {
    const response = await api.post('/person-client-contact', params)

    if (response.status === 200) {
      toast.add({ severity: 'success', summary: 'Sucesso', detail: 'Contato de Pessoa Cliente atualizado com sucesso', life: 10000 })
    }
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Gravação de Contato de Pessoa Cliente', detail: 'Requisição de alteração de pessoa cliente de fornecedor terminou com o erro: ' + error.response.data, life: 10000 })
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
        await api.delete(`/person-client-contact?id=${entity.id}`)

        toast.add({ severity: 'success', summary: 'Sucesso', detail: 'Contato de Pessoa Cliente removido com sucesso', life: 10000 })
      } catch (error) {
        toast.add({ severity: 'error', summary: 'Falha de Remoção de Contato de Pessoa Cliente', detail: 'Requisição de remoção de contato de pessoa cliente terminou com o erro: ' + error?.response?.data, life: 10000 })
      } finally {
        load()
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
  <Card>
    <template #title>
      <div class="grid grid-cols-2">
        <h3>Lista de Contatos da Pessoa Cliente</h3>
        <div class="flex justify-end items-center">
          <Button icon="pi pi-replay" @click="router.push('/register/person')" class="p-button-text" v-tooltip.bottom="'Voltar'"/>
        </div>
      </div>
    </template>
    <template #content>
      <DataTable :value="data" :lazy="true" :paginator="true" :rows="size" :totalRecords="totalRecords"
        :first="page * size" @page="onPage" @sort="onSort" :sortField="sortField" :sortOrder="sortOrder" responsiveLayout="scroll" stripedRows
        :rowsPerPageOptions="[20, 40, 60, 100]" size="small">

        <Column field="id" header="Id" sortable/>
        <Column field="whatsapp" header="Whatsapp">
          <template #body="slotProps">
            {{ formatPhone(slotProps.data.whatsapp) }}
          </template>
        </Column>
        <Column field="email" header="E-mail" sortable/>

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
    <Form ref="contactForm" :resolver="contactFormValidator" :initialValues="contactFormValues" @submit="save" class="grid flex flex-column gap-2">
      <div class="grid grid-cols-2 gap-2 mt-1">
        <FormField v-slot="$field" name="whatsapp">
          <FloatLabel variant="on">
            <InputMask id="whatsapp" mask="(99) 99999-9999" autocomplete="off" fluid/>
            <label for="whatsapp">Whatsapp</label>
          </FloatLabel>
          <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
        </FormField>

        <FormField v-slot="$field" name="email" initialValue="">
          <FloatLabel variant="on" class="flex-1">
            <InputText id="email" maxlength="100" autocomplete="off" fluid/>
            <label for="email">E-mail</label>
          </FloatLabel>
          <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
        </FormField>
      </div>

      <FormField v-slot="$field" name="observacoes" initialValue="">
        <FloatLabel variant="on" class="flex-1">
          <Textarea id="observacoes" rows="3" size="1024" style="resize: none" fluid/>
          <label for="email">Observações</label>
        </FloatLabel>
        <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
      </FormField>

      <FormField class="flex justify-end gap-2">
        <Button label="Limpar" icon="pi pi-times" type="reset" severity="secondary" raised/>
        <Button label="Cancelar" icon="pi pi-ban" @click="visible = false" severity="secondary" raised/>
        <Button label="Salvar" icon="pi pi-save" type="submit" raised/>
      </FormField>
    </Form>
  </Dialog>
</template>
