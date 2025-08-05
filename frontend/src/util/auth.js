import api from './api'

export function isAuthenticated() {
  return !!localStorage.getItem('token')
}

export function login(response) {
  localStorage.setItem('token', response.token)

  api.defaults.headers.common['Authorization'] = `Bearer ${response.token}`
}

export function logout() {
  localStorage.removeItem('token')

  delete api.defaults.headers.common['Authorization']
}

export async function sha256Hex(input) {
  const data = new TextEncoder().encode(input)
  const hashBuffer = await crypto.subtle.digest('SHA-256', data)
  const hashArray = Array.from(new Uint8Array(hashBuffer))

  return hashArray.map(b => b.toString(16).padStart(2, '0')).join('')
}
