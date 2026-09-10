// @vitest-environment jsdom
//
// Testes unitários para src/views/pages/register/company/Contact.vue
// ------------------------------------------------------------------
// Stack: Vitest + @vue/test-utils

import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'

import Contact from '../Contact.vue'

import api from '@/util/api'

// ------------------------------------------------------------------
// Mocks de módulos utilitários e de app
// ------------------------------------------------------------------

vi.mock('@/util/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    delete: vi.fn()
  }
}))

vi.mock('@/util/util', () => ({
  formatPhone: vi.fn((v) => (v ? `fmt(${v})` : '')),
  onlyDigits: vi.fn((v) => (v ? String(v).replace(/\D/g, '') : ''))
}))

const toastAddMock = vi.fn()
vi.mock('primevue/usetoast', () => ({
  useToast: () => ({ add: toastAddMock })
}))

const confirmRequireMock = vi.fn()
vi.mock('primevue/useconfirm', () => ({
  useConfirm: () => ({ require: confirmRequireMock })
}))

const routerPushMock = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: routerPushMock })
}))

// ------------------------------------------------------------------
// Stubs dos componentes do PrimeVue
// ------------------------------------------------------------------
const { FormStub, FormFieldStub, CardStub, DataTableStub, ColumnStub, DialogStub, genericStub, ButtonStub, FieldControlStub } = await vi.hoisted(async () => {
  const { defineComponent, h, inject, provide, reactive } = await import('vue')

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

  const DataTableStub = defineComponent({
    name: 'DataTable',
    props: ['value'],
    setup(props, { slots }) {
      return () =>
        h('div', { class: 'datatable-teststub' }, [
          slots.default ? slots.default() : null,
          Array.isArray(props.value)
            ? props.value.map((row, index) =>
                h('div', { key: index, class: 'datatable-row' }, [
                  slots.body ? slots.body({ data: row }) : null
                ])
              )
            : null
        ])
    }
  })

  const ColumnStub = defineComponent({
    name: 'Column',
    props: ['field', 'header'],
    setup(props, { slots }) {
      return () =>
        h('div', { class: 'column-teststub' }, [
          slots.header ? slots.header() : null,
          slots.body ? slots.body({ data: {} }) : null
        ])
    }
  })

  const DialogStub = defineComponent({
    name: 'Dialog',
    props: ['visible', 'header', 'modal', 'closable'],
    setup(props, { slots, attrs }) {
      return () => {
        if (!props.visible) return null
        
        // Se a prop header for uma função (gerada pela expressão :header="modalHeader()"), executa-a aqui
        const resolvedHeader = typeof props.header === 'function' ? props.header() : props.header

        return h(
          'div',
          {
            class: 'dialog-teststub',
            header: resolvedHeader
          },
          slots.default ? slots.default() : null
        )
      }
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

  return { FormStub, FormFieldStub, CardStub, DataTableStub, ColumnStub, DialogStub, genericStub, ButtonStub, FieldControlStub }
})

vi.mock('@primevue/forms/form', () => ({ default: FormStub }))
vi.mock('@primevue/forms/formfield', () => ({ default: FormFieldStub }))
vi.mock('primevue/card', () => ({ default: CardStub }))
vi.mock('primevue/datatable', () => ({ default: DataTableStub }))
vi.mock('primevue/column', () => ({ default: ColumnStub }))
vi.mock('primevue/dialog', () => ({ default: DialogStub }))
vi.mock('primevue/button', () => ({ default: ButtonStub }))
vi.mock('primevue/inputtext', () => ({ default: FieldControlStub }))
vi.mock('primevue/inputmask', () => ({ default: FieldControlStub }))
vi.mock('primevue/textarea', () => ({ default: FieldControlStub }))
vi.mock('primevue/datepicker', () => ({ default: genericStub('DatePicker') }))
vi.mock('primevue/message', () => ({ default: genericStub('Message') }))
vi.mock('primevue/floatlabel', () => ({ default: genericStub('FloatLabel') }))
vi.mock('primevue/confirmdialog', () => ({ default: genericStub('ConfirmDialog') }))

// ------------------------------------------------------------------
// Fixtures
// ------------------------------------------------------------------

const contactsFixture = [
  {
    id: 1,
    nome: 'João Silva',
    cargo: 'Gerente',
    fone: '5133334444',
    ramal: '123',
    celular: '51988887777',
    email: 'joao@empresa.com',
    dataAniversario: '1990-01-01',
    observacoes: 'Nenhuma'
  }
]

function rejectWith(message) {
  return Promise.reject({ response: { data: message } })
}

async function mountComponent(props = { id: 1 }) {
  const wrapper = mount(Contact, { props })
  await flushPromises()
  return wrapper
}

beforeEach(() => {
  vi.clearAllMocks()
  api.get.mockResolvedValue({
    data: {
      content: contactsFixture,
      totalElements: 1
    }
  })
  api.post.mockResolvedValue({ status: 200, data: { id: 10 } })
  api.delete.mockResolvedValue({ status: 200 })
})

afterEach(() => {
  vi.restoreAllMocks()
})

// ==================================================================
// onMounted / load() / Paginação / Ordenação
// ==================================================================
describe('onMounted & load', () => {
  it('carrega os contatos quando há id inicial', async () => {
    await mountComponent({ id: 10 })

    expect(api.get).toHaveBeenCalledWith('/company-client-contact/list', {
      params: { idEmpresa: 10, page: 0, size: 20 }
    })
  })

  it('não carrega os contatos quando não há id inicial', async () => {
    await mountComponent({ id: null })

    expect(api.get).not.toHaveBeenCalled()
  })

  it('exibe toast de erro quando a listagem falha', async () => {
    api.get.mockImplementation(() => rejectWith('Erro de listagem'))

    await mountComponent({ id: 1 })

    expect(toastAddMock).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'error',
        summary: 'Falha de Carga de Contatos da Empresa Cliente',
        detail: expect.stringContaining('Erro de listagem')
      })
    )
  })

  it('carrega com ordenação ascendente', async () => {
    const wrapper = await mountComponent({ id: 1 })
    
    wrapper.vm.sortField = 'nome'
    wrapper.vm.sortOrder = 1
    await wrapper.vm.load()

    expect(api.get).toHaveBeenCalledWith('/company-client-contact/list', {
      params: { idEmpresa: 1, page: 0, size: 20, sort: 'nome,asc' }
    })
  })

  it('carrega com ordenação descendente', async () => {
    const wrapper = await mountComponent({ id: 1 })
    
    wrapper.vm.sortField = 'nome'
    wrapper.vm.sortOrder = -1
    await wrapper.vm.load()

    expect(api.get).toHaveBeenCalledWith('/company-client-contact/list', {
      params: { idEmpresa: 1, page: 0, size: 20, sort: 'nome,desc' }
    })
  })

  it('executa onPage corretamente', async () => {
    const wrapper = await mountComponent({ id: 1 })

    wrapper.vm.onPage({ page: 2, rows: 40 })
    await flushPromises()

    expect(wrapper.vm.page).toBe(2)
    expect(wrapper.vm.size).toBe(40)
  })

  it('executa onSort corretamente', async () => {
    const wrapper = await mountComponent({ id: 1 })

    wrapper.vm.onSort({ sortField: 'cargo', sortOrder: 1 })
    await flushPromises()

    expect(wrapper.vm.page).toBe(0)
    expect(wrapper.vm.sortField).toBe('cargo')
    expect(wrapper.vm.sortOrder).toBe(1)
  })
})

