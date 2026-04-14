<template>
  <div v-if="form">
    <h1>레시피 편집</h1>

    <section class="card">
      <label class="field">
        <span>제목</span>
        <input v-model="form.title" type="text" required />
      </label>
      <label class="field">
        <span>요약</span>
        <textarea v-model="form.summary" rows="2"></textarea>
      </label>
      <div class="row">
        <label class="field">
          <span>인분</span>
          <input v-model="form.servings" type="text" placeholder="2인분" />
        </label>
        <label class="field">
          <span>조리시간</span>
          <input v-model="form.cookingTime" type="text" placeholder="약 30분" />
        </label>
        <label class="field">
          <span>난이도</span>
          <select v-model="form.difficulty">
            <option value="">-</option>
            <option>쉬움</option>
            <option>보통</option>
            <option>어려움</option>
          </select>
        </label>
      </div>
      <label class="field">
        <span>태그 (쉼표 구분)</span>
        <input v-model="form.tags" type="text" placeholder="한식,찌개,저녁" />
      </label>
    </section>

    <section class="card">
      <div class="section-head">
        <h2>재료</h2>
        <button class="add-btn" @click="addIngredient">+ 추가</button>
      </div>
      <div v-for="(i, idx) in form.ingredients" :key="idx" class="ing-row">
        <input v-model="i.name" type="text" placeholder="재료명" />
        <input v-model="i.amount" type="text" placeholder="수량" />
        <button class="remove" @click="form.ingredients.splice(idx, 1)">×</button>
      </div>
    </section>

    <section class="card">
      <div class="section-head">
        <h2>만드는 법</h2>
        <button class="add-btn" @click="addStep">+ 추가</button>
      </div>
      <div v-for="(s, idx) in form.steps" :key="idx" class="step-row">
        <div class="step-num">{{ idx + 1 }}</div>
        <div class="step-body">
          <textarea v-model="s.instruction" rows="2" placeholder="조리 단계"></textarea>
          <input v-model="s.tip" type="text" placeholder="팁 (선택)" />
        </div>
        <button class="remove" @click="form.steps.splice(idx, 1)">×</button>
      </div>
    </section>

    <div class="actions">
      <button class="cancel" @click="$router.back()">취소</button>
      <button class="save" @click="save" :disabled="saving">
        {{ saving ? '저장 중...' : '저장' }}
      </button>
    </div>
    <p v-if="error" class="error">{{ error }}</p>
  </div>
  <div v-else-if="loadError" class="error">{{ loadError }}</div>
  <div v-else class="loading">불러오는 중...</div>
</template>

<script>
import { recipeAPI } from '@/utils/api'

export default {
  name: 'RecipeEditPage',
  props: ['id'],
  data() {
    return { form: null, loadError: null, error: null, saving: false }
  },
  mounted() { this.load() },
  methods: {
    async load() {
      try {
        const res = await recipeAPI.get(this.id)
        if (res.data?.success) {
          const r = res.data.data
          this.form = {
            title: r.title || '',
            summary: r.summary || '',
            servings: r.servings || '',
            cookingTime: r.cookingTime || '',
            difficulty: r.difficulty || '',
            tags: r.tags || '',
            ingredients: (r.ingredients || []).map(i => ({ name: i.name, amount: i.amount })),
            steps: (r.steps || []).map(s => ({ stepNo: s.stepNo, instruction: s.instruction, tip: s.tip || '' }))
          }
        } else {
          this.loadError = res.data?.message || '불러오기 실패'
        }
      } catch (e) {
        this.loadError = e.response?.data?.message || e.message
      }
    },
    addIngredient() { this.form.ingredients.push({ name: '', amount: '' }) },
    addStep() { this.form.steps.push({ instruction: '', tip: '' }) },
    async save() {
      this.saving = true
      this.error = null
      try {
        const payload = {
          ...this.form,
          steps: this.form.steps.map((s, idx) => ({ ...s, stepNo: idx + 1 }))
        }
        const res = await recipeAPI.update(this.id, payload)
        if (res.data?.success) {
          this.$router.push(`/recipes/${this.id}`)
        } else {
          this.error = res.data?.message || '저장 실패'
        }
      } catch (e) {
        this.error = e.response?.data?.message || e.message
      } finally {
        this.saving = false
      }
    }
  }
}
</script>

<style scoped>
h1 { margin: 0 0 24px; }
.card {
  background: rgba(255,255,255,0.05);
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 20px;
  padding: 24px;
  margin-bottom: 20px;
}
.section-head {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 16px;
}
.section-head h2 { margin: 0; font-size: 1.15rem; color: #f59e0b; }
.add-btn {
  padding: 6px 14px; border-radius: 8px;
  background: rgba(245,158,11,0.15);
  border: 1px solid rgba(245,158,11,0.3);
  color: #fbbf24; cursor: pointer; font-size: 0.85rem;
}
.add-btn:hover { background: rgba(245,158,11,0.25); }
.field { display: flex; flex-direction: column; gap: 6px; margin-bottom: 14px; }
.field span { font-size: 0.85rem; opacity: 0.7; }
.field input, .field textarea, .field select {
  padding: 10px 12px;
  border-radius: 10px;
  background: rgba(0,0,0,0.3);
  border: 1px solid rgba(255,255,255,0.1);
  color: inherit; font-size: 0.95rem;
  outline: none; font-family: inherit;
}
.field input:focus, .field textarea:focus, .field select:focus { border-color: #f59e0b; }
.row { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 12px; }
.ing-row, .step-row { display: flex; gap: 8px; margin-bottom: 10px; align-items: flex-start; }
.ing-row input { flex: 1; padding: 10px 12px; border-radius: 10px; background: rgba(0,0,0,0.3); border: 1px solid rgba(255,255,255,0.1); color: inherit; outline: none; }
.step-num {
  flex-shrink: 0; width: 32px; height: 32px; display: flex; align-items: center; justify-content: center;
  border-radius: 50%; background: rgba(245,158,11,0.15); color: #fbbf24; font-weight: 700; font-size: 0.9rem;
  margin-top: 4px;
}
.step-body { flex: 1; display: flex; flex-direction: column; gap: 6px; }
.step-body textarea, .step-body input {
  padding: 10px 12px; border-radius: 10px;
  background: rgba(0,0,0,0.3); border: 1px solid rgba(255,255,255,0.1);
  color: inherit; outline: none; font-family: inherit; font-size: 0.95rem;
}
.remove {
  flex-shrink: 0; width: 32px; height: 32px;
  background: transparent; border: 1px solid rgba(248,113,113,0.3);
  color: #fca5a5; border-radius: 8px; cursor: pointer;
  margin-top: 4px;
}
.remove:hover { background: rgba(248,113,113,0.1); }
.actions { display: flex; justify-content: flex-end; gap: 12px; margin-top: 20px; }
.cancel, .save { padding: 12px 24px; border-radius: 10px; cursor: pointer; font-size: 0.95rem; }
.cancel { background: transparent; border: 1px solid rgba(255,255,255,0.15); color: inherit; }
.save {
  background: linear-gradient(135deg, #f59e0b, #f97316); border: none;
  color: #1a1a2e; font-weight: 700;
}
.save:disabled { opacity: 0.5; cursor: not-allowed; }
.error { color: #fca5a5; text-align: center; margin-top: 12px; }
.loading, .loadError { text-align: center; padding: 60px 0; opacity: 0.6; }
</style>
