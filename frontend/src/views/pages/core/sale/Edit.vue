<script setup>
import api from '@/util/api'
import { eAdmin, getUserId } from '@/util/auth'
import { formatNumber } from '@/util/util'
import { zodResolver } from '@primevue/forms/resolvers/zod'
import { useConfirm } from "primevue/useconfirm"
import { useToast } from 'primevue/usetoast'
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { z } from 'zod'

const router = useRouter()
const route = useRoute()
const toast = useToast()
const loading = ref(false)
const confirm = useConfirm()

const form = ref(null)
const formValues = ref({ idCliente: null, idVendedor: null, idTabela: null, razaoSocial: null, subTotal: null, desconto: null, total: null, observacoes: null })

const formValidator = zodResolver(
  z.object({
    idCliente: z.coerce.number().nullable().refine(val => val !== null && val >= 1, { message: "Preenchimento do Cliente é obrigatório." }),
    idVendedor: z.coerce.number().nullable().refine(val => val !== null && val >= 1, { message: "Preenchimento do Vendedor é obrigatório." }),
    idTabela: z.coerce.number().nullable().refine(val => val !== null && val >= 1, { message: "Tabela de preços é de preenchimento obrigatório." }),
    razaoSocial: z.string().nullable().optional(),
    subTotal: z.number().nullable().optional(),
    desconto: z.number().nullable().optional(),
    total: z.number().nullable().optional(),
    observacoes: z.string().nullable().optional()
  })
)

const id = ref(route.query.id)

async function load(idVenda) {
  loading.value = true

  try {
    const res = await api.get('/sale', { params: { id: idVenda } })

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
    }
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga da Venda', detail: 'Requisição de venda terminou com o erro: ' + error.response.data, life: 10000 })
  } finally {
    loading.value = false
  }
}

const save = async ({ valid, values }) => {
  if (!valid) return

  const filter = itens.value.filter(item => item.editando === false)

  if (filter.length === 0) {
    toast.add({ severity: 'error', summary: 'Itens de Venda necessários', detail: 'Ao menos um item de venda deve ser informado.', life: 10000 })
    return
  }

  const params = {
    id: Number.parseInt(id.value),
    cliente: { id: values.idCliente },
    vendedor: { id: values.idVendedor },
    tabela: { id: values.idTabela },
    subTotal: values.subTotal,
    desconto: values.desconto,
    total: values.total,
    observacoes: values.observacoes
  }

  loading.value = true

  try {
    const response = await api.post('/sale', params)

    if (response.status === 200) {
      id.value = response.data.id

      await saveItens()
      await load(id.value)
      loadItens(id.value)

      toast.add({ severity: 'success', summary: 'Sucesso', detail: 'Venda salva com sucesso', life: 10000 })
    }
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Gravação da Venda', detail: 'Requisição de alteração da venda terminou com o erro: ' + error.response.data, life: 10000 })
  } finally {
    loading.value = false
  }
}

async function saveItens() {
  try {
    await api.post('/sale-item/save-items', itens.value.filter(item => item.editando === false))
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Gravação dos Itens de Venda', detail: 'Requisição de alteração dos itens de venda terminou com o erro: ' + error.response.data, life: 10000 })
  }
}

