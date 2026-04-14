<template>
  <div v-if="recipe">
    <a :href="recipe.youtubeUrl" target="_blank" class="thumb-wrap">
      <img v-if="recipe.thumbnailUrl" :src="recipe.thumbnailUrl" :alt="recipe.title" />
      <span class="play">▶ YouTube에서 보기</span>
    </a>

    <div class="title-row">
      <h1>{{ recipe.title }}</h1>
      <div class="actions">
        <button
          v-if="authStore.isAuthenticated"
          class="fav-btn"
          :class="{ active: isFavorite }"
          @click="toggleFavorite"
          :disabled="favLoading"
        >
          {{ isFavorite ? '★ 즐겨찾기됨' : '☆ 즐겨찾기' }}
        </button>
        <router-link
          v-else
          :to="{ path: '/login', query: { redirect: $route.fullPath } }"
          class="fav-btn login-hint"
        >
          ☆ 로그인하고 저장
        </router-link>
        <button class="share-btn" @click="share">
          {{ shared ? '✓ 복사됨' : '🔗 공유' }}
        </button>
      </div>
    </div>

    <p class="channel">
      {{ recipe.channelName }}
      <span v-if="recipe.creatorName" class="creator"> · 등록: {{ recipe.creatorName }}</span>
    </p>
    <p v-if="recipe.summary" class="summary">{{ recipe.summary }}</p>

    <div class="badges">
      <span v-if="recipe.servings">🍽 {{ recipe.servings }}</span>
      <span v-if="recipe.cookingTime">⏱ {{ recipe.cookingTime }}</span>
      <span v-if="recipe.difficulty">📊 {{ recipe.difficulty }}</span>
    </div>

    <div v-if="tagList.length" class="tags">
      <router-link
        v-for="t in tagList"
        :key="t"
        :to="{ path: '/recipes', query: { tag: t } }"
        class="tag"
      >#{{ t }}</router-link>
    </div>

    <section class="card">
      <h2>재료</h2>
      <ul class="ingredients">
        <li v-for="(i, idx) in recipe.ingredients" :key="idx">
          <span class="name">{{ i.name }}</span>
          <span class="amount">{{ i.amount }}</span>
        </li>
      </ul>
    </section>

    <section class="card">
      <h2>만드는 법</h2>
      <ol class="steps">
        <li v-for="s in recipe.steps" :key="s.stepNo">
          <div class="instr">{{ s.instruction }}</div>
          <div v-if="s.tip" class="tip">💡 {{ s.tip }}</div>
        </li>
      </ol>
    </section>

    <div v-if="isOwner" class="owner-actions">
      <router-link :to="`/recipes/${recipe.id}/edit`" class="edit">✏️ 편집</router-link>
      <button class="delete" @click="remove">🗑 삭제</button>
    </div>
  </div>
  <div v-else-if="error" class="error">{{ error }}</div>
  <div v-else class="loading">불러오는 중...</div>
</template>

<script>
import { recipeAPI, favoriteAPI } from '@/utils/api'
import { authStore } from '@/stores/auth'

export default {
  name: 'RecipeDetailPage',
  props: ['id'],
  data() {
    return {
      recipe: null,
      error: null,
      isFavorite: false,
      favLoading: false,
      shared: false,
      authStore
    }
  },
  computed: {
    tagList() {
      return (this.recipe?.tags || '').split(',').map(s => s.trim()).filter(Boolean)
    },
    isOwner() {
      return authStore.isAuthenticated
        && this.recipe?.creatorId
        && authStore.user?.id === this.recipe.creatorId
    }
  },
  mounted() { this.load() },
  watch: { id() { this.load() } },
  methods: {
    async load() {
      this.recipe = null
      this.error = null
      this.isFavorite = false
      try {
        const res = await recipeAPI.get(this.id)
        if (res.data?.success) {
          this.recipe = res.data.data
          if (authStore.isAuthenticated) await this.loadFavoriteStatus()
        } else {
          this.error = res.data?.message || '불러오기 실패'
        }
      } catch (e) {
        this.error = e.response?.data?.message || e.message
      }
    },
    async loadFavoriteStatus() {
      try {
        const res = await favoriteAPI.status(this.id)
        if (res.data?.success) this.isFavorite = !!res.data.data?.favorite
      } catch { /* ignore */ }
    },
    async toggleFavorite() {
      if (!authStore.isAuthenticated) return
      this.favLoading = true
      try {
        if (this.isFavorite) {
          await favoriteAPI.remove(this.id)
          this.isFavorite = false
        } else {
          await favoriteAPI.add(this.id)
          this.isFavorite = true
        }
      } catch (e) {
        alert(e.response?.data?.message || e.message)
      } finally {
        this.favLoading = false
      }
    },
    async share() {
      const url = window.location.href
      try {
        await navigator.clipboard.writeText(url)
        this.shared = true
        setTimeout(() => { this.shared = false }, 2000)
      } catch {
        prompt('링크를 복사하세요', url)
      }
    },
    async remove() {
      if (!confirm('정말 삭제하시겠습니까?')) return
      try {
        await recipeAPI.remove(this.id)
        this.$router.push('/recipes')
      } catch (e) {
        alert(e.response?.data?.message || e.message)
      }
    }
  }
}
</script>

