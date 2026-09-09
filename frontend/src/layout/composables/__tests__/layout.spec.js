import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useLayout } from '../layout'

describe('useLayout Composable', () => {
  let layout

  beforeEach(() => {
    vi.restoreAllMocks()
    layout = useLayout()

    // Reseta as propriedades reativas para os valores padrão do composable
    layout.layoutConfig.preset = 'Aura'
    layout.layoutConfig.primary = 'noir'
    layout.layoutConfig.surface = null
    layout.layoutConfig.darkTheme = false
    layout.layoutConfig.menuMode = 'static'

    layout.layoutState.staticMenuDesktopInactive = true
    layout.layoutState.overlayMenuActive = false
    layout.layoutState.profileSidebarVisible = false
    layout.layoutState.configSidebarVisible = false
    layout.layoutState.staticMenuMobileActive = false
    layout.layoutState.menuHoverActive = false
    layout.layoutState.activeMenuItem = null

    document.documentElement.classList.remove('app-dark')
  })

  afterEach(() => {
    delete document.startViewTransition
  })

  it('define activeMenuItem através do método setActiveMenuItem', () => {
    // Passando valor como string/primitivo
    layout.setActiveMenuItem('item1')
    expect(layout.layoutState.activeMenuItem).toBe('item1')

    // Passando objeto com propriedade .value
    layout.setActiveMenuItem({ value: 'item2' })
    expect(layout.layoutState.activeMenuItem).toBe('item2')
  })

  it('alterna o modo escuro sem suporte a startViewTransition', () => {
    delete document.startViewTransition

    layout.toggleDarkMode()

    expect(layout.layoutConfig.darkTheme).toBe(true)
    expect(layout.isDarkTheme.value).toBe(true)
    expect(document.documentElement.classList.contains('app-dark')).toBe(true)

    // Desativa novamente
    layout.toggleDarkMode()
    expect(layout.layoutConfig.darkTheme).toBe(false)
    expect(document.documentElement.classList.contains('app-dark')).toBe(false)
  })

  it('alterna o modo escuro utilizando startViewTransition', () => {
    const startViewTransitionMock = vi.fn((callback) => callback())
    document.startViewTransition = startViewTransitionMock

    layout.toggleDarkMode()

    expect(startViewTransitionMock).toHaveBeenCalled()
    expect(layout.layoutConfig.darkTheme).toBe(true)
    expect(document.documentElement.classList.contains('app-dark')).toBe(true)
  })

  it('alterna o menu no modo overlay e no modo de tela grande (> 991px)', () => {
    layout.layoutConfig.menuMode = 'overlay'
    window.innerWidth = 1024

    layout.toggleMenu()

    expect(layout.layoutState.overlayMenuActive).toBe(true)
    expect(layout.layoutState.staticMenuDesktopInactive).toBe(false)
  })

  it('alterna o menu no modo static e no modo mobile (<= 991px)', () => {
    layout.layoutConfig.menuMode = 'static'
    window.innerWidth = 800

    layout.toggleMenu()

    expect(layout.layoutState.overlayMenuActive).toBe(false)
    expect(layout.layoutState.staticMenuMobileActive).toBe(true)
  })

  it('avalia as propriedades computadas corretamente', () => {
    expect(layout.getPrimary.value).toBe('noir')
    expect(layout.getSurface.value).toBeNull()

    // isSidebarActive deve ser true se overlayMenuActive ou staticMenuMobileActive for true
    expect(layout.isSidebarActive.value).toBe(false)

    layout.layoutState.overlayMenuActive = true
    expect(layout.isSidebarActive.value).toBe(true)

    layout.layoutState.overlayMenuActive = false
    layout.layoutState.staticMenuMobileActive = true
    expect(layout.isSidebarActive.value).toBe(true)
  })
})