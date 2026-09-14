import api from '@/util/api'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, h, nextTick } from 'vue'
import List from '../List.vue'

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
  formatDate: vi.fn((value) => (value ? '2026-09-14' : value))
}))

describe('List.vue - src/views/pages/purchase-order/List.vue (refatorado)', () => {
  const mockOrderList = [
    { id: 1, numeroPedido: 'PC-001', fornecedor: { id: 201, fantasia: 'Fornecedor A' }, dataPedido: '2026-09-10T12:00:00.000Z' },
    { id: 2, numeroPedido: 'PC-002', fornecedor: { id: 202, fantasia: 'Fornecedor B' }, dataPedido: '2026-09-11T12:00:00.000Z' }
  ]
  const mockProviders = [
    { id: 201, fantasia: 'Fornecedor A' },
    { id: 202, fantasia: 'Fornecedor B' }
  ]

  beforeEach(() => {
    vi.clearAllMocks()
    api.get.mockImplementation((url) => {
      if (url === '/purchase-order/list') {
        return Promise.resolve({ data: { content: JSON.parse(JSON.stringify(mockOrderList)), totalElements: 2 } })
      }
      if (url === '/provider/list') {
        return Promise.resolve({ data: { content: JSON.parse(JSON.stringify(mockProviders)) } })
      }
      return Promise.resolve({ data: {} })
    })
  })

  function mountComponent() {
    return mount(List, {
      global: {
        stubs: {
          ConfirmDialog: true,
          Card: { template: '<div><slot name="title" /><slot name="content" /></div>' },
          Form: defineComponent({
            name: 'Form',
            template: '<form @submit.prevent="$emit(\'submit\', { valid: true, values: { numeroPedido: \'PC-001\', idFornecedor: 201, minDataPedido: new Date(\'2026-09-01\'), maxDataPedido: new Date(\'2026-09-30\') } })" @reset.prevent="$emit(\'reset\')"><slot /></form>'
          }),
          FormField: { template: '<div><slot /></div>' },
          FloatLabel: { template: '<div><slot /></div>' },
          InputText: { name: 'InputText', template: '<input />' },
          Select: { name: 'Select', template: '<select><slot /></select>' },
          DatePicker: { name: 'DatePicker', template: '<input type="date" />' },
          Button: {
            props: ['label', 'icon', 'disabled'],
            inheritAttrs: false,
            template: '<button type="button" :data-icon="icon" :disabled="disabled" @click="$emit(\'click\', $event)">{{ label }}<slot /></button>'
          },
          DataTable: defineComponent({
            name: 'DataTable',
            props: ['value', 'first', 'sortField', 'sortOrder'],
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
          Edit: defineComponent({
            name: 'Edit',
            setup(props, { expose }) {
              const newOrder = vi.fn()
              const view = vi.fn()
              expose({ newOrder, view })
              return () => h('div', { class: 'edit-stub' })
            }
          })
        },
        directives: { tooltip: {} }
      }
    })
  }

  it('carrega pedidos e fornecedores no onMounted com sucesso', async () => {
    const wrapper = mountComponent()
    await nextTick()
    await nextTick()
    expect(api.get).toHaveBeenCalledWith('/purchase-order/list', { params: expect.objectContaining({ page: 0, size: 20 }) })
    expect(api.get).toHaveBeenCalledWith('/provider/list', expect.objectContaining({ params: expect.objectContaining({ sort: 'fantasia,asc' }) }))
    expect(wrapper.vm.data.length).toBe(2)
    expect(wrapper.vm.fornecedores.length).toBe(2)
  })

  it('trata erro ao carregar a lista de pedidos no onMounted', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/purchase-order/list') return Promise.reject({ response: { data: 'Erro na API' } })
      if (url === '/provider/list') return Promise.resolve({ data: { content: [] } })
      return Promise.resolve({ data: {} })
    })
    mountComponent()
    await nextTick()
    await nextTick()
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Carga de Pedidos de Compra' }))
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

  it('executa filtro válido com datas e inválido sem chamar a API', async () => {
    const wrapper = mountComponent()
    await nextTick()
    await wrapper.vm.filter({
      valid: true,
      values: { numeroPedido: 'PC-001', idFornecedor: 201, minDataPedido: new Date('2026-09-01'), maxDataPedido: new Date('2026-09-30') }
    })
    expect(wrapper.vm.page).toBe(0)
    expect(api.get).toHaveBeenLastCalledWith('/purchase-order/list', {
      params: expect.objectContaining({
        page: 0,
        numeroPedido: 'PC-001',
        idFornecedor: 201,
        minDataPedido: '2026-09-14',
        maxDataPedido: '2026-09-14'
      })
    })
    api.get.mockClear()
    await wrapper.vm.filter({ valid: false, values: {} })
    expect(api.get).not.toHaveBeenCalled()
  })

  it('executa limpar resetando página, filtros e ordenação', async () => {
    const wrapper = mountComponent()
    await nextTick()
    wrapper.vm.sortField = 'numeroPedido'
    wrapper.vm.page = 3
    wrapper.vm.filterValues = { numeroPedido: 'X' }
    wrapper.vm.limpar()
    await nextTick()
    expect(wrapper.vm.page).toBe(0)
    expect(wrapper.vm.sortField).toBeNull()
    expect(wrapper.vm.filterValues).toEqual({ numeroPedido: null, idFornecedor: null, minDataPedido: null, maxDataPedido: null })
    expect(api.get).toHaveBeenLastCalledWith('/purchase-order/list', expect.anything())
  })

  it('executa onPage atualizando página e tamanho e recarregando com filtros', async () => {
    const wrapper = mountComponent()
    await nextTick()
    await wrapper.vm.onPage({ page: 2, rows: 40 })
    expect(wrapper.vm.page).toBe(2)
    expect(wrapper.vm.size).toBe(40)
    expect(api.get).toHaveBeenLastCalledWith('/purchase-order/list', { params: expect.objectContaining({ page: 2, size: 40 }) })
  })

  it('executa onSort com asc, desc e sem sortOrder (fallback)', async () => {
    const wrapper = mountComponent()
    await nextTick()
    await wrapper.vm.onSort({ sortField: 'numeroPedido', sortOrder: 1 })
    expect(api.get).toHaveBeenLastCalledWith('/purchase-order/list', { params: expect.objectContaining({ sort: 'numeroPedido,asc' }) })
    await wrapper.vm.onSort({ sortField: 'numeroPedido', sortOrder: -1 })
    expect(api.get).toHaveBeenLastCalledWith('/purchase-order/list', { params: expect.objectContaining({ sort: 'numeroPedido,desc' }) })
    wrapper.vm.sortField = 'numeroPedido'
    wrapper.vm.sortOrder = null
    await wrapper.vm.load({})
    expect(api.get).toHaveBeenLastCalledWith('/purchase-order/list', { params: expect.objectContaining({ sort: 'numeroPedido' }) })
  })

  it('interage com o formulário de filtro via template (submit e reset)', async () => {
    const wrapper = mountComponent()
    await nextTick()
    const form = wrapper.find('form')
    await form.trigger('submit')
    expect(wrapper.vm.filterValues.numeroPedido).toBe('PC-001')
    expect(wrapper.vm.page).toBe(0)
    await form.trigger('reset')
    await nextTick()
    expect(wrapper.vm.sortField).toBeNull()
    expect(wrapper.vm.filterValues).toEqual({ numeroPedido: null, idFornecedor: null, minDataPedido: null, maxDataPedido: null })
  })

  it('newOrder chama o método exposto do Edit (pai -> filho)', async () => {
    const wrapper = mountComponent()
    await nextTick()
    const editStub = wrapper.findComponent({ name: 'Edit' })
    wrapper.vm.newOrder()
    expect(editStub.vm.newOrder).toHaveBeenCalledTimes(1)
  })

  it('viewOrder chama o método exposto do Edit com o pedido (pai -> filho)', async () => {
    const wrapper = mountComponent()
    await nextTick()
    const editStub = wrapper.findComponent({ name: 'Edit' })
    const order = wrapper.vm.data[0]
    wrapper.vm.viewOrder(order)
    expect(editStub.vm.view).toHaveBeenCalledWith(order)
  })

  it('reloadList recarrega a lista quando o Edit emite saved (filho -> pai)', async () => {
    const wrapper = mountComponent()
    await nextTick()
    api.get.mockClear()
    const editStub = wrapper.findComponent({ name: 'Edit' })
    await editStub.vm.$emit('saved')
    expect(api.get).toHaveBeenCalledWith('/purchase-order/list', expect.anything())
  })

  it('interage com os botões do template: novo pedido e visualizar', async () => {
    const wrapper = mountComponent()
    await nextTick()
    await nextTick()
    const editStub = wrapper.findComponent({ name: 'Edit' })
    const plusButton = wrapper.find('button[data-icon="pi pi-plus"]')
    await plusButton.trigger('click')
    expect(editStub.vm.newOrder).toHaveBeenCalledTimes(1)
    const eyeButton = wrapper.find('button[data-icon="pi pi-eye"]')
    await eyeButton.trigger('click')
    expect(editStub.vm.view).toHaveBeenCalledTimes(1)
    expect(editStub.vm.view).toHaveBeenCalledWith(expect.objectContaining({ id: 1 }))
  })

  it('renderiza a data formatada no body da coluna dataPedido', async () => {
    const wrapper = mountComponent()
    await nextTick()
    await nextTick()
    expect(wrapper.text()).toContain('2026-09-14')
  })
})