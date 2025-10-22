<script setup>
import api from '@/util/api'
import { eAdmin, getUserId } from '@/util/auth'
import { formatNumber } from '@/util/util'
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
const formValues = ref({ idCliente: null, idVendedor: null, idTabela: null, razaoSocial: null, subTotal: null, desconto: null, total: null, observacoes: null })

const formValidator = zodResolver(
  z.object({
    idCliente: z.number().refine(val => !id || (val && val > 0), { message: "Preenchimento do Cliente é obrigatório." }),
    idVendedor: z.number().refine(val => !id || (val && val > 0), { message: "Preenchimento do Vendedor é obrigatório." }),
    idTabela: z.int().min(1, "Tabela de preços é de preenchimento obrigatório."),
    razaoSocial: z.string().optional(),
    subTotal: z.number().optional(),
    desconto: z.number().optional(),
    total: z.number().optional(),
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
        idCliente: res.data.cliente.id,
        idVendedor: res.data.vendedor.id,
        idTabela: res.data.tabela.id,
        subTotal: res.data.subTotal,
        desconto: res.data.desconto,
        total: res.data.total,
        observacoes: res.data.observacoes
      })

      loadClient(res.data.cliente.id)
      loadProducts(res.data.tabela.id)
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

  params['id'] = Number.parseInt(id.value)

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
    loadTables()
  }

  loadUsers()
  loadClients()

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

const itens = ref([])

async function loadItens() {
  const query = {
    idVenda: id.value,
    page: 0,
    size: 10000,
  }

  try {
    const response = await api.get("/sale-item/list", { params: query })

    itens.value = response.data.content
    for (let item of itens.value) {
      item.edicao = { ...item.tabelaPrecoProduto }
      item.editando = false
      item.temProduto = true
    }

    evaluateTotal()
  } catch (error) {
    toast.add({ severity: "error", summary: "Falha de Carga de Itens de Venda", detail: "Requisição de lista de itens de venda terminou com o erro: " + error.response.data, life: 10000 })
  }
}

function addItem() {
  itens.value.push({
    id: null,
    venda: { id: id.value },
    tabelaPrecoProduto: { id: null, produto: { nome: null }, quantidade: null, precoUnitario: null, total: null },
    quantidade: null,
    precoUnitario: null,
    total: null,
    edicao: { id: null, produto: { nome: null }, quantidade: null, precoUnitario: null, total: null },
    editando: true,
    temProduto: false
  })
}

const tables = ref([])

async function loadTables() {
  try {
    const response = await api.get("/user-price-table/list", { params: { idVendedor: getUserId(), page: 0, size: 10000, sort: 'tabela.nome,asc' } })

    tables.value = response.data.content
  } catch (error) {
    toast.add({ severity: "error", summary: "Falha de Carga de Tabelas de Preço do Vendedor", detail: "Requisição de lista de Tabelas de Preço do Vendedor terminou com o erro: " + error.response.data, life: 10000 })
  }
}

const products = ref([])

async function loadProducts(idTabela) {
  try {
    const response = await api.get("/price-table-product/list", { params: { idTabelaPreco: idTabela, page: 0, size: 10000, sort: 'produto.nome,asc' } })

    products.value = response.data.content
  } catch (error) {
    toast.add({ severity: "error", summary: "Falha de Carga de Produtos para o Vendedor", detail: "Requisição de lista de Produtos para o Vendedor terminou com o erro: " + error.response.data, life: 10000 })
  }
}

const clients = ref([])

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

const pj = ref(false)

const loadClient = async (value) => {
  loading.value = true

  try {
    const res = await api.get('/client', { params: { id: value } })

    pj.value = res.data.cnpj?.length > 0

    form.value.setValues({
      razaoSocial: res.data.razaoSocial
    })
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Cliente', detail: 'Requisição de cliente terminou com o erro: ' + error.response.data, life: 10000 })
  } finally {
    loading.value = false
  }
}

function edit(item) {
  item.edicao = { ...item.tabelaPrecoProduto }
  item.edicao.nome = item.tabelaPrecoProduto.produto.nome
  item.edicao.quantidade = item.quantidade
  item.edicao.precoUnitario = item.precoUnitario
  item.edicao.total = item.total
  item.editando = true
  item.temProduto = true
}

