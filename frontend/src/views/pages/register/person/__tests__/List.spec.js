import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, nextTick } from 'vue'
import api from '../../../../../util/api'
import { formatDate, formatNumber, formatPhone, onlyDigits } from '../../../../../util/util'
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
  formatDate: vi.fn((v) => (v ? `DATE:${v}` : '')),
  formatNumber: vi.fn((v) => (v ? `NUM:${v}` : '')),
  formatPhone: vi.fn((v) => (v ? `PHONE:${v}` : '')),
  onlyDigits: vi.fn((v) => (v ? String(v).replace(/\D/g, '') : ''))
}))

describe('person/List.vue', () => {
  const mockPeopleResponse = {
    data: {
      content: [
        {
          id: 1,
          nome: 'Fulano',
          empresa: { nome: 'Empresa A' },
          cracha: '123',
          limite: 500,
          fone: '51999999999',
          dataAniversario: '1990-05-15'
        }
      ],
      totalElements: 1
    }
  }

  const mockCompaniesResponse = {
    data: {
      content: [
        { id: 10, nome: 'Empresa A' },
        { id: 20, nome: 'Empresa B' }
      ]
    }
  }

  beforeEach(() => {
    vi.clearAllMocks()
    api.get.mockImplementation((url) => {
      if (url === '/client/list-people') return Promise.resolve(mockPeopleResponse)
      if (url === '/client/list-companies') return Promise.resolve(mockCompaniesResponse)
      return Promise.resolve({ data: {} })
    })
  })

  function mountComponent(customStubs = {}) {
    return mount(List, {
      global: {
        stubs: {
          Card: { template: '<div><slot name="title" /><slot name="content" /></div>' },
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
                <slot name="body" :data="{ id: 1, limite: 500, fone: '51999999999', dataAniversario: '1990-05-15' }" />
              </div>
            `
          },
          Button: { 
            props: ['icon'],
            template: '<button type="button" :data-icon="icon" @click="$emit(\'click\', $event)"><slot /></button>' 
          },
          InputText: true,
          InputNumber: true,
          DatePicker: true,
          Select: true,
          ConfirmDialog: true,
          Form: defineComponent({
            name: 'Form',
            template: '<form @submit.prevent="$emit(\'submit\')" @reset="$emit(\'reset\')"><slot /></form>'
          }),
          FormField: { template: '<div><slot /></div>' },
          FloatLabel: { template: '<div><slot /></div>' },
          ...customStubs
        },
        directives: {
          tooltip: {}
        }
      }
    })
  }

  it('carrega a lista de pessoas e empresas na montagem (onMounted)', async () => {
    mountComponent()
    await nextTick()
    await nextTick()

    expect(api.get).toHaveBeenCalledWith('/client/list-people', {
      params: {
        fone: null,
        page: 0,
        size: 20
      }
    })
    expect(api.get).toHaveBeenCalledWith('/client/list-companies', {
      params: { page: 0, size: 10000, sort: 'nome,asc' }
    })
  })

  it('trata erros ao carregar pessoas e empresas', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/client/list-people') return Promise.reject({ response: { data: 'Erro Pessoas' } })
      if (url === '/client/list-companies') return Promise.reject({ response: { data: 'Erro Empresas' } })
      return Promise.resolve({ data: {} })
    })

    mountComponent()
    await nextTick()
    await nextTick()

    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'error',
        summary: 'Falha de Carga de Pessoas Cliente'
      })
    )
    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'error',
        summary: 'Falha de Carga de Empresas'
      })
    )
  })

  it('formata parâmetros de busca (fone, minAniversario, maxAniversario, ordenação asc/desc)', async () => {
    const wrapper = mountComponent()
    await nextTick()

    wrapper.vm.sortField = 'nome'
    wrapper.vm.sortOrder = 1

    await wrapper.vm.filter({
      valid: true,
      values: {
        nome: 'João',
        fone: '(51) 99999-9999',
        minAniversario: '2023-01-01',
        maxAniversario: '2023-12-31'
      }
    })

    expect(onlyDigits).toHaveBeenCalledWith('(51) 99999-9999')
    expect(formatDate).toHaveBeenCalledWith('2023-01-01')
    expect(formatDate).toHaveBeenCalledWith('2023-12-31')
    expect(api.get).toHaveBeenLastCalledWith('/client/list-people', {
      params: expect.objectContaining({
        nome: 'João',
        sort: 'nome,asc',
        fone: '51999999999',
        minAniversario: 'DATE:2023-01-01',
        maxAniversario: 'DATE:2023-12-31'
      })
    })

    wrapper.vm.onSort({ sortField: 'nome', sortOrder: -1 })
    expect(api.get).toHaveBeenLastCalledWith('/client/list-people', {
      params: expect.objectContaining({
        sort: 'nome,desc'
      })
    })
  })

  it('trata fone vazio ou nulo na busca', async () => {
    const wrapper = mountComponent()
    await nextTick()

    await wrapper.vm.filter({
      valid: true,
      values: {
        fone: ''
      }
    })

    expect(api.get).toHaveBeenLastCalledWith('/client/list-people', {
      params: expect.objectContaining({
        fone: null
      })
    })
  })

  it('interrompe o filtro se a validação falhar (if (!valid) return)', async () => {
    const wrapper = mountComponent()
    await nextTick()
    api.get.mockClear()

    await wrapper.vm.filter({ valid: false, values: {} })
    expect(api.get).not.toHaveBeenCalled()
  })

  it('executa a paginação (onPage)', async () => {
    const wrapper = mountComponent()
    await nextTick()

    wrapper.vm.onPage({ page: 2, rows: 40 })

    expect(api.get).toHaveBeenLastCalledWith('/client/list-people', {
      params: expect.objectContaining({
        page: 2,
        size: 40
      })
    })
  })

  it('redireciona ao editar com ID, sem ID ou com objeto vazio', async () => {
    const wrapper = mountComponent()
    await nextTick()

    wrapper.vm.edit({ id: 5 })
    expect(mockRouterPush).toHaveBeenCalledWith('/register/person/edit?id=5')

    wrapper.vm.edit(null)
    expect(mockRouterPush).toHaveBeenCalledWith('/register/person/edit')

    wrapper.vm.edit({})
    expect(mockRouterPush).toHaveBeenCalledWith('/register/person/edit')
  })

  it('remove uma pessoa cliente via caixa de confirmação (sucesso e erro)', async () => {
    const wrapper = mountComponent()
    await nextTick()

    wrapper.vm.confirmDelete({ id: 7 })
    expect(mockConfirmRequire).toHaveBeenCalled()

    api.delete.mockResolvedValueOnce({})
    const confirmOptions = mockConfirmRequire.mock.calls[0][0]
    await confirmOptions.accept()

    expect(api.delete).toHaveBeenCalledWith('/client?id=7')
    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({ severity: 'success', summary: 'Sucesso' })
    )

    wrapper.vm.confirmDelete({ id: 7 })
    api.delete.mockRejectedValueOnce({ response: { data: 'Erro na deleção' } })
    await mockConfirmRequire.mock.calls[1][0].accept()

    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'error',
        summary: 'Falha de Remoção de Pessoa Cliente'
      })
    )
  })

  it('limpa os filtros de pesquisa e reinicia as variáveis (limpar)', async () => {
    const wrapper = mountComponent()
    await nextTick()

    wrapper.vm.sortField = 'nome'
    wrapper.vm.limpar()

    await nextTick()
    await nextTick()

    expect(wrapper.vm.page).toBe(0)
    expect(wrapper.vm.sortField).toBeNull()
    expect(api.get).toHaveBeenLastCalledWith('/client/list-people', {
      params: {
        nome: null,
        idEmpresa: null,
        fone: null,
        minAniversario: null,
        maxAniversario: null,
        minLimite: null,
        maxLimite: null,
        page: 0,
        size: 20
      }
    })
  })

  it('executa os métodos auxiliares de formatação de colunas no template', async () => {
    mountComponent()
    await nextTick()

    expect(formatNumber).toHaveBeenCalledWith(500)
    expect(formatPhone).toHaveBeenCalledWith('51999999999')
    expect(formatDate).toHaveBeenCalledWith('1990-05-15')
  })

  it('interage com os botoes de acao no template (cobertura linhas 256 e 257)', async () => {
    const wrapper = mountComponent()
    await nextTick()

    const btnPencil = wrapper.find('button[data-icon="pi pi-pencil"]')
    expect(btnPencil.exists()).toBe(true)
    await btnPencil.trigger('click')
    expect(mockRouterPush).toHaveBeenCalledWith('/register/person/edit?id=1')

    const btnTrash = wrapper.find('button[data-icon="pi pi-trash"]')
    expect(btnTrash.exists()).toBe(true)
    await btnTrash.trigger('click')
    expect(mockConfirmRequire).toHaveBeenCalled()
  })

  it('formata parâmetros de busca (fone, minAniversario, maxAniversario, ordenação asc/desc/null)', async () => {
    const wrapper = mountComponent()
    await nextTick()

    wrapper.vm.sortField = 'nome'
    wrapper.vm.sortOrder = 1

    await wrapper.vm.filter({
      valid: true,
      values: {
        nome: 'João',
        fone: '(51) 99999-9999',
        minAniversario: '2023-01-01',
        maxAniversario: '2023-12-31'
      }
    })

    expect(onlyDigits).toHaveBeenCalledWith('(51) 99999-9999')
    expect(formatDate).toHaveBeenCalledWith('2023-01-01')
    expect(formatDate).toHaveBeenCalledWith('2023-12-31')
    expect(api.get).toHaveBeenLastCalledWith('/client/list-people', {
      params: expect.objectContaining({
        nome: 'João',
        sort: 'nome,asc',
        fone: '51999999999',
        minAniversario: 'DATE:2023-01-01',
        maxAniversario: 'DATE:2023-12-31'
      })
    })

    wrapper.vm.onSort({ sortField: 'nome', sortOrder: -1 })
    expect(api.get).toHaveBeenLastCalledWith('/client/list-people', {
      params: expect.objectContaining({
        sort: 'nome,desc'
      })
    })

    wrapper.vm.sortField = 'nome'
    wrapper.vm.sortOrder = null
    await wrapper.vm.filter({ valid: true, values: {} })

    expect(api.get).toHaveBeenLastCalledWith('/client/list-people', {
      params: expect.objectContaining({
        sort: 'nome'
      })
    })
  })
})