// ==================================================================
// edit()
// ==================================================================
describe('edit', () => {
  it('abre o modal preenchendo os dados ao editar um contato existente', async () => {
    const wrapper = await mountComponent({ id: 1 })

    wrapper.vm.edit(contactsFixture[0])
    await nextTick()
    await flushPromises()

    expect(wrapper.vm.visible).toBe(true)
    expect(wrapper.vm.form.states.nome.value).toBe('João Silva')
  })

  it('abre o modal resetando os valores ao criar um novo contato', async () => {
    const wrapper = await mountComponent({ id: 1 })

    wrapper.vm.edit(null)
    await nextTick()
    await flushPromises()

    expect(wrapper.vm.visible).toBe(true)
    expect(wrapper.vm.form.states.nome.value).toBe('')
  })
})

// ==================================================================
// save()
// ==================================================================
describe('save', () => {
  it('não faz nada se o formulário for inválido', async () => {
    const wrapper = await mountComponent({ id: 1 })

    await wrapper.vm.save({ valid: false, values: {} })

    expect(api.post).not.toHaveBeenCalled()
  })

  it('salva com sucesso um novo contato (sem idContact prévio)', async () => {
    const wrapper = await mountComponent({ id: 1 })
    wrapper.vm.edit(null)
    await nextTick()

    await wrapper.vm.save({
      valid: true,
      values: {
        nome: 'Novo Contato',
        cargo: 'Analista',
        fone: '(51) 3333-2222',
        celular: '(51) 99999-8888',
        email: 'novo@email.com',
        observacoes: ' Teste '
      }
    })

    expect(api.post).toHaveBeenCalledWith(
      '/company-client-contact',
      expect.objectContaining({
        cliente: { id: 1 },
        fone: '5133332222',
        celular: '51999998888',
        observacoes: 'Teste'
      })
    )
    expect(toastAddMock).toHaveBeenCalledWith(expect.objectContaining({ severity: 'success', summary: 'Sucesso' }))
    expect(wrapper.vm.visible).toBe(false)
  })

  it('salva com sucesso atualizando um contato existente (com idContact)', async () => {
    const wrapper = await mountComponent({ id: 1 })
    wrapper.vm.edit(contactsFixture[0])
    await nextTick()

    await wrapper.vm.save({
      valid: true,
      values: {
        nome: 'João Editado',
        cargo: 'Diretor',
        email: 'joao.editado@empresa.com'
      }
    })

    expect(api.post).toHaveBeenCalledWith(
      '/company-client-contact',
      expect.objectContaining({
        id: 1,
        cliente: { id: 1 }
      })
    )
    expect(toastAddMock).toHaveBeenCalledWith(expect.objectContaining({ severity: 'success', summary: 'Sucesso' }))
  })

  it('exibe toast de erro quando a gravação falha', async () => {
    const wrapper = await mountComponent({ id: 1 })
    api.post.mockImplementationOnce(() => rejectWith('Erro ao salvar contato'))

    await wrapper.vm.save({
      valid: true,
      values: {
        nome: 'Erro',
        cargo: 'Cargo',
        email: 'erro@email.com'
      }
    })

    expect(toastAddMock).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'error',
        summary: 'Falha de Gravação de Contato de Empresa Cliente',
        detail: expect.stringContaining('Erro ao salvar contato')
      })
    )
  })
})

