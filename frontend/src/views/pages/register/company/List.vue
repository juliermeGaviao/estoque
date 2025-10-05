<script setup>
import api from '@/util/api'
import { formatCpfCnpj, formatPhone, onlyDigits } from '@/util/util'
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
const size = ref(20)
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

  for (let field of ['cnpj', 'fone']) {
    query[field] = query[field] ? onlyDigits(query[field]) : null
  }

  loading.value = true

  try {
    const response = await api.get('/company-client/list', { params: query })

    data.value = response.data.content
    totalRecords.value = response.data.totalElements
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Empresas Cliente', detail: 'Requisição de lista de empresas cliente terminou com o erro: ' + error.response.data, life: 10000 })
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  load({})
})

function onPage(event) {
  page.value = event.page
  size.value = event.rows

  load( { ...filterValues.value } )
}

function onSort(event) {
  sortField.value = event.sortField
  sortOrder.value = event.sortOrder

  load( { ...filterValues.value } )
}

function edit(entity) {
  if (entity?.id) {
    router.push(`/register/company/edit?id=${entity.id}`)
  } else {
    router.push('/register/company/edit')
  }
}

const confirmDelete = entity => {
  confirm.require({
    message: 'Deseja remover a empresa cliente?',
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
        await api.delete(`/company-client?id=${entity.id}`)

        toast.add({ severity: 'success', summary: 'Sucesso', detail: 'Empresa cliente removida com sucesso', life: 10000 })

        load()
      } catch (error) {
        toast.add({ severity: 'error', summary: 'Falha de Remoção de Empresa Cliente', detail: 'Requisição de remoção de empresa cliente terminou com o erro: ' + error.response.data, life: 10000 })
      }
    }
  })
}

const form = ref(null)
const formValues = ref({ razaoSocial: null, fantasia: null, cnpj: null, fone: null })
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
</script>

<template>
  <ConfirmDialog :closable="false"></ConfirmDialog>
  <BlockUI :blocked="loading" fullScreen>
    <Card>
      <template #title><h3>Lista de Empresas Cliente</h3></template>
      <template #content>
        <Form ref="form" :initialValues="formValues" @submit="filter" @reset="limpar" class="grid flex flex-column gap-4 mb-4">
          <div class="grid grid-cols-12 gap-4">
            <div class="col-span-3">
              <FormField name="razaoSocial">
                <FloatLabel variant="on">
                  <InputText id="razaoSocial" maxlength="255" autocomplete="off" fluid/>
                  <label for="razaoSocial">Razão Social</label>
                </FloatLabel>
              </FormField>
            </div>
            <div class="col-span-3">
              <FormField name="fantasia">
                <FloatLabel variant="on">
                  <InputText id="fantasia" maxlength="255" autocomplete="off" fluid/>
                  <label for="fantasia">Nome de Fantasia</label>
                </FloatLabel>
              </FormField>
            </div>
            <div class="col-span-2">
              <FormField name="cnpj">
                <FloatLabel variant="on">
                  <InputMask id="cnpj" mask="99.999.999/9999-99" autocomplete="off" fluid/>
                  <label for="cnpj">CNPJ</label>
                </FloatLabel>
              </FormField>
            </div>
            <div class="col-span-2">
              <FormField name="fone">
                <FloatLabel variant="on">
                  <InputMask id="fone" mask="(99) 99999-9999" autocomplete="off" fluid/>
                  <label for="fone">Fone</label>
                </FloatLabel>
              </FormField>
            </div>
            <div class="col-span-2">
              <FormField class="flex justify-end gap-4">
                <Button label="Limpar" icon="pi pi-times" type="reset" severity="secondary" raised/>
                <Button label="Buscar" icon="pi pi-search" type="submit" raised/>
              </FormField>
            </div>
          </div>
        </Form>

        <DataTable :value="data" :lazy="true" :paginator="true" :rows="size" :totalRecords="totalRecords"
          :first="page * size" @page="onPage" @sort="onSort" :sortField="sortField" :sortOrder="sortOrder" responsiveLayout="scroll" stripedRows
          :rowsPerPageOptions="[10, 20, 50, 100]">

          <Column field="id" header="Id" sortable/>
          <Column field="razaoSocial" header="Razão Social" sortable/>
          <Column field="fantasia" header="Nome de Fantasia" sortable/>
          <Column field="cnpj" header="CNPJ" sortable>
            <template #body="slotProps">
              {{ formatCpfCnpj(slotProps.data.cnpj) }}
            </template>
          </Column>
          <Column field="fone" header="Fone">
            <template #body="slotProps">
              {{ formatPhone(slotProps.data.fone) }}
            </template>
          </Column>

          <Column headerClass="flex justify-center" bodyClass="flex justify-center">
            <template #header>
              <Button icon="pi pi-plus" class="p-button-sm p-button-text p-mr-2" @click="edit(null)" v-tooltip.bottom="'Nova Empresa Cliente'"/>
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
