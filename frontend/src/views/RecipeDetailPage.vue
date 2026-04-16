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

    <!-- 평점 & 리뷰 -->
    <section class="card rating-section">
      <h2>평점</h2>

      <!-- 집계 -->
      <div class="rating-overview">
        <div class="big-score">
          <span class="number">{{ avgDisplay }}</span>
          <StarRating :modelValue="Math.round(recipe.avgScore || 0)" readonly />
          <span class="count">{{ ratingCount }}개 리뷰</span>
        </div>
        <div class="distribution">
          <div v-for="n in [5,4,3,2,1]" :key="n" class="dist-row">
            <span class="label">{{ n }}★</span>
            <div class="bar-bg">
              <div class="bar-fill" :style="{ width: distPercent(n) + '%' }"></div>
            </div>
            <span class="dist-count">{{ distribution[n] || 0 }}</span>
          </div>
        </div>
      </div>

      <!-- 내 평점 -->
      <div v-if="authStore.isAuthenticated" class="my-rating">
        <h3>{{ myRating ? '내 평점 수정' : '평점 남기기' }}</h3>
        <div class="rate-form">
          <StarRating v-model="myScore" />
          <textarea
            v-model="myComment"
            placeholder="한줄평 (선택)"
            maxlength="2000"
            rows="2"
          ></textarea>
          <div class="rate-actions">
            <button class="submit-btn" @click="submitRating" :disabled="myScore < 1 || ratingLoading">
              {{ ratingLoading ? '저장 중...' : '저장' }}
            </button>
            <button v-if="myRating" class="delete-rating" @click="deleteRating" :disabled="ratingLoading">
              삭제
            </button>
          </div>
        </div>
      </div>
      <div v-else class="login-to-rate">
        <router-link :to="{ path: '/login', query: { redirect: $route.fullPath } }">
          로그인하고 평점 남기기
        </router-link>
      </div>

      <!-- 리뷰 목록 -->
      <div v-if="reviews.length" class="reviews">
        <h3>리뷰 ({{ ratingCount }})</h3>
        <div v-for="r in reviews" :key="r.id" class="review-item">
          <div class="review-header">
            <StarRating :modelValue="r.score" readonly />
            <span class="reviewer">{{ r.username }}</span>
            <span class="review-date">{{ formatDate(r.createdAt) }}</span>
          </div>
          <p v-if="r.comment" class="review-comment">{{ r.comment }}</p>
        </div>
        <button v-if="reviewHasNext" class="more-btn" @click="loadMoreReviews" :disabled="reviewLoading">
          {{ reviewLoading ? '불러오는 중...' : '더 보기' }}
        </button>
      </div>
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
import { recipeAPI, favoriteAPI, ratingAPI } from '@/utils/api'
import { authStore } from '@/stores/auth'
import StarRating from '@/components/StarRating.vue'

