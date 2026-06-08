<script setup>
import api from '@/util/api'
import { eAdmin, getUserId } from '@/util/auth'
import { formatNumber } from '@/util/util'
import { zodResolver } from '@primevue/forms/resolvers/zod'
import { useToast } from 'primevue/usetoast'
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { z } from 'zod'

const props = defineProps({
  id: { type: [Number, String], default: null },
  backEndpoint: { type: [String], default: null }
})

const router = useRouter()
const toast = useToast()
const loading = ref(false)

const form = ref(null)
const formValues = ref({ idCliente: null, idVendedor: null, idTabela: null, idPontoVenda: null, razaoSocial: null, subTotal: null, desconto: null, total: null, observacoes: null })

const formValidator = zodResolver(
  z.object({
    idVendedor: z.coerce.number().nullable().refine(val => val !== null && val >= 1, { message: "Preenchimento do Vendedor é obrigatório." }),
    idTabela: z.coerce.number().nullable().refine(val => val !== null && val >= 1, { message: "Tabela de preços é de preenchimento obrigatório." }),
    idPontoVenda: z.coerce.number().nullable().refine(val => val !== null && val >= 1, { message: "Ponto de Venda é de preenchimento obrigatório." }),
    razaoSocial: z.string().nullable().optional(),
    subTotal: z.number().nullable().optional(),
    desconto: z.number().nullable().optional(),
    total: z.number().nullable().optional(),
    observacoes: z.string().nullable().optional()
  })
)

const id = ref(props.id)

async function load(idVenda) {
  try {
    const response = await api.get('/sale', { params: { id: idVenda } })

    if (form.value) {
      form.value.setValues({
        idCliente: response.data.cliente?.id,
        idVendedor: response.data.vendedor.id,
        idTabela: response.data.tabela.id,
        idPontoVenda: response.data.pontoVenda.id,
        subTotal: response.data.subTotal,
        desconto: response.data.desconto,
        total: response.data.total,
        observacoes: response.data.observacoes
      })
    }
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga da Venda', detail: 'Requisição de venda terminou com o erro: ' + error.response.data, life: 10000 })
  }
}

const submitAction = ref('')

const save = async ({ valid, values }) => {
  if (!valid) return

  const quantidades = itens.value.filter(item => item.quantidade && item.quantidade > 0)

  if (quantidades.length === 0) {
    toast.add({ severity: 'error', summary: 'Itens de Venda necessários', detail: 'Venda sem itens.', life: 10000 })
    return
  }

  const estoques = itens.value.filter(item => item.quantidade && (item.quantidade - item.quantidadeOriginal > item.tabelaPrecoProduto.produto.estoque))

  if (estoques.length > 0) {
    toast.add({ severity: 'error', summary: 'Itens de Venda fora de estoque', detail: 'Há itens com quantidade maior que o estoque de seu produto.', life: 10000 })
    return
  }

  const params = {
    id: Number.parseInt(id.value),
    vendedor: { id: values.idVendedor },
    tabela: { id: values.idTabela },
    pontoVenda: { id: values.idPontoVenda },
    subTotal: values.subTotal,
    desconto: values.desconto,
    total: values.total,
    observacoes: values.observacoes
  }

  if (values.idCliente) {
    params['cliente'] =  { id: values.idCliente }
  }

  loading.value = true

  try {
    const response = await api.post('/sale', params)

    if (response.status === 200) {
      id.value = response.data.id

      itens.value.forEach(item => {
        if (item.quantidade && item.quantidade > 0) {
          item.venda = { id: response.data.id, pontoVenda: { id: values.idPontoVenda } }
        }
      })

      await api.post('/sale-item/save-items', itens.value.filter(item => item.quantidade && item.quantidade > 0))

      if (submitAction.value === 'saveNew') {
        id.value = null
        clear()
      } else {
        await load(id.value)
        loadItens('list-by-sale', { idVenda: id.value })
      }

      toast.add({ severity: 'success', summary: 'Sucesso', detail: 'Venda salva com sucesso', life: 10000 })
    }
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Gravação da Venda', detail: 'Requisição de alteração da venda terminou com o erro: ' + error.response.data, life: 10000 })
  } finally {
    loading.value = false
  }
}

async function loadTableProducts() {
  if (!eAdmin()) {
    form.value.setFieldValue('idVendedor', getUserId())
    form.value.setFieldValue('idTabela', tables.value[0].tabela.id)
    form.value.setFieldValue('idPontoVenda', salePoints.value[0].pontoVenda.id)
    loadItens('list-by-price-table', { idTabelaPreco: tables.value[0].tabela.id, idPontoVenda: salePoints.value[0].pontoVenda.id })
  }
}

