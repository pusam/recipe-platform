<template>
  <div class="auth-page">
    <div class="bg-decor"></div>
    <div class="panel">
      <router-link to="/" class="brand">🍳 Recipe Platform</router-link>
      <h1>다시 오신 것을 환영해요</h1>
      <p class="subtitle">저장해둔 레시피를 보려면 로그인하세요.</p>
      <div v-if="$route.query.expired" class="expired-notice">
        세션이 만료되었습니다. 다시 로그인해주세요.
      </div>

      <form @submit.prevent="submit">
        <label>
          <span>이메일</span>
          <input v-model="email" type="email" autocomplete="email" required placeholder="you@example.com" />
        </label>
        <label>
          <span>비밀번호</span>
          <input v-model="password" type="password" autocomplete="current-password" required placeholder="••••••••" />
        </label>
        <button type="submit" :disabled="loading">
          {{ loading ? '로그인 중...' : '로그인' }}
        </button>
        <p v-if="error" class="error">{{ error }}</p>
      </form>

      <div class="divider"><span>또는</span></div>

      <router-link to="/signup" class="secondary-btn">회원가입하고 시작하기</router-link>

      <router-link to="/" class="back-home">← 둘러보기로 돌아가기</router-link>
    </div>
  </div>
</template>

<script>
import { authAPI } from '@/utils/api'
import { authStore } from '@/stores/auth'

export default {
  name: 'LoginPage',
  data() {
    return { email: '', password: '', loading: false, error: null }
  },
  methods: {
    async submit() {
      this.loading = true
      this.error = null
      try {
        const res = await authAPI.login({ email: this.email, password: this.password })
        if (res.data?.success) {
          const { token, userId, email, username } = res.data.data
          authStore.set(token, { id: userId, email, username })
          const redirect = this.$route.query.redirect || '/'
          this.$router.push(redirect)
        } else {
          this.error = res.data?.message || '로그인 실패'
        }
      } catch (e) {
        this.error = e.response?.data?.message || e.message
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  position: relative;
  overflow: hidden;
}
.bg-decor {
  position: absolute; inset: 0;
  background:
    radial-gradient(600px 400px at 15% 20%, rgba(245,158,11,0.15), transparent 60%),
    radial-gradient(500px 500px at 85% 80%, rgba(249,115,22,0.12), transparent 60%);
  pointer-events: none;
}
.panel {
  position: relative;
  width: 100%;
  max-width: 420px;
  background: rgba(20, 20, 35, 0.7);
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 24px;
  padding: 40px 36px;
  backdrop-filter: blur(16px);
  box-shadow: 0 20px 60px rgba(0,0,0,0.4);
}
.brand {
  display: inline-block;
  font-size: 1.1rem;
  font-weight: 700;
  margin-bottom: 24px;
  opacity: 0.9;
}
h1 {
  margin: 0 0 8px;
  font-size: 1.6rem;
  background: linear-gradient(90deg, #f59e0b, #f97316);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}
.subtitle { margin: 0 0 28px; opacity: 0.65; font-size: 0.95rem; }
.expired-notice {
  margin-bottom: 20px; padding: 10px 14px;
  background: rgba(248,113,113,0.1);
  border: 1px solid rgba(248,113,113,0.25);
  border-radius: 10px; font-size: 0.9rem; color: #fca5a5;
}
form { display: flex; flex-direction: column; gap: 14px; }
label { display: flex; flex-direction: column; gap: 6px; }
label span { font-size: 0.85rem; opacity: 0.7; }
input {
  padding: 12px 14px;
  border-radius: 10px;
  background: rgba(0,0,0,0.35);
  border: 1px solid rgba(255,255,255,0.1);
  color: inherit;
  font-size: 1rem;
  outline: none;
  transition: border-color 0.2s;
}
input:focus { border-color: #f59e0b; }
button[type="submit"] {
  margin-top: 8px;
  padding: 13px 20px;
  border-radius: 10px;
  border: none;
  background: linear-gradient(135deg, #f59e0b, #f97316);
  color: #1a1a2e;
  font-weight: 700;
  cursor: pointer;
  font-size: 1rem;
  transition: transform 0.15s;
}
button[type="submit"]:hover:not(:disabled) { transform: translateY(-1px); }
button[type="submit"]:disabled { opacity: 0.5; cursor: not-allowed; }
.error { color: #fca5a5; margin: 6px 0 0; font-size: 0.9rem; text-align: center; }
.divider {
  display: flex; align-items: center; gap: 12px;
  margin: 28px 0 20px;
  font-size: 0.8rem; opacity: 0.5;
}
.divider::before, .divider::after {
  content: ''; flex: 1; height: 1px; background: rgba(255,255,255,0.1);
}
.secondary-btn {
  display: block;
  padding: 12px;
  border-radius: 10px;
  text-align: center;
  border: 1px solid rgba(255,255,255,0.15);
  background: rgba(255,255,255,0.03);
  font-size: 0.95rem;
  transition: 0.2s;
}
.secondary-btn:hover {
  background: rgba(245,158,11,0.1);
  border-color: rgba(245,158,11,0.4);
}
.back-home {
  display: block;
  text-align: center;
  margin-top: 24px;
  font-size: 0.85rem;
  opacity: 0.5;
}
.back-home:hover { opacity: 0.8; }
</style>
