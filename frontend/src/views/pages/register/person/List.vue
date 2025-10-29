<script setup>
import api from '@/util/api'
import { formatDate, formatNumber, formatPhone, onlyDigits } from '@/util/util'
import { useConfirm } from "primevue/useconfirm"
import { useToast } from 'primevue/usetoast'
import { nextTick, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const toast = useToast()
const confirm = useConfirm()

const data = ref([])
const totalRecords = ref(0)
const loading = ref(false)

const page = ref(0)
const size = ref(15)
const sortField = ref(null)
const sortOrder = ref(null)

async function load(params) {
  const query = {
    ...params,
    page: page.value,
    size: size.value,
  }

  if (sortField.value) {
    query.sort = sortField.value

    if (sortOrder) {
      query.sort += sortOrder.value === 1 ? ",asc" : ",desc"
    }
  }

  for (let field of ['fone']) {
    query[field] = query[field] ? onlyDigits(query[field]) : null
  }

  if (query.minAniversario) {
    query.minAniversario = formatDate(query.minAniversario)
  }

  if (query.maxAniversario) {
    query.maxAniversario = formatDate(query.maxAniversario)
  }

  try {
    const response = await api.get('/client/list-people', { params: query })

    data.value = response.data.content
    totalRecords.value = response.data.totalElements
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Pessoas Cliente', detail: 'Requisição de lista de pessoas cliente terminou com o erro: ' + error.response.data, life: 10000 })
  }
}

onMounted(async () => {
  loading.value = true

  load({})
  loadCompanies()

  loading.value = false
})

function onPage(event) {
  page.value = event.page
  size.value = event.rows

  loading.value = true
  load( { ...filterValues.value } )
  loading.value = false
}

function onSort(event) {
  page.value = 0
  sortField.value = event.sortField
  sortOrder.value = event.sortOrder

  loading.value = true
  load( { ...filterValues.value } )
  loading.value = false
}

function edit(entity) {
  if (entity?.id) {
    router.push(`/register/person/edit?id=${entity.id}`)
  } else {
    router.push('/register/person/edit')
  }
}

const confirmDelete = entity => {
  confirm.require({
    message: 'Deseja remover a pessoa cliente?',
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
        await api.delete(`/client?id=${entity.id}`)

        toast.add({ severity: 'success', summary: 'Sucesso', detail: 'Pessoa cliente removida com sucesso', life: 10000 })

        load()
      } catch (error) {
        toast.add({ severity: 'error', summary: 'Falha de Remoção de Pessoa Cliente', detail: 'Requisição de remoção de pessoa cliente terminou com o erro: ' + error.response.data, life: 10000 })
      } finally {
        loading.value = false
      }
    }
  })
}

const form = ref(null)
const formValues = ref({ nome: null, idEmpresa: null, fone: null, minAniversario: null, maxAniversario: null, minLimite: null, maxLimite: null })
const filterValues = ref({ ... formValues.value })

const filter = async ({ valid, values }) => {
  if (!valid) return

  filterValues.value = { ...values }
  page.value = 0

  load( { ...filterValues.value } )
}

function limpar() {
  nextTick(() => {
    page.value = 0
    filterValues.value = { ... formValues.value }
    sortField.value = null
    load( { ...filterValues.value } )
  })
}

const companies = ref([])

async function loadCompanies() {
  try {
    const response = await api.get('/client/list-companies', { params: { page: 0, size: 10000, sort: 'nome,asc' } })

    companies.value = response.data.content
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Empresas', detail: 'Requisição de lista de Empresas terminou com o erro: ' + error.response.data, life: 10000 })
  } finally {
    loading.value = false
  }

}
</script>

