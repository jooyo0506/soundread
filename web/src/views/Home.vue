<template>
  <div class="h-full overflow-y-auto px-5 pt-14 pb-24 hide-scrollbar">
    <!-- Header -->
    <div class="flex justify-between items-center mb-5">
      <div>
        <h1 class="text-2xl font-bold text-white tracking-wide">声读</h1>
        <p class="text-xs text-gray-500 mt-0.5">AI 语音创作平台</p>
      </div>
      <div class="w-10 h-10 rounded-full glass-panel flex justify-center items-center text-white cursor-pointer hover:bg-white/10 transition-colors">
        <i class="fas fa-bell"></i>
      </div>
    </div>

    <!-- ==================== Hero 区 — AI 声音制作人 ==================== -->
    <div class="rounded-2xl p-5 mb-5 bg-gradient-to-br from-cyan-900/30 via-[#0f1525] to-blue-900/30 border border-cyan-500/20 relative overflow-hidden">
      <!-- 装饰光效 -->
      <div class="absolute top-0 right-0 w-48 h-48 bg-gradient-to-bl from-cyan-500/10 to-transparent rounded-full -translate-y-16 translate-x-16"></div>
      <div class="absolute bottom-0 left-0 w-32 h-32 bg-gradient-to-tr from-blue-500/10 to-transparent rounded-full translate-y-12 -translate-x-12"></div>

      <div class="relative">
        <div class="flex items-center gap-3 mb-4">
          <div class="w-12 h-12 rounded-xl bg-gradient-to-br from-cyan-400 to-blue-600 flex justify-center items-center text-white text-xl shadow-[0_0_20px_rgba(34,211,238,0.4)]">
            🎙️
          </div>
          <div>
            <h2 class="text-base font-bold text-white">AI 声音制作人</h2>
            <p class="text-[10px] text-gray-400 mt-0.5">一句话完成有声内容制作</p>
          </div>
        </div>

        <!-- 点击跳转到 workshop -->
        <div @click="router.push('/workshop')"
             class="w-full px-4 py-3 rounded-xl bg-white/5 border border-white/10 cursor-pointer hover:border-cyan-500/30 hover:bg-white/8 transition-all flex items-center gap-2 mb-4 group">
          <i class="fas fa-search text-gray-600 text-xs group-hover:text-cyan-400 transition-colors"></i>
          <span class="text-sm text-gray-500 group-hover:text-gray-400 transition-colors">帮我录一段深夜电台独白...</span>
        </div>

        <!-- 快捷场景标签 -->
        <div class="flex gap-2 flex-wrap">
          <button v-for="(scene, idx) in quickScenes" :key="idx"
                  @click="goWorkshop(scene.text)"
                  class="px-3 py-1.5 rounded-full text-[11px] bg-white/5 border border-white/8 text-gray-400 cursor-pointer hover:border-cyan-500/30 hover:text-cyan-400 hover:bg-cyan-500/5 transition-all">
            {{ scene.emoji }} {{ scene.label }}
          </button>
        </div>

        <!-- 折叠式快速试听 -->
        <div class="mt-4 pt-3 border-t border-white/5">
          <div @click="showQuickSynth = !showQuickSynth"
               class="flex items-center justify-between cursor-pointer group">
            <span class="text-[11px] text-gray-500 group-hover:text-gray-400 transition-colors">
              <i class="fas fa-volume-up mr-1"></i> 或者，直接输入文字快速试听
            </span>
            <i class="fas text-gray-600 text-[10px] transition-transform"
               :class="showQuickSynth ? 'fa-chevron-up' : 'fa-chevron-down'"></i>
          </div>

          <!-- 展开的快速合成区 -->
          <transition name="slide">
            <div v-if="showQuickSynth" class="mt-3">
              <div class="flex gap-1.5 mb-2 overflow-x-auto hide-scrollbar">
                <button v-for="(preset, idx) in quickPresets" :key="idx"
                        @click="applyPreset(preset)"
                        class="shrink-0 px-2.5 py-1 rounded-full text-[10px] font-bold cursor-pointer transition-all border"
                        :class="quickText === preset.text
                          ? 'bg-[#FF9500]/15 border-[#FF9500]/50 text-[#FF9500]'
                          : 'bg-white/5 border-white/5 text-gray-400 hover:border-[#FF9500]/30 hover:text-[#FF9500]'">
                  {{ preset.emoji }} {{ preset.label }}
                </button>
              </div>
              <textarea v-model="quickText"
                        placeholder="输入任意文字，听听 AI 怎么读..."
                        class="w-full h-[60px] bg-white/5 rounded-xl px-3 py-2 text-sm text-white placeholder-gray-600 resize-none focus:outline-none focus:ring-1 focus:ring-cyan-500/50 border border-white/5"
                        maxlength="50"></textarea>
              <div class="flex items-center justify-between mt-2">
                <div @click="showVoiceSelector = true"
                     class="flex items-center gap-2 cursor-pointer group">
                  <div class="w-6 h-6 rounded-full bg-gradient-to-br from-purple-500 to-pink-500 flex justify-center items-center shadow-lg shrink-0">
                    <i class="fas fa-user-astronaut text-white text-[8px]"></i>
                  </div>
                  <span class="text-[10px] text-gray-500 group-hover:text-purple-400 transition-colors">
                    {{ quickVoice?.name || 'vivi 2.0' }} <i class="fas fa-exchange-alt ml-1"></i>
                  </span>
                </div>
                <button @click="quickSynthesize"
                        :disabled="!quickText.trim() || quickLoading"
                        class="px-4 py-1.5 rounded-lg text-[11px] font-bold cursor-pointer transition-all flex items-center gap-1.5"
                        :class="quickText.trim() && !quickLoading
                          ? 'bg-gradient-to-r from-[#FF9500] to-[#FFD60A] text-black hover:shadow-[0_0_15px_rgba(255,149,0,0.3)]'
                          : 'bg-white/5 text-gray-600 cursor-not-allowed'">
                  <i v-if="quickLoading" class="fas fa-circle-notch fa-spin"></i>
                  <i v-else class="fas fa-volume-up"></i>
                  {{ quickLoading ? '合成中' : '试听' }}
                </button>
              </div>
            </div>
          </transition>
        </div>
      </div>
    </div>

    <!-- ==================== 核心功能 ==================== -->
    <div class="mb-5">
      <h3 class="text-sm font-bold text-white mb-3 flex items-center gap-2">
        <i class="fas fa-star text-[#FF9500]"></i> 核心功能
      </h3>
      <div class="grid grid-cols-3 gap-3">

        <!-- 情感调音台 -->
        <div @click="router.push('/emotion')" class="glass-panel rounded-xl p-3 cursor-pointer hover:border-purple-400/40 transition-all group text-center relative">
          <div v-if="!authStore.hasFeature('tts_emotion_v2')" class="absolute top-1.5 right-1.5 text-[7px] bg-gradient-to-r from-amber-400 to-yellow-500 text-black px-1 py-0.5 rounded font-bold">VIP</div>
          <div class="w-10 h-10 mx-auto rounded-xl bg-gradient-to-br from-purple-500 to-pink-500 flex justify-center items-center text-white text-lg mb-2 group-hover:shadow-[0_0_15px_rgba(168,85,247,0.3)] transition-shadow">
            <i class="fas fa-masks-theater"></i>
          </div>
          <h4 class="text-white text-[11px] font-bold">情感调音台</h4>
          <p class="text-[9px] text-gray-500 mt-0.5">AI 情感合成</p>
        </div>

        <!-- AI 创作 -->
        <div @click="router.push('/studio')" class="glass-panel rounded-xl p-3 cursor-pointer hover:border-[#FF9500]/40 transition-all group text-center">
          <div class="w-10 h-10 mx-auto rounded-xl bg-gradient-to-br from-[#FF9500] to-[#FF6B00] flex justify-center items-center text-white text-lg mb-2 group-hover:shadow-[0_0_15px_rgba(255,149,0,0.3)] transition-shadow">
            <i class="fas fa-pen-fancy"></i>
          </div>
          <h4 class="text-white text-[11px] font-bold">AI 创作</h4>
          <p class="text-[9px] text-gray-500 mt-0.5">写作 + 配音</p>
        </div>

        <!-- AI 音乐 -->
        <div @click="router.push('/music')" class="glass-panel rounded-xl p-3 cursor-pointer hover:border-fuchsia-400/40 transition-all group text-center">
          <div class="w-10 h-10 mx-auto rounded-xl bg-gradient-to-br from-fuchsia-500 to-purple-600 flex justify-center items-center text-white text-lg mb-2 group-hover:shadow-[0_0_15px_rgba(192,80,240,0.3)] transition-shadow">
            <i class="fas fa-music"></i>
          </div>
          <h4 class="text-white text-[11px] font-bold">AI 音乐</h4>
          <p class="text-[9px] text-gray-500 mt-0.5">歌曲 & 纯音乐</p>
        </div>

      </div>
    </div>

    <!-- ==================== 推荐收听（真实数据） ==================== -->
    <div v-if="featuredWorks.length > 0" class="mb-6">
      <div class="flex justify-between items-center mb-3">
        <h3 class="text-sm font-bold text-white flex items-center gap-2">
          <i class="fas fa-fire text-orange-400"></i> 推荐收听
        </h3>
        <router-link to="/discover" class="text-[10px] text-gray-500 hover:text-[#FF9500] transition-colors">
          更多 <i class="fas fa-chevron-right text-[8px]"></i>
        </router-link>
      </div>

      <div class="flex gap-3 overflow-x-auto hide-scrollbar pb-2">
        <div v-for="work in featuredWorks" :key="work.id"
             @click="playWork(work)"
             class="flex-shrink-0 w-[150px] glass-panel rounded-xl overflow-hidden cursor-pointer hover:border-white/15 transition-all group">
          <!-- 封面区 -->
          <div class="w-full h-[80px] flex items-center justify-center relative overflow-hidden"
               :class="getWorkGradient(work.contentType)">
            <i class="fas text-white/30 text-2xl group-hover:text-white/60 transition-colors"
               :class="getWorkIcon(work.contentType)"></i>
            <!-- hover 播放按钮 -->
            <div class="absolute inset-0 bg-black/30 flex justify-center items-center opacity-0 group-hover:opacity-100 transition-opacity">
              <div class="w-8 h-8 rounded-full bg-[#FF9500] flex items-center justify-center text-black pl-0.5 shadow-lg">
                <i class="fas fa-play text-[10px]"></i>
              </div>
            </div>
            <!-- 时长标签 -->
            <span v-if="work.audioDuration" class="absolute bottom-1 right-1.5 text-[8px] text-white/70 bg-black/40 px-1 py-0.5 rounded backdrop-blur-sm">
              {{ formatDuration(work.audioDuration) }}
            </span>
          </div>
          <!-- 信息区 -->
          <div class="p-2.5">
            <h4 class="text-[11px] font-bold text-white truncate">{{ work.title }}</h4>
            <p class="text-[9px] text-gray-500 mt-0.5">{{ getContentLabel(work.contentType) }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 音色选择器弹窗 -->
    <VoiceSelector
      :visible="showVoiceSelector"
      @update:visible="showVoiceSelector = $event"
      engine="tts-2.0"
      :initialVoiceId="quickVoice?.voiceId || 'zh_female_vv_uranus_bigtts'"
      @select="onVoiceSelected"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useToastStore } from '../stores/toast'
