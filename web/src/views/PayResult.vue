<template>
  <div class="min-h-screen bg-[#050505] flex flex-col items-center justify-center px-6">
    <!-- 加载中 -->
    <div v-if="status === 'loading'" class="text-center">
      <div class="w-16 h-16 border-4 border-[#FF9500] border-t-transparent rounded-full animate-spin mx-auto mb-6"></div>
      <p class="text-white font-bold text-lg">正在确认支付结果...</p>
      <p class="text-gray-500 text-sm mt-2">请稍候，通常需要几秒钟</p>
    </div>

    <!-- 支付成功 -->
    <div v-else-if="status === 'success'" class="text-center">
      <div class="w-20 h-20 rounded-full bg-green-500/20 flex items-center justify-center mx-auto mb-6">
        <i class="fas fa-check text-green-400 text-4xl"></i>
      </div>
      <h1 class="text-2xl font-bold text-white mb-2">开通成功</h1>
      <p class="text-gray-400 mb-1">{{ planName }} 已激活</p>
      <p class="text-gray-600 text-sm mb-8">订单号：{{ orderNo }}</p>
      <button @click="goHome" class="px-8 py-3 rounded-full bg-gradient-to-r from-[#FF9500] to-[#FFD60A] text-black font-bold text-base cursor-pointer">
        开始体验
      </button>
    </div>

    <!-- 支付待确认（支付宝回调可能稍有延迟） -->
    <div v-else-if="status === 'pending'" class="text-center">
      <div class="w-20 h-20 rounded-full bg-yellow-500/20 flex items-center justify-center mx-auto mb-6">
        <i class="fas fa-clock text-yellow-400 text-4xl"></i>
      </div>
      <h1 class="text-xl font-bold text-white mb-2">支付确认中</h1>
      <p class="text-gray-400 text-sm mb-6">支付结果正在处理，稍后会自动刷新</p>
      <button @click="pollStatus" class="px-6 py-2.5 rounded-full bg-white/10 text-white text-sm cursor-pointer mr-3">
        手动刷新
      </button>
      <button @click="goHome" class="px-6 py-2.5 rounded-full bg-white/5 text-gray-400 text-sm cursor-pointer">
        返回首页
      </button>
    </div>

    <!-- 支付失败 -->
    <div v-else class="text-center">
      <div class="w-20 h-20 rounded-full bg-red-500/20 flex items-center justify-center mx-auto mb-6">
        <i class="fas fa-xmark text-red-400 text-4xl"></i>
      </div>
      <h1 class="text-xl font-bold text-white mb-2">支付未完成</h1>
      <p class="text-gray-400 text-sm mb-6">如已扣款请联系客服，订单号：{{ orderNo }}</p>
      <button @click="router.push('/vip')" class="px-6 py-2.5 rounded-full bg-[#FF9500] text-black font-bold text-sm cursor-pointer">
        重新开通
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { vipApi } from '../api/vip'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const status = ref('loading')  // loading | success | pending | failed
const orderNo = ref('')
const planName = ref('')
let pollTimer = null
let pollCount = 0

const pollStatus = async () => {
  if (!orderNo.value) {
    status.value = 'failed'
    return
  }
  try {
    const res = await vipApi.queryOrderStatus(orderNo.value)
    if (res.paid) {
      status.value = 'success'
      planName.value = res.planName || ''
      // 刷新用户信息（更新 tierCode）
      await authStore.fetchUserInfo()
      clearInterval(pollTimer)
    } else if (res.status === 'failed') {
      status.value = 'failed'
      clearInterval(pollTimer)
    } else {
      // pending，继续等待
      status.value = pollCount > 0 ? 'pending' : 'loading'
      pollCount++
      if (pollCount >= 20) {
        // 20次（约60s）还没结果，显示 pending 状态
        status.value = 'pending'
        clearInterval(pollTimer)
      }
    }
  } catch (e) {
    console.error('订单状态查询失败', e)
    if (pollCount >= 5) {
      status.value = 'pending'
      clearInterval(pollTimer)
    }
    pollCount++
  }
}

const goHome = async () => {
  sessionStorage.removeItem('pendingOrderNo')
  router.replace('/')
}

onMounted(() => {
  // 优先从 URL 参数获取（支付宝 return_url 可携带参数）
  orderNo.value = route.query.out_trade_no || sessionStorage.getItem('pendingOrderNo') || ''
  // 立即查一次，再每 3s 轮询
  pollStatus()
  pollTimer = setInterval(pollStatus, 3000)
})

onUnmounted(() => {
  clearInterval(pollTimer)
})
</script>
