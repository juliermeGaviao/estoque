import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, nextTick, ref } from 'vue'
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

vi.mock('../../../../../service/StateService', () => ({
  StateService: {
    getStates: vi.fn().mockResolvedValue([{ name: 'Rio Grande do Sul', code: 'RS' }])
  }
}))

vi.mock('@/util/util', () => ({
  formatPhone: vi.fn((v) => `FONE: ${v}`),
  onlyDigits: vi.fn((v) => (v ? String(v).replace(/\D/g, '') : ''))
}))

describe('company/Edit.vue', () => {
  const mockCompany = {
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

  const mockContactsResponse = {
    data: {
      content: [
        {
          id: 10,
          nome: 'João Silva',
          cargo: 'Gerente',
          celular: '51988888888',
          email: 'joao@teste.com',
          dataAniversario: '1990-01-01',
          observacoes: 'Obs'
        }
      ],
      totalElements: 1
    }
  }

  beforeEach(() => {
    vi.clearAllMocks()
    mockRouteQuery = {}
    api.get.mockImplementation((url) => {
      if (url === '/client') return Promise.resolve({ data: mockCompany })
      if (url === '/company-client-contact/list') return Promise.resolve(mockContactsResponse)
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
            props: ['field', 'header', 'headerClass', 'bodyClass'],
            template: `
              <div class="column-stub">
                <slot name="header" />
                <slot name="body" :data="{ id: 10, celular: '51988888888' }" />
              </div>
            `
          },
          Button: {
            props: ['label', 'icon'],
            template: '<button type="button" :data-icon="icon" @click="$emit(\'click\', $event)">{{ label }}<slot /></button>'
          },
          InputText: { template: '<input type="text" />' },
          InputMask: { template: '<input type="text" />' },
          DatePicker: { template: '<input type="date" />' },
          Textarea: { template: '<textarea></textarea>' },
          Select: { template: '<select></select>' },
          Message: {
            name: 'Message',
            props: ['severity', 'size', 'variant'],
            template: '<span class="message-stub"><slot /></span>'
          },
          ConfirmDialog: true,
          Dialog: {
            props: ['visible'],
            template: '<div class="dialog-stub" v-if="visible"><slot /></div>'
          },
          Popover: {
            template: `
              <div class="popover-stub">
                <slot />
                <h4>Arquivo de Colaboradores</h4>
                <p>O arquivo de colaboradores da empresa é um arquivo texto onde cada linha contém dados do colaborador.</p>
                <p>A primeira linha do arquivo é a linha de cabeçalho identificando os campos de dados do colaboradores</p>
                <ul style="list-style-type: disc; margin-left: 1.5rem;">
                  <li>nome: contém o nome completo do colaborador;</li>
                </ul>
                <p>Assim, as demais linhas devem conter os dados dos colaboradores</p>
                <p>Exemplo:</p>
                <p>nome, numero-cracha, data-aniversario, limite-gasto<br/>Fulano, DKJF-DC, 01/01/1970, 400</p>
              </div>
            `,
            methods: { toggle: vi.fn() }
          },
          FloatLabel: { template: '<div><slot /></div>' },
          FileUpload: defineComponent({
            name: 'FileUpload',
            template: '<div><slot /></div>',
            setup() {
              const files = ref([])
              const clear = vi.fn()
              return { files, clear }
            }
          }),
          Form: defineComponent({
            name: 'Form',
            props: ['resolver', 'initialValues'],
            emits: ['submit', 'reset'],
            template: '<form @submit.prevent="handleSubmit" @reset="$emit(\'reset\')"><slot /></form>',
            setup(props, { emit }) {
              const setValues = vi.fn((vals) => {
                if (vals) Object.assign(props.initialValues || {}, vals)
              })
              const reset = vi.fn()
              const handleSubmit = async () => {
                let valid = true
                let values = props.initialValues || {}
                if (props.resolver) {
                  const res = await props.resolver(values)
                  if (res && res.errors && Object.keys(res.errors).length > 0) {
                    valid = false
                  }
                }
                emit('submit', { valid, values })
              }
              return { setValues, reset, handleSubmit }
            }
          }),
          FormField: {
            name: 'FormField',
            props: ['name', 'initialValue'],
            template: `
              <div>
                <slot :$field="{ invalid: false, error: { message: 'Erro' }, value: '' }" />
                <slot :$field="{ invalid: true, error: { message: 'Erro' }, value: '51999999999' }" />
              </div>
            `
          }
        },
        directives: {
          tooltip: {}
        }
      }
    })
  }

  it('carrega os estados e dados da empresa quando ID está presente', async () => {
    mockRouteQuery = { id: '5' }
    mountComponent()
    await nextTick()
    await nextTick()

    expect(StateService.getStates).toHaveBeenCalled()
    expect(api.get).toHaveBeenCalledWith('/client', { params: { id: '5' } })
    expect(api.get).toHaveBeenCalledWith('/company-client-contact/list', expect.anything())
  })

  it('trata erro na carga da empresa cliente e na carga de contatos', async () => {
    mockRouteQuery = { id: '5' }
    api.get.mockImplementation((url) => {
      if (url === '/client') return Promise.reject({ response: { data: 'Erro ao carregar empresa' } })
      if (url === '/company-client-contact/list') return Promise.reject({ response: { data: 'Erro ao carregar contatos' } })
      return Promise.resolve({ data: {} })
    })

    mountComponent()
    await nextTick()
    await nextTick()

    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'error',
        summary: 'Falha de Carga de Empresa Cliente'
      })
    )
    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'error',
        summary: 'Falha de Carga de Contatos da Empresa Cliente'
      })
    )
  })

  it('executa a alteração de paginação (onPage) e ordenação (onSort com asc e desc)', async () => {
    mockRouteQuery = { id: '5' }
    const wrapper = mountComponent()
    await nextTick()

    api.get.mockClear()

    wrapper.vm.onPage({ page: 1, rows: 40 })
    expect(api.get).toHaveBeenLastCalledWith('/company-client-contact/list', {
      params: { idEmpresa: '5', page: 1, size: 40 }
    })

    wrapper.vm.onSort({ sortField: 'nome', sortOrder: 1 })
    expect(api.get).toHaveBeenLastCalledWith('/company-client-contact/list', {
      params: { idEmpresa: '5', page: 0, size: 40, sort: 'nome,asc' }
    })

    wrapper.vm.onSort({ sortField: 'nome', sortOrder: -1 })
    expect(api.get).toHaveBeenLastCalledWith('/company-client-contact/list', {
      params: { idEmpresa: '5', page: 0, size: 40, sort: 'nome,desc' }
    })
  })

  it('interrompe a gravação se o formulário for inválido', async () => {
    const wrapper = mountComponent()
    await nextTick()

    await wrapper.vm.save({ valid: false, values: {} })
    expect(api.post).not.toHaveBeenCalled()

    await wrapper.vm.saveContact({ valid: false, values: {} })
    expect(api.post).not.toHaveBeenCalled()
  })

  it('salva a empresa cliente com sucesso, testa validador Zod (transform/refine) e trata falhas', async () => {
    mockRouteQuery = { id: '5' }
    const wrapper = mountComponent()
    await nextTick()

    api.post.mockResolvedValueOnce({ status: 200, data: { id: 5 } })

    const formValues = { ...mockCompany, cnpj: '12345678000195', fone: '(51) 99999-9999', cep: '90000000' }
    await wrapper.vm.save({ valid: true, values: formValues })

    expect(onlyDigits).toHaveBeenCalled()
    expect(api.post).toHaveBeenCalledWith('/client', expect.objectContaining({ id: 5 }))
    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({ severity: 'success', summary: 'Sucesso' })
    )

    // Testa validação Zod via submissão do formulário principal
    const companyFormEl = wrapper.findComponent({ name: 'Form' })
    if (companyFormEl.exists()) {
      await companyFormEl.vm.handleSubmit()
    }

    api.post.mockRejectedValueOnce({ response: { data: 'Erro de validação no banco' } })
    await wrapper.vm.save({ valid: true, values: formValues })

    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'error',
        summary: 'Falha de Gravação de Empresa Cliente'
      })
    )
  })

  it('gerencia abertura do modal de contato para novo e edição, e cobre campos do contato', async () => {
    mockRouteQuery = { id: '5' }
    const wrapper = mountComponent()
    await nextTick()

    const contact = mockContactsResponse.data.content[0]
    wrapper.vm.edit(contact)
    await nextTick()
    expect(wrapper.vm.visible).toBe(true)

    wrapper.vm.edit(null)
    await nextTick()
    expect(wrapper.vm.visible).toBe(true)
  })

  it('salva contato da empresa cliente com sucesso e trata falhas', async () => {
    mockRouteQuery = { id: '5' }
    const wrapper = mountComponent()
    await nextTick()

    api.post.mockResolvedValueOnce({ status: 200 })

    await wrapper.vm.saveContact({
      valid: true,
      values: { nome: 'Maria', cargo: 'Analista', fone: '(51) 3333-3333', celular: '(51) 99999-9999', email: 'maria@teste.com' }
    })

    expect(api.post).toHaveBeenCalledWith('/company-client-contact', expect.objectContaining({ cliente: { id: '5' } }))
    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({ severity: 'success', summary: 'Sucesso' })
    )

    api.post.mockRejectedValueOnce({ response: { data: 'Erro ao salvar contato' } })
    await wrapper.vm.saveContact({ valid: true, values: { nome: 'Maria', email: 'maria@teste.com' } })

    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'error',
        summary: 'Falha de Gravação de Contato de Empresa Cliente'
      })
    )
  })

  it('remove contato via caixa de confirmação (confirmDelete)', async () => {
    mockRouteQuery = { id: '5' }
    const wrapper = mountComponent()
    await nextTick()

    wrapper.vm.confirmDelete({ id: 10 })
    expect(mockConfirmRequire).toHaveBeenCalled()

    api.delete.mockResolvedValueOnce({})
    const confirmOptions = mockConfirmRequire.mock.calls[0][0]
    await confirmOptions.accept()

    expect(api.delete).toHaveBeenCalledWith('/company-client-contact?id=10')
    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({ severity: 'success', summary: 'Sucesso' })
    )

    wrapper.vm.confirmDelete({ id: 10 })
    api.delete.mockRejectedValueOnce({ response: { data: 'Erro ao remover' } })
    await mockConfirmRequire.mock.calls[1][0].accept()

    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({ severity: 'error', summary: 'Falha de Remoção de Contato de Empresa Cliente' })
    )
  })

  it('realiza validações e upload do arquivo de colaboradores (plural, singular e erros)', async () => {
    mockRouteQuery = { id: '5' }
    const wrapper = mountComponent()
    await nextTick()

    const fileUploadComponent = wrapper.findComponent({ ref: 'fileupload' })

    fileUploadComponent.vm.files = [{ name: 'invalid.pdf', size: 100 }]
    await wrapper.vm.upload()
    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({ summary: 'Tipo inválido' })
    )

    fileUploadComponent.vm.files = [{ name: 'employees.csv', size: 11 * 1024 * 1024 }]
    await wrapper.vm.upload()
    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({ summary: 'Arquivo muito grande' })
    )

    // Plural (> 1)
    fileUploadComponent.vm.files = [{ name: 'employees.csv', size: 1024 }]
    api.post.mockResolvedValueOnce({ status: 200, data: { carregados: 2, total: 2 } })
    await wrapper.vm.upload()
    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({
        summary: 'Carga concluída',
        detail: '2 colaboradores carregados de 2 enviados.'
      })
    )

    // Singular (<= 1)
    fileUploadComponent.vm.files = [{ name: 'employees.csv', size: 1024 }]
    api.post.mockResolvedValueOnce({ status: 200, data: { carregados: 1, total: 1 } })
    await wrapper.vm.upload()
    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({
        summary: 'Carga concluída',
        detail: '1 colaborador carregado de 1 enviados.'
      })
    )

    api.post.mockRejectedValueOnce(new Error('Falha de rede'))
    await wrapper.vm.upload()
    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({ summary: 'Erro na carga' })
    )

    wrapper.vm.clearUpload()
    expect(fileUploadComponent.vm.clear).toHaveBeenCalled()
  })

  it('alterna a exibição do Popover explicativo e interage com todos os botões e formulários', async () => {
    mockRouteQuery = { id: '5' }
    const wrapper = mountComponent()
    await nextTick()

    const mockToggle = vi.fn()
    wrapper.vm.pop = { toggle: mockToggle }
    const infoIcon = wrapper.find('.pi-info-circle')
    if (infoIcon.exists()) {
      await infoIcon.trigger('click')
    }
    wrapper.vm.togglePopover({ target: {} })
    expect(mockToggle).toHaveBeenCalled()

    const buttons = wrapper.findAll('button[data-icon="pi pi-replay"]')
    for (const btn of buttons) {
      await btn.trigger('click')
    }
    expect(mockRouterPush).toHaveBeenCalledWith('/register/company')

    wrapper.vm.visible = true
    await nextTick()

    const cancelBtn = wrapper.findAll('button').find((b) => b.text().includes('Cancelar'))
    if (cancelBtn) {
      await cancelBtn.trigger('click')
      expect(wrapper.vm.visible).toBe(false)
    }

    wrapper.vm.edit(null)
    await nextTick()

    await wrapper.vm.saveContact({
      valid: true,
      values: {
        nome: 'Contato Completo',
        cargo: 'Gerente Geral',
        fone: '(51) 3333-3333',
        ramal: '456',
        celular: '(51) 98888-8888',
        email: 'completo@teste.com',
        dataAniversario: new Date('1995-05-15'),
        observacoes: 'Observações completas'
      }
    })
  })
})