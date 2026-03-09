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
              <!-- 头像区域 (带试听播放按钮) -->
              <div class="w-10 h-10 rounded-full bg-gradient-to-br from-indigo-500 to-purple-500 flex justify-center items-center shrink-0 shadow-lg relative group">
                <!-- 默认：用户图标 -->
                <i v-if="previewingVoiceId !== voice.voiceId && !previewLoading[voice.voiceId]"
                   class="fas fa-user-astronaut text-white/80 group-hover:hidden text-sm"></i>
                
                <!-- 可试听（有权限）：悬浮显示播放按钮 -->
                <i v-if="hasAccess(voice) && previewingVoiceId !== voice.voiceId && !previewLoading[voice.voiceId]"
                   class="fas fa-play text-white hidden group-hover:block text-xs ml-0.5"
                   @click.stop="previewVoice(voice)"></i>
                
                <!-- 无权限：悬浮显示锁 -->
                <i v-if="!hasAccess(voice) && previewingVoiceId !== voice.voiceId"
                   class="fas fa-lock text-white/80 hidden group-hover:block text-xs"
                   @click.stop="tryPurchase(voice)"></i>

                <!-- 试听加载中 -->
                <i v-if="previewLoading[voice.voiceId]"
                   class="fas fa-circle-notch fa-spin text-[#FF9500] text-sm"></i>

                <!-- 正在试听：显示暂停按钮（常驻，不需要 hover） -->
                <i v-if="previewingVoiceId === voice.voiceId && !previewLoading[voice.voiceId]"
                   class="fas fa-pause text-[#FF9500] text-sm"
                   @click.stop="stopPreview()"></i>

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

        <!-- 底部试听进度条（正在试听时显示） -->
        <transition name="fade">
          <div v-if="previewingVoiceId" class="shrink-0 px-5 py-3 border-t border-white/10 bg-[#1C1C1E]">
            <div class="flex items-center gap-3">
              <button @click="stopPreview" class="w-8 h-8 rounded-full bg-[#FF9500]/20 flex items-center justify-center cursor-pointer shrink-0">
                <i class="fas fa-stop text-[#FF9500] text-xs"></i>
              </button>
              <div class="flex-1 min-w-0">
                <p class="text-white text-xs font-bold truncate">🎧 试听: {{ previewingVoiceName }}</p>
                <div class="w-full h-1 bg-white/10 rounded-full mt-1.5 overflow-hidden">
                  <div class="h-full bg-gradient-to-r from-[#FF9500] to-amber-400 rounded-full transition-all duration-300"
                       :style="{ width: previewProgress + '%' }"></div>
                </div>
              </div>
            </div>
          </div>
        </transition>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, reactive, computed, watch, onBeforeUnmount } from 'vue'
import { voiceApi } from '../api/voice'
import { ttsApi } from '../api/tts'
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

// ── 试听相关状态 ──
const previewAudio = ref(null)
const previewingVoiceId = ref(null)
const previewingVoiceName = ref('')
const previewProgress = ref(0)
const previewLoading = reactive({})
let progressTimer = null

/** 试听示范文本（用于无 previewUrl 时实时合成） */
const PREVIEW_TEXT = '大家好，这是我的声音，希望你会喜欢。'

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
    } catch (e) {
        console.warn('[VoiceSelector] 加载音色库失败:', e)
        toastStore.show('音色库加载失败，请重试')
    } finally {
        loading.value = false
    }
}

watch(() => props.visible, (val) => {
    if (val) {
        selectedVoiceId.value = props.initialVoiceId
        fetchLibrary()
    } else {
        // 关闭弹窗时停止试听
        stopPreview()
    }
})

const filteredVoices = computed(() => {
    if (activeCategory.value === '全部') { return voices.value }
    return voices.value.filter(v => v.category === activeCategory.value)
})

/**
 * 试听音色
 * 优先使用 previewUrl（预录制音频），无 previewUrl 时调用 TTS 实时合成
 */
const previewVoice = async (voice) => {
    // 如果正在试听同一个，切换暂停
    if (previewingVoiceId.value === voice.voiceId) {
        stopPreview()
        return
    }

    // 停止之前正在播放的
    stopPreview()

    // 有 previewUrl 直接播放
    if (voice.previewUrl) {
        playPreviewAudio(voice.voiceId, voice.name, voice.previewUrl)
        return
    }

    // 无 previewUrl → 调用 TTS 实时合成
    previewLoading[voice.voiceId] = true
    try {
        const res = await ttsApi.synthesizeShort({
            text: PREVIEW_TEXT,
            voiceId: voice.voiceId
        })
        if (res && res.audioUrl) {
            playPreviewAudio(voice.voiceId, voice.name, res.audioUrl)
        } else {
            toastStore.show('试听合成失败，请重试')
        }
    } catch (e) {
        console.warn('[VoiceSelector] 试听合成失败:', e)
        toastStore.show('试听合成失败: ' + (e.message || '未知错误'))
    } finally {
        previewLoading[voice.voiceId] = false
    }
}

/**
 * 播放试听音频
 */
const playPreviewAudio = (voiceId, name, url) => {
    previewAudio.value = new Audio(url)
    previewingVoiceId.value = voiceId
    previewingVoiceName.value = name
    previewProgress.value = 0

    previewAudio.value.addEventListener('ended', () => {
        stopPreview()
    })

    previewAudio.value.addEventListener('error', () => {
        toastStore.show('试听播放失败')
        stopPreview()
    })

    previewAudio.value.play().catch(() => {
        toastStore.show('播放失败，请重试')
        stopPreview()
    })

    // 更新进度条
    progressTimer = setInterval(() => {
        if (previewAudio.value && previewAudio.value.duration > 0) {
            previewProgress.value = Math.min(100,
                (previewAudio.value.currentTime / previewAudio.value.duration) * 100)
        }
    }, 100)
}

/**
 * 停止试听
 */
const stopPreview = () => {
    if (previewAudio.value) {
        previewAudio.value.pause()
        previewAudio.value.src = ''
        previewAudio.value = null
    }
    previewingVoiceId.value = null
    previewingVoiceName.value = ''
    previewProgress.value = 0
    if (progressTimer) {
        clearInterval(progressTimer)
        progressTimer = null
    }
}

// 鉴权判断函数（对应后端校验）
const hasAccess = (voice) => {
    if (voice.price == 0) { return true }
    if (ownedVoiceIds.value.includes(voice.voiceId)) { return true }
    if (voice.isVipFree === 1 && authStore.isVip) { return true }
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
    stopPreview()
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
        } catch (e) {
            toastStore.show('交易打桩失败：' + (e.response?.data?.message || e.message))
        }
    }
}

// 组件卸载时清理
onBeforeUnmount(() => {
    stopPreview()
})
</script>

<style scoped>
.slide-up-enter-active, .slide-up-leave-active { transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1); }
.slide-up-enter-from, .slide-up-leave-to { transform: translateY(100%); opacity: 0; }
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>

