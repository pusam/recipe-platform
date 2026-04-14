import axios from 'axios'
import { authStore } from '@/stores/auth'

const apiClient = axios.create({
  baseURL: '/api',
  timeout: 60000,
  headers: { 'Content-Type': 'application/json' }
})

let onUnauthorized = null
export function setUnauthorizedHandler(fn) { onUnauthorized = fn }

apiClient.interceptors.request.use((config) => {
  const token = authStore.token
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

apiClient.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      const wasAuthenticated = authStore.isAuthenticated
      authStore.clear()
      if (wasAuthenticated && onUnauthorized) onUnauthorized()
    }
    return Promise.reject(err)
  }
)

export default apiClient

export const recipeAPI = {
  list({ q = '', tag = '', page = 0, size = 20 } = {}) {
    return apiClient.get('/recipes', { params: { q, tag, page, size } })
  },
  get(id) { return apiClient.get(`/recipes/${id}`) },
  create(youtubeUrl) { return apiClient.post('/recipes', { youtubeUrl }) },
  update(id, payload) { return apiClient.put(`/recipes/${id}`, payload) },
  remove(id) { return apiClient.delete(`/recipes/${id}`) }
}

export const authAPI = {
  signup(payload) { return apiClient.post('/auth/signup', payload) },
  login(payload) { return apiClient.post('/auth/login', payload) },
  me() { return apiClient.get('/users/me') },
  updateProfile(payload) { return apiClient.put('/users/me', payload) },
  changePassword(payload) { return apiClient.put('/users/me/password', payload) }
}

export const favoriteAPI = {
  list() { return apiClient.get('/users/me/favorites') },
  status(recipeId) { return apiClient.get(`/favorites/${recipeId}`) },
  add(recipeId) { return apiClient.post(`/favorites/${recipeId}`) },
  remove(recipeId) { return apiClient.delete(`/favorites/${recipeId}`) }
}
