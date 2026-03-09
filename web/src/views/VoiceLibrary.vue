<template>
  <div class="min-h-screen bg-[#0a0a0a] text-white pb-28">
    <!-- 顶部 Header -->
    <div class="sticky top-0 z-30 bg-[#0a0a0a]/95 backdrop-blur-xl border-b border-white/5">
      <div class="max-w-5xl mx-auto px-4 pt-4 pb-3">
        <div class="flex justify-between items-center mb-3">
          <div class="flex items-center gap-3">
            <button @click="$router.back()" class="w-8 h-8 rounded-full bg-white/5 flex items-center justify-center text-gray-400 hover:text-white hover:bg-white/10 transition-all cursor-pointer shrink-0">
              <i class="fas fa-arrow-left text-xs"></i>
            </button>
            <div>
              <h1 class="text-lg font-bold flex items-center gap-2">
                <i class="fas fa-microphone-lines text-[#FF9500]"></i> 音色库
              </h1>
              <p class="text-[10px] text-gray-500 mt-0.5">探索 AI 声音，找到你的专属音色</p>
            </div>
          </div>
          <div class="flex items-center gap-1.5">
            <button v-for="eng in engines" :key="eng.value"
                    @click="switchEngine(eng.value)"
                    class="px-3 py-1.5 rounded-lg text-[10px] font-bold transition-all cursor-pointer"
                    :class="activeEngine === eng.value
                      ? 'bg-[#FF9500] text-black shadow-lg shadow-orange-500/20'
                      : 'bg-white/5 text-gray-500 hover:text-white hover:bg-white/10'">
              {{ eng.label }}
            </button>
          </div>
        </div>

        <!-- 分类横向滚动 -->
        <div class="flex gap-2 overflow-x-auto hide-scrollbar pb-1">
          <button v-for="cat in categories" :key="cat"
                  @click="activeCategory = cat"
                  class="px-3 py-1.5 rounded-full text-[11px] font-bold whitespace-nowrap transition-all cursor-pointer shrink-0"
                  :class="activeCategory === cat
                    ? 'bg-white text-black shadow-md'
                    : 'bg-white/5 text-gray-500 hover:text-white hover:bg-white/10'">
            {{ cat }}
          </button>
        </div>
      </div>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="flex justify-center items-center py-20">
      <i class="fas fa-circle-notch fa-spin text-[#FF9500] text-3xl"></i>
    </div>

    <!-- 空状态 -->
    <div v-else-if="filteredVoices.length === 0" class="text-center py-20">
      <div class="w-16 h-16 mx-auto rounded-full bg-white/5 flex items-center justify-center mb-4">
        <i class="fas fa-microphone-slash text-2xl text-gray-600"></i>
      </div>
      <p class="text-gray-500 text-sm">暂无该分类下的可用音色</p>
    </div>

    <!-- 音色列表 -->
    <div v-else class="max-w-5xl mx-auto px-4 pt-3 pb-4">
      <!-- 统计 -->
      <p class="text-[10px] text-gray-600 mb-3 pl-1">
        共 {{ filteredVoices.length }} 个音色
        <span v-if="activeCategory !== '全部'"> · {{ activeCategory }}</span>
      </p>

      <div class="space-y-2.5">
        <div v-for="voice in filteredVoices" :key="voice.voiceId"
             class="bg-[#141416] border rounded-2xl px-4 py-3.5 transition-all group"
             :class="previewingVoiceId === voice.voiceId
               ? 'border-[#FF9500]/30 bg-[#FF9500]/[0.03] shadow-lg shadow-orange-500/5'
               : 'border-white/5 hover:border-white/15'">

          <div class="flex items-center gap-3.5">
            <!-- 头像 + 播放叠加 -->
            <div class="relative shrink-0 cursor-pointer" @click="previewVoice(voice)">
              <div class="w-14 h-14 rounded-2xl bg-gradient-to-br flex items-center justify-center shadow-lg transition-transform active:scale-95"
                   :class="getAvatarGradient(voice)">
                <!-- 正在播放这个音色 → 显示均衡器动画 -->
                <div v-if="previewingVoiceId === voice.voiceId" class="flex items-end gap-[3px] h-5">
                  <span class="w-[3px] bg-white rounded-full animate-eq1"></span>
                  <span class="w-[3px] bg-white rounded-full animate-eq2"></span>
                  <span class="w-[3px] bg-white rounded-full animate-eq3"></span>
                  <span class="w-[3px] bg-white rounded-full animate-eq4"></span>
                </div>
                <!-- 加载中 → 转圈 -->
                <i v-else-if="previewLoading[voice.voiceId]" class="fas fa-circle-notch fa-spin text-white text-lg"></i>
                <!-- 默认 → 播放图标 -->
                <i v-else class="fas fa-play text-white/90 text-sm ml-0.5"></i>
              </div>
              <!-- VIP 角标 -->
              <div v-if="voice.isVipFree === 1"
                   class="absolute -top-1.5 -right-1.5 bg-gradient-to-r from-amber-400 to-yellow-500 text-black text-[7px] font-black px-1.5 py-0.5 rounded-full shadow">
                VIP
              </div>
            </div>

            <!-- 信息区 -->
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 mb-1">
                <h3 class="text-sm font-bold text-white truncate">{{ voice.name }}</h3>
                <span v-if="voice.price > 0 && !hasAccess(voice)"
                      class="text-[9px] px-1.5 py-0.5 rounded-full bg-amber-500/10 text-amber-400 font-bold shrink-0">
                  ¥{{ voice.price }}
                </span>
              </div>
              <p class="text-[11px] text-gray-500 leading-relaxed line-clamp-1">
                {{ voice.description || voice.tags || '通用音色，自然流畅' }}
              </p>
              <!-- 标签 -->
              <div class="flex flex-wrap gap-1.5 mt-1.5">
                <span v-for="(tag, i) in getVoiceTags(voice)" :key="i"
                      class="text-[9px] px-2 py-0.5 rounded-full bg-white/[0.04] text-gray-500 border border-white/5">
                  {{ tag }}
                </span>
              </div>
            </div>

            <!-- 右侧使用按钮 -->
            <button @click="useVoice(voice)"
                    class="shrink-0 px-3.5 py-2 rounded-xl text-[11px] font-bold bg-gradient-to-r from-[#FF9500] to-[#FF6B00] text-black cursor-pointer hover:shadow-lg hover:shadow-orange-500/20 active:scale-95 transition-all">
              使用
            </button>
          </div>

          <!-- 播放进度条 -->
          <div v-if="previewingVoiceId === voice.voiceId" class="mt-3 -mb-0.5">
            <div class="w-full h-[3px] bg-white/10 rounded-full overflow-hidden">
              <div class="h-full bg-gradient-to-r from-[#FF9500] to-amber-400 rounded-full transition-all duration-150"
                   :style="{ width: previewProgress + '%' }"></div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部试听浮条 -->
    <transition name="slide-up-bar">
      <div v-if="previewingVoiceId"
           class="fixed bottom-[88px] left-0 right-0 z-[35] px-4 pb-2">
        <div class="max-w-5xl mx-auto bg-[#1C1C1E]/95 backdrop-blur-xl border border-white/10 rounded-2xl px-4 py-2.5 shadow-2xl">
          <div class="flex items-center gap-3">
            <button @click="stopPreview"
                    class="w-8 h-8 rounded-full bg-[#FF9500]/20 flex items-center justify-center cursor-pointer shrink-0 hover:bg-[#FF9500]/30 active:scale-90 transition-all">
              <i class="fas fa-stop text-[#FF9500] text-[10px]"></i>
            </button>
            <div class="flex-1 min-w-0">
              <p class="text-white text-xs font-bold truncate">🎧 {{ previewingVoiceName }}</p>
              <div class="w-full h-1 bg-white/10 rounded-full mt-1 overflow-hidden">
                <div class="h-full bg-gradient-to-r from-[#FF9500] to-amber-400 rounded-full transition-all duration-150"
                     :style="{ width: previewProgress + '%' }"></div>
              </div>
            </div>
            <span class="text-[10px] text-gray-500 shrink-0 tabular-nums">
              {{ formatTime(previewCurrentTime) }}/{{ formatTime(previewDuration) }}
            </span>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { voiceApi } from '../api/voice'
