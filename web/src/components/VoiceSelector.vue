<template>
  <transition name="slide-up">
    <div v-if="visible" class="fixed inset-0 z-[120] flex flex-col justify-end">
      <!-- 背景遮罩 -->
      <div class="absolute inset-0 bg-black/60 backdrop-blur-sm" @click="close"></div>
      
      <!-- 弹窗主体 -->
      <div class="bg-[#1C1C1E] w-full h-[75vh] rounded-t-3xl relative z-10 flex flex-col shadow-[-0_-10px_40px_rgba(0,0,0,0.5)]">
        <!-- 弹窗头 -->
        <div class="flex justify-between items-center p-5 border-b border-white/10 shrink-0">
          <h2 class="text-white font-bold text-lg">选择发音人</h2>
          <button @click="close" class="w-8 h-8 rounded-full bg-white/10 flex justify-center items-center text-gray-400 hover:text-white transition-colors cursor-pointer">
            <i class="fas fa-times"></i>
          </button>
        </div>

        <!-- 分类切换栏 -->
        <div class="flex gap-2 overflow-x-auto px-5 py-3 shrink-0 hide-scrollbar border-b border-white/5">
          <button 
            v-for="cat in categories" 
            :key="cat"
            @click="activeCategory = cat"
            class="px-4 py-1.5 rounded-full text-xs font-bold whitespace-nowrap transition-colors cursor-pointer"
            :class="activeCategory === cat ? 'bg-white text-black' : 'bg-white/10 text-gray-400 hover:text-white'"
          >
            {{ cat }}
          </button>
        </div>

        <!-- 音色列表 -->
        <div class="flex-1 overflow-y-auto p-5 relative">
          <div v-if="loading" class="absolute inset-0 flex justify-center items-center text-[#FF9500]">
             <i class="fas fa-circle-notch fa-spin text-3xl"></i>
          </div>
          <div v-else-if="filteredVoices.length === 0" class="text-center text-gray-500 mt-10 text-sm">
             暂无该分类下的可用音色
          </div>
          <div v-else class="grid grid-cols-2 gap-3 pb-8">
            <div 
              v-for="voice in filteredVoices" 
              :key="voice.voiceId"
              @click="handleSelect(voice)"
              class="flex items-center p-3 rounded-xl border transition-all cursor-pointer relative"
              :class="selectedVoiceId === voice.voiceId ? 'bg-[#FF9500]/10 border-[#FF9500]' : 'bg-[#2C2C2E] border-transparent hover:border-white/10'"
            >
              <!-- 头像区域 (带悬浮播放图标) -->
              <div class="w-10 h-10 rounded-full bg-gradient-to-br from-indigo-500 to-purple-500 flex justify-center items-center shrink-0 shadow-lg relative group">
                <i class="fas fa-user-astronaut text-white/80 group-hover:hidden text-sm"></i>
                <i v-if="hasAccess(voice)" class="fas fa-play text-white hidden group-hover:block text-xs ml-0.5" @click.stop="previewVoice(voice)"></i>
                <i v-else class="fas fa-lock text-white/80 hidden group-hover:block text-xs" @click.stop="tryPurchase(voice)"></i>

                <!-- 选中角标 -->
                <div v-if="selectedVoiceId === voice.voiceId" class="absolute -bottom-1 -right-1 bg-[#FF9500] w-4 h-4 rounded-full flex justify-center items-center border-[1.5px] border-[#2C2C2E]">
                   <i class="fas fa-check text-white text-[8px]"></i>
                </div>
              </div>

              <!-- 信息区 -->
              <div class="ml-3 flex-1 overflow-hidden">
                <div class="flex items-center gap-1.5">
                   <h3 class="text-white font-bold text-[13px] truncate">{{ voice.name }}</h3>
                   <span v-if="voice.isVipFree === 1" class="shrink-0 text-[8px] bg-gradient-to-r from-amber-200 to-yellow-500 text-black px-1 rounded font-bold">VIP</span>
                </div>
                <!-- 显示第一个 tag 作为副标题 -->
                <p class="text-[10px] text-gray-400 mt-0.5 truncate">{{ voice.tags ? voice.tags.split('，')[0].split(',')[0] : '默认音色' }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { voiceApi } from '../api/voice'
import { useAuthStore } from '../stores/auth'
import { useRouter } from 'vue-router'
import { useToastStore } from '../stores/toast'

const props = defineProps({
  visible: { type: Boolean, default: false },
  engine: { type: String, default: 'tts-1.0' }, // tts-1.0, tts-2.0, podcast
  initialVoiceId: { type: String, default: 'BV001_streaming' }
})
const emit = defineEmits(['update:visible', 'select'])

const authStore = useAuthStore()
const toastStore = useToastStore()
const router = useRouter()

const loading = ref(false)
const voices = ref([])
const ownedVoiceIds = ref([])
const categories = ref([])
const activeCategory = ref('全部')
const selectedVoiceId = ref(props.initialVoiceId)

const fetchLibrary = async () => {
    loading.value = true
    try {
        const res = await voiceApi.getLibrary(props.engine)
        if (res && res.list) {
            voices.value = res.list
            ownedVoiceIds.value = res.owned || []
            
            // 提取所有唯一种类并加入'全部'
            const catSet = new Set(voices.value.map(v => v.category).filter(Boolean))
            categories.value = ['全部', ...Array.from(catSet)]
            if (!activeCategory.value || !categories.value.includes(activeCategory.value)) {
                activeCategory.value = '全部'
            }
        }
    } catch(e) {
        console.error("加载音色库失败:", e)
    } finally {
        loading.value = false
    }
}

watch(() => props.visible, (val) => {
    if (val) {
        selectedVoiceId.value = props.initialVoiceId
        fetchLibrary()
    }
})

const filteredVoices = computed(() => {
    if (activeCategory.value === '全部') return voices.value
    return voices.value.filter(v => v.category === activeCategory.value)
})

const previewVoice = (voice) => {
    // 实际项目中这里可以播放 previewUrl
    console.log("播放试听:", voice.name)
}

// 鉴权判断函数（对应后端校验）
const hasAccess = (voice) => {
    if (voice.price == 0) return true
    if (ownedVoiceIds.value.includes(voice.voiceId)) return true
    if (voice.isVipFree === 1 && authStore.isVip) return true
    return false
}

const handleSelect = (voice) => {
    if (!hasAccess(voice)) {
        tryPurchase(voice)
        return
    }
    selectedVoiceId.value = voice.voiceId
    
    // 延迟一点关闭让用户看到选中动效
    setTimeout(() => {
        emit('select', voice)
        close()
    }, 200)
}

const close = () => {
    emit('update:visible', false)
}

const tryPurchase = async (voice) => {
    if (!authStore.isLoggedIn) {
        toastStore.show("长官，请先登录才可解锁高级音色库！")
        return router.replace('/login')
    }
    const confirmBuy = confirm(`确定要花费 ￥${voice.price} 解锁【${voice.name}】吗？(当前支付模式为内置直接抵扣测试)`)
    if (confirmBuy) {
        try {
            const res = await voiceApi.purchase(voice.voiceId, 'wechat')
            if (res.data) {
                toastStore.show('购买成功！此音色现已对您永久开放。')
                // 刷新拥有的资产列表
                fetchLibrary()
            }
        } catch(e) {
            toastStore.show('交易打桩失败：' + (e.response?.data?.message || e.message))
        }
    }
}
</script>

<style scoped>
.slide-up-enter-active, .slide-up-leave-active { transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1); }
.slide-up-enter-from, .slide-up-leave-to { transform: translateY(100%); opacity: 0; }
</style>
