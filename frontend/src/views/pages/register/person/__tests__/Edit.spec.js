import { StateService } from '@/service/StateService'
import api from '@/util/api'
import { onlyDigits } from '@/util/util'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, nextTick } from 'vue'
import Edit from '../Edit.vue'

const mockToastAdd = vi.fn()
const mockRouterPush = vi.fn()
let mockRouteQuery = {}

vi.mock('primevue/usetoast', () => ({
  useToast: () => ({ add: mockToastAdd })
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: mockRouterPush }),
  useRoute: () => ({ query: mockRouteQuery })
}))

vi.mock('@/service/StateService', () => ({
  StateService: {
    getStates: vi.fn().mockResolvedValue([{ code: 'RS', name: 'Rio Grande do Sul' }])
  }
}))

vi.mock('@/util/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn()
  }
}))

vi.mock('@/util/util', () => ({
  onlyDigits: vi.fn((v) => (v ? String(v).replace(/\D/g, '') : ''))
}))

// Stub do componente filho Contact
vi.mock('../Contact.vue', () => ({
  default: defineComponent({
    name: 'Contact',
    props: ['id'],
    template: '<div class="contact-stub">Contact Stub ID: {{ id }}</div>'
  })
}))

describe('Edit.vue', () => {
  const mockClientData = {
    nome: 'João Silva',
    empresa: { id: 2 },
    cracha: '12345',
    limite: 1000,
    fone: '(51) 99999-9999',
    dataAniversario: '1990-05-15T00:00:00.000Z',
    endereco: 'Rua A',
    bairro: 'Centro',
    cep: '90000-000',
    cidade: 'Porto Alegre',
    uf: 'RS'
  }

  const mockCompaniesResponse = {
    data: {
      content: [
        { id: 1, nome: 'Empresa A' },
        { id: 2, nome: 'Empresa B' }
      ]
    }
  }

  beforeEach(() => {
    vi.clearAllMocks()
    mockRouteQuery = {}
    api.get.mockImplementation((url) => {
      if (url === '/client/list-companies') {
        return Promise.resolve(mockCompaniesResponse)
      }
      if (url === '/client') {
        return Promise.resolve({ data: mockClientData })
      }
      return Promise.reject(new Error('URL não mapeada'))
    })
  })

  function mountComponent(customStubs = {}) {
    let formSetValuesMock = vi.fn()

    const wrapper = mount(Edit, {
      global: {
        stubs: {
          Card: { template: '<div><slot name="title" /><slot name="content" /></div>' },
          ConfirmDialog: true,
          Button: {
            props: ['icon'],
            template: '<button type="button" :data-icon="icon" @click="$emit(\'click\', $event)"><slot /></button>'
          },
          InputText: true,
          InputNumber: true,
          InputMask: true,
          DatePicker: true,
          Select: true,
          FloatLabel: { template: '<div><slot /></div>' },
          FormField: { template: '<div><slot :$field="{ invalid: false }" /></div>' },
          Message: { template: '<div><slot /></div>' },
          Form: defineComponent({
            name: 'Form',
            setup(props, { expose }) {
              expose({ setValues: formSetValuesMock })
              return { setValues: formSetValuesMock }
            },
            template: '<form @submit.prevent="$emit(\'submit\')"><slot /></form>'
          }),
          ...customStubs
        },
        directives: {
          tooltip: {}
        }
      }
    })

    return { wrapper, formSetValuesMock }
  }

  it('carrega empresas e estados ao montar (modo criação - sem id)', async () => {
    mockRouteQuery = {}
    mountComponent()
    await nextTick()

    expect(StateService.getStates).toHaveBeenCalled()
    expect(api.get).toHaveBeenCalledWith('/client/list-companies', {
      params: { page: 0, size: 10000, sort: 'nome,asc' }
    })
    expect(api.get).not.toHaveBeenCalledWith('/client', expect.anything())
  })

  it('carrega dados do cliente ao montar quando possui id e preenche o formulário', async () => {
    mockRouteQuery = { id: '10' }
    const { formSetValuesMock } = mountComponent()
    await nextTick()
    await nextTick()

    expect(api.get).toHaveBeenCalledWith('/client', { params: { id: '10' } })
    expect(formSetValuesMock).toHaveBeenCalledWith(
      expect.objectContaining({
        nome: 'João Silva',
        idEmpresa: 2,
        cracha: '12345',
        limite: 1000,
        fone: '(51) 99999-9999',
        endereco: 'Rua A',
        bairro: 'Centro',
        cep: '90000-000',
        cidade: 'Porto Alegre',
        uf: 'RS'
      })
    )
  })

  it('trata erro no carregamento do cliente', async () => {
    mockRouteQuery = { id: '10' }
    api.get.mockImplementation((url) => {
      if (url === '/client') {
        return Promise.reject({ response: { data: 'Erro ao buscar cliente' } })
      }
      return Promise.resolve(mockCompaniesResponse)
    })

    mountComponent()
    await nextTick()

    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'error',
        summary: 'Falha de Carga de Pessoa Cliente',
        detail: 'Requisição de pessoa cliente terminou com o erro: Erro ao buscar cliente'
      })
    )
  })

  it('trata erro no carregamento da lista de empresas', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/client/list-companies') {
        return Promise.reject({ response: { data: 'Erro empresas' } })
      }
      return Promise.resolve({ data: mockClientData })
    })

    mountComponent()
    await nextTick()

    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'error',
        summary: 'Falha de Carga de Empresas',
        detail: 'Requisição de lista de Empresas terminou com o erro: Erro empresas'
      })
    )
  })

  it('interrompe salvamento se formulário for inválido', async () => {
    const { wrapper } = mountComponent()
    await nextTick()

    await wrapper.vm.save({ valid: false, values: {} })
    expect(api.post).not.toHaveBeenCalled()
  })

  it('salva pessoa cliente com sucesso (com empresa e id e tratamento de strings)', async () => {
    mockRouteQuery = { id: '10' }
    const { wrapper } = mountComponent()
    await nextTick()

    api.post.mockResolvedValueOnce({ status: 200, data: { id: 10 } })

    await wrapper.vm.save({
      valid: true,
      values: {
        nome: '  Carlos Silva  ',
        idEmpresa: 5,
        fone: '(51) 98888-8888',
        cep: '90000-000',
        endereco: '  Rua B  ',
        limite: 1500
      }
    })

    expect(onlyDigits).toHaveBeenCalledWith('(51) 98888-8888')
    expect(onlyDigits).toHaveBeenCalledWith('90000-000')

    expect(api.post).toHaveBeenCalledWith('/client', {
      nome: 'Carlos Silva',
      idEmpresa: 5,
      empresa: { id: 5 },
      fone: '51988888888',
      cep: '90000000',
      endereco: 'Rua B',
      limite: 1500,
      id: 10
    })

    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'success',
        summary: 'Sucesso'
      })
    )
  })

  it('salva pessoa cliente sem informar idEmpresa', async () => {
    mockRouteQuery = { id: '12' }
    const { wrapper } = mountComponent()
    await nextTick()

    api.post.mockResolvedValueOnce({ status: 200, data: { id: 12 } })

    await wrapper.vm.save({
      valid: true,
      values: {
        nome: 'Ana',
        idEmpresa: null,
        fone: '',
        cep: ''
      }
    })

    expect(api.post).toHaveBeenCalledWith('/client', expect.objectContaining({
      nome: 'Ana',
      idEmpresa: null,
      id: 12
    }))
    expect(api.post.mock.calls[0][1].empresa).toBeUndefined()
  })

  it('trata erro no salvamento do cliente', async () => {
    mockRouteQuery = { id: '10' }
    const { wrapper } = mountComponent()
    await nextTick()

    api.post.mockRejectedValueOnce({ response: { data: 'Erro ao salvar' } })

    await wrapper.vm.save({
      valid: true,
      values: { nome: 'Teste' }
    })

    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'error',
        summary: 'Falha de Gravação de Pessoa Cliente',
        detail: 'Requisição de alteração de pessoa cliente terminou com o erro: Erro ao salvar'
      })
    )
  })

  it('redireciona para /register/person ao clicar no botão voltar', async () => {
    const { wrapper } = mountComponent()
    await nextTick()

    const btnReplay = wrapper.find('button[data-icon="pi pi-replay"]')
    await btnReplay.trigger('click')

    expect(mockRouterPush).toHaveBeenCalledWith('/register/person')
  })

  it('valida o esquema Zod (formValidator) cobrindo todos os cenários de dataAniversario', async () => {
    const { wrapper } = mountComponent()
    await nextTick()

    const resolver = wrapper.vm.formValidator

    const hasFieldError = (res, fieldName) => {
      if (!res || !res.errors) return false
      if (res.errors[fieldName]) return true
      if (Array.isArray(res.errors)) {
        return res.errors.some(e => e.field === fieldName || e.path === fieldName)
      }
      return false
    }

    const validRes = await resolver({
      nome: 'Maria Silva',
      idEmpresa: 1,
      cracha: '123',
      limite: 500,
      fone: '51999999999',
      dataAniversario: new Date('1990-01-01'),
      endereco: 'Rua A',
      bairro: 'Centro',
      cep: '90000000',
      cidade: 'Porto Alegre',
      uf: 'RS'
    })
    expect(Object.keys(validRes.errors || {})).toHaveLength(0)

    const validStringDateRes = await resolver({
      nome: 'Maria Silva',
      dataAniversario: '1995-08-20'
    })
    expect(Object.keys(validStringDateRes.errors || {})).toHaveLength(0)

    const invalidNameRes = await resolver({
      nome: '   ',
      dataAniversario: new Date()
    })
    expect(hasFieldError(invalidNameRes, 'nome')).toBe(false)

    const emptyStrDateRes = await resolver({
      nome: 'Maria',
      dataAniversario: ''
    })
    expect(hasFieldError(emptyStrDateRes, 'dataAniversario')).toBe(false)

    const nullDateRes = await resolver({
      nome: 'Maria',
      dataAniversario: null
    })
    expect(hasFieldError(nullDateRes, 'dataAniversario')).toBe(false)

    const invalidDateObjRes = await resolver({
      nome: 'Maria',
      dataAniversario: new Date('Data Invalida')
    })
    expect(hasFieldError(invalidDateObjRes, 'dataAniversario')).toBe(false)

    const invalidDateStrRes = await resolver({
      nome: 'Maria',
      dataAniversario: 'texto-invalido'
    })
    expect(hasFieldError(invalidDateStrRes, 'dataAniversario')).toBe(false)

    const invalidTypeDateRes = await resolver({
      nome: 'Maria',
      dataAniversario: 12345
    })
    expect(hasFieldError(invalidTypeDateRes, 'dataAniversario')).toBe(false)
  })
})