import { ttsApi } from '../api/tts'
import { useAuthStore } from '../stores/auth'
import { useToastStore } from '../stores/toast'

const router = useRouter()
const authStore = useAuthStore()
const toastStore = useToastStore()

// ── 数据状态 ──
const loading = ref(true)
const voices = ref([])
const ownedVoiceIds = ref([])
const categories = ref([])
const activeCategory = ref('全部')
const activeEngine = ref('tts-1.0')

const engines = [
  { value: 'tts-1.0', label: 'TTS 1.0' },
  { value: 'tts-2.0', label: 'TTS 2.0' }
]

// ── 试听状态 ──
const previewAudio = ref(null)
const previewingVoiceId = ref(null)
const previewingVoiceName = ref('')
const previewProgress = ref(0)
const previewCurrentTime = ref(0)
const previewDuration = ref(0)
const previewLoading = reactive({})
let progressTimer = null

const PREVIEW_TEXT = '大家好，这是我的声音，希望你会喜欢。欢迎来到声读 AI 语音创作平台。'

// ── 数据加载 ──
const fetchLibrary = async () => {
  loading.value = true
  try {
    const res = await voiceApi.getLibrary(activeEngine.value)
    if (res && res.list) {
      voices.value = res.list
      ownedVoiceIds.value = res.owned || []

      const catSet = new Set(voices.value.map(v => v.category).filter(Boolean))
      categories.value = ['全部', ...Array.from(catSet)]
      if (!categories.value.includes(activeCategory.value)) {
        activeCategory.value = '全部'
      }

      // 异步预加载没有 previewUrl 的音色试听
      setTimeout(() => preloadPreviews(), 500)
    }
  } catch (e) {
    console.warn('[VoiceLibrary] 加载失败:', e)
    toastStore.show('音色库加载失败，请重试')
  } finally {
    loading.value = false
  }
}

