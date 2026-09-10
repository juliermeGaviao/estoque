// @vitest-environment jsdom
//
// Testes unitários para src/views/pages/register/company/Edit.vue
// ------------------------------------------------------------------
// Stack: Vitest + @vue/test-utils
//
// LEIA ANTES DE RODAR:
// 1. Coloque este arquivo em
//    src/views/pages/register/company/__tests__/Edit.spec.js
//    (o import relativo '../Edit.vue' já assume esse caminho).
// 2. Este projeto usa `unplugin-vue-components` com `PrimeVueResolver()`
//    (ver vite.config), que injeta os imports do PrimeVue diretamente no
//    <script setup> do componente (ex.: `import Form from
//    '@primevue/forms/form'`). Por isso os componentes do PrimeVue são
//    mockados aqui via `vi.mock` nos caminhos reais dos módulos — os
//    mesmos caminhos que o resolver usa — obtidos a partir de
//    `@primevue/metadata`. Isso funciona independentemente de o
//    componente vir de auto-import ou de registro global.
// 3. `Contact.vue` e `Employee.vue` (importados por Edit.vue) são
//    mockados como componentes vazios — o teste cobre Edit.vue
//    isoladamente, não o conteúdo desses componentes filhos. Ajuste o
//    caminho do vi.mock se a localização real desses arquivos diferir.
// 4. `@primevue/forms/resolvers/zod` e `zod` são dependências reais do
//    projeto e NÃO são mockadas — usamos o `formValidator` de verdade
//    num describe dedicado para cobrir os `.refine()`/`.transform()` do
//    schema.
// 5. Não foi possível rodar este arquivo neste ambiente (sandbox sem
//    acesso à rede para instalar as dependências). Rode
//    `npx vitest run --coverage` no seu projeto para confirmar os
//    números e ajustar qualquer detalhe específico do seu setup real.

import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'

import Edit from '../Edit.vue'

import { StateService } from '@/service/StateService'
import api from '@/util/api'

// ------------------------------------------------------------------
// Mocks de módulos utilitários e de app
// ------------------------------------------------------------------

vi.mock('@/util/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn()
  }
}))

vi.mock('@/util/util', () => ({
  onlyDigits: vi.fn((v) => (v ? String(v).replace(/\D/g, '') : ''))
}))

vi.mock('@/service/StateService', () => ({
  StateService: {
    getStates: vi.fn()
  }
}))

const toastAddMock = vi.fn()
vi.mock('primevue/usetoast', () => ({
  useToast: () => ({ add: toastAddMock })
}))

const routerPushMock = vi.fn()
let mockRouteQuery = {}
vi.mock('vue-router', () => ({
  useRoute: () => ({ query: mockRouteQuery }),
  useRouter: () => ({ push: routerPushMock })
}))

// Componentes filhos delegados — fora do escopo deste teste (testam-se
// isoladamente em seus próprios arquivos de spec).
vi.mock('../Contact.vue', () => ({
  default: { name: 'Contact', props: ['id'], template: '<div class="contact-stub"/>' }
}))
vi.mock('../Employee.vue', () => ({
  default: { name: 'Employee', props: ['id'], template: '<div class="employee-stub"/>' }
}))

