import { mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { reactive, ref } from 'vue'
import AppLayout from '../AppLayout.vue'

const mockLayoutConfig = reactive({ menuMode: 'static' })
const mockLayoutState = reactive({
  staticMenuDesktopInactive: false,
  overlayMenuActive: false,
  staticMenuMobileActive: false,
  menuHoverActive: false
})
const mockIsSidebarActive = ref(false)

vi.mock('@/layout/composables/layout', () => ({
  useLayout: () => ({
    layoutConfig: mockLayoutConfig,
    layoutState: mockLayoutState,
    isSidebarActive: mockIsSidebarActive
  })
}))

describe('AppLayout.vue - Cobertura Total de Branches', () => {
  let sidebarEl
  let topbarEl

  beforeEach(() => {
    mockLayoutConfig.menuMode = 'static'
    mockLayoutState.staticMenuDesktopInactive = false
    mockLayoutState.overlayMenuActive = false
    mockLayoutState.staticMenuMobileActive = false
    mockLayoutState.menuHoverActive = false
    mockIsSidebarActive.value = false

    sidebarEl = document.createElement('div')
    sidebarEl.className = 'layout-sidebar'
    document.body.appendChild(sidebarEl)

    topbarEl = document.createElement('button')
    topbarEl.className = 'layout-menu-button'
    document.body.appendChild(topbarEl)
  })

  afterEach(() => {
    document.body.removeChild(sidebarEl)
    document.body.removeChild(topbarEl)
    vi.restoreAllMocks()
  })

  function mountComponent() {
    return mount(AppLayout, {
      global: {
        stubs: {
          AppTopbar: true,
          AppFooter: true,
          Toast: true,
          RouterView: true
        }
      }
    })
  }

  it('cobertura total unbindOutsideClickListener (ramo verdadeiro e falso)', async () => {
    const wrapper = mountComponent()

    // 1. Vincula o listener
    wrapper.vm.bindOutsideClickListener()
    expect(wrapper.vm.outsideClickListener).not.toBeNull()

    // 2. RAMO VERDADEIRO: unbind com valor existente
    wrapper.vm.unbindOutsideClickListener()
    expect(wrapper.vm.outsideClickListener).toBeNull()

    // 3. RAMO FALSO: unbind com valor nulo/já desvinculado
    wrapper.vm.unbindOutsideClickListener()
    expect(wrapper.vm.outsideClickListener).toBeNull()
  })

  it('cobertura total de curto-circuito em isOutsideClicked', async () => {
    const wrapper = mountComponent()
    
    wrapper.vm.bindOutsideClickListener()
    mockLayoutState.overlayMenuActive = true

    const sidebarChild = document.createElement('span')
    sidebarEl.appendChild(sidebarChild)

    const topbarChild = document.createElement('i')
    topbarEl.appendChild(topbarChild)

    // Teste 1: Clique exatamente na Sidebar (sidebarEl.isSameNode = true)
    sidebarEl.dispatchEvent(new MouseEvent('click', { bubbles: true }))
    expect(mockLayoutState.overlayMenuActive).toBe(true)

    // Teste 2: Clique em filho da Sidebar (sidebarEl.contains = true)
    sidebarChild.dispatchEvent(new MouseEvent('click', { bubbles: true }))
    expect(mockLayoutState.overlayMenuActive).toBe(true)

    // Teste 3: Clique exatamente no Botão Topbar (topbarEl.isSameNode = true)
    topbarEl.dispatchEvent(new MouseEvent('click', { bubbles: true }))
    expect(mockLayoutState.overlayMenuActive).toBe(true)

    // Teste 4: Clique em filho do Topbar (topbarEl.contains = true)
    topbarChild.dispatchEvent(new MouseEvent('click', { bubbles: true }))
    expect(mockLayoutState.overlayMenuActive).toBe(true)

    // Teste 5: Clique totalmente fora (Todos avaliados como FALSE)
    const outsideEl = document.createElement('div')
    document.body.appendChild(outsideEl)
    outsideEl.dispatchEvent(new MouseEvent('click', { bubbles: true }))

    expect(mockLayoutState.overlayMenuActive).toBe(false)
    document.body.removeChild(outsideEl)
  })

  it('cobertura do watcher (ativação e desativação da sidebar)', async () => {
    mountComponent()

    // Dispara ramo 'if (newVal)'
    mockIsSidebarActive.value = true
    await vi.waitFor(() => {})

    // Dispara ramo 'else' (unbindOutsideClickListener via watcher)
    mockIsSidebarActive.value = false
    await vi.waitFor(() => {})
  })

  it('cobertura linha 23: avalia todos os cenários de curto-circuito da classe layout-static-inactive', async () => {
    const wrapper = mountComponent()

    // Cenário 1: Condição 1 é FALSE (staticMenuDesktopInactive = false, menuMode = 'static')
    mockLayoutState.staticMenuDesktopInactive = false
    mockLayoutConfig.menuMode = 'static'
    await wrapper.vm.$nextTick()
    expect(wrapper.find('.layout-wrapper').classes()).not.toContain('layout-static-inactive')

    // Cenário 2: Condição 1 é TRUE, mas Condição 2 é FALSE (staticMenuDesktopInactive = true, menuMode = 'overlay')
    mockLayoutState.staticMenuDesktopInactive = true
    mockLayoutConfig.menuMode = 'overlay'
    await wrapper.vm.$nextTick()
    expect(wrapper.find('.layout-wrapper').classes()).not.toContain('layout-static-inactive')

    // Cenário 3: Ambas são TRUE (staticMenuDesktopInactive = true, menuMode = 'static')
    mockLayoutState.staticMenuDesktopInactive = true
    mockLayoutConfig.menuMode = 'static'
    await wrapper.vm.$nextTick()
    expect(wrapper.find('.layout-wrapper').classes()).toContain('layout-static-inactive')
  })

})