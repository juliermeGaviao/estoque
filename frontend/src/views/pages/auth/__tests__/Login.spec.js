import api from '@/util/api'
import * as auth from '@/util/auth'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import Login from '../Login.vue'

const mockToastAdd = vi.fn()
const mockRouterPush = vi.fn()

vi.mock('primevue/usetoast', () => ({
  useToast: () => ({ add: mockToastAdd })
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: mockRouterPush })
}))

vi.mock('@/util/api', () => ({
  default: { post: vi.fn() }
}))

vi.mock('@/util/auth', () => ({
  login: vi.fn(),
  sha256Hex: vi.fn().mockResolvedValue('hashed_password')
}))

describe('Login.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  function mountComponent() {
    return mount(Login, {
      global: {
        stubs: {
          FloatingConfigurator: true,
          Image: true,
          InputText: true,
          Password: true,
          Checkbox: true,
          Button: true
        }
      }
    })
  }

  it('exibe erro no toast se email ou senha estiverem vazios', async () => {
    const wrapper = mountComponent()

    await wrapper.findComponent({ name: 'Button' }).trigger('click')

    expect(mockToastAdd).toHaveBeenCalledWith({
      severity: 'error',
      summary: 'Falha de Autenticação',
      detail: 'Usuário e senha são obrigatórios',
      life: 10000
    })
    expect(api.post).not.toHaveBeenCalled()
  })

  it('efetua login com sucesso e redireciona para a raiz', async () => {
    const wrapper = mountComponent()

    const mockResponse = { data: { id: 1, token: 'abc' } }
    api.post.mockResolvedValueOnce(mockResponse)

    await wrapper.findComponent({ name: 'InputText' }).setValue('user@test.com')
    await wrapper.findComponent({ name: 'Password' }).setValue('secret')
    await wrapper.findComponent({ name: 'Checkbox' }).setValue(true)

    await wrapper.findComponent({ name: 'Button' }).trigger('click')

    expect(auth.sha256Hex).toHaveBeenCalledWith('secret')
    expect(api.post).toHaveBeenCalledWith('/auth/login', {
      email: 'user@test.com',
      senha: 'hashed_password'
    })
    expect(auth.login).toHaveBeenCalledWith(mockResponse.data, true)
    expect(mockRouterPush).toHaveBeenCalledWith('/')
  })

  it('exibe erro de credenciais inválidas em caso de status 403', async () => {
    const wrapper = mountComponent()

    api.post.mockRejectedValueOnce({ status: 403 })

    await wrapper.findComponent({ name: 'InputText' }).setValue('user@test.com')
    await wrapper.findComponent({ name: 'Password' }).setValue('wrong')

    await wrapper.findComponent({ name: 'Button' }).trigger('click')

    expect(mockToastAdd).toHaveBeenCalledWith({
      severity: 'error',
      summary: 'Falha de Autenticação',
      detail: 'Usuário ou senha inválidos',
      life: 10000
    })
  })

  it('exibe erro genérico de comunicação para outras falhas na API', async () => {
    const wrapper = mountComponent()

    api.post.mockRejectedValueOnce({ status: 500 })

    await wrapper.findComponent({ name: 'InputText' }).setValue('user@test.com')
    await wrapper.findComponent({ name: 'Password' }).setValue('secret')

    await wrapper.findComponent({ name: 'Button' }).trigger('click')

    expect(mockToastAdd).toHaveBeenCalledWith({
      severity: 'error',
      summary: 'Falha de Autenticação',
      detail: 'Falha de comunicação. Tente mais tarde',
      life: 10000
    })
  })
})