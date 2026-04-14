import { reactive } from 'vue'

const STORAGE_KEY = 'recipe_auth'

function load() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

const initial = load()

export const authStore = reactive({
  token: initial?.token || null,
  user: initial?.user || null,

  get isAuthenticated() {
    return !!this.token
  },

  set(token, user) {
    this.token = token
    this.user = user
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ token, user }))
  },

  clear() {
    this.token = null
    this.user = null
    localStorage.removeItem(STORAGE_KEY)
  }
})