<template>
  <ConfirmDialog :closable="false"></ConfirmDialog>
  <BlockUI :blocked="loading" fullScreen>
    <Card>
      <template #title><h3>Lista de Pessoas Cliente</h3></template>
      <template #content>
        <Form ref="form" :initialValues="formValues" @submit="filter" @reset="limpar" class="grid flex flex-column gap-2 mb-4">
          <div class="grid grid-cols-12 gap-2">
            <div class="col-span-8">
              <FormField name="nome">
                <FloatLabel variant="on">
                  <InputText id="nome" maxlength="255" autocomplete="off" fluid/>
                  <label for="nome">Nome</label>
                </FloatLabel>
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
            <div class="col-span-2">
              <FormField name="fone">
                <FloatLabel variant="on">
                  <InputText id="fone" maxlength="255" autocomplete="off" fluid/>
                  <label for="fone">Fone</label>
                </FloatLabel>
              </FormField>
            </div>
            <div class="col-span-2">
              <FormField name="minLimite">
                <FloatLabel variant="on" class="flex-1">
                  <InputNumber id="minLimite" :max="10000" :minFractionDigits="2" :maxFractionDigits="2" fluid/>
                  <label for="minLimite">Limite Mínimo (R$)</label>
                </FloatLabel>
              </FormField>
            </div>
            <div class="col-span-2">
              <FormField name="maxLimite">
                <FloatLabel variant="on" class="flex-1">
                  <InputNumber id="maxLimite" :max="10000" :minFractionDigits="2" :maxFractionDigits="2" fluid/>
                  <label for="maxLimite">Limite Máximo (R$)</label>
                </FloatLabel>
              </FormField>
            </div>
            <div class="col-span-2">
              <FormField name="minAniversario">
                <FloatLabel variant="on" class="flex-1">
                  <DatePicker dateFormat="dd/mm/yy" fluid/>
                  <label for="minAniversario">Data de Aniversário Mínima</label>
                </FloatLabel>
              </FormField>
            </div>
            <div class="col-span-2">
              <FormField name="maxAniversario">
                <FloatLabel variant="on" class="flex-1">
                  <DatePicker dateFormat="dd/mm/yy" fluid/>
                  <label for="maxAniversario">Data de Aniversário Máxima</label>
                </FloatLabel>
              </FormField>
            </div>
            <div class="col-span-2">
              <FormField class="flex justify-end gap-2">
                <Button label="Limpar" icon="pi pi-times" type="reset" severity="secondary" raised/>
                <Button label="Buscar" icon="pi pi-search" type="submit" raised/>
              </FormField>
            </div>
          </div>
        </Form>

        <DataTable :value="data" :lazy="true" :paginator="true" :rows="size" :totalRecords="totalRecords"
          :first="page * size" @page="onPage" @sort="onSort" :sortField="sortField" :sortOrder="sortOrder" responsiveLayout="scroll" stripedRows
          :rowsPerPageOptions="[15, 30, 60, 100]" size="small">

          <Column field="id" header="Id" sortable/>
          <Column field="nome" header="Nome" sortable/>
          <Column field="empresa.nome" header="Empresa"/>
          <Column field="limite" header="Limite (R$)">
            <template #body="slotProps">
              {{ formatNumber(slotProps.data.limite ) }}
            </template>
          </Column>
          <Column field="fone" header="Fone">
            <template #body="slotProps">
              {{ formatPhone(slotProps.data.fone) }}
            </template>
          </Column>
          <Column field="dataAniversario" header="Data de Aniversário">
            <template #body="slotProps">
              {{ formatDate(slotProps.data.dataAniversario) }}
            </template>
          </Column>

          <Column headerClass="flex justify-center" bodyClass="flex justify-center">
            <template #header>
              <Button icon="pi pi-plus" class="p-button-sm p-button-text p-mr-2" @click="edit(null)" v-tooltip.bottom="'Nova Pessoa Cliente'"/>
            </template>

            <template #body="slotProps">
              <Button icon="pi pi-pencil" class="p-button-sm p-button-text p-mr-2" @click="edit(slotProps.data)" v-tooltip.bottom="'Editar'"/>
              <Button icon="pi pi-trash" class="p-button-sm p-button-text p-button-danger" @click="confirmDelete(slotProps.data)" v-tooltip.bottom="'Remover'"/>
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>
  </BlockUI>
</template>
