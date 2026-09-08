// @vitest-environment jsdom
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import FloatingConfigurator from '../FloatingConfigurator.vue'

const mockToggleDarkMode = vi.fn()
const mockIsDarkTheme = { value: false }

vi.mock('@/layout/composables/layout', () => ({
  useLayout: () => ({
    toggleDarkMode: mockToggleDarkMode,
    get isDarkTheme() {
      return mockIsDarkTheme.value
    }
  })
}))

const globalStubs = {
  AppConfigurator: {
    template: '<div class="app-configurator-stub" />'
  },
  Button: {
    props: ['icon', 'type', 'rounded', 'severity'],
    // v-bind="$attrs" repassa a diretiva para o elemento DOM real
    template: '<button :class="icon" v-bind="$attrs" />'
  }
}

const mockStyleClassFn = vi.fn()

describe('FloatingConfigurator.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockIsDarkTheme.value = false
  })

  it('deve renderizar o botão com o ícone de sol quando isDarkTheme for false', () => {
    const wrapper = mount(FloatingConfigurator, {
      global: {
        stubs: globalStubs,
        directives: {
          styleclass: mockStyleClassFn
        }
      }
    })

    const darkModeBtn = wrapper.findAll('button')[0]
    expect(darkModeBtn.classes()).toContain('pi-sun')
  })

  it('deve renderizar o botão com o ícone de lua quando isDarkTheme for true', () => {
    mockIsDarkTheme.value = true

    const wrapper = mount(FloatingConfigurator, {
      global: {
        stubs: globalStubs,
        directives: {
          styleclass: mockStyleClassFn
        }
      }
    })

    const darkModeBtn = wrapper.findAll('button')[0]
    expect(darkModeBtn.classes()).toContain('pi-moon')
  })

  it('deve acionar toggleDarkMode ao clicar no botão de tema', async () => {
    const wrapper = mount(FloatingConfigurator, {
      global: {
        stubs: globalStubs,
        directives: {
          styleclass: mockStyleClassFn
        }
      }
    })

    const darkModeBtn = wrapper.findAll('button')[0]
    await darkModeBtn.trigger('click')

    expect(mockToggleDarkMode).toHaveBeenCalledTimes(1)
  })

})