<style scoped>
.thumb-wrap { position: relative; display: block; border-radius: 20px; overflow: hidden; margin-bottom: 24px; }
.thumb-wrap img { width: 100%; aspect-ratio: 16/9; object-fit: cover; display: block; }
.thumb-wrap .play {
  position: absolute; bottom: 16px; right: 16px;
  background: rgba(0,0,0,0.7); color: #fff; padding: 8px 14px; border-radius: 999px; font-size: 0.85rem;
}
.title-row { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; flex-wrap: wrap; }
h1 { margin: 0 0 8px; flex: 1; min-width: 220px; }
.actions { display: flex; gap: 8px; flex-wrap: wrap; }
.fav-btn, .share-btn {
  flex-shrink: 0;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(255,255,255,0.06);
  border: 1px solid rgba(255,255,255,0.15);
  color: inherit;
  cursor: pointer;
  font-size: 0.9rem;
  transition: 0.2s;
  white-space: nowrap;
  text-decoration: none;
}
.fav-btn:hover:not(:disabled), .share-btn:hover { background: rgba(245,158,11,0.15); border-color: rgba(245,158,11,0.4); }
.fav-btn.active { background: rgba(245,158,11,0.25); border-color: #f59e0b; color: #fbbf24; }
.fav-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.fav-btn.login-hint { opacity: 0.7; font-size: 0.85rem; }
.channel { opacity: 0.6; margin: 0 0 16px; }
.creator { opacity: 0.8; }
.summary { opacity: 0.85; line-height: 1.6; margin: 0 0 16px; }
.badges { display: flex; gap: 12px; margin-bottom: 12px; flex-wrap: wrap; }
.badges span { padding: 6px 12px; background: rgba(245,158,11,0.15); border: 1px solid rgba(245,158,11,0.3); border-radius: 999px; font-size: 0.85rem; color: #fbbf24; }
.tags { margin-bottom: 24px; display: flex; gap: 8px; flex-wrap: wrap; }
.tag {
  font-size: 0.8rem; opacity: 0.7; cursor: pointer;
  padding: 3px 10px; border-radius: 999px;
  background: rgba(255,255,255,0.04);
}
.tag:hover { opacity: 1; background: rgba(245,158,11,0.15); color: #fbbf24; }

.card {
  background: rgba(255,255,255,0.05);
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 20px;
  padding: 24px;
  margin-bottom: 20px;
}
.card h2 { margin: 0 0 16px; font-size: 1.15rem; color: #f59e0b; }

.ingredients { list-style: none; padding: 0; margin: 0; }
.ingredients li {
  display: flex; justify-content: space-between;
  padding: 10px 0; border-bottom: 1px dashed rgba(255,255,255,0.1);
}
.ingredients li:last-child { border-bottom: none; }
.ingredients .amount { opacity: 0.7; }

.steps { padding-left: 20px; margin: 0; }
.steps li { margin-bottom: 16px; line-height: 1.6; }
.steps .tip {
  margin-top: 8px; font-size: 0.9rem; padding: 8px 12px;
  background: rgba(245,158,11,0.08); border-left: 3px solid #f59e0b; border-radius: 4px;
}

.owner-actions { display: flex; gap: 12px; margin-top: 24px; }
.edit, .delete {
  padding: 10px 20px; border-radius: 10px; cursor: pointer; text-decoration: none;
}
.edit {
  background: rgba(245,158,11,0.1); border: 1px solid rgba(245,158,11,0.3);
  color: #fbbf24;
}
.edit:hover { background: rgba(245,158,11,0.2); }
.delete {
  background: transparent; border: 1px solid rgba(248,113,113,0.4);
  color: #fca5a5;
}
.delete:hover { background: rgba(248,113,113,0.1); }

.loading, .error { text-align: center; padding: 60px 0; opacity: 0.6; }
.error { color: #fca5a5; }
</style>
