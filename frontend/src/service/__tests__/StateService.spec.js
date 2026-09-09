import { describe, expect, it } from 'vitest'
import { StateService } from '../StateService'

describe('StateService', () => {
  it('retorna a lista com os 27 estados brasileiros via getData()', () => {
    const data = StateService.getData()

    expect(Array.isArray(data)).toBe(true)
    expect(data).toHaveLength(27)
    expect(data[0]).toEqual({ name: 'Acre', code: 'AC' })
  })

  it('retorna uma Promise resolvida com os estados via getStates()', async () => {
    const data = await StateService.getStates()

    expect(Array.isArray(data)).toBe(true)
    expect(data).toEqual(StateService.getData())
  })

  it('garante a estrutura correta (name e sigla de 2 caracteres) para cada estado', () => {
    const data = StateService.getData()

    data.forEach((state) => {
      expect(typeof state.name).toBe('string')
      expect(typeof state.code).toBe('string')
      expect(state.code).toHaveLength(2)
      expect(state.code).toMatch(/^[A-Z]{2}$/)
    })
  })
})