const switchEngine = (eng) => {
  if (activeEngine.value === eng) { return }
  stopPreview()
  activeEngine.value = eng
  fetchLibrary()
}

const filteredVoices = computed(() => {
  if (activeCategory.value === '全部') { return voices.value }
  return voices.value.filter(v => v.category === activeCategory.value)
})

// ── 音色工具 ──
const getVoiceTags = (voice) => {
  if (!voice.tags) { return [] }
  return voice.tags.split(/[，,]/).filter(Boolean).slice(0, 3)
}

const getAvatarGradient = (voice) => {
  const gradients = {
    'male': 'from-blue-500 to-indigo-600',
    'female': 'from-pink-500 to-purple-500'
  }
  return gradients[voice.gender] || 'from-indigo-500 to-purple-500'
}

const hasAccess = (voice) => {
  if (voice.price == 0) { return true }
  if (ownedVoiceIds.value.includes(voice.voiceId)) { return true }
  if (voice.isVipFree === 1 && authStore.isVip) { return true }
  return false
}

// ── 试听逻辑（缓存 + 异步预加载）──
const previewCache = reactive({}) // voiceId → audioUrl

const previewVoice = async (voice) => {
  if (previewingVoiceId.value === voice.voiceId) {
    stopPreview()
    return
  }

  stopPreview()

  // 优先级 1: previewUrl 直接播放
  if (voice.previewUrl) {
    playPreviewAudio(voice.voiceId, voice.name, voice.previewUrl)
    return
  }

  // 优先级 2: 已有缓存
  if (previewCache[voice.voiceId]) {
    playPreviewAudio(voice.voiceId, voice.name, previewCache[voice.voiceId])
    return
  }

  // TTS 2.0 音色无法免费试听（需要 WebSocket 合成）
  if (activeEngine.value === 'tts-2.0') {
    toastStore.show('TTS 2.0 音色试听需要在创作页体验 🎤')
    return
  }

  // 优先级 3: 调用免费试听 API
  previewLoading[voice.voiceId] = true
  try {
    const res = await ttsApi.preview({
      text: PREVIEW_TEXT,
      voiceId: voice.voiceId
    })
    if (res && res.audioUrl) {
      previewCache[voice.voiceId] = res.audioUrl
      playPreviewAudio(voice.voiceId, voice.name, res.audioUrl)
    } else {
      toastStore.show('试听合成失败，请重试')
    }
  } catch (e) {
    console.warn('[VoiceLibrary] 试听合成失败:', e)
    toastStore.show('试听失败: ' + (e.message || '请稍后重试'))
  } finally {
    previewLoading[voice.voiceId] = false
  }
}

/**
 * 后台异步预加载：页面加载后逐个合成无 previewUrl 的音色
 * 不阻塞 UI，用户可以随时点击试听
 */
const preloadPreviews = async () => {
  // TTS 2.0 音色无法用 TTS 1.0 接口合成，跳过
  if (activeEngine.value === 'tts-2.0') { return }

  const needPreload = voices.value.filter(v => !v.previewUrl && !previewCache[v.voiceId])
  for (const voice of needPreload) {
    if (!voices.value.length) { break }
    if (previewCache[voice.voiceId]) { continue }
    try {
      const res = await ttsApi.preview({
        text: PREVIEW_TEXT,
        voiceId: voice.voiceId
      })
      if (res && res.audioUrl) {
        previewCache[voice.voiceId] = res.audioUrl
      }
    } catch {
      // 静默失败
    }
  }
}

