import { describe, expect, it } from 'vitest'
import {
  formatCpfCnpj,
  formatDate,
  formatDateTime,
  formatNumber,
  formatPhone,
  formatarCEP,
  onlyDigits,
  toDate
} from '../util'

describe('util.js', () => {
  describe('formatCpfCnpj()', () => {
    it('retorna string vazia quando o valor é falso ou nulo', () => {
      expect(formatCpfCnpj('')).toBe('')
      expect(formatCpfCnpj(null)).toBe('')
      expect(formatCpfCnpj(undefined)).toBe('')
    })

    it('formata um CPF valido com 11 digitos', () => {
      expect(formatCpfCnpj('12345678901')).toBe('123.456.789-01')
      expect(formatCpfCnpj('123.456.789-01')).toBe('123.456.789-01')
    })

    it('formata um CNPJ valido com 14 digitos', () => {
      expect(formatCpfCnpj('12345678000195')).toBe('12.345.678/0001-95')
    })

    it('retorna o valor original se a quantidade de digitos for diferente de 11 e 14', () => {
      expect(formatCpfCnpj('12345')).toBe('12345')
    })
  })

  describe('formatDate()', () => {
    it('retorna string vazia quando isoString é falso ou nulo', () => {
      expect(formatDate('')).toBe('')
      expect(formatDate(null)).toBe('')
    })

    it('formata uma data ISO para o padrao pt-BR em UTC', () => {
      expect(formatDate('2026-09-09T10:00:00Z')).toBe('09/09/2026')
    })
  })

  describe('formatDateTime()', () => {
    it('retorna string vazia quando isoString é falso ou nulo', () => {
      expect(formatDateTime('')).toBe('')
      expect(formatDateTime(null)).toBe('')
    })

    it('formata data e hora ISO para o padrao pt-BR em UTC', () => {
      expect(formatDateTime('2026-09-09T14:30:15Z')).toBe('09/09/2026 14:30:15')
    })
  })

  describe('toDate()', () => {
    it('retorna null quando isoString é falso ou nulo', () => {
      expect(toDate('')).toBeNull()
      expect(toDate(null)).toBeNull()
    })

    it('converte string de data YYYY-MM-DD para objeto Date local', () => {
      const date = toDate('2026-09-09')
      expect(date).toBeInstanceOf(Date)
      expect(date.getFullYear()).toBe(2026)
      expect(date.getMonth()).toBe(8) // Setembro e mes 8 (base zero)
      expect(date.getDate()).toBe(9)
    })
  })

  describe('formatPhone()', () => {
    it('retorna string vazia quando valor é falso ou nulo', () => {
      expect(formatPhone('')).toBe('')
      expect(formatPhone(null)).toBe('')
    })

    it('formata celular com 11 digitos', () => {
      expect(formatPhone('11987654321')).toBe('(11) 98765-4321')
    })

    it('formata telefone fixo com 10 digitos', () => {
      expect(formatPhone('1133334444')).toBe('(11) 3333-4444')
    })

    it('retorna o valor original se a quantidade de digitos for diferente de 10 e 11', () => {
      expect(formatPhone('123')).toBe('123')
    })
  })

  describe('formatarCEP()', () => {
    it('retorna string vazia quando cep é falso ou nulo', () => {
      expect(formatarCEP('')).toBe('')
      expect(formatarCEP(null)).toBe('')
    })

    it('formata CEP valido com mais de 5 digitos', () => {
      expect(formatarCEP('90000000')).toBe('90000-000')
    })

    it('retorna CEP sem hifen se tiver 5 ou menos digitos', () => {
      expect(formatarCEP('90000')).toBe('90000')
    })
  })

  describe('onlyDigits()', () => {
    it('remove todos os caracteres nao numericos', () => {
      expect(onlyDigits('123.456.789-00')).toBe('12345678900')
      expect(onlyDigits('ABC-123')).toBe('123')
    })
  })

  describe('formatNumber()', () => {
    it('retorna null quando o valor é falso, zero ou nulo', () => {
      expect(formatNumber(null)).toBeNull()
      expect(formatNumber(0)).toBeNull()
      expect(formatNumber(undefined)).toBeNull()
    })

    it('formata numero com valores padrao de locale e options', () => {
      const result = formatNumber(1234.56)
      // Aceita espaco inquebravel ou formato padrao do ambiente pt-BR
      expect(result.replace(/\u00a0/g, ' ')).toBe('1.234,56')
    })

    it('formata numero aceitando locale e options customizados', () => {
      const result = formatNumber(1234.56, 'en-US', { style: 'currency', currency: 'USD' })
      expect(result).toBe('$1,234.56')
    })
  })
})