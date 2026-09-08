import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'

import SaleComponent from '../SaleComponent.vue'

import api from '@/util/api'
import { eAdmin, getUserId } from '@/util/auth'
import { formatNumber } from '@/util/util'

vi.mock('@/util/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn()
  }
}))

vi.mock('@/util/auth', () => ({
  eAdmin: vi.fn(),getUserId: vi.fn()
}))

vi.mock('@/util/util', () => ({
  formatNumber: vi.fn((value) => String(value))
}))

const toastAddMock = vi.fn()
vi.mock('primevue/usetoast', () => ({
  useToast: () => ({ add: toastAddMock })
}))

const routerPushMock = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: routerPushMock })
}))

const { FormStub, FormFieldStub, CardStub, genericStub, ButtonStub, DataTableStub, InputNumberStub } = await vi.hoisted(async () => {
  const { defineComponent, h, inject, provide, reactive } = await import('vue')

  const FormStub = defineComponent({
    name: 'Form',
    props: ['resolver', 'validateOn', 'initialValues'],
    emits: ['submit'],
    setup(props, { slots, expose, emit }) {
      const values = reactive({ ...(props.initialValues || {}) })
      const invalidMap = reactive({})

      function setValues(newValues) {
        Object.assign(values, newValues)
      }
      function setFieldValue(key, val) {
        values[key] = val
      }
      function reset() {
        Object.keys(values).forEach((key) => {
          values[key] = (props.initialValues || {})[key] ?? null
        })
      }
      function setFieldInvalid(key, message = 'Campo inválido.') {
        invalidMap[key] = message
      }
      function clearFieldInvalid(key) {
        delete invalidMap[key]
      }
      function submitWith(payload) {
        emit('submit', payload)
      }

      const states = new Proxy(
        {},
        {
          get(_target, key) {
            if (typeof key !== 'string') return undefined
            return {
              get value() {
                return values[key]
              },
              set value(v) {
                values[key] = v
              },
              get invalid() {
                return Boolean(invalidMap[key])
              },
              get error() {
                return invalidMap[key] ? { message: invalidMap[key] } : null
              }
            }
          }
        }
      )

      provide('__formStubCtx', { values, invalidMap })
      expose({ states, setValues, setFieldValue, reset, setFieldInvalid, clearFieldInvalid, submitWith })

      return () => (slots.default ? slots.default() : null)
    }
  })

  const FormFieldStub = defineComponent({
    name: 'FormField',
    props: ['name'],
    setup(props, { slots }) {
      const ctx = inject('__formStubCtx', null)
      return () => {
        const field = ctx
          ? {
              invalid: Boolean(ctx.invalidMap[props.name]),
              error: ctx.invalidMap[props.name] ? { message: ctx.invalidMap[props.name] } : null
            }
          : { invalid: false, error: null }
        return slots.default ? slots.default(field) : null
      }
    }
  })

  const CardStub = defineComponent({
    name: 'Card',
    setup(_, { slots }) {
      return () =>
        h('div', { class: 'card-teststub' }, [
          slots.title ? h('div', {}, slots.title()) : null,
          slots.content ? h('div', {}, slots.content()) : null,
          slots.default ? slots.default() : null
        ])
    }
  })

  function genericStub(name, tag) {
    const renderTag = tag || `${name.toLowerCase()}-teststub`
    return defineComponent({
      name,
      inheritAttrs: false,
      setup(_, { attrs, slots, expose }) {
        // Expõe um helper de teste para disparar manualmente um handler
        // recebido via attrs (ex.: onChange), simulando o componente real
        // emitindo o evento — necessário para cobrir os wrappers inline
        // do template como `@change="loadClient($event.value)"`, que são
        // funções distintas de `loadClient` e só contam como cobertas
        // quando de fato invocadas.
        expose({
          emit(eventName, payload) {
            const handlerKey = 'on' + eventName.charAt(0).toUpperCase() + eventName.slice(1)
            if (typeof attrs[handlerKey] === 'function') attrs[handlerKey](payload)
          }
        })
        return () => h(renderTag, attrs, slots.default ? slots.default() : undefined)
      }
    })
  }

  const ButtonStub = genericStub('Button', 'button')

  // Stub de <DataTable> que replica o essencial do contrato real: para
  // cada linha de `value`, invoca o slot nomeado #body de cada <Column>
  // filha (lido diretamente do vnode retornado por slots.default(), do
  // jeito que a própria implementação real do PrimeVue faz), passando
  // { data: linha }. Isso é o que permite cobrir de verdade o código dos
  // templates #body (quantidade/preço/total), sem precisar reimplementar
  // o DataTable inteiro.
  const DataTableStub = defineComponent({
    name: 'DataTable',
    props: ['value'],
    setup(props, { slots }) {
      return () => {
        const columnVNodes = slots.default ? slots.default() : []
        const rows = props.value || []
        return h(
          'datatable-teststub',
          {},
          rows.map((row, rowIndex) =>
            h(
              'row-teststub',
              { key: rowIndex },
              columnVNodes.map((colVNode, colIndex) => {
                const bodySlot = colVNode && colVNode.children && typeof colVNode.children === 'object' ? colVNode.children.body : null
                if (typeof bodySlot === 'function') {
                  return h('cell-teststub', { key: colIndex }, bodySlot({ data: row }))
                }
                return null
              })
            )
          )
        )
      }
    }
  })

  // Stub específico de <InputNumber>: renderiza um <input> nativo de
  // verdade e faz a ponte manual entre os handlers recebidos via props
  // não-declaradas (attrs) — onUpdate:modelValue (do v-model), onInput e
  // onBlur — e o evento nativo do DOM. Isso permite disparar de fato o
  // v-model e os handlers @input/@blur do template real (ex.: a coluna
  // "Quantidade" da tabela de itens, que chama setAmount()), em vez de
  // só renderizar a tag sem nunca invocar esses fechamentos.
  const InputNumberStub = defineComponent({
    name: 'InputNumber',
    inheritAttrs: false,
    setup(_, { attrs }) {
      return () =>
        h('input', {
          ...attrs,
          class: 'inputnumber-teststub',
          onInput: (event) => {
            const raw = event.target.value
            const value = raw === '' ? null : Number(raw)
            if (typeof attrs['onUpdate:modelValue'] === 'function') attrs['onUpdate:modelValue'](value)
            if (typeof attrs.onInput === 'function') attrs.onInput({ value, originalEvent: event })
          },
          onBlur: (event) => {
            const raw = event.target.value
            const value = raw === '' ? null : Number(raw)
            if (typeof attrs.onBlur === 'function') attrs.onBlur({ value, originalEvent: event })
          }
        })
    }
  })

  return { FormStub, FormFieldStub, CardStub, genericStub, ButtonStub, DataTableStub, InputNumberStub }
})

