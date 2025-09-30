<script setup>
import api from '@/util/api'
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
      query.sort += sortOrder.value === 1 ? ',asc' : ',desc'
    }
  }

  loading.value = true

  try {
    const response = await api.get('/product/list', { params: query })

    data.value = response.data.content
    totalRecords.value = response.data.totalElements
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Produtos', detail: 'Requisição de lista de Produtos terminou com o erro: ' + error.response.data, life: 10000 })
  } finally {
    loading.value = false
  }
}


onMounted(async () => {
  load({})
  loadProductTypes()
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
    router.push(`/register/product/edit?id=${entity.id}`)
  } else {
    router.push('/register/product/edit')
  }
}

const confirmDelete = entity => {
  confirm.require({
    message: 'Deseja remover o produto?',
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
        await api.delete(`/product?id=${entity.id}`)

        toast.add({ severity: 'success', summary: 'Sucesso', detail: 'Produto removida com sucesso', life: 10000 })

        load()
      } catch (error) {
        toast.add({ severity: 'error', summary: 'Falha de Remoção de Produto', detail: 'Requisição de remoção de produto terminou com o erro: ' + error.response.data, life: 10000 })
      }
    }
  })
}

let tipos = ref([])

async function loadProductTypes() {
  loading.value = true

  try {
    const response = await api.get('/product-type/list', { params: { page: 0, size: 10000, sort: 'nome,asc' } })

    tipos.value = response.data.content
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Tipos de Produto', detail: 'Requisição de lista de Tipos de Produto terminou com o erro: ' + error.response.data, life: 10000 })
  } finally {
    loading.value = false
  }
}

const resetValues = { nome: null, idTipoProduto: null, referencia: null, peso: null, ativo: null }
const productForm = ref(null)
const productFormValues = ref({ ... resetValues })
const filterValues = ref({ ... resetValues })
const validade = [ { value: true, label: 'Sim' }, { value: false, label: 'Não' } ]

const filter = async ({ valid, values }) => {
  if (!valid) return

  filterValues.value = { ...values }
  page.value = 0

  load( { ...filterValues.value } )
}

const formKey = ref(0)

function limpar() {
  nextTick(() => {
    page.value = 0
    filterValues.value = { ...resetValues }
    formKey.value++
    load( { ...filterValues.value } )
  })
}

</script>

<template>
  <ConfirmDialog :closable="false"></ConfirmDialog>
  <BlockUI :blocked="loading" fullScreen>
    <Card>
      <template #title><h3>Lista de Produtos</h3></template>
      <template #content>
        <Form ref="productForm" :key="formKey" :initialValues="productFormValues" @submit="filter" @reset="limpar" class="grid flex flex-column gap-4 mb-4">
          <div class="grid grid-cols-12 gap-4">
            <div class="col-span-9">
              <FormField name="nome">
                <FloatLabel variant="on">
                  <InputText id="nome" maxlength="255" autocomplete="off" fluid/>
                  <label for="nome">Nome</label>
                </FloatLabel>
              </FormField>
            </div>
            <div class="col-span-3">
              <FormField name="idTipoProduto">
                <FloatLabel variant="on">
                  <Select :options="tipos" optionLabel="nome" optionValue="id" fluid/>
                  <label for="idTipoProduto">Tipo do Produto</label>
                </FloatLabel>
              </FormField>
            </div>
          </div>

          <div class="grid grid-cols-12 gap-4">
            <div class="col-span-4">
              <FormField name="referencia">
                <FloatLabel variant="on">
                  <InputText id="referencia" maxlength="100" autocomplete="off" fluid/>
                  <label for="referencia">Código de Referência</label>
                </FloatLabel>
              </FormField>
            </div>

            <div class="col-span-4">
              <FormField name="peso">
                <FloatLabel variant="on">
                  <InputNumber name="peso" :max="10000" fluid/>
                  <label for="peso">Peso (em gramas)</label>
                </FloatLabel>
              </FormField>
            </div>

            <div class="col-span-4">
              <FormField name="ativo">
                <FloatLabel variant="on">
                  <Select :options="validade" optionLabel="label" optionValue="value" fluid/>
                  <label for="ativo">Ativo</label>
                </FloatLabel>
              </FormField>
            </div>
          </div>

          <FormField class="flex justify-end gap-4">
            <Button label="Limpar" icon="pi pi-times" type="reset" severity="secondary" raised/>
            <Button label="Buscar" icon="pi pi-search" type="submit" raised/>
          </FormField>

          <DataTable :value="data" :lazy="true" :paginator="true" :rows="size" :totalRecords="totalRecords"
            :first="page * size" @page="onPage" @sort="onSort" :sortField="sortField" :sortOrder="sortOrder" responsiveLayout="scroll" stripedRows
            :rowsPerPageOptions="[10, 20, 50, 100]">

            <Column field="id" header="Id" sortable/>
            <Column field="nome" header="Nome" sortable/>
            <Column field="referencia" header="Referência" sortable/>
            <Column field="tipoProduto.nome" header="Tipo de Produto" sortable/>
            <Column field="peso" header="Peso (em gramas)" sortable/>
            <Column field="ativo" header="Ativo" sortable>
              <template #body="slotProps">
                {{ slotProps.data.ativo ? 'Sim' : 'Não' }}
              </template>
            </Column>

            <Column :bodyStyle="{ textAlign: 'center' }">
              <template #header>
                <div style="width: 100%; display: flex; justify-content: center;">
                  <Button icon="pi pi-plus" class="p-button-sm p-button-text p-mr-2" @click="edit(null)" v-tooltip.bottom="'Novo Tipo de Produto'"/>
                </div>
              </template>

              <template #body="slotProps">
                <Button icon="pi pi-pencil" class="p-button-sm p-button-text p-mr-2" @click="edit(slotProps.data)" v-tooltip.bottom="'Editar'"/>
                <Button icon="pi pi-trash" class="p-button-sm p-button-text p-button-danger" @click="confirmDelete(slotProps.data)" v-tooltip.bottom="'Remover'"/>
              </template>
            </Column>
          </DataTable>
        </Form>
      </template>
    </Card>
  </BlockUI>
</template>