function cancel(item) {
  item.editando = false

  if (!item.id) {
    const index = itens.value.indexOf(item)

    if (index !== -1) {
      itens.value.splice(index, 1)
    }
  }

  evaluateTotal()
}

function setProduct(idTabelaPrecoProduto, item) {
  let product = null

  for (let i = 0; i < products.value.length && product === null; i++) {
    product = idTabelaPrecoProduto === products.value[i].id ? products.value[i] : null
  }

  item.edicao.produto.nome = product.produto.nome
  item.edicao.precoUnitario = product.preco
  item.temProduto = true

  evaluateItem(item)
}

function evaluateItem(item) {
  item.edicao.total = item.edicao.quantidade ? item.edicao.quantidade * item.edicao.precoUnitario : null

  evaluateTotal()
}

function setAmount(evento, item) {
  item.edicao.total = Number.parseInt(evento.value) * item.edicao.precoUnitario
  evaluateTotal()
}

function evaluateTotal() {
  let subTotal = 0

  for (const item of itens.value) {
    if (item.editando) {
      subTotal += item.edicao.total ? item.edicao.total : 0
    } else {
      subTotal += item.total ? item.total : 0
    }
  }

  const total = form.value?.states?.desconto?.value ? (subTotal - (subTotal * form.value.states.desconto.value / 100)) : subTotal

  form.value.setValues({ subTotal: subTotal, total: total })
}
</script>

