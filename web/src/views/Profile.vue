<template>
  <div class="h-full bg-[#050505] relative flex flex-col pt-14 hide-scrollbar">
    
    <!-- 顶部状态栏占位 -->
    <div class="absolute top-0 left-0 w-full h-14 bg-gradient-to-b from-black/80 to-transparent z-40"></div>
    
    <div class="flex-1 overflow-y-auto px-5 pb-24 z-10 hide-scrollbar">
      
      <!-- 用户信息头 -->
      <div class="flex items-center gap-4 mb-8 mt-4">
        <div class="w-16 h-16 rounded-full bg-gradient-to-br from-purple-500 to-indigo-500 flex justify-center items-center text-white text-2xl font-bold shadow-lg border-2 border-white/10">
          {{ authStore.user?.nickname?.charAt(0) || 'U' }}
        </div>
        <div class="flex-1">
          <div class="flex items-center gap-2 mb-1">
            <h2 class="text-xl font-bold text-white">{{ authStore.user?.nickname || '未登录用户' }}</h2>
            <span v-if="tierInfo.code !== 'user' && tierInfo.code !== 'guest'" 
                  class="bg-[#FFD60A] text-black text-[9px] font-bold px-1.5 py-0.5 rounded uppercase tracking-wider">
              {{ tierInfo.name }}
            </span>
          </div>
          <p class="text-xs text-gray-500">ID: {{ authStore.user?.userId || '--' }}</p>
        </div>
        <button v-if="!authStore.isLoggedIn" @click="$router.replace('/login')" class="px-4 py-1.5 rounded-full bg-white/10 text-white text-xs font-bold border border-white/20 active:scale-95 transition-transform cursor-pointer">
          登录 / 注册
        </button>
      </div>

      <!-- VIP 引导卡片 -->
      <div v-if="!authStore.isVip" @click="$router.push('/vip')" class="w-full bg-gradient-to-r from-[#FF9500] to-[#FFD60A] rounded-2xl p-4 mb-6 relative overflow-hidden cursor-pointer shadow-[0_4px_15px_rgba(255,149,0,0.2)] active:scale-95 transition-transform">
        <div class="absolute right-0 top-0 w-32 h-32 bg-white/20 rounded-full blur-2xl -mr-10 -mt-10 pointer-events-none"></div>
        <div class="flex justify-between items-center relative z-10">
          <div>
            <h3 class="text-black font-bold text-base mb-0.5 flex items-center gap-1"><i class="fas fa-crown"></i> 开通声读 VIP</h3>
            <p class="text-black/70 text-[10px] font-medium">解锁 AI 双播、情感合成、AI 音乐等特权</p>
          </div>
          <div class="w-8 h-8 rounded-full bg-black/10 flex justify-center items-center text-black">
            <i class="fas fa-chevron-right text-xs"></i>
          </div>
        </div>
      </div>

      <!-- 今日配额仪表板 -->
      <div v-if="authStore.isLoggedIn" class="mb-6">
        <div class="flex justify-between items-center mb-3">
          <h3 class="text-white text-sm font-bold flex items-center gap-2">
            <i class="fas fa-chart-bar text-cyan-400 text-xs"></i> 今日配额
          </h3>
          <button @click="loadQuota" class="text-[10px] text-gray-500 hover:text-gray-300 transition-colors cursor-pointer">
            <i class="fas fa-sync-alt" :class="{ 'fa-spin': loadingQuota }"></i> 刷新
          </button>
        </div>
        
        <div class="grid grid-cols-2 gap-3">
          <div v-for="(item, key) in quotaItems" :key="key"
               class="rounded-2xl p-3.5 border border-white/5 relative overflow-hidden"
               :class="item.bgClass">
            <!-- 背景装饰 -->
            <div class="absolute top-0 right-0 w-16 h-16 rounded-full blur-2xl -mr-6 -mt-6 pointer-events-none" 
                 :class="item.glowClass"></div>
            
            <div class="relative z-10">
              <!-- 图标 + 标签 -->
              <div class="flex items-center gap-1.5 mb-2">
                <i :class="'fas fa-' + item.icon" class="text-xs" :style="{ color: item.iconColor }"></i>
                <span class="text-gray-400 text-[11px] font-medium">{{ item.label }}</span>
              </div>
              
              <!-- 数值 -->
              <div class="flex items-baseline gap-1 mb-2">
                <span class="text-white font-bold text-xl tabular-nums">{{ item.used }}</span>
                <span class="text-gray-600 text-xs">/</span>
                <span class="text-gray-500 text-xs">{{ item.limit === -1 ? '∞' : item.limit }}{{ item.unit }}</span>
              </div>
              
              <!-- 进度条 -->
              <div class="h-1.5 bg-white/5 rounded-full overflow-hidden">
                <div class="h-full rounded-full transition-all duration-500"
                     :style="{ 
                       width: item.limit === -1 ? '5%' : Math.min(100, (item.used / item.limit) * 100) + '%',
                       background: item.barColor 
                     }"></div>
              </div>
              
              <!-- 状态标签 -->
              <div class="mt-1.5 flex justify-end">
                <span v-if="item.limit === 0" class="text-[9px] text-red-400 bg-red-500/10 px-1.5 py-0.5 rounded">
                  <i class="fas fa-lock text-[8px]"></i> 未开通
                </span>
                <span v-else-if="item.limit === -1" class="text-[9px] text-green-400 bg-green-500/10 px-1.5 py-0.5 rounded">无限</span>
                <span v-else-if="item.used >= item.limit" class="text-[9px] text-red-400 bg-red-500/10 px-1.5 py-0.5 rounded">已用完</span>
                <span v-else class="text-[9px] text-gray-500">剩余 {{ item.limit - item.used }}{{ item.unit }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div v-else class="glass-panel rounded-2xl p-4 mb-6 flex justify-center items-center h-[72px] text-gray-500 text-sm">
         登录后查看配额与特权属性
      </div>

      <!-- 功能列表 -->
      <div class="glass-panel rounded-2xl overflow-hidden mb-6">
        <div @click="$router.push('/creations')" class="flex justify-between items-center p-4 border-b border-white/5 active:bg-white/5 transition-colors cursor-pointer">
          <div class="flex items-center gap-3"><i class="fas fa-history text-gray-400 w-4 text-center"></i><span class="text-white text-sm">合成历史</span></div>
          <i class="fas fa-chevron-right text-gray-600 text-[10px]"></i>
        </div>
        <div class="flex justify-between items-center p-4 border-b border-white/5 active:bg-white/5 transition-colors cursor-pointer">
          <div class="flex items-center gap-3"><i class="fas fa-star text-yellow-500 w-4 text-center"></i><span class="text-white text-sm">我的收藏</span></div>
          <i class="fas fa-chevron-right text-gray-600 text-[10px]"></i>
        </div>
        <div class="flex justify-between items-center p-4 active:bg-white/5 transition-colors cursor-pointer">
          <div class="flex items-center gap-3"><i class="fas fa-cog text-gray-400 w-4 text-center"></i><span class="text-white text-sm">设置</span></div>
          <i class="fas fa-chevron-right text-gray-600 text-[10px]"></i>
        </div>
      </div>
      
      <!-- 退出登录 -->
      <button v-if="authStore.isLoggedIn" @click="showLogoutConfirm = true" class="w-full py-3.5 rounded-xl glass-panel text-red-400 font-bold text-sm active:bg-white/5 transition-colors cursor-pointer">
        退出登录
      </button>

    </div>

    <!-- 退出确认弹窗 -->
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="showLogoutConfirm" class="fixed inset-0 z-[9999] flex items-center justify-center bg-black/60 backdrop-blur-sm" @click.self="showLogoutConfirm = false">
          <div class="bg-[#1a1a1a] rounded-2xl p-6 mx-8 w-full max-w-xs border border-white/10 shadow-2xl">
            <h3 class="text-white text-base font-bold text-center mb-2">退出登录</h3>
            <p class="text-gray-400 text-sm text-center mb-6">确定要退出当前账号吗？</p>
            <div class="flex gap-3">
              <button @click="showLogoutConfirm = false" class="flex-1 py-2.5 rounded-xl bg-white/10 text-white text-sm font-medium active:bg-white/20 transition-colors cursor-pointer">
                取消
              </button>
              <button @click="confirmLogout" class="flex-1 py-2.5 rounded-xl bg-red-500 text-white text-sm font-bold active:bg-red-600 transition-colors cursor-pointer">
                确认退出
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useAuthStore } from '../stores/auth'
import { useRouter } from 'vue-router'
import { authApi } from '../api/auth'

