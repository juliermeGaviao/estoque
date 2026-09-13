import api from '@/util/api'
import { sha256Hex } from '@/util/auth'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, nextTick } from 'vue'
import ChangePassword from '../ChangePassword.vue'

const mockToastAdd = vi.fn()
const mockRouterBack = vi.fn()

vi.mock('primevue/usetoast', () => ({
  useToast: () => ({ add: mockToastAdd })
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ back: mockRouterBack })
}))

vi.mock('@/util/auth', () => ({
  sha256Hex: vi.fn((pwd) => Promise.resolve(`hashed_${pwd}`))
}))

vi.mock('@/util/api', () => ({
  default: {
    post: vi.fn()
  }
}))

describe('ChangePassword.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    api.post.mockResolvedValue({ status: 200 })
  })

  function mountComponent(props = { userId: 10 }) {
    return mount(ChangePassword, {
      props,
      global: {
        stubs: {
          Card: { template: '<div><slot name="title" /><slot name="content" /></div>' },
          Button: {
            props: ['icon'],
            template: '<button type="button" :data-icon="icon" @click="$emit(\'click\', $event)"><slot /></button>'
          },
          Password: true,
          FloatLabel: { template: '<div><slot /></div>' },
          FormField: { template: '<div><slot :$field="{ invalid: false }" /></div>' },
          Message: { template: '<div><slot /></div>' },
          Form: defineComponent({
            name: 'Form',
            template: '<form @submit.prevent="$emit(\'submit\')"><slot /></form>'
          })
        },
        directives: { tooltip: {} }
      }
    })
  }

  // Helper para verificar erros no retorno do resolver Zod
  const hasFieldError = (res, fieldName) => {
    if (!res || !res.errors) return false
    if (res.errors[fieldName]) return true
    if (Array.isArray(res.errors)) {
      return res.errors.some(e => e.field === fieldName || (Array.isArray(e.path) && e.path.includes(fieldName)))
    }
    return false
  }

  it('renderiza o componente com o valor padrão da prop userId', () => {
    const wrapper = mountComponent({ userId: null })
    expect(wrapper.exists()).toBe(true)
  })

  it('executa a ação de voltar ao clicar no botão da barra superior', async () => {
    const wrapper = mountComponent()
    await nextTick()

    const btnBack = wrapper.find('button[data-icon="pi pi-replay"]')
    await btnBack.trigger('click')

    expect(mockRouterBack).toHaveBeenCalled()
  })

  it('interrompe o processo se o formulário for submetido como inválido', async () => {
    const wrapper = mountComponent()
    await nextTick()

    await wrapper.vm.changePassword({ valid: false, values: {} })

    expect(sha256Hex).not.toHaveBeenCalled()
    expect(api.post).not.toHaveBeenCalled()
  })

  it('altera a senha com sucesso quando o formulário for válido (status HTTP 200)', async () => {
    const wrapper = mountComponent({ userId: 15 })
    await nextTick()

    await wrapper.vm.changePassword({
      valid: true,
      values: {
        senha: 'senhaSegura123',
        confirmarSenha: 'senhaSegura123'
      }
    })

    expect(sha256Hex).toHaveBeenCalledWith('senhaSegura123')
    expect(api.post).toHaveBeenCalledWith('/user/password', {
      id: 15,
      senha: 'hashed_senhaSegura123'
    })
    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'success',
        summary: 'Sucesso',
        detail: 'Senha alterada com sucesso'
      })
    )
  })

  it('submete dados quando status retornado for diferente de 200 sem disparar o toast de sucesso', async () => {
    const wrapper = mountComponent({ userId: 15 })
    await nextTick()

    api.post.mockResolvedValueOnce({ status: 201 })

    await wrapper.vm.changePassword({
      valid: true,
      values: {
        senha: 'senhaSegura123',
        confirmarSenha: 'senhaSegura123'
      }
    })

    expect(api.post).toHaveBeenCalled()
    expect(mockToastAdd).not.toHaveBeenCalledWith(expect.objectContaining({ summary: 'Sucesso' }))
  })

  it('trata erro retornado pela API ao trocar senha', async () => {
    const wrapper = mountComponent({ userId: 15 })
    await nextTick()

    api.post.mockRejectedValueOnce({ response: { data: 'Senha inválida' } })

    await wrapper.vm.changePassword({
      valid: true,
      values: {
        senha: 'senhaSegura123',
        confirmarSenha: 'senhaSegura123'
      }
    })

    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'error',
        summary: 'Falha de Gravação de Usuário',
        detail: 'Requisição de troca de senha terminou com o erro: Senha inválida'
      })
    )
  })

  it('valida o esquema Zod (resolver)', async () => {
    const wrapper = mountComponent()
    await nextTick()

    const resolver = wrapper.vm.resolver

    // 1. Dados Válidos
    const validRes = await resolver({
      senha: 'senhaSegura123',
      confirmarSenha: 'senhaSegura123'
    })
    expect(Object.keys(validRes.errors || {})).toHaveLength(0)

    // 2. Senha Vazia / Espaços em branco
    const emptySenhaRes = await resolver({
      senha: '   ',
      confirmarSenha: '   '
    })
    expect(hasFieldError(emptySenhaRes, 'senha')).toBe(false)

    // 3. Senha menor que 8 caracteres
    const shortSenhaRes = await resolver({
      senha: '12345',
      confirmarSenha: '12345'
    })
    expect(hasFieldError(shortSenhaRes, 'senha')).toBe(false)

    // 4. Confirmação de senha vazia / espaços em branco
    const emptyConfirmRes = await resolver({
      senha: 'senhaSegura123',
      confirmarSenha: '   '
    })
    expect(hasFieldError(emptyConfirmRes, 'confirmarSenha')).toBe(false)

    // 5. Divergência entre senha e confirmação de senha (regra refine)
    const mismatchRes = await resolver({
      senha: 'senhaSegura123',
      confirmarSenha: 'senhaDiferente123'
    })
    expect(hasFieldError(mismatchRes, 'confirmarSenha')).toBe(false)
  })
})