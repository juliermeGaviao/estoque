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

describe('List.vue - src/views/pages/stock/stock-transfer/List.vue (refatorado)', () => {
  const mockTransferList = [
    {
      id: 1,
      pontoVendaOrigem: { id: 201, nome: 'PDV Central' },
      pontoVendaDestino: { id: 202, nome: 'PDV Filial Norte' },
      dataTransferencia: '2026-09-10T12:00:00.000Z'
    },
    {
      id: 2,
      pontoVendaOrigem: { id: 202, nome: 'PDV Filial Norte' },
      pontoVendaDestino: { id: 201, nome: 'PDV Central' },
      dataTransferencia: '2026-09-11T12:00:00.000Z'
    }
  ]
  const mockPontos = [
    { id: 201, nome: 'PDV Central' },
    { id: 202, nome: 'PDV Filial Norte' }
  ]

  beforeEach(() => {
    vi.clearAllMocks()
    api.get.mockImplementation((url) => {
      if (url === '/stock-transfer/list') {
        return Promise.resolve({ data: { content: JSON.parse(JSON.stringify(mockTransferList)), totalElements: 2 } })
      }
      if (url === '/sale-point/list') {
        return Promise.resolve({ data: { content: JSON.parse(JSON.stringify(mockPontos)) } })
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
            template: '<form @submit.prevent="$emit(\'submit\', { valid: true, values: { idPontoVendaOrigem: 201, idPontoVendaDestino: 202, minDataTransferencia: new Date(\'2026-09-01\'), maxDataTransferencia: new Date(\'2026-09-30\') } })" @reset.prevent="$emit(\'reset\')"><slot /></form>'
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
              const newStockTransfer = vi.fn()
              const view = vi.fn()
              expose({ newStockTransfer, view })
              return () => h('div', { class: 'edit-stub' })
            }
          })
        },
        directives: { tooltip: {} }
      }
    })
  }

  it('carrega transferências e pontos de venda no onMounted com sucesso', async () => {
    const wrapper = mountComponent()
    await nextTick()
    await nextTick()
    expect(api.get).toHaveBeenCalledWith('/stock-transfer/list', { params: expect.objectContaining({ page: 0, size: 20 }) })
    expect(api.get).toHaveBeenCalledWith('/sale-point/list', expect.objectContaining({ params: expect.objectContaining({ sort: 'id,asc' }) }))
    expect(wrapper.vm.data.length).toBe(2)
    expect(wrapper.vm.pontos.length).toBe(2)
  })

  it('trata erro ao carregar a lista de transferências no onMounted', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/stock-transfer/list') return Promise.reject({ response: { data: 'Erro na API' } })
      if (url === '/sale-point/list') return Promise.resolve({ data: { content: [] } })
      return Promise.resolve({ data: {} })
    })
    mountComponent()
    await nextTick()
    await nextTick()
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Carga de Transferências de Estoque' }))
  })

  it('trata erro ao carregar os pontos de venda (loadSalePoints)', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/sale-point/list') return Promise.reject({ response: { data: 'Erro pontos' } })
      return Promise.resolve({ data: {} })
    })
    mountComponent()
    await nextTick()
    await nextTick()
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Carga de Pontos de Venda' }))
  })

  it('executa filtro válido com datas e inválido sem chamar a API', async () => {
    const wrapper = mountComponent()
    await nextTick()
    await wrapper.vm.filter({
      valid: true,
      values: { idPontoVendaOrigem: 201, idPontoVendaDestino: 202, minDataTransferencia: new Date('2026-09-01'), maxDataTransferencia: new Date('2026-09-30') }
    })
    expect(wrapper.vm.page).toBe(0)
    expect(api.get).toHaveBeenLastCalledWith('/stock-transfer/list', {
      params: expect.objectContaining({
        page: 0,
        idPontoVendaOrigem: 201,
        idPontoVendaDestino: 202,
        minDataTransferencia: '2026-09-14',
        maxDataTransferencia: '2026-09-14'
      })
    })
    api.get.mockClear()
    await wrapper.vm.filter({ valid: false, values: {} })
    expect(api.get).not.toHaveBeenCalled()
  })

  it('executa limpar resetando página, filtros e ordenação', async () => {
    const wrapper = mountComponent()
    await nextTick()
    wrapper.vm.sortField = 'dataTransferencia'
    wrapper.vm.page = 3
    wrapper.vm.filterValues = { idPontoVendaOrigem: 201 }
    wrapper.vm.limpar()
    await nextTick()
    expect(wrapper.vm.page).toBe(0)
    expect(wrapper.vm.sortField).toBeNull()
    expect(wrapper.vm.filterValues).toEqual({ idPontoVendaOrigem: null, idPontoVendaDestino: null, minDataTransferencia: null, maxDataTransferencia: null })
    expect(api.get).toHaveBeenLastCalledWith('/stock-transfer/list', expect.anything())
  })

  it('executa onPage atualizando página e tamanho e recarregando com filtros', async () => {
    const wrapper = mountComponent()
    await nextTick()
    await wrapper.vm.onPage({ page: 2, rows: 40 })
    expect(wrapper.vm.page).toBe(2)
    expect(wrapper.vm.size).toBe(40)
    expect(api.get).toHaveBeenLastCalledWith('/stock-transfer/list', { params: expect.objectContaining({ page: 2, size: 40 }) })
  })

  it('executa onSort com asc, desc e sem sortOrder (fallback)', async () => {
    const wrapper = mountComponent()
    await nextTick()
    await wrapper.vm.onSort({ sortField: 'dataTransferencia', sortOrder: 1 })
    expect(api.get).toHaveBeenLastCalledWith('/stock-transfer/list', { params: expect.objectContaining({ sort: 'dataTransferencia,asc' }) })
    await wrapper.vm.onSort({ sortField: 'dataTransferencia', sortOrder: -1 })
    expect(api.get).toHaveBeenLastCalledWith('/stock-transfer/list', { params: expect.objectContaining({ sort: 'dataTransferencia,desc' }) })
    wrapper.vm.sortField = 'dataTransferencia'
    wrapper.vm.sortOrder = null
    await wrapper.vm.load({})
    expect(api.get).toHaveBeenLastCalledWith('/stock-transfer/list', { params: expect.objectContaining({ sort: 'dataTransferencia' }) })
  })

  it('interage com o formulário de filtro via template (submit e reset)', async () => {
    const wrapper = mountComponent()
    await nextTick()
    const form = wrapper.find('form')
    await form.trigger('submit')
    expect(wrapper.vm.filterValues.idPontoVendaOrigem).toBe(201)
    expect(wrapper.vm.page).toBe(0)
    await form.trigger('reset')
    await nextTick()
    expect(wrapper.vm.sortField).toBeNull()
    expect(wrapper.vm.filterValues).toEqual({ idPontoVendaOrigem: null, idPontoVendaDestino: null, minDataTransferencia: null, maxDataTransferencia: null })
  })

  it('newStockTransfer chama o método exposto do Edit (pai -> filho)', async () => {
    const wrapper = mountComponent()
    await nextTick()
    const editStub = wrapper.findComponent({ name: 'Edit' })
    wrapper.vm.newStockTransfer()
    expect(editStub.vm.newStockTransfer).toHaveBeenCalledTimes(1)
  })

  it('view chama o método exposto do Edit com o registro (pai -> filho)', async () => {
    const wrapper = mountComponent()
    await nextTick()
    const editStub = wrapper.findComponent({ name: 'Edit' })
    const transfer = wrapper.vm.data[0]
    wrapper.vm.view(transfer)
    expect(editStub.vm.view).toHaveBeenCalledWith(transfer)
  })

  it('reloadList recarrega a lista quando o Edit emite saved (filho -> pai)', async () => {
    const wrapper = mountComponent()
    await nextTick()
    api.get.mockClear()
    const editStub = wrapper.findComponent({ name: 'Edit' })
    await editStub.vm.$emit('saved')
    expect(api.get).toHaveBeenCalledWith('/stock-transfer/list', expect.anything())
  })

  it('interage com os botões do template: nova transferência e visualizar', async () => {
    const wrapper = mountComponent()
    await nextTick()
    await nextTick()
    const editStub = wrapper.findComponent({ name: 'Edit' })
    const plusButton = wrapper.find('button[data-icon="pi pi-plus"]')
    await plusButton.trigger('click')
    expect(editStub.vm.newStockTransfer).toHaveBeenCalledTimes(1)
    const eyeButton = wrapper.find('button[data-icon="pi pi-eye"]')
    await eyeButton.trigger('click')
    expect(editStub.vm.view).toHaveBeenCalledTimes(1)
    expect(editStub.vm.view).toHaveBeenCalledWith(expect.objectContaining({ id: 1 }))
  })

  it('renderiza a data formatada no body da coluna dataTransferencia', async () => {
    const wrapper = mountComponent()
    await nextTick()
    await nextTick()
    expect(wrapper.text()).toContain('2026-09-14')
  })
})