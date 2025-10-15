import api from './api'

export function isAuthenticated() {
  return !!localStorage.getItem('userId') || !!sessionStorage.getItem('userId')
}

export function getUserId() {
  const result = localStorage.getItem('userId')

  if (result) return result

  return sessionStorage.getItem('userId')
}

export function login(response, remember) {
  const storage = remember ? localStorage : sessionStorage

  storage.setItem('userId', response.id)
  storage.setItem('perfis', response.perfis)

  api.defaults.headers.common['Authorization'] = `Bearer ${response.token}`
}

export function logout() {
  localStorage.removeItem('userId')
  sessionStorage.removeItem('userId')
  localStorage.removeItem('perfis')
  sessionStorage.removeItem('perfis')
  delete api.defaults.headers.common['Authorization']
}

export async function sha256Hex(input) {
  const data = new TextEncoder().encode(input)
  const hashBuffer = await crypto.subtle.digest('SHA-256', data)
  const hashArray = Array.from(new Uint8Array(hashBuffer))

  return hashArray.map(b => b.toString(16).padStart(2, '0')).join('')
}

export function eAdmin() {
  return temPerfil('admin')
}

export function temPerfil(perfil) {
  const perfis = getProfiles()

  return perfis.includes(perfil)
}

function getProfiles() {
  if (localStorage.getItem('perfis')) {
    return localStorage.getItem('perfis')
  }

  return sessionStorage.getItem('perfis')
}