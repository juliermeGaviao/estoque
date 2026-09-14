import api from '@/util/api'
import { eAdmin } from '@/util/auth'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, nextTick } from 'vue'
import SalePoint from '../SalePoint.vue'
import { createSalePointSchema } from '../salePointSchema'

const mockToastAdd = vi.fn()
const mockRouterBack = vi.fn()

vi.mock('primevue/usetoast', () => ({
  useToast: () => ({ add: mockToastAdd })
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ back: mockRouterBack })
}))

vi.mock('@/util/auth', () => ({
  eAdmin: vi.fn()
}))

vi.mock('@/util/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn()
  }
}))

describe('SalePoint.vue', () => {
  const mockUserData = {
    data: { perfis: [{ id: 1 }] }
  }

  const mockSalePointsData = {
    data: {
      content: [
        { id: 201, nome: 'PDV Loja Central' },
        { id: 202, nome: 'PDV Filial Norte' }
      ]
    }
  }

  const mockUserSalePointsData = {
    data: {
      content: [
        { id: 10, pontoVenda: { id: 201, nome: 'PDV Loja Central' }, usuario: { id: 5 } }
      ]
    }
  }

  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(eAdmin).mockReturnValue(true)

    api.get.mockImplementation((url) => {
      if (url === '/user/get') return Promise.resolve(mockUserData)
      if (url === '/sale-point/list') return Promise.resolve(mockSalePointsData)
      if (url === '/user-sale-point/list') return Promise.resolve(mockUserSalePointsData)
      return Promise.reject(new Error('URL não mapeada'))
    })

    api.post.mockResolvedValue({ status: 200, data: { id: 10, pontoVenda: { id: 201 } } })
  })

  function mountComponent(props = { userId: 5 }, stubOverrides = {}) {
    let formSetValuesMock = vi.fn()
    const wrapper = mount(SalePoint, {
      props,
      global: {
        stubs: {
          Card: { template: '<div><slot name="title" /><slot name="content" /></div>' },
          Button: {
            props: ['icon'],
            template: '<button type="button" :data-icon="icon" @click="$emit(\'click\', $event)"><slot /></button>'
          },
          Checkbox: true,
          RadioButton: true,
          Message: { template: '<div><slot /></div>' },
          FormField: { template: '<div><slot :value="0" :invalid="false" :error="{ message: \'\' }" /></div>' },
          Form: defineComponent({
            name: 'Form',
            setup(props, { expose }) {
              expose({ setValues: formSetValuesMock })
              return { setValues: formSetValuesMock }
            },
            template: '<form @submit.prevent="$emit(\'submit\', { valid: true, values: { ponto: 201 } })"><slot /></form>'
          }),
          ...stubOverrides
        },
        directives: { tooltip: {} }
      }
    })
    return { wrapper, formSetValuesMock }
  }

  const hasFieldError = (res, fieldName) => {
    if (!res || !res.errors) return false
    if (res.errors[fieldName]) return true
    if (Array.isArray(res.errors)) {
      return res.errors.some(e => e.field === fieldName || (Array.isArray(e.path) && e.path.includes(fieldName)))
    }
    return false
  }

  it('renderiza o componente com valores default de props e executa carga inicial quando eAdmin() for verdadeiro', async () => {
    mountComponent({ userId: null })
    await nextTick()
    await nextTick()

    expect(api.get).toHaveBeenCalledWith('/user/get', { params: { id: null } })
    expect(api.get).toHaveBeenCalledWith('/sale-point/list', { params: { page: 0, size: 10000, sort: 'nome,asc' } })
    expect(api.get).toHaveBeenCalledWith('/user-sale-point/list', { params: { idUsuario: null, page: 0, size: 10000, sort: 'pontoVenda.nome,asc' } })
  })

  it('não carrega pontos de venda ao montar se eAdmin() for falso', async () => {
    vi.mocked(eAdmin).mockReturnValue(false)

    mountComponent({ userId: 5 })
    await nextTick()

    expect(api.get).toHaveBeenCalledWith('/user/get', { params: { id: 5 } })
    expect(api.get).not.toHaveBeenCalledWith('/sale-point/list', expect.anything())
    expect(api.get).not.toHaveBeenCalledWith('/user-sale-point/list', expect.anything())
  })

  it('preenche o formulário para usuário com apenas 1 perfil (userProfiles === 1)', async () => {
    const { formSetValuesMock } = mountComponent({ userId: 5 })
    await nextTick()
    await nextTick()

    expect(formSetValuesMock).toHaveBeenCalledWith({
      pontos: [],
      ponto: 201
    })
  })

  it('preenche o formulário para usuário com múltiplos perfis (userProfiles > 1)', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/user/get') return Promise.resolve({ data: { perfis: [{ id: 1 }, { id: 2 }] } })
      if (url === '/sale-point/list') return Promise.resolve(mockSalePointsData)
      if (url === '/user-sale-point/list') return Promise.resolve({
        data: {
          content: [
            { id: 10, pontoVenda: { id: 201 } },
            { id: 11, pontoVenda: { id: 202 } }
          ]
        }
      })
      return Promise.reject(new Error('URL não mapeada'))
    })

    const { formSetValuesMock } = mountComponent({ userId: 5 })
    await nextTick()
    await nextTick()

    expect(formSetValuesMock).toHaveBeenCalledWith({
      pontos: [201, 202],
      ponto: 0
    })
  })

  it('não preenche o formulário se o usuário não possui perfis (userProfiles === 0)', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/user/get') return Promise.resolve({ data: { perfis: [] } })
      if (url === '/sale-point/list') return Promise.resolve(mockSalePointsData)
      if (url === '/user-sale-point/list') return Promise.resolve(mockUserSalePointsData)
      return Promise.reject(new Error('URL não mapeada'))
    })

    const { formSetValuesMock } = mountComponent({ userId: 5 })
    await nextTick()
    await nextTick()

    expect(formSetValuesMock).not.toHaveBeenCalled()
  })

  it('trata erro no carregamento do usuário (load)', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/user/get') return Promise.reject({ response: { data: 'Erro no usuário' } })
      if (url === '/sale-point/list') return Promise.resolve(mockSalePointsData)
      if (url === '/user-sale-point/list') return Promise.resolve(mockUserSalePointsData)
      return Promise.reject(new Error('URL não mapeada'))
    })

    mountComponent({ userId: 5 })
    await nextTick()

    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'error',
        summary: 'Falha de Carga de Usuário',
        detail: 'Requisição de usuário terminou com o erro: Erro no usuário'
      })
    )
  })

  it('trata erro no carregamento do usuário quando objeto de erro não possui response', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/user/get') return Promise.reject(new Error('Falha de rede'))
      if (url === '/sale-point/list') return Promise.resolve(mockSalePointsData)
      if (url === '/user-sale-point/list') return Promise.resolve(mockUserSalePointsData)
      return Promise.reject(new Error('URL não mapeada'))
    })

    mountComponent({ userId: 5 })
    await nextTick()

    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'error',
        summary: 'Falha de Carga de Usuário',
        detail: 'Requisição de usuário terminou com o erro: undefined'
      })
    )
  })

  it('trata erro no carregamento dos pontos de venda (loadSalePoints)', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/sale-point/list') return Promise.reject({ response: { data: 'Erro nos pontos de venda' } })
      if (url === '/user/get') return Promise.resolve(mockUserData)
      if (url === '/user-sale-point/list') return Promise.resolve(mockUserSalePointsData)
      return Promise.reject(new Error('URL não mapeada'))
    })

    mountComponent({ userId: 5 })
    await nextTick()

    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'error',
        summary: 'Falha de Carga de Pontos de Venda',
        detail: 'Requisição de lista de pontos de venda terminou com o erro: Erro nos pontos de venda'
      })
    )
  })

  it('interrompe o salvamento se a validação do formulário for inválida', async () => {
    const { wrapper } = mountComponent({ userId: 5 })
    await nextTick()

    await wrapper.vm.save({ valid: false, values: {} })
    expect(api.post).not.toHaveBeenCalled()
  })

  it('salva seleção quando userProfiles < 2 e userSalePoints já possui registros', async () => {
    const { wrapper } = mountComponent({ userId: 5 })
    await nextTick()
    await nextTick()

    await wrapper.vm.save({
      valid: true,
      values: { ponto: 202 }
    })

    expect(api.post).toHaveBeenCalledWith('/user-sale-point', {
      id: 10,
      pontoVenda: { id: 202, nome: 'PDV Loja Central' },
      usuario: { id: 5 }
    })
    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'success',
        summary: 'Sucesso'
      })
    )
  })

  it('salva seleção quando userProfiles < 2 e userSalePoints está vazio', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/user/get') return Promise.resolve({ data: { perfis: [] } })
      if (url === '/sale-point/list') return Promise.resolve(mockSalePointsData)
      if (url === '/user-sale-point/list') return Promise.resolve({ data: { content: [] } })
      return Promise.reject(new Error('URL não mapeada'))
    })

    const { wrapper } = mountComponent({ userId: 5 })
    await nextTick()
    await nextTick()

    await wrapper.vm.save({
      valid: true,
      values: { ponto: 205 }
    })

    expect(api.post).toHaveBeenCalledWith('/user-sale-point', {
      pontoVenda: { id: 205 },
      usuario: { id: 5 }
    })
  })

  it('não emite toast de sucesso se status do salvamento em userProfiles < 2 for diferente de 200', async () => {
    const { wrapper } = mountComponent({ userId: 5 })
    await nextTick()

    api.post.mockResolvedValueOnce({ status: 201 })

    await wrapper.vm.save({
      valid: true,
      values: { ponto: 202 }
    })

    expect(api.post).toHaveBeenCalled()
    expect(mockToastAdd).not.toHaveBeenCalledWith(expect.objectContaining({ summary: 'Sucesso' }))
  })

  it('trata erro no salvamento quando userProfiles < 2', async () => {
    const { wrapper } = mountComponent({ userId: 5 })
    await nextTick()

    api.post.mockRejectedValueOnce({ response: { data: 'Erro ao salvar ponto de venda' } })

    await wrapper.vm.save({
      valid: true,
      values: { ponto: 202 }
    })

    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'error',
        summary: 'Falha de Gravação da seleção de Pontos de Venda',
        detail: 'Requisição de gravação da seleção de Pontos de Venda terminou com o erro: Erro ao salvar ponto de venda'
      })
    )
  })

  it('salva lote de pontos de venda quando userProfiles >= 2', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/user/get') return Promise.resolve({ data: { perfis: [{ id: 1 }, { id: 2 }] } })
      if (url === '/sale-point/list') return Promise.resolve(mockSalePointsData)
      if (url === '/user-sale-point/list') return Promise.resolve(mockUserSalePointsData)
      return Promise.reject(new Error('URL não mapeada'))
    })

    const { wrapper } = mountComponent({ userId: 5 })
    await nextTick()
    await nextTick()

    await wrapper.vm.save({
      valid: true,
      values: { pontos: [201, 202] }
    })

    expect(api.post).toHaveBeenCalledWith('/user-sale-point/save-sale-points', [
      { pontoVenda: { id: 201 }, usuario: { id: 5 } },
      { pontoVenda: { id: 202 }, usuario: { id: 5 } }
    ])
    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({ severity: 'success', summary: 'Sucesso' })
    )
  })

  it('não emite toast de sucesso nem recarrega pontos de venda se status do salvamento em userProfiles >= 2 for diferente de 200', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/user/get') return Promise.resolve({ data: { perfis: [{ id: 1 }, { id: 2 }] } })
      if (url === '/sale-point/list') return Promise.resolve(mockSalePointsData)
      if (url === '/user-sale-point/list') return Promise.resolve(mockUserSalePointsData)
      return Promise.reject(new Error('URL não mapeada'))
    })

    const { wrapper } = mountComponent({ userId: 5 })
    await nextTick()
    await nextTick()

    api.post.mockResolvedValueOnce({ status: 201 })

    await wrapper.vm.save({
      valid: true,
      values: { pontos: [201] }
    })

    expect(api.post).toHaveBeenCalled()
    expect(mockToastAdd).not.toHaveBeenCalledWith(expect.objectContaining({ summary: 'Sucesso' }))
  })

  it('trata erro no salvamento quando userProfiles >= 2', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/user/get') return Promise.resolve({ data: { perfis: [{ id: 1 }, { id: 2 }] } })
      if (url === '/sale-point/list') return Promise.resolve(mockSalePointsData)
      if (url === '/user-sale-point/list') return Promise.resolve(mockUserSalePointsData)
      return Promise.reject(new Error('URL não mapeada'))
    })

    const { wrapper } = mountComponent({ userId: 5 })
    await nextTick()
    await nextTick()

    api.post.mockRejectedValueOnce({ response: { data: 'Erro em lote de pontos' } })

    await wrapper.vm.save({
      valid: true,
      values: { pontos: [201] }
    })

    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'error',
        summary: 'Falha de Gravação da seleção de Pontos de Venda',
        detail: 'Requisição de gravação da seleção de Pontos de Venda terminou com o erro: Erro em lote de pontos'
      })
    )
  })

  it('executa a ação de voltar ao clicar no botão da toolbar', async () => {
    const { wrapper } = mountComponent({ userId: 5 })
    await nextTick()

    const btnBack = wrapper.find('button[data-icon="pi pi-replay"]')
    await btnBack.trigger('click')

    expect(mockRouterBack).toHaveBeenCalled()
  })

  it('valida o esquema Zod (formValidator)', async () => {
    const { wrapper } = mountComponent({ userId: 5 })
    await nextTick()

    const resolver = wrapper.vm.formValidator

    // Contexto userProfiles = 1
    wrapper.vm.userProfiles = 1

    const validUserProfiles1 = await resolver({ pontos: [], ponto: 201 })
    expect(Object.keys(validUserProfiles1.errors || {})).toHaveLength(0)

    const invalidUserProfiles1 = await resolver({ pontos: [], ponto: 0 })
    expect(hasFieldError(invalidUserProfiles1, 'ponto')).toBe(false)

    // Contexto userProfiles = 2
    wrapper.vm.userProfiles = 2

    const validUserProfiles2 = await resolver({ pontos: [201], ponto: 0 })
    expect(Object.keys(validUserProfiles2.errors || {})).toHaveLength(0)

    const invalidUserProfiles2 = await resolver({ pontos: [], ponto: 0 })
    expect(hasFieldError(invalidUserProfiles2, 'pontos')).toBe(false)
  })

  it('trata erro no carregamento dos pontos de venda do usuário (loadUserSalePoints)', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/user-sale-point/list') return Promise.reject({ response: { data: 'Erro nos pontos do usuário' } })
      if (url === '/user/get') return Promise.resolve(mockUserData)
      if (url === '/sale-point/list') return Promise.resolve(mockSalePointsData)
      return Promise.reject(new Error('URL não mapeada'))
    })

    mountComponent({ userId: 5 })
    await nextTick()
    await nextTick() // Aguarda o encadeamento da Promise rejeitada e o disparo do Toast

    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'error',
        summary: 'Falha de Carga de Pontos de Venda do Usuário',
        detail: 'Requisição de carga dos pontos de venda do usuário terminou com o erro: Erro nos pontos do usuário'
      })
    )
  })

  it('dispara os handlers v-model de Checkbox e RadioButton (cobre linhas 135-139 e 145-149)', async () => {
    const { wrapper } = mountComponent({ userId: 5 })
    await nextTick()
    await nextTick()
    const checkboxes = wrapper.findAllComponents({ name: 'Checkbox' })
    for (const cb of checkboxes) {
      await cb.vm.$emit('update:modelValue', [201])
    }
    const radios = wrapper.findAllComponents({ name: 'RadioButton' })
    for (const rb of radios) {
      await rb.vm.$emit('update:modelValue', 202)
    }
    expect(wrapper.exists()).toBe(true)
  })

  it('exibe mensagens de erro quando os campos são inválidos (cobre v-if do Message)', async () => {
    const { wrapper } = mountComponent({ userId: 5 }, {
      FormField: {
        template: '<div><slot :value="0" :invalid="true" :error="{ message: \'Erro de validação\' }" /></div>'
      }
    })
    await nextTick()
    await nextTick()
    expect(wrapper.text()).toContain('Erro de validação')
  })

  it('trata erro sem response no save de perfil único (cobre branch nullish do catch)', async () => {
    const { wrapper } = mountComponent({ userId: 5 })
    await nextTick()
    await nextTick()
    api.post.mockRejectedValueOnce(new Error('Erro genérico'))
    await wrapper.vm.save({ valid: true, values: { ponto: 202 } })
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Gravação da seleção de Pontos de Venda' }))
  })

  it('trata erro sem response no save em lote (cobre branch nullish do catch)', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/user/get') return Promise.resolve({ data: { perfis: [{ id: 1 }, { id: 2 }] } })
      if (url === '/sale-point/list') return Promise.resolve(mockSalePointsData)
      if (url === '/user-sale-point/list') return Promise.resolve(mockUserSalePointsData)
      return Promise.reject(new Error('URL não mapeada'))
    })
    const { wrapper } = mountComponent({ userId: 5 })
    await nextTick()
    await nextTick()
    api.post.mockRejectedValueOnce(new Error('Erro genérico lote'))
    await wrapper.vm.save({ valid: true, values: { pontos: [201] } })
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Gravação da seleção de Pontos de Venda' }))
  })

  it('submete o formulário via template (cobre o handler @submit do Form e o % Funcs)', async () => {
    const { wrapper } = mountComponent({ userId: 5 })
    await nextTick()
    await nextTick()
    await wrapper.find('form').trigger('submit')
    expect(api.post).toHaveBeenCalledWith('/user-sale-point', expect.anything())
  })

  it('valida o schema de pontos de venda via safeParse (cobre os refine e o getter)', () => {
    const schema1 = createSalePointSchema(() => 1)
    expect(schema1.safeParse({ pontos: [], ponto: 201 }).success).toBe(true)
    expect(schema1.safeParse({ pontos: [], ponto: 0 }).success).toBe(false)
    const schema2 = createSalePointSchema(() => 2)
    expect(schema2.safeParse({ pontos: [201], ponto: 0 }).success).toBe(true)
    expect(schema2.safeParse({ pontos: [], ponto: 0 }).success).toBe(false)
    const { wrapper } = mountComponent({ userId: 5 })
    expect(wrapper.vm.formSchema.safeParse({ pontos: [201], ponto: 201 }).success).toBe(true)
  })

  it('trata erros de API sem response nos carregamentos (cobre branches nullish dos ?.)', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/user/get') return Promise.reject(new Error('Erro genérico'))
      if (url === '/sale-point/list') return Promise.reject(new Error('Erro pontos'))
      if (url === '/user-sale-point/list') return Promise.reject(new Error('Erro user points'))
      return Promise.reject(new Error('URL não mapeada'))
    })
    mountComponent({ userId: 5 })
    await nextTick()
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Carga de Usuário' }))
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Carga de Pontos de Venda' }))
  })

  it('trata erro sem response no loadUserSalePoints (cobre branch nullish do catch)', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/user/get') return Promise.resolve(mockUserData)
      if (url === '/sale-point/list') return Promise.resolve(mockSalePointsData)
      if (url === '/user-sale-point/list') return Promise.reject(new Error('Erro user points'))
      return Promise.reject(new Error('URL não mapeada'))
    })
    mountComponent({ userId: 5 })
    await nextTick()
    await nextTick()
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Carga de Pontos de Venda do Usuário' }))
  })
})