export default {
  name: 'RecipeDetailPage',
  components: { StarRating },
  props: ['id'],
  data() {
    return {
      recipe: null,
      error: null,
      isFavorite: false,
      favLoading: false,
      shared: false,
      authStore,
      // ratings
      myScore: 0,
      myComment: '',
      myRating: null,
      distribution: { 1: 0, 2: 0, 3: 0, 4: 0, 5: 0 },
      ratingCount: 0,
      reviews: [],
      reviewPage: 0,
      reviewHasNext: false,
      reviewLoading: false,
      ratingLoading: false
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
    },
    avgDisplay() {
      const v = this.recipe?.avgScore
      return v != null ? v.toFixed(1) : '-'
    }
  },
  mounted() { this.load() },
  watch: { id() { this.load() } },
  methods: {
    async load() {
      this.recipe = null
      this.error = null
      this.isFavorite = false
      this.reviews = []
      this.reviewPage = 0
      try {
        const res = await recipeAPI.get(this.id)
        if (res.data?.success) {
          this.recipe = res.data.data
          this.ratingCount = this.recipe.ratingCount || 0
          if (authStore.isAuthenticated) await this.loadFavoriteStatus()
          await this.loadRatings()
        } else {
          this.error = res.data?.message || '불러오기 실패'
        }
      } catch (e) {
        this.error = e.response?.data?.message || e.message
      }
    },
    async loadRatings() {
      try {
        const res = await ratingAPI.list(this.id, { page: 0, size: 20 })
        if (res.data?.success) {
          this.reviews = res.data.data || []
          this.reviewHasNext = !!res.data.page?.hasNext
          this.reviewPage = 0
          this.applyAggregate(res.data.aggregate)
        }
      } catch { /* ignore */ }
    },
    async loadMoreReviews() {
      this.reviewLoading = true
      try {
        this.reviewPage++
        const res = await ratingAPI.list(this.id, { page: this.reviewPage, size: 20 })
        if (res.data?.success) {
          this.reviews.push(...(res.data.data || []))
          this.reviewHasNext = !!res.data.page?.hasNext
        }
      } finally { this.reviewLoading = false }
    },
    applyAggregate(agg) {
      if (!agg) return
      this.ratingCount = agg.ratingCount || 0
      if (this.recipe) {
        this.recipe.avgScore = agg.averageScore
        this.recipe.ratingCount = agg.ratingCount
      }
      this.distribution = agg.distribution || { 1: 0, 2: 0, 3: 0, 4: 0, 5: 0 }
      if (agg.myRating) {
        this.myRating = agg.myRating
        this.myScore = agg.myRating.score
        this.myComment = agg.myRating.comment || ''
      } else {
        this.myRating = null
        this.myScore = 0
        this.myComment = ''
      }
    },
    distPercent(n) {
      if (!this.ratingCount) return 0
      return ((this.distribution[n] || 0) / this.ratingCount * 100).toFixed(1)
    },
    async submitRating() {
      if (this.myScore < 1) return
      this.ratingLoading = true
      try {
        const res = await ratingAPI.upsert(this.id, { score: this.myScore, comment: this.myComment || null })
        if (res.data?.success) {
          this.applyAggregate(res.data.aggregate)
          await this.loadRatings()
        }
      } catch (e) {
        alert(e.response?.data?.message || e.message)
      } finally { this.ratingLoading = false }
    },
    async deleteRating() {
      if (!confirm('평점을 삭제하시겠습니까?')) return
      this.ratingLoading = true
      try {
        const res = await ratingAPI.remove(this.id)
        if (res.data?.success) {
          this.applyAggregate(res.data.aggregate)
          this.myRating = null
          this.myScore = 0
          this.myComment = ''
          await this.loadRatings()
        }
      } catch (e) {
        alert(e.response?.data?.message || e.message)
      } finally { this.ratingLoading = false }
    },
    formatDate(dt) {
      if (!dt) return ''
      return new Date(dt).toLocaleDateString('ko-KR', { year: 'numeric', month: 'short', day: 'numeric' })
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

/* ---------- Rating section ---------- */
.rating-section h2 { margin-bottom: 20px; }
.rating-overview { display: flex; gap: 32px; flex-wrap: wrap; margin-bottom: 24px; }
.big-score { display: flex; flex-direction: column; align-items: center; gap: 4px; min-width: 100px; }
.big-score .number { font-size: 2.4rem; font-weight: 700; color: #f59e0b; line-height: 1; }
.big-score .count { font-size: 0.8rem; opacity: 0.5; }
.distribution { flex: 1; min-width: 200px; display: flex; flex-direction: column; gap: 4px; }
.dist-row { display: flex; align-items: center; gap: 8px; font-size: 0.82rem; }
.dist-row .label { width: 28px; text-align: right; opacity: 0.7; }
.bar-bg { flex: 1; height: 8px; background: rgba(255,255,255,0.08); border-radius: 4px; overflow: hidden; }
.bar-fill { height: 100%; background: #f59e0b; border-radius: 4px; transition: width 0.3s; }
.dist-count { width: 28px; opacity: 0.5; font-size: 0.75rem; }

.my-rating { margin-bottom: 24px; }
.my-rating h3 { font-size: 1rem; margin: 0 0 12px; opacity: 0.9; }
.rate-form { display: flex; flex-direction: column; gap: 10px; }
.rate-form textarea {
  background: rgba(0,0,0,0.3); border: 1px solid rgba(255,255,255,0.1);
  border-radius: 10px; padding: 10px 14px; color: inherit; resize: vertical;
  font-size: 0.9rem; outline: none;
}
.rate-form textarea:focus { border-color: #f59e0b; }
.rate-actions { display: flex; gap: 8px; }
.submit-btn {
  padding: 8px 20px; border-radius: 10px; border: none;
  background: linear-gradient(135deg, #f59e0b, #f97316);
  color: #1a1a2e; font-weight: 600; cursor: pointer; font-size: 0.9rem;
}
.submit-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.delete-rating {
  padding: 8px 14px; border-radius: 10px; background: none;
  border: 1px solid rgba(248,113,113,0.4); color: #fca5a5; cursor: pointer; font-size: 0.85rem;
}
.delete-rating:hover { background: rgba(248,113,113,0.1); }
.login-to-rate { margin-bottom: 20px; }
.login-to-rate a { color: #f59e0b; opacity: 0.8; font-size: 0.9rem; }

.reviews h3 { font-size: 1rem; margin: 0 0 14px; opacity: 0.9; }
.review-item {
  padding: 14px 0; border-bottom: 1px solid rgba(255,255,255,0.06);
}
.review-item:last-child { border-bottom: none; }
.review-header { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.reviewer { font-weight: 500; font-size: 0.9rem; }
.review-date { font-size: 0.75rem; opacity: 0.4; }
.review-comment { margin: 8px 0 0; font-size: 0.9rem; line-height: 1.5; opacity: 0.85; }
.reviews .more-btn {
  display: block; margin: 16px auto 0; padding: 8px 24px; border-radius: 10px;
  background: rgba(255,255,255,0.06); border: 1px solid rgba(255,255,255,0.12);
  color: inherit; cursor: pointer; font-size: 0.85rem;
}
.reviews .more-btn:hover:not(:disabled) { background: rgba(245,158,11,0.1); }

.loading, .error { text-align: center; padding: 60px 0; opacity: 0.6; }
.error { color: #fca5a5; }
</style>