vi.mock('@primevue/forms/form', () => ({ default: FormStub }))
vi.mock('@primevue/forms/formfield', () => ({ default: FormFieldStub }))
vi.mock('primevue/card', () => ({ default: CardStub }))
vi.mock('primevue/button', () => ({ default: ButtonStub }))
vi.mock('primevue/select', () => ({ default: genericStub('Select') }))
vi.mock('primevue/inputnumber', () => ({ default: InputNumberStub }))
vi.mock('primevue/inputtext', () => ({ default: genericStub('InputText') }))
vi.mock('primevue/textarea', () => ({ default: genericStub('Textarea') }))
vi.mock('primevue/message', () => ({ default: genericStub('Message') }))
vi.mock('primevue/floatlabel', () => ({ default: genericStub('FloatLabel') }))
vi.mock('primevue/datatable', () => ({ default: DataTableStub }))
vi.mock('primevue/column', () => ({ default: genericStub('Column') }))
vi.mock('primevue/tooltip', () => ({ default: {} }))

// ------------------------------------------------------------------
// Fixtures
// ------------------------------------------------------------------

const usersFixture = [{ id: 1, email: 'vendedor1@teste.com' }]
const tablesFixture = [{ tabela: { id: 10, nome: 'Tabela A' } }]
const salePointsRawFixture = [{ pontoVenda: { id: 20, nome: 'Loja A', empresa: { id: 99 } } }]
const clientsFixture = [{ id: 30, nome: 'Cliente A' }]

const saleFixture = {
  cliente: { id: 30 },
  vendedor: { id: 1 },
  tabela: { id: 10 },
  pontoVenda: { id: 20 },
  subTotal: 100,
  desconto: 10,
  total: 90,
  observacoes: 'Observação teste'
}

const itemsFixture = [
  {
    id: 1,
    quantidade: 2,
    quantidadeOriginal: 0,
    precoUnitario: 50,
    total: 100,
    tabelaPrecoProduto: { produto: { estoque: 5, nome: 'Produto A' } }
  },
  {
    id: 2,
    quantidade: null,
    quantidadeOriginal: 0,
    precoUnitario: 20,
    total: null,
    tabelaPrecoProduto: { produto: { estoque: 10, nome: 'Produto B' } }
  }
]

function defaultApiGetImpl(url, config) {
  if (url === '/user/list') return Promise.resolve({ data: { content: usersFixture } })
  if (url === '/user-price-table/list') return Promise.resolve({ data: { content: tablesFixture } })
  if (url === '/user-sale-point/list') return Promise.resolve({ data: { content: salePointsRawFixture } })
  if (url === '/client/list-people') return Promise.resolve({ data: { content: clientsFixture } })
  if (url === '/client/find-all') return Promise.resolve({ data: clientsFixture })
  if (url === '/client') {
    return Promise.resolve({ data: { id: config?.params?.id, razaoSocial: 'Empresa X', cnpj: '' } })
  }
  if (url === '/sale') return Promise.resolve({ data: saleFixture })
  if (url && url.startsWith('/sale-item/')) return Promise.resolve({ data: itemsFixture })
  return Promise.resolve({ data: {} })
}

function rejectWith(message) {
  return Promise.reject({ response: { data: message } })
}

async function mountComponent(props = {}) {
  const wrapper = mount(SaleComponent, { props })
  await flushPromises()
  return wrapper
}

beforeEach(() => {
  vi.clearAllMocks()
  eAdmin.mockReturnValue(true)
  getUserId.mockReturnValue(1)
  api.get.mockImplementation(defaultApiGetImpl)
  api.post.mockResolvedValue({ status: 200, data: { id: 123 } })
})

afterEach(() => {
  vi.restoreAllMocks()
})

