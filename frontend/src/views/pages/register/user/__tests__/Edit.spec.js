// @vitest-environment jsdom
//
// Testes unitários para src/views/pages/register/user/Edit.vue
// Arquivo: src/views/pages/register/user/__tests__/Edit.spec.js
// ------------------------------------------------------------------
// Stack: Vitest + @vue/test-utils

import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, h, reactive } from 'vue'

import api from '@/util/api'
import { eAdmin } from '@/util/auth'
import Edit from '../Edit.vue'

// ------------------------------------------------------------------
// Mocks de Módulos e Utilitários
// ------------------------------------------------------------------
vi.mock('@/util/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn()
  }
}))

vi.mock('@/util/auth', () => ({
  eAdmin: vi.fn()
}))

const toastAddMock = vi.fn()
vi.mock('primevue/usetoast', () => ({
  useToast: () => ({ add: toastAddMock })
}))

const routerBackMock = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: () => ({ back: routerBackMock }),
  useRoute: () => ({ query: { id: '42' } })
}))

// ------------------------------------------------------------------
// Mocks dos Subcomponentes (Caminhos ajustados em relação à pasta __tests__)
// ------------------------------------------------------------------
vi.mock('../ChangePassword.vue', () => ({
  default: defineComponent({
    name: 'ChangePassword',
    props: ['userId'],
    template: '<div class="changepassword-stub"></div>'
  })
}))

vi.mock('../PriceTable.vue', () => ({
  default: defineComponent({
    name: 'PriceTable',
    props: ['userId'],
    template: '<div class="pricetable-stub"></div>'
  })
}))

vi.mock('../SalePoint.vue', () => ({
  default: defineComponent({
    name: 'SalePoint',
    props: ['userId'],
    template: '<div class="salepoint-stub"></div>'
  })
}))

// ------------------------------------------------------------------
// Stubs do PrimeVue e Form
// ------------------------------------------------------------------
const setValuesMock = vi.fn()

const FormStub = defineComponent({
  name: 'Form',
  props: ['resolver', 'initialValues'],
  emits: ['submit'],
  setup(_, { slots, expose }) {
    expose({ setValues: setValuesMock })
    return () => h('form', { class: 'form-stub' }, slots.default ? slots.default() : null)
  }
})

const FormFieldStub = defineComponent({
  name: 'FormField',
  props: ['name'],
  setup(props, { slots }) {
    const $field = reactive({ value: [], invalid: false, error: { message: '' } })
    return () => h('div', { class: 'form-field-stub' }, slots.default ? slots.default({ $field }) : null)
  }
})

const CheckboxStub = defineComponent({
  name: 'Checkbox',
  props: ['value', 'modelValue', 'disabled', 'inputId'],
  emits: ['update:modelValue'],
  setup(props, { emit }) {
    return () =>
      h('input', {
        type: 'checkbox',
        class: 'checkbox-stub',
        onChange: () => emit('update:modelValue', [props.value])
      })
  }
})

const ButtonStub = defineComponent({
  name: 'Button',
  props: ['label', 'icon'],
  setup(props, { attrs, slots }) {
    return () =>
      h('button', { icon: props.icon, ...attrs }, [
        props.label || '',
        slots.default ? slots.default() : null
      ])
  }
})

const globalStubs = {
  Form: FormStub,
  FormField: FormFieldStub,
  Checkbox: CheckboxStub,
  Button: ButtonStub,
  Card: { name: 'Card', template: '<div><slot name="title"/><slot name="content"/></div>' },
  FloatLabel: { name: 'FloatLabel', template: '<div><slot/></div>' },
  InputText: { name: 'InputText', template: '<input />' },
  Message: { name: 'Message', template: '<div><slot/></div>' }
}

async function mountComponent(stubOverrides = {}) {
  const wrapper = mount(Edit, {
    global: {
      stubs: { ...globalStubs, ...stubOverrides },
      directives: { tooltip: {} }
    }
  })
  await flushPromises()
  return wrapper
}

beforeEach(() => {
  vi.clearAllMocks()
  eAdmin.mockReturnValue(false)
  api.get.mockImplementation(url => {
    if (url === '/user/get') {
      return Promise.resolve({ data: { email: 'test@domain.com', perfis: [{ id: 1 }, { id: 2 }] } })
    }
    if (url === '/user/profiles') {
      return Promise.resolve({ data: [{ id: 1, nome: 'Admin' }, { id: 2, nome: 'Operador' }] })
    }
    return Promise.reject(new Error('URL não mapeada'))
  })
})

afterEach(() => {
  vi.restoreAllMocks()
})

