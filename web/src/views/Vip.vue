<template>
  <div class="h-full bg-[#050505] flex flex-col relative hide-scrollbar">
    
    <div class="h-14 flex justify-between items-center px-4 mt-8 z-50">
      <button @click="$router.back()" class="text-white w-8 h-8 flex justify-center items-center cursor-pointer hover:bg-white/10 rounded-full transition-colors"><i class="fas fa-chevron-left"></i></button>
      <h1 class="text-white font-bold text-base">VIP 会员</h1>
      <div class="w-8"></div>
    </div>

    <div class="flex-1 overflow-y-auto px-5 pt-2 pb-32 hide-scrollbar">
      
      <!-- VIP 动态背景卡片 -->
      <div class="vip-hero rounded-3xl p-6 mb-6 flex flex-col items-center justify-center relative overflow-hidden">
        <div class="absolute top-0 right-0 w-32 h-32 bg-[#FFD60A]/10 rounded-full blur-3xl -mr-10 -mt-10"></div>
        <div class="absolute bottom-0 left-0 w-32 h-32 bg-purple-500/20 rounded-full blur-3xl -ml-10 -mb-10"></div>
        
        <i class="fas fa-crown text-5xl bg-clip-text text-transparent bg-gradient-to-r from-[#FF9500] to-[#FFD60A] mb-3 drop-shadow-lg scale-110"></i>
        <h2 class="text-2xl font-bold text-white mb-2 tracking-widest">VIP 会员</h2>
        <div class="bg-white/10 px-4 py-1.5 rounded-full text-xs text-gray-300 font-medium border border-white/5 shadow-inner">
          当前状态：{{ authStore.isVip ? '尊贵 VIP' : '普通用户' }}
        </div>
      </div>

      <h3 class="text-white font-bold mb-3 text-sm">套餐选择</h3>
      <div class="flex flex-col gap-3 mb-8">
        
        <div v-if="plans.length === 0" class="text-center text-gray-500 text-sm py-4">正在加载商品计划...</div>
        
        <template v-else>
        <!-- 推荐套餐 (长卡片) -->
        <div 
          v-if="recommendedPlan"
          @click="selectedPlan = recommendedPlan.id" 
          class="glass-panel rounded-2xl p-4 flex justify-between items-center cursor-pointer transition-all relative overflow-hidden group mb-1" 
          :class="selectedPlan === recommendedPlan.id ? 'plan-selected' : 'border-white/10 hover:border-white/20'"
        >
          <div v-if="recommendedPlan.originalPrice > recommendedPlan.price" class="absolute top-0 right-0 bg-gradient-to-r from-red-500 to-orange-500 text-white text-[10px] font-bold px-2 py-0.5 rounded-bl-lg shadow-md">🔥 超值特惠</div>
          
          <div>
            <h4 class="text-white font-bold text-base mb-1">{{ recommendedPlan.name }}</h4>
            <p class="text-xs transition-colors" :class="selectedPlan === recommendedPlan.id ? 'text-[#FFD60A]' : 'text-gray-500'">{{ formatDuration(recommendedPlan.durationDays) }}</p>
          </div>
          <div class="text-right flex flex-col items-end">
            <span v-if="recommendedPlan.originalPrice" class="text-[10px] text-gray-500 line-through">¥{{ recommendedPlan.originalPrice }}</span>
            <div class="flex items-baseline gap-1">
              <span class="text-sm font-bold text-[#FFD60A]">¥</span>
              <span class="text-3xl font-bold text-[#FFD60A]">{{ recommendedPlan.price }}</span>
            </div>
          </div>
        </div>

        <!-- 其余套餐 (小卡片并排) -->
        <div class="flex gap-3">
          <div 
            v-for="plan in otherPlans" 
            :key="plan.id"
            @click="selectedPlan = plan.id" 
            class="flex-1 glass-panel rounded-2xl p-4 flex flex-col justify-between cursor-pointer transition-all group relative overflow-hidden" 
            :class="selectedPlan === plan.id ? 'plan-selected' : 'border-white/10 hover:border-white/20'"
          >
            <div class="mb-2">
              <h4 class="text-white font-bold text-sm mb-0.5">{{ plan.name }}</h4>
              <p class="text-[10px] transition-colors" :class="selectedPlan === plan.id ? 'text-[#FFD60A]' : 'text-gray-500'">{{ formatDuration(plan.durationDays) }}</p>
            </div>
            <div class="flex items-baseline gap-0.5">
              <span class="text-xs font-bold text-white">¥</span>
              <span class="text-xl font-bold text-white">{{ plan.price }}</span>
            </div>
          </div>
        </div>
        </template>
      </div>

      <h3 class="text-white font-bold mb-3 text-sm">会员特权</h3>
      <div class="glass-panel rounded-2xl p-5 mb-8">
        <ul class="grid grid-cols-2 gap-y-4 gap-x-2">
          <li class="flex items-center gap-2 text-sm text-gray-200"><i class="fas fa-check text-green-500 text-xs"></i> 每日 2000 字配音</li>
          <li class="flex items-center gap-2 text-sm text-gray-200"><i class="fas fa-check text-green-500 text-xs"></i> 智能情感语音合成</li>
          <li class="flex items-center gap-2 text-sm text-gray-200"><i class="fas fa-check text-green-500 text-xs"></i> AI 双人访谈播客</li>
          <li class="flex items-center gap-2 text-sm text-gray-200"><i class="fas fa-check text-green-500 text-xs"></i> AI 音乐一键生成</li>
          <li class="flex items-center gap-2 text-sm text-gray-200"><i class="fas fa-check text-green-500 text-xs"></i> AI 工作台全功能解锁</li>
          <li class="flex items-center gap-2 text-sm text-gray-200"><i class="fas fa-check text-green-500 text-xs"></i> 有声绘本·广播剧创作</li>
          <li class="flex items-center gap-2 text-sm text-gray-200"><i class="fas fa-check text-green-500 text-xs"></i> 边听边问 30 次/天</li>
          <li class="flex items-center gap-2 text-sm text-gray-200"><i class="fas fa-check text-green-500 text-xs"></i> 优先客服支持</li>
        </ul>
      </div>

      <h3 class="text-white font-bold mb-3 text-sm">套餐对比</h3>
      <div class="glass-panel rounded-2xl overflow-hidden mb-6">
        <div class="flex bg-black/40 border-b border-white/10 p-3">
          <div class="flex-1 text-xs text-[#FFD60A] font-bold text-center">功能</div>
          <div class="flex-1 text-xs text-gray-400 font-bold text-center">免费版</div>
          <div class="flex-1 text-xs text-[#FFD60A] font-bold text-center">VIP版</div>
        </div>
        <div class="flex border-b border-white/5 p-3 hover:bg-white/5 transition-colors">
          <div class="flex-1 text-xs text-gray-300 text-center">每日配音字数</div>
          <div class="flex-1 text-xs text-gray-500 text-center">100字</div>
          <div class="flex-1 text-xs text-[#FF9500] font-bold text-center">2000字</div>
        </div>
        <div class="flex border-b border-white/5 p-3 hover:bg-white/5 transition-colors">
          <div class="flex-1 text-xs text-gray-300 text-center">边听边问</div>
          <div class="flex-1 text-xs text-gray-500 text-center">3次/天</div>
          <div class="flex-1 text-xs text-[#FF9500] font-bold text-center">30次/天</div>
        </div>
        <div class="flex border-b border-white/5 p-3 hover:bg-white/5 transition-colors">
          <div class="flex-1 text-xs text-gray-300 text-center">情感语音合成</div>
          <div class="flex-1 text-xs text-gray-500 text-center">—</div>
          <div class="flex-1 text-xs text-green-500 font-bold text-center">2000字/天</div>
        </div>
        <div class="flex border-b border-white/5 p-3 hover:bg-white/5 transition-colors">
          <div class="flex-1 text-xs text-gray-300 text-center">AI 工作台</div>
          <div class="flex-1 text-xs text-gray-500 text-center">—</div>
          <div class="flex-1 text-xs text-green-500 font-bold text-center">50次/天</div>
        </div>
        <div class="flex p-3 hover:bg-white/5 transition-colors">
          <div class="flex-1 text-xs text-gray-300 text-center">AI 音乐生成</div>
          <div class="flex-1 text-xs text-gray-500 text-center">—</div>
          <div class="flex-1 text-xs text-green-500 font-bold text-center">10次/天</div>
        </div>
      </div>

    </div>

    <!-- 底部购买栏 -->
    <div class="absolute bottom-0 w-full p-5 bg-gradient-to-t from-[#050505] via-[#050505]/95 to-transparent z-40 pb-8 pointer-events-none">
      <button 
        @click="handlePay"
        :disabled="!selectedPlan || isPaying"
        class="w-full py-4 rounded-xl bg-gradient-to-r from-[#FF9500] to-[#FFD60A] text-black font-bold text-lg shadow-[0_4px_20px_rgba(255,149,0,0.3)] active:scale-95 transition-transform translate-y-0 pointer-events-auto cursor-pointer flex justify-center items-center gap-2 disabled:opacity-50"
      >
        <i v-if="isPaying" class="fas fa-spinner fa-spin"></i>
        立即开通专属会员
      </button>
      <p class="text-center text-[10px] text-gray-500 mt-3 pointer-events-auto">开通即代表同意《声读VIP服务协议》</p>
    </div>

  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '../stores/auth'
