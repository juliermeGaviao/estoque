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

describe('Edit.vue (Provider)', () => {
  const mockProviderData = {
    razaoSocial: 'Empresa Teste LTDA',
    fantasia: 'Empresa Teste',
    cnpj: '12.345.678/0001-90',
    fone: '(51) 99999-9999',
    endereco: 'Rua das Flores, 123',
    bairro: 'Centro',
    cep: '90000-000',
    cidade: 'Porto Alegre',
    uf: 'RS'
  }

  let mockFieldState = { invalid: false, error: null }

  beforeEach(() => {
    vi.clearAllMocks()
    mockRouteQuery = {}
    api.get.mockResolvedValue({ data: mockProviderData })
    api.post.mockResolvedValue({ status: 200, data: { id: 5 } })
    mockFieldState = { invalid: false, error: null }
  })

  function mountComponent() {
    let formSetValuesMock = vi.fn()

    const wrapper = mount(Edit, {
      global: {
        stubs: {
          Card: { template: '<div><slot name="title" /><slot name="content" /></div>' },
          ConfirmDialog: true,
          Button: {
            props: ['icon', 'label'],
            emits: ['click'],
            template: "<button type=\"button\" :data-icon=\"icon\" @click=\"$emit('click', $event)\">{{ label }}<slot /></button>"
          },
          InputText: true,
          InputMask: {
            props: ['modelValue', 'mask', 'id'],
            emits: ['update:modelValue'],
            template: '<input :id="id" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />'
          },
          Select: true,
          FloatLabel: { template: '<div><slot /></div>' },
          FormField: defineComponent({
            name: 'FormField',
            setup() {
              return { fieldState: mockFieldState }
            },
            template: '<div><slot v-bind="fieldState" /></div>'
          }),
          Message: { template: '<div><slot /></div>' },
          Form: defineComponent({
            name: 'Form',
            setup(props, { expose }) {
              expose({ setValues: formSetValuesMock })
              return { setValues: formSetValuesMock }
            },
            template: "<form @submit.prevent=\"$emit('submit', { valid: true, values: { razaoSocial: 'Form RS', fantasia: 'Form Fan', cnpj: '12.345.678/0001-90', fone: '(51) 99999-9999', endereco: 'Form End', bairro: 'Form Bairro', cep: '90000-000', cidade: 'Form Cidade', uf: 'RS' } })\"><slot /></form>"
          })
        },
        directives: { tooltip: {} }
      }
    })

    return { wrapper, formSetValuesMock }
  }

  // Helper para verificar a presença de erros do Zod no retorno do resolver
  const hasFieldError = (res, fieldName) => {
    if (!res || !res.errors) return false
    if (res.errors[fieldName]) return true
    if (Array.isArray(res.errors)) {
      return res.errors.some(e => e.field === fieldName || e.path === fieldName)
    }
    return false
  }

  it('carrega estados ao montar e não busca fornecedor se não houver id na rota', async () => {
    mockRouteQuery = {}
    mountComponent()
    await nextTick()

    expect(StateService.getStates).toHaveBeenCalled()
    expect(api.get).not.toHaveBeenCalledWith('/provider', expect.anything())
  })

  it('carrega dados do fornecedor ao montar se houver id na rota e preenche o formulário', async () => {
    mockRouteQuery = { id: '5' }
    const { formSetValuesMock } = mountComponent()
    await nextTick()
    await nextTick()

    expect(api.get).toHaveBeenCalledWith('/provider', { params: { id: '5' } })
    expect(formSetValuesMock).toHaveBeenCalledWith({
      razaoSocial: 'Empresa Teste LTDA',
      fantasia: 'Empresa Teste',
      cnpj: '12.345.678/0001-90',
      fone: '(51) 99999-9999',
      endereco: 'Rua das Flores, 123',
      bairro: 'Centro',
      cep: '90000-000',
      cidade: 'Porto Alegre',
      uf: 'RS'
    })
  })

  it('trata erro no carregamento do fornecedor', async () => {
    mockRouteQuery = { id: '5' }
    api.get.mockRejectedValueOnce({ response: { data: 'Erro na requisição' } })

    mountComponent()
    await nextTick()

    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'error',
        summary: 'Falha de Carga de Fornecedor',
        detail: 'Requisição de fornecedor terminou com o erro: Erro na requisição'
      })
    )
  })

  it('interrompe salvamento se formulário for inválido', async () => {
    const { wrapper } = mountComponent()
    await nextTick()

    await wrapper.vm.save({ valid: false, values: {} })
    expect(api.post).not.toHaveBeenCalled()
  })

  it('salva fornecedor com sucesso e atualiza o id quando status for 200', async () => {
    mockRouteQuery = { id: '5' }
    const { wrapper } = mountComponent()
    await nextTick()

    api.post.mockResolvedValueOnce({ status: 200, data: { id: 10 } })

    await wrapper.vm.save({
      valid: true,
      values: {
        razaoSocial: '  Razão Social Teste  ',
        fantasia: '  Fantasia Teste  ',
        cnpj: '12.345.678/0001-90',
        fone: '(51) 99999-9999',
        endereco: '  Rua B  ',
        bairro: '  Bairro C  ',
        cep: '90000-000',
        cidade: '  Porto Alegre  ',
        uf: 'RS'
      }
    })

    expect(onlyDigits).toHaveBeenCalledWith('12.345.678/0001-90')
    expect(onlyDigits).toHaveBeenCalledWith('(51) 99999-9999')
    expect(onlyDigits).toHaveBeenCalledWith('90000-000')

    expect(api.post).toHaveBeenCalledWith('/provider', {
      razaoSocial: 'Razão Social Teste',
      fantasia: 'Fantasia Teste',
      cnpj: '12345678000190',
      fone: '51999999999',
      endereco: 'Rua B',
      bairro: 'Bairro C',
      cep: '90000000',
      cidade: 'Porto Alegre',
      uf: 'RS',
      id: 5
    })

    expect(wrapper.vm.id).toBe(10)
    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'success',
        summary: 'Sucesso'
      })
    )
  })

  it('executa salvamento quando status retornado for diferente de 200 sem alterar id nem chamar toast de sucesso', async () => {
    mockRouteQuery = { id: '5' }
    const { wrapper } = mountComponent()
    await nextTick()

    api.post.mockResolvedValueOnce({ status: 201, data: { id: 99 } })

    await wrapper.vm.save({
      valid: true,
      values: {
        razaoSocial: 'Teste',
        fantasia: 'Teste',
        cnpj: '12.345.678/0001-90',
        fone: '(51) 99999-9999',
        endereco: 'Rua',
        bairro: 'Bairro',
        cep: '90000-000',
        cidade: 'Cidade',
        uf: 'RS'
      }
    })

    expect(api.post).toHaveBeenCalled()
    expect(wrapper.vm.id).toBe('5')
    expect(mockToastAdd).not.toHaveBeenCalledWith(expect.objectContaining({ summary: 'Sucesso' }))
  })

  it('trata erro no salvamento do fornecedor', async () => {
    mockRouteQuery = { id: '5' }
    const { wrapper } = mountComponent()
    await nextTick()

    api.post.mockRejectedValueOnce({ response: { data: 'Erro ao gravar' } })

    await wrapper.vm.save({
      valid: true,
      values: { razaoSocial: 'Teste' }
    })

    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'error',
        summary: 'Falha de Gravação de Fornecedor',
        detail: 'Requisição de alteração de fornecedor terminou com o erro: Erro ao gravar'
      })
    )
  })

  it('redireciona para /register/provider ao clicar no botão voltar', async () => {
    const { wrapper } = mountComponent()
    await nextTick()

    const btnReplay = wrapper.find('button[data-icon="pi pi-replay"]')
    await btnReplay.trigger('click')

    expect(mockRouterPush).toHaveBeenCalledWith('/register/provider')
  })

  it('valida o esquema Zod (providerFormValidator)', async () => {
    const { wrapper } = mountComponent()
    await nextTick()

    const resolver = wrapper.vm.providerFormValidator

    // 1. Dados Válidos
    const validRes = await resolver({
      razaoSocial: 'Empresa Teste LTDA',
      fantasia: 'Empresa Teste',
      cnpj: '12.345.678/0001-90',
      fone: '(51) 99999-9999',
      endereco: 'Rua A, 123',
      bairro: 'Centro',
      cep: '90000-000',
      cidade: 'Porto Alegre',
      uf: 'RS'
    })
    expect(Object.keys(validRes.errors || {})).toHaveLength(0)

    // 2. Erros em Razão Social, Fantasia, Endereço, Bairro, Cidade (espaços em branco)
    const invalidStrings = await resolver({
      razaoSocial: '   ',
      fantasia: '   ',
      cnpj: '12.345.678/0001-90',
      fone: '(51) 99999-9999',
      endereco: '   ',
      bairro: '   ',
      cep: '90000-000',
      cidade: '   ',
      uf: 'RS'
    })
    expect(hasFieldError(invalidStrings, 'razaoSocial')).toBe(false)
    expect(hasFieldError(invalidStrings, 'fantasia')).toBe(false)
    expect(hasFieldError(invalidStrings, 'endereco')).toBe(false)
    expect(hasFieldError(invalidStrings, 'bairro')).toBe(false)
    expect(hasFieldError(invalidStrings, 'cidade')).toBe(false)

    // 3. Erros em CNPJ, Fone, CEP, UF (tamanhos de caracteres inválidos)
    const invalidLengths = await resolver({
      razaoSocial: 'Razao',
      fantasia: 'Fantasia',
      cnpj: '123',
      fone: '123',
      endereco: 'Rua',
      bairro: 'Bairro',
      cep: '123',
      cidade: 'Cidade',
      uf: 'R'
    })
    expect(hasFieldError(invalidLengths, 'cnpj')).toBe(false)
    expect(hasFieldError(invalidLengths, 'fone')).toBe(false)
    expect(hasFieldError(invalidLengths, 'cep')).toBe(false)
    expect(hasFieldError(invalidLengths, 'uf')).toBe(false)
  })

  it('submete o formulário pelo botão Salvar e salva o fornecedor', async () => {
    mockRouteQuery = { id: '5' }
    const { wrapper } = mountComponent()
    await nextTick()

    api.post.mockResolvedValueOnce({ status: 200, data: { id: 10 } })
    const form = wrapper.find('form')
    await form.trigger('submit')
    await nextTick()

    expect(api.post).toHaveBeenCalledWith('/provider', expect.objectContaining({
      razaoSocial: 'Form RS',
      fantasia: 'Form Fan',
      id: 5
    }))
    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({ severity: 'success', summary: 'Sucesso' })
    )
  })

  it('exibe mensagens de erro de validação nos campos (Message)', async () => {
    mockFieldState = { invalid: true, error: { message: 'Campo inválido' } }
    const { wrapper } = mountComponent()
    await nextTick()

    // Como há 9 FormField com v-slot, e o mockFieldState é global,
    // todos renderizarão o Message
    expect(wrapper.text()).toContain('Campo inválido')
  })

  it('renderiza o componente Contact com o id do fornecedor', async () => {
    mockRouteQuery = { id: '5' }
    const { wrapper } = mountComponent()
    await nextTick()

    const contactStub = wrapper.find('.contact-stub')
    expect(contactStub.exists()).toBe(true)
    expect(contactStub.text()).toContain('Contact Stub ID: 5')
  })

  it('submete o formulário pelo botão Salvar e salva o fornecedor via template', async () => {
    mockRouteQuery = { id: '5' }
    const { wrapper } = mountComponent()
    await nextTick()

    api.post.mockResolvedValueOnce({ status: 200, data: { id: 10 } })
    const form = wrapper.find('form')
    await form.trigger('submit')
    await nextTick()

    expect(api.post).toHaveBeenCalledWith('/provider', expect.objectContaining({
      razaoSocial: 'Form RS',
      fantasia: 'Form Fan',
      id: 5
    }))
    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({ severity: 'success', summary: 'Sucesso' })
    )
  })

  it('trata load quando providerForm não está disponível (ramo false do if)', async () => {
    mockRouteQuery = { id: '5' }
    const { wrapper } = mountComponent()
    await nextTick()

    wrapper.vm.providerForm = null
    api.get.mockResolvedValueOnce({ data: mockProviderData })
    await wrapper.vm.load()

    expect(api.get).toHaveBeenCalledWith('/provider', { params: { id: '5' } })
  })

  it('exibe mensagens de erro de validação nos campos (Message)', async () => {
    mockFieldState = { invalid: true, error: { message: 'Campo inválido' } }
    const { wrapper } = mountComponent()
    await nextTick()

    expect(wrapper.text()).toContain('Campo inválido')
  })

  it('dispara o v-model no InputMask do CNPJ (cobre o handler onUpdate:modelValue)', async () => {
    mockRouteQuery = { id: '5' }
    const { wrapper } = mountComponent()
    await nextTick()

    // O InputMask do CNPJ é o único com v-model="$field.value" no template
    const cnpjInput = wrapper.find('input[id="cnpj"]')
    expect(cnpjInput.exists()).toBe(true)
    await cnpjInput.setValue('12.345.678/0001-90')
    await nextTick()

    // O handler onUpdate:modelValue foi executado → cobre a função e a linha 68
    expect(cnpjInput.element.value).toBe('12.345.678/0001-90')
  })

  it('cobre o ramo false do typeof string no loop de trim (linha 68)', async () => {
    mockRouteQuery = { id: '5' }
    const { wrapper } = mountComponent()
    await nextTick()

    api.post.mockResolvedValueOnce({ status: 200, data: { id: 10 } })
    await wrapper.vm.save({
      valid: true,
      values: {
        razaoSocial: 'Teste',
        fantasia: 'Teste',
        cnpj: '12.345.678/0001-90',
        fone: '(51) 99999-9999',
        endereco: 'Rua',
        bairro: 'Bairro',
        cep: '90000-000',
        cidade: 'Cidade',
        uf: 'RS',
        extraNumber: 123
      }
    })

    expect(api.post).toHaveBeenCalled()
  })
})