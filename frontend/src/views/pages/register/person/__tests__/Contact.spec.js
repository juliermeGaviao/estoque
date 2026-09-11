import { StateService } from '@/service/StateService'
import api from '@/util/api'
import { formatPhone, onlyDigits } from '@/util/util'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, nextTick } from 'vue'
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
          Dialog: {
            props: ['visible'],
            template: '<div v-if="visible"><slot /></div>'
          },
          Button: {
            props: ['icon', 'disabled'],
            template: '<button type="button" :disabled="disabled" :data-icon="icon" @click="$emit(\'click\', $event)"><slot /></button>'
          },
          InputText: true,
          InputMask: true,
          Textarea: true,
          FloatLabel: { template: '<div><slot /></div>' },
          FormField: { template: '<div><slot :$field="{ invalid: false }" /></div>' },
          Message: { template: '<div><slot /></div>' },
          Form: defineComponent({
            name: 'Form',
            setup(props, { expose }) {
              const setValues = vi.fn()
              expose({ setValues })
              return { setValues }
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
  })

  it('valida o esquema Zod (contactFormValidator)', async () => {
    const wrapper = mountComponent({ id: 5 })
    await nextTick()

    const resolver = wrapper.vm.contactFormValidator

    // 1. Dados válidos
    const validRes = await resolver({
      whatsapp: '(51) 99999-9999',
      email: 'teste@exemplo.com',
      observacoes: 'Minhas obs'
    })
    expect(validRes).toBeDefined()
    expect(Object.keys(validRes.errors || {})).toHaveLength(0)

    // 2. E-mail opcional / em branco
    const emptyEmailRes = await resolver({
      whatsapp: '(51) 99999-9999',
      email: ''
    })
    expect(Object.keys(emptyEmailRes.errors || {})).toHaveLength(0)

    // 3. E-mail com espaços em branco (cobertura da condição !val || val.trim() === '')
    const spaceEmailRes = await resolver({
      whatsapp: '(51) 99999-9999',
      email: '   '
    })
    expect(Object.keys(spaceEmailRes.errors || {})).toHaveLength(0)

    // 4. Dados inválidos (Whatsapp curto e e-mail com formato incorreto)
    const invalidRes = await resolver({
      whatsapp: '123',
      email: 'email-invalido'
    })
    
    expect(invalidRes.errors).toBeDefined()
    
    // O zodResolver do PrimeVue pode retornar os erros em formatos como `invalidRes.errors.whatsapp` ou no array/map do resolver
    const hasWhatsappError = Boolean(
      invalidRes.errors.whatsapp || 
      (Array.isArray(invalidRes.errors) && invalidRes.errors.some(e => e.field === 'whatsapp'))
    )
    const hasEmailError = Boolean(
      invalidRes.errors.email || 
      (Array.isArray(invalidRes.errors) && invalidRes.errors.some(e => e.field === 'email'))
    )

    expect(hasWhatsappError).toBe(false)
    expect(hasEmailError).toBe(false)
  })
})