// ==================================================================
// Ciclo de montagem (onMounted)
// ==================================================================
describe('onMounted', () => {
  it('venda nova + usuário não admin: preenche vendedor/tabela/ponto de venda e carrega itens da tabela', async () => {
    eAdmin.mockReturnValue(false)
    getUserId.mockReturnValue(7)

    const wrapper = await mountComponent({ id: null })

    expect(wrapper.vm.form.states.idVendedor.value).toBe(7)
    expect(wrapper.vm.form.states.idTabela.value).toBe(tablesFixture[0].tabela.id)
    expect(wrapper.vm.form.states.idPontoVenda.value).toBe(salePointsRawFixture[0].pontoVenda.id)

    expect(api.get).toHaveBeenCalledWith(
      '/sale-item/list-by-price-table',
      expect.objectContaining({
        params: { idTabelaPreco: tablesFixture[0].tabela.id, idPontoVenda: salePointsRawFixture[0].pontoVenda.id }
      })
    )
    expect(api.get).toHaveBeenCalledWith('/user/list', expect.anything())
    // Como o ponto de venda preenchido automaticamente tem empresa vinculada,
    // loadClients() busca os colaboradores da empresa, não todos os clientes.
    expect(api.get).toHaveBeenCalledWith(
      '/client/list-people',
      expect.objectContaining({ params: expect.objectContaining({ idEmpresa: 99 }) })
    )
  })

  it('venda nova + usuário admin: não preenche vendedor automaticamente', async () => {
    eAdmin.mockReturnValue(true)

    const wrapper = await mountComponent({ id: null })

    expect(wrapper.vm.form.states.idVendedor.value).toBeFalsy()
  })

  it('edição de venda: carrega a venda e, quando há cliente, também carrega o cliente', async () => {
    const wrapper = await mountComponent({ id: 5 })

    expect(api.get).toHaveBeenCalledWith('/sale', expect.objectContaining({ params: { id: 5 } }))
    expect(api.get).toHaveBeenCalledWith('/client', expect.objectContaining({ params: { id: saleFixture.cliente.id } }))
    expect(api.get).toHaveBeenCalledWith('/sale-item/list-by-sale', expect.objectContaining({ params: { idVenda: 5 } }))
  })

  it('edição de venda sem cliente vinculado: não chama loadClient', async () => {
    api.get.mockImplementation((url, config) => {
      if (url === '/sale') return Promise.resolve({ data: { ...saleFixture, cliente: undefined } })
      return defaultApiGetImpl(url, config)
    })

    await mountComponent({ id: 5 })

    expect(api.get).not.toHaveBeenCalledWith('/client', expect.anything())
  })
})

// ==================================================================
// load()
// ==================================================================
describe('load', () => {
  it('preenche o formulário com os dados retornados', async () => {
    const wrapper = await mountComponent({ id: null })

    await wrapper.vm.load(42)

    expect(wrapper.vm.form.states.idVendedor.value).toBe(saleFixture.vendedor.id)
    expect(wrapper.vm.form.states.idTabela.value).toBe(saleFixture.tabela.id)
    expect(wrapper.vm.form.states.idPontoVenda.value).toBe(saleFixture.pontoVenda.id)
    expect(wrapper.vm.form.states.subTotal.value).toBe(saleFixture.subTotal)
    expect(wrapper.vm.form.states.observacoes.value).toBe(saleFixture.observacoes)
  })

  it('funciona quando a venda não possui cliente vinculado', async () => {
    api.get.mockImplementation((url, config) => {
      if (url === '/sale') return Promise.resolve({ data: { ...saleFixture, cliente: undefined } })
      return defaultApiGetImpl(url, config)
    })
    const wrapper = await mountComponent({ id: null })

    await wrapper.vm.load(42)

    expect(wrapper.vm.form.states.idCliente.value).toBeUndefined()
  })

  it('não faz nada se o formulário ainda não estiver disponível', async () => {
    const wrapper = await mountComponent({ id: null })
    wrapper.vm.form = null

    await expect(wrapper.vm.load(42)).resolves.not.toThrow()
  })

  it('exibe toast de erro quando a requisição falha', async () => {
    api.get.mockImplementation((url, config) => {
      if (url === '/sale') return rejectWith('Venda não encontrada')
      return defaultApiGetImpl(url, config)
    })
    const wrapper = await mountComponent({ id: null })

    await wrapper.vm.load(999)

    expect(toastAddMock).toHaveBeenCalledWith(
      expect.objectContaining({ severity: 'error', summary: 'Falha de Carga da Venda' })
    )
  })
})

