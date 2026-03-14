<template>
  <div class="home-page h-full overflow-y-auto pb-28 hide-scrollbar">

    <!-- ── Header ── -->
    <div class="px-5 pt-12 pb-4 flex justify-between items-center">
      <div>
        <p class="text-[11px] text-gray-500 mb-0.5">{{ greeting }}</p>
        <h1 class="text-xl font-black text-white tracking-tight">声读</h1>
      </div>
      <button class="w-9 h-9 rounded-full bg-white/5 border border-white/8 flex items-center justify-center text-gray-400 hover:text-white transition-colors cursor-pointer relative">
        <i class="fas fa-bell text-sm"></i>
        <span class="absolute top-2 right-2 w-1.5 h-1.5 bg-[#FF9500] rounded-full"></span>
      </button>
    </div>

    <div class="px-5 space-y-5">

      <!-- ══════════════════════════════════
           HERO：AI 声音制作人（Agent 入口）
      ══════════════════════════════════ -->
      <div class="agent-hero rounded-2xl p-5 relative overflow-hidden">
        <!-- 背景光晕 -->
        <div class="agent-orb orb-1"></div>
        <div class="agent-orb orb-2"></div>

        <div class="relative z-10">
          <!-- 标题区 -->
          <div class="flex items-center gap-2.5 mb-4">
            <div class="agent-icon">
              <i class="fas fa-microphone text-xl text-cyan-400"></i>
              <span class="agent-pulse"></span>
            </div>
            <div>
              <div class="flex items-center gap-1.5">
                <h2 class="text-base font-black text-white">AI 声音制作人</h2>
              </div>
              <p class="text-[10px] text-cyan-400/80 mt-0.5">理解你的意图 · 自动创作有声内容</p>
            </div>
          </div>

          <!-- 对话式输入框（核心 CTA）-->
          <div @click="router.push('/workshop')"
               class="agent-input group cursor-pointer mb-3">
            <i class="fas fa-comment-dots text-cyan-400/60 text-sm group-hover:text-cyan-400 transition-colors shrink-0"></i>
            <div class="flex-1 min-w-0">
              <p class="text-sm text-gray-400 group-hover:text-gray-300 transition-colors truncate">{{ currentScene }}</p>
            </div>
            <div class="send-btn">
              <i class="fas fa-arrow-right text-[11px]"></i>
            </div>
          </div>

          <!-- 场景 Chips -->
          <div class="flex gap-2 flex-wrap">
            <button v-for="scene in quickScenes" :key="scene.label"
                    @click="goWorkshop(scene.text)"
                    class="scene-chip">
              {{ scene.label }}
            </button>
          </div>

          <!-- 底部 — 能力标签 + 音色入口 -->
          <div class="flex items-center justify-between mt-4 pt-3 border-t border-white/5">
            <div class="flex items-center gap-3">
              <div v-for="(cap, i) in capabilities" :key="cap"
                   class="flex items-center gap-2 text-[9px] text-cyan-400/50">
                <span v-if="i > 0" class="w-px h-2.5 bg-white/10"></span>
                {{ cap }}
              </div>
            </div>
            <button @click="router.push('/voices')"
                    class="flex items-center gap-1 text-[10px] text-gray-600 hover:text-[#FF9500] transition-colors cursor-pointer shrink-0">
              <i class="fas fa-microphone-lines text-[9px]"></i> 浏览音色
              <i class="fas fa-chevron-right text-[8px]"></i>
            </button>
          </div>
        </div>
      </div>

      <!-- ══════════════════════════════════
           功能矩阵：2×2 + 1 行
      ══════════════════════════════════ -->
      <div>
        <p class="text-[10px] font-bold text-gray-600 uppercase tracking-widest mb-2.5">专项工具</p>
        <div class="grid grid-cols-2 gap-2.5">

          <div @click="router.push('/emotion')"
               class="feat-card feat-card--purple group relative cursor-pointer">
            <div v-if="!authStore.hasFeature('tts_emotion_v2')" class="vip-tag">VIP</div>
            <div class="feat-icon bg-gradient-to-br from-purple-500 to-pink-500">
              <i class="fas fa-masks-theater"></i>
            </div>
            <p class="text-white text-[13px] font-black mt-3 mb-0.5">情感调音台</p>
            <p class="text-[10px] text-gray-500">演讲·配音·广播剧，声音更有戏</p>
          </div>

          <div @click="router.push('/podcast')"
               class="feat-card feat-card--emerald group cursor-pointer">
            <div class="feat-icon bg-gradient-to-br from-emerald-500 to-teal-600">
              <i class="fas fa-podcast"></i>
            </div>
            <p class="text-white text-[13px] font-black mt-3 mb-0.5">AI 播客</p>
            <p class="text-[10px] text-gray-500">两个人的对话，一键生成</p>
          </div>

          <div @click="router.push('/music')"
               class="feat-card feat-card--fuchsia group cursor-pointer active:scale-[0.98] active:shadow-inner">
            <div class="feat-icon bg-gradient-to-br from-fuchsia-500 to-purple-600">
              <i class="fas fa-music"></i>
            </div>
            <p class="text-white text-[13px] font-black mt-3 mb-0.5">AI 音乐</p>
            <p class="text-[10px] text-gray-500">从一句歌词开始</p>
          </div>

          <div @click="router.push('/create')"
               class="feat-card feat-card--orange group cursor-pointer active:scale-[0.98] active:shadow-inner">
            <div class="feat-icon bg-gradient-to-br from-[#FF9500] to-[#FF6B00]">
              <i class="fas fa-microphone-alt"></i>
            </div>
            <p class="text-white text-[13px] font-black mt-3 mb-0.5">文字配音</p>
            <p class="text-[10px] text-gray-500">输入文字，立即成声</p>
          </div>

        </div>
      </div>

      <!-- ══════════════════════════════════
           推荐收听
      ══════════════════════════════════ -->
      <div v-if="featuredWorks.length > 0">
        <div class="flex justify-between items-center mb-2.5">
          <p class="text-[10px] font-bold text-gray-600 uppercase tracking-widest">推荐收听</p>
          <router-link to="/discover" class="text-[10px] text-gray-600 hover:text-[#FF9500] transition-colors">
            更多 <i class="fas fa-chevron-right text-[8px]"></i>
          </router-link>
        </div>
        <div class="flex gap-3 overflow-x-auto hide-scrollbar pb-1">
          <div v-for="work in featuredWorks" :key="work.id"
               @click="playWork(work)"
               class="flex-shrink-0 w-[138px] cursor-pointer group">
            <div class="w-full h-[96px] rounded-xl overflow-hidden relative mb-2"
                 :class="getWorkGradient(work.contentType)">
              <i class="fas text-white/20 text-3xl"
                 :class="getWorkIcon(work.contentType)"
                 style="position:absolute;top:50%;left:50%;transform:translate(-50%,-50%)"></i>
              <div class="absolute inset-0 bg-black/25 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
                <div class="w-9 h-9 rounded-full bg-[#FF9500] flex items-center justify-center text-black shadow-lg">
                  <i class="fas fa-play text-[11px] pl-0.5"></i>
                </div>
              </div>
              <span v-if="work.audioDuration" class="absolute bottom-1.5 right-1.5 text-[8px] text-white/80 bg-black/50 px-1.5 py-0.5 rounded-full">
                {{ formatDuration(work.audioDuration) }}
              </span>
            </div>
            <h4 class="text-[11px] font-bold text-white truncate leading-tight">{{ work.title }}</h4>
            <p class="text-[9px] text-gray-600 mt-0.5">{{ getContentLabel(work.contentType) }}</p>
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
import { ref, computed, onMounted, onUnmounted } from 'vue'
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
  if (h < 6) return '夜深了，还在创作？'
  if (h < 12) return '早安，开启创意的一天'
  if (h < 18) return '下午好，有什么想创作？'
  return '晚上好，来点有声内容吧'
})