// ------------------------------------------------------------------
// Stubs dos componentes do PrimeVue, definidos em vi.hoisted para
// poderem ser referenciados dentro das factories de vi.mock (içadas
// para o topo do arquivo pelo Vitest).
// ------------------------------------------------------------------
const { FormStub, FormFieldStub, CardStub, genericStub, ButtonStub, FieldControlStub } = await vi.hoisted(async () => {
  // Não referenciamos os imports de nível superior deste arquivo aqui
  // dentro (Vitest içar vi.hoisted/vi.mock para o topo do arquivo não
  // garante, em toda versão, que outros imports do MESMO arquivo já
  // estejam inicializados nesse ponto — daí o
  // "Cannot access '__vi_import_N__' before initialization"). Import
  // dinâmico de 'vue' é a forma correta e estável de obter essas APIs
  // aqui dentro, independente da versão do Vitest.
  const { defineComponent, h, inject, provide, reactive } = await import('vue')
  // Stub de <Form> (@primevue/forms) — expõe de fato states/setValues/
  // setFieldValue/reset (a implementação "burra" de um <form> que só
  // reemite @submit sem payload quebra qualquer chamada a
  // form.value.setValues, gerando "Unhandled Rejection" mesmo com os
  // testes "passando"). Também expõe helpers só de teste
  // (submitWith/setFieldInvalid) para simular resultados de validação
  // sem depender do zodResolver de verdade nesses testes.
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

  // Stub de <FormField> — repassa invalid/error (para os <Message v-if>)
  // e um `.value` get/set escrevendo direto no Form pai (necessário para
  // o campo cnpj, que usa v-model="$field.value").
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

  const CardStub = defineComponent({
    name: 'Card',
    setup(_, { slots }) {
      return () =>
        h('div', { class: 'card-teststub' }, [
          slots.title ? h('div', {}, slots.title()) : null,
          slots.content ? h('div', {}, slots.content()) : null,
          slots.default ? slots.default() : null
        ])
    }
  })

  function genericStub(name, tag) {
    const renderTag = tag || `${name.toLowerCase()}-teststub`
    return defineComponent({
      name,
      inheritAttrs: false,
      setup(_, { attrs, slots }) {
        return () => h(renderTag, attrs, slots.default ? slots.default() : undefined)
      }
    })
  }

  const ButtonStub = genericStub('Button', 'button')

  // Stub genérico para campos que podem receber v-model (InputText,
  // InputMask, Select): renderiza um <input> nativo de verdade e liga
  // manualmente onUpdate:modelValue/onInput/onChange — recebidos como
  // props não-declaradas (attrs) — aos eventos nativos do DOM. Isso
  // permite exercitar de fato o v-model do campo cnpj
  // (v-model="$field.value"), em vez de só desenhar a tag.
  const FieldControlStub = defineComponent({
    name: 'FieldControl',
    inheritAttrs: false,
    setup(_, { attrs }) {
      return () =>
        h('input', {
          ...attrs,
          onInput: (event) => {
            const value = event.target.value
            if (typeof attrs['onUpdate:modelValue'] === 'function') attrs['onUpdate:modelValue'](value)
            if (typeof attrs.onInput === 'function') attrs.onInput({ value, originalEvent: event })
          },
          onChange: (event) => {
            const value = event.target.value
            if (typeof attrs.onChange === 'function') attrs.onChange({ value, originalEvent: event })
          }
        })
    }
  })

  return { FormStub, FormFieldStub, CardStub, genericStub, ButtonStub, FieldControlStub }
})

vi.mock('@primevue/forms/form', () => ({ default: FormStub }))
vi.mock('@primevue/forms/formfield', () => ({ default: FormFieldStub }))
vi.mock('primevue/card', () => ({ default: CardStub }))
vi.mock('primevue/button', () => ({ default: ButtonStub }))
vi.mock('primevue/inputtext', () => ({ default: FieldControlStub }))
vi.mock('primevue/inputmask', () => ({ default: FieldControlStub }))
vi.mock('primevue/select', () => ({ default: FieldControlStub }))
vi.mock('primevue/message', () => ({ default: genericStub('Message') }))
vi.mock('primevue/floatlabel', () => ({ default: genericStub('FloatLabel') }))
vi.mock('primevue/confirmdialog', () => ({ default: genericStub('ConfirmDialog') }))

// ------------------------------------------------------------------
// Fixtures
// ------------------------------------------------------------------

const statesFixture = [
  { name: 'Rio Grande do Sul', code: 'RS' },
  { name: 'São Paulo', code: 'SP' }
]

const clientFixture = {
  razaoSocial: 'Empresa Teste LTDA',
  nome: 'Empresa Teste',
  cnpj: '12.345.678/0001-95',
  fone: '(51) 99999-9999',
  endereco: 'Rua A, 123',
  bairro: 'Centro',
  cep: '90000-000',
  cidade: 'Porto Alegre',
  uf: 'RS'
}

function rejectWith(message) {
  return Promise.reject({ response: { data: message } })
}

async function mountComponent(routeQuery = {}) {
  mockRouteQuery = routeQuery
  const wrapper = mount(Edit)
  await flushPromises()
  return wrapper
}

beforeEach(() => {
  vi.clearAllMocks()
  StateService.getStates.mockResolvedValue(statesFixture)
  api.get.mockResolvedValue({ data: clientFixture })
  api.post.mockResolvedValue({ status: 200, data: { id: 7 } })
})

afterEach(() => {
  vi.restoreAllMocks()
})