// ==================================================================
// save()
// ==================================================================
describe('save', () => {
  it('não faz nada quando o formulário é inválido', async () => {
    const wrapper = await mountComponent({ id: null })
    api.post.mockClear()

    await wrapper.vm.save({ valid: false, values: {} })

    expect(api.post).not.toHaveBeenCalled()
  })

  it('exibe erro quando não há itens com quantidade informada', async () => {
    const wrapper = await mountComponent({ id: null })
    wrapper.vm.itens = [{ ...itemsFixture[1], quantidade: 0 }]
    api.post.mockClear()

    await wrapper.vm.save({ valid: true, values: { idVendedor: 1, idTabela: 10, idPontoVenda: 20 } })

    expect(toastAddMock).toHaveBeenCalledWith(
      expect.objectContaining({ summary: 'Itens de Venda necessários' })
    )
    expect(api.post).not.toHaveBeenCalled()
  })

  it('exibe erro quando a quantidade solicitada supera o estoque disponível', async () => {
    const wrapper = await mountComponent({ id: null })
    wrapper.vm.itens = [
      {
        id: 1,
        quantidade: 10,
        quantidadeOriginal: 0,
        precoUnitario: 5,
        tabelaPrecoProduto: { produto: { estoque: 5 } }
      }
    ]
    api.post.mockClear()

    await wrapper.vm.save({ valid: true, values: { idVendedor: 1, idTabela: 10, idPontoVenda: 20 } })

    expect(toastAddMock).toHaveBeenCalledWith(
      expect.objectContaining({ summary: 'Itens de Venda fora de estoque' })
    )
    expect(api.post).not.toHaveBeenCalled()
  })

  it('salva a venda sem cliente e, com submitAction "save", recarrega a venda', async () => {
    const wrapper = await mountComponent({ id: null })
    wrapper.vm.itens = [{ ...itemsFixture[0] }]
    wrapper.vm.submitAction = 'save'
    api.get.mockClear()

    const values = { idVendedor: 1, idTabela: 10, idPontoVenda: 20, subTotal: 100, desconto: 0, total: 100, observacoes: null }
    await wrapper.vm.save({ valid: true, values })

    expect(api.post).toHaveBeenCalledWith(
      '/sale',
      expect.objectContaining({
        vendedor: { id: 1 },
        tabela: { id: 10 },
        pontoVenda: { id: 20 }
      })
    )
    const [, paramsSent] = api.post.mock.calls[0]
    // id.value é null (venda nova) -> Number.parseInt(null) é NaN, comportamento real do componente
    expect(Number.isNaN(paramsSent.id)).toBe(true)
    expect(paramsSent.cliente).toBeUndefined()

    expect(api.post).toHaveBeenCalledWith('/sale-item/save-items', expect.any(Array))
    expect(wrapper.vm.id).toBe(123)
    expect(api.get).toHaveBeenCalledWith('/sale', expect.objectContaining({ params: { id: 123 } }))
    expect(toastAddMock).toHaveBeenCalledWith(expect.objectContaining({ severity: 'success' }))
  })

  it('salva a venda com cliente informado', async () => {
    const wrapper = await mountComponent({ id: null })
    wrapper.vm.itens = [{ ...itemsFixture[0] }]
    wrapper.vm.submitAction = 'save'

    const values = { idCliente: 30, idVendedor: 1, idTabela: 10, idPontoVenda: 20, subTotal: 100, desconto: 0, total: 100 }
    await wrapper.vm.save({ valid: true, values })

    const [, paramsSent] = api.post.mock.calls[0]
    expect(paramsSent.cliente).toEqual({ id: 30 })
  })

  it('vincula a venda apenas aos itens com quantidade informada, ignorando os que ficaram com quantidade zero', async () => {
    const wrapper = await mountComponent({ id: null })
    const itemComQuantidade = { ...itemsFixture[0], quantidade: 2 }
    const itemSemQuantidade = { ...itemsFixture[1], quantidade: 0, quantidadeOriginal: 0 }
    wrapper.vm.itens = [itemComQuantidade, itemSemQuantidade]
    wrapper.vm.submitAction = 'save'

    const values = { idVendedor: 1, idTabela: 10, idPontoVenda: 20, subTotal: 100, desconto: 0, total: 100 }
    await wrapper.vm.save({ valid: true, values })

    expect(itemComQuantidade.venda).toEqual({ id: 123, pontoVenda: { id: 20 } })
    expect(itemSemQuantidade.venda).toBeUndefined()

    const saveItemsCall = api.post.mock.calls.find((call) => call[0] === '/sale-item/save-items')
    expect(saveItemsCall[1]).toEqual([itemComQuantidade])
  })

  it('com submitAction "saveNew", zera o id e limpa o formulário', async () => {
    const wrapper = await mountComponent({ id: null })
    wrapper.vm.itens = [{ ...itemsFixture[0] }]
    wrapper.vm.submitAction = 'saveNew'
    const resetSpy = vi.spyOn(wrapper.vm.form, 'reset')

    const values = { idVendedor: 1, idTabela: 10, idPontoVenda: 20, subTotal: 100, desconto: 0, total: 100 }
    await wrapper.vm.save({ valid: true, values })

    expect(resetSpy).toHaveBeenCalled()
    expect(wrapper.vm.pj).toBe(false)
    expect(wrapper.vm.id).toBeNull()
  })

  it('não executa o pós-processamento quando a API não retorna status 200', async () => {
    api.post.mockResolvedValueOnce({ status: 204, data: {} })
    const wrapper = await mountComponent({ id: null })
    wrapper.vm.itens = [{ ...itemsFixture[0] }]

    const values = { idVendedor: 1, idTabela: 10, idPontoVenda: 20, subTotal: 100, desconto: 0, total: 100 }
    await wrapper.vm.save({ valid: true, values })

    expect(api.post).toHaveBeenCalledTimes(1)
    expect(toastAddMock).not.toHaveBeenCalledWith(expect.objectContaining({ severity: 'success' }))
  })

  it('exibe toast de erro quando a gravação falha', async () => {
    api.post.mockRejectedValueOnce({ response: { data: 'Erro de gravação' } })
    const wrapper = await mountComponent({ id: null })
    wrapper.vm.itens = [{ ...itemsFixture[0] }]

    const values = { idVendedor: 1, idTabela: 10, idPontoVenda: 20, subTotal: 100, desconto: 0, total: 100 }
    await wrapper.vm.save({ valid: true, values })

    expect(toastAddMock).toHaveBeenCalledWith(
      expect.objectContaining({ severity: 'error', summary: 'Falha de Gravação da Venda' })
    )
  })
})

// ==================================================================
// loadTableProducts()
// ==================================================================
describe('loadTableProducts', () => {
  it('preenche os campos padrão e carrega os itens quando o usuário não é admin', async () => {
    eAdmin.mockReturnValue(false)
    getUserId.mockReturnValue(3)
    const wrapper = await mountComponent({ id: null })
    api.get.mockClear()

    wrapper.vm.tables = [{ tabela: { id: 55 } }]
    wrapper.vm.salePoints = [{ id: 66 }]

    wrapper.vm.loadTableProducts()
    await flushPromises()

    expect(wrapper.vm.form.states.idVendedor.value).toBe(3)
    expect(wrapper.vm.form.states.idTabela.value).toBe(55)
    expect(wrapper.vm.form.states.idPontoVenda.value).toBe(66)
    expect(api.get).toHaveBeenCalledWith(
      '/sale-item/list-by-price-table',
      expect.objectContaining({ params: { idTabelaPreco: 55, idPontoVenda: 66 } })
    )
  })

  it('não faz nada quando o usuário é admin', async () => {
    eAdmin.mockReturnValue(true)
    const wrapper = await mountComponent({ id: null })
    api.get.mockClear()

    wrapper.vm.loadTableProducts()
    await flushPromises()

    expect(api.get).not.toHaveBeenCalledWith('/sale-item/list-by-price-table', expect.anything())
  })
})

// ==================================================================
// loadUsers()
// ==================================================================
describe('loadUsers', () => {
  it('carrega a lista de usuários com sucesso', async () => {
    const wrapper = await mountComponent({ id: null })

    await wrapper.vm.loadUsers()

    expect(wrapper.vm.users).toEqual(usersFixture)
  })

  it('exibe toast de erro em caso de falha', async () => {
    api.get.mockImplementation((url, config) => {
      if (url === '/user/list') return rejectWith('Erro de usuários')
      return defaultApiGetImpl(url, config)
    })
    const wrapper = await mountComponent({ id: null })

    await wrapper.vm.loadUsers()

    expect(toastAddMock).toHaveBeenCalledWith(
      expect.objectContaining({ summary: 'Falha de Carga de Vendedores' })
    )
  })
})

