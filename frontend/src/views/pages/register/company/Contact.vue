<script setup>
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
const confirm = useConfirm()

const id = ref(props.id)

const data = ref([])
const totalRecords = ref(0)
const page = ref(0)
const size = ref(20)
const sortField = ref(null)
const sortOrder = ref(null)

const form = ref(null)
const formValues = ref({ nome: '', cargo: '', fone: '', ramal: '', celular: '', email: '', dataAniversario: '', observacoes: '' })

const formValidator = zodResolver(
  z.object({
    nome: z.string().trim().min(1, { message: 'Nome do Contato é obrigatório.' }),
    cargo: z.string().trim().min(1, { message: 'Cargo é obrigatório.' }),
    fone: z.string().optional(),
    ramal: z.string().trim().optional(),
    celular: z.string().optional(),
    email: z.string().trim().min(1, { message: 'E-mail é obrigatório.' }).email({ message: 'E-mail inválido.' }),
    dataAniversario: z.date().optional(),
    observacoes: z.string().trim().optional()
  })
)

const idContact = ref(null)
const visible = ref(false)

async function load() {
  try {
    let params = {
      idEmpresa: id.value,
      page: page.value,
      size: size.value
    }

    if (sortField?.value) {
      params.sort = sortField.value
  
      if (sortOrder?.value) {
        params.sort += sortOrder.value === 1 ? ',asc' : ',desc'
      }
    }

    const response = await api.get('/company-client-contact/list', { params: params })

    data.value = response.data.content
    totalRecords.value = response.data.totalElements
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Contatos da Empresa Cliente', detail: 'Requisição de lista de contatos da empresa cliente terminou com o erro: ' + error?.response?.data, life: 10000 })
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
      idContact.value = contact.id

      form.value.setValues({
        nome: contact.nome,
        cargo: contact.cargo,
        fone: contact.fone,
        ramal: contact.ramal,
        celular: contact.celular,
        email: contact.email,
        dataAniversario: new Date(contact.dataAniversario),
        observacoes: contact.observacoes
      })
    } else {
      idContact.value = null
      form.value.setValues(formValues.value)
    }
  })
}

const save = async ({ valid, values }) => {
  if (!valid) return

  let params = { ... values }

  params['cliente'] = { "id": id.value }

  if (idContact?.value) {
    params['id'] = idContact.value
  }

  for (let field of ['fone', 'celular']) {
    params[field] = onlyDigits(params[field])
  }

  for (let param in params) {
    if (typeof params[param] === 'string') {
      params[param] = params[param].trim()
    }
  }

  try {
    const response = await api.post('/company-client-contact', params)

    if (response.status === 200) {
      toast.add({ severity: 'success', summary: 'Sucesso', detail: 'Contato de Empresa Cliente atualizado com sucesso', life: 10000 })
    }
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Gravação de Contato de Empresa Cliente', detail: 'Requisição de alteração de empresa cliente de fornecedor terminou com o erro: ' + error?.response?.data, life: 10000 })
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
        await api.delete(`/company-client-contact?id=${entity.id}`)

        toast.add({ severity: 'success', summary: 'Sucesso', detail: 'Contato de Empresa Cliente removido com sucesso', life: 10000 })
      } catch (error) {
        toast.add({ severity: 'error', summary: 'Falha de Remoção de Contato de Empresa Cliente', detail: 'Requisição de remoção de contato de empresa cliente terminou com o erro: ' + error?.response?.data, life: 10000 })
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

const modalHeader = () => {
  return idContact?.value ? 'Editar Contato' : 'Inserir Contato'
}

</script>

<template>
  <ConfirmDialog :closable="false"></ConfirmDialog>
  <Card class="mb-4">
    <template #title>
      <div class="grid grid-cols-2">
        <h3>Lista de Contatos da Empresa Cliente</h3>
        <div class="flex justify-end items-center">
          <Button icon="pi pi-replay" @click="router.push('/register/company')" class="p-button-text" v-tooltip.bottom="'Voltar'"/>
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
  <Dialog v-model:visible="visible" modal :closable="false" :header="modalHeader()" style="width: 40%">
    <Form ref="form" :resolver="formValidator" :initialValues="formValues" @submit="save" class="grid flex flex-column gap-2">
      <FormField v-slot="$field" name="nome" class="mt-1">
        <FloatLabel variant="on">
          <InputText id="nomeDialog" maxlength="255" autocomplete="off" fluid/>
          <label for="nomeDialog">Nome</label>
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

      <div class="grid grid-cols-3 gap-2">
        <FormField v-slot="$field" name="fone">
          <FloatLabel variant="on">
            <InputMask id="foneDialog" mask="(99) 9999-99999" autocomplete="off" fluid/>
            <label for="foneDialog">Fone</label>
          </FloatLabel>
          <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
        </FormField>

        <FormField v-slot="$field" name="ramal">
          <FloatLabel variant="on">
            <InputText id="ramal" maxlength="20" autocomplete="off" fluid/>
            <label for="ramal">Ramal</label>
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
      </div>

      <div class="grid grid-cols-12 gap-2">
        <div class="col-span-8">
          <FormField v-slot="$field" name="email" initialValue="">
            <FloatLabel variant="on" class="flex-1">
              <InputText id="email" maxlength="100" autocomplete="off" fluid/>
              <label for="email">E-mail</label>
            </FloatLabel>
            <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
          </FormField>
        </div>

        <div class="col-span-4">
          <FormField v-slot="$field" name="dataAniversario" initialValue="">
            <FloatLabel variant="on" class="flex-1">
              <DatePicker dateFormat="dd/mm/yy" showIcon :manualInput="false" fluid/>
              <label for="dataAniversario">Data de Aniversário</label>
            </FloatLabel>
            <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
          </FormField>
        </div>
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
