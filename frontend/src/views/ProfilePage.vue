<template>
  <div class="wrap">
    <h1>프로필</h1>

    <section class="card">
      <h2>기본 정보</h2>
      <div class="field">
        <span class="label">이메일</span>
        <div class="readonly">{{ email }}</div>
      </div>
      <label class="field">
        <span class="label">닉네임</span>
        <input v-model="username" type="text" minlength="2" maxlength="100" />
      </label>
      <button class="primary" @click="saveProfile" :disabled="savingProfile">
        {{ savingProfile ? '저장 중...' : '닉네임 변경' }}
      </button>
      <p v-if="profileMsg" class="msg" :class="{ err: profileErr }">{{ profileMsg }}</p>
    </section>

    <section class="card">
      <h2>비밀번호 변경</h2>
      <label class="field">
        <span class="label">현재 비밀번호</span>
        <input v-model="currentPw" type="password" autocomplete="current-password" />
      </label>
      <label class="field">
        <span class="label">새 비밀번호 (6자 이상)</span>
        <input v-model="newPw" type="password" autocomplete="new-password" minlength="6" />
      </label>
      <button class="primary" @click="changePassword" :disabled="savingPw">
        {{ savingPw ? '변경 중...' : '비밀번호 변경' }}
      </button>
      <p v-if="pwMsg" class="msg" :class="{ err: pwErr }">{{ pwMsg }}</p>
    </section>
  </div>
</template>

<script>
import { authAPI } from '@/utils/api'
import { authStore } from '@/stores/auth'

export default {
  name: 'ProfilePage',
  data() {
    return {
      email: authStore.user?.email || '',
      username: authStore.user?.username || '',
      currentPw: '', newPw: '',
      savingProfile: false, savingPw: false,
      profileMsg: '', profileErr: false,
      pwMsg: '', pwErr: false
    }
  },
  methods: {
    async saveProfile() {
      this.savingProfile = true
      this.profileMsg = ''
      try {
        const res = await authAPI.updateProfile({ username: this.username })
        if (res.data?.success) {
          authStore.set(authStore.token, { ...authStore.user, username: res.data.data.username })
          this.profileErr = false
          this.profileMsg = '닉네임이 변경되었습니다.'
        } else {
          this.profileErr = true
          this.profileMsg = res.data?.message || '변경 실패'
        }
      } catch (e) {
        this.profileErr = true
        this.profileMsg = e.response?.data?.message || e.message
      } finally { this.savingProfile = false }
    },
    async changePassword() {
      if (!this.currentPw || !this.newPw) {
        this.pwErr = true
        this.pwMsg = '현재/새 비밀번호를 모두 입력하세요.'
        return
      }
      this.savingPw = true
      this.pwMsg = ''
      try {
        const res = await authAPI.changePassword({
          currentPassword: this.currentPw,
          newPassword: this.newPw
        })
        if (res.data?.success) {
          this.pwErr = false
          this.pwMsg = '비밀번호가 변경되었습니다.'
          this.currentPw = ''
          this.newPw = ''
        } else {
          this.pwErr = true
          this.pwMsg = res.data?.message || '변경 실패'
        }
      } catch (e) {
        this.pwErr = true
        this.pwMsg = e.response?.data?.message || e.message
      } finally { this.savingPw = false }
    }
  }
}
</script>

<style scoped>
.wrap { max-width: 560px; margin: 0 auto; }
h1 { margin: 0 0 24px; }
.card {
  background: rgba(255,255,255,0.05);
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 20px;
  padding: 28px;
  margin-bottom: 20px;
}
.card h2 { margin: 0 0 20px; font-size: 1.1rem; color: #f59e0b; }
.field { display: flex; flex-direction: column; gap: 6px; margin-bottom: 16px; }
.label { font-size: 0.85rem; opacity: 0.7; }
.readonly { padding: 10px 14px; background: rgba(0,0,0,0.2); border-radius: 10px; opacity: 0.7; }
input {
  padding: 10px 14px;
  border-radius: 10px;
  background: rgba(0,0,0,0.3);
  border: 1px solid rgba(255,255,255,0.1);
  color: inherit; font-size: 0.95rem; outline: none;
}
input:focus { border-color: #f59e0b; }
.primary {
  padding: 11px 20px;
  border-radius: 10px; border: none;
  background: linear-gradient(135deg, #f59e0b, #f97316);
  color: #1a1a2e; font-weight: 700; cursor: pointer; font-size: 0.95rem;
}
.primary:disabled { opacity: 0.5; cursor: not-allowed; }
.msg { margin-top: 12px; font-size: 0.9rem; color: #a7f3d0; }
.msg.err { color: #fca5a5; }
</style>
