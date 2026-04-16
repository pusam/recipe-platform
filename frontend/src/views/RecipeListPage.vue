<template>
  <div>
    <div class="header-row">
      <h1>레시피</h1>
      <div class="controls">
        <select v-model="sort" @change="reload" class="sort-select">
          <option value="">최신순</option>
          <option value="rating_count">리뷰 많은순</option>
          <option value="score_desc">별점 높은순</option>
          <option value="score_asc">별점 낮은순</option>
        </select>
        <div class="search">
          <input
            v-model="q"
            type="text"
            placeholder="제목 검색..."
            @keydown.enter="reload"
          />
          <button class="search-btn" @click="reload">검색</button>
        </div>
      </div>
    </div>

    <div v-if="activeTag" class="active-filter">
      태그 필터: <span class="tag-chip">#{{ activeTag }}</span>
      <button class="clear" @click="clearTag">× 해제</button>
    </div>

    <p v-if="!loading && recipes.length === 0" class="empty">
      {{ q || activeTag ? '검색 결과가 없습니다.' : '아직 저장된 레시피가 없습니다.' }}
      <router-link v-if="!q && !activeTag" to="/">홈에서 추가</router-link>
    </p>

    <div class="grid">
      <router-link
        v-for="r in recipes"
        :key="r.id"
        :to="`/recipes/${r.id}`"
        class="card recipe-card"
      >
        <img v-if="r.thumbnailUrl" :src="r.thumbnailUrl" :alt="r.title" />
        <div class="meta">
          <h3>{{ r.title }}</h3>
          <p class="channel">{{ r.channelName }}</p>
          <p v-if="r.creatorName" class="creator">by {{ r.creatorName }}</p>
          <div class="tags">
            <span v-if="r.cookingTime">⏱ {{ r.cookingTime }}</span>
            <span v-if="r.difficulty">📊 {{ r.difficulty }}</span>
            <span v-if="r.ratingCount" class="rating-badge">
              ★ {{ r.avgScore != null ? r.avgScore.toFixed(1) : '-' }} ({{ r.ratingCount }})
            </span>
          </div>
          <div v-if="tagsOf(r).length" class="tag-row">
            <button
              v-for="t in tagsOf(r).slice(0, 3)"
              :key="t"
              class="tag-chip clickable"
              @click.prevent.stop="filterByTag(t)"
            >#{{ t }}</button>
          </div>
        </div>
      </router-link>
    </div>

    <div v-if="hasNext" class="more-wrap">
      <button class="more-btn" @click="loadMore" :disabled="loading">
        {{ loading ? '불러오는 중...' : '더 보기' }}
      </button>
    </div>
  </div>
</template>

<script>
import { recipeAPI } from '@/utils/api'

export default {
  name: 'RecipeListPage',
  data() {
    return {
      recipes: [],
      loading: true,
      q: '',
      sort: '',
      activeTag: '',
      page: 0,
      size: 12,
      hasNext: false
    }
  },
  mounted() {
    this.activeTag = this.$route.query.tag || ''
    this.q = this.$route.query.q || ''
    this.sort = this.$route.query.sort || ''
    this.reload()
  },
  watch: {
    '$route.query'() {
      this.activeTag = this.$route.query.tag || ''
      this.q = this.$route.query.q || ''
      this.sort = this.$route.query.sort || ''
      this.reload()
    }
  },
  methods: {
    tagsOf(r) {
      return (r.tags || '').split(',').map(s => s.trim()).filter(Boolean)
    },
    async reload() {
      this.page = 0
      this.recipes = []
      await this.fetch()
    },
    async loadMore() {
      this.page += 1
      await this.fetch()
    },
    async fetch() {
      this.loading = true
      try {
        const res = await recipeAPI.list({
          q: this.q, tag: this.activeTag, sort: this.sort, page: this.page, size: this.size
        })
        if (res.data?.success) {
          this.recipes = this.page === 0
            ? (res.data.data || [])
            : [...this.recipes, ...(res.data.data || [])]
          this.hasNext = !!res.data.page?.hasNext
        }
      } finally { this.loading = false }
    },
    filterByTag(tag) {
      this.$router.push({ query: { ...this.$route.query, tag } })
    },
    clearTag() {
      const q = { ...this.$route.query }
      delete q.tag
      this.$router.push({ query: q })
    }
  }
}
</script>

<style scoped>
.header-row {
  display: flex; justify-content: space-between; align-items: center;
  flex-wrap: wrap; gap: 16px; margin-bottom: 20px;
}
h1 { margin: 0; }
.controls { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.sort-select {
  padding: 10px 14px; border-radius: 10px;
  background: rgba(0,0,0,0.3); border: 1px solid rgba(255,255,255,0.1);
  color: inherit; font-size: 0.9rem; outline: none; cursor: pointer;
}
.sort-select:focus { border-color: #f59e0b; }
.search { display: flex; gap: 8px; }
.search input {
  padding: 10px 14px;
  border-radius: 10px;
  background: rgba(0,0,0,0.3);
  border: 1px solid rgba(255,255,255,0.1);
  color: inherit; font-size: 0.95rem; outline: none; min-width: 220px;
}
.search input:focus { border-color: #f59e0b; }
.search-btn {
  padding: 10px 16px; border-radius: 10px; border: none;
  background: linear-gradient(135deg, #f59e0b, #f97316);
  color: #1a1a2e; font-weight: 600; cursor: pointer;
}
.active-filter {
  margin-bottom: 16px; padding: 10px 14px;
  background: rgba(245,158,11,0.08);
  border: 1px solid rgba(245,158,11,0.25);
  border-radius: 10px; font-size: 0.9rem;
  display: flex; align-items: center; gap: 10px;
}
.active-filter .tag-chip {
  background: rgba(245,158,11,0.2); color: #fbbf24;
  padding: 2px 10px; border-radius: 999px; font-size: 0.85rem;
}
.active-filter .clear {
  background: none; border: none; color: inherit;
  cursor: pointer; opacity: 0.6; font-size: 0.85rem;
}
.active-filter .clear:hover { opacity: 1; }

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
.meta .channel { opacity: 0.6; font-size: 0.85rem; margin: 0 0 2px; }
.meta .creator { opacity: 0.5; font-size: 0.75rem; margin: 0 0 8px; }
.tags { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 8px; }
.tags span { font-size: 0.75rem; opacity: 0.7; padding: 2px 8px; background: rgba(255,255,255,0.06); border-radius: 999px; }
.rating-badge { color: #fbbf24 !important; background: rgba(245,158,11,0.12) !important; opacity: 1 !important; }
.tag-row { display: flex; gap: 6px; flex-wrap: wrap; }
.tag-chip.clickable {
  font-size: 0.72rem; background: rgba(245,158,11,0.1);
  color: #fbbf24; padding: 2px 8px; border-radius: 999px;
  border: none; cursor: pointer;
}
.tag-chip.clickable:hover { background: rgba(245,158,11,0.25); }
.more-wrap { text-align: center; margin-top: 32px; }
.more-btn {
  padding: 12px 32px; border-radius: 12px;
  background: rgba(255,255,255,0.06);
  border: 1px solid rgba(255,255,255,0.15);
  color: inherit; cursor: pointer; font-size: 0.95rem;
}
.more-btn:hover:not(:disabled) { background: rgba(245,158,11,0.1); }
.more-btn:disabled { opacity: 0.5; cursor: not-allowed; }
</style>
