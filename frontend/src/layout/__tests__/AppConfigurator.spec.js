import { mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { reactive, ref } from 'vue'
import AppConfigurator from '../AppConfigurator.vue'

// Mocks do PrimeUIX Themes e Layout
const mockUpdatePreset = vi.fn()
const mockUpdateSurfacePalette = vi.fn()
const mockPreset = vi.fn()
const mockSurfacePalette = vi.fn()
const mockUse = vi.fn()

const mockT = vi.fn(() => ({
  preset: mockPreset,
  surfacePalette: mockSurfacePalette,
  use: mockUse
}))

vi.mock('@primeuix/themes', () => ({
  updatePreset: (...args) => mockUpdatePreset(...args),
  updateSurfacePalette: (...args) => mockUpdateSurfacePalette(...args),
  $t: (...args) => mockT(...args)
}))

vi.mock('@primeuix/themes/aura', () => ({ default: { name: 'Aura' } }))
vi.mock('@primeuix/themes/lara', () => ({ default: { name: 'Lara' } }))
vi.mock('@primeuix/themes/nora', () => ({ default: { name: 'Nora' } }))

const mockLayoutConfig = reactive({
  preset: 'Aura',
  primary: 'emerald',
  surface: 'slate',
  menuMode: 'static'
})
const mockIsDarkTheme = ref(false)

vi.mock('@/layout/composables/layout', () => ({
  useLayout: () => ({
    layoutConfig: mockLayoutConfig,
    isDarkTheme: mockIsDarkTheme
  })
}))

// Stub do SelectButton
const SelectButtonStub = {
  name: 'SelectButton',
  props: ['modelValue', 'options', 'allowEmpty', 'optionLabel', 'optionValue'],
  emits: ['update:modelValue', 'change'],
  template: `
    <div class="select-button-stub">
      <button 
        v-for="opt in options" 
        :key="typeof opt === 'object' ? opt.value : opt"
        class="select-option-btn"
        @click="$emit('update:modelValue', typeof opt === 'object' ? opt.value : opt); $emit('change')"
      >
        {{ typeof opt === 'object' ? opt.label : opt }}
      </button>
    </div>
  `
}

describe('AppConfigurator.vue', () => {
  let localStorageStore = {}

  beforeEach(() => {
    vi.clearAllMocks()
    localStorageStore = {}

    vi.spyOn(Storage.prototype, 'getItem').mockImplementation((key) => localStorageStore[key] || null)
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation((key, value) => {
      localStorageStore[key] = String(value)
    })

    mockPreset.mockReturnValue({ preset: mockPreset, surfacePalette: mockSurfacePalette, use: mockUse })
    mockSurfacePalette.mockReturnValue({ preset: mockPreset, surfacePalette: mockSurfacePalette, use: mockUse })
    mockUse.mockReturnValue({})

    mockLayoutConfig.preset = 'Aura'
    mockLayoutConfig.primary = 'emerald'
    mockLayoutConfig.surface = 'slate'
    mockLayoutConfig.menuMode = 'static'
    mockIsDarkTheme.value = false
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  function mountComponent() {
    return mount(AppConfigurator, {
      global: {
        stubs: {
          SelectButton: SelectButtonStub
        }
      }
    })
  }

  it('inicializa o tema com a cor salva no localStorage se existente', () => {
    localStorageStore['primary-color'] = JSON.stringify({ name: 'blue', palette: {} })
    
    mountComponent()

    expect(mockUpdatePreset).toHaveBeenCalled()
  })

  it('inicializa com a primeira cor primária caso o localStorage esteja vazio', () => {
    mountComponent()

    expect(mockUpdatePreset).toHaveBeenCalledWith(
      expect.objectContaining({
        semantic: expect.objectContaining({
          primary: expect.any(Object)
        })
      })
    )
  })

  it('cobre 100% das branches da linha 174 (primaryColor.name === "noir")', () => {
    const wrapper = mountComponent()
    const primaryButtons = wrapper.findAll('button[title]')

    // Branch TRUE: primaryColor.name === 'noir'
    const noirBtn = primaryButtons.find((btn) => btn.attributes('title') === 'noir')
    expect(noirBtn.attributes('style')).toContain('background-color: var(--text-color)')

    // Branch FALSE: primaryColor.name !== 'noir'
    const emeraldBtn = primaryButtons.find((btn) => btn.attributes('title') === 'emerald')
    expect(emeraldBtn.attributes('style')).toContain('background-color: rgb(16, 185, 129)')
  })

  it('cobre 100% das branches da linha 184 (layoutConfig.surface ternário completo)', () => {
    // 1. layoutConfig.surface é Truthy e bate com surface.name
    mockLayoutConfig.surface = 'slate'
    const wrapper1 = mountComponent()
    const slateBtn1 = wrapper1.findAll('button[title]').find((btn) => btn.attributes('title') === 'slate')
    const zincBtn1 = wrapper1.findAll('button[title]').find((btn) => btn.attributes('title') === 'zinc')
    expect(slateBtn1.classes()).toContain('outline-primary')
    expect(zincBtn1.classes()).not.toContain('outline-primary')

    // 2. layoutConfig.surface é Falsy e isDarkTheme = TRUE
    mockLayoutConfig.surface = undefined
    mockIsDarkTheme.value = true
    const wrapper2 = mountComponent()
    const zincBtn2 = wrapper2.findAll('button[title]').find((btn) => btn.attributes('title') === 'zinc')
    const oceanBtn2 = wrapper2.findAll('button[title]').find((btn) => btn.attributes('title') === 'ocean')
    expect(zincBtn2.classes()).toContain('outline-primary') // surface.name === 'zinc' (True)
    expect(oceanBtn2.classes()).not.toContain('outline-primary') // surface.name === 'zinc' (False)

    // 3. layoutConfig.surface é Falsy e isDarkTheme = FALSE
    mockLayoutConfig.surface = null
    mockIsDarkTheme.value = false
    const wrapper3 = mountComponent()
    const slateBtn3 = wrapper3.findAll('button[title]').find((btn) => btn.attributes('title') === 'slate')
    const oceanBtn3 = wrapper3.findAll('button[title]').find((btn) => btn.attributes('title') === 'ocean')
    expect(slateBtn3.classes()).toContain('outline-primary') // surface.name === 'slate' (True)
    expect(oceanBtn3.classes()).not.toContain('outline-primary') // surface.name === 'slate' (False)
  })

  it('atualiza a cor primária ao clicar em um botão de cor primária (não-noir)', async () => {
    const wrapper = mountComponent()
    const emeraldBtn = wrapper.findAll('button[title]').find((btn) => btn.attributes('title') === 'emerald')
    await emeraldBtn.trigger('click')

    expect(mockLayoutConfig.primary).toBe('emerald')
    expect(Storage.prototype.setItem).toHaveBeenCalledWith('primary-color', expect.anything())
    expect(mockUpdatePreset).toHaveBeenCalled()
  })

  it('aplica corretamente as propriedades estendidas quando a cor selecionada é "noir"', async () => {
    const wrapper = mountComponent()
    const noirBtn = wrapper.findAll('button[title]').find((btn) => btn.attributes('title') === 'noir')
    await noirBtn.trigger('click')

    expect(mockLayoutConfig.primary).toBe('noir')
    expect(mockUpdatePreset).toHaveBeenCalledWith(
      expect.objectContaining({
        semantic: expect.objectContaining({
          primary: expect.objectContaining({
            50: '{surface.50}'
          })
        })
      })
    )
  })

  it('atualiza a paleta de superfície ao clicar em uma opção de surface', async () => {
    const wrapper = mountComponent()
    const oceanBtn = wrapper.findAll('button[title]').find((btn) => btn.attributes('title') === 'ocean')
    await oceanBtn.trigger('click')

    expect(mockLayoutConfig.surface).toBe('ocean')
    expect(mockUpdateSurfacePalette).toHaveBeenCalledWith(
      expect.objectContaining({
        0: '#ffffff',
        50: '#fbfcfc'
      })
    )
  })

  it('altera o preset com sucesso e executa a cadeia de funções de $t()', async () => {
    const wrapper = mountComponent()
    const selectButtons = wrapper.findAllComponents(SelectButtonStub)
    const presetSelect = selectButtons[0]

    const laraOptionBtn = presetSelect.findAll('.select-option-btn').find((btn) => btn.text() === 'Lara')
    await laraOptionBtn.trigger('click')

    expect(mockLayoutConfig.preset).toBe('Lara')
    expect(mockT).toHaveBeenCalled()
    expect(mockPreset).toHaveBeenCalled()
    expect(mockSurfacePalette).toHaveBeenCalled()
    expect(mockUse).toHaveBeenCalledWith({ useDefaultOptions: true })
  })

  it('altera o modo de menu (menuMode)', async () => {
    const wrapper = mountComponent()
    const selectButtons = wrapper.findAllComponents(SelectButtonStub)
    const menuModeSelect = selectButtons[1]

    const overlayOptionBtn = menuModeSelect.findAll('.select-option-btn').find((btn) => btn.text() === 'Overlay')
    await overlayOptionBtn.trigger('click')

    expect(mockLayoutConfig.menuMode).toBe('overlay')
  })

})