// ==================================================================
// onMounted / load()
// ==================================================================
describe('onMounted', () => {
  it('sempre carrega os estados', async () => {
    await mountComponent({})

    expect(StateService.getStates).toHaveBeenCalled()
  })

  it('carrega a empresa cliente quando há id na rota', async () => {
    const wrapper = await mountComponent({ id: '5' })

    expect(api.get).toHaveBeenCalledWith('/client', { params: { id: '5' } })
    expect(wrapper.vm.form.states.razaoSocial.value).toBe(clientFixture.razaoSocial)
    expect(wrapper.vm.form.states.cnpj.value).toBe(clientFixture.cnpj)
  })

  it('não carrega a empresa cliente quando não há id na rota', async () => {
    await mountComponent({})

    expect(api.get).not.toHaveBeenCalled()
  })

  it('exibe toast de erro quando a carga falha (erro com response)', async () => {
    api.get.mockImplementation(() => rejectWith('Empresa não encontrada'))

    await mountComponent({ id: '5' })

    expect(toastAddMock).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'error',
        summary: 'Falha de Carga de Empresa Cliente',
        detail: expect.stringContaining('Empresa não encontrada')
      })
    )
  })

  it('exibe toast de erro quando a carga falha (erro sem response)', async () => {
    api.get.mockImplementation(() => Promise.reject(new Error('Falha de rede')))

    await mountComponent({ id: '5' })

    expect(toastAddMock).toHaveBeenCalledWith(
      expect.objectContaining({ severity: 'error', summary: 'Falha de Carga de Empresa Cliente' })
    )
  })

  it('não faz nada se o formulário ainda não estiver disponível', async () => {
    const wrapper = await mountComponent({ id: '5' })
    wrapper.vm.form = null

    await expect(wrapper.vm.load()).resolves.not.toThrow()
  })
})

// ==================================================================
// save()
// ==================================================================
describe('save', () => {
  it('não faz nada quando o formulário é inválido', async () => {
    const wrapper = await mountComponent({})
    api.post.mockClear()

    await wrapper.vm.save({ valid: false, values: {} })

    expect(api.post).not.toHaveBeenCalled()
  })

  it('salva com sucesso, higienizando os campos numéricos e strings', async () => {
    const wrapper = await mountComponent({ id: '5' })
    api.post.mockResolvedValueOnce({ status: 200, data: { id: 5 } })

    const values = { ...clientFixture, razaoSocial: '  Empresa Teste LTDA  ' }
    await wrapper.vm.save({ valid: true, values })

    const [, paramsSent] = api.post.mock.calls[0]
    expect(paramsSent.id).toBe(5)
    expect(paramsSent.razaoSocial).toBe('Empresa Teste LTDA')
    expect(paramsSent.cnpj).toBe('12345678000195')
    expect(wrapper.vm.id).toBe(5)
    expect(toastAddMock).toHaveBeenCalledWith(expect.objectContaining({ severity: 'success', summary: 'Sucesso' }))
  })

  it('não exibe sucesso quando a API não retorna status 200', async () => {
    const wrapper = await mountComponent({})
    api.post.mockResolvedValueOnce({ status: 204, data: {} })

    await wrapper.vm.save({ valid: true, values: { ...clientFixture } })

    expect(toastAddMock).not.toHaveBeenCalledWith(expect.objectContaining({ severity: 'success' }))
  })

  it('exibe toast de erro quando a gravação falha (erro com response)', async () => {
    const wrapper = await mountComponent({})
    api.post.mockImplementationOnce(() => rejectWith('Erro de validação no banco'))

    await wrapper.vm.save({ valid: true, values: { ...clientFixture } })

    expect(toastAddMock).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'error',
        summary: 'Falha de Gravação de Empresa Cliente',
        detail: expect.stringContaining('Erro de validação no banco')
      })
    )
  })

  it('exibe toast de erro quando a gravação falha (erro sem response)', async () => {
    const wrapper = await mountComponent({})
    api.post.mockImplementationOnce(() => Promise.reject(new Error('Falha de rede')))

    await wrapper.vm.save({ valid: true, values: { ...clientFixture } })

    expect(toastAddMock).toHaveBeenCalledWith(
      expect.objectContaining({ severity: 'error', summary: 'Falha de Gravação de Empresa Cliente' })
    )
  })
})