// ==================================================================
// loadItens()
// ==================================================================
describe('loadItens', () => {
  it('mapeia quantidadeOriginal e recalcula os totais', async () => {
    const wrapper = await mountComponent({ id: null })

    await wrapper.vm.loadItens('list-by-sale', { idVenda: 1 })

    expect(wrapper.vm.itens[0].quantidadeOriginal).toBe(itemsFixture[0].quantidade)
    expect(wrapper.vm.itens[1].quantidadeOriginal).toBe(0)
    expect(wrapper.vm.form.states.subTotal.value).toBe(100)
  })

  it('exibe toast de erro em caso de falha', async () => {
    api.get.mockImplementation((url, config) => {
      if (url.startsWith('/sale-item/')) return rejectWith('Erro de itens')
      return defaultApiGetImpl(url, config)
    })
    const wrapper = await mountComponent({ id: null })

    await wrapper.vm.loadItens('list-by-sale', { idVenda: 1 })

    expect(toastAddMock).toHaveBeenCalledWith(
      expect.objectContaining({ summary: 'Falha de Carga de Itens de Venda' })
    )
  })
})

// ==================================================================
// loadTables()
// ==================================================================
describe('loadTables', () => {
  it('carrega as tabelas de preço com sucesso', async () => {
    const wrapper = await mountComponent({ id: null })

    await wrapper.vm.loadTables(1)

    expect(wrapper.vm.tables).toEqual(tablesFixture)
  })

  it('exibe toast de erro em caso de falha', async () => {
    api.get.mockImplementation((url, config) => {
      if (url === '/user-price-table/list') return rejectWith('Erro de tabelas')
      return defaultApiGetImpl(url, config)
    })
    const wrapper = await mountComponent({ id: null })

    await wrapper.vm.loadTables(1)

    expect(toastAddMock).toHaveBeenCalledWith(
      expect.objectContaining({ summary: 'Falha de Carga de Tabelas de Preço do Vendedor' })
    )
  })
})

// ==================================================================
// loadSalePoints()
// ==================================================================
describe('loadSalePoints', () => {
  it('carrega e "desembrulha" os pontos de venda com sucesso', async () => {
    const wrapper = await mountComponent({ id: null })

    await wrapper.vm.loadSalePoints(1)

    expect(wrapper.vm.salePoints).toEqual(salePointsRawFixture.map((item) => item.pontoVenda))
  })

  it('exibe toast de erro em caso de falha', async () => {
    api.get.mockImplementation((url, config) => {
      if (url === '/user-sale-point/list') return rejectWith('Erro de pontos de venda')
      return defaultApiGetImpl(url, config)
    })
    const wrapper = await mountComponent({ id: null })

    await wrapper.vm.loadSalePoints(1)

    expect(toastAddMock).toHaveBeenCalledWith(
      expect.objectContaining({ summary: 'Falha de Carga de Pontos de Venda do Vendedor' })
    )
  })
})

// ==================================================================
// loadClients()
// ==================================================================
describe('loadClients', () => {
  it('carrega todos os clientes quando não há ponto de venda selecionado', async () => {
    const wrapper = await mountComponent({ id: null })
    wrapper.vm.form.setFieldValue('idPontoVenda', null)
    api.get.mockClear()

    await wrapper.vm.loadClients()

    expect(api.get).toHaveBeenCalledWith('/client/find-all')
  })

  it('carrega apenas os colaboradores da empresa quando o ponto de venda tem empresa vinculada', async () => {
    const wrapper = await mountComponent({ id: null })
    wrapper.vm.salePoints = [{ id: 20, empresa: { id: 99 } }]
    wrapper.vm.form.setFieldValue('idPontoVenda', 20)
    api.get.mockClear()

    await wrapper.vm.loadClients()

    expect(api.get).toHaveBeenCalledWith(
      '/client/list-people',
      expect.objectContaining({ params: expect.objectContaining({ idEmpresa: 99 }) })
    )
  })

  it('exibe toast de erro quando a busca de colaboradores falha', async () => {
    const wrapper = await mountComponent({ id: null })
    wrapper.vm.salePoints = [{ id: 20, empresa: { id: 99 } }]
    wrapper.vm.form.setFieldValue('idPontoVenda', 20)
    api.get.mockImplementation((url, config) => {
      if (url === '/client/list-people') return rejectWith('Erro de colaboradores')
      return defaultApiGetImpl(url, config)
    })

    await wrapper.vm.loadClients()

    expect(toastAddMock).toHaveBeenCalledWith(
      expect.objectContaining({ summary: 'Falha de Carga Colaboradores' })
    )
  })

  it('carrega todos os clientes quando o ponto de venda não tem empresa vinculada', async () => {
    const wrapper = await mountComponent({ id: null })
    wrapper.vm.salePoints = [{ id: 20, empresa: null }]
    wrapper.vm.form.setFieldValue('idPontoVenda', 20)
    api.get.mockClear()

    await wrapper.vm.loadClients()

    expect(api.get).toHaveBeenCalledWith('/client/find-all')
  })
})

// ==================================================================
// loadAllClients()
// ==================================================================
describe('loadAllClients', () => {
  it('carrega a lista completa de clientes', async () => {
    const wrapper = await mountComponent({ id: null })

    await wrapper.vm.loadAllClients()

    expect(wrapper.vm.clients).toEqual(clientsFixture)
  })

  it('exibe toast de erro em caso de falha', async () => {
    api.get.mockImplementation((url, config) => {
      if (url === '/client/find-all') return rejectWith('Erro de clientes')
      return defaultApiGetImpl(url, config)
    })
    const wrapper = await mountComponent({ id: null })

    await wrapper.vm.loadAllClients()

    expect(toastAddMock).toHaveBeenCalledWith(
      expect.objectContaining({ summary: 'Falha de Carga de Clientes' })
    )
  })
})