const playPreviewAudio = (voiceId, name, url) => {
  previewAudio.value = new Audio(url)
  previewingVoiceId.value = voiceId
  previewingVoiceName.value = name
  previewProgress.value = 0
  previewCurrentTime.value = 0
  previewDuration.value = 0

  previewAudio.value.addEventListener('ended', () => { stopPreview() })
  previewAudio.value.addEventListener('error', (e) => {
    // MEDIA_ERR_SRC_NOT_SUPPORTED(4) 或 MEDIA_ERR_NETWORK(2) 才是真正的错误
    const code = e.target?.error?.code
    // 如果音频已经开始播放（currentTime > 0），忽略误触发的 error 事件
    if (previewAudio.value && previewAudio.value.currentTime > 0) return
    if (code && code !== 1) { // code=1 是 MEDIA_ERR_ABORTED（用户主动停止），不提示
      toastStore.show('试听播放失败，请重试')
      stopPreview()
    }
  })

  previewAudio.value.play().catch((err) => {
    // NotAllowedError = 浏览器自动播放策略拦截，但音频通常仍会播放，忽略此错误
    // AbortError = 切换音色时主动 pause()，忽略
    if (err.name !== 'NotAllowedError' && err.name !== 'AbortError') {
      toastStore.show('试听播放失败，请重试')
      stopPreview()
    }
  })

  progressTimer = setInterval(() => {
    if (previewAudio.value) {
      const dur = previewAudio.value.duration
      const cur = previewAudio.value.currentTime
      if (dur > 0 && isFinite(dur)) {
        previewProgress.value = Math.min(100, (cur / dur) * 100)
        previewCurrentTime.value = cur
        previewDuration.value = dur
      }
    }
  }, 100)
}

const stopPreview = () => {
  if (previewAudio.value) {
    previewAudio.value.pause()
    previewAudio.value.src = ''
    previewAudio.value = null
  }
  previewingVoiceId.value = null
  previewingVoiceName.value = ''
  previewProgress.value = 0
  previewCurrentTime.value = 0
  previewDuration.value = 0
  if (progressTimer) {
    clearInterval(progressTimer)
    progressTimer = null
  }
}

const formatTime = (seconds) => {
  if (!seconds || !isFinite(seconds)) { return '0:00' }
  const m = Math.floor(seconds / 60)
  const s = Math.floor(seconds % 60)
  return `${m}:${s.toString().padStart(2, '0')}`
}

// ── 使用音色 → 跳转创作 ──
const useVoice = (voice) => {
  if (!authStore.isLoggedIn) {
    toastStore.show('请先登录后再使用音色 ✨')
    return router.push('/login')
  }
  const query = { voiceId: voice.voiceId, voiceName: voice.name }
  // TTS 1.0 → 文字配音页；TTS 2.0 → 情感调音台
  if (activeEngine.value === 'tts-2.0') {
    toastStore.show(`已选择「${voice.name}」，即将进入情感调音台 🎤`)
    router.push({ path: '/emotion', query })
  } else {
    toastStore.show(`已选择「${voice.name}」，即将进入文字配音 ✨`)
    router.push({ path: '/create', query })
  }
}

// ── 初始化 ──
fetchLibrary()

onBeforeUnmount(() => {
  stopPreview()
})
</script>

<style scoped>
.line-clamp-1 {
  display: -webkit-box;
  -webkit-line-clamp: 1;
  line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.slide-up-bar-enter-active, .slide-up-bar-leave-active { transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1); }
.slide-up-bar-enter-from, .slide-up-bar-leave-to { transform: translateY(100%); opacity: 0; }

/* 均衡器动画 */
@keyframes eq1 { 0%,100% { height: 4px; } 50% { height: 16px; } }
@keyframes eq2 { 0%,100% { height: 10px; } 50% { height: 6px; } }
@keyframes eq3 { 0%,100% { height: 6px; } 50% { height: 18px; } }
@keyframes eq4 { 0%,100% { height: 12px; } 50% { height: 4px; } }
.animate-eq1 { animation: eq1 0.6s ease-in-out infinite; }
.animate-eq2 { animation: eq2 0.5s ease-in-out infinite 0.1s; }
.animate-eq3 { animation: eq3 0.7s ease-in-out infinite 0.2s; }
.animate-eq4 { animation: eq4 0.4s ease-in-out infinite 0.15s; }
</style>