onMounted(async () => {
  loading.value = true

  if (id.value) {
    await load(id.value)
    loadItens(id.value)

    const fields = form.value.states
    loadTables(fields.idVendedor.value)
    loadClient(fields.idCliente.value)
    loadProducts(fields.idTabela.value)
  } else {
    loadTables(getUserId())
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

async function loadItens(idVenda) {
  const query = {
    idVenda: idVenda,
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
    tabelaPrecoProduto: { id: null, produto: { nome: null, referencia: null }, quantidade: null, precoUnitario: null, total: null },
    quantidade: null,
    precoUnitario: null,
    total: null,
    edicao: { id: null, referencia: null, nome: null, quantidade: null, precoUnitario: null, total: null },
    editando: true,
    temProduto: false
  })
}

const tables = ref([])

async function loadTables(idVendedor) {
  try {
    const response = await api.get("/user-price-table/list", { params: { idVendedor: idVendedor, page: 0, size: 10000, sort: 'tabela.nome,asc' } })

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

    form.value.setFieldValue('razaoSocial', res.data.razaoSocial)
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Cliente', detail: 'Requisição de cliente terminou com o erro: ' + error.response.data, life: 10000 })
  } finally {
    loading.value = false
  }
}

function edit(item) {
  if (item.id) {
    item.edicao = {
      id: item.tabelaPrecoProduto.id,
      referencia: item.tabelaPrecoProduto.produto.nome,
      nome: item.tabelaPrecoProduto.produto.nome,
      quantidade: item.quantidade,
      precoUnitario: item.precoUnitario,
      total: item.total
    }

    item.temProduto = true
  } else if (!item.edicao) {
    item.edicao = { id: null, nome: null, referencia: null, quantidade: null, precoUnitario: null, total: null }
  }

  item.editando = true
  evaluateItem(item)
}

function cancel(item) {
  item.editando = false

  if (!item.id && (!item.edicao.quantidade || Number.parseInt(item.edicao.quantidade) <= 0)) {
    itens.value.splice(itens.value.indexOf(item), 1)
  }

  evaluateTotal()
}

function setProduct(idTabelaPrecoProduto, item) {
  const product = products.value.find(product => idTabelaPrecoProduto === product.id)

  item.edicao.id = product.id
  item.edicao.referencia = product.produto.referencia
  item.edicao.nome = product.produto.nome
  item.edicao.precoUnitario = product.preco
  item.temProduto = true

  evaluateItem(item)
}

function evaluateItem(item) {
  item.edicao.total = item.edicao?.quantidade * item.edicao.precoUnitario

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

  form.value.setValues({ subTotal: Number.parseFloat(subTotal.toFixed(2)), total: Number.parseFloat(total.toFixed(2)) })
}

function commit(item) {
  if (!item.temProduto) {
    toast.add({ severity: 'error', summary: 'Dados Insuficientes', detail: 'Item de venda está sem produto associado.', life: 10000 })
    return
  }

  if (!item.edicao.quantidade || Number.parseInt(item.edicao.quantidade) <= 0) {
    toast.add({ severity: 'error', summary: 'Dados Insuficientes', detail: 'Quantidade de itens deve ter valor maior que zero.', life: 10000 })
    return
  }

  const product = products.value.find(product => item.edicao.id === product.id)

  item.tabelaPrecoProduto = product
  item.precoUnitario = item.edicao.precoUnitario
  item.quantidade = item.edicao.quantidade
  item.total = item.edicao.quantidade * item.edicao.precoUnitario

  item.editando = false

  evaluateTotal()
}

const confirmDelete = item => {
  confirm.require({
    message: 'Deseja remover o item venda?',
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
      deleteItemSale(item)
    }
  })
}

async function deleteItemSale(item) {
  if (item.id) {
    try {
      await api.delete(`/sale-item?id=${item.id}`)
      
      toast.add({ severity: 'success', summary: 'Sucesso', detail: 'Item de Venda removido com sucesso', life: 10000 })
      
      load(item.venda.id)
    } catch (error) {
      toast.add({ severity: 'error', summary: 'Falha de Remoção do Item de Venda', detail: 'Requisição de remoção de item de venda terminou com o erro: ' + error.response.data, life: 10000 })
    }
  }

  itens.value.splice(itens.value.indexOf(item), 1)
}
function changeDiscount(event) {
  const subTotal = form.value?.states?.subTotal?.value
  const value = typeof event.value === 'string' ? event.value.replace(',', '.') : event.value
  const desconto = event.value ? Number.parseFloat(value) : 0
  const total = value ? subTotal - (subTotal * Math.min(desconto, 99.99) / 100) : subTotal

  form.value.setFieldValue('total', Number.parseFloat(total.toFixed(2)))
}

async function changeSalesman(idVendedor) {
  await loadTables(idVendedor)

  if (tables.value.length === 1) {
    form.value.setFieldValue('idTabela', tables.value[0].tabela.id)
    loadProducts(tables.value[0].tabela.id)
  } else {
    form.value.setFieldValue('idTabela', null)
    products.value = []
  }
}

async function changePriceTable(idTabela) {
  const oldValue = form.value.states.idTabela.value

  confirm.require({
    message: 'Alterar a tabela de preços pode remover itens da venda cujos produtos não existam ou não tenham preço definido para a nova tabela escolhida. Deseja continuar?',
    header: "Alerta",
    icon: 'pi pi-info-circle',
    rejectProps: {
      label: 'Cancelar',
      severity: 'secondary',
      raised: true
    },
    acceptProps: {
      label: 'Prosseguir',
      severity: 'danger',
      raised: true
    },
    accept: async () => {
      await loadProducts(idTabela)

      for (let item of itens.value) {
        if (item.tabelaPrecoProduto?.produto?.id) {
          const tabelaPrecoProduto = products.value.find(product => product.produto.id === item.tabelaPrecoProduto?.produto?.id)

          if (tabelaPrecoProduto) {
            item.tabelaPrecoProduto = tabelaPrecoProduto
            item.precoUnitario = tabelaPrecoProduto.preco
            item.total = item.quantidade * tabelaPrecoProduto.preco
          } else {
            deleteItemSale(item)
          }
        } else {
          deleteItemSale(item)
        }
      }

      evaluateTotal()
    },
    reject: () => form.value.setFieldValue('idTabela', oldValue)
  })
}
</script>

<template>
  <ConfirmDialog :closable="false"></ConfirmDialog>
  <BlockUI :blocked="loading" fullScreen>
    <Card class="mb-4">
      <template #title>
        <div class="grid grid-cols-2">
          <h3>Venda</h3>
          <div class="flex justify-end items-center">
            <Button icon="pi pi-replay" @click="router.push('/core/sale')" class="p-button-text" v-tooltip.bottom="'Voltar'"/>
          </div>
        </div>
      </template>

      <template #content>
        <Form ref="form" :resolver="formValidator" :initialValues="formValues" @submit="save" class="grid flex flex-column gap-4">
          <div class="grid grid-cols-12 gap-4">
            <div :class="'col-span-' + (pj ? 5 : 12)">
              <FormField v-slot="$field" name="idCliente">
                <FloatLabel variant="on">
                  <Select id="idCliente" :options="clients" optionLabel="nome" optionValue="id" filter fluid @update:modelValue="loadClient($event)"/>
                  <label for="idCliente">Cliente</label>
                </FloatLabel>
                <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
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
              <FormField v-slot="$field" name="idVendedor">
                <FloatLabel variant="on">
                  <Select id="idVendedor" :options="users" optionLabel="email" optionValue="id" fluid @update:modelValue="changeSalesman($event)"/>
                  <label for="idVendedor">Vendedor</label>
                </FloatLabel>
                <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
              </FormField>
            </div>
            <div class="col-span-6">
              <FormField v-slot="$field" name="idTabela">
                <FloatLabel variant="on">
                  <Select id="idTabela" :options="tables" optionLabel="tabela.nome" optionValue="tabela.id" fluid @value-change="changePriceTable($event)"/>
                  <label for="idTabela">Tabela de Preços</label>
                </FloatLabel>
                <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
              </FormField>
            </div>
          </div>
          <div class="grid grid-cols-12 gap-4">
            <div class="col-span-4">
              <FormField v-slot="$field" name="subTotal">
                <FloatLabel variant="on">
                  <InputNumber id="subTotal" :minFractionDigits="2" :maxFractionDigits="2" fluid readonly/>
                  <label for="subTotal">Subtotal (R$)</label>
                </FloatLabel>
                <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
              </FormField>
            </div>
            <div class="col-span-4">
              <FormField v-slot="$field" name="desconto">
                <FloatLabel variant="on">
                  <InputNumber id="desconto" :max="99.99" :minFractionDigits="2" :maxFractionDigits="2" fluid @input="changeDiscount"/>
                  <label for="desconto">Desconto (%)</label>
                </FloatLabel>
                <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
              </FormField>
            </div>
            <div class="col-span-4">
              <FormField v-slot="$field" name="total">
                <FloatLabel variant="on">
                  <InputNumber id="total" :minFractionDigits="2" :maxFractionDigits="2" fluid readonly/>
                  <label for="total">Total (R$)</label>
                </FloatLabel>
                <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
              </FormField>
            </div>
          </div>
          <FormField v-slot="$field" name="observacoes">
            <FloatLabel variant="on" class="flex-1">
              <Textarea id="observacoes" rows="3" size="1024" style="resize: none" fluid/>
              <label for="observacoes">Observações</label>
            </FloatLabel>
            <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
          </FormField>
          <div class="flex justify-end gap-4 mt-2">
            <Button label="Limpar" icon="pi pi-times" type="reset" severity="secondary" raised/>
            <Button label="Salvar" icon="pi pi-save" type="submit" raised/>
          </div>
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
              <div v-show="slotProps.data.editando">{{slotProps.data.edicao.nome}}</div>
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
              <Button icon="pi pi-check" class="p-button-sm p-button-text p-mr-2" @click="commit(slotProps.data)" v-tooltip.bottom="'Consolidar'" v-show="slotProps.data.editando"/>
              <Button icon="pi pi-times" class="p-button-sm p-button-text p-mr-2" @click="cancel(slotProps.data)" v-tooltip.bottom="'Cancelar'" v-show="slotProps.data.editando"/>
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>
  </BlockUI>
</template>