// ==================================================================
// loadClient()
// ==================================================================
describe('loadClient', () => {
  it('marca pj = true quando o cliente possui CNPJ', async () => {
    api.get.mockImplementation((url, config) => {
      if (url === '/client') return Promise.resolve({ data: { razaoSocial: 'Empresa Y', cnpj: '12345678000190' } })
      return defaultApiGetImpl(url, config)
    })
    const wrapper = await mountComponent({ id: null })

    await wrapper.vm.loadClient(30)

    expect(wrapper.vm.pj).toBe(true)
    expect(wrapper.vm.form.states.razaoSocial.value).toBe('Empresa Y')
  })

  it('marca pj = false quando o cliente não possui CNPJ', async () => {
    api.get.mockImplementation((url, config) => {
      if (url === '/client') return Promise.resolve({ data: { razaoSocial: 'Pessoa Física', cnpj: '' } })
      return defaultApiGetImpl(url, config)
    })
    const wrapper = await mountComponent({ id: null })

    await wrapper.vm.loadClient(30)

    expect(wrapper.vm.pj).toBe(false)
  })

  it('exibe toast de erro em caso de falha', async () => {
    api.get.mockImplementation((url, config) => {
      if (url === '/client') return rejectWith('Erro de cliente')
      return defaultApiGetImpl(url, config)
    })
    const wrapper = await mountComponent({ id: null })

    await wrapper.vm.loadClient(30)

    expect(toastAddMock).toHaveBeenCalledWith(
      expect.objectContaining({ summary: 'Falha de Carga de Cliente' })
    )
  })
})

// ==================================================================
// setAmount() / evaluateTotal()
// ==================================================================
describe('setAmount e evaluateTotal', () => {
  it('calcula o total do item quando a quantidade é informada', async () => {
    const wrapper = await mountComponent({ id: null })
    const item = { quantidade: 3, precoUnitario: 10, total: null, quantidadeOriginal: 0, tabelaPrecoProduto: { produto: { estoque: 10 } } }
    wrapper.vm.itens = [item]
    await nextTick()

    wrapper.vm.setAmount({ value: 3 }, item)
    await nextTick()

    expect(item.total).toBe(30)
    expect(wrapper.vm.form.states.subTotal.value).toBe(30)
  })

  it('zera o total do item quando a quantidade é removida', async () => {
    const wrapper = await mountComponent({ id: null })
    const item = { quantidade: null, precoUnitario: 10, total: 30, quantidadeOriginal: 0, tabelaPrecoProduto: { produto: { estoque: 10 } } }
    wrapper.vm.itens = [item]
    await nextTick()

    wrapper.vm.setAmount({ value: null }, item)
    await nextTick()

    expect(item.total).toBeNull()
  })

  it('aplica o desconto configurado no formulário ao recalcular o total geral', async () => {
    const wrapper = await mountComponent({ id: null })
    wrapper.vm.form.setFieldValue('desconto', 10)
    wrapper.vm.itens = [
      { total: 100, quantidade: 1, quantidadeOriginal: 0, precoUnitario: 100, tabelaPrecoProduto: { produto: { estoque: 10 } } },
      { total: null, quantidade: null, quantidadeOriginal: 0, precoUnitario: 20, tabelaPrecoProduto: { produto: { estoque: 10 } } }
    ]
    await nextTick()

    wrapper.vm.evaluateTotal()
    await nextTick()

    expect(wrapper.vm.form.states.subTotal.value).toBe(100)
    expect(wrapper.vm.form.states.total.value).toBe(90)
  })

  it('usa o subtotal como total quando não há desconto', async () => {
    const wrapper = await mountComponent({ id: null })
    wrapper.vm.form.setFieldValue('desconto', null)
    wrapper.vm.itens = [{ total: 50, quantidade: 1, quantidadeOriginal: 0, precoUnitario: 50, tabelaPrecoProduto: { produto: { estoque: 10 } } }]
    await nextTick()

    wrapper.vm.evaluateTotal()
    await nextTick()

    expect(wrapper.vm.form.states.total.value).toBe(50)
  })
})

// ==================================================================
// changeDiscount()
// ==================================================================
describe('changeDiscount', () => {
  it('calcula o total considerando o subtotal e o desconto informados', async () => {
    const wrapper = await mountComponent({ id: null })
    wrapper.vm.form.setFieldValue('subTotal', 200)

    wrapper.vm.changeDiscount({ value: 10 })

    expect(wrapper.vm.form.states.total.value).toBe(180)
  })

  it('usa subtotal 0 quando não há subtotal definido e desconto 0 quando o valor não é informado', async () => {
    const wrapper = await mountComponent({ id: null })
    wrapper.vm.form.setFieldValue('subTotal', null)

    wrapper.vm.changeDiscount({ value: null })

    expect(wrapper.vm.form.states.total.value).toBe(0)
  })

  it('limita o desconto máximo em 99.99%', async () => {
    const wrapper = await mountComponent({ id: null })
    wrapper.vm.form.setFieldValue('subTotal', 200)

    wrapper.vm.changeDiscount({ value: 150 })

    expect(wrapper.vm.form.states.total.value).toBe(0.02)
  })
})