// ==================================================================
// confirmDelete()
// ==================================================================
describe('confirmDelete', () => {
  it('solicita confirmação e remove o contato com sucesso ao aceitar', async () => {
    const wrapper = await mountComponent({ id: 1 })

    wrapper.vm.confirmDelete(contactsFixture[0])

    expect(confirmRequireMock).toHaveBeenCalled()
    const config = confirmRequireMock.mock.calls[0][0]

    // Executa a função accept configurada no confirm dialog
    await config.accept()
    await flushPromises()

    expect(api.delete).toHaveBeenCalledWith('/company-client-contact?id=1')
    expect(toastAddMock).toHaveBeenCalledWith(expect.objectContaining({ severity: 'success', summary: 'Sucesso' }))
  })

  it('trata erro ao tentar remover o contato quando a exclusão falha', async () => {
    const wrapper = await mountComponent({ id: 1 })
    api.delete.mockImplementationOnce(() => rejectWith('Erro ao excluir'))

    wrapper.vm.confirmDelete(contactsFixture[0])

    const config = confirmRequireMock.mock.calls[0][0]
    await config.accept()
    await flushPromises()

    expect(toastAddMock).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'error',
        summary: 'Falha de Remoção de Contato de Empresa Cliente',
        detail: expect.stringContaining('Erro ao excluir')
      })
    )
  })
})

// ==================================================================
// formValidator (schema zod real)
// ==================================================================
describe('formValidator', () => {
  it('valida com sucesso quando os campos obrigatórios estão corretos', async () => {
    const wrapper = await mountComponent({ id: 1 })

    const result = await wrapper.vm.formValidator({
      values: {
        nome: 'Maria',
        cargo: 'Analista',
        email: 'maria@teste.com'
      }
    })

    expect(result.errors).toEqual({})
  })

  it('acusa erro quando os campos obrigatórios estão vazios ou inválidos', async () => {
    const wrapper = await mountComponent({ id: 1 })

    const result = await wrapper.vm.formValidator({
      values: {
        nome: '',
        cargo: '',
        email: 'email-invalido'
      }
    })

    expect(result.errors.nome[0].message).toBe('Nome do Contato é obrigatório.')
    expect(result.errors.cargo[0].message).toBe('Cargo é obrigatório.')
    expect(result.errors.email[0].message).toBe('E-mail inválido.')
  })
})

