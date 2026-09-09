import { beforeEach, describe, expect, it, vi } from 'vitest'
import api from '../api'
import {
  eAdmin,
  getUserId,
  isAuthenticated,
  login,
  logout,
  sha256Hex,
  temPerfil
} from '../auth'

vi.mock('../api', () => ({
  default: {
    defaults: {
      headers: {
        common: {}
      }
    }
  }
}))

describe('src/util/auth.js', () => {
  beforeEach(() => {
    localStorage.clear()
    sessionStorage.clear()
    api.defaults.headers.common = {}
  })

  describe('isAuthenticated()', () => {
    it('retorna false quando não há userId em nenhum storage', () => {
      expect(isAuthenticated()).toBe(false)
    })

    it('retorna true quando userId está presente no localStorage', () => {
      localStorage.setItem('userId', '123')
      expect(isAuthenticated()).toBe(true)
    })

    it('retorna true quando userId está presente no sessionStorage', () => {
      sessionStorage.setItem('userId', '456')
      expect(isAuthenticated()).toBe(true)
    })
  })

  describe('getUserId()', () => {
    it('retorna userId do localStorage se existir', () => {
      localStorage.setItem('userId', 'user-local')
      sessionStorage.setItem('userId', 'user-session')

      expect(getUserId()).toBe('user-local')
    })

    it('retorna userId do sessionStorage se não estiver no localStorage', () => {
      sessionStorage.setItem('userId', 'user-session')

      expect(getUserId()).toBe('user-session')
    })

    it('retorna null se userId não estiver em nenhum storage', () => {
      expect(getUserId()).toBeNull()
    })
  })

  describe('login()', () => {
    it('salva dados no localStorage quando remember for true', () => {
      const response = { id: '10', perfis: 'admin,user', token: 'token-123' }

      login(response, true)

      expect(localStorage.getItem('userId')).toBe('10')
      expect(localStorage.getItem('perfis')).toBe('admin,user')
      expect(sessionStorage.getItem('userId')).toBeNull()
      expect(api.defaults.headers.common['Authorization']).toBe('Bearer token-123')
    })

    it('salva dados no sessionStorage quando remember for false', () => {
      const response = { id: '20', perfis: 'user', token: 'token-456' }

      login(response, false)

      expect(sessionStorage.getItem('userId')).toBe('20')
      expect(sessionStorage.getItem('perfis')).toBe('user')
      expect(localStorage.getItem('userId')).toBeNull()
      expect(api.defaults.headers.common['Authorization']).toBe('Bearer token-456')
    })
  })

  describe('logout()', () => {
    it('remove dados de ambos storages e remove o header de Autorização', () => {
      localStorage.setItem('userId', '123')
      localStorage.setItem('perfis', 'admin')
      sessionStorage.setItem('userId', '123')
      sessionStorage.setItem('perfis', 'admin')
      api.defaults.headers.common['Authorization'] = 'Bearer token-123'

      logout()

      expect(localStorage.getItem('userId')).toBeNull()
      expect(localStorage.getItem('perfis')).toBeNull()
      expect(sessionStorage.getItem('userId')).toBeNull()
      expect(sessionStorage.getItem('perfis')).toBeNull()
      expect(api.defaults.headers.common['Authorization']).toBeUndefined()
    })
  })

  describe('temPerfil() e eAdmin()', () => {
    it('retorna true para eAdmin() se o perfil admin estiver no localStorage', () => {
      localStorage.setItem('perfis', 'admin,gestor')

      expect(eAdmin()).toBe(true)
    })

    it('retorna true para temPerfil() se o perfil estiver no sessionStorage', () => {
      sessionStorage.setItem('perfis', 'financeiro,operador')

      expect(temPerfil('financeiro')).toBe(true)
      expect(temPerfil('admin')).toBe(false)
    })

    it('retorna false quando perfis for null em ambos storages', () => {
      expect(temPerfil('admin')).toBe(false)
    })
  })

  describe('sha256Hex()', () => {
    it('gera a hash SHA-256 em formato hexadecimal corretamente', async () => {
      const input = 'senha123'
      const hash = await sha256Hex(input)

      expect(hash).toBe('55a5e9e78207b4df8699d60886fa070079463547b095d1a05bc719bb4e6cd251')
    })
  })
})