import { usePlayerStore } from '../stores/player'
import { discoverApi } from '../api/discover'
import request from '../api/request'
import VoiceSelector from '../components/VoiceSelector.vue'

const router = useRouter()
const authStore = useAuthStore()
const toastStore = useToastStore()
const playerStore = usePlayerStore()

// ==================== Hero 区 ====================
const quickScenes = [
  { emoji: '🌙', label: '深夜电台', text: '帮我录一段深夜电台独白' },
  { emoji: '💌', label: '情感祝福', text: '帮我录一段生日祝福' },
  { emoji: '🎬', label: '短视频配音', text: '帮我做一段短视频旁白' },
  { emoji: '📖', label: '有声故事', text: '帮我创作一个睡前故事' },
]

const goWorkshop = (text) => {
  sessionStorage.setItem('workshop_init_text', text)
  router.push('/workshop')
}

// ==================== 快速试听（折叠区）====================
const showQuickSynth = ref(false)
const quickPresets = [
  { emoji: '😤', label: '愤怒吵架', text: '够了！你每次都这样！从来不听我说完！我受够了你的自以为是！' },
  { emoji: '💗', label: '暧昧心动', text: '嘘……别动。让我靠近一点……再近一点。听见了吗？你的心跳。' },
  { emoji: '🌙', label: '深夜治愈', text: '夜深了……如果你也睡不着，就听我说几句吧。一切都会好起来的。' },
  { emoji: '😱', label: '悬疑紧张', text: '门……开了。可是这间屋子，明明已经锁了三十年，谁打开的？' },
]
const quickText = ref('')
const quickLoading = ref(false)
const showVoiceSelector = ref(false)
const quickVoice = ref({ voiceId: 'zh_female_vv_uranus_bigtts', name: 'vivi 2.0' })