// ==================================================================
// formValidator (schema zod real)
// ------------------------------------------------------------------
// O <Form> é substituído por um stub que nunca chama o resolver de
// verdade, então os .refine()/.transform() do schema (incluindo a
// normalização do telefone) nunca executam via save()/onMounted().
// Chamamos `formValidator` diretamente — é o resolver real
// (zodResolver + zod, dependências reais do projeto) — para exercitar
// a validação de fato.
// ==================================================================
describe('formValidator', () => {
  it('valida com sucesso quando todos os campos obrigatórios estão preenchidos corretamente', async () => {
    const wrapper = await mountComponent({})

    const result = await wrapper.vm.formValidator({
      values: { ...clientFixture, fone: '(51) 99999-9999' }
    })

    expect(result.errors).toEqual({})
  })

  it('acusa erro em cada campo obrigatório quando vazio', async () => {
    const wrapper = await mountComponent({})

    const result = await wrapper.vm.formValidator({
      values: { razaoSocial: '', nome: '', cnpj: '', fone: '', endereco: '', bairro: '', cep: '', cidade: '', uf: '' }
    })

    expect(result.errors.razaoSocial[0].message).toBe('Razão Social é obrigatório.')
    expect(result.errors.nome[0].message).toBe('Nome de Fantasia é obrigatório.')
    expect(result.errors.cnpj[0].message).toBe('CNPJ é obrigatório.')
    expect(result.errors.fone[0].message).toBe('Fone é obrigatório.')
    expect(result.errors.endereco[0].message).toBe('Endereço é obrigatório.')
    expect(result.errors.bairro[0].message).toBe('Bairro é obrigatório.')
    expect(result.errors.cep[0].message).toBe('CEP é obrigatório.')
    expect(result.errors.cidade[0].message).toBe('Cidade é obrigatório.')
    expect(result.errors.uf[0].message).toBe('UF é obrigatório.')
  })

  it('acusa erro quando o telefone não tem DDD + 8 ou 9 dígitos', async () => {
    const wrapper = await mountComponent({})

    const result = await wrapper.vm.formValidator({
      values: { ...clientFixture, fone: '123' }
    })

    expect(result.errors.fone[0].message).toBe('O telefone deve conter DDD + (8 ou 9) dígitos.')
  })
})

// ==================================================================
// Template
// ==================================================================
describe('Template', () => {
  it('mostra "Inserir Empresa Cliente" sem id e "Editar Empresa Cliente" com id', async () => {
    const semId = await mountComponent({})
    expect(semId.text()).toContain('Inserir Empresa Cliente')

    const comId = await mountComponent({ id: '5' })
    expect(comId.text()).toContain('Editar Empresa Cliente')
  })

  it('navega para a lista de empresas ao clicar no botão de voltar', async () => {
    const wrapper = await mountComponent({})

    await wrapper.find('[icon="pi pi-replay"]').trigger('click')

    expect(routerPushMock).toHaveBeenCalledWith('/register/company')
  })

  it('exibe a mensagem de erro de todos os campos do formulário quando estão inválidos', async () => {
    const wrapper = await mountComponent({})
    const campos = ['razaoSocial', 'nome', 'cnpj', 'fone', 'endereco', 'bairro', 'cep', 'cidade', 'uf']

    campos.forEach((campo) => wrapper.vm.form.setFieldInvalid(campo, `${campo} inválido`))
    await nextTick()

    expect(wrapper.findAll('message-teststub').length).toBe(campos.length)
  })

  it('salva a empresa cliente disparando o submit real do formulário (botão "Salvar")', async () => {
    const wrapper = await mountComponent({ id: '5' })
    api.post.mockResolvedValueOnce({ status: 200, data: { id: 5 } })

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(api.post).toHaveBeenCalledWith('/client', expect.anything())
  })

  it('reseta o formulário via evento nativo de reset (botão "Limpar")', async () => {
    const wrapper = await mountComponent({ id: '5' })
    await flushPromises()

    expect(wrapper.vm.form.states.razaoSocial.value).toBe(clientFixture.razaoSocial)

    await wrapper.find('form').trigger('reset')

    expect(wrapper.vm.form.states.razaoSocial.value).toBe('')
  })

  it('escreve no campo CNPJ através do v-model="$field.value"', async () => {
    const wrapper = await mountComponent({})

    const cnpjInput = wrapper.find('#cnpj')
    expect(cnpjInput.exists()).toBe(true)

    await cnpjInput.setValue('98.765.432/0001-10')

    expect(wrapper.vm.form.states.cnpj.value).toBe('98.765.432/0001-10')
  })

  it('repassa o id atual para os componentes Contact e Employee', async () => {
    const wrapper = await mountComponent({ id: '5' })

    expect(wrapper.findComponent({ name: 'Contact' }).props('id')).toBe('5')
    expect(wrapper.findComponent({ name: 'Employee' }).props('id')).toBe('5')
  })

  it('ignora propriedades que não sejam strings ao sanitizar os parâmetros', async () => {
    const wrapper = await mountComponent({ id: '5' })
    api.post.mockResolvedValueOnce({ status: 200, data: { id: 5 } })

    const values = { 
      ...clientFixture, 
      razaoSocial: ' Empresa Teste ', 
      campoNaoString: 12345
    }
    
    await wrapper.vm.save({ valid: true, values })

    const [, paramsSent] = api.post.mock.calls[0]
    expect(paramsSent.campoNaoString).toBe(12345)
    expect(api.post).toHaveBeenCalled()
  })
})
