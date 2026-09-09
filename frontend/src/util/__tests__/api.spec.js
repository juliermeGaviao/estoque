import axios from 'axios'
import { describe, expect, it, vi } from 'vitest'

vi.mock('axios', () => {
  const createMock = vi.fn((config) => ({
    defaults: config,
    interceptors: {
      request: { use: vi.fn() },
      response: { use: vi.fn() }
    }
  }))

  return {
    default: {
      create: createMock
    }
  }
})

describe('api.js (Axios Instance)', () => {
  it('cria a instância do axios com as configurações corretas', async () => {
    const apiModule = await import('../api')
    const apiInstance = apiModule.default

    expect(axios.create).toHaveBeenCalledWith({
      baseURL: import.meta.env.VITE_API_BASE_URL,
      withCredentials: true
    })

    expect(apiInstance.defaults.withCredentials).toBe(true)
    expect(apiInstance.defaults.baseURL).toBe(import.meta.env.VITE_API_BASE_URL)
  })
})