<template>
  <ConfirmDialog :closable="false"></ConfirmDialog>
  <BlockUI :blocked="loading" fullScreen>
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
            <div :class="'col-span-' + (pj ? 5 : 12)">
              <FormField name="idCliente">
                <FloatLabel variant="on">
                  <Select :options="clients" optionLabel="nome" optionValue="id" filter fluid @update:modelValue="loadClient($event)"/>
                  <label for="idCliente">Cliente</label>
                </FloatLabel>
              </FormField>
            </div>
            <div class="col-span-7" v-show="pj">
              <FormField name="razaoSocial">
                <FloatLabel variant="on">
                  <InputText id="razaoSocial" maxlength="255" autocomplete="off" fluid readonly/>
                  <label for="razaoSocial">Razão Social</label>
                </FloatLabel>
              </FormField>
            </div>
          </div>
          <div class="grid grid-cols-12 gap-4" v-show="eAdmin()">
            <div class="col-span-6">
              <FormField name="idTabela">
                <FloatLabel variant="on">
                  <Select :options="tables" optionLabel="tabela.nome" optionValue="tabela.id" fluid/>
                  <label for="idTabela">Tabela de Preços</label>
                </FloatLabel>
              </FormField>
            </div>
            <div class="col-span-6">
              <FormField name="idVendedor">
                <FloatLabel variant="on">
                  <Select :options="users" optionLabel="email" optionValue="id" fluid/>
                  <label for="idVendedor">Vendedor</label>
                </FloatLabel>
              </FormField>
            </div>
          </div>
          <div class="grid grid-cols-12 gap-4">
            <div class="col-span-4">
              <FormField name="subTotal">
                <FloatLabel variant="on">
                  <InputNumber id="subTotal" :max="100" :minFractionDigits="2" :maxFractionDigits="2" fluid @input="evaluateTotal"/>
                  <label for="subTotal">Subtotal (R$)</label>
                </FloatLabel>
              </FormField>
            </div>
            <div class="col-span-4">
              <FormField name="desconto">
                <FloatLabel variant="on">
                  <InputNumber id="desconto" :max="100" :minFractionDigits="2" :maxFractionDigits="2" fluid/>
                  <label for="desconto">Desconto (%)</label>
                </FloatLabel>
              </FormField>
            </div>
            <div class="col-span-4">
              <FormField name="total">
                <FloatLabel variant="on">
                  <InputNumber id="total" :max="100" :minFractionDigits="2" :maxFractionDigits="2" fluid/>
                  <label for="total">Total (R$)</label>
                </FloatLabel>
              </FormField>
            </div>
          </div>
          <FormField name="observacoes">
            <FloatLabel variant="on" class="flex-1">
              <Textarea id="observacoes" rows="3" size="1024" style="resize: none" fluid/>
              <label for="observacoes">Observações</label>
            </FloatLabel>
          </FormField>
        </Form>
      </template>
    </Card>
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
        <DataTable :value="itens" :lazy="true" responsiveLayout="scroll" stripedRows size="small">
          <Column field="id" header="Id"/>
          <Column header="Referência">
            <template #body="slotProps">
              <div v-show="!slotProps.data.editando">{{ slotProps.data.tabelaPrecoProduto.produto.referencia }}</div>
              <div v-show="slotProps.data.editando">
                <Select v-model="slotProps.data.edicao.id" :options="products" optionLabel="produto.referencia" optionValue="id" filter fluid @update:modelValue="setProduct($event, slotProps.data)"/>
              </div>
            </template>
          </Column>
          <Column field="tabelaPrecoProduto.produto.nome" header="Nome">
            <template #body="slotProps">
              <div v-show="!slotProps.data.editando">{{slotProps.data.tabelaPrecoProduto.produto.nome}}</div>
              <div v-show="slotProps.data.editando">{{slotProps.data.edicao.produto.nome}}</div>
            </template>
          </Column>
          <Column field="quantidade" header="Quantidade">
            <template #body="slotProps">
              <div v-show="!slotProps.data.editando">{{slotProps.data.quantidade}}</div>
              <div v-show="slotProps.data.editando && slotProps.data.temProduto">
                <InputNumber v-model="slotProps.data.edicao.quantidade" :max="10000" :maxFractionDigits="0" @input="setAmount($event, slotProps.data)" @blur="evaluateItem(slotProps.data)"/>
              </div>
            </template>
          </Column>
          <Column field="precoUnitario" header="Preço Unitário (R$)">
            <template #body="slotProps">
              <div v-show="!slotProps.data.editando">{{ formatNumber(slotProps.data.precoUnitario, 'pt-BR', { style: 'decimal', minimumFractionDigits: 2 }) }}</div>
              <div v-show="slotProps.data.editando && slotProps.data.temProduto">{{ formatNumber(slotProps.data.edicao.precoUnitario, 'pt-BR', { style: 'decimal', minimumFractionDigits: 2 }) }}</div>
            </template>
          </Column>
          <Column field="total" header="Total (R$)">
            <template #body="slotProps">
              <div v-show="!slotProps.data.editando">{{ formatNumber(slotProps.data.total, 'pt-BR', { style: 'decimal', minimumFractionDigits: 2 }) }}</div>
              <div v-show="slotProps.data.editando && slotProps.data.temProduto">{{ formatNumber(slotProps.data.edicao.total, 'pt-BR', { style: 'decimal', minimumFractionDigits: 2 }) }}</div>
            </template>
          </Column>
          <Column headerClass="flex justify-center" bodyClass="flex justify-center">
            <template #header>
              <Button icon="pi pi-plus" class="p-button-sm p-button-text p-mr-2" @click="addItem" v-tooltip.bottom="'Nova Tabela de Preços'"/>
            </template>
            <template #body="slotProps">
              <Button icon="pi pi-pencil" class="p-button-sm p-button-text p-mr-2" @click="edit(slotProps.data)" v-tooltip.bottom="'Editar'" v-show="!slotProps.data.editando"/>
              <Button icon="pi pi-trash" class="p-button-sm p-button-text p-button-danger" @click="confirmDelete(slotProps.data)" v-tooltip.bottom="'Remover'" v-show="!slotProps.data.editando"/>
              <Button icon="pi pi-check" class="p-button-sm p-button-text p-mr-2" @click="edit(slotProps.data)" v-tooltip.bottom="'Verificar'" v-show="slotProps.data.editando"/>
              <Button icon="pi pi-times" class="p-button-sm p-button-text p-mr-2" @click="cancel(slotProps.data)" v-tooltip.bottom="'Cancelar'" v-show="slotProps.data.editando"/>
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>
    <div class="flex justify-end gap-4 mt-4">
      <Button label="Limpar" icon="pi pi-times" severity="secondary" raised/>
      <Button label="Salvar" icon="pi pi-save" raised/>
    </div>
  </BlockUI>
</template>