onMounted(async () => {
  if (id.value) {
    await load(id.value)

    const fields = form.value?.states
    
    loadTables(fields.idVendedor.value)
    loadSalePoints(fields.idVendedor.value)
    if (fields.idCliente.value) {
      loadClient(fields.idCliente.value)
    }
    loadItens('list-by-sale', { idVenda: id.value })
  } else {
    await loadTables(getUserId())
    await loadSalePoints(getUserId())
    loadTableProducts()
  }

  loadUsers()
  loadClients()
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

async function loadItens(path, parameters) {
  try {
    const response = await api.get(`/sale-item/${path}`, { params: parameters })

    itens.value = response.data.map(item => ({
      ...item,
      quantidadeOriginal: item.quantidade ? item.quantidade : 0
    }))

    evaluateTotal()
  } catch (error) {
    toast.add({ severity: "error", summary: "Falha de Carga de Itens de Venda", detail: "Requisição de lista de itens de venda terminou com o erro: " + error.response.data, life: 10000 })
  }
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

const salePoints = ref([])

async function loadSalePoints(idVendedor) {
  try {
    const response = await api.get("/user-sale-point/list", { params: { idUsuario: idVendedor, page: 0, size: 10000, sort: 'pontoVenda.nome,asc' } })

    salePoints.value = response.data.content
  } catch (error) {
    toast.add({ severity: "error", summary: "Falha de Carga de Pontos de Venda do Vendedor", detail: "Requisição de lista de Pontos de Venda do Vendedor terminou com o erro: " + error.response.data, life: 10000 })
  }
}

const clients = ref([])

async function loadClients() {
  try {
    const response = await api.get('/client/find-all')

    clients.value = response.data
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Clientes', detail: 'Requisição de lista de clientes terminou com o erro: ' + error.response.data, life: 10000 })
  }
}

const pj = ref(false)

const loadClient = async (value) => {
  try {
    const response = await api.get('/client', { params: { id: value } })

    pj.value = response.data.cnpj?.length > 0

    form.value.setFieldValue('razaoSocial', response.data.razaoSocial)
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Cliente', detail: 'Requisição de cliente terminou com o erro: ' + error.response.data, life: 10000 })
  }
}

function setAmount(evento, item) {
  item.total = evento.value ? Number.parseInt(evento.value) * item.precoUnitario : null

  evaluateTotal()
}

function evaluateTotal() {
  let subTotal = 0

  itens.value.forEach(item => {
    subTotal += item.total ? item.total : 0
  })

  const total = form.value?.states?.desconto?.value ? (subTotal - (subTotal * form.value.states.desconto.value / 100)) : subTotal

  form.value.setValues({ subTotal: Number.parseFloat(subTotal.toFixed(2)), total: Number.parseFloat(total.toFixed(2)) })
}

function changeDiscount(event) {
  const subTotal = form.value?.states?.subTotal?.value || 0
  const desconto = event.value ? Number.parseFloat(event.value) : 0
  const total = subTotal - (subTotal * Math.min(desconto, 99.99) / 100)

  form.value.setFieldValue('total', Number.parseFloat(total.toFixed(2)))
}

async function changeSalesman(event) {
  await loadTables(event.value)
  await loadSalePoints(event.value)

  if (tables.value.length === 1) {
    form.value.setFieldValue('idTabela', tables.value[0].tabela.id)
    form.value.setFieldValue('idPontoVenda', salePoints.value.length === 1 ? salePoints.value[0].pontoVenda.id : null)
    loadItens('list-by-price-table', { idTabelaPreco: form.value?.states?.idTabela?.value, idPontoVenda: form.value?.states?.idPontoVenda?.value })
  } else {
    form.value.setFieldValue('idTabela', null)
    form.value.setFieldValue('idPontoVenda', null)
    itens.value = []
    evaluateTotal()
  }
}

function changePriceTable(event) {
  if (id.value) {
    loadItens('list-by-sale', { idVenda: id.value })
  } else if (form.value?.states?.idTabela?.value && form.value?.states?.idPontoVenda?.value) {
    loadItens('list-by-price-table', { idTabelaPreco: form.value?.states?.idTabela?.value, idPontoVenda: form.value?.states?.idPontoVenda?.value })
  }
}

function clear() {
  form.value.reset()
  pj.value = false
  itens.value = []
  loadTableProducts()
}

function changeSalePoint(event) {
  if (form.value?.states?.idTabela?.value && form.value?.states?.idPontoVenda?.value) {
    loadItens('list-by-price-table', { idTabelaPreco: form.value?.states?.idTabela?.value, idPontoVenda: form.value?.states?.idPontoVenda?.value })
  }
}
</script>

<template>
  <BlockUI :blocked="loading">
    <Card class="mb-4">
      <template #title>
        <div class="grid grid-cols-2">
          <h3>Venda</h3>
          <div class="flex justify-end items-center" v-show="props.backEndpoint">
            <Button icon="pi pi-replay" @click="router.push(props.backEndpoint)" class="p-button-text" v-tooltip.bottom="'Voltar'"/>
          </div>
        </div>
      </template>

      <template #content>
        <Form ref="form" :resolver="formValidator" :initialValues="formValues" @submit="save" class="grid flex flex-column gap-2">
          <div class="grid grid-cols-12 gap-2">
            <div :class="pj ? 'col-span-5' : 'col-span-12'">
              <FormField v-slot="$field" name="idCliente">
                <FloatLabel variant="on">
                  <Select id="idCliente" :options="clients" optionLabel="nome" optionValue="id" filter fluid @change="loadClient($event)"/>
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
          <div class="grid grid-cols-12 gap-2" v-show="eAdmin()">
            <div class="col-span-4">
              <FormField v-slot="$field" name="idVendedor">
                <FloatLabel variant="on">
                  <Select id="idVendedor" :options="users" optionLabel="email" optionValue="id" fluid @change="changeSalesman($event)"/>
                  <label for="idVendedor">Vendedor</label>
                </FloatLabel>
                <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
              </FormField>
            </div>
            <div class="col-span-4">
              <FormField v-slot="$field" name="idTabela">
                <FloatLabel variant="on">
                  <Select id="idTabela" :options="tables" optionLabel="tabela.nome" optionValue="tabela.id" fluid @change="changePriceTable($event)"/>
                  <label for="idTabela">Tabela de Preços</label>
                </FloatLabel>
                <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
              </FormField>
            </div>
            <div class="col-span-4">
              <FormField v-slot="$field" name="idPontoVenda">
                <FloatLabel variant="on">
                  <Select id="idPontoVenda" :options="salePoints" optionLabel="pontoVenda.nome" optionValue="pontoVenda.id" fluid @change="changeSalePoint($event)"/>
                  <label for="idPontoVenda">Ponto de Venda</label>
                </FloatLabel>
                <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
              </FormField>
            </div>
          </div>
          <div class="grid grid-cols-12 gap-2">
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
          <div class="flex justify-end gap-2 mt-2">
            <Button label="Limpar" icon="pi pi-times" @click="clear" severity="secondary" raised size="small"/>
            <Button label="Salvar & Nova" icon="pi pi-plus" type="submit" iconPos="left" raised @click="submitAction = 'saveNew'" size="small"/>
            <Button label="Salvar" icon="pi pi-save" type="submit" raised size="small"/>
          </div>
        </Form>
      </template>
    </Card>
    <Card class="mb-4">
      <template #title>
        <div class="grid grid-cols-2">
          <h3>Itens da Venda</h3>
          <div class="flex justify-end items-center" v-show="props.backEndpoint">
            <Button icon="pi pi-replay" @click="router.push('/core/sale')" class="p-button-text" v-tooltip.bottom="'Voltar'"/>
          </div>
        </div>
      </template>

      <template #content>
        <DataTable :value="itens" :lazy="true" responsiveLayout="scroll" stripedRows size="small">
          <Column field="id" header="Id" v-if="id"/>
          <Column field="tabelaPrecoProduto.produto.nome" header="Nome"/>
          <Column field="tabelaPrecoProduto.produto.referencia" header="Referência"/>
          <Column field="tabelaPrecoProduto.produto.estoque" header="Estoque"/>
          <Column field="quantidade" header="Quantidade">
            <template #body="slotProps">
              <InputNumber v-model="slotProps.data.quantidade" :min="0" :max="slotProps.data.tabelaPrecoProduto.produto.estoque + slotProps.data.quantidadeOriginal" :step="1" showButtons buttonLayout="horizontal" :maxFractionDigits="0" @input="setAmount($event, slotProps.data)" @blur="setAmount($event, slotProps.data)" size="small"/>
            </template>
          </Column>
          <Column field="precoUnitario" header="Preço Unitário (R$)">
            <template #body="slotProps">
              {{ formatNumber(slotProps.data.precoUnitario) }}
            </template>
          </Column>
          <Column field="total" header="Total (R$)">
            <template #body="slotProps">
              {{ formatNumber(slotProps.data.total) }}
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>
  </BlockUI>
</template>
