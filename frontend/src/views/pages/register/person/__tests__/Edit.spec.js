import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, h, nextTick } from 'vue'
import { StateService } from '../../../../../service/StateService'
import api from '../../../../../util/api'
import { onlyDigits } from '../../../../../util/util'
import Edit from '../Edit.vue'

const mockToastAdd = vi.fn()
const mockConfirmRequire = vi.fn()
const mockRouterPush = vi.fn()
let mockRouteQuery = {}

vi.mock('primevue/usetoast', () => ({
  useToast: () => ({ add: mockToastAdd })
}))

vi.mock('primevue/useconfirm', () => ({
  useConfirm: () => ({ require: mockConfirmRequire })
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({ query: mockRouteQuery }),
  useRouter: () => ({ push: mockRouterPush })
}))

vi.mock('@/util/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    delete: vi.fn()
  }
}))

vi.mock('@/service/StateService', () => ({
  StateService: {
    getStates: vi.fn().mockResolvedValue([{ name: 'Rio Grande do Sul', code: 'RS' }])
  }
}))

vi.mock('@/util/util', () => ({
  formatPhone: vi.fn((v) => `FONE: ${v}`),
  onlyDigits: vi.fn((v) => (v ? String(v).replace(/\D/g, '') : ''))
}))

describe('src/views/pages/register/person/Edit.vue', () => {
  const mockPerson = {
    nome: 'Maria Silva',
    cracha: '12345',
    limite: 500,
    fone: '(51) 99999-9999',
    dataAniversario: '1990-05-15',
    endereco: 'Rua das Flores, 123',
    bairro: 'Centro',
    cep: '90000-000',
    cidade: 'Porto Alegre',
    uf: 'RS'
  }

  const mockContactsResponse = {
    data: {
      content: [
        {
          id: 10,
          nome: 'Contato 1',
          whatsapp: '(51) 98888-8888',
          email: 'maria@teste.com',
          dataAniversario: '1990-05-15',
          observacoes: 'Obs contato'
        }
      ],
      totalElements: 1
    }
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
      if (url === '/client') return Promise.resolve({ data: { ...mockPerson, empresa: { id: 2 } } })
      if (url === '/person-client-contact/list') return Promise.resolve(mockContactsResponse)
      if (url === '/client/list-companies') return Promise.resolve(mockCompaniesResponse)
      return Promise.resolve({ data: {} })
    })
  })

  function mountComponent() {
    return mount(Edit, {
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
                <slot name="body" :data="{ id: 10, whatsapp: '51988888888' }" />
              </div>
            `
          },
          Button: {
            props: ['label', 'icon'],
            template: '<button type="button" :data-icon="icon" @click="$emit(\'click\', $event)">{{ label }}<slot /></button>'
          },
          InputText: true,
          InputNumber: true,
          InputMask: true,
          DatePicker: true,
          Textarea: true,
          Select: true,
          Message: {
            name: 'Message',
            render() {
              return h('span', { class: 'message-stub' }, this.$slots.default ? this.$slots.default() : null)
            }
          },
          ConfirmDialog: true,
          Dialog: { template: '<div><slot /></div>' },
          FloatLabel: { template: '<div><slot /></div>' },
          Form: defineComponent({
            name: 'Form',
            template: '<form @submit.prevent="$emit(\'submit\')" @reset="$emit(\'reset\')"><slot /></form>',
            setup() {
              const setValues = vi.fn()
              const reset = vi.fn()
              return { setValues, reset }
            }
          }),
          FormField: {
            name: 'FormField',
            props: ['name'],
            render() {
              const $field = {
                invalid: true,
                error: { message: `Erro no campo ${this.name}` },
                value: ''
              }
              return this.$slots.default ? this.$slots.default({ $field }) : null
            }
          }
        },
        directives: {
          tooltip: {}
        }
      }
    })
  }

  it('coberta total das condicionais Zod do personFormValidator', async () => {
    const wrapper = mountComponent()
    const validator = wrapper.vm.personFormValidator

    // Date válido
    let res = await validator({ values: { nome: 'Teste', dataAniversario: new Date() } })
    expect(Object.keys(res.errors || {}).length).toBe(0)

    // String data válida
    res = await validator({ values: { nome: 'Teste', dataAniversario: '1990-01-01' } })
    expect(Object.keys(res.errors || {}).length).toBe(0)

    // Falsy ou vazio
    res = await validator({ values: { nome: 'Teste', dataAniversario: null } })
    expect(Object.keys(res.errors || {}).length).toBeGreaterThan(0)

    res = await validator({ values: { nome: 'Teste', dataAniversario: '' } })
    expect(Object.keys(res.errors || {}).length).toBeGreaterThan(0)

    // Date/String inválidos (NaN)
    res = await validator({ values: { nome: 'Teste', dataAniversario: new Date('invalid') } })
    expect(Object.keys(res.errors || {}).length).toBeGreaterThan(0)

    res = await validator({ values: { nome: 'Teste', dataAniversario: 'invalid_date' } })
    expect(Object.keys(res.errors || {}).length).toBeGreaterThan(0)

    // Tipos não cobertos (boolean/number)
    res = await validator({ values: { nome: 'Teste', dataAniversario: 12345 } })
    expect(Object.keys(res.errors || {}).length).toBeGreaterThan(0)
  })

  it('coberta total das condicionais Zod do contactFormValidator', async () => {
    const wrapper = mountComponent()
    const validator = wrapper.vm.contactFormValidator

    // Whatsapp e E-mail válidos
    let res = await validator({ values: { whatsapp: '(51) 99999-9999', email: 'email@valido.com' } })
    expect(Object.keys(res.errors || {}).length).toBe(0)

    // E-mail em branco
    res = await validator({ values: { whatsapp: '(51) 99999-9999', email: '   ' } })
    expect(Object.keys(res.errors || {}).length).toBe(0)

    // E-mail no formato inválido
    res = await validator({ values: { whatsapp: '(51) 99999-9999', email: 'email-invalido' } })
    expect(Object.keys(res.errors || {}).length).toBeGreaterThan(0)

    // Whatsapp insuficiente (< 15 caracteres)
    res = await validator({ values: { whatsapp: '123' } })
    expect(Object.keys(res.errors || {}).length).toBeGreaterThan(0)
  })

  it('carrega dados no onMounted quando ID existe e trata branches de sucesso', async () => {
    mockRouteQuery = { id: '5' }
    const wrapper = mountComponent()
    await nextTick()
    await nextTick()

    expect(StateService.getStates).toHaveBeenCalled()
    expect(api.get).toHaveBeenCalledWith('/client', { params: { id: '5' } })
    expect(api.get).toHaveBeenCalledWith('/client/list-companies', { params: { page: 0, size: 10000, sort: 'nome,asc' } })
    expect(wrapper.vm.personForm.setValues).toHaveBeenCalled()
  })

  it('trata os cenários de catch em load, loadContacts e loadCompanies', async () => {
    mockRouteQuery = { id: '5' }
    api.get.mockImplementation((url) => {
      if (url === '/client') return Promise.reject({ response: { data: 'Erro cliente' } })
      if (url === '/person-client-contact/list') return Promise.reject({ response: { data: 'Erro contatos' } })
      if (url === '/client/list-companies') return Promise.reject({ response: { data: 'Erro empresas' } })
      return Promise.resolve({ data: {} })
    })

    const wrapper = mountComponent()
    await nextTick()
    await nextTick()

    await wrapper.vm.loadContacts()

    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Carga de Pessoa Cliente' }))
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Carga de Empresas' }))
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Carga de Contatos da Pessoa Cliente' }))
  })

  it('retorna precocemente quando valid é false em save e saveContact', async () => {
    const wrapper = mountComponent()
    await nextTick()

    await wrapper.vm.save({ valid: false, values: {} })
    expect(api.post).not.toHaveBeenCalled()

    await wrapper.vm.saveContact({ valid: false, values: {} })
    expect(api.post).not.toHaveBeenCalled()
  })

  it('salva pessoa cliente (save) com e sem idEmpresa cobrindo formatação e tratamentos de erro', async () => {
    mockRouteQuery = { id: '5' }
    const wrapper = mountComponent()
    await nextTick()

    // Sucesso com idEmpresa
    api.post.mockResolvedValueOnce({ status: 200, data: { id: 5 } })
    await wrapper.vm.save({
      valid: true,
      values: { ...mockPerson, idEmpresa: 2 }
    })

    expect(onlyDigits).toHaveBeenCalled()
    expect(api.post).toHaveBeenCalledWith('/client', expect.objectContaining({
      id: 5,
      empresa: { id: 2 }
    }))
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ severity: 'success' }))

    // Sucesso sem idEmpresa (sem passar `empresa` no payload)
    api.post.mockResolvedValueOnce({ status: 200, data: { id: 5 } })
    await wrapper.vm.save({
      valid: true,
      values: { ...mockPerson, idEmpresa: null, empresa: undefined }
    })
    
    const secondCallPayload = api.post.mock.calls[1][1]
    expect(secondCallPayload.empresa).toBeUndefined()

    // Erro ao salvar
    api.post.mockRejectedValueOnce({ response: { data: 'Erro post' } })
    await wrapper.vm.save({ valid: true, values: mockPerson })
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Gravação de Pessoa Cliente' }))
  })

  it('executa a paginação (onPage) e ordenação (onSort) de contatos com asc e desc', async () => {
    mockRouteQuery = { id: '5' }
    const wrapper = mountComponent()
    await nextTick()

    api.get.mockClear()

    wrapper.vm.onPage({ page: 2, rows: 40 })
    expect(api.get).toHaveBeenLastCalledWith('/person-client-contact/list', {
      params: { idPessoa: '5', page: 2, size: 40 }
    })

    wrapper.vm.onSort({ sortField: 'email', sortOrder: 1 })
    expect(api.get).toHaveBeenLastCalledWith('/person-client-contact/list', {
      params: { idPessoa: '5', page: 0, size: 40, sort: 'email,asc' }
    })

    wrapper.vm.onSort({ sortField: 'email', sortOrder: -1 })
    expect(api.get).toHaveBeenLastCalledWith('/person-client-contact/list', {
      params: { idPessoa: '5', page: 0, size: 40, sort: 'email,desc' }
    })
  })

  it('manipula a abertura do formulário de contato (edit) para novo e existente', async () => {
    mockRouteQuery = { id: '5' }
    const wrapper = mountComponent()
    await nextTick()

    // Edição de contato
    wrapper.vm.edit(mockContactsResponse.data.content[0])
    await nextTick()
    await nextTick()

    expect(wrapper.vm.visible).toBe(true)

    // Novo contato
    wrapper.vm.edit(null)
    await nextTick()
    await nextTick()

    expect(wrapper.vm.visible).toBe(true)
  })

  it('salva contato (saveContact) com idContact e sem idContact tratando erro e bloco finally', async () => {
    mockRouteQuery = { id: '5' }
    const wrapper = mountComponent()
    await nextTick()

    // Com idContact (editar)
    wrapper.vm.edit(mockContactsResponse.data.content[0])
    await nextTick()

    api.post.mockResolvedValueOnce({ status: 200 })
    await wrapper.vm.saveContact({
      valid: true,
      values: { whatsapp: '(51) 98888-8888', email: 'a@a.com', observacoes: ' Obs ' }
    })

    expect(api.post).toHaveBeenCalledWith('/person-client-contact', expect.objectContaining({
      id: 10,
      cliente: { id: '5' },
      observacoes: 'Obs'
    }))
    expect(wrapper.vm.visible).toBe(false)

    // Trata erro na gravação do contato
    api.post.mockRejectedValueOnce({ response: { data: 'Erro salvando contato' } })
    await wrapper.vm.saveContact({ valid: true, values: { whatsapp: '123' } })
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Gravação de Contato de Pessoa Cliente' }))
  })

  it('exclui contato (confirmDelete) com aceite e rejeição de erro', async () => {
    mockRouteQuery = { id: '5' }
    const wrapper = mountComponent()
    await nextTick()

    wrapper.vm.confirmDelete({ id: 10 })
    expect(mockConfirmRequire).toHaveBeenCalled()

    const confirmOptions = mockConfirmRequire.mock.calls[0][0]

    // Aceita e remove com sucesso
    api.delete.mockResolvedValueOnce({})
    await confirmOptions.accept()

    expect(api.delete).toHaveBeenCalledWith('/person-client-contact?id=10')
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ severity: 'success' }))

    // Aceita e falha na remoção
    wrapper.vm.confirmDelete({ id: 10 })
    api.delete.mockRejectedValueOnce({ response: { data: 'Erro ao deletar' } })
    await mockConfirmRequire.mock.calls[1][0].accept()

    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Remoção de Contato de Pessoa Cliente' }))
  })

  it('dispara ações nos botões do template (roteamento e fechamento de modal)', async () => {
    mockRouteQuery = { id: '5' }
    const wrapper = mountComponent()
    await nextTick()

    // Botoes de navegar para voltar
    const buttons = wrapper.findAll('button[data-icon="pi pi-replay"]')
    for (const btn of buttons) {
      await btn.trigger('click')
    }
    expect(mockRouterPush).toHaveBeenCalledWith('/register/person')

    // Fechar modal no botão Cancelar
    wrapper.vm.visible = true
    await nextTick()

    const cancelBtn = wrapper.findAll('button').find((b) => b.text().includes('Cancelar'))
    if (cancelBtn) {
      await cancelBtn.trigger('click')
      expect(wrapper.vm.visible).toBe(false)
    }
  })
})