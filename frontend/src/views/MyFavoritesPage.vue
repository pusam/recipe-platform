<template>
  <div>
    <h1>⭐ 내 즐겨찾기</h1>
    <p v-if="!loading && recipes.length === 0" class="empty">
      아직 즐겨찾기한 레시피가 없습니다. <router-link to="/recipes">레시피 둘러보기</router-link>
    </p>
    <div v-else class="grid">
      <router-link
        v-for="r in recipes"
        :key="r.id"
        :to="`/recipes/${r.id}`"
        class="card recipe-card"
      >
        <img v-if="r.thumbnailUrl" :src="r.thumbnailUrl" :alt="r.title" />
        <div class="meta">
          <h3>{{ r.title }}</h3>
          <p>{{ r.channelName }}</p>
          <div class="tags">
            <span v-if="r.cookingTime">⏱ {{ r.cookingTime }}</span>
            <span v-if="r.difficulty">📊 {{ r.difficulty }}</span>
          </div>
        </div>
      </router-link>
    </div>
  </div>
</template>

<script>
import { favoriteAPI } from '@/utils/api'

export default {
  name: 'MyFavoritesPage',
  data() { return { recipes: [], loading: true } },
  mounted() { this.load() },
  methods: {
    async load() {
      this.loading = true
      try {
        const res = await favoriteAPI.list()
        if (res.data?.success) this.recipes = res.data.data || []
      } finally { this.loading = false }
    }
  }
}
</script>

<style scoped>
h1 { margin: 0 0 24px; }
.empty { opacity: 0.6; }
.empty a { color: #f59e0b; }
.grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 16px; }
.card {
  background: rgba(255,255,255,0.05);
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 20px;
  overflow: hidden;
  display: block;
  transition: transform 0.2s;
}
.card:hover { transform: translateY(-4px); }
.recipe-card img { width: 100%; aspect-ratio: 16/9; object-fit: cover; display: block; }
.meta { padding: 14px 18px 18px; }
.meta h3 { font-size: 1rem; margin: 0 0 4px; line-height: 1.4; }
.meta p { opacity: 0.6; font-size: 0.85rem; margin: 0 0 8px; }
.tags { display: flex; gap: 8px; flex-wrap: wrap; }
.tags span { font-size: 0.75rem; opacity: 0.7; padding: 2px 8px; background: rgba(255,255,255,0.06); border-radius: 999px; }
</style>
