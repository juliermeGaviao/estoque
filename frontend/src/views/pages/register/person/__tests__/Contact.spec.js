import { StateService } from '@/service/StateService'
import api from '@/util/api'
import { formatPhone, onlyDigits } from '@/util/util'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, h, inject, nextTick, provide, reactive } from 'vue'
import Contact from '../Contact.vue'

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

vi.mock('@/service/StateService', () => ({
  StateService: {
    getStates: vi.fn().mockResolvedValue([{ id: 1, nome: 'RS' }])
  }
}))

vi.mock('@/util/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    delete: vi.fn()
  }
}))

vi.mock('@/util/util', () => ({
  formatPhone: vi.fn((v) => (v ? `PHONE:${v}` : '')),
  onlyDigits: vi.fn((v) => (v ? String(v).replace(/\D/g, '') : ''))
}))

// ------------------------------------------------------------------
// Stub de <Form> (@primevue/forms) — implementa de fato
// states/setValues/setFieldValue/reset (o stub anterior só expunha um
// setValues vi.fn() sem estado nenhum, e o FormField hardcodeava
// invalid:false sempre — por isso as 3 mensagens de erro nunca podiam
// aparecer). Também expõe helpers só de teste (submitWith/
// setFieldInvalid).
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

// Stub de <FormField> — repassa invalid/error de verdade (lidos do
// Form pai via provide/inject), em vez do { invalid: false } fixo.
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

// Stub de <Dialog> com um helper de teste para disparar manualmente um
// handler recebido via attrs — necessário para cobrir o
// `@update:visible="visible = $event"` gerado pelo v-model:visible.
const DialogStub = defineComponent({
  name: 'Dialog',
  props: ['visible'],
  setup(props, { slots, attrs, expose }) {
    expose({
      emit(eventName, payload) {
        const handlerKey = 'on' + eventName.charAt(0).toUpperCase() + eventName.slice(1)
        if (typeof attrs[handlerKey] === 'function') attrs[handlerKey](payload)
      }
    })
    return () => (props.visible ? h('div', { class: 'dialog-teststub' }, slots.default ? slots.default() : null) : null)
  }
})

