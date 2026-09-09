import { describe, expect, it } from 'vitest'
import { CountryService } from '../CountryService'

describe('CountryService', () => {
  it('retorna a lista de países via getData() de forma síncrona', () => {
    const data = CountryService.getData()

    expect(Array.isArray(data)).toBe(true)
    expect(data.length).toBeGreaterThan(0)
    expect(data[0]).toHaveProperty('name')
    expect(data[0]).toHaveProperty('code')
  })

  it('retorna uma Promise resolvida com os dados dos países via getCountries()', async () => {
    const data = await CountryService.getCountries()

    expect(Array.isArray(data)).toBe(true)
    expect(data).toEqual(CountryService.getData())
  })

  it('garante que todos os itens do array possuem name e code válidos', () => {
    const data = CountryService.getData()

    data.forEach((country) => {
      expect(typeof country.name).toBe('string')
      expect(typeof country.code).toBe('string')
      expect(country.code.length).toBe(2)
    })
  })
})