<script setup>
import api from '@/util/api'
import { zodResolver } from '@primevue/forms/resolvers/zod'
import { useToast } from 'primevue/usetoast'
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { z } from 'zod'

const router = useRouter()
const route = useRoute()
const toast = useToast()
const loading = ref(false)

const form = ref(null)
const formValues = ref({ idVendedor: null, subTotal: null, desconto: null, total: null, observacoes: null })

const formValidator = zodResolver(
  z.object({
    idVendedor: z.number().refine(val => !id || (val && val > 0), { message: "Preenchimento do Vendedor é obrigatório." }),
    desconto: z.number().optional(),
    observacoes: z.string().optional()
  })
)

const id = ref(route.query.id)

async function load() {
  loading.value = true

  try {
    const res = await api.get('/sale', { params: { id: id.value } })

    if (form.value) {
      form.value.setValues({
        idVendedor: res.data.vendedor.id,
        subTotal: res.data.subTotal,
        desconto: res.data.desconto,
        total: res.data.total,
        observacoes: res.data.observacoes
      })
    }
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga da Venda', detail: 'Requisição de venda terminou com o erro: ' + error.response.data, life: 10000 })
  } finally {
    loading.value = false
  }
}

const save = async ({ valid, values }) => {
  if (!valid) return

  let params = { ... values }

  params['id'] = parseInt(id.value)

  loading.value = true

  try {
    const response = await api.post('/sale', params)

    if (response.status === 200) {
      id.value = response.data.id

      toast.add({ severity: 'success', summary: 'Sucesso', detail: 'Venda efetuada com sucesso', life: 10000 })
    }
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Gravação da Venda', detail: 'Requisição de alteração da venda terminou com o erro: ' + error.response.data, life: 10000 })
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loading.value = true

  if (id.value) {
    load()
    loadItens()
  }

  loadUsers()

  loading.value = false
})


let users = ref([])

async function loadUsers() {
  try {
    const response = await api.get('/user/list', { params: { page: 0, size: 10000, sort: 'email,asc' } })

    users.value = response.data.content
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Vendedores', detail: 'Requisição de lista de Vendedores terminou com o erro: ' + error.response.data, life: 10000 })
  }
}

const data = ref([])

async function loadItens() {
  const query = {
    idVenda: id.value,
    page: 0,
    size: 10000,
  }

  try {
    const response = await api.get("/sale-item/list", { params: query })

    data.value = response.data.content
  } catch (error) {
    toast.add({ severity: "error", summary: "Falha de Carga de Itens de Venda", detail: "Requisição de lista de itens de venda terminou com o erro: " + error.response.data, life: 10000 })
  }
}
</script>

<template>
  <ConfirmDialog :closable="false"></ConfirmDialog>
  <BlockUI :blocked="loading" fullScreen>
    <Card class="mb-4">
      <template #title>
        <div class="grid grid-cols-2">
          <h3>Itens da Venda</h3>
          <div class="flex justify-end items-center">
            <Button icon="pi pi-replay" @click="router.push('/core/sale')" class="p-button-text" v-tooltip.bottom="'Voltar'"/>
          </div>
        </div>
      </template>

      <template #content>
        <DataTable :value="data" :lazy="true" responsiveLayout="scroll" stripedRows size="small">
          <Column field="id" header="Id"/>
          <Column field="tabelaPrecoProduto.produto.referencia" header="Referência"/>
          <Column field="tabelaPrecoProduto.produto.nome" header="Nome"/>
          <Column field="quantidade" header="Quantidade"/>
          <Column field="precoUnitario" header="Preço Unitário"/>
          <Column headerClass="flex justify-center" bodyClass="flex justify-center">
            <template #header>
              <Button icon="pi pi-plus" class="p-button-sm p-button-text p-mr-2" @click="edit(null)" v-tooltip.bottom="'Nova Tabela de Preços'"/>
            </template>
            <template #body="slotProps">
              <Button icon="pi pi-pencil" class="p-button-sm p-button-text p-mr-2" @click="edit(slotProps.data)" v-tooltip.bottom="'Editar'"/>
              <Button icon="pi pi-trash" class="p-button-sm p-button-text p-button-danger" @click="confirmDelete(slotProps.data)" v-tooltip.bottom="'Remover'"/>
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>
    <Card class="mb-4">
      <template #title>
        <div class="grid grid-cols-2">
          <h3>Sumário da Venda</h3>
          <div class="flex justify-end items-center" v-show="id">
            <Button icon="pi pi-replay" @click="router.push('/core/sale')" class="p-button-text" v-tooltip.bottom="'Voltar'"/>
          </div>
        </div>
      </template>

      <template #content>
        <Form ref="form" :resolver="formValidator" :initialValues="formValues" @submit="save" class="grid flex flex-column gap-4">
          <div class="grid grid-cols-12 gap-4">
            <div :class="'col-span-' + (id ? '3': '4')">
              <FormField name="subTotal">
                <FloatLabel variant="on">
                  <InputNumber id="subTotal" :max="100" :minFractionDigits="2" :maxFractionDigits="2" fluid/>
                  <label for="subTotal">Sub-Total</label>
                </FloatLabel>
              </FormField>
            </div>
            <div :class="'col-span-' + (id ? '3': '4')">
              <FormField name="desconto">
                <FloatLabel variant="on">
                  <InputNumber id="desconto" :max="100" :minFractionDigits="2" :maxFractionDigits="2" fluid/>
                  <label for="desconto">Desconto (%)</label>
                </FloatLabel>
              </FormField>
            </div>
            <div :class="'col-span-' + (id ? '3': '4')">
              <FormField name="total">
                <FloatLabel variant="on">
                  <InputNumber id="total" :max="100" :minFractionDigits="2" :maxFractionDigits="2" fluid/>
                  <label for="total">Total</label>
                </FloatLabel>
              </FormField>
            </div>
            <div :class="'col-span-3'" v-show="id">
              <FormField name="idVendedor">
                <FloatLabel variant="on">
                  <Select :options="users" optionLabel="email" optionValue="id" fluid/>
                  <label for="idVendedor">Vendedor</label>
                </FloatLabel>
              </FormField>
            </div>
          </div>
          <FormField name="observacoes">
            <FloatLabel variant="on" class="flex-1">
              <Textarea id="observacoes" rows="3" size="1024" style="resize: none" fluid/>
              <label for="email">Observações</label>
            </FloatLabel>
          </FormField>
        </Form>
      </template>
    </Card>
    <div class="flex justify-end gap-4 mt-4">
      <Button label="Limpar" icon="pi pi-times" @click="cleanPrices" severity="secondary" raised/>
      <Button label="Salvar" icon="pi pi-save" @click="savePrices" raised/>
    </div>
  </BlockUI>
</template>
