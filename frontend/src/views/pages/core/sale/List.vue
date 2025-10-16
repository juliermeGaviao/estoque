<script setup>
import api from '@/util/api'
import { eAdmin, getUserId } from '@/util/auth'
import { formatNumber } from '@/util/util'
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

  if (!eAdmin()) {
    query.idVendedor = getUserId()
  }

  if (sortField.value) {
    query.sort = sortField.value

    if (sortOrder) {
      query.sort += sortOrder.value === 1 ? ",asc" : ",desc"
    }
  } else {
    query.sort = "id,desc"
  }

  loading.value = true

  try {
    const response = await api.get("/sale/list", { params: query })

    data.value = response.data.content
    totalRecords.value = response.data.totalElements
  } catch (error) {
    toast.add({ severity: "error", summary: "Falha de Carga de Vendas", detail: "Requisição de lista de vendas terminou com o erro: " + error.response.data, life: 10000 })
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  load({})
  loadUsers()
  loadClients()
})

function onPage(event) {
  page.value = event.page
  size.value = event.rows

  load( { ...filterValues.value } )
}

function onSort(event) {
  page.value = 0
  sortField.value = event.sortField
  sortOrder.value = event.sortOrder

  load( { ...filterValues.value } )
}

function edit(entity) {
  if (entity?.id) {
    router.push(`/core/sale/edit?id=${entity.id}`)
  } else {
    router.push('/core/sale/edit')
  }
}

const confirmDelete = entity => {
  confirm.require({
    message: 'Deseja remover a venda?',
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
        await api.delete(`/sale?id=${entity.id}`)

        toast.add({ severity: 'success', summary: 'Sucesso', detail: 'Venda removida com sucesso', life: 10000 })

        load()
      } catch (error) {
        toast.add({ severity: 'error', summary: 'Falha de Remoção da Venda', detail: 'Requisição de remoção de venda terminou com o erro: ' + error.response.data, life: 10000 })
      }
    }
  })
}

let users = ref([])

async function loadUsers() {
  loading.value = true

  try {
    const response = await api.get('/user/list', { params: { page: 0, size: 10000, sort: 'email,asc' } })

    users.value = response.data.content
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Vendedores', detail: 'Requisição de lista de Vendedores terminou com o erro: ' + error.response.data, life: 10000 })
  } finally {
    loading.value = false
  }
}


let clients = ref([])

async function loadClients() {
  loading.value = true

  try {
    const response = await api.get('/client/find-all')

    clients.value = response.data
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Clientes', detail: 'Requisição de lista de Clientes terminou com o erro: ' + error.response.data, life: 10000 })
  } finally {
    loading.value = false
  }
}

const form = ref(null)
const formValues = ref({ idCliente: null, idVendedor: null, minDesconto: null, maxDesconto: null, observacoes: null })
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
      <template #title><h3>Lista de Vendas</h3></template>
      <template #content>
        <Form ref="form" :initialValues="formValues" @submit="filter" @reset="limpar" class="grid flex flex-column gap-4 mb-4">
          <div class="grid grid-cols-12 gap-4">
            <div :class="'col-span-' + (eAdmin() ? 2 : 4)">
              <FormField name="idCliente">
                <FloatLabel variant="on">
                  <Select :options="clients" optionLabel="nome" optionValue="id" fluid/>
                  <label for="idCliente">Clientes</label>
                </FloatLabel>
              </FormField>
            </div>
            <div :class="'col-span-' + (eAdmin() ? 2 : 3)" v-show="eAdmin()">
              <FormField name="idVendedor">
                <FloatLabel variant="on">
                  <Select :options="users" optionLabel="email" optionValue="id" fluid/>
                  <label for="idVendedor">Vendedores</label>
                </FloatLabel>
              </FormField>
            </div>
            <div class="col-span-2">
              <FormField name="minDesconto">
                <FloatLabel variant="on">
                  <InputNumber id="minDesconto" :max="100" :minFractionDigits="2" :maxFractionDigits="2" fluid/>
                  <label for="minDesconto">Desconto Mínimo (%)</label>
                </FloatLabel>
              </FormField>
            </div>
            <div class="col-span-2">
              <FormField name="maxDesconto">
                <FloatLabel variant="on">
                  <InputNumber id="maxDesconto" :max="100" :minFractionDigits="2" :maxFractionDigits="2" fluid/>
                  <label for="maxDesconto">Desconto Máximo (%)</label>
                </FloatLabel>
              </FormField>
            </div>
            <div class="col-span-2">
              <FormField name="observacoes">
                <FloatLabel variant="on">
                  <InputText id="observacoes" maxlength="255" autocomplete="off" fluid/>
                  <label for="observacoes">Observações</label>
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
          :rowsPerPageOptions="[15, 30, 60, 100]" size="small">

          <Column field="id" header="Id" sortable/>
          <Column field="cliente.nome" header="Cliente" sortable/>
          <Column field="vendedor.email" header="Vendedor" sortable v-if="eAdmin()"/>
          <Column field="subTotal" header="Subtotal (R$)" sortable>
            <template #body="slotProps">{{ formatNumber(slotProps.data.subTotal, 'pt-BR', { style: 'decimal', minimumFractionDigits: 2 }) }}</template>
          </Column>
          <Column field="desconto" header="Desconto (%)" sortable>
            <template #body="slotProps">{{ formatNumber(slotProps.data.desconto, 'pt-BR', { style: 'decimal', minimumFractionDigits: 2 }) }}</template>
          </Column>
          <Column field="total" header="Total (R$)" sortable>
            <template #body="slotProps">{{ formatNumber(slotProps.data.total, 'pt-BR', { style: 'decimal', minimumFractionDigits: 2 }) }}</template>
          </Column>

          <Column headerClass="flex justify-center" bodyClass="flex justify-center">
            <template #header>
              <Button icon="pi pi-plus" class="p-button-sm p-button-text p-mr-2" @click="edit(null)" v-tooltip.bottom="'Nova Venda'"/>
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
