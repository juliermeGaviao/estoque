import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'
import api from '../../../../../util/api'
import { formatCpfCnpj, formatPhone, onlyDigits } from '../../../../../util/util'
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

vi.mock('@/util/util', () => ({
  formatCpfCnpj: vi.fn((val) => `CNPJ: ${val}`),
  formatPhone: vi.fn((val) => `FONE: ${val}`),
  onlyDigits: vi.fn((val) => (val ? val.replace(/\D/g, '') : null))
}))

describe('company/List.vue', () => {
  const mockCompaniesResponse = {
    data: {
      content: [
        { id: 1, razaoSocial: 'Empresa A', nome: 'Fantasia A', cnpj: '12345678000195', fone: '51999999999' }
      ],
      totalElements: 1
    }
  }

  beforeEach(() => {
    vi.clearAllMocks()
    api.get.mockResolvedValue(mockCompaniesResponse)
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
                <slot name="body" :data="{ id: 1, cnpj: '12345678000195', fone: '51999999999' }" />
              </div>
            `
          },
          Button: {
            template: '<button type="button" @click="$emit(\'click\')"><slot /></button>'
          },
          InputText: true,
          InputMask: true,
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

  it('carrega a lista de empresas cliente no onMounted', async () => {
    const wrapper = mountComponent()
    await nextTick()

    expect(api.get).toHaveBeenCalledWith('/client/list-companies', {
      params: { page: 0, size: 20, cnpj: null, fone: null }
    })
    expect(wrapper.exists()).toBe(true)
  })

  it('trata erro no carregamento da lista de empresas cliente', async () => {
    api.get.mockRejectedValueOnce({ response: { data: 'Erro Interno' } })

    mountComponent()
    await nextTick()

    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'error',
        summary: 'Falha de Carga de Empresas Cliente',
        detail: 'Requisição de lista de empresas cliente terminou com o erro: Erro Interno'
      })
    )
  })

  it('atualiza a página e mantém os filtros ao disparar onPage', async () => {
    const wrapper = mountComponent()
    await nextTick()

    api.get.mockClear()

    wrapper.vm.onPage({ page: 1, rows: 40 })

    expect(api.get).toHaveBeenLastCalledWith('/client/list-companies', {
      params: {
        page: 1,
        size: 40,
        razaoSocial: null,
        nome: null,
        cnpj: null,
        fone: null
      }
    })
  })

  it('cobre todas as ramificações de ordenação e higienização de cnpj/fone (onSort e onlyDigits)', async () => {
    const wrapper = mountComponent()
    await nextTick()

    api.get.mockClear()

    // 1. sortOrder numérico com valor 1 (,asc) e tratamento de cnpj/fone
    wrapper.vm.filterValues = { cnpj: '12.345.678/0001-95', fone: '(51) 99999-9999' }
    wrapper.vm.onSort({ sortField: 'razaoSocial', sortOrder: 1 })

    expect(onlyDigits).toHaveBeenCalledWith('12.345.678/0001-95')
    expect(onlyDigits).toHaveBeenCalledWith('(51) 99999-9999')
    expect(api.get).toHaveBeenLastCalledWith('/client/list-companies', {
      params: {
        page: 0,
        size: 20,
        sort: 'razaoSocial,asc',
        cnpj: '12345678000195',
        fone: '51999999999'
      }
    })

    // 2. sortOrder numérico com valor -1 (,desc)
    wrapper.vm.onSort({ sortField: 'razaoSocial', sortOrder: -1 })

    expect(api.get).toHaveBeenLastCalledWith('/client/list-companies', {
      params: {
        page: 0,
        size: 20,
        sort: 'razaoSocial,desc',
        cnpj: '12345678000195',
        fone: '51999999999'
      }
    })

    // 3. sortOrder falsy / nulo
    wrapper.vm.onSort({ sortField: 'razaoSocial', sortOrder: null })

    expect(api.get).toHaveBeenLastCalledWith('/client/list-companies', {
      params: {
        page: 0,
        size: 20,
        sort: 'razaoSocial',
        cnpj: '12345678000195',
        fone: '51999999999'
      }
    })
  })

  it('executa a navegação de edição com ou sem id da entidade', async () => {
    const wrapper = mountComponent()
    await nextTick()

    // Editar existente
    wrapper.vm.edit({ id: 50 })
    expect(mockRouterPush).toHaveBeenCalledWith('/register/company/edit?id=50')

    // Nova empresa
    wrapper.vm.edit(null)
    expect(mockRouterPush).toHaveBeenCalledWith('/register/company/edit')
  })

  it('aciona os botões de ação diretamente da tabela', async () => {
    const wrapper = mountComponent()
    await nextTick()

    const buttons = wrapper.findAll('button')

    // Botão Adicionar (+)
    const addButton = buttons.find((b) => b.attributes('icon') === 'pi pi-plus')
    if (addButton) {
      await addButton.trigger('click')
      expect(mockRouterPush).toHaveBeenCalledWith('/register/company/edit')
    }

    // Botão Editar
    const editButton = buttons.find((b) => b.attributes('icon') === 'pi pi-pencil')
    if (editButton) {
      await editButton.trigger('click')
      expect(mockRouterPush).toHaveBeenCalledWith('/register/company/edit?id=1')
    }

    // Botão Remover
    const deleteButton = buttons.find((b) => b.attributes('icon') === 'pi pi-trash')
    if (deleteButton) {
      await deleteButton.trigger('click')
      expect(mockConfirmRequire).toHaveBeenCalled()
    }
  })

  it('filtra e limpa o formulário corretamente', async () => {
    const wrapper = mountComponent()
    await nextTick()

    // Filtro válido
    await wrapper.vm.filter({ valid: true, values: { razaoSocial: 'Test' } })
    expect(api.get).toHaveBeenLastCalledWith('/client/list-companies', {
      params: {
        page: 0,
        size: 20,
        razaoSocial: 'Test',
        cnpj: null,
        fone: null
      }
    })

    // Filtro inválido
    api.get.mockClear()
    await wrapper.vm.filter({ valid: false, values: {} })
    expect(api.get).not.toHaveBeenCalled()

    // Evento de Reset (limpar)
    const form = wrapper.find('form')
    await form.trigger('reset')
    await nextTick()
    await nextTick()

    expect(api.get).toHaveBeenLastCalledWith('/client/list-companies', {
      params: {
        page: 0,
        size: 20,
        razaoSocial: null,
        nome: null,
        cnpj: null,
        fone: null
      }
    })
  })

  it('executa formatCpfCnpj e formatPhone para renderização da tabela', async () => {
    mountComponent()
    await nextTick()

    expect(formatCpfCnpj).toHaveBeenCalledWith('12345678000195')
    expect(formatPhone).toHaveBeenCalledWith('51999999999')
  })

  describe('confirmDelete()', () => {
    it('remove a empresa cliente ao aceitar a caixa de confirmação', async () => {
      api.delete.mockResolvedValueOnce({})
      const wrapper = mountComponent()
      await nextTick()

      wrapper.vm.confirmDelete({ id: 10 })

      expect(mockConfirmRequire).toHaveBeenCalled()

      const confirmOptions = mockConfirmRequire.mock.calls[0][0]
      await confirmOptions.accept()

      expect(api.delete).toHaveBeenCalledWith('/client?id=10')
      expect(mockToastAdd).toHaveBeenCalledWith(
        expect.objectContaining({
          severity: 'success',
          summary: 'Sucesso',
          detail: 'Empresa cliente removida com sucesso'
        })
      )
    })

    it('exibe mensagem de erro se a remoção falhar', async () => {
      api.delete.mockRejectedValueOnce({ response: { data: 'Empresa vinculada a vendas' } })
      const wrapper = mountComponent()
      await nextTick()

      wrapper.vm.confirmDelete({ id: 10 })

      const confirmOptions = mockConfirmRequire.mock.calls[0][0]
      await confirmOptions.accept()

      expect(mockToastAdd).toHaveBeenCalledWith(
        expect.objectContaining({
          severity: 'error',
          summary: 'Falha de Remoção de Empresa Cliente',
          detail: 'Requisição de remoção de empresa cliente terminou com o erro: Empresa vinculada a vendas'
        })
      )
    })
  })
})