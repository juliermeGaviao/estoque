// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

// 1. Mocks dos módulos
vi.mock('vue', async () => {
  const actual = await vi.importActual('vue')
  return {
    ...actual,
    createApp: vi.fn(() => ({
      use: vi.fn().mockReturnThis(),
      mount: vi.fn()
    }))
  }
})

vi.mock('../App.vue', () => ({ default: {} }))
vi.mock('../router', () => ({ default: {} }))
vi.mock('../util/api', () => ({
  default: {
    defaults: {
      headers: {
        common: {}
      }
    }
  }
}))

vi.mock('chart.js', () => ({
  Chart: {
    register: vi.fn()
  }
}))

vi.mock('chartjs-plugin-datalabels', () => ({ default: {} }))
vi.mock('@primeuix/themes/aura', () => ({ default: {} }))
vi.mock('primevue/config', () => ({ default: {} }))
vi.mock('primevue/confirmationservice', () => ({ default: {} }))
vi.mock('primevue/toastservice', () => ({ default: {} }))

describe('main.js - Bootstrapping da Aplicação', () => {
  beforeEach(async () => {
    vi.clearAllMocks()
    vi.resetModules()
    localStorage.clear()

    // Reseta o cabeçalho do mock da API entre cada teste
    const api = (await import('../util/api')).default
    delete api.defaults.headers.common['Authorization']
  })

  afterEach(() => {
    localStorage.clear()
  })

  it('deve registrar o plugin de datalabels no ChartJS', async () => {
    const { Chart } = await import('chart.js')
    const ChartDataLabels = (await import('chartjs-plugin-datalabels')).default

    await import('../main.js')

    expect(Chart.register).toHaveBeenCalledWith(ChartDataLabels)
  })

  it('deve definir o cabeçalho Authorization se houver token no localStorage', async () => {
    localStorage.setItem('token', 'meu-token-123')
    const api = (await import('../util/api')).default

    await import('../main.js')

    expect(api.defaults.headers.common['Authorization']).toBe('Bearer meu-token-123')
  })

  it('não deve definir o cabeçalho Authorization se não houver token no localStorage', async () => {
    const api = (await import('../util/api')).default

    await import('../main.js')

    expect(api.defaults.headers.common['Authorization']).toBeUndefined()
  })

  it('deve inicializar e montar o app Vue com os plugins e localização corretos', async () => {
    const { createApp } = await import('vue')
    const router = (await import('../router')).default
    const PrimeVue = (await import('primevue/config')).default
    const ToastService = (await import('primevue/toastservice')).default
    const ConfirmationService = (await import('primevue/confirmationservice')).default

    await import('../main.js')

    const mockApp = createApp.mock.results[0].value

    expect(createApp).toHaveBeenCalled()
    expect(mockApp.use).toHaveBeenCalledWith(router)
    expect(mockApp.use).toHaveBeenCalledWith(
      PrimeVue,
      expect.objectContaining({
        locale: expect.objectContaining({
          choose: 'Selecionar',
          dateFormat: 'dd/mm/yy'
        })
      })
    )
    expect(mockApp.use).toHaveBeenCalledWith(ToastService)
    expect(mockApp.use).toHaveBeenCalledWith(ConfirmationService)
    expect(mockApp.mount).toHaveBeenCalledWith('#app')
  })
})