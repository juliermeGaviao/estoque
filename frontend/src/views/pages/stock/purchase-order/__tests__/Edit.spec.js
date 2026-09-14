import api from '@/util/api'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, nextTick } from 'vue'
import Edit from '../Edit.vue'
import { createPurchaseOrderSchema } from '../purchaseOrderSchema'

const mockToastAdd = vi.fn()

vi.mock('primevue/usetoast', () => ({
  useToast: () => ({ add: mockToastAdd })
}))

vi.mock('@/util/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn()
  }
}))

vi.mock('@/util/util', () => ({
  formatDate: vi.fn((value) => (value ? '2026-09-14' : value)),
  toDate: vi.fn((value) => (typeof value === 'string' ? new Date(value) : value))
}))

describe('Edit.vue - src/views/pages/purchase-order/Edit.vue', () => {
  const mockProviders = [
    { id: 201, fantasia: 'Fornecedor A' },
    { id: 202, fantasia: 'Fornecedor B' }
  ]
  const mockProducts = [
    { id: 301, nome: 'Produto 1', referencia: 'REF1', peso: 100, estoque: 10 },
    { id: 302, nome: 'Produto 2', referencia: 'REF2', peso: 200, estoque: 5 }
  ]

  beforeEach(() => {
    vi.clearAllMocks()
    api.get.mockImplementation((url) => {
      if (url === '/provider/list') {
        return Promise.resolve({ data: { content: JSON.parse(JSON.stringify(mockProviders)) } })
      }
      if (url === '/purchase-order/list-products') {
        return Promise.resolve({ data: JSON.parse(JSON.stringify(mockProducts)) })
      }
      if (url === '/purchase-order/list-purchase-order-products') {
        return Promise.resolve({ data: JSON.parse(JSON.stringify(mockProducts)) })
      }
      if (url === '/purchase-order/find-by-order-number') {
        return Promise.resolve({ status: 200, data: [] })
      }
      return Promise.resolve({ data: {} })
    })
    api.post.mockResolvedValue({ status: 200 })
  })

  function mountComponent(stubOverrides = {}) {
    let formSetValuesMock = vi.fn()
    let formResetMock = vi.fn()
    const wrapper = mount(Edit, {
      global: {
        stubs: {
          Dialog: defineComponent({
            name: 'Dialog',
            props: ['visible'],
            template: '<div v-if="visible" class="dialog-stub"><slot /></div>'
          }),
          Form: defineComponent({
            name: 'Form',
            props: ['resolver', 'initialValues'],
            setup(props, { expose }) {
              expose({ setValues: formSetValuesMock, reset: formResetMock })
              return { setValues: formSetValuesMock, reset: formResetMock }
            },
            template: '<form @submit.prevent="$emit(\'submit\', { valid: true, values: { numeroPedido: \'PC-001\', idFornecedor: 201, dataPedido: new Date(\'2026-09-14\') } })"><slot /></form>'
          }),
          FormField: { template: '<div><slot :value="0" :invalid="false" :error="{ message: \'\' }" /></div>' },
          FloatLabel: { template: '<div><slot /></div>' },
          InputText: { name: 'InputText', template: '<input />' },
          Select: { name: 'Select', props: ['options', 'optionLabel', 'optionValue', 'disabled'], template: '<select><slot /></select>' },
          DatePicker: { name: 'DatePicker', props: ['disabled'], template: '<input type="date" />' },
          Button: {
            props: ['label', 'icon', 'disabled'],
            inheritAttrs: false,
            template: '<button type="button" :data-icon="icon" :disabled="disabled" @click="$emit(\'click\', $event)">{{ label }}<slot /></button>'
          },
          Message: { template: '<div><slot /></div>' },
          DataTable: defineComponent({
            name: 'DataTable',
            props: ['value'],
            provide() {
              const self = this
              return {
                dataTableValue: {
                  get value() { return self.value }
                }
              }
            },
            template: '<div class="datatable-stub"><slot /></div>'
          }),
          Column: defineComponent({
            name: 'Column',
            props: ['field'],
            inject: ['dataTableValue'],
            computed: {
              rows() {
                return this.dataTableValue?.value ?? []
              }
            },
            template: `
              <div class="column-stub">
                <slot name="header" />
                <div v-for="(item, index) in rows" :key="index" class="column-body-row">
                  <slot name="body" :data="item" />
                </div>
              </div>
            `
          }),
          InputNumber: {
            name: 'InputNumber',
            props: ['modelValue'],
            template: '<input class="inputnumber-stub" :value="modelValue" @input="$emit(\'update:modelValue\', Number($event.target.value))" />'
          },
          ...stubOverrides
        },
        directives: { tooltip: {} }
      }
    })
    return { wrapper, formSetValuesMock, formResetMock }
  }

  it('carrega fornecedores no onMounted com sucesso', async () => {
    const { wrapper } = mountComponent()
    await nextTick()
    await nextTick()
    expect(api.get).toHaveBeenCalledWith('/provider/list', expect.objectContaining({ params: expect.objectContaining({ sort: 'fantasia,asc' }) }))
    expect(wrapper.vm.fornecedores.length).toBe(2)
  })

  it('trata erro ao carregar os fornecedores (loadProviders)', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/provider/list') return Promise.reject({ response: { data: 'Erro fornecedores' } })
      return Promise.resolve({ data: {} })
    })
    mountComponent()
    await nextTick()
    await nextTick()
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Carga de Fornecedores' }))
  })

  it('newOrder abre o dialog em modo criar e toggle fecha e reseta o form', async () => {
    const { wrapper, formResetMock } = mountComponent()
    await nextTick()
    wrapper.vm.newOrder()
    await nextTick()
    expect(wrapper.vm.action).toBe('criar')
    expect(wrapper.vm.visible).toBe(true)
    expect(wrapper.find('.dialog-stub').exists()).toBe(true)
    wrapper.vm.toggle()
    await nextTick()
    expect(wrapper.vm.visible).toBe(false)
    expect(formResetMock).toHaveBeenCalled()
  })

  it('view preenche o formulário e carrega produtos do pedido', async () => {
    const { wrapper, formSetValuesMock } = mountComponent()
    await nextTick()
    const order = { id: 1, numeroPedido: 'PC-001', fornecedor: { id: 201 }, dataPedido: '2026-09-10T12:00:00.000Z' }
    await wrapper.vm.view(order)
    await nextTick()
    expect(wrapper.vm.action).toBe('visualizar')
    expect(wrapper.vm.visible).toBe(true)
    expect(formSetValuesMock).toHaveBeenCalledWith({
      numeroPedido: 'PC-001',
      idFornecedor: 201,
      dataPedido: '2026-09-14'
    })
    expect(api.get).toHaveBeenCalledWith('/purchase-order/list-purchase-order-products', { params: { idPedidoCompra: 1 } })
    expect(wrapper.vm.products.length).toBe(2)
    expect(wrapper.vm.products[0].quantidade).toBe(0)
  })

  it('providerChange carrega produtos do fornecedor selecionado', async () => {
    const { wrapper } = mountComponent()
    await nextTick()
    await wrapper.vm.providerChange({ value: 201 })
    expect(api.get).toHaveBeenCalledWith('/purchase-order/list-products', { params: { idFornecedor: 201 } })
    expect(wrapper.vm.products.length).toBe(2)
    expect(wrapper.vm.products[0].quantidade).toBe(0)
  })

  it('trata erro ao carregar produtos do fornecedor (loadProviderProducts)', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/purchase-order/list-products') return Promise.reject({ response: { data: 'Erro produtos' } })
      return Promise.resolve({ data: {} })
    })
    const { wrapper } = mountComponent()
    await nextTick()
    await wrapper.vm.loadProviderProducts(201)
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Carga de Produtos' }))
  })

  it('trata erro ao carregar produtos do pedido (loadPurchaseOrderProducts)', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/purchase-order/list-purchase-order-products') return Promise.reject({ response: { data: 'Erro produtos pedido' } })
      return Promise.resolve({ data: {} })
    })
    const { wrapper } = mountComponent()
    await nextTick()
    await wrapper.vm.loadPurchaseOrderProducts(1)
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Carga de Produtos' }))
  })

  it('interrompe o salvamento quando o formulário é inválido', async () => {
    const { wrapper } = mountComponent()
    await nextTick()
    await wrapper.vm.save({ valid: false, values: {} })
    expect(api.post).not.toHaveBeenCalled()
    expect(api.get).not.toHaveBeenCalledWith('/purchase-order/find-by-order-number', expect.anything())
  })

  it('bloqueia o salvamento quando nenhum produto tem quantidade diferente de zero', async () => {
    const { wrapper } = mountComponent()
    await nextTick()
    await wrapper.vm.save({ valid: true, values: { numeroPedido: 'PC-003', idFornecedor: 201, dataPedido: new Date('2026-09-14') } })
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Pedido de Compra' }))
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ detail: 'Algum produto deve ter valor diferente de zero.' }))
    expect(api.post).not.toHaveBeenCalled()
  })

  it('bloqueia o salvamento quando já existe pedido com o mesmo número', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/purchase-order/find-by-order-number') return Promise.resolve({ status: 200, data: [{ id: 99 }] })
      if (url === '/purchase-order/list-products') return Promise.resolve({ data: JSON.parse(JSON.stringify(mockProducts)) })
      return Promise.resolve({ data: {} })
    })
    const { wrapper } = mountComponent()
    await nextTick()
    await wrapper.vm.loadProviderProducts(201)
    wrapper.vm.products[0].quantidade = 5
    await wrapper.vm.save({ valid: true, values: { numeroPedido: 'PC-001', idFornecedor: 201, dataPedido: new Date('2026-09-14') } })
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ detail: 'Já existe outro pedido com o mesmo número.' }))
    expect(api.post).not.toHaveBeenCalled()
  })

  it('trata erro na verificação de duplicidade e salva mesmo assim', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/purchase-order/find-by-order-number') return Promise.reject({ response: { data: 'Erro verificação' } })
      if (url === '/purchase-order/list-products') return Promise.resolve({ data: JSON.parse(JSON.stringify(mockProducts)) })
      return Promise.resolve({ data: {} })
    })
    const { wrapper } = mountComponent()
    await nextTick()
    await wrapper.vm.loadProviderProducts(201)
    wrapper.vm.products[0].quantidade = 5
    await wrapper.vm.save({ valid: true, values: { numeroPedido: 'PC-003', idFornecedor: 201, dataPedido: new Date('2026-09-14') } })
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Verificação de Pedido' }))
    expect(api.post).toHaveBeenCalled()
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Sucesso' }))
  })

  it('salva o pedido com sucesso, emite saved e fecha o dialog', async () => {
    const { wrapper, formResetMock } = mountComponent()
    await nextTick()
    wrapper.vm.newOrder()
    await nextTick()
    await wrapper.vm.loadProviderProducts(201)
    wrapper.vm.products[0].quantidade = 5
    wrapper.vm.products[1].quantidade = 3
    await wrapper.vm.save({ valid: true, values: { numeroPedido: 'PC-003', idFornecedor: 201, dataPedido: new Date('2026-09-14') } })
    expect(api.post).toHaveBeenCalledWith('/purchase-order', {
      numeroPedido: 'PC-003',
      idFornecedor: 201,
      dataPedido: expect.any(Date),
      fornecedor: { id: 201 },
      estoque: [
        { idProduto: 301, quantidade: 5 },
        { idProduto: 302, quantidade: 3 }
      ]
    })
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Sucesso' }))
    expect(wrapper.emitted('saved')).toHaveLength(1)
    expect(wrapper.vm.visible).toBe(false)
    expect(formResetMock).toHaveBeenCalled()
  })

  it('não exibe toast de sucesso quando o status da gravação difere de 200', async () => {
    const { wrapper } = mountComponent()
    await nextTick()
    wrapper.vm.newOrder()
    await nextTick()
    await wrapper.vm.loadProviderProducts(201)
    wrapper.vm.products[0].quantidade = 2
    api.post.mockResolvedValueOnce({ status: 201 })
    await wrapper.vm.save({ valid: true, values: { numeroPedido: 'PC-004', idFornecedor: 201, dataPedido: new Date('2026-09-14') } })
    expect(api.post).toHaveBeenCalled()
    expect(mockToastAdd).not.toHaveBeenCalledWith(expect.objectContaining({ summary: 'Sucesso' }))
    expect(wrapper.emitted('saved')).toHaveLength(1)
    expect(wrapper.vm.visible).toBe(false)
  })

  it('trata erro na gravação do pedido de compra', async () => {
    const { wrapper } = mountComponent()
    await nextTick()
    wrapper.vm.newOrder()
    await nextTick()
    await wrapper.vm.loadProviderProducts(201)
    wrapper.vm.products[0].quantidade = 2
    api.post.mockRejectedValueOnce({ response: { data: 'Erro ao salvar pedido' } })
    await wrapper.vm.save({ valid: true, values: { numeroPedido: 'PC-004', idFornecedor: 201, dataPedido: new Date('2026-09-14') } })
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Gravação do Pedido de Compra' }))
    expect(wrapper.emitted('saved')).toHaveLength(1)
    expect(wrapper.vm.visible).toBe(false)
  })

  it('submete o formulário via template (cobre o handler @submit de save)', async () => {
    const { wrapper } = mountComponent()
    await nextTick()
    wrapper.vm.newOrder()
    await nextTick()
    await wrapper.vm.loadProviderProducts(201)
    wrapper.vm.products[0].quantidade = 3
    const form = wrapper.find('form')
    await form.trigger('submit')
    await nextTick()
    expect(api.post).toHaveBeenCalledWith('/purchase-order', expect.objectContaining({
      estoque: [{ idProduto: 301, quantidade: 3 }]
    }))
  })

  it('dispara o change do Select (cobre o handler @change de providerChange)', async () => {
    const { wrapper } = mountComponent()
    await nextTick()
    wrapper.vm.newOrder()
    await nextTick()
    const select = wrapper.findComponent({ name: 'Select' })
    await select.vm.$emit('change', { value: 202 })
    await nextTick()
    expect(api.get).toHaveBeenCalledWith('/purchase-order/list-products', { params: { idFornecedor: 202 } })
  })

  it('interage com os botões Cancelar e Fechar do template', async () => {
    const { wrapper } = mountComponent()
    await nextTick()
    wrapper.vm.newOrder()
    await nextTick()
    const cancelar = wrapper.findAll('button').find(b => b.text().includes('Cancelar'))
    await cancelar.trigger('click')
    await nextTick()
    expect(wrapper.vm.visible).toBe(false)
    await wrapper.vm.view({ id: 1, numeroPedido: 'PC-001', fornecedor: { id: 201 }, dataPedido: '2026-09-10T12:00:00.000Z' })
    await nextTick()
    const fechar = wrapper.findAll('button').find(b => b.text().includes('Fechar'))
    await fechar.trigger('click')
    await nextTick()
    expect(wrapper.vm.visible).toBe(false)
  })

  it('exibe mensagens de erro quando os campos são inválidos (cobre v-if do Message)', async () => {
    const { wrapper } = mountComponent({
      FormField: {
        template: '<div><slot :value="0" :invalid="true" :error="{ message: \'Campo obrigatório\' }" /></div>'
      }
    })
    await nextTick()
    wrapper.vm.newOrder()
    await nextTick()
    expect(wrapper.text()).toContain('Campo obrigatório')
  })

  it('dispara os handlers v-model do Dialog e do InputNumber', async () => {
    const { wrapper } = mountComponent()
    await nextTick()
    wrapper.vm.newOrder()
    await nextTick()
    await wrapper.vm.loadProviderProducts(201)
    await nextTick()
    const dialog = wrapper.findComponent({ name: 'Dialog' })
    await dialog.vm.$emit('update:visible', false)
    expect(wrapper.vm.visible).toBe(false)
    wrapper.vm.visible = true
    await nextTick()
    const inputNumber = wrapper.findComponent({ name: 'InputNumber' })
    expect(inputNumber.exists()).toBe(true)
    await inputNumber.vm.$emit('update:modelValue', 7)
    expect(wrapper.vm.products[0].quantidade).toBe(7)
  })

  it('valida o schema do pedido via safeParse (cobre refine, branches e o getter de action)', () => {
    const criar = createPurchaseOrderSchema(() => 'criar')
    expect(criar.safeParse({ numeroPedido: 'PC-001', idFornecedor: 201, dataPedido: new Date('2026-09-14') }).success).toBe(true)
    const nulo = criar.safeParse({ numeroPedido: null, idFornecedor: 201, dataPedido: new Date('2026-09-14') })
    expect(nulo.success).toBe(false)
    expect(nulo.error.issues.some(i => i.path[0] === 'numeroPedido')).toBe(true)
    expect(criar.safeParse({ numeroPedido: '   ', idFornecedor: 201, dataPedido: new Date('2026-09-14') }).success).toBe(false)
    const semFornecedor = criar.safeParse({ numeroPedido: 'PC-001', dataPedido: new Date('2026-09-14') })
    expect(semFornecedor.success).toBe(false)
    expect(semFornecedor.error.issues.some(i => i.path[0] === 'idFornecedor')).toBe(true)
    const semData = criar.safeParse({ numeroPedido: 'PC-001', idFornecedor: 201, dataPedido: null })
    expect(semData.success).toBe(false)
    expect(semData.error.issues.some(i => i.path[0] === 'dataPedido')).toBe(true)
    const visualizar = createPurchaseOrderSchema(() => 'visualizar')
    expect(visualizar.safeParse({ numeroPedido: 'PC-001', idFornecedor: 201, dataPedido: null }).success).toBe(true)
    const { wrapper } = mountComponent()
    expect(wrapper.vm.formSchema.safeParse({ numeroPedido: 'PC-001', idFornecedor: 201, dataPedido: new Date('2026-09-14') }).success).toBe(true)
  })
})