const quickScenes = [
  { label: '深夜电台', text: '帮我录一段深夜情感电台独白' },
  { label: '情感祝福', text: '帮我录一段生日祝福语音卡片' },
  { label: '短视频配音', text: '帮我做一段短视频励志旁白' },
  { label: '有声故事', text: '帮我创作一个睡前童话故事' },
]

// 轮播展示场景提示词
const sceneIndex = ref(0)
const currentScene = computed(() => quickScenes[sceneIndex.value].text + '...')
let sceneTimer = null

const capabilities = ['脚本自动生成', '智能音色匹配', '一键发布上架']

const showVoiceSelector = ref(false)
const quickVoice = ref({ voiceId: 'zh_female_vv_uranus_bigtts', name: 'vivi 2.0' })

const goWorkshop = (text) => {
  sessionStorage.setItem('workshop_init_text', text)
  router.push('/workshop')
}
const onVoiceSelected = (voice) => { quickVoice.value = voice }

// ── 首页发现数据缓存（游客离线刷新优化） ──
const SESSION_KEY = 'home:featured'
const CACHE_TTL_MS = 120_000 // 120 秒

const readHomeCache = () => {
  try {
    const raw = sessionStorage.getItem(SESSION_KEY)
    if (!raw) return null
    const { ts, data } = JSON.parse(raw)
    if (Date.now() - ts < CACHE_TTL_MS) return data
    sessionStorage.removeItem(SESSION_KEY)
  } catch { }
  return null
}

const writeHomeCache = (data) => {
  try {
    sessionStorage.setItem(SESSION_KEY, JSON.stringify({ ts: Date.now(), data }))
  } catch { }
}

