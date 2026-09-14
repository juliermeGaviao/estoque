import api from '@/util/api'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, nextTick } from 'vue'
import Edit from '../Edit.vue'
import { createStockTransferSchema } from '../stockTransferSchema'

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

describe('Edit.vue - src/views/pages/stock/stock-transfer/Edit.vue', () => {
  const mockPontos = [
    { id: 201, nome: 'PDV Central' },
    { id: 202, nome: 'PDV Filial Norte' }
  ]
  const mockProducts = [
    { id: 301, nome: 'Produto 1', referencia: 'REF1', peso: 100, estoque: 10, estoqueDestino: 0 },
    { id: 302, nome: 'Produto 2', referencia: 'REF2', peso: 200, estoque: 5, estoqueDestino: 0 }
  ]

  beforeEach(() => {
    vi.clearAllMocks()
    api.get.mockImplementation((url) => {
      if (url === '/sale-point/list') {
        return Promise.resolve({ data: { content: JSON.parse(JSON.stringify(mockPontos)) } })
      }
      if (url === '/stock-transfer/list-products') {
        return Promise.resolve({ data: JSON.parse(JSON.stringify(mockProducts)) })
      }
      if (url === '/stock-transfer/list-sale-point-products') {
        return Promise.resolve({ data: JSON.parse(JSON.stringify(mockProducts)) })
      }
      return Promise.resolve({ data: {} })
    })
    api.post.mockResolvedValue({ status: 200 })
  })

  function mountComponent(stubOverrides = {}) {
    let formSetValuesMock = vi.fn()
    let formResetMock = vi.fn()
    let formSetFieldValueMock = vi.fn()
    const formStates = {
      idPontoVendaOrigem: { value: null },
      idPontoVendaDestino: { value: null }
    }
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
              expose({
                setValues: formSetValuesMock,
                reset: formResetMock,
                setFieldValue: formSetFieldValueMock,
                states: formStates
              })
              return {
                setValues: formSetValuesMock,
                reset: formResetMock,
                setFieldValue: formSetFieldValueMock,
                states: formStates
              }
            },
            template: '<form @submit.prevent="$emit(\'submit\', { valid: true, values: { idPontoVendaOrigem: 201, idPontoVendaDestino: 202, dataTransferencia: \'14/09/2026\' } })"><slot /></form>'
          }),
          FormField: { template: '<div><slot :value="0" :invalid="false" :error="{ message: \'\' }" /></div>' },
          FloatLabel: { template: '<div><slot /></div>' },
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
    return { wrapper, formSetValuesMock, formResetMock, formSetFieldValueMock, formStates }
  }

  it('carrega pontos de venda no onMounted com sucesso', async () => {
    const { wrapper } = mountComponent()
    await nextTick()
    await nextTick()
    expect(api.get).toHaveBeenCalledWith('/sale-point/list', expect.objectContaining({ params: expect.objectContaining({ sort: 'id,asc' }) }))
    expect(wrapper.vm.pontos.length).toBe(2)
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

  it('newStockTransfer abre o dialog em modo criar e seta a data via setFieldValue', async () => {
    const { wrapper, formSetFieldValueMock } = mountComponent()
    await nextTick()
    wrapper.vm.newStockTransfer()
    await nextTick()
    expect(wrapper.vm.action).toBe('criar')
    expect(wrapper.vm.visible).toBe(true)
    expect(wrapper.find('.dialog-stub').exists()).toBe(true)
    expect(formSetFieldValueMock).toHaveBeenCalledWith('dataTransferencia', '2026-09-14')
  })

  it('toggle fecha o dialog, limpa products e reseta o form', async () => {
    const { wrapper, formResetMock } = mountComponent()
    await nextTick()
    wrapper.vm.newStockTransfer()
    await nextTick()
    wrapper.vm.toggle()
    await nextTick()
    expect(wrapper.vm.visible).toBe(false)
    expect(wrapper.vm.products.length).toBe(0)
    expect(formResetMock).toHaveBeenCalled()
  })

  it('view preenche o formulário e carrega produtos da transferência', async () => {
    const { wrapper, formSetValuesMock } = mountComponent()
    await nextTick()
    const transfer = { id: 1, pontoVendaOrigem: { id: 201 }, pontoVendaDestino: { id: 202 }, dataTransferencia: '2026-09-10T12:00:00.000Z' }
    await wrapper.vm.view(transfer)
    await nextTick()
    expect(wrapper.vm.action).toBe('visualizar')
    expect(wrapper.vm.visible).toBe(true)
    expect(formSetValuesMock).toHaveBeenCalledWith({
      idPontoVendaOrigem: 201,
      idPontoVendaDestino: 202,
      dataTransferencia: '2026-09-14'
    })
    expect(api.get).toHaveBeenCalledWith('/stock-transfer/list-sale-point-products', { params: { idTransferenciaEstoque: 1 } })
    expect(wrapper.vm.products.length).toBe(2)
    expect(wrapper.vm.products[0].quantidade).toBe(0)
  })

  it('salePointChange retorna cedo quando o form não está montado', async () => {
    const { wrapper } = mountComponent()
    await nextTick()
    await wrapper.vm.salePointChange({ value: 201 })
    expect(api.get).not.toHaveBeenCalledWith('/stock-transfer/list-products', expect.anything())
  })

  it('salePointChange carrega produtos quando origem e destino são válidos e diferentes', async () => {
    const { wrapper, formStates } = mountComponent()
    await nextTick()
    wrapper.vm.newStockTransfer()
    await nextTick()
    formStates.idPontoVendaOrigem.value = 201
    formStates.idPontoVendaDestino.value = 202
    await wrapper.vm.salePointChange({ value: 202 })
    expect(api.get).toHaveBeenCalledWith('/stock-transfer/list-products', {
      params: expect.objectContaining({ idPontoVendaOrigem: 201, idPontoVendaDestingo: 202 })
    })
    expect(wrapper.vm.products.length).toBe(2)
    expect(wrapper.vm.products[0].quantidade).toBe(0)
  })

  it('salePointChange limpa products quando origem e destino são iguais', async () => {
    const { wrapper, formStates } = mountComponent()
    await nextTick()
    wrapper.vm.newStockTransfer()
    await nextTick()
    formStates.idPontoVendaOrigem.value = 201
    formStates.idPontoVendaDestino.value = 201
    await wrapper.vm.salePointChange({ value: 201 })
    expect(wrapper.vm.products.length).toBe(0)
    expect(api.get).not.toHaveBeenCalledWith('/stock-transfer/list-products', expect.anything())
  })

  it('salePointChange limpa products quando origem está ausente', async () => {
    const { wrapper, formStates } = mountComponent()
    await nextTick()
    wrapper.vm.newStockTransfer()
    await nextTick()
    formStates.idPontoVendaOrigem.value = null
    formStates.idPontoVendaDestino.value = 202
    await wrapper.vm.salePointChange({ value: 202 })
    expect(wrapper.vm.products.length).toBe(0)
  })

  it('trata erro ao carregar produtos do ponto de venda (loadSalePointProducts)', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/stock-transfer/list-products') return Promise.reject({ response: { data: 'Erro produtos' } })
      return Promise.resolve({ data: {} })
    })
    const { wrapper } = mountComponent()
    await nextTick()
    await wrapper.vm.loadSalePointProducts(201, 202)
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Carga de Produtos' }))
  })

  it('trata erro ao carregar produtos da transferência (loadStockTransferProducts)', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/stock-transfer/list-sale-point-products') return Promise.reject({ response: { data: 'Erro produtos transferência' } })
      return Promise.resolve({ data: {} })
    })
    const { wrapper } = mountComponent()
    await nextTick()
    await wrapper.vm.loadStockTransferProducts(1)
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Carga de Produtos' }))
  })

  it('interrompe o salvamento quando o formulário é inválido', async () => {
    const { wrapper } = mountComponent()
    await nextTick()
    await wrapper.vm.save({ valid: false, values: {} })
    expect(api.post).not.toHaveBeenCalled()
  })

  it('bloqueia o salvamento quando nenhum produto tem quantidade diferente de zero', async () => {
    const { wrapper } = mountComponent()
    await nextTick()
    await wrapper.vm.save({ valid: true, values: { idPontoVendaOrigem: 201, idPontoVendaDestino: 202, dataTransferencia: '14/09/2026' } })
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Transferência de Estoque' }))
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ detail: 'Algum produto deve ter valor diferente de zero para transferir.' }))
    expect(api.post).not.toHaveBeenCalled()
  })

  it('salva a transferência com sucesso, converte a data, emite saved e fecha o dialog', async () => {
    const { wrapper, formResetMock } = mountComponent()
    await nextTick()
    wrapper.vm.newStockTransfer()
    await nextTick()
    await wrapper.vm.loadSalePointProducts(201, 202)
    wrapper.vm.products[0].quantidade = 5
    wrapper.vm.products[1].quantidade = 3
    await wrapper.vm.save({ valid: true, values: { idPontoVendaOrigem: 201, idPontoVendaDestino: 202, dataTransferencia: '14/09/2026' } })
    expect(api.post).toHaveBeenCalledWith('/stock-transfer', {
      idPontoVendaOrigem: 201,
      idPontoVendaDestino: 202,
      dataTransferencia: '2026-09-14',
      pontoVendaOrigem: { id: 201 },
      pontoVendaDestino: { id: 202 },
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
    wrapper.vm.newStockTransfer()
    await nextTick()
    await wrapper.vm.loadSalePointProducts(201, 202)
    wrapper.vm.products[0].quantidade = 2
    api.post.mockResolvedValueOnce({ status: 201 })
    await wrapper.vm.save({ valid: true, values: { idPontoVendaOrigem: 201, idPontoVendaDestino: 202, dataTransferencia: '14/09/2026' } })
    expect(api.post).toHaveBeenCalled()
    expect(mockToastAdd).not.toHaveBeenCalledWith(expect.objectContaining({ summary: 'Sucesso' }))
    expect(wrapper.emitted('saved')).toHaveLength(1)
    expect(wrapper.vm.visible).toBe(false)
  })

  it('trata erro na gravação da transferência', async () => {
    const { wrapper } = mountComponent()
    await nextTick()
    wrapper.vm.newStockTransfer()
    await nextTick()
    await wrapper.vm.loadSalePointProducts(201, 202)
    wrapper.vm.products[0].quantidade = 2
    api.post.mockRejectedValueOnce({ response: { data: 'Erro ao salvar transferência' } })
    await wrapper.vm.save({ valid: true, values: { idPontoVendaOrigem: 201, idPontoVendaDestino: 202, dataTransferencia: '14/09/2026' } })
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Gravação do Transferência de Estoque' }))
    expect(wrapper.emitted('saved')).toHaveLength(1)
    expect(wrapper.vm.visible).toBe(false)
  })

  it('submete o formulário via template (cobre o handler @submit de save)', async () => {
    const { wrapper } = mountComponent()
    await nextTick()
    wrapper.vm.newStockTransfer()
    await nextTick()
    await wrapper.vm.loadSalePointProducts(201, 202)
    wrapper.vm.products[0].quantidade = 3
    const form = wrapper.find('form')
    await form.trigger('submit')
    await nextTick()
    expect(api.post).toHaveBeenCalledWith('/stock-transfer', expect.objectContaining({
      estoque: [{ idProduto: 301, quantidade: 3 }]
    }))
  })

  it('dispara o change do Select (cobre o handler @change de salePointChange)', async () => {
    const { wrapper, formStates } = mountComponent()
    await nextTick()
    wrapper.vm.newStockTransfer()
    await nextTick()
    formStates.idPontoVendaOrigem.value = 201
    formStates.idPontoVendaDestino.value = 202
    const selects = wrapper.findAllComponents({ name: 'Select' })
    await selects[0].vm.$emit('change', { value: 201 })
    await nextTick()
    expect(api.get).toHaveBeenCalledWith('/stock-transfer/list-products', expect.anything())
  })

  it('interage com os botões Cancelar e Fechar do template', async () => {
    const { wrapper } = mountComponent()
    await nextTick()
    wrapper.vm.newStockTransfer()
    await nextTick()
    const cancelar = wrapper.findAll('button').find(b => b.text().includes('Cancelar'))
    await cancelar.trigger('click')
    await nextTick()
    expect(wrapper.vm.visible).toBe(false)
    await wrapper.vm.view({ id: 1, pontoVendaOrigem: { id: 201 }, pontoVendaDestino: { id: 202 }, dataTransferencia: '2026-09-10T12:00:00.000Z' })
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
    wrapper.vm.newStockTransfer()
    await nextTick()
    expect(wrapper.text()).toContain('Campo obrigatório')
  })

  it('dispara os handlers v-model do Dialog e do InputNumber', async () => {
    const { wrapper } = mountComponent()
    await nextTick()
    wrapper.vm.newStockTransfer()
    await nextTick()
    await wrapper.vm.loadSalePointProducts(201, 202)
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

  it('valida o schema da transferência via safeParse (cobre refine e branches)', () => {
    const schema = createStockTransferSchema()
    expect(schema.safeParse({ idPontoVendaOrigem: 201, idPontoVendaDestino: 202, dataTransferencia: '2026-09-14' }).success).toBe(true)
    const semOrigem = schema.safeParse({ idPontoVendaOrigem: null, idPontoVendaDestino: 202 })
    expect(semOrigem.success).toBe(false)
    expect(semOrigem.error.issues.some(i => i.path[0] === 'idPontoVendaOrigem')).toBe(true)
    const semDestino = schema.safeParse({ idPontoVendaOrigem: 201, idPontoVendaDestino: null })
    expect(semDestino.success).toBe(false)
    expect(semDestino.error.issues.some(i => i.path[0] === 'idPontoVendaDestino')).toBe(true)
    const semData = schema.safeParse({ idPontoVendaOrigem: 201, idPontoVendaDestino: 202 })
    expect(semData.success).toBe(true)
    const { wrapper } = mountComponent()
    expect(wrapper.vm.stockTransferFormSchema.safeParse({ idPontoVendaOrigem: 201, idPontoVendaDestino: 202, dataTransferencia: '2026-09-14' }).success).toBe(true)
  })
})