import * as auth from '@/util/auth'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AppProfileMenu from '../AppProfileMenu.vue'

const mockPush = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: mockPush
  })
}))

describe('AppProfileMenu.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.spyOn(auth, 'getUserId').mockReturnValue(123)
  })

  function mountComponent() {
    return mount(AppProfileMenu, {
      global: {
        stubs: {
          TieredMenu: {
            name: 'TieredMenu',
            props: ['model', 'popup'],
            methods: {
              toggle: vi.fn()
            },
            template: '<div class="mock-tiered-menu" />'
          }
        }
      }
    })
  }

  it('monta a lista de opções completa quando o usuário é administrador', () => {
    vi.spyOn(auth, 'eAdmin').mockReturnValue(true)
    const wrapper = mountComponent()

    const tieredMenu = wrapper.findComponent({ name: 'TieredMenu' })
    const items = tieredMenu.props('model')

    expect(items).toHaveLength(3)
    expect(items.map((i) => i.label)).toEqual([
      'Minha Conta',
      'Gestão de Usuários',
      'Sair'
    ])
  })

  it('omite a opção "Gestão de Usuários" quando o usuário NÃO é administrador', () => {
    vi.spyOn(auth, 'eAdmin').mockReturnValue(false)
    const wrapper = mountComponent()

    const tieredMenu = wrapper.findComponent({ name: 'TieredMenu' })
    const items = tieredMenu.props('model')

    expect(items).toHaveLength(2)
    expect(items.map((i) => i.label)).toEqual(['Minha Conta', 'Sair'])
  })

  it('redireciona para edição de usuário com o ID correto em "Minha Conta"', () => {
    vi.spyOn(auth, 'eAdmin').mockReturnValue(false)
    const wrapper = mountComponent()

    const items = wrapper.findComponent({ name: 'TieredMenu' }).props('model')
    items[0].command()

    expect(auth.getUserId).toHaveBeenCalled()
    expect(mockPush).toHaveBeenCalledWith('/register/user/edit?id=123')
  })

  it('redireciona para "/register/user" em "Gestão de Usuários"', () => {
    vi.spyOn(auth, 'eAdmin').mockReturnValue(true)
    const wrapper = mountComponent()

    const items = wrapper.findComponent({ name: 'TieredMenu' }).props('model')
    items[1].command()

    expect(mockPush).toHaveBeenCalledWith('/register/user')
  })

  it('executa logout e redireciona para login ao clicar em "Sair"', () => {
    vi.spyOn(auth, 'eAdmin').mockReturnValue(false)
    const logoutSpy = vi.spyOn(auth, 'logout').mockImplementation(() => {})
    const wrapper = mountComponent()

    const items = wrapper.findComponent({ name: 'TieredMenu' }).props('model')
    items[1].command() // Índice 1 é o "Sair" para não-admin

    expect(logoutSpy).toHaveBeenCalled()
    expect(mockPush).toHaveBeenCalledWith('/auth/login')
  })

  it('dispara a função toggle do TieredMenu ao clicar no botão do perfil', async () => {
    vi.spyOn(auth, 'eAdmin').mockReturnValue(false)
    const wrapper = mountComponent()

    const tieredMenu = wrapper.findComponent({ name: 'TieredMenu' })
    const toggleSpy = vi.spyOn(tieredMenu.vm, 'toggle')

    const button = wrapper.find('button.layout-menu-button')
    await button.trigger('click')

    expect(toggleSpy).toHaveBeenCalled()
  })
})