const featuredWorks = ref([])
const loadFeaturedWorks = async () => {
  // 登录用户不读缓存（isLiked 等状态需实时）
  if (!authStore.isLoggedIn) {
    const cached = readHomeCache()
    if (cached) {
      featuredWorks.value = cached
      return
    }
  }
  try {
    const res = await discoverApi.getWorks({ page: 1, size: 10, sort: 'hot' })
    if (res.records?.length > 0) {
      const filtered = res.records
        .filter(w => w.audioUrl && w.audioDuration && w.audioDuration >= 30)
        .slice(0, 6)
      featuredWorks.value = filtered
      // 仅游客写入缓存
      if (!authStore.isLoggedIn) writeHomeCache(filtered)
    }
  } catch { }
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

onMounted(() => {
  loadFeaturedWorks()
  sceneTimer = setInterval(() => {
    sceneIndex.value = (sceneIndex.value + 1) % quickScenes.length
  }, 3000)
})
onUnmounted(() => { clearInterval(sceneTimer) })
</script>

<style scoped>
.home-page { background: #080c14; }

/* ── Agent Hero ── */
.agent-hero {
  background: linear-gradient(135deg, #0a1628 0%, #0d1f32 50%, #0a1422 100%);
  border: 1px solid rgba(34,211,238,0.15);
}
.agent-hero:hover { border-color: rgba(34,211,238,0.25); }

.agent-orb {
  position: absolute; border-radius: 50%;
  filter: blur(50px); pointer-events: none;
}
.orb-1 { width: 180px; height: 180px; background: rgba(34,211,238,0.08); top: -40px; right: -20px; }
.orb-2 { width: 120px; height: 120px; background: rgba(59,130,246,0.07); bottom: -20px; left: 10px; }
@keyframes orb-pulse { 0%,100%{opacity:0.6;transform:scale(1)} 50%{opacity:1;transform:scale(1.2)} }

.agent-icon {
  width: 48px; height: 48px;
  border-radius: 14px;
  background: linear-gradient(135deg, #1a4a6e, #0e3050);
  border: 1px solid rgba(34,211,238,0.25);
  display: flex; align-items: center; justify-content: center;
  font-size: 22px;
  box-shadow: 0 0 20px rgba(34,211,238,0.15);
  position: relative; flex-shrink: 0;
}
.agent-pulse {
  position: absolute; inset: -4px;
  border-radius: 18px;
  border: 1px solid rgba(34,211,238,0.2);
  animation: agent-ring 2.5s ease-in-out infinite;
}
@keyframes agent-ring {
  0%,100%{opacity:0.6;transform:scale(1)} 50%{opacity:0;transform:scale(1.12)}
}
.agent-badge {
  font-size: 9px; font-weight: 800;
  padding: 2px 6px; border-radius: 4px;
  background: rgba(34,211,238,0.15);
  color: #22d3ee;
  border: 1px solid rgba(34,211,238,0.25);
  letter-spacing: 0.05em;
}

.agent-input {
  display: flex; align-items: center;
  padding: 12px 14px; border-radius: 14px;
  background: rgba(255,255,255,0.05);
  border: 1px solid rgba(255,255,255,0.08);
  gap: 10px;
  transition: all 0.2s;
}
.agent-input:hover {
  border-color: rgba(34,211,238,0.3);
  background: rgba(34,211,238,0.05);
}
.send-btn {
  width: 30px; height: 30px; border-radius: 9px;
  background: linear-gradient(135deg, #0284c7, #0ea5e9);
  display: flex; align-items: center; justify-content: center;
  color: white; flex-shrink: 0;
  box-shadow: 0 2px 10px rgba(14,165,233,0.3);
}

.scene-chip {
  font-size: 11px;
  padding: 5px 12px; border-radius: 9999px;
  background: rgba(255,255,255,0.05);
  border: 1px solid rgba(255,255,255,0.08);
  color: rgba(255,255,255,0.5);
  cursor: pointer; transition: all 0.15s;
  white-space: nowrap;
}
.scene-chip:hover {
  border-color: rgba(34,211,238,0.3);
  color: #22d3ee; background: rgba(34,211,238,0.08);
}
.scene-chip:active { transform: scale(0.93); transition: transform 0.08s; }

/* ── Feature Cards ── */
.feat-card {
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.07);
  border-radius: 16px; padding: 14px;
  transition: all 0.2s; position: relative; overflow: hidden;
}
.feat-card:hover { transform: translateY(-2px); background: rgba(255,255,255,0.06); }
.feat-card:active { transform: scale(0.96); transition: transform 0.08s; }
.feat-card--purple:hover { border-color: rgba(168,85,247,0.3); }
.feat-card--emerald:hover { border-color: rgba(16,185,129,0.3); }
.feat-card--fuchsia:hover { border-color: rgba(192,80,240,0.3); }
.feat-card--orange:hover { border-color: rgba(255,149,0,0.3); }

.feat-icon {
  width: 44px; height: 44px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  color: white; font-size: 18px;
  transition: box-shadow 0.2s;
}
.group:hover .feat-icon { box-shadow: 0 0 18px rgba(255,255,255,0.15); }

.vip-tag {
  position: absolute; top: 10px; right: 10px;
  font-size: 8px; font-weight: 800;
  padding: 2px 6px; border-radius: 4px;
  background: linear-gradient(90deg,#f59e0b,#fbbf24); color: black;
}

/* scrollbar */
.hide-scrollbar { -ms-overflow-style:none; scrollbar-width:none; }
.hide-scrollbar::-webkit-scrollbar { display:none; }
</style>
