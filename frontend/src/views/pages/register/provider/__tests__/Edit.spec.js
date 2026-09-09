import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, h, nextTick } from 'vue'
import Edit from '../Edit.vue'

const mockToastAdd = vi.fn()
const mockConfirmRequire = vi.fn()
const mockRouterPush = vi.fn()
let mockRouteQueryId = '1'

vi.mock('primevue/usetoast', () => ({
  useToast: () => ({ add: mockToastAdd })
}))

vi.mock('primevue/useconfirm', () => ({
  useConfirm: () => ({ require: mockConfirmRequire })
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: mockRouterPush }),
  useRoute: () => ({ query: { get id() { return mockRouteQueryId } } })
}))

vi.mock('@/service/StateService', () => ({
  StateService: {
    getStates: vi.fn().mockResolvedValue([{ code: 'RS', name: 'Rio Grande do Sul' }])
  }
}))

vi.mock('@/util/util', () => ({
  formatPhone: (val) => val ? `TEL:${val}` : '',
  onlyDigits: (val) => val ? val.replace(/\D/g, '') : null
}))

vi.mock('@/util/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    delete: vi.fn()
  }
}))

import api from '@/util/api'

describe('Edit.vue - src/views/pages/register/provider/Edit.vue', () => {
  const mockProviderData = {
    razaoSocial: 'Fornecedor Teste LTDA',
    fantasia: 'Teste',
    cnpj: '12345678000195',
    fone: '11999998888',
    endereco: 'Rua A',
    bairro: 'Centro',
    cep: '90000000',
    cidade: 'Porto Alegre',
    uf: 'RS'
  }

  const mockContacts = [
    { id: 1, nome: 'João', cargo: 'Gerente', celular: '11988887777' }
  ]

  beforeEach(() => {
    vi.clearAllMocks()
    mockRouteQueryId = '1'

    api.get.mockImplementation((url) => {
      if (url === '/provider') {
        return Promise.resolve({ data: JSON.parse(JSON.stringify(mockProviderData)) })
      }
      if (url === '/provider-contact/list') {
        return Promise.resolve({
          data: {
            content: JSON.parse(JSON.stringify(mockContacts)),
            totalElements: 1
          }
        })
      }
      return Promise.resolve({ data: {} })
    })
  })

  function mountComponent() {
    return mount(Edit, {
      global: {
        stubs: {
          Card: {
            render() {
              return h('div', [this.$slots.title?.(), this.$slots.content?.()])
            }
          },
          DataTable: {
            props: ['value'],
            render() {
              return h('div', [
                this.$slots.default?.(),
                (this.value || []).map((item, index) =>
                  h('div', { key: index, class: 'data-table-row' }, [
                    this.$slots.default?.({ data: item })
                  ])
                )
              ])
            }
          },
          Column: {
            render() {
              return h('div', { class: 'column-stub' }, [
                this.$slots.header?.(),
                this.$slots.body?.({ data: { id: 1, nome: 'João', cargo: 'Gerente', celular: '11988887777' } })
              ])
            }
          },
          Button: {
            props: ['label', 'icon', 'disabled'],
            render() {
              return h('button', {
                type: 'button',
                'data-icon': this.icon,
                disabled: this.disabled,
                onClick: (e) => this.$emit('click', e)
              }, [this.label, this.$slots.default?.()])
            }
          },
          InputText: {
            props: ['modelValue'],
            render() {
              return h('input', {
                value: this.modelValue,
                onInput: (e) => this.$emit('update:modelValue', e.target.value)
              })
            }
          },
          InputMask: {
            props: ['modelValue'],
            render() {
              return h('input', {
                value: this.modelValue,
                onInput: (e) => this.$emit('update:modelValue', e.target.value)
              })
            }
          },
          Select: {
            render() { return h('select') }
          },
          Form: defineComponent({
            name: 'Form',
            methods: {
              setValues(vals) { this.internalValues = vals }
            },
            data() { return { internalValues: {} } },
            render() {
              return h('form', {
                onSubmit: (e) => {
                  e.preventDefault()
                  this.$emit('submit', { valid: true, values: this.internalValues })
                }
              }, [this.$slots.default?.()])
            }
          }),
          FormField: {
            render() {
              return h('div', [
                this.$slots.default?.({ value: '', invalid: false, error: null })
              ])
            }
          },
          FloatLabel: {
            render() { return h('div', [this.$slots.default?.()]) }
          },
          Message: {
            render() { return h('div', [this.$slots.default?.()]) }
          },
          Dialog: {
            props: ['visible'],
            render() {
              return this.visible ? h('div', { class: 'dialog-stub' }, [this.$slots.default?.()]) : null
            }
          },
          ConfirmDialog: true
        },
        directives: {
          tooltip: {}
        }
      }
    })
  }

  it('carrega dados do fornecedor e contatos com sucesso no onMounted quando há id', async () => {
    const wrapper = mountComponent()
    await nextTick()
    await nextTick()

    expect(api.get).toHaveBeenCalledWith('/provider', { params: { id: '1' } })
    expect(api.get).toHaveBeenCalledWith('/provider-contact/list', expect.any(Object))
    expect(wrapper.vm.data.length).toBe(1)
  })

  it('não chama load de fornecedor quando id é nulo/vazio (modo inserção)', async () => {
    mockRouteQueryId = undefined
    mountComponent()
    await nextTick()
    await nextTick()

    expect(api.get).not.toHaveBeenCalledWith('/provider', expect.any(Object))
  })

  it('trata erros de API ao carregar fornecedor e contatos', async () => {
    api.get.mockRejectedValueOnce({ response: { data: 'Erro fornecedor' } })
    api.get.mockRejectedValueOnce({ response: { data: 'Erro contatos' } })

    mountComponent()
    await nextTick()
    await nextTick()

    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Carga de Fornecedor' }))
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Carga de Contatos do Fornecedor' }))
  })

  it('trata erros de API apenas ao carregar contatos', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/provider') return Promise.resolve({ data: mockProviderData })
      if (url === '/provider-contact/list') return Promise.reject({ response: { data: 'Erro lista contatos' } })
      return Promise.resolve({ data: {} })
    })

    mountComponent()
    await nextTick()
    await nextTick()

    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Carga de Contatos do Fornecedor' }))
  })

  it('executa a paginação e ordenação de contatos (onPage e onSort com asc e desc)', async () => {
    const wrapper = mountComponent()
    await nextTick()

    wrapper.vm.onPage({ page: 1, rows: 40 })
    expect(wrapper.vm.page).toBe(1)
    expect(wrapper.vm.size).toBe(40)

    wrapper.vm.onSort({ sortField: 'nome', sortOrder: 1 })
    expect(wrapper.vm.sortField).toBe('nome')
    expect(wrapper.vm.sortOrder).toBe(1)
    expect(api.get).toHaveBeenLastCalledWith('/provider-contact/list', expect.objectContaining({
      params: expect.objectContaining({ sort: 'nome,asc' })
    }))

    wrapper.vm.onSort({ sortField: 'cargo', sortOrder: -1 })
    expect(api.get).toHaveBeenLastCalledWith('/provider-contact/list', expect.objectContaining({
      params: expect.objectContaining({ sort: 'cargo,desc' })
    }))
  })

  it('executa onSort sem sortOrder definido', async () => {
    const wrapper = mountComponent()
    await nextTick()

    wrapper.vm.sortOrder = null
    wrapper.vm.onSort({ sortField: 'nome', sortOrder: null })
    expect(api.get).toHaveBeenLastCalledWith('/provider-contact/list', expect.objectContaining({
      params: expect.objectContaining({ sort: 'nome' })
    }))
  })

  it('salva fornecedor com sucesso e falha', async () => {
    const wrapper = mountComponent()
    await nextTick()

    api.post.mockResolvedValueOnce({ status: 200, data: { id: 5 } })
    await wrapper.vm.save({ valid: true, values: { ...mockProviderData } })
    expect(api.post).toHaveBeenCalledWith('/provider', expect.any(Object))
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Sucesso' }))

    api.post.mockRejectedValueOnce({ response: { data: 'Erro ao salvar' } })
    await wrapper.vm.save({ valid: true, values: { ...mockProviderData } })
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Gravação de Fornecedor' }))

    // Validação inválida (deve ignorar)
    await wrapper.vm.save({ valid: false, values: {} })
  })

  it('gerencia abertura e salvamento de contato (novo e edição) com sucesso e falha', async () => {
    const wrapper = mountComponent()
    await nextTick()

    // Abre modo novo contato
    wrapper.vm.edit(null)
    expect(wrapper.vm.visible).toBe(true)

    // Abre modo edição de contato existente
    wrapper.vm.edit({ id: 10, nome: 'Carlos', cargo: 'Analista', celular: '11977776666' })
    expect(wrapper.vm.visible).toBe(true)

    // Salva contato com sucesso
    api.post.mockResolvedValueOnce({ status: 200 })
    await wrapper.vm.saveContact({ valid: true, values: { nome: 'Carlos', cargo: 'Analista', celular: '(11) 97777-6666' } })
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Sucesso', detail: 'Contato de Fornecedor atualizado com sucesso' }))

    // Salva contato com falha
    api.post.mockRejectedValueOnce({ response: { data: 'Erro contato' } })
    await wrapper.vm.saveContact({ valid: true, values: { nome: 'Carlos', cargo: 'Analista', celular: '(11) 97777-6666' } })
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Gravação de Contato de Fornecedor' }))

    // Validação inválida em saveContact
    await wrapper.vm.saveContact({ valid: false, values: {} })
  })

  it('exclui contato via confirmDelete com sucesso e tratamento de erro', async () => {
    const wrapper = mountComponent()
    await nextTick()

    const contactItem = { id: 1, nome: 'João' }
    wrapper.vm.confirmDelete(contactItem)
    expect(mockConfirmRequire).toHaveBeenCalled()

    const confirmArgs = mockConfirmRequire.mock.calls[0][0]

    // Sucesso na exclusão
    api.delete.mockResolvedValueOnce({})
    await confirmArgs.accept()
    expect(api.delete).toHaveBeenCalledWith('/provider-contact?id=1')
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Sucesso', detail: 'Contato de Fornecedor removido com sucesso' }))

    // Erro na exclusão
    wrapper.vm.confirmDelete(contactItem)
    api.delete.mockRejectedValueOnce({ response: { data: 'Erro exclusão' } })
    await mockConfirmRequire.mock.calls[1][0].accept()
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Remoção de Contato de Fornecedor' }))
  })

  it('interage com botões do template (voltar, limpar, salvar, novo contato, editar, remover e cancelar diálogo)', async () => {
    const wrapper = mountComponent()
    await nextTick()

    const buttons = wrapper.findAll('button')
    for (const btn of buttons) {
      await btn.trigger('click')
    }

    const form = wrapper.find('form')
    if (form.exists()) {
      await form.trigger('submit')
    }

    expect(mockRouterPush).toHaveBeenCalled()
  })
})