const applyPreset = (preset) => { quickText.value = preset.text }
const onVoiceSelected = (voice) => {
  quickVoice.value = voice
  toastStore.show(`已切换音色：${voice.name} 🎙️`)
}

const quickSynthesize = async () => {
  if (!quickText.value.trim()) return
  quickLoading.value = true
  try {
    if (!authStore.isLoggedIn) {
      toastStore.show('登录后即可试听，点击右下角「我的」注册 👤')
      quickLoading.value = false
      return
    }
    const voiceId = quickVoice.value?.voiceId || 'zh_female_vv_uranus_bigtts'
    const res = await request.post('/tts/v2/synthesize', {
      text: quickText.value.substring(0, 50),
      voiceType: voiceId,
      mode: 'default',
      userKey: authStore.user?.id?.toString() || 'anonymous'
    })
    if (res && res.audioUrl) {
      playerStore.play({ title: '快速试听', author: quickVoice.value?.name || 'vivi 2.0', url: res.audioUrl })
      toastStore.show('合成成功 🎧')
    } else {
      toastStore.show(res?.message || '合成失败，请稍后重试')
    }
  } catch (e) {
    toastStore.show(e.message || '合成失败，请稍后重试')
  } finally {
    quickLoading.value = false
  }
}

// ==================== 推荐收听（真实数据） ====================
const featuredWorks = ref([])