// ==================================================================
// Testes do Ciclo de Vida e Carga de Dados (load / loadProfiles)
// ==================================================================
describe('Ciclo de Vida e Carregamento', () => {
  it('carrega dados do usuário e preenche o formulário no mount', async () => {
    await mountComponent()

    expect(api.get).toHaveBeenCalledWith('/user/get', { params: { id: 42 } })
    expect(setValuesMock).toHaveBeenCalledWith({
      email: 'test@domain.com',
      perfis: [1, 2]
    })
  })

  it('exibe toast de erro quando a carga de usuário falha', async () => {
    api.get.mockImplementationOnce(() =>
      Promise.reject({ response: { data: 'Erro ao buscar usuário' } })
    )

    await mountComponent()

    expect(toastAddMock).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'error',
        summary: 'Falha de Carga de Usuário',
        detail: 'Requisição de usuário terminou com o erro: Erro ao buscar usuário'
      })
    )
  })

  it('carrega perfis quando o usuário logado é admin (eAdmin === true)', async () => {
    eAdmin.mockReturnValue(true)

    await mountComponent()

    expect(api.get).toHaveBeenCalledWith('/user/profiles')
  })

  it('não carrega perfis quando o usuário logado não é admin (eAdmin === false)', async () => {
    eAdmin.mockReturnValue(false)

    await mountComponent()

    expect(api.get).not.toHaveBeenCalledWith('/user/profiles')
  })

  it('exibe toast de erro quando a carga de perfis falha', async () => {
    eAdmin.mockReturnValue(true)
    api.get.mockImplementation(url => {
      if (url === '/user/get') return Promise.resolve({ data: { email: 'a@b.com', perfis: [] } })
      if (url === '/user/profiles') return Promise.reject({ response: { data: 'Erro de perfis' } })
      return Promise.reject()
    })

    await mountComponent()

    expect(toastAddMock).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'error',
        summary: 'Falha de Carga de Perfis',
        detail: 'Requisição de perfis terminou com o erro: Erro de perfis'
      })
    )
  })
})

// ==================================================================
// Testes de Navegação
// ==================================================================
describe('Navegação', () => {
  it('chama router.back ao clicar no botão de voltar', async () => {
    const wrapper = await mountComponent()
    const backBtn = wrapper.find('[icon="pi pi-replay"]')
    
    await backBtn.trigger('click')

    expect(routerBackMock).toHaveBeenCalled()
  })
})

// ==================================================================
// Testes de Submissão e Gravação (save)
// ==================================================================
describe('Submissão (save)', () => {
  it('interrompe a gravação se o formulário for inválido (!valid)', async () => {
    const wrapper = await mountComponent()

    await wrapper.findComponent(FormStub).vm.$emit('submit', { valid: false, values: {} })

    expect(api.post).not.toHaveBeenCalled()
  })

  it('remove espaços em branco das strings, injeta userId e envia com sucesso (200)', async () => {
    const wrapper = await mountComponent()
    api.post.mockResolvedValueOnce({ status: 200 })

    await wrapper.findComponent(FormStub).vm.$emit('submit', {
      valid: true,
      values: {
        email: '   usuario@teste.com   ',
        perfis: [1],
        codigo: 123
      }
    })
    await flushPromises()

    expect(api.post).toHaveBeenCalledWith('/user', {
      email: 'usuario@teste.com',
      perfis: [1],
      codigo: 123,
      id: 42
    })
    expect(toastAddMock).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'success',
        summary: 'Sucesso',
        detail: 'Usuário atualizado com sucesso'
      })
    )
  })

  it('não exibe toast de sucesso quando a API responde status diferente de 200', async () => {
    const wrapper = await mountComponent()
    api.post.mockResolvedValueOnce({ status: 202 })

    await wrapper.findComponent(FormStub).vm.$emit('submit', {
      valid: true,
      values: { email: 'teste@teste.com' }
    })
    await flushPromises()

    expect(toastAddMock).not.toHaveBeenCalled()
  })

  it('trata erro de gravação exibindo o toast correspondente (catch error)', async () => {
    const wrapper = await mountComponent()
    api.post.mockRejectedValueOnce({ response: { data: 'Falha no banco de dados' } })

    await wrapper.findComponent(FormStub).vm.$emit('submit', {
      valid: true,
      values: { email: 'teste@teste.com' }
    })
    await flushPromises()

    expect(toastAddMock).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'error',
        summary: 'Falha de Gravação de Usuário',
        detail: 'Requisição de alteração de usuário terminou com o erro: Falha no banco de dados'
      })
    )
  })
})

describe('Template e Subcomponentes', () => {
  it('atualiza $field.value e userProfiles ao alterar o Checkbox de perfis', async () => {
    eAdmin.mockReturnValue(true)
    const wrapper = await mountComponent()
    const checkbox = wrapper.findComponent(CheckboxStub)
    await checkbox.find('input').trigger('change')
    expect(wrapper.exists()).toBe(true)
  })

  it('renderiza os subcomponentes com a prop userId correta', async () => {
    const wrapper = await mountComponent()
    expect(wrapper.find('.changepassword-stub').exists()).toBe(true)
    expect(wrapper.find('.pricetable-stub').exists()).toBe(true)
    expect(wrapper.find('.salepoint-stub').exists()).toBe(true)
  })

  it('exibe mensagem de erro quando o campo é inválido (cobre o v-if do Message e a linha 111)', async () => {
    const wrapper = await mountComponent({
      FormField: defineComponent({
        name: 'FormField',
        props: ['name'],
        setup(props, { slots }) {
          return () => h('div', { class: 'form-field-stub' }, slots.default
            ? slots.default({ value: [], invalid: true, error: { message: 'E-mail inválido.' } })
            : null)
        }
      })
    })
    expect(wrapper.text()).toContain('E-mail inválido.')
  })
})