const authStore = useAuthStore()
const router = useRouter()

const loadingQuota = ref(false)
const tierInfo = reactive({ code: 'user', name: '普通用户' })

// 配额项配置 (样式)
const quotaConfig = {
  ttsChars:   { bgClass: 'bg-gradient-to-br from-[#0a0f1a] to-[#0a1520]', glowClass: 'bg-cyan-500/20', iconColor: '#06b6d4', barColor: 'linear-gradient(90deg, #06b6d4, #3b82f6)' },
  ttsV2Chars: { bgClass: 'bg-gradient-to-br from-[#1a0a14] to-[#180a12]', glowClass: 'bg-rose-500/20', iconColor: '#f43f5e', barColor: 'linear-gradient(90deg, #f43f5e, #f59e0b)' },
  aiScript:   { bgClass: 'bg-gradient-to-br from-[#100a1a] to-[#0f0a18]', glowClass: 'bg-purple-500/20', iconColor: '#a855f7', barColor: 'linear-gradient(90deg, #a855f7, #ec4899)' },
  podcast:    { bgClass: 'bg-gradient-to-br from-[#0a1a0f] to-[#0a180f]', glowClass: 'bg-green-500/20', iconColor: '#22c55e', barColor: 'linear-gradient(90deg, #22c55e, #06b6d4)' },
  novel:      { bgClass: 'bg-gradient-to-br from-[#0a151a] to-[#0a1218]', glowClass: 'bg-teal-500/20', iconColor: '#14b8a6', barColor: 'linear-gradient(90deg, #14b8a6, #06b6d4)' },
  music:      { bgClass: 'bg-gradient-to-br from-[#1a1000] to-[#180e00]', glowClass: 'bg-orange-500/20', iconColor: '#f97316', barColor: 'linear-gradient(90deg, #f97316, #fbbf24)' },
}

