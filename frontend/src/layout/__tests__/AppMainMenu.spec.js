import * as auth from '@/util/auth'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AppMainMenu from '../AppMainMenu.vue'

const mockPush = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: mockPush
  })
}))

describe('AppMainMenu.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  function mountComponent() {
    return mount(AppMainMenu, {
      global: {
        stubs: {
          Menubar: {
            name: 'Menubar',
            props: ['model'],
            template: '<div class="mock-menubar" />'
          }
        }
      }
    })
  }

  it('monta o menu corretamente com permissão de administrador', () => {
    vi.spyOn(auth, 'eAdmin').mockReturnValue(true)
    const wrapper = mountComponent()

    const menubar = wrapper.findComponent({ name: 'Menubar' })
    const items = menubar.props('model')

    expect(items).toHaveLength(5)
    expect(items.map((i) => i.label)).toEqual([
      'Início',
      'Vendas',
      'Estoque',
      'Cadastro',
      'Indicadores'
    ])
  })

  it('oculta seções restritas quando o usuário NÃO é administrador', () => {
    vi.spyOn(auth, 'eAdmin').mockReturnValue(false)
    const wrapper = mountComponent()

    const menubar = wrapper.findComponent({ name: 'Menubar' })
    const items = menubar.props('model')

    const estoqueItem = items.find((i) => i.label === 'Estoque')
    const cadastroItem = items.find((i) => i.label === 'Cadastro')
    const indicadoresItem = items.find((i) => i.label === 'Indicadores')

    expect(estoqueItem.visible).toBe(false)
    expect(cadastroItem.visible).toBe(false)
    expect(indicadoresItem.visible).toBe(false)
  })

  it('executa o router.push corretamente ao acionar o command dos itens principais', () => {
    vi.spyOn(auth, 'eAdmin').mockReturnValue(true)
    const wrapper = mountComponent()
    const items = wrapper.findComponent({ name: 'Menubar' }).props('model')

    items[0].command()
    expect(mockPush).toHaveBeenLastCalledWith('/')

    items[1].command()
    expect(mockPush).toHaveBeenLastCalledWith('/core/sale')

    items[4].command()
    expect(mockPush).toHaveBeenLastCalledWith('/dashboard')
  })

  it('executa o router.push nos subitens de Estoque e Cadastro', () => {
    vi.spyOn(auth, 'eAdmin').mockReturnValue(true)
    const wrapper = mountComponent()
    const items = wrapper.findComponent({ name: 'Menubar' }).props('model')

    const estoqueItems = items.find((i) => i.label === 'Estoque').items
    estoqueItems[0].command()
    expect(mockPush).toHaveBeenLastCalledWith('/stock/purchase-order')

    estoqueItems[1].command()
    expect(mockPush).toHaveBeenLastCalledWith('/stock/transfer')

    const cadastroItems = items.find((i) => i.label === 'Cadastro').items
    cadastroItems[0].command()
    expect(mockPush).toHaveBeenLastCalledWith('/register/provider')

    cadastroItems[1].command()
    expect(mockPush).toHaveBeenLastCalledWith('/register/sale-point')

    cadastroItems[3].command()
    expect(mockPush).toHaveBeenLastCalledWith('/register/company')

    cadastroItems[4].command()
    expect(mockPush).toHaveBeenLastCalledWith('/register/person')

    cadastroItems[6].command()
    expect(mockPush).toHaveBeenLastCalledWith('/register/product-type')

    cadastroItems[7].command()
    expect(mockPush).toHaveBeenLastCalledWith('/register/product')

    cadastroItems[8].command()
    expect(mockPush).toHaveBeenLastCalledWith('/register/price-table')
  })

  it('cobre o método toggleMenu quando menu ref está definido e indefinido', () => {
    vi.spyOn(auth, 'eAdmin').mockReturnValue(true)
    const wrapper = mountComponent()

    const mockToggle = vi.fn()
    const dummyEvent = new Event('click')

    wrapper.vm.toggleMenu(dummyEvent)
    expect(mockToggle).not.toHaveBeenCalled()

    wrapper.vm.menu = { toggle: mockToggle }
    wrapper.vm.toggleMenu(dummyEvent)
    expect(mockToggle).toHaveBeenCalledWith(dummyEvent)
  })
})