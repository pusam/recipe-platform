<template>
  <div class="star-rating" :class="{ readonly }">
    <button
      v-for="n in 5"
      :key="n"
      class="star"
      :class="{ filled: n <= displayValue, hovered: !readonly && n <= hoverValue }"
      :disabled="readonly"
      @mouseenter="readonly || (hoverValue = n)"
      @mouseleave="readonly || (hoverValue = 0)"
      @click="readonly || $emit('update:modelValue', n)"
    >★</button>
  </div>
</template>

<script>
export default {
  name: 'StarRating',
  props: {
    modelValue: { type: Number, default: 0 },
    readonly: { type: Boolean, default: false }
  },
  emits: ['update:modelValue'],
  data() {
    return { hoverValue: 0 }
  },
  computed: {
    displayValue() {
      return this.hoverValue || this.modelValue
    }
  }
}
</script>

<style scoped>
.star-rating { display: inline-flex; gap: 2px; }
.star {
  background: none; border: none; cursor: pointer;
  font-size: 1.4rem; color: rgba(255,255,255,0.2);
  transition: color 0.15s, transform 0.1s;
  padding: 0;
  line-height: 1;
}
.star.filled { color: #f59e0b; }
.star.hovered { color: #fbbf24; transform: scale(1.15); }
.star-rating.readonly .star { cursor: default; }
</style>
