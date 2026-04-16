<template>
  <div class="app">
    <template v-if="!$route.meta.standalone">
      <header class="nav">
        <router-link to="/" class="brand">🍳 Recipe Platform</router-link>
        <nav>
          <router-link to="/">홈</router-link>
          <router-link to="/recipes">레시피</router-link>
          <router-link v-if="authStore.isAuthenticated" to="/favorites">⭐ 즐겨찾기</router-link>
          <router-link v-if="authStore.isAuthenticated" to="/profile">프로필</router-link>
        </nav>
        <div class="auth">
          <template v-if="authStore.isAuthenticated">
            <span class="user">{{ authStore.user?.username }}</span>
            <button class="link-btn" @click="logout">로그아웃</button>
          </template>
          <template v-else>
            <router-link to="/login">로그인</router-link>
            <router-link to="/signup" class="signup-link">회원가입</router-link>
          </template>
        </div>
      </header>
      <main class="content">
        <router-view />
      </main>
    </template>
    <router-view v-else />
  </div>
</template>

<script>
import { authStore } from '@/stores/auth'

export default {
  name: 'App',
  data() { return { authStore } },
  methods: {
    logout() {
      authStore.clear()
      if (this.$route.meta.requiresAuth) this.$router.push('/')
    }
  }
}
</script>

<style>
* { box-sizing: border-box; }
html, body, #app { margin: 0; padding: 0; min-height: 100vh; font-family: 'Pretendard', -apple-system, sans-serif; }
body {
  background: linear-gradient(135deg, #0f0f1a 0%, #1a1a2e 50%, #16213e 100%);
  background-attachment: fixed;
  color: #e4e4e7;
}
a { color: inherit; text-decoration: none; }
.app { min-height: 100vh; }
.nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 32px;
  background: rgba(255,255,255,0.03);
  border-bottom: 1px solid rgba(255,255,255,0.08);
  backdrop-filter: blur(8px);
  position: sticky; top: 0; z-index: 10;
  gap: 24px;
}
.brand { font-size: 1.25rem; font-weight: 700; }
.nav nav { display: flex; gap: 16px; flex: 1; margin-left: 24px; }
.nav nav a { padding: 6px 12px; border-radius: 8px; font-size: 0.95rem; opacity: 0.7; transition: 0.2s; }
.nav nav a:hover { opacity: 1; background: rgba(255,255,255,0.05); }
.nav nav a.router-link-active { opacity: 1; color: #f59e0b; }
.auth { display: flex; align-items: center; gap: 12px; font-size: 0.9rem; }
.auth a { padding: 6px 12px; border-radius: 8px; opacity: 0.8; }
.auth a:hover { opacity: 1; background: rgba(255,255,255,0.05); }
.auth .signup-link {
  background: linear-gradient(135deg, #f59e0b, #f97316);
  color: #1a1a2e; font-weight: 700; opacity: 1;
}
.auth .user { opacity: 0.8; }
.link-btn { background: none; border: none; color: inherit; cursor: pointer; padding: 6px 12px; font-size: 0.9rem; opacity: 0.7; }
.link-btn:hover { opacity: 1; }
.content { max-width: 960px; margin: 0 auto; padding: 32px 24px; }

@media (max-width: 768px) {
  .nav {
    flex-wrap: wrap;
    padding: 12px 16px;
    gap: 8px;
  }
  .brand { font-size: 1.1rem; }
  .nav nav {
    order: 3;
    width: 100%;
    margin-left: 0;
    gap: 4px;
    justify-content: center;
    border-top: 1px solid rgba(255,255,255,0.06);
    padding-top: 8px;
  }
  .nav nav a { font-size: 0.85rem; padding: 6px 10px; }
  .auth { font-size: 0.8rem; gap: 8px; }
  .auth .signup-link { padding: 5px 10px; font-size: 0.8rem; }
  .content { padding: 20px 14px; }
}

@media (max-width: 380px) {
  .nav nav { gap: 2px; }
  .nav nav a { font-size: 0.78rem; padding: 5px 6px; }
  .content { padding: 16px 10px; }
}
</style>
