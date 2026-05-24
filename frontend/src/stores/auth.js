import { defineStore } from 'pinia'

const keys = ['token', 'username', 'role']
keys.forEach((key) => localStorage.removeItem(key))

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: sessionStorage.getItem('token') || '',
    username: sessionStorage.getItem('username') || '',
    role: sessionStorage.getItem('role') || ''
  }),
  actions: {
    setSession(payload) {
      this.token = payload.token
      this.username = payload.username
      this.role = payload.role
      sessionStorage.setItem('token', payload.token)
      sessionStorage.setItem('username', payload.username)
      sessionStorage.setItem('role', payload.role)
    },
    logout() {
      this.token = ''
      this.username = ''
      this.role = ''
      keys.forEach((key) => {
        sessionStorage.removeItem(key)
        localStorage.removeItem(key)
      })
    }
  }
})
