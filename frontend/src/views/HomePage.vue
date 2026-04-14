<template>
  <div>
    <section class="hero">
      <h1>YouTube 영상으로 레시피 만들기</h1>
      <p>요리 영상 URL을 붙여넣으면 AI가 재료와 조리 단계를 정리해 저장합니다.</p>
    </section>

    <section class="card">
      <div v-if="!authStore.isAuthenticated" class="auth-notice">
        레시피를 추출하려면 먼저
        <router-link :to="{ path: '/login', query: { redirect: $route.fullPath } }">로그인</router-link>
        하거나 <router-link to="/signup">회원가입</router-link>이 필요합니다.
      </div>
      <div class="input-row">
        <input
          v-model="url"
          type="text"
          placeholder="https://www.youtube.com/watch?v=..."
          @keydown.enter="submit"
          :disabled="loading || !authStore.isAuthenticated"
        />
        <button @click="submit" :disabled="loading || !url.trim() || !authStore.isAuthenticated">
          {{ loading ? stepMessage : '레시피 만들기' }}
        </button>
      </div>
      <p v-if="error" class="error">{{ error }}</p>
      <div v-if="loading" class="progress">
        <div class="bar"><div class="fill" :style="{ width: progress + '%' }"></div></div>
        <p class="loading-hint">{{ stepMessage }} · 보통 10~30초 소요</p>
      </div>
    </section>

    <section v-if="!authStore.isAuthenticated" class="guest-hint">
      <p>
        로그인하면 <router-link to="/signup">가입</router-link>한 내 레시피와
        즐겨찾기를 한눈에 볼 수 있습니다.
      </p>
    </section>

    <section v-if="authStore.isAuthenticated && favorites.length" class="fav-section">
      <div class="section-head">
        <h2>⭐ 내 즐겨찾기</h2>
        <router-link to="/favorites" class="more-link">전체 보기 →</router-link>
      </div>
      <div class="grid">
        <router-link
          v-for="r in favorites.slice(0, 4)"
          :key="r.id"
          :to="`/recipes/${r.id}`"
          class="card recipe-card"
        >
          <img v-if="r.thumbnailUrl" :src="r.thumbnailUrl" :alt="r.title" />
          <div class="meta">
            <h3>{{ r.title }}</h3>
            <p>{{ r.channelName }}</p>
          </div>
        </router-link>
      </div>
    </section>

    <section v-if="authStore.isAuthenticated && recent.length" class="recent">
      <div class="section-head">
        <h2>최근 추출한 레시피</h2>
        <router-link to="/recipes" class="more-link">전체 보기 →</router-link>
      </div>
      <div class="grid">
        <router-link
          v-for="r in recent"
          :key="r.id"
          :to="`/recipes/${r.id}`"
          class="card recipe-card"
        >
          <img v-if="r.thumbnailUrl" :src="r.thumbnailUrl" :alt="r.title" />
          <div class="meta">
            <h3>{{ r.title }}</h3>
            <p>{{ r.channelName }}</p>
            <small>{{ formatDate(r.createdAt) }}</small>
          </div>
        </router-link>
      </div>
    </section>
  </div>
</template>

<script>
import { recipeAPI, favoriteAPI } from '@/utils/api'
import { authStore } from '@/stores/auth'

const LOADING_STEPS = [
  '영상 정보 가져오는 중...',
  '자막 추출 중...',
  'AI가 레시피 분석 중...',
  '재료와 단계 정리 중...',
  '거의 다 됐어요...'
]

