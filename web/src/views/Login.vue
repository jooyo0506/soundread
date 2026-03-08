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
        @click="loginType = 'sms'"
        class="flex-1 py-2.5 text-sm font-bold rounded-lg transition-all"
        :class="loginType === 'sms' ? 'bg-[#4F46E5] text-white shadow-lg' : 'text-gray-500 hover:text-gray-300'"
      >验证码登录</button>
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

      <!-- 验证码登录/注册模式 -->
      <template v-if="loginType === 'sms'">
        <div class="mb-4 flex gap-3">
          <input 
            v-model="form.code" 
            type="text" 
            placeholder="输入验证码" 
            class="flex-1 bg-black/30 border border-white/10 rounded-xl px-4 py-3.5 text-white placeholder-gray-500 outline-none focus:border-[#4F46E5] transition-colors"
          >
          <button 
            @click="sendSms"
            :disabled="countdown > 0"
            class="w-28 bg-[#4F46E5]/20 text-[#818CF8] rounded-xl text-xs font-bold border border-[#4F46E5]/30 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            {{ countdown > 0 ? `${countdown}s 后重试` : '获取验证码' }}
          </button>
        </div>
        <div class="mb-4">
          <input 
            v-model="form.password" 
            type="password" 
            placeholder="设置密码" 
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
        <p class="text-[10px] text-gray-500 mb-4 -mt-4">
          <i class="fas fa-info-circle text-gray-600 mr-1"></i>
          新用户注册需设置密码，已有账号可直接使用密码登录
        </p>
      </template>

      <button 
        @click="handleAuth"
        :disabled="loading"
        class="w-full py-3.5 rounded-xl bg-[#4F46E5] text-white font-bold text-base shadow-[0_4px_20px_rgba(79,70,229,0.3)] active:scale-95 transition-all disabled:opacity-70 disabled:active:scale-100 flex justify-center items-center gap-2"
      >
        <i v-if="loading" class="fas fa-circle-notch fa-spin"></i>
        {{ loading ? '处理中...' : (loginType === 'password' ? '登录' : '登录 / 注册') }}
      </button>
      
      <div class="text-center mt-6">
        <span class="text-xs text-gray-500">没有账号？ 
          <a href="#" @click.prevent="loginType = 'sms'" class="text-[#818CF8] font-bold">验证码注册登录</a>
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

const loginType = ref('password') // 'password' | 'sms'
const loading = ref(false)
const countdown = ref(0)
let timer = null

const form = ref({
  phone: '',
  password: '',
  confirmPassword: '',
  code: ''
})

// 组件卸载时清除倒计时，防止内存泄漏
onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
})

const sendSms = async () => {
  if (!form.value.phone) {
    toastStore.show('请输入手机号')
    return
  }
  try {
    const res = await authApi.getSmsCode(form.value.phone)
    toastStore.show(`【测试使用】您的验证码是: ${res}`)
    
    countdown.value = 60
    timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
  } catch (err) {
    toastStore.show(err.message || '发送失败')
  }
}

const handleAuth = async () => {
  if (!form.value.phone) return toastStore.show('请输入手机号')
  if (loginType.value === 'password' && !form.value.password) return toastStore.show('请输入密码')
  if (loginType.value === 'sms' && !form.value.code) return toastStore.show('请输入验证码')

  // 验证码模式：前端校验密码一致性
  if (loginType.value === 'sms') {
    if (!form.value.password) return toastStore.show('请设置密码')
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
      const res = await authApi.smsLogin({
        phone: form.value.phone,
        code: form.value.code,
        password: form.value.password,
        confirmPassword: form.value.confirmPassword
      })
      authStore.setToken(res.token)
      authStore.user = { userId: res.userId, nickname: res.nickname, vip: res.vip, admin: res.admin }
      if (res.policy) authStore.setPolicy(res.policy)
    }
    
    // 强制使用 Vue Router 的异步进行跳转，并捕获任何已知的 Navigation Guard 重复跳转错
    const redirect = route.query.redirect || '/'
    await router.replace(redirect).catch(() => {})
  } catch (err) {
    if (err && err.message && !err.message.includes('成功')) {
      toastStore.show(err.message || '登录异常')
    }
  } finally {
    loading.value = false
    // 强制兜底：只要 Token 写入成功，若 Vue Router 状态机卡死，则硬核 fallback 跳转
    if (authStore.token && route.name === 'Login') {
      const redirect = route.query.redirect || '/'
      router.replace(redirect).catch(() => {
        window.location.href = redirect
      })
    }
  }
}
</script>
