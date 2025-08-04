export function isAuthenticated() {
  return !!localStorage.getItem('user')
}

export function login(usuario) {
  localStorage.setItem('user', JSON.stringify(usuario))
}

export function logout() {
  localStorage.removeItem('user')
}
