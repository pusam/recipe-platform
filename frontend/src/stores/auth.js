import { reactive } from 'vue'

const STORAGE_KEY = 'recipe_auth'

// XSS 노출을 줄이기 위해 sessionStorage 사용 (탭 닫을 때 제거, 탭 간 격리).
// 더 강한 보호가 필요하면 백엔드에서 HttpOnly+Secure 쿠키 기반 세션으로 전환 필요.
const storage = typeof window !== 'undefined' ? window.sessionStorage : null

function load() {
  if (!storage) return null
  try {
    const raw = storage.getItem(STORAGE_KEY)
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
    if (storage) storage.setItem(STORAGE_KEY, JSON.stringify({ token, user }))
  },

  clear() {
    this.token = null
    this.user = null
    if (storage) storage.removeItem(STORAGE_KEY)
    // 이전 버전에서 localStorage에 저장된 잔여 토큰 정리
    if (typeof window !== 'undefined' && window.localStorage) {
      window.localStorage.removeItem(STORAGE_KEY)
    }
  }
})