// ==================================================================
// changeSalesman()
// ==================================================================
describe('changeSalesman', () => {
  it('recarrega tabelas e pontos de venda e preenche os campos padrão', async () => {
    const wrapper = await mountComponent({ id: null })
    api.get.mockClear()

    await wrapper.vm.changeSalesman({ value: 9 })

    expect(api.get).toHaveBeenCalledWith('/user-price-table/list', expect.objectContaining({ params: expect.objectContaining({ idVendedor: 9 }) }))
    expect(api.get).toHaveBeenCalledWith('/user-sale-point/list', expect.objectContaining({ params: expect.objectContaining({ idUsuario: 9 }) }))
    expect(wrapper.vm.form.states.idTabela.value).toBe(tablesFixture[0].tabela.id)
    expect(wrapper.vm.form.states.idPontoVenda.value).toBe(salePointsRawFixture[0].pontoVenda.id)
    expect(api.get).toHaveBeenCalledWith('/sale-item/list-by-price-table', expect.anything())
  })
})

// ==================================================================
// changePriceTable()
// ==================================================================
describe('changePriceTable', () => {
  it('recarrega os itens da venda quando já existe um id de venda', async () => {
    const wrapper = await mountComponent({ id: 5 })
    api.get.mockClear()

    wrapper.vm.changePriceTable({})
    await flushPromises()

    expect(api.get).toHaveBeenCalledWith('/sale-item/list-by-sale', expect.objectContaining({ params: { idVenda: 5 } }))
  })

  it('recarrega os itens pela tabela de preços quando não há id de venda e os campos estão preenchidos', async () => {
    const wrapper = await mountComponent({ id: null })
    wrapper.vm.form.setValues({ idTabela: 10, idPontoVenda: 20 })
    api.get.mockClear()

    wrapper.vm.changePriceTable({})
    await flushPromises()

    expect(api.get).toHaveBeenCalledWith(
      '/sale-item/list-by-price-table',
      expect.objectContaining({ params: { idTabelaPreco: 10, idPontoVenda: 20 } })
    )
  })

  it('não faz nada quando faltam tabela ou ponto de venda', async () => {
    const wrapper = await mountComponent({ id: null })
    wrapper.vm.form.setValues({ idTabela: null, idPontoVenda: null })
    api.get.mockClear()

    wrapper.vm.changePriceTable({})
    await flushPromises()

    expect(api.get).not.toHaveBeenCalled()
  })
})

// ==================================================================
// changeSalePoint()
// ==================================================================
describe('changeSalePoint', () => {
  it('recarrega os itens quando tabela e ponto de venda estão preenchidos', async () => {
    const wrapper = await mountComponent({ id: null })
    wrapper.vm.form.setValues({ idTabela: 10, idPontoVenda: 20 })
    api.get.mockClear()

    wrapper.vm.changeSalePoint({})
    await flushPromises()

    expect(api.get).toHaveBeenCalledWith(
      '/sale-item/list-by-price-table',
      expect.objectContaining({ params: { idTabelaPreco: 10, idPontoVenda: 20 } })
    )
  })

  it('não faz nada quando falta tabela ou ponto de venda', async () => {
    const wrapper = await mountComponent({ id: null })
    wrapper.vm.form.setValues({ idTabela: null, idPontoVenda: 20 })
    api.get.mockClear()

    wrapper.vm.changeSalePoint({})
    await flushPromises()

    expect(api.get).not.toHaveBeenCalled()
  })
})

// ==================================================================
// clear()
// ==================================================================
describe('clear', () => {
  it('reseta o formulário, o indicador de PJ e os itens', async () => {
    const wrapper = await mountComponent({ id: null })
    const resetSpy = vi.spyOn(wrapper.vm.form, 'reset')
    wrapper.vm.pj = true
    wrapper.vm.itens = [{ ...itemsFixture[0] }]

    wrapper.vm.clear()

    expect(resetSpy).toHaveBeenCalled()
    expect(wrapper.vm.pj).toBe(false)
    expect(wrapper.vm.itens).toEqual([])
  })
})

// ==================================================================
// formValidator (schema zod real)
// ------------------------------------------------------------------
// O <Form> é substituído por um stub que nunca chama o resolver de
// verdade (ver FormStub.submitWith), então os predicados dos .refine()
// do schema zod (linhas 25-27 do componente) nunca executam nos testes
// de save()/onMounted(). Aqui chamamos `formValidator` diretamente —
// ele é o resolver real (zodResolver + zod, ambos dependências reais
// do projeto, não mockadas) — para exercitar de fato a validação.
// ==================================================================
describe('formValidator', () => {
  const camposBase = {
    idCliente: null,
    razaoSocial: null,
    subTotal: null,
    desconto: null,
    total: null,
    observacoes: null
  }

  it('não acusa erro quando vendedor, tabela e ponto de venda são válidos', async () => {
    const wrapper = await mountComponent({ id: null })

    const result = await wrapper.vm.formValidator({
      values: { ...camposBase, idVendedor: 1, idTabela: 1, idPontoVenda: 1 }
    })

    expect(result.errors).toEqual({})
  })

  it('acusa erro em vendedor, tabela e ponto de venda quando não informados', async () => {
    const wrapper = await mountComponent({ id: null })

    const result = await wrapper.vm.formValidator({
      values: { ...camposBase, idVendedor: null, idTabela: null, idPontoVenda: null }
    })

    expect(result.errors.idVendedor[0].message).toBe('Preenchimento do Vendedor é obrigatório.')
    expect(result.errors.idTabela[0].message).toBe('Tabela de preços é de preenchimento obrigatório.')
    expect(result.errors.idPontoVenda[0].message).toBe('Ponto de Venda é de preenchimento obrigatório.')
  })
})

