<template>
  <div class="min-h-screen bg-[#0a0a0a] flex items-center justify-center">
    <div class="w-full max-w-sm px-6">
      <!-- Logo -->
      <div class="text-center mb-8">
        <div class="w-16 h-16 rounded-2xl bg-[#FF9500]/10 flex items-center justify-center mx-auto mb-4">
          <i class="fas fa-shield-alt text-[#FF9500] text-2xl"></i>
        </div>
        <h1 class="text-xl font-bold text-white">运营后台</h1>
        <p class="text-xs text-gray-500 mt-1">SoundRead 内容管理系统</p>
      </div>

      <!-- 登录表单 -->
      <form @submit.prevent="handleLogin" class="space-y-4">
        <div>
          <label class="text-[11px] text-gray-400 mb-1.5 block">手机号</label>
          <input v-model="form.phone" type="text" placeholder="请输入手机号"
                 class="w-full bg-[#1a1a1c] border border-white/10 rounded-xl px-4 py-3 text-sm text-white outline-none focus:border-[#FF9500]/50 transition-colors" />
        </div>
        <div>
          <label class="text-[11px] text-gray-400 mb-1.5 block">密码</label>
          <input v-model="form.password" type="password" placeholder="请输入密码"
                 class="w-full bg-[#1a1a1c] border border-white/10 rounded-xl px-4 py-3 text-sm text-white outline-none focus:border-[#FF9500]/50 transition-colors" />
        </div>

        <div v-if="error" class="text-xs text-red-400 bg-red-500/10 rounded-lg px-3 py-2">
          {{ error }}
        </div>

        <button type="submit" :disabled="logging"
                class="w-full py-3 rounded-xl bg-[#FF9500] text-black text-sm font-bold hover:bg-[#FF9500]/90 transition-colors cursor-pointer disabled:opacity-50">
          <i v-if="logging" class="fas fa-circle-notch fa-spin mr-1.5"></i>
          {{ logging ? '登录中...' : '登录' }}
        </button>
      </form>

      <p class="text-center text-[10px] text-gray-600 mt-8">仅限管理员登录</p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const form = reactive({ phone: '', password: '' })
const logging = ref(false)
const error = ref('')

const handleLogin = async () => {
  error.value = ''
  if (!form.phone || !form.password) {
    error.value = '请输入手机号和密码'
    return
  }
  logging.value = true
  try {
    await authStore.login(form)
    router.replace('/')
  } catch (e) {
    error.value = e.message || '登录失败'
  } finally {
    logging.value = false
  }
}
</script>