const loadFeaturedWorks = async () => {
  try {
    const res = await discoverApi.getWorks({ page: 1, size: 10, sort: 'hot' })
    if (res.records && res.records.length > 0) {
      // 质量过滤：只展示有音频且时长 >= 30 秒的作品
      featuredWorks.value = res.records
        .filter(w => w.audioUrl && w.audioDuration && w.audioDuration >= 30)
        .slice(0, 6)
    }
  } catch (e) {
    console.warn('加载推荐收听失败:', e.message)
  }
}

const playWork = async (work) => {
  if (!work.audioUrl) return toastStore.show('该作品音频正在生成中')
  playerStore.play({
    title: work.title,
    author: work.authorId || '声读',
    url: work.audioUrl
  })
  try { await discoverApi.playWork(work.id) } catch {}
}

const getWorkGradient = (type) => {
  const map = {
    audio:   'bg-gradient-to-br from-[#4F46E5]/40 via-[#6D28D9]/25 to-[#7C3AED]/20',
    podcast: 'bg-gradient-to-br from-emerald-900/50 via-teal-900/30 to-cyan-900/20',
    novel:   'bg-gradient-to-br from-indigo-900/50 via-blue-900/30 to-cyan-900/20',
    music:   'bg-gradient-to-br from-fuchsia-900/50 via-pink-900/30 to-violet-900/20',
  }
  return map[type] || map.audio
}

const getWorkIcon = (type) => {
  const map = { audio: 'fa-headphones', podcast: 'fa-podcast', novel: 'fa-book-open', music: 'fa-music' }
  return map[type] || 'fa-headphones'
}

const getContentLabel = (type) => {
  const map = { audio: 'AI 配音', podcast: 'AI 双播', novel: 'AI 小说', music: 'AI 音乐' }
  return map[type] || 'AI 创作'
}

const formatDuration = (seconds) => {
  if (!seconds) return ''
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return m > 0 ? `${m}:${String(s).padStart(2, '0')}` : `0:${String(s).padStart(2, '0')}`
}

// ==================== 生命周期 ====================
onMounted(() => {
  loadFeaturedWorks()
})
</script>

<style scoped>
.slide-enter-active, .slide-leave-active {
  transition: all 0.3s ease;
  max-height: 200px;
  overflow: hidden;
}
.slide-enter-from, .slide-leave-to {
  max-height: 0;
  opacity: 0;
  margin-top: 0;
}
</style>
