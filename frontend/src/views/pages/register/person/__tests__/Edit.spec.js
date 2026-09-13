import { StateService } from '@/service/StateService'
import api from '@/util/api'
import { onlyDigits } from '@/util/util'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, h, inject, nextTick, provide, reactive } from 'vue'
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

// ------------------------------------------------------------------
// Stub de <Form> (@primevue/forms) — implementa de fato
// states/setValues/setFieldValue/reset (o stub anterior expunha um
// setValues vi.fn() sem estado, e o FormField hardcodeava invalid:false
// sempre — por isso as 8 mensagens de erro nunca podiam aparecer).
// ------------------------------------------------------------------
const FormStub = defineComponent({
  name: 'Form',
  props: ['resolver', 'validateOn', 'initialValues'],
  emits: ['submit', 'reset'],
  setup(props, { slots, expose, emit }) {
    const values = reactive({ ...(props.initialValues || {}) })
    const invalidMap = reactive({})

    function setValues(newValues) {
      Object.assign(values, newValues)
    }
    function setFieldValue(key, val) {
      values[key] = val
    }
    function reset() {
      Object.keys(values).forEach((key) => {
        values[key] = (props.initialValues || {})[key] ?? null
      })
    }
    function setFieldInvalid(key, message = 'Campo inválido.') {
      invalidMap[key] = message
    }
    function clearFieldInvalid(key) {
      delete invalidMap[key]
    }
    function submitWith(payload) {
      emit('submit', payload)
    }

    const states = new Proxy(
      {},
      {
        get(_t, key) {
          if (typeof key !== 'string') return undefined
          return {
            get value() {
              return values[key]
            },
            set value(v) {
              values[key] = v
            },
            get invalid() {
              return Boolean(invalidMap[key])
            },
            get error() {
              return invalidMap[key] ? { message: invalidMap[key] } : null
            }
          }
        }
      }
    )

    provide('__formStubCtx', { values, invalidMap })
    expose({ states, setValues, setFieldValue, reset, setFieldInvalid, clearFieldInvalid, submitWith, values })

    return () =>
      h(
        'form',
        {
          onSubmit: (event) => {
            event.preventDefault()
            emit('submit', { valid: true, values: { ...values } })
          },
          onReset: () => {
            reset()
            emit('reset')
          }
        },
        slots.default ? slots.default() : null
      )
  }
})

