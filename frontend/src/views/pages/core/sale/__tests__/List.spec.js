import api from '@/util/api'
import * as auth from '@/util/auth'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'
import List from '../List.vue'

const mockToastAdd = vi.fn()
const mockConfirmRequire = vi.fn()
const mockRouterPush = vi.fn()

vi.mock('primevue/usetoast', () => ({
  useToast: () => ({ add: mockToastAdd })
}))

vi.mock('primevue/useconfirm', () => ({
  useConfirm: () => ({ require: mockConfirmRequire })
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: mockRouterPush })
}))

vi.mock('@/util/api', () => ({
  default: {
    get: vi.fn(),
    delete: vi.fn()
  }
}))

vi.mock('@/util/auth', () => ({
  eAdmin: vi.fn(),
  getUserId: vi.fn()
}))

// Mock do util para garantir execução explicita das funções de formatação do template
vi.mock('@/util/util', () => ({
  formatNumber: vi.fn((val) => `R$ ${val}`)
}))

describe('sale/List.vue', () => {
  const mockSalesResponse = {
    data: {
      content: [
        { id: 1, subTotal: 100, desconto: 10, total: 90, cliente: { nome: 'Cliente 1' }, vendedor: { email: 'vendedor@test.com' }, pontoVenda: { nome: 'PDV 1' } }
      ],
      totalElements: 1
    }
  }

  const mockUsersResponse = {
    data: {
      content: [{ id: 10, email: 'vendedor@test.com' }]
    }
  }

  const mockClientsResponse = {
    data: [{ id: 100, nome: 'Cliente Teste' }]
  }

  beforeEach(() => {
    vi.clearAllMocks()
    auth.eAdmin.mockReturnValue(true)
    auth.getUserId.mockReturnValue('user-123')

    api.get.mockImplementation((url) => {
      if (url === '/sale/list') return Promise.resolve(mockSalesResponse)
      if (url === '/user/list') return Promise.resolve(mockUsersResponse)
      if (url === '/client/find-all') return Promise.resolve(mockClientsResponse)
      return Promise.reject(new Error('URL não mapeada'))
    })
  })

  function mountComponent() {
    return mount(List, {
      global: {
        stubs: {
          Card: {
            template: '<div><slot name="title" /><slot name="content" /></div>'
          },
          DataTable: {
            props: ['value'],
            template: `
              <div>
                <slot />
                <div v-for="(item, index) in value" :key="index" class="data-table-row">
                  <slot name="default" :data="item" />
                </div>
              </div>
            `
          },
          Column: {
            props: ['field'],
            template: `
              <div class="column-stub">
                <slot name="header" />
                <slot name="body" :data="{ id: 1, subTotal: 100, desconto: 10, total: 90, cliente: { nome: 'Cliente 1' } }" />
              </div>
            `
          },
          Button: {
            template: '<button type="button" @click="$emit(\'click\')"><slot /></button>'
          },
          InputText: true,
          InputNumber: true,
          Select: true,
          ConfirmDialog: true,
          FloatLabel: {
            template: '<div><slot /></div>'
          },
          Form: {
            template: '<form @submit.prevent="$emit(\'submit\')" @reset="$emit(\'reset\')"><slot /></form>'
          },
          FormField: {
            template: '<div><slot /></div>'
          }
        },
        directives: {
          tooltip: {}
        }
      }
    })
  }

  it('carrega dados no onMounted para usuário administrador', async () => {
    const wrapper = mountComponent()
    await nextTick()

    expect(api.get).toHaveBeenCalledWith('/sale/list', {
      params: { page: 0, size: 20, sort: 'id,desc' }
    })
    expect(api.get).toHaveBeenCalledWith('/user/list', {
      params: { page: 0, size: 10000, sort: 'email,asc' }
    })
    expect(api.get).toHaveBeenCalledWith('/client/find-all')
    expect(wrapper.exists()).toBe(true)
  })

  it('injeta idVendedor na busca quando usuário NÃO for administrador', async () => {
    auth.eAdmin.mockReturnValue(false)

    mountComponent()
    await nextTick()

    expect(api.get).toHaveBeenCalledWith('/sale/list', {
      params: { page: 0, size: 20, sort: 'id,desc', idVendedor: 'user-123' }
    })
  })

  it('trata erros no carregamento inicial de dados', async () => {
    api.get.mockRejectedValue({ response: { data: 'Erro Interno' } })

    mountComponent()
    await nextTick()

    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'error',
        summary: 'Falha de Carga de Vendas'
      })
    )
    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'error',
        summary: 'Falha de Carga de Vendedores'
      })
    )
    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'error',
        summary: 'Falha de Carga de Clientes'
      })
    )
  })

  it('atualiza paginação e recarrega lista ao disparar onPage', async () => {
    const wrapper = mountComponent()
    await nextTick()

    api.get.mockClear()

    wrapper.vm.onPage({ page: 2, rows: 40 })

    expect(api.get).toHaveBeenLastCalledWith('/sale/list', {
      params: {
        page: 2,
        size: 40,
        sort: 'id,desc',
        idCliente: null,
        idVendedor: null,
        minDesconto: null,
        maxDesconto: null,
        observacoes: null
      }
    })
  })

  it('cobre todas as ramificações de ordenação (sortOrder nulo, === 1 e !== 1)', async () => {
    const wrapper = mountComponent()
    await nextTick()

    api.get.mockClear()

    // 1. sortField preenchido, mas sortOrder nulo/indefinido
    wrapper.vm.onSort({ sortField: 'subTotal', sortOrder: null })
    expect(api.get).toHaveBeenLastCalledWith('/sale/list', {
      params: {
        page: 0,
        size: 20,
        sort: 'subTotal',
        idCliente: null,
        idVendedor: null,
        minDesconto: null,
        maxDesconto: null,
        observacoes: null
      }
    })

    // 2. sortOrder.value === 1 (,asc)
    wrapper.vm.onSort({ sortField: 'subTotal', sortOrder: 1 })
    expect(api.get).toHaveBeenLastCalledWith('/sale/list', {
      params: {
        page: 0,
        size: 20,
        sort: 'subTotal,asc',
        idCliente: null,
        idVendedor: null,
        minDesconto: null,
        maxDesconto: null,
        observacoes: null
      }
    })

    // 3. sortOrder.value !== 1 (,desc)
    wrapper.vm.onSort({ sortField: 'subTotal', sortOrder: -1 })
    expect(api.get).toHaveBeenLastCalledWith('/sale/list', {
      params: {
        page: 0,
        size: 20,
        sort: 'subTotal,desc',
        idCliente: null,
        idVendedor: null,
        minDesconto: null,
        maxDesconto: null,
        observacoes: null
      }
    })
  })

  it('redireciona para edição de venda com ou sem ID', async () => {
    const wrapper = mountComponent()
    await nextTick()

    wrapper.vm.edit(null)
    expect(mockRouterPush).toHaveBeenCalledWith('/core/sale/edit')

    wrapper.vm.edit({ id: 99 })
    expect(mockRouterPush).toHaveBeenCalledWith('/core/sale/edit?id=99')
  })

  it('aciona os botões do cabeçalho e das linhas da tabela diretamente pelo DOM', async () => {
    const wrapper = mountComponent()
    await nextTick()

    const buttons = wrapper.findAll('button')

    // Clica no botão de "Nova Venda" (+) no cabeçalho
    const addButton = buttons.find((b) => b.attributes('icon') === 'pi pi-plus')
    if (addButton) {
      await addButton.trigger('click')
      expect(mockRouterPush).toHaveBeenCalledWith('/core/sale/edit')
    }

    // Clica no botão de "Editar"
    const editButton = buttons.find((b) => b.attributes('icon') === 'pi pi-pencil')
    if (editButton) {
      await editButton.trigger('click')
      expect(mockRouterPush).toHaveBeenCalledWith('/core/sale/edit?id=1')
    }

    // Clica no botão de "Remover"
    const deleteButton = buttons.find((b) => b.attributes('icon') === 'pi pi-trash')
    if (deleteButton) {
      await deleteButton.trigger('click')
      expect(mockConfirmRequire).toHaveBeenCalled()
    }
  })

  it('submete e reseta o formulário de filtro', async () => {
    const wrapper = mountComponent()
    await nextTick()

    await wrapper.vm.filter({ valid: true, values: { idCliente: 5 } })
    expect(api.get).toHaveBeenLastCalledWith('/sale/list', {
      params: { page: 0, size: 20, sort: 'id,desc', idCliente: 5 }
    })

    api.get.mockClear()
    await wrapper.vm.filter({ valid: false, values: {} })
    expect(api.get).not.toHaveBeenCalled()

    const form = wrapper.find('form')
    await form.trigger('reset')
    await nextTick()
    await nextTick()

    expect(api.get).toHaveBeenLastCalledWith('/sale/list', {
      params: {
        page: 0,
        size: 20,
        sort: 'id,desc',
        idCliente: null,
        idVendedor: null,
        minDesconto: null,
        maxDesconto: null,
        observacoes: null
      }
    })
  })

  describe('confirmDelete()', () => {
    it('executa a exclusão de venda com sucesso ao aceitar a confirmação', async () => {
      api.delete.mockResolvedValueOnce({})
      const wrapper = mountComponent()
      await nextTick()

      wrapper.vm.confirmDelete({ id: 10 })

      expect(mockConfirmRequire).toHaveBeenCalled()

      const confirmOptions = mockConfirmRequire.mock.calls[0][0]
      await confirmOptions.accept()

      expect(api.delete).toHaveBeenCalledWith('/sale?id=10')
      expect(mockToastAdd).toHaveBeenCalledWith(
        expect.objectContaining({
          severity: 'success',
          summary: 'Sucesso'
        })
      )
    })

    it('trata erro de exclusão ao falhar na requisição delete', async () => {
      api.delete.mockRejectedValueOnce({ response: { data: 'Venda vinculada' } })
      const wrapper = mountComponent()
      await nextTick()

      wrapper.vm.confirmDelete({ id: 10 })

      const confirmOptions = mockConfirmRequire.mock.calls[0][0]
      await confirmOptions.accept()

      expect(mockToastAdd).toHaveBeenCalledWith(
        expect.objectContaining({
          severity: 'error',
          summary: 'Falha de Remoção da Venda'
        })
      )
    })
  })
})