import { useRouter } from 'vue-router'
import { vipApi } from '../api/vip'
import { useToastStore } from '../stores/toast'

const authStore = useAuthStore()
const toastStore = useToastStore()
const router = useRouter()

const plans = ref([])
const selectedPlan = ref(null)
const isPaying = ref(false)

// 推荐套餐：优先取有 originalPrice 的（促销款），否则取最贵的
const recommendedPlan = computed(() => {
  if (plans.value.length === 0) return null
  const promo = plans.value.find(p => p.originalPrice && p.originalPrice > p.price)
  if (promo) return promo
  return [...plans.value].sort((a, b) => b.price - a.price)[0]
})

// 其余套餐
const otherPlans = computed(() => {
  if (!recommendedPlan.value) return plans.value
  return plans.value.filter(p => p.id !== recommendedPlan.value.id)
})

// 格式化天数显示
const formatDuration = (days) => {
  if (!days && days !== 0) return '—'
  if (days > 9000) return '永久有效'
  if (days >= 365) return Math.floor(days / 365) + ' 年'
  if (days >= 30) return Math.floor(days / 30) + ' 个月'
  return days + ' 天'
}

const loadPlans = async () => {
    try {
        // 假设后台返回的列表中商品 id 分别是 vip_month, vip_year, vip_lifetime
        const res = await vipApi.getPlans()
        if (res && res.length > 0) {
            plans.value = res
            selectedPlan.value = plans.value[0].id // 默认选中第一项
        }
    } catch(e) {
        // Mock fallback if api not ready
        plans.value = [
            { id: 'vip_month', name: '月度体验', durationDays: 30, price: 30 },
            { id: 'vip_lifetime', name: '终身 VIP', durationDays: 9999, price: 1000 },
            { id: 'vip_year', name: '年度 VIP', durationDays: 365, price: 300, originalPrice: 360 }
        ]
        selectedPlan.value = 'vip_year'
    }
}

