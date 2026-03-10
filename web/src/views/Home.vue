<template>
  <div class="home-page h-full overflow-y-auto pb-28 hide-scrollbar">

    <!-- ── Header ── -->
    <div class="px-5 pt-12 pb-4 flex justify-between items-center">
      <div>
        <h1 class="text-xl font-black text-white tracking-tight">
          {{ greeting }}，{{ authStore.user?.nickname || '创作者' }} 👋
        </h1>
        <p class="text-[11px] text-gray-500 mt-0.5">AI 语音创作平台</p>
      </div>
      <button class="w-9 h-9 rounded-full bg-white/5 border border-white/8 flex justify-center items-center text-gray-400 hover:text-white transition-colors cursor-pointer relative">
        <i class="fas fa-bell text-sm"></i>
        <span class="absolute top-1.5 right-1.5 w-1.5 h-1.5 bg-[#FF9500] rounded-full"></span>
      </button>
    </div>

    <div class="px-5 space-y-4">

      <!-- ── Quick Input Entry ── -->
      <div @click="router.push('/workshop')"
           class="quick-entry flex items-center gap-3 px-4 py-3 cursor-pointer">
        <div class="w-7 h-7 rounded-lg bg-cyan-500/20 flex items-center justify-center shrink-0">
          <i class="fas fa-microphone text-cyan-400 text-xs"></i>
        </div>
        <span class="text-sm text-gray-500 flex-1">帮我录一段深夜电台独白...</span>
        <div class="flex gap-1.5">
          <span v-for="scene in quickScenes.slice(0,2)" :key="scene.label"
                class="text-[9px] px-2 py-0.5 rounded-full bg-white/5 text-gray-600">
            {{ scene.emoji }} {{ scene.label }}
          </span>
        </div>
      </div>

      <!-- ── AI 播客 — 主推 Banner ── -->
      <div @click="router.push('/podcast')"
           class="podcast-banner relative rounded-2xl overflow-hidden cursor-pointer group">
        <!-- 背景 -->
        <div class="banner-bg"></div>
        <!-- 装饰粒子 -->
        <div class="banner-dot dot-1"></div>
        <div class="banner-dot dot-2"></div>
        <div class="banner-dot dot-3"></div>

        <div class="relative z-10 p-5">
          <div class="flex items-start justify-between">
            <div class="flex-1">
              <div class="flex items-center gap-2 mb-2">
                <span class="hot-badge">🔥 主推</span>
                <span class="text-[10px] text-emerald-400/80">NEW</span>
              </div>
              <h2 class="text-xl font-black text-white leading-tight mb-1.5">AI 播客</h2>
              <p class="text-[11px] text-white/60 leading-relaxed mb-3">
                多角色 · 真人对话风格<br/>
                一键生成专业双播内容
              </p>
              <div class="flex items-center gap-3">
                <div class="start-btn group-hover:shadow-[0_4px_20px_rgba(16,185,129,0.5)] transition-shadow">
                  <i class="fas fa-podcast text-[11px]"></i>
                  立即创作
                </div>
                <div class="flex items-center gap-1 text-white/40">
                  <i class="fas fa-headphones text-[9px]"></i>
                  <span class="text-[10px]">双声道混音</span>
                </div>
              </div>
            </div>

            <!-- 右侧声波装饰 -->
            <div class="podcast-wave-wrap ml-4">
              <div class="soundwave">
                <span v-for="i in 8" :key="i" :style="`--i:${i}`"></span>
              </div>
              <div class="w-12 h-12 rounded-2xl bg-gradient-to-br from-emerald-400 to-teal-600 flex items-center justify-center text-2xl shadow-[0_0_24px_rgba(16,185,129,0.4)] mx-auto mt-2">
                🎙️
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- ── 核心功能 2×2 Grid ── -->
      <div>
        <h3 class="text-xs font-bold text-gray-500 mb-2.5 flex items-center gap-1.5 uppercase tracking-wider">
          <i class="fas fa-th text-[9px]"></i> 全部功能
        </h3>
        <div class="grid grid-cols-2 gap-2.5">

          <!-- 情感调音台 -->
          <div @click="router.push('/emotion')"
               class="feature-card feature-card--purple group cursor-pointer relative">
            <div v-if="!authStore.hasFeature('tts_emotion_v2')" class="vip-badge">VIP</div>
            <div class="feature-icon bg-gradient-to-br from-purple-500 to-pink-500 group-hover:shadow-[0_0_20px_rgba(168,85,247,0.4)] transition-shadow">
              <i class="fas fa-masks-theater"></i>
            </div>
            <h4 class="text-white text-sm font-black mt-2.5 mb-0.5">情感调音台</h4>
            <p class="text-[10px] text-gray-500">TTS 2.0 · 情感合成</p>
            <div class="feature-arrow"><i class="fas fa-arrow-right text-[8px]"></i></div>
          </div>

          <!-- AI 创作 -->
          <div @click="router.push('/studio')"
               class="feature-card feature-card--orange group cursor-pointer">
            <div class="feature-icon bg-gradient-to-br from-[#FF9500] to-[#FF6B00] group-hover:shadow-[0_0_20px_rgba(255,149,0,0.4)] transition-shadow">
              <i class="fas fa-pen-fancy"></i>
            </div>
            <h4 class="text-white text-sm font-black mt-2.5 mb-0.5">AI 创作</h4>
            <p class="text-[10px] text-gray-500">写作 + 一键配音</p>
            <div class="feature-arrow"><i class="fas fa-arrow-right text-[8px]"></i></div>
          </div>

          <!-- AI 音乐 -->
          <div @click="router.push('/music')"
               class="feature-card feature-card--fuchsia group cursor-pointer">
            <div class="feature-icon bg-gradient-to-br from-fuchsia-500 to-purple-600 group-hover:shadow-[0_0_20px_rgba(192,80,240,0.4)] transition-shadow">
              <i class="fas fa-music"></i>
            </div>
            <h4 class="text-white text-sm font-black mt-2.5 mb-0.5">AI 音乐</h4>
            <p class="text-[10px] text-gray-500">歌曲 & 纯音乐</p>
            <div class="feature-arrow"><i class="fas fa-arrow-right text-[8px]"></i></div>
          </div>

          <!-- 文字配音 -->
          <div @click="router.push('/create')"
               class="feature-card feature-card--cyan group cursor-pointer">
            <div class="feature-icon bg-gradient-to-br from-cyan-500 to-blue-600 group-hover:shadow-[0_0_20px_rgba(34,211,238,0.4)] transition-shadow">
              <i class="fas fa-microphone-alt"></i>
            </div>
            <h4 class="text-white text-sm font-black mt-2.5 mb-0.5">文字配音</h4>
            <p class="text-[10px] text-gray-500">TTS 1.0 · 极速合成</p>
            <div class="feature-arrow"><i class="fas fa-arrow-right text-[8px]"></i></div>
          </div>

        </div>
      </div>

      <!-- ── 推荐收听 ── -->
      <div v-if="featuredWorks.length > 0">
        <div class="flex justify-between items-center mb-2.5">
          <h3 class="text-xs font-bold text-gray-500 flex items-center gap-1.5 uppercase tracking-wider">
            <i class="fas fa-fire text-orange-400 text-[9px]"></i> 推荐收听
          </h3>
          <router-link to="/discover" class="text-[10px] text-gray-600 hover:text-[#FF9500] transition-colors flex items-center gap-1">
            更多 <i class="fas fa-chevron-right text-[8px]"></i>
          </router-link>
        </div>

        <div class="flex gap-3 overflow-x-auto hide-scrollbar pb-1">
          <div v-for="work in featuredWorks" :key="work.id"
               @click="playWork(work)"
               class="flex-shrink-0 w-[140px] group cursor-pointer">
            <!-- 封面 -->
            <div class="w-full h-[100px] rounded-xl overflow-hidden relative mb-2"
                 :class="getWorkGradient(work.contentType)">
              <i class="fas text-white/20 text-3xl absolute inset-0 flex items-center justify-center m-auto w-8 h-8"
                 :class="getWorkIcon(work.contentType)" style="top:50%;left:50%;transform:translate(-50%,-50%)"></i>
              <div class="absolute inset-0 bg-black/20 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
                <div class="w-9 h-9 rounded-full bg-[#FF9500] flex items-center justify-center text-black shadow-lg">
                  <i class="fas fa-play text-[11px] pl-0.5"></i>
                </div>
              </div>
              <span v-if="work.audioDuration" class="absolute bottom-1.5 right-1.5 text-[8px] text-white/80 bg-black/50 px-1.5 py-0.5 rounded-full backdrop-blur-sm">
                {{ formatDuration(work.audioDuration) }}
              </span>
              <span class="absolute top-1.5 left-1.5 text-[8px] text-white/60 bg-black/40 px-1.5 py-0.5 rounded-full">
                {{ getContentLabel(work.contentType) }}
              </span>
            </div>
            <h4 class="text-[11px] font-bold text-white truncate">{{ work.title }}</h4>
          </div>
        </div>
      </div>

    </div>

    <!-- 音色选择器 -->
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
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useToastStore } from '../stores/toast'
import { usePlayerStore } from '../stores/player'
import { discoverApi } from '../api/discover'
import VoiceSelector from '../components/VoiceSelector.vue'