const quotaItems = reactive({})

const loadQuota = async () => {
  if (!authStore.isLoggedIn) return
  loadingQuota.value = true
  try {
    const data = await authApi.getQuotaUsage()
    // 合并 API 数据和本地样式
    for (const [key, apiItem] of Object.entries(data)) {
      if (key === 'tier') {
        tierInfo.code = apiItem.code
        tierInfo.name = apiItem.name
        continue
      }
      const config = quotaConfig[key] || quotaConfig.ttsChars
      quotaItems[key] = { ...apiItem, ...config }
    }
  } catch (err) {
    console.error('加载配额失败', err)
    // fallback: 用本地 policy 数据
    const limits = authStore.policy?.quotaLimits
    if (limits) {
      quotaItems.ttsChars = { label: '基础合成', icon: 'volume-up', used: 0, limit: limits.tts_daily_chars || 0, unit: '字', ...quotaConfig.ttsChars }
      quotaItems.ttsV2Chars = { label: '情感合成', icon: 'theater-masks', used: 0, limit: limits.tts_v2_daily_chars || 0, unit: '字', ...quotaConfig.ttsV2Chars }
      quotaItems.aiScript = { label: 'AI 编排', icon: 'magic', used: 0, limit: limits.ai_script_daily_count || 0, unit: '次', ...quotaConfig.aiScript }
      quotaItems.podcast = { label: 'AI 双播', icon: 'podcast', used: 0, limit: limits.podcast_daily_count || 0, unit: '次', ...quotaConfig.podcast }
      quotaItems.novel = { label: '小说合成', icon: 'book-open', used: 0, limit: limits.novel_daily_chars || 0, unit: '字', ...quotaConfig.novel }
      quotaItems.music = { label: 'AI 音乐', icon: 'music', used: 0, limit: limits.music_daily_count || 0, unit: '次', ...quotaConfig.music }
    }
  } finally {
    loadingQuota.value = false
  }
}

onMounted(() => {
  loadQuota()
})

const showLogoutConfirm = ref(false)

const confirmLogout = () => {
  showLogoutConfirm.value = false
  authStore.clearToken()
  router.replace('/login')
}
</script>

<style scoped>
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
</style>