// ==================================================================
// Template — cobertura de branches de renderização
// ==================================================================
describe('Template', () => {
  it('mostra o botão de voltar apenas quando backEndpoint é informado', async () => {
    const semBackEndpoint = await mountComponent({ id: null })
    const comBackEndpoint = await mountComponent({ id: null, backEndpoint: '/core/sale' })

    const divsSemBack = semBackEndpoint.findAll('.flex.justify-end.items-center')
    const divsComBack = comBackEndpoint.findAll('.flex.justify-end.items-center')

    divsSemBack.forEach((div) => expect(div.isVisible()).toBe(false))
    divsComBack.forEach((div) => expect(div.isVisible()).toBe(true))
  })

  it('navega para o backEndpoint e para a rota fixa ao clicar nos botões de voltar', async () => {
    const wrapper = await mountComponent({ id: null, backEndpoint: '/core/sale-list' })

    const backButtons = wrapper.findAll('[icon="pi pi-replay"]')
    expect(backButtons.length).toBe(2)

    await backButtons[0].trigger('click')
    expect(routerPushMock).toHaveBeenCalledWith('/core/sale-list')

    await backButtons[1].trigger('click')
    expect(routerPushMock).toHaveBeenCalledWith('/core/sale')
  })

  it('alterna a largura do campo Cliente conforme o indicador de PJ', async () => {
    const wrapper = await mountComponent({ id: null })

    const wrapperDiv = () => wrapper.findAll('.grid.grid-cols-12.gap-2 > div').at(0)

    expect(wrapperDiv().classes()).toContain('col-span-12')

    wrapper.vm.pj = true
    await nextTick()

    expect(wrapperDiv().classes()).toContain('col-span-5')
  })

  it('exibe a mensagem de erro do campo quando ele está inválido', async () => {
    const wrapper = await mountComponent({ id: null })

    expect(wrapper.find('message-teststub').exists()).toBe(false)

    wrapper.vm.form.setFieldInvalid('idVendedor', 'Vendedor é obrigatório.')
    await nextTick()

    expect(wrapper.find('message-teststub').exists()).toBe(true)

    wrapper.vm.form.clearFieldInvalid('idVendedor')
    await nextTick()

    expect(wrapper.find('message-teststub').exists()).toBe(false)
  })

  it('exibe a mensagem de erro de todos os campos do formulário quando estão inválidos', async () => {
    const wrapper = await mountComponent({ id: null })
    const campos = ['idCliente', 'idVendedor', 'idTabela', 'idPontoVenda', 'subTotal', 'desconto', 'total', 'observacoes']

    campos.forEach((campo) => wrapper.vm.form.setFieldInvalid(campo, `${campo} inválido`))
    await nextTick()

    expect(wrapper.findAll('message-teststub').length).toBe(campos.length)
  })

  it('atribui a ação de submissão correta ao clicar em "Salvar & Nova" e "Salvar", nas duas seções da tela', async () => {
    const wrapper = await mountComponent({ id: null })

    const saveNewButtons = wrapper.findAll('[label="Salvar & Nova"]')
    const saveButtons = wrapper.findAll('[label="Salvar"]')
    // Uma dupla desses botões em cada um dos dois <Card> (Venda e Itens da Venda)
    expect(saveNewButtons.length).toBe(2)
    expect(saveButtons.length).toBe(2)

    await saveButtons[0].trigger('click')
    expect(wrapper.vm.submitAction).toBe('save')

    await saveNewButtons[0].trigger('click')
    expect(wrapper.vm.submitAction).toBe('saveNew')

    await saveButtons[1].trigger('click')
    expect(wrapper.vm.submitAction).toBe('save')

    await saveNewButtons[1].trigger('click')
    expect(wrapper.vm.submitAction).toBe('saveNew')
  })

  it('aciona os wrappers inline de @change dos Selects (cliente, vendedor, tabela, ponto de venda)', async () => {
    const wrapper = await mountComponent({ id: null })

    // @change="loadClient($event.value)"
    api.get.mockClear()
    wrapper.findComponent('#idCliente').vm.emit('change', { value: 30 })
    await flushPromises()
    expect(api.get).toHaveBeenCalledWith('/client', expect.objectContaining({ params: { id: 30 } }))

    // @change="changeSalesman($event)"
    api.get.mockClear()
    wrapper.findComponent('#idVendedor').vm.emit('change', { value: 9 })
    await flushPromises()
    expect(api.get).toHaveBeenCalledWith(
      '/user-price-table/list',
      expect.objectContaining({ params: expect.objectContaining({ idVendedor: 9 }) })
    )
    // changeSalesman já deixou idTabela/idPontoVenda preenchidos, então os
    // dois próximos @change encontram os campos necessários preenchidos.

    // @change="changePriceTable($event)"
    api.get.mockClear()
    wrapper.findComponent('#idTabela').vm.emit('change', {})
    await flushPromises()
    expect(api.get).toHaveBeenCalledWith('/sale-item/list-by-price-table', expect.anything())

    // @change="changeSalePoint($event)"
    api.get.mockClear()
    wrapper.findComponent('#idPontoVenda').vm.emit('change', {})
    await flushPromises()
    expect(api.get).toHaveBeenCalledWith('/sale-item/list-by-price-table', expect.anything())
  })

  it('renderiza o corpo das colunas de quantidade, preço unitário e total, e aciona os eventos de quantidade', async () => {
    const wrapper = await mountComponent({ id: null })
    const item = { ...itemsFixture[0] }
    wrapper.vm.itens = [item]
    await nextTick()

    expect(wrapper.findAll('cell-teststub').length).toBeGreaterThan(0)
    expect(formatNumber).toHaveBeenCalledWith(itemsFixture[0].precoUnitario)
    expect(formatNumber).toHaveBeenCalledWith(itemsFixture[0].total)

    // O único <input> dentro de um cell-teststub é o da coluna "Quantidade"
    // (as colunas de preço/total só têm texto) — os InputNumber do
    // formulário (subtotal/desconto/total) ficam fora dos cell-teststub.
    const quantidadeInput = wrapper.find('cell-teststub input')
    expect(quantidadeInput.exists()).toBe(true)

    await quantidadeInput.setValue(5)
    expect(item.quantidade).toBe(5)
    expect(item.total).toBe(5 * item.precoUnitario)

    await quantidadeInput.trigger('blur')
  })
})