const router = useRouter()
const authStore = useAuthStore()
const toastStore = useToastStore()
const playerStore = usePlayerStore()

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 12) return '早安'
  if (h < 18) return '下午好'
  return '晚上好'
})

const quickScenes = [
  { emoji: '🌙', label: '深夜电台', text: '帮我录一段深夜电台独白' },
  { emoji: '💌', label: '情感祝福', text: '帮我录一段生日祝福' },
  { emoji: '🎬', label: '短视频配音', text: '帮我做一段短视频旁白' },
  { emoji: '📖', label: '有声故事', text: '帮我创作一个睡前故事' },
]

const showVoiceSelector = ref(false)
const quickVoice = ref({ voiceId: 'zh_female_vv_uranus_bigtts', name: 'vivi 2.0' })
const onVoiceSelected = (voice) => {
  quickVoice.value = voice
  toastStore.show(`已切换音色：${voice.name} 🎙️`)
}

const featuredWorks = ref([])
const loadFeaturedWorks = async () => {
  try {
    const res = await discoverApi.getWorks({ page: 1, size: 10, sort: 'hot' })
    if (res.records?.length > 0) {
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
  playerStore.play({ title: work.title, author: work.authorId || '声读', url: work.audioUrl })
  try { await discoverApi.playWork(work.id) } catch { }
}

const getWorkGradient = (type) => ({
  audio:   'bg-gradient-to-br from-[#4F46E5]/40 via-[#6D28D9]/25 to-[#7C3AED]/20',
  podcast: 'bg-gradient-to-br from-emerald-900/50 via-teal-900/30 to-cyan-900/20',
  novel:   'bg-gradient-to-br from-indigo-900/50 via-blue-900/30 to-cyan-900/20',
  music:   'bg-gradient-to-br from-fuchsia-900/50 via-pink-900/30 to-violet-900/20',
}[type] || 'bg-gradient-to-br from-[#4F46E5]/40 via-[#6D28D9]/25 to-[#7C3AED]/20')

const getWorkIcon = (type) =>
  ({ audio: 'fa-headphones', podcast: 'fa-podcast', novel: 'fa-book-open', music: 'fa-music' }[type] || 'fa-headphones')

const getContentLabel = (type) =>
  ({ audio: 'AI 配音', podcast: 'AI 双播', novel: 'AI 小说', music: 'AI 音乐' }[type] || 'AI 创作')

const formatDuration = (seconds) => {
  if (!seconds) return ''
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return m > 0 ? `${m}:${String(s).padStart(2, '0')}` : `0:${String(s).padStart(2, '0')}`
}

onMounted(() => { loadFeaturedWorks() })
</script>

<style scoped>
.home-page { background: #080c14; }

/* ── Quick Entry ── */
.quick-entry {
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.07);
  border-radius: 14px;
  transition: border-color 0.2s;
}
.quick-entry:hover { border-color: rgba(34,211,238,0.25); }

/* ── Podcast Banner ── */
.podcast-banner {
  min-height: 160px;
}
.banner-bg {
  position: absolute; inset: 0;
  background: linear-gradient(135deg, #0d2d20 0%, #0a1f2e 50%, #0d1a2e 100%);
  border: 1px solid rgba(16,185,129,0.2);
  border-radius: 1rem;
}
.podcast-banner:hover .banner-bg {
  border-color: rgba(16,185,129,0.4);
  box-shadow: 0 8px 32px rgba(16,185,129,0.12);
}
.banner-dot {
  position: absolute;
  border-radius: 50%;
  filter: blur(40px);
  pointer-events: none;
}
.dot-1 { width: 120px; height: 120px; background: rgba(16,185,129,0.12); top: -20px; right: 20px; }
.dot-2 { width: 80px; height: 80px; background: rgba(6,182,212,0.08); bottom: -10px; left: 30px; }
.dot-3 { width: 60px; height: 60px; background: rgba(52,211,153,0.1); top: 40px; right: 60px; animation: pulse-dot 3s ease-in-out infinite; }
@keyframes pulse-dot { 0%,100%{opacity:0.5;transform:scale(1)} 50%{opacity:1;transform:scale(1.3)} }

.hot-badge {
  font-size: 10px;
  font-weight: 800;
  padding: 2px 8px;
  border-radius: 9999px;
  background: linear-gradient(90deg, rgba(255,149,0,0.25), rgba(255,80,50,0.2));
  color: #fbbf24;
  border: 1px solid rgba(255,149,0,0.25);
}
.start-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 16px;
  border-radius: 9999px;
  background: linear-gradient(135deg, #10b981, #0d9488);
  color: white;
  font-size: 12px;
  font-weight: 800;
  box-shadow: 0 4px 16px rgba(16,185,129,0.35);
}

/* ── Soundwave animation ── */
.podcast-wave-wrap { display: flex; flex-direction: column; align-items: center; }
.soundwave {
  display: flex; align-items: center; gap: 3px; height: 32px; margin-bottom: 6px;
}
.soundwave span {
  display: block;
  width: 3px; border-radius: 2px;
  background: linear-gradient(to top, rgba(16,185,129,0.3), rgba(52,211,153,0.8));
  animation: sw var(--dur, 1s) ease-in-out infinite;
}
.soundwave span:nth-child(1) { height: 10px; --dur: 0.9s; animation-delay: 0s; }
.soundwave span:nth-child(2) { height: 20px; --dur: 1.1s; animation-delay: 0.1s; }
.soundwave span:nth-child(3) { height: 28px; --dur: 0.8s; animation-delay: 0.2s; }
.soundwave span:nth-child(4) { height: 32px; --dur: 1.2s; animation-delay: 0.3s; }
.soundwave span:nth-child(5) { height: 24px; --dur: 0.95s; animation-delay: 0.4s; }
.soundwave span:nth-child(6) { height: 28px; --dur: 1.05s; animation-delay: 0.2s; }
.soundwave span:nth-child(7) { height: 18px; --dur: 0.85s; animation-delay: 0.1s; }
.soundwave span:nth-child(8) { height: 10px; --dur: 1.0s; animation-delay: 0s; }
@keyframes sw {
  0%,100% { transform: scaleY(1); }
  50% { transform: scaleY(0.35); }
}

/* ── Feature Cards ── */
.feature-card {
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.07);
  border-radius: 16px;
  padding: 14px;
  position: relative;
  transition: all 0.2s;
  overflow: hidden;
}
.feature-card:hover {
  transform: translateY(-2px);
  background: rgba(255,255,255,0.06);
}
.feature-card--purple:hover { border-color: rgba(168,85,247,0.3); }
.feature-card--orange:hover { border-color: rgba(255,149,0,0.3); }
.feature-card--fuchsia:hover { border-color: rgba(192,80,240,0.3); }
.feature-card--cyan:hover { border-color: rgba(34,211,238,0.3); }

.feature-icon {
  width: 44px; height: 44px;
  border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  color: white;
  font-size: 18px;
}

.feature-arrow {
  position: absolute;
  top: 12px; right: 12px;
  width: 20px; height: 20px;
  border-radius: 50%;
  background: rgba(255,255,255,0.06);
  display: flex; align-items: center; justify-content: center;
  color: rgba(255,255,255,0.3);
  transition: all 0.2s;
}
.group:hover .feature-arrow {
  background: rgba(255,255,255,0.12);
  color: white;
  transform: translateX(2px);
}

.vip-badge {
  position: absolute;
  top: 10px; right: 10px;
  font-size: 8px;
  font-weight: 800;
  padding: 2px 6px;
  border-radius: 4px;
  background: linear-gradient(90deg, #f59e0b, #fbbf24);
  color: black;
}

/* scrollbar */
.hide-scrollbar { -ms-overflow-style: none; scrollbar-width: none; }
.hide-scrollbar::-webkit-scrollbar { display: none; }
</style>
