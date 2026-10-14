import { defineStore } from 'pinia'
import { ref } from 'vue'
import { clearToken, getToken, setToken as persistToken } from '@/utils/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(getToken())
  const username = ref('')
  const nickname = ref('')

  function setSession(newToken: string, name?: string, nick?: string): void {
    token.value = newToken
    persistToken(newToken)
    if (name) username.value = name
    if (nick) nickname.value = nick
  }

  function logout(): void {
    token.value = ''
    username.value = ''
    nickname.value = ''
    clearToken()
  }

  return { token, username, nickname, setSession, logout }
})