const handlePay = async () => {
  if (!authStore.isLoggedIn) {
    toastStore.show('请先登录及完成实名认证')
    router.replace('/login')
    return
  }
  
  if (!selectedPlan.value) return toastStore.show('请选择一个会员套餐')

  isPaying.value = true
  try {
      const order = await vipApi.createOrder(selectedPlan.value)
      // TODO: 对接真实支付（微信/支付宝），当前为模拟支付流程
      toastStore.show(`支付成功！订单 ${order.orderNo || '待分配'} 已生效，正在加载您的全新特权...`)
      
      // 更新 authStore user 状态并在刷新后触发重新请求 profile 和 policy
      await authStore.fetchUserInfo()
      router.back()
  } catch(e) {
      toastStore.show('模拟支付发起失败，请稍后重试: ' + e.message)
      // 若是测试环境也可以兜底成功
      authStore.user.vip = true
  } finally {
      isPaying.value = false
  }
}

onMounted(() => {
    loadPlans()
})
</script>

<style scoped>
.vip-hero { 
  background: linear-gradient(135deg, rgba(45, 20, 65, 0.8) 0%, rgba(20, 15, 30, 0.9) 100%); 
  border: 1px solid rgba(212, 175, 55, 0.2); 
}

/* 选中套餐的流光发光边框 */
.plan-selected { 
  border-color: #FFD60A; 
  background: rgba(255, 214, 10, 0.1); 
  box-shadow: 0 0 20px rgba(255, 214, 10, 0.15); 
}
</style>
