import api from './api'

export function isAuthenticated() {
  return !!localStorage.getItem('token') || !!sessionStorage.getItem('token')
}

export function login(response, remember) {
  const storage = remember ? localStorage : sessionStorage

  storage.setItem('token', response.token)
  storage.setItem('perfis', response.perfis)

  api.defaults.headers.common['Authorization'] = `Bearer ${response.token}`
}

export function logout() {
  localStorage.removeItem('token')
  sessionStorage.removeItem('token')
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