// Stub de <FormField> — repassa invalid/error de verdade (lidos do Form
// pai via provide/inject), em vez do { invalid: false } fixo.
const FormFieldStub = defineComponent({
  name: 'FormField',
  props: ['name'],
  setup(props, { slots }) {
    const ctx = inject('__formStubCtx', null)
    return () => {
      const field = ctx
        ? {
            get value() {
              return props.name ? ctx.values[props.name] : undefined
            },
            set value(v) {
              if (props.name) ctx.values[props.name] = v
            },
            invalid: Boolean(props.name && ctx.invalidMap[props.name]),
            error: props.name && ctx.invalidMap[props.name] ? { message: ctx.invalidMap[props.name] } : null
          }
        : { value: undefined, invalid: false, error: null }
      return slots.default ? slots.default(field) : null
    }
  }
})

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
          FormField: FormFieldStub,
          Message: { template: '<div><slot /></div>' },
          Form: FormStub,
          ...customStubs
        },
        directives: {
          tooltip: {}
        }
      }
    })

    return { wrapper }
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
    const { wrapper } = mountComponent()
    await nextTick()
    await nextTick()

    expect(api.get).toHaveBeenCalledWith('/client', { params: { id: '10' } })
    expect(wrapper.vm.form.states.nome.value).toBe('João Silva')
    expect(wrapper.vm.form.states.idEmpresa.value).toBe(2)
    expect(wrapper.vm.form.states.cracha.value).toBe('12345')
    expect(wrapper.vm.form.states.limite.value).toBe(1000)
    expect(wrapper.vm.form.states.fone.value).toBe('(51) 99999-9999')
    expect(wrapper.vm.form.states.endereco.value).toBe('Rua A')
    expect(wrapper.vm.form.states.bairro.value).toBe('Centro')
    expect(wrapper.vm.form.states.cep.value).toBe('90000-000')
    expect(wrapper.vm.form.states.cidade.value).toBe('Porto Alegre')
    expect(wrapper.vm.form.states.uf.value).toBe('RS')
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

  it('não faz nada em load() se o formulário ainda não estiver disponível', async () => {
    mockRouteQuery = { id: '10' }
    const { wrapper } = mountComponent()
    await nextTick()
    wrapper.vm.form = null

    await expect(wrapper.vm.load()).resolves.not.toThrow()
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

  it('não exibe sucesso quando a API responde com status diferente de 200', async () => {
    mockRouteQuery = { id: '10' }
    const { wrapper } = mountComponent()
    await nextTick()

    api.post.mockResolvedValueOnce({ status: 204, data: {} })

    await wrapper.vm.save({ valid: true, values: { nome: 'Sem Sucesso' } })

    expect(mockToastAdd).not.toHaveBeenCalledWith(expect.objectContaining({ severity: 'success' }))
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

    // O zodResolver real espera { values, name } — passar os campos soltos
    // (como o teste fazia antes) faz `values` chegar `undefined` dentro do
    // resolver, e o parseAsync falha genericamente sem nunca exercitar o
    // .refine() de dataAniversario (linhas 29-38). Por isso os testes
    // anteriores esperavam `toBe(false)` mesmo para dados claramente
    // inválidos — a validação de verdade nunca tinha rodado.
    async function validate(values) {
      return resolver({ values })
    }

    const validRes = await validate({
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
    expect(validRes.errors).toEqual({})

    // dataAniversario como string de data válida (cobre `typeof val === 'string'` -> true)
    const validStringDateRes = await validate({
      nome: 'Maria Silva',
      dataAniversario: '1995-08-20'
    })
    expect(validStringDateRes.errors).toEqual({})

    // nome só com espaços (trim().min(1) falha)
    const invalidNameRes = await validate({
      nome: '   ',
      dataAniversario: new Date()
    })
    expect(invalidNameRes.errors.nome[0].message).toBe('Nome é obrigatório.')

    // dataAniversario === '' (cobre `val === ''` -> false)
    const emptyStrDateRes = await validate({
      nome: 'Maria',
      dataAniversario: ''
    })
    expect(emptyStrDateRes.errors.dataAniversario[0].message).toBe('Data de aniversário é obrigatória.')

    // dataAniversario null (cobre `!val` -> false)
    const nullDateRes = await validate({
      nome: 'Maria',
      dataAniversario: null
    })
    expect(nullDateRes.errors.dataAniversario[0].message).toBe('Data de aniversário é obrigatória.')

    // Date inválida (cobre `val instanceof Date && !Number.isNaN(...)` -> false,
    // caindo até o `return false` final, já que não é string)
    const invalidDateObjRes = await validate({
      nome: 'Maria',
      dataAniversario: new Date('Data Invalida')
    })
    expect(invalidDateObjRes.errors.dataAniversario[0].message).toBe('Data de aniversário é obrigatória.')

    // String que não é uma data válida (cobre `typeof val === 'string'` -> true,
    // mas `!Number.isNaN(date.getTime())` -> false)
    const invalidDateStrRes = await validate({
      nome: 'Maria',
      dataAniversario: 'texto-invalido'
    })
    expect(invalidDateStrRes.errors.dataAniversario[0].message).toBe('Data de aniversário é obrigatória.')

    // Tipo totalmente diferente (cobre o `return false` final do refine)
    const invalidTypeDateRes = await validate({
      nome: 'Maria',
      dataAniversario: 12345
    })
    expect(invalidTypeDateRes.errors.dataAniversario[0].message).toBe('Data de aniversário é obrigatória.')
  })

  it('exibe a mensagem de erro de todos os campos com Message no formulário quando estão inválidos', async () => {
    const { wrapper } = mountComponent()
    await nextTick()

    const campos = ['nome', 'fone', 'dataAniversario', 'endereco', 'bairro', 'cep', 'cidade', 'uf']
    campos.forEach((campo) => wrapper.vm.form.setFieldInvalid(campo, `${campo} inválido`))
    await nextTick()

    campos.forEach((campo) => {
      expect(wrapper.text()).toContain(`${campo} inválido`)
    })
  })
})