export default {
  name: 'HomePage',
  data() {
    return {
      url: '',
      loading: false,
      error: null,
      recent: [],
      favorites: [],
      authStore,
      stepIdx: 0,
      stepTimer: null,
      progress: 0,
      progressTimer: null
    }
  },
  computed: {
    stepMessage() { return LOADING_STEPS[this.stepIdx] }
  },
  mounted() {
    if (authStore.isAuthenticated) {
      this.loadRecent()
      this.loadFavorites()
    }
  },
  beforeUnmount() { this.stopTimers() },
  methods: {
    async loadRecent() {
      try {
        const res = await recipeAPI.list({ size: 6 })
        if (res.data?.success) this.recent = res.data.data || []
      } catch (e) { /* ignore */ }
    },
    async loadFavorites() {
      try {
        const res = await favoriteAPI.list()
        if (res.data?.success) this.favorites = res.data.data || []
      } catch (e) { /* ignore */ }
    },
    async submit() {
      const url = this.url.trim()
      if (!url) return
      this.loading = true
      this.error = null
      this.startTimers()
      try {
        const res = await recipeAPI.create(url)
        if (res.data?.success) {
          this.$router.push(`/recipes/${res.data.data.id}`)
        } else {
          this.error = res.data?.message || '추출에 실패했습니다.'
        }
      } catch (e) {
        this.error = e.response?.data?.message || e.message || '요청 실패'
      } finally {
        this.loading = false
        this.stopTimers()
      }
    },
    startTimers() {
      this.stepIdx = 0
      this.progress = 5
      this.stepTimer = setInterval(() => {
        if (this.stepIdx < LOADING_STEPS.length - 1) this.stepIdx += 1
      }, 4000)
      this.progressTimer = setInterval(() => {
        if (this.progress < 92) this.progress += Math.random() * 3
      }, 400)
    },
    stopTimers() {
      clearInterval(this.stepTimer); this.stepTimer = null
      clearInterval(this.progressTimer); this.progressTimer = null
      this.progress = 0
    },
    formatDate(s) {
      if (!s) return ''
      return new Date(s).toLocaleDateString('ko-KR')
    }
  }
}
</script>

<style scoped>
.hero { text-align: center; margin-bottom: 32px; }
.hero h1 { font-size: 2rem; margin: 0 0 12px; background: linear-gradient(90deg, #f59e0b, #f97316); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
.hero p { opacity: 0.7; margin: 0; }

.card {
  background: rgba(255,255,255,0.05);
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 20px;
  padding: 24px;
}

.auth-notice {
  margin-bottom: 14px;
  padding: 10px 14px;
  background: rgba(245,158,11,0.08);
  border: 1px solid rgba(245,158,11,0.25);
  border-radius: 10px;
  font-size: 0.9rem;
}
.auth-notice a { color: #f59e0b; text-decoration: underline; }

.input-row { display: flex; gap: 12px; }
.input-row input {
  flex: 1;
  padding: 14px 18px;
  border-radius: 12px;
  background: rgba(0,0,0,0.3);
  border: 1px solid rgba(255,255,255,0.1);
  color: inherit;
  font-size: 1rem;
  outline: none;
}
.input-row input:focus { border-color: #f59e0b; }
.input-row button {
  padding: 14px 24px;
  border-radius: 12px;
  border: none;
  background: linear-gradient(135deg, #f59e0b, #f97316);
  color: #1a1a2e;
  font-weight: 700;
  cursor: pointer;
  font-size: 1rem;
  min-width: 160px;
}
.input-row button:disabled { opacity: 0.5; cursor: not-allowed; }
.error { color: #fca5a5; margin: 12px 0 0; }
.progress { margin-top: 14px; }
.progress .bar {
  height: 4px; background: rgba(255,255,255,0.08); border-radius: 999px; overflow: hidden;
}
.progress .fill {
  height: 100%;
  background: linear-gradient(90deg, #f59e0b, #f97316);
  transition: width 0.4s ease-out;
}
.loading-hint { opacity: 0.6; margin: 8px 0 0; font-size: 0.9rem; }

.guest-hint {
  margin-top: 40px; padding: 20px;
  background: rgba(255,255,255,0.03);
  border: 1px dashed rgba(255,255,255,0.1);
  border-radius: 16px; text-align: center;
}
.guest-hint p { margin: 0; opacity: 0.7; font-size: 0.95rem; }
.guest-hint a { color: #f59e0b; }

.fav-section, .recent { margin-top: 40px; }
.section-head {
  display: flex; justify-content: space-between; align-items: baseline;
  margin-bottom: 16px;
}
.section-head h2 { margin: 0; font-size: 1.25rem; }
.more-link { font-size: 0.85rem; color: #f59e0b; opacity: 0.8; }
.more-link:hover { opacity: 1; }
.grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 16px; }
.recipe-card { padding: 0; overflow: hidden; transition: transform 0.2s; display: block; }
.recipe-card:hover { transform: translateY(-4px); }
.recipe-card img { width: 100%; aspect-ratio: 16/9; object-fit: cover; display: block; }
.recipe-card .meta { padding: 12px 16px 16px; }
.recipe-card h3 { font-size: 0.95rem; margin: 0 0 4px; line-height: 1.4; }
.recipe-card p { opacity: 0.6; margin: 0 0 4px; font-size: 0.85rem; }
.recipe-card small { opacity: 0.4; font-size: 0.75rem; }
</style>
