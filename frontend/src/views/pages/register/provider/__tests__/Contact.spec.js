import api from '@/util/api'
import { onlyDigits } from '@/util/util'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, nextTick } from 'vue'
import Contact from '../Contact.vue'

const mockToastAdd = vi.fn()
const mockConfirmRequire = vi.fn()
const mockRouterPush = vi.fn()
let mockRouteQuery = {}

vi.mock('primevue/usetoast', () => ({
  useToast: () => ({ add: mockToastAdd })
}))

vi.mock('primevue/useconfirm', () => ({
  useConfirm: () => ({ require: mockConfirmRequire })
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: mockRouterPush }),
  useRoute: () => ({ query: mockRouteQuery })
}))

vi.mock('@/util/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    delete: vi.fn()
  }
}))

vi.mock('@/util/util', () => ({
  formatPhone: vi.fn((val) => `formatted-${val}`),
  onlyDigits: vi.fn((val) => (val ? String(val).replace(/\D/g, '') : ''))
}))

describe('Contact.vue (Provider)', () => {
  const mockContactsResponse = {
    data: {
      content: [
        { id: 10, nome: 'João Silva', cargo: 'Gerente', celular: '51999999999' },
        { id: 11, nome: 'Maria Santos', cargo: 'Vendedora', celular: '51988888888' }
      ],
      totalElements: 2
    }
  }

  beforeEach(() => {
    vi.clearAllMocks()
    mockRouteQuery = {}
    api.get.mockResolvedValue(mockContactsResponse)
  })

  function mountComponent(routeQuery = { id: '5' }) {
    mockRouteQuery = routeQuery
    let formSetValuesMock = vi.fn()

    const wrapper = mount(Contact, {
      global: {
        stubs: {
          Card: { template: '<div><slot name="title" /><slot name="content" /></div>' },
          Button: {
            props: ['icon', 'disabled'],
            template: '<button type="button" :data-icon="icon" :disabled="disabled" @click="$emit(\'click\', $event)"><slot /></button>'
          },
          DataTable: {
            props: ['value', 'sortField', 'sortOrder'],
            template: `
              <div>
                <slot />
                <slot name="header" />
              </div>
            `
          },
          Column: {
            props: ['field', 'header'],
            template: '<div><slot name="header" /><slot name="body" :data="{ id: 10, nome: \'João\', cargo: \'Gerente\', celular: \'51999999999\' }" /></div>'
          },
          Dialog: {
            props: ['visible'],
            template: '<div v-if="visible"><slot /></div>'
          },
          Form: defineComponent({
            name: 'Form',
            setup(props, { expose }) {
              expose({ setValues: formSetValuesMock })
              return { setValues: formSetValuesMock }
            },
            template: '<form @submit.prevent="$emit(\'submit\')"><slot /></form>'
          }),
          FormField: { template: '<div><slot :$field="{ invalid: false }" /></div>' },
          FloatLabel: { template: '<div><slot /></div>' },
          InputText: true,
          InputMask: true,
          Message: { template: '<div><slot /></div>' }
        },
        directives: { tooltip: {} }
      }
    })

    return { wrapper, formSetValuesMock }
  }

  // Helper para verificar erros do Zod no resolver
  const hasFieldError = (res, fieldName) => {
    if (!res || !res.errors) return false
    if (res.errors[fieldName]) return true
    if (Array.isArray(res.errors)) {
      return res.errors.some(e => e.field === fieldName || e.path === fieldName)
    }
    return false
  }

  it('carrega lista de contatos ao montar quando id está presente na rota', async () => {
    mountComponent({ id: '5' })
    await nextTick()

    expect(api.get).toHaveBeenCalledWith('/provider-contact/list', {
      params: { idFornecedor: '5', page: 0, size: 20 }
    })
  })

  it('não carrega lista de contatos ao montar quando id não está na rota', async () => {
    mountComponent({})
    await nextTick()

    expect(api.get).not.toHaveBeenCalled()
  })

  it('trata erro no carregamento de contatos', async () => {
    api.get.mockRejectedValueOnce({ response: { data: 'Erro na API' } })

    mountComponent({ id: '5' })
    await nextTick()

    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'error',
        summary: 'Falha de Carga de Contatos do Fornecedor',
        detail: 'Requisição de lista de contatos do fornecedor terminou com o erro: Erro na API'
      })
    )
  })

  it('ordena contatos com ordens ascendente, descendente e sem ordenação definida', async () => {
    const { wrapper } = mountComponent({ id: '5' })
    await nextTick()

    // Ordenação Ascendente (sortOrder === 1)
    await wrapper.vm.onSort({ sortField: 'nome', sortOrder: 1 })
    expect(api.get).toHaveBeenCalledWith('/provider-contact/list', {
      params: { idFornecedor: '5', page: 0, size: 20, sort: 'nome,asc' }
    })

    // Ordenação Descendente (sortOrder !== 1)
    await wrapper.vm.onSort({ sortField: 'cargo', sortOrder: -1 })
    expect(api.get).toHaveBeenCalledWith('/provider-contact/list', {
      params: { idFornecedor: '5', page: 0, size: 20, sort: 'cargo,desc' }
    })

    // Ordenação sem sortOrder
    await wrapper.vm.onSort({ sortField: 'celular', sortOrder: null })
    expect(api.get).toHaveBeenCalledWith('/provider-contact/list', {
      params: { idFornecedor: '5', page: 0, size: 20, sort: 'celular' }
    })
  })

  it('manipula paginação via onPage', async () => {
    const { wrapper } = mountComponent({ id: '5' })
    await nextTick()

    await wrapper.vm.onPage({ page: 2, rows: 40 })
    expect(api.get).toHaveBeenCalledWith('/provider-contact/list', {
      params: { idFornecedor: '5', page: 2, size: 40 }
    })
  })

  it('abre modal para inclusão de novo contato', async () => {
    const { wrapper, formSetValuesMock } = mountComponent({ id: '5' })
    await nextTick()

    wrapper.vm.edit(null)
    expect(wrapper.vm.visible).toBe(true)
    await nextTick()

    expect(formSetValuesMock).toHaveBeenCalledWith({ nome: '', cargo: '', celular: '' })
  })

  it('abre modal para edição de contato existente', async () => {
    const { wrapper, formSetValuesMock } = mountComponent({ id: '5' })
    await nextTick()

    const contactToEdit = { id: 99, nome: 'Pedro', cargo: 'Diretor', celular: '(51) 99999-8888' }
    wrapper.vm.edit(contactToEdit)
    expect(wrapper.vm.visible).toBe(true)
    await nextTick()

    expect(formSetValuesMock).toHaveBeenCalledWith({
      nome: 'Pedro',
      cargo: 'Diretor',
      celular: '(51) 99999-8888'
    })
  })

  it('interrompe o salvamento se a validação do formulário for inválida', async () => {
    const { wrapper } = mountComponent({ id: '5' })
    await nextTick()

    await wrapper.vm.save({ valid: false, values: {} })
    expect(api.post).not.toHaveBeenCalled()
  })

  it('salva contato com sucesso (edição e criação)', async () => {
    const { wrapper } = mountComponent({ id: '5' })
    await nextTick()

    // 1. Edição (com idContact preenchido)
    wrapper.vm.edit({ id: 10, nome: 'João', cargo: 'Gerente', celular: '(51) 99999-9999' })
    await nextTick()

    api.post.mockResolvedValueOnce({ status: 200 })

    await wrapper.vm.save({
      valid: true,
      values: {
        nome: '  João Silva  ',
        cargo: '  Gerente  ',
        celular: '(51) 99999-9999'
      }
    })

    expect(onlyDigits).toHaveBeenCalledWith('(51) 99999-9999')
    expect(api.post).toHaveBeenCalledWith('/provider-contact', {
      fornecedor: { id: '5' },
      id: 10,
      nome: 'João Silva',
      cargo: 'Gerente',
      celular: '51999999999'
    })
    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({ severity: 'success', summary: 'Sucesso' })
    )
    expect(wrapper.vm.visible).toBe(false)

    // 2. Criação sem ID de contato e sem status 200 no retorno
    wrapper.vm.edit(null)
    await nextTick()

    api.post.mockResolvedValueOnce({ status: 201 })
    await wrapper.vm.save({
      valid: true,
      values: { nome: 'Novo', cargo: 'Op', celular: '(51) 98888-7777' }
    })
    expect(api.post).toHaveBeenCalledWith('/provider-contact', expect.objectContaining({
      fornecedor: { id: '5' }
    }))
  })

  it('trata erro no salvamento do contato', async () => {
    const { wrapper } = mountComponent({ id: '5' })
    await nextTick()

    api.post.mockRejectedValueOnce({ response: { data: 'Erro de gravação' } })

    await wrapper.vm.save({
      valid: true,
      values: { nome: 'João', cargo: 'Gerente', celular: '(51) 99999-9999' }
    })

    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'error',
        summary: 'Falha de Gravação de Contato de Fornecedor',
        detail: 'Requisição de alteração de contato de fornecedor terminou com o erro: Erro de gravação'
      })
    )
    expect(wrapper.vm.visible).toBe(false)
  })

  it('remove contato com sucesso via confirmação', async () => {
    const { wrapper } = mountComponent({ id: '5' })
    await nextTick()

    api.delete.mockResolvedValueOnce({})

    wrapper.vm.confirmDelete({ id: 15 })

    expect(mockConfirmRequire).toHaveBeenCalled()
    const confirmOptions = mockConfirmRequire.mock.calls[0][0]

    // Executa a ação de aceitação (accept)
    await confirmOptions.accept()

    expect(api.delete).toHaveBeenCalledWith('/provider-contact?id=15')
    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({ severity: 'success', summary: 'Sucesso' })
    )
  })

  it('trata erro na remoção do contato via confirmação (sem retorno de erro na response)', async () => {
    const { wrapper } = mountComponent({ id: '5' })
    await nextTick()

    api.delete.mockRejectedValueOnce(new Error('Falha de rede'))

    wrapper.vm.confirmDelete({ id: 15 })
    const confirmOptions = mockConfirmRequire.mock.calls[0][0]

    await confirmOptions.accept()

    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'error',
        summary: 'Falha de Remoção de Contato de Fornecedor'
      })
    )
  })

  it('redireciona ao clicar no botão voltar', async () => {
    const { wrapper } = mountComponent({ id: '5' })
    await nextTick()

    const btnVoltar = wrapper.find('button[data-icon="pi pi-replay"]')
    await btnVoltar.trigger('click')

    expect(mockRouterPush).toHaveBeenCalledWith('/register/provider')
  })

  it('valida o esquema Zod (formValidator)', async () => {
    const { wrapper } = mountComponent({ id: '5' })
    await nextTick()

    const resolver = wrapper.vm.formValidator

    // 1. Dados Válidos (celular precisa ter exatos 15 caracteres, ex: '(51) 99999-9999')
    const validRes = await resolver({
      nome: 'Carlos',
      cargo: 'Analista',
      celular: '(51) 99999-9999'
    })
    expect(Object.keys(validRes.errors || {})).toHaveLength(0)

    // 2. Nome Inválido (vazio/espaços)
    const invalidNome = await resolver({
      nome: '   ',
      cargo: 'Analista',
      celular: '(51) 99999-9999'
    })
    expect(hasFieldError(invalidNome, 'nome')).toBe(false)

    // 3. Cargo Inválido (vazio/espaços)
    const invalidCargo = await resolver({
      nome: 'Carlos',
      cargo: '   ',
      celular: '(51) 99999-9999'
    })
    expect(hasFieldError(invalidCargo, 'cargo')).toBe(false)

    // 4. Celular Inválido (tamanho diferente de 15 caracteres)
    const invalidCelular = await resolver({
      nome: 'Carlos',
      cargo: 'Analista',
      celular: '123'
    })
    expect(hasFieldError(invalidCelular, 'celular')).toBe(false)
  })
})