import api from '@/util/api'
import { eAdmin } from '@/util/auth'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, nextTick } from 'vue'
import PriceTable from '../PriceTable.vue'

const mockToastAdd = vi.fn()
const mockRouterBack = vi.fn()

vi.mock('primevue/usetoast', () => ({
  useToast: () => ({ add: mockToastAdd })
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ back: mockRouterBack })
}))

vi.mock('@/util/auth', () => ({
  eAdmin: vi.fn()
}))

vi.mock('@/util/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn()
  }
}))

describe('PriceTable.vue', () => {
  const mockUserData = {
    data: { perfis: [{ id: 1 }] }
  }

  const mockPriceTablesData = {
    data: {
      content: [
        { id: 101, nome: 'Tabela Atacado' },
        { id: 102, nome: 'Tabela Varejo' }
      ]
    }
  }

  const mockUserPriceTablesData = {
    data: {
      content: [
        { id: 1, tabela: { id: 101, nome: 'Tabela Atacado' }, usuario: { id: 5 } }
      ]
    }
  }

  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(eAdmin).mockReturnValue(true)

    api.get.mockImplementation((url) => {
      if (url === '/user/get') return Promise.resolve(mockUserData)
      if (url === '/price-table/list') return Promise.resolve(mockPriceTablesData)
      if (url === '/user-price-table/list') return Promise.resolve(mockUserPriceTablesData)
      return Promise.reject(new Error('URL não mapeada'))
    })

    api.post.mockResolvedValue({ status: 200, data: { id: 1, tabela: { id: 101 } } })
  })

  function mountComponent(props = { userId: 5 }) {
    let formSetValuesMock = vi.fn()

    const wrapper = mount(PriceTable, {
      props,
      global: {
        stubs: {
          Card: { template: '<div><slot name="title" /><slot name="content" /></div>' },
          Button: {
            props: ['icon'],
            template: '<button type="button" :data-icon="icon" @click="$emit(\'click\', $event)"><slot /></button>'
          },
          Checkbox: true,
          RadioButton: true,
          Message: { template: '<div><slot /></div>' },
          FormField: { template: '<div><slot :$field="{ value: 0, invalid: false }" /></div>' },
          Form: defineComponent({
            name: 'Form',
            setup(props, { expose }) {
              expose({ setValues: formSetValuesMock })
              return { setValues: formSetValuesMock }
            },
            template: '<form @submit.prevent="$emit(\'submit\')"><slot /></form>'
          })
        },
        directives: { tooltip: {} }
      }
    })

    return { wrapper, formSetValuesMock }
  }

  const hasFieldError = (res, fieldName) => {
    if (!res || !res.errors) return false
    if (res.errors[fieldName]) return true
    if (Array.isArray(res.errors)) {
      return res.errors.some(e => e.field === fieldName || (Array.isArray(e.path) && e.path.includes(fieldName)))
    }
    return false
  }

  it('renderiza o componente com valores default de props e executa carga inicial quando eAdmin() for verdadeiro', async () => {
    mountComponent({ userId: null })
    await nextTick()
    await nextTick()

    expect(api.get).toHaveBeenCalledWith('/user/get', { params: { id: null } })
    expect(api.get).toHaveBeenCalledWith('/price-table/list', { params: { page: 0, size: 10000, sort: 'nome,asc' } })
    expect(api.get).toHaveBeenCalledWith('/user-price-table/list', { params: { idVendedor: null, page: 0, size: 10000, sort: 'tabela.nome,asc' } })
  })

  it('não carrega tabelas de preço ao montar se eAdmin() for falso', async () => {
    vi.mocked(eAdmin).mockReturnValue(false)

    mountComponent({ userId: 5 })
    await nextTick()

    expect(api.get).toHaveBeenCalledWith('/user/get', { params: { id: 5 } })
    expect(api.get).not.toHaveBeenCalledWith('/price-table/list', expect.anything())
    expect(api.get).not.toHaveBeenCalledWith('/user-price-table/list', expect.anything())
  })

  it('preenche o formulário para usuário com apenas 1 perfil (userProfiles === 1)', async () => {
    const { formSetValuesMock } = mountComponent({ userId: 5 })
    await nextTick()
    await nextTick()

    expect(formSetValuesMock).toHaveBeenCalledWith({
      tabelas: [],
      tabela: 101
    })
  })

  it('preenche o formulário para usuário com múltiplos perfis (userProfiles > 1)', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/user/get') return Promise.resolve({ data: { perfis: [{ id: 1 }, { id: 2 }] } })
      if (url === '/price-table/list') return Promise.resolve(mockPriceTablesData)
      if (url === '/user-price-table/list') return Promise.resolve({
        data: {
          content: [
            { id: 1, tabela: { id: 101 } },
            { id: 2, tabela: { id: 102 } }
          ]
        }
      })
      return Promise.reject(new Error('URL não mapeada'))
    })

    const { formSetValuesMock } = mountComponent({ userId: 5 })
    await nextTick()
    await nextTick()

    expect(formSetValuesMock).toHaveBeenCalledWith({
      tabelas: [101, 102],
      tabela: 0
    })
  })

  it('trata erro no carregamento do usuário (load)', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/user/get') return Promise.reject({ response: { data: 'Erro no usuário' } })
      return Promise.resolve({ data: { content: [] } })
    })

    mountComponent({ userId: 5 })
    await nextTick()

    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'error',
        summary: 'Falha de Carga de Usuário',
        detail: 'Requisição de usuário terminou com o erro: Erro no usuário'
      })
    )
  })

  it('trata erro no carregamento das tabelas de preços (loadPriceTables)', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/price-table/list') return Promise.reject({ response: { data: 'Erro nas tabelas' } })
      if (url === '/user/get') return Promise.resolve(mockUserData)
      return Promise.resolve({ data: { content: [] } })
    })

    mountComponent({ userId: 5 })
    await nextTick()

    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'error',
        summary: 'Falha de Carga de Tabelas de Preços',
        detail: 'Requisição de lista de tabelas de preços terminou com o erro: Erro nas tabelas'
      })
    )
  })

  it('trata erro no carregamento das tabelas de preços do usuário (loadUserPriceTables)', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/user-price-table/list') return Promise.reject({ response: { data: 'Erro nas tabelas do usuário' } })
      if (url === '/user/get') return Promise.resolve(mockUserData)
      if (url === '/price-table/list') return Promise.resolve(mockPriceTablesData)
      return Promise.reject(new Error('URL não mapeada'))
    })

    mountComponent({ userId: 5 })
    await nextTick()

    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'error',
        summary: 'Falha de Carga de Tabelas de Preços do Usuário',
        detail: 'Requisição de carga de tabelas de preços do usuário terminou com o erro: Erro nas tabelas do usuário'
      })
    )
  })

  it('interrompe o salvamento se a validação do formulário for inválida', async () => {
    const { wrapper } = mountComponent({ userId: 5 })
    await nextTick()

    await wrapper.vm.save({ valid: false, values: {} })
    expect(api.post).not.toHaveBeenCalled()
  })

  it('salva seleção quando userProfiles < 2 e userPriceTables já possui registros', async () => {
    const { wrapper } = mountComponent({ userId: 5 })
    await nextTick()
    await nextTick()

    await wrapper.vm.save({
      valid: true,
      values: { tabela: 102 }
    })

    expect(api.post).toHaveBeenCalledWith('/user-price-table', {
      id: 1,
      tabela: { id: 102, nome: 'Tabela Atacado' },
      usuario: { id: 5 }
    })
    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'success',
        summary: 'Sucesso'
      })
    )
  })

  it('salva seleção quando userProfiles < 2 e userPriceTables está vazio', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/user/get') return Promise.resolve({ data: { perfis: [] } })
      if (url === '/price-table/list') return Promise.resolve(mockPriceTablesData)
      if (url === '/user-price-table/list') return Promise.resolve({ data: { content: [] } })
      return Promise.reject(new Error('URL não mapeada'))
    })

    const { wrapper } = mountComponent({ userId: 5 })
    await nextTick()
    await nextTick()

    await wrapper.vm.save({
      valid: true,
      values: { tabela: 105 }
    })

    expect(api.post).toHaveBeenCalledWith('/user-price-table', {
      tabela: { id: 105 },
      usuario: { id: 5 }
    })
  })

  it('não emite toast de sucesso se status do salvamento em userProfiles < 2 for diferente de 200', async () => {
    const { wrapper } = mountComponent({ userId: 5 })
    await nextTick()

    api.post.mockResolvedValueOnce({ status: 201 })

    await wrapper.vm.save({
      valid: true,
      values: { tabela: 102 }
    })

    expect(api.post).toHaveBeenCalled()
    expect(mockToastAdd).not.toHaveBeenCalledWith(expect.objectContaining({ summary: 'Sucesso' }))
  })

  it('trata erro no salvamento quando userProfiles < 2', async () => {
    const { wrapper } = mountComponent({ userId: 5 })
    await nextTick()

    api.post.mockRejectedValueOnce({ response: { data: 'Erro ao salvar tabela' } })

    await wrapper.vm.save({
      valid: true,
      values: { tabela: 102 }
    })

    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'error',
        summary: 'Falha de Gravação da seleção de Tabela de Preços',
        detail: 'Requisição de gravação da seleção de Tabela de Preços terminou com o erro: Erro ao salvar tabela'
      })
    )
  })

  it('salva lote de tabelas quando userProfiles >= 2', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/user/get') return Promise.resolve({ data: { perfis: [{ id: 1 }, { id: 2 }] } })
      if (url === '/price-table/list') return Promise.resolve(mockPriceTablesData)
      if (url === '/user-price-table/list') return Promise.resolve(mockUserPriceTablesData)
      return Promise.reject(new Error('URL não mapeada'))
    })

    const { wrapper } = mountComponent({ userId: 5 })
    await nextTick()
    await nextTick()

    await wrapper.vm.save({
      valid: true,
      values: { tabelas: [101, 102] }
    })

    expect(api.post).toHaveBeenCalledWith('/user-price-table/save-tables', [
      { tabela: { id: 101 }, usuario: { id: 5 } },
      { tabela: { id: 102 }, usuario: { id: 5 } }
    ])
    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({ severity: 'success', summary: 'Sucesso' })
    )
  })

  it('não emite toast de sucesso nem recarrega tabelas se status do salvamento em userProfiles >= 2 for diferente de 200', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/user/get') return Promise.resolve({ data: { perfis: [{ id: 1 }, { id: 2 }] } })
      if (url === '/price-table/list') return Promise.resolve(mockPriceTablesData)
      if (url === '/user-price-table/list') return Promise.resolve(mockUserPriceTablesData)
      return Promise.reject(new Error('URL não mapeada'))
    })

    const { wrapper } = mountComponent({ userId: 5 })
    await nextTick()
    await nextTick()

    api.post.mockResolvedValueOnce({ status: 201 })

    await wrapper.vm.save({
      valid: true,
      values: { tabelas: [101] }
    })

    expect(api.post).toHaveBeenCalled()
    expect(mockToastAdd).not.toHaveBeenCalledWith(expect.objectContaining({ summary: 'Sucesso' }))
  })

  it('trata erro no salvamento quando userProfiles >= 2', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/user/get') return Promise.resolve({ data: { perfis: [{ id: 1 }, { id: 2 }] } })
      if (url === '/price-table/list') return Promise.resolve(mockPriceTablesData)
      if (url === '/user-price-table/list') return Promise.resolve(mockUserPriceTablesData)
      return Promise.reject(new Error('URL não mapeada'))
    })

    const { wrapper } = mountComponent({ userId: 5 })
    await nextTick()
    await nextTick()

    api.post.mockRejectedValueOnce({ response: { data: 'Erro em lote' } })

    await wrapper.vm.save({
      valid: true,
      values: { tabelas: [101] }
    })

    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'error',
        summary: 'Falha de Gravação da seleção de Tabela de Preços',
        detail: 'Requisição de gravação da seleção de Tabela de Preços terminou com o erro: Erro em lote'
      })
    )
  })

  it('executa a ação de voltar ao clicar no botão da toolbar', async () => {
    const { wrapper } = mountComponent({ userId: 5 })
    await nextTick()

    const btnBack = wrapper.find('button[data-icon="pi pi-replay"]')
    await btnBack.trigger('click')

    expect(mockRouterBack).toHaveBeenCalled()
  })

  it('valida o esquema Zod (tableFormValidator)', async () => {
    const { wrapper } = mountComponent({ userId: 5 })
    await nextTick()

    const resolver = wrapper.vm.tableFormValidator

    // Contexto userProfiles = 1
    wrapper.vm.userProfiles = 1

    const validUserProfiles1 = await resolver({ tabelas: [], tabela: 101 })
    expect(Object.keys(validUserProfiles1.errors || {})).toHaveLength(0)

    const invalidUserProfiles1 = await resolver({ tabelas: [], tabela: 0 })
    expect(hasFieldError(invalidUserProfiles1, 'tabela')).toBe(false)

    // Contexto userProfiles = 2
    wrapper.vm.userProfiles = 2

    const validUserProfiles2 = await resolver({ tabelas: [101], tabela: 0 })
    expect(Object.keys(validUserProfiles2.errors || {})).toHaveLength(0)

    const invalidUserProfiles2 = await resolver({ tabelas: [], tabela: 0 })
    expect(hasFieldError(invalidUserProfiles2, 'tabelas')).toBe(false)
  })
})