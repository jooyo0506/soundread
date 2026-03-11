<template>
  <div class="h-full bg-gradient-to-b from-[#1a1a2e] to-[#050505] px-6 pt-24 relative overflow-y-auto hide-scrollbar">
    
    <button @click="router.replace('/')" class="absolute top-14 left-4 w-8 h-8 flex justify-center items-center text-white cursor-pointer hover:bg-white/10 rounded-full transition-colors z-50">
      <i class="fas fa-arrow-left text-sm"></i>
    </button>

    <div class="text-center mb-12">
      <h1 class="text-3xl font-bold text-white tracking-wider mb-2">登录 / 注册</h1>
      <p class="text-sm text-gray-400">欢迎使用声读</p>
    </div>

    <!-- Tab 切换 -->
    <div class="flex p-1 bg-black/40 border border-white/5 rounded-xl mb-8">
      <button 
        @click="loginType = 'password'"
        class="flex-1 py-2.5 text-sm font-bold rounded-lg transition-all"
        :class="loginType === 'password' ? 'bg-[#4F46E5] text-white shadow-lg' : 'text-gray-500 hover:text-gray-300'"
      >密码登录</button>
      <button 
        @click="loginType = 'register'"
        class="flex-1 py-2.5 text-sm font-bold rounded-lg transition-all"
        :class="loginType === 'register' ? 'bg-[#4F46E5] text-white shadow-lg' : 'text-gray-500 hover:text-gray-300'"
      >邀请码注册</button>
    </div>

    <!-- 表单区 -->
    <div class="glass-panel rounded-2xl p-5 mb-6">
      <!-- 手机号 (共用) -->
      <div class="mb-4">
        <input 
          v-model="form.phone" 
          type="text" 
          placeholder="请输入手机号" 
          class="w-full bg-black/30 border border-white/10 rounded-xl px-4 py-3.5 text-white placeholder-gray-500 outline-none focus:border-[#4F46E5] transition-colors"
        >
      </div>

      <!-- 密码登录模式 -->
      <div v-if="loginType === 'password'" class="mb-6">
        <input 
          v-model="form.password" 
          type="password" 
          placeholder="请输入密码" 
          class="w-full bg-black/30 border border-white/10 rounded-xl px-4 py-3.5 text-white placeholder-gray-500 outline-none focus:border-[#4F46E5] transition-colors"
          @keyup.enter="handleAuth"
        >
      </div>

      <!-- 邀请码注册模式 -->
      <template v-if="loginType === 'register'">
        <!-- 邀请码输入 -->
        <div class="mb-4">
          <div class="relative">
            <i class="fas fa-key absolute left-4 top-1/2 -translate-y-1/2 text-[#4F46E5] text-sm"></i>
            <input 
              v-model="form.inviteCode" 
              type="text" 
              placeholder="请输入邀请码" 
              class="w-full bg-black/30 border border-white/10 rounded-xl pl-10 pr-4 py-3.5 text-white placeholder-gray-500 outline-none focus:border-[#4F46E5] transition-colors uppercase tracking-widest"
            >
          </div>
          <p class="text-[10px] text-gray-500 mt-1.5 ml-1">
            <i class="fas fa-info-circle mr-1 text-gray-600"></i>邀请码可从平台运营人员处获取
          </p>
        </div>
        
        <div class="mb-4">
          <input 
            v-model="form.password" 
            type="password" 
            placeholder="设置密码（至少6位）" 
            class="w-full bg-black/30 border border-white/10 rounded-xl px-4 py-3.5 text-white placeholder-gray-500 outline-none focus:border-[#4F46E5] transition-colors"
          >
        </div>
        <div class="mb-6">
          <input 
            v-model="form.confirmPassword" 
            type="password" 
            placeholder="确认密码" 
            class="w-full bg-black/30 border border-white/10 rounded-xl px-4 py-3.5 text-white placeholder-gray-500 outline-none focus:border-[#4F46E5] transition-colors"
            @keyup.enter="handleAuth"
          >
        </div>
      </template>

      <button 
        @click="handleAuth"
        :disabled="loading"
        class="w-full py-3.5 rounded-xl bg-[#4F46E5] text-white font-bold text-base shadow-[0_4px_20px_rgba(79,70,229,0.3)] active:scale-95 transition-all disabled:opacity-70 disabled:active:scale-100 flex justify-center items-center gap-2"
      >
        <i v-if="loading" class="fas fa-circle-notch fa-spin"></i>
        {{ loading ? '处理中...' : (loginType === 'password' ? '登录' : '注册并登录') }}
      </button>
      
      <div class="text-center mt-6">
        <span class="text-xs text-gray-500">
          {{ loginType === 'password' ? '没有账号？' : '已有账号？' }}
          <a href="#" @click.prevent="loginType = loginType === 'password' ? 'register' : 'password'" class="text-[#818CF8] font-bold">
            {{ loginType === 'password' ? '邀请码注册' : '密码登录' }}
          </a>
        </span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { authApi } from '../api/auth'
import { useToastStore } from '../stores/toast'

const router = useRouter()
const toastStore = useToastStore()
const route = useRoute()
const authStore = useAuthStore()

const loginType = ref('password') // 'password' | 'register'
const loading = ref(false)

const form = ref({
  phone: '',
  password: '',
  confirmPassword: '',
  inviteCode: ''
})

const handleAuth = async () => {
  if (!form.value.phone) return toastStore.show('请输入手机号')

  if (loginType.value === 'password') {
    if (!form.value.password) return toastStore.show('请输入密码')
  } else {
    if (!form.value.inviteCode) return toastStore.show('请输入邀请码')
    if (!form.value.password) return toastStore.show('请设置密码')
    if (form.value.password.length < 6) return toastStore.show('密码至少6位')
    if (!form.value.confirmPassword) return toastStore.show('请确认密码')
    if (form.value.password !== form.value.confirmPassword) {
      return toastStore.show('两次密码输入不一致')
    }
  }

  loading.value = true
  try {
    if (loginType.value === 'password') {
      await authStore.login({ phone: form.value.phone, password: form.value.password })
    } else {
      // 邀请码注册
      const res = await authApi.register({
        phone: form.value.phone,
        inviteCode: form.value.inviteCode.trim().toUpperCase(),
        password: form.value.password,
        confirmPassword: form.value.confirmPassword
      })
      authStore.setToken(res.token)
      authStore.user = { userId: res.userId, nickname: res.nickname, vip: res.vip, admin: res.admin }
      if (res.policy) authStore.setPolicy(res.policy)
    }
    
    const redirect = route.query.redirect || '/'
    await router.replace(redirect).catch(() => {})
  } catch (err) {
    if (err && err.message && !err.message.includes('成功')) {
      toastStore.show(err.message || '操作失败')
    }
  } finally {
    loading.value = false
    // 兜底跳转
    if (authStore.token && route.name === 'Login') {
      const redirect = route.query.redirect || '/'
      router.replace(redirect).catch(() => {
        window.location.href = redirect
      })
    }
  }
}
</script>
