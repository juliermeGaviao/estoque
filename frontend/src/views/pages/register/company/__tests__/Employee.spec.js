// @vitest-environment jsdom
//
// Testes unitários para src/views/pages/register/company/Employee.vue
// ------------------------------------------------------------------
// Stack: Vitest + @vue/test-utils

import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import api from '@/util/api'
import Employee from '../Employee.vue'

// ------------------------------------------------------------------
// Mocks de módulos utilitários e de app
// ------------------------------------------------------------------

vi.mock('@/util/api', () => ({
  default: {
    post: vi.fn()
  }
}))

const toastAddMock = vi.fn()
vi.mock('primevue/usetoast', () => ({
  useToast: () => ({ add: toastAddMock })
}))

const routerPushMock = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: routerPushMock })
}))

// ------------------------------------------------------------------
// Stubs dos componentes do PrimeVue
// ------------------------------------------------------------------
const { CardStub, FileUploadStub, PopoverStub, ButtonStub } = await vi.hoisted(async () => {
  const { defineComponent, h, ref } = await import('vue')

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

  const FileUploadStub = defineComponent({
    name: 'FileUpload',
    props: ['modelValue'],
    setup(props, { expose }) {
      const files = ref([])
      const clear = vi.fn(() => {
        files.value = []
      })
      expose({ clear, files })
      return () => h('div', { class: 'fileupload-teststub' })
    }
  })

  const PopoverStub = defineComponent({
    name: 'Popover',
    setup(_, { slots, expose }) {
      const toggle = vi.fn()
      expose({ toggle })
      return () => h('div', { class: 'popover-teststub' }, slots.default ? slots.default() : null)
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

  return { CardStub, FileUploadStub, PopoverStub, ButtonStub }
})

vi.mock('primevue/card', () => ({ default: CardStub }))
vi.mock('primevue/fileupload', () => ({ default: FileUploadStub }))
vi.mock('primevue/popover', () => ({ default: PopoverStub }))
vi.mock('primevue/button', () => ({ default: ButtonStub }))

// ------------------------------------------------------------------
// Auxiliar de Montagem
// ------------------------------------------------------------------
async function mountComponent(props = { id: 1 }) {
  const wrapper = mount(Employee, { props })
  await flushPromises()
  return wrapper
}

// Auxiliar para localizar botão por texto/label
function findButton(wrapper, text) {
  const buttons = wrapper.findAllComponents(ButtonStub)
  return buttons.find(b => b.props('label')?.includes(text) || b.text().includes(text))
}

beforeEach(() => {
  vi.clearAllMocks()
  api.post.mockResolvedValue({ status: 200, data: { carregados: 5, total: 5 } })
})

afterEach(() => {
  vi.restoreAllMocks()
})

// ==================================================================
// Testes de Renderização e Visibilidade do Card
// ==================================================================
describe('Renderização e Card', () => {
  it('exibe o card quando o id é fornecido', async () => {
    const wrapper = await mountComponent({ id: 10 })
    expect(wrapper.find('.card-teststub').isVisible()).toBe(true)
  })

  it('oculta o card quando o id não é fornecido (v-show="id")', async () => {
    const wrapper = await mountComponent({ id: null })
    const card = wrapper.findComponent(CardStub)
    expect(card.attributes('style')).toContain('display: none')
  })
})

// ==================================================================
// Testes de Navegação (Botão Voltar)
// ==================================================================
describe('Navegação', () => {
  it('navega para /register/company ao clicar no botão de voltar', async () => {
    const wrapper = await mountComponent({ id: 1 })
    await wrapper.find('[icon="pi pi-replay"]').trigger('click')

    expect(routerPushMock).toHaveBeenCalledWith('/register/company')
  })
})

// ==================================================================
// Testes do Popover (Informações)
// ==================================================================
describe('Popover e Informações', () => {
  it('alterna o popover ao clicar no ícone de informação', async () => {
    const wrapper = await mountComponent({ id: 1 })
    const infoIcon = wrapper.find('.pi-info-circle')
    
    await infoIcon.trigger('click')
    
    const popoverComponent = wrapper.findComponent(PopoverStub)
    expect(popoverComponent.vm.toggle).toHaveBeenCalled()
  })
})

// ==================================================================
// Testes da Limpeza de Arquivo (clearUpload)
// ==================================================================
describe('clearUpload', () => {
  it('limpa os arquivos do FileUpload ao acionar o botão Limpar', async () => {
    const wrapper = await mountComponent({ id: 1 })
    const fileUploadComp = wrapper.findComponent(FileUploadStub)
    
    fileUploadComp.vm.files.push({ name: 'test.csv', size: 100 })
    await wrapper.vm.$nextTick()

    const clearButton = findButton(wrapper, 'Limpar')
    await clearButton.trigger('click')

    expect(fileUploadComp.vm.clear).toHaveBeenCalled()
    expect(fileUploadComp.vm.files.length).toBe(0)
  })
})

// ==================================================================
// Testes da Função upload() e Validações
// ==================================================================
describe('upload', () => {
  it('exibe erro se o arquivo não terminar com .csv', async () => {
    const wrapper = await mountComponent({ id: 1 })
    const fileUploadComp = wrapper.findComponent(FileUploadStub)
    
    fileUploadComp.vm.files.push({ name: 'documento.txt', size: 100 })
    await wrapper.vm.$nextTick()

    const uploadButton = findButton(wrapper, 'Carregar')
    await uploadButton.trigger('click')

    expect(toastAddMock).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'error',
        summary: 'Tipo inválido',
        detail: 'Apenas arquivos CSV são permitidos.'
      })
    )
    expect(api.post).not.toHaveBeenCalled()
  })

  it('exibe erro se o arquivo for maior que 10MB', async () => {
    const wrapper = await mountComponent({ id: 1 })
    const fileUploadComp = wrapper.findComponent(FileUploadStub)
    
    fileUploadComp.vm.files.push({ name: 'grandes-dados.csv', size: 11 * 1024 * 1024 })
    await wrapper.vm.$nextTick()

    const uploadButton = findButton(wrapper, 'Carregar')
    await uploadButton.trigger('click')

    expect(toastAddMock).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'error',
        summary: 'Arquivo muito grande',
        detail: 'O tamanho máximo é de 10MB.'
      })
    )
    expect(api.post).not.toHaveBeenCalled()
  })

  it('realiza o upload com sucesso e formata mensagem no plural (carregados > 1)', async () => {
    const wrapper = await mountComponent({ id: 1 })
    const fileUploadComp = wrapper.findComponent(FileUploadStub)
    
    fileUploadComp.vm.files.push({ name: 'colaboradores.csv', size: 500 })
    await wrapper.vm.$nextTick()

    api.post.mockResolvedValueOnce({ status: 200, data: { carregados: 2, total: 3 } })

    const uploadButton = findButton(wrapper, 'Carregar')
    await uploadButton.trigger('click')
    await flushPromises()

    expect(api.post).toHaveBeenCalledWith(
      '/client/load-employees',
      expect.any(FormData),
      { headers: { 'Content-Type': 'multipart/form-data' } }
    )
    expect(toastAddMock).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'success',
        summary: 'Carga concluída',
        detail: '2 colaboradores carregados de 3 enviados.'
      })
    )
  })

  it('realiza o upload com sucesso e formata mensagem no singular (carregados === 1)', async () => {
    const wrapper = await mountComponent({ id: 1 })
    const fileUploadComp = wrapper.findComponent(FileUploadStub)
    
    fileUploadComp.vm.files.push({ name: 'colaboradores.csv', size: 500 })
    await wrapper.vm.$nextTick()

    api.post.mockResolvedValueOnce({ status: 200, data: { carregados: 1, total: 1 } })

    const uploadButton = findButton(wrapper, 'Carregar')
    await uploadButton.trigger('click')
    await flushPromises()

    expect(toastAddMock).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'success',
        summary: 'Carga concluída',
        detail: '1 colaborador carregado de 1 enviados.'
      })
    )
  })

  it('trata erro corretamente quando a requisição falha (catch error)', async () => {
    const wrapper = await mountComponent({ id: 1 })
    const fileUploadComp = wrapper.findComponent(FileUploadStub)
    
    fileUploadComp.vm.files.push({ name: 'colaboradores.csv', size: 500 })
    await wrapper.vm.$nextTick()

    api.post.mockRejectedValueOnce(new Error('Erro interno no servidor'))

    const uploadButton = findButton(wrapper, 'Carregar')
    await uploadButton.trigger('click')
    await flushPromises()

    expect(toastAddMock).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'error',
        summary: 'Erro na carga',
        detail: 'Erro interno no servidor'
      })
    )
  })

  it('não exibe mensagem de sucesso nem erro quando a resposta é resolvida com status diferente de 200', async () => {
    const wrapper = await mountComponent({ id: 1 })
    const fileUploadComp = wrapper.findComponent(FileUploadStub)
    
    fileUploadComp.vm.files.push({ name: 'colaboradores.csv', size: 500 })
    await wrapper.vm.$nextTick()

    api.post.mockResolvedValueOnce({ status: 204, data: null })

    const uploadButton = findButton(wrapper, 'Carregar')
    await uploadButton.trigger('click')
    await flushPromises()

    expect(api.post).toHaveBeenCalled()
    expect(toastAddMock).not.toHaveBeenCalled()
  })
})