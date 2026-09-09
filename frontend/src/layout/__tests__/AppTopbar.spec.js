import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'
import AppTopbar from '../AppTopbar.vue'

const mockToggleDarkMode = vi.fn()
const mockIsDarkTheme = ref(false)

vi.mock('@/layout/composables/layout', () => ({
  useLayout: () => ({
    toggleDarkMode: mockToggleDarkMode,
    isDarkTheme: mockIsDarkTheme
  })
}))

describe('AppTopbar.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockIsDarkTheme.value = false
  })

  function mountComponent() {
    return mount(AppTopbar, {
      global: {
        stubs: {
          AppMainMenu: true,
          AppConfigurator: true,
          AppProfileMenu: true
        },
        directives: {
          styleclass: () => {}
        }
      }
    })
  }

  it('monta o componente e exibe os ícones com tema claro', () => {
    mockIsDarkTheme.value = false
    const wrapper = mountComponent()

    expect(wrapper.find('button .pi-sun').exists()).toBe(true)
    expect(wrapper.find('button .pi-moon').exists()).toBe(false)
  })

  it('altera o ícone para tema escuro quando isDarkTheme é verdadeiro', () => {
    mockIsDarkTheme.value = true
    const wrapper = mountComponent()

    expect(wrapper.find('button .pi-moon').exists()).toBe(true)
    expect(wrapper.find('button .pi-sun').exists()).toBe(false)
  })

  it('chama toggleDarkMode ao clicar no botão de alternar tema', async () => {
    const wrapper = mountComponent()

    const themeButton = wrapper.find('.layout-config-menu button')
    await themeButton.trigger('click')

    expect(mockToggleDarkMode).toHaveBeenCalledTimes(1)
  })
})