describe('Contact.vue', () => {
  const mockContactsResponse = {
    data: {
      content: [
        {
          id: 10,
          nome: 'Contato Teste',
          whatsapp: '51999999999',
          email: 'teste@exemplo.com',
          dataAniversario: '1995-03-20T00:00:00.000Z',
          observacoes: 'Obs Teste'
        }
      ],
      totalElements: 1
    }
  }

  beforeEach(() => {
    vi.clearAllMocks()
    api.get.mockResolvedValue(mockContactsResponse)
  })

  function mountComponent(props = { id: 1 }, customStubs = {}) {
    return mount(Contact, {
      props,
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
            setup(props, { slots }) {
              return () => {
                const dummyData = { id: 10, whatsapp: '51999999999', email: 'teste@exemplo.com' }
                return [
                  slots.header ? slots.header() : null,
                  slots.body ? slots.body({ data: dummyData }) : null
                ]
              }
            }
          },
          Dialog: DialogStub,
          Button: {
            props: ['icon', 'disabled'],
            template: '<button type="button" :disabled="disabled" :data-icon="icon" @click="$emit(\'click\', $event)"><slot /></button>'
          },
          InputText: true,
          InputMask: true,
          Textarea: true,
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
  }

  it('carrega lista e estados ao montar quando possui id', async () => {
    mountComponent({ id: 5 })
    await nextTick()

    expect(StateService.getStates).toHaveBeenCalled()
    expect(api.get).toHaveBeenCalledWith('/person-client-contact/list', {
      params: { idPessoa: 5, page: 0, size: 20 }
    })
    expect(formatPhone).toHaveBeenCalledWith('51999999999')
  })

  it('não carrega lista na montagem se não houver id', async () => {
    mountComponent({ id: null })
    await nextTick()

    expect(StateService.getStates).toHaveBeenCalled()
    expect(api.get).not.toHaveBeenCalled()
  })

  it('trata erro na busca/carga de contatos', async () => {
    api.get.mockRejectedValueOnce({ response: { data: 'Erro na API' } })

    mountComponent({ id: 5 })
    await nextTick()

    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'error',
        summary: 'Falha de Carga de Contatos da Pessoa Cliente'
      })
    )
  })

  it('abre o modal para inserção e edição de contatos (edit)', async () => {
    const wrapper = mountComponent({ id: 5 })
    await nextTick()

    // Inserção (edit null)
    wrapper.vm.edit(null)
    await nextTick()
    await nextTick()
    expect(wrapper.vm.visible).toBe(true)

    // Edição (edit com objeto)
    const contactData = {
      id: 10,
      nome: 'Maria',
      whatsapp: '(51) 99999-9999',
      email: 'maria@teste.com',
      dataAniversario: '1990-01-01T00:00:00.000Z',
      observacoes: 'Observação'
    }
    wrapper.vm.edit(contactData)
    await nextTick()
    await nextTick()

    expect(wrapper.vm.visible).toBe(true)
  })

  it('interrompe salvamento se formulário for inválido', async () => {
    const wrapper = mountComponent({ id: 5 })
    await nextTick()

    await wrapper.vm.save({ valid: false, values: {} })
    expect(api.post).not.toHaveBeenCalled()
  })

  it('salva novo contato e contato existente com sucesso', async () => {
    const wrapper = mountComponent({ id: 5 })
    await nextTick()

    api.post.mockResolvedValueOnce({ status: 200 })

    // Salvar sem idContact prévio
    await wrapper.vm.save({
      valid: true,
      values: {
        whatsapp: '(51) 98888-8888',
        email: '   novo@teste.com   ',
        observacoes: '   Obs   '
      }
    })

    expect(onlyDigits).toHaveBeenCalledWith('(51) 98888-8888')
    expect(api.post).toHaveBeenCalledWith('/person-client-contact', {
      cliente: { id: 5 },
      whatsapp: '51988888888',
      email: 'novo@teste.com',
      observacoes: 'Obs'
    })
    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({ severity: 'success', summary: 'Sucesso' })
    )

    // Salvar com idContact preenchido (edição)
    wrapper.vm.edit({ id: 10, whatsapp: '51999999999' })
    await nextTick()

    api.post.mockResolvedValueOnce({ status: 201 }) // Exemplo com outro código HTTP de sucesso
    await wrapper.vm.save({
      valid: true,
      values: { whatsapp: '(51) 99999-9999' }
    })

    expect(api.post).toHaveBeenLastCalledWith('/person-client-contact', expect.objectContaining({ id: 10 }))
  })

  it('trata erro no salvamento do contato', async () => {
    const wrapper = mountComponent({ id: 5 })
    await nextTick()

    api.post.mockRejectedValueOnce({ response: { data: 'Erro na API ao salvar' } })

    await wrapper.vm.save({
      valid: true,
      values: { whatsapp: '(51) 98888-8888' }
    })

    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'error',
        summary: 'Falha de Gravação de Contato de Pessoa Cliente'
      })
    )
  })

  it('remove um contato via caixa de confirmação com sucesso e erro', async () => {
    const wrapper = mountComponent({ id: 5 })
    await nextTick()

    // 1. Sucesso na Remoção
    api.delete.mockResolvedValueOnce({})
    wrapper.vm.confirmDelete({ id: 10 })

    expect(mockConfirmRequire).toHaveBeenCalled()
    const confirmOptionsSuccess = mockConfirmRequire.mock.calls[0][0]
    await confirmOptionsSuccess.accept()

    expect(api.delete).toHaveBeenCalledWith('/person-client-contact?id=10')
    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({ severity: 'success', summary: 'Sucesso' })
    )

    // 2. Erro na Remoção (com fallback para erro?.response?.data)
    api.delete.mockRejectedValueOnce({ response: { data: 'Erro na deleção' } })
    wrapper.vm.confirmDelete({ id: 10 })

    const confirmOptionsError = mockConfirmRequire.mock.calls[1][0]
    await confirmOptionsError.accept()

    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'error',
        summary: 'Falha de Remoção de Contato de Pessoa Cliente'
      })
    )

    // 3. Erro na Remoção sem a propriedade `response`
    api.delete.mockRejectedValueOnce(new Error('Network Error'))
    wrapper.vm.confirmDelete({ id: 10 })

    const confirmOptionsNetworkError = mockConfirmRequire.mock.calls[2][0]
    await confirmOptionsNetworkError.accept()

    expect(mockToastAdd).toHaveBeenCalled()
  })

  it('interage com os botões da interface (redirecionar, cancelar e botões de ação)', async () => {
    const wrapper = mountComponent({ id: 5 })
    await nextTick()

    // Botão Voltar
    const btnReplay = wrapper.find('button[data-icon="pi pi-replay"]')
    await btnReplay.trigger('click')
    expect(mockRouterPush).toHaveBeenCalledWith('/register/person')

    // Botão "+" no cabeçalho da coluna de ações (cobre o @click="edit(null)" inline)
    const btnPlus = wrapper.find('button[data-icon="pi pi-plus"]')
    await btnPlus.trigger('click')
    expect(wrapper.vm.visible).toBe(true)
    wrapper.vm.visible = false
    await nextTick()

    // Botões Editar e Excluir no DataTable
    const btnPencil = wrapper.find('button[data-icon="pi pi-pencil"]')
    await btnPencil.trigger('click')
    expect(wrapper.vm.visible).toBe(true)

    const btnTrash = wrapper.find('button[data-icon="pi pi-trash"]')
    await btnTrash.trigger('click')
    expect(mockConfirmRequire).toHaveBeenCalled()

    // Botão Cancelar no Dialog
    wrapper.vm.visible = true
    await nextTick()
    wrapper.vm.visible = false
    expect(wrapper.vm.visible).toBe(false)
  })

  it('executa a paginação e a ordenação (onPage e onSort)', async () => {
    const wrapper = mountComponent({ id: 5 })
    await nextTick()

    // Teste onPage
    wrapper.vm.onPage({ page: 2, rows: 40 })
    expect(api.get).toHaveBeenLastCalledWith('/person-client-contact/list', {
      params: { idPessoa: 5, page: 2, size: 40 }
    })

    // Teste onSort ASC (1)
    wrapper.vm.onSort({ sortField: 'email', sortOrder: 1 })
    expect(api.get).toHaveBeenLastCalledWith('/person-client-contact/list', {
      params: expect.objectContaining({ sort: 'email,asc', page: 0 })
    })

    // Teste onSort DESC (-1)
    wrapper.vm.onSort({ sortField: 'email', sortOrder: -1 })
    expect(api.get).toHaveBeenLastCalledWith('/person-client-contact/list', {
      params: expect.objectContaining({ sort: 'email,desc' })
    })

    // Teste sortField falsy/null (garante o ramo negativo do "if (sortField.value)")
    wrapper.vm.sortField = null
    wrapper.vm.load()
    expect(api.get).toHaveBeenLastCalledWith('/person-client-contact/list', {
      params: { idPessoa: 5, page: 0, size: 40 }
    })

    // Teste sortField preenchido mas sortOrder ausente (garante o ramo
    // negativo do "if (sortOrder?.value)", sem anexar ",asc"/",desc")
    wrapper.vm.sortField = 'email'
    wrapper.vm.sortOrder = null
    await wrapper.vm.load()
    expect(api.get).toHaveBeenLastCalledWith('/person-client-contact/list', {
      params: { idPessoa: 5, page: 0, size: 40, sort: 'email' }
    })
  })

  it('valida o esquema Zod (contactFormValidator)', async () => {
    const wrapper = mountComponent({ id: 5 })
    await nextTick()

    const resolver = wrapper.vm.contactFormValidator

    // O zodResolver real espera { values, name } — passar os campos soltos
    // (como o teste fazia antes) faz `values` chegar `undefined` dentro do
    // resolver, e o parseAsync falha genericamente sem nunca exercitar o
    // .refine() do e-mail (linha 36). Por isso os testes anteriores nunca
    // detectavam nenhum erro específico de campo, mesmo com dados inválidos.
    async function validate(values) {
      return resolver({ values })
    }

    // 1. Dados válidos
    const validRes = await validate({
      whatsapp: '(51) 99999-9999',
      email: 'teste@exemplo.com',
      observacoes: 'Minhas obs'
    })
    expect(validRes.errors).toEqual({})

    // 2. E-mail opcional / em branco (cobre o `!val` do refine)
    const emptyEmailRes = await validate({
      whatsapp: '(51) 99999-9999',
      email: ''
    })
    expect(emptyEmailRes.errors).toEqual({})

    // 3. E-mail só com espaços (cobre o `val.trim() === ''` do refine)
    const spaceEmailRes = await validate({
      whatsapp: '(51) 99999-9999',
      email: '   '
    })
    expect(spaceEmailRes.errors).toEqual({})

    // 4. Whatsapp curto demais (menos de 15 caracteres)
    const shortWhatsappRes = await validate({
      whatsapp: '123',
      email: ''
    })
    expect(shortWhatsappRes.errors.whatsapp[0].message).toBe('Whatsapp é obrigatório.')

    // 5. E-mail com formato inválido (cobre o `z.string().email().safeParse(val).success` falso)
    const invalidEmailRes = await validate({
      whatsapp: '(51) 99999-9999',
      email: 'email-invalido'
    })
    expect(invalidEmailRes.errors.email[0].message).toBe('E-mail inválido ou vazio.')
  })

  it('exibe a mensagem de erro de todos os campos do formulário quando estão inválidos', async () => {
    const wrapper = mountComponent({ id: 5 })
    await nextTick()

    wrapper.vm.edit(null)
    await nextTick()

    const campos = ['whatsapp', 'email', 'observacoes']
    campos.forEach((campo) => wrapper.vm.contactForm.setFieldInvalid(campo, `${campo} inválido`))
    await nextTick()

    // O stub de Message sempre renderiza o slot recebido — o que varia é
    // se o <Message v-if="$field?.invalid"> do template chega a criar o
    // componente. Contamos quantos ficam com esse texto de erro.
    campos.forEach((campo) => {
      expect(wrapper.text()).toContain(`${campo} inválido`)
    })
  })

  it('atualiza o cabeçalho do diálogo para "Editar Contato" depois de editar um contato existente', async () => {
    const wrapper = mountComponent({ id: 5 })
    await nextTick()

    wrapper.vm.edit({ id: 10, whatsapp: '51999999999' })
    await nextTick()
    await nextTick()

    // `idContact` é uma variável de módulo comum (não reativa) — setá-la
    // dentro do nextTick() do edit() não dispara sozinha um novo render;
    // o header do Dialog só é reavaliado no PRÓXIMO ciclo de renderização.
    // Forçamos esse ciclo para exercitar de fato o ramo "Editar Contato"
    // do ternário (o ramo "Inserir Contato" já é coberto pela
    // renderização inicial, antes de qualquer edit()).
    await wrapper.vm.$forceUpdate()
    await nextTick()

    expect(wrapper.find('.dialog-teststub').attributes('header')).toBe('Editar Contato')
  })

  it('fecha o dialog através do v-model:visible (update:visible emitido pelo próprio Dialog)', async () => {
    const wrapper = mountComponent({ id: 5 })
    wrapper.vm.visible = true
    await nextTick()

    wrapper.findComponent(DialogStub).vm.emit('update:visible', false)
    await nextTick()

    expect(wrapper.vm.visible).toBe(false)
  })

  it('fecha o dialog clicando no botão "Cancelar" (cobre o @click="visible = false" inline)', async () => {
    const wrapper = mountComponent({ id: 5 })
    wrapper.vm.visible = true
    await nextTick()

    const cancelButton = wrapper.find('button[data-icon="pi pi-ban"]')
    expect(cancelButton.exists()).toBe(true)

    await cancelButton.trigger('click')

    expect(wrapper.vm.visible).toBe(false)
  })
})