// ==================================================================
// Template & Interações da UI
// ==================================================================
describe('Template UI', () => {
  it('navega ao clicar no botão de voltar', async () => {
    const wrapper = await mountComponent({ id: 1 })

    await wrapper.find('[icon="pi pi-replay"]').trigger('click')

    expect(routerPushMock).toHaveBeenCalledWith('/register/company')
  })

  it('desabilita o botão de novo contato se não houver id', async () => {
    const wrapper = await mountComponent({ id: null })

    const plusButton = wrapper.find('[icon="pi pi-plus"]')
    expect(plusButton.attributes('disabled')).toBeDefined()
  })

  it('permite fechar o dialog clicando no botão cancelar', async () => {
    const wrapper = await mountComponent({ id: 1 })
    wrapper.vm.visible = true
    await nextTick()

    const cancelButton = wrapper.find('[icon="pi pi-ban"]')
    await cancelButton.trigger('click')

    expect(wrapper.vm.visible).toBe(false)
  })

  it('abre o modal de edição ao clicar no botão de lápis da linha (cobre o @click inline)', async () => {
    const wrapper = await mountComponent({ id: 1 })

    const editButton = wrapper.find('[icon="pi pi-pencil"]')
    expect(editButton.exists()).toBe(true)

    await editButton.trigger('click')

    expect(wrapper.vm.visible).toBe(true)
  })

  it('aciona a confirmação de remoção ao clicar no botão de lixeira da linha (cobre o @click inline)', async () => {
    const wrapper = await mountComponent({ id: 1 })

    const deleteButton = wrapper.find('[icon="pi pi-trash"]')
    expect(deleteButton.exists()).toBe(true)

    await deleteButton.trigger('click')

    expect(confirmRequireMock).toHaveBeenCalled()
  })

  it('atualiza o cabeçalho do diálogo para "Editar Contato" depois de editar um contato existente', async () => {
    const wrapper = await mountComponent({ id: 1 })

    wrapper.vm.edit(contactsFixture[0])
    await nextTick()
    await flushPromises()

    // `idContact` é uma variável de módulo comum (não reativa) — setá-la
    // dentro do nextTick() do edit() não dispara sozinha um novo render;
    // o header do Dialog só é reavaliado no PRÓXIMO ciclo de renderização
    // do componente. Forçamos esse ciclo para exercitar de fato o ramo
    // "Editar Contato" do ternário (o ramo "Inserir Contato" já é coberto
    // pela renderização inicial, antes de qualquer edit()).
    await wrapper.vm.$forceUpdate()
    await nextTick()

    // O DialogStub não declara `header` como prop, então ele cai em
    // $attrs e é aplicado como atributo do elemento raiz (não como texto
    // dentro do slot) — por isso checamos o atributo, não wrapper.text().
    expect(wrapper.find('.dialog-teststub').attributes('header')).toBe('Editar Contato')
  })

  it('exibe a mensagem de erro de todos os campos do formulário quando estão inválidos', async () => {
    const wrapper = await mountComponent({ id: 1 })
    wrapper.vm.visible = true
    await nextTick()

    const campos = ['nome', 'cargo', 'fone', 'ramal', 'celular', 'email', 'dataAniversario', 'observacoes']
    campos.forEach((campo) => wrapper.vm.form.setFieldInvalid(campo, `${campo} inválido`))
    await nextTick()

    expect(wrapper.findAll('message-teststub').length).toBe(campos.length)
  })

  it('permite interagir e alterar o valor de todos os campos do formulário no Dialog', async () => {
    const wrapper = await mountComponent({ id: 1 })
    wrapper.vm.visible = true
    await nextTick()

    const campos = ['nome', 'cargo', 'fone', 'ramal', 'celular', 'email', 'observacoes']

    for (const campo of campos) {
      wrapper.vm.form.setFieldValue(campo, `Valor ${campo}`)
      await nextTick()
    }

    expect(wrapper.vm.form.states.nome.value).toBe('Valor nome')
    expect(wrapper.vm.form.states.cargo.value).toBe('Valor cargo')
    expect(wrapper.vm.form.states.fone.value).toBe('Valor fone')
    expect(wrapper.vm.form.states.ramal.value).toBe('Valor ramal')
    expect(wrapper.vm.form.states.celular.value).toBe('Valor celular')
    expect(wrapper.vm.form.states.email.value).toBe('Valor email')
    expect(wrapper.vm.form.states.observacoes.value).toBe('Valor observacoes')
  })

  it('executa modalHeader com idContact nulo ao abrir para novo contato', async () => {
    const wrapper = await mountComponent({ id: 1 })

    wrapper.vm.edit(null)
    await nextTick()
    await flushPromises()

    expect(wrapper.vm.visible).toBe(true)
    expect(wrapper.find('.dialog-teststub').attributes('header')).toBe('Inserir Contato')
  })

  it('executa modalHeader com idContact preenchido ao abrir para edição', async () => {
    const wrapper = await mountComponent({ id: 1 })

    wrapper.vm.edit(contactsFixture[0])
    await nextTick()
    await flushPromises()

    expect(wrapper.vm.visible).toBe(true)
    expect(wrapper.find('.dialog-teststub').attributes('header')).toBe('Editar Contato')
  })

  it('cobre a renderização completa do Dialog ao abrir e fechar', async () => {
    const wrapper = await mountComponent({ id: 1 })

    wrapper.vm.visible = true
    await nextTick()
    await flushPromises()

    expect(wrapper.find('.dialog-teststub').exists()).toBe(true)

    wrapper.vm.visible = false
    await nextTick()
    
    expect(wrapper.find('.dialog-teststub').exists()).toBe(false)
  })
})
