<template>
  <div v-if="playerStore.showPlayer" class="absolute bottom-20 left-4 right-4 bg-black/80 backdrop-blur-md rounded-2xl p-3 border border-white/10 z-[100] flex items-center shadow-lg">
    <!-- 封面图 -->
    <div class="w-10 h-10 rounded-lg bg-gradient-to-br from-indigo-500 to-purple-600 flex-shrink-0 flex justify-center items-center overflow-hidden cursor-pointer"
         @click="hasLyrics && toggleLyrics()">
      <img v-if="playerStore.currentTrack?.cover" :src="playerStore.currentTrack.cover" class="w-full h-full object-cover" />
      <i v-else class="fas fa-music text-white/70"></i>
    </div>
    
    <!-- 信息区 -->
    <div class="ml-3 flex-1 overflow-hidden cursor-pointer" @click="hasLyrics && toggleLyrics()">
      <h4 class="text-white text-xs font-bold truncate">{{ playerStore.currentTrack?.title || '未知曲目' }}</h4>
      <p class="text-gray-400 text-[10px] truncate">{{ playerStore.currentTrack?.author || '未知作者' }}</p>
    </div>

    <!-- 歌词按钮 -->
    <button v-if="hasLyrics" @click="toggleLyrics"
            class="mr-2 text-xs p-1.5 rounded-full transition-all cursor-pointer"
            :class="showLyrics ? 'text-fuchsia-400 bg-fuchsia-500/20' : 'text-gray-500 hover:text-fuchsia-400'">
      <i class="fas fa-align-left"></i>
    </button>

    <!-- 边听边问互动按钮 -->
    <button @click="toggleInteraction" class="mr-3 text-[#FF9500] text-sm relative z-10 p-1">
      <div v-if="isInteracting" class="absolute inset-0 bg-[#FF9500]/20 rounded-full animate-ping"></div>
      <i class="fas fa-microphone-lines"></i>
    </button>

    <!-- 播放控制 -->
    <button @click="playerStore.togglePlay" class="w-8 h-8 rounded-full bg-white text-black flex justify-center items-center">
      <i :class="playerStore.isPlaying ? 'fas fa-pause' : 'fas fa-play'"></i>
    </button>
    <button @click="playerStore.close" class="ml-2 text-gray-500 text-xs p-1">
      <i class="fas fa-times"></i>
    </button>

    <!-- 进度条 -->
    <div class="absolute bottom-0 left-0 h-[2px] bg-white/20 w-full overflow-hidden rounded-b-2xl">
       <div class="h-full bg-[#FF9500] transition-all" :style="{ width: progressPercent + '%' }"></div>
    </div>
  </div>

  <!-- ═══ 歌词面板（全屏） ═══ -->
  <transition name="lyrics-panel">
    <div v-if="showLyrics && hasLyrics"
         class="fixed inset-0 z-[105] flex flex-col"
         @touchstart="onTouchStart"
         @touchmove="onTouchMove"
         @touchend="onTouchEnd">
      <!-- 全屏深色背景 -->
      <div class="absolute inset-0 bg-gradient-to-b from-[#0d0d12] via-[#0a0a0e] to-black"></div>
      
      <div class="relative z-10 flex flex-col h-full w-full max-w-[430px] mx-auto">
        
        <!--  拖拽关闭手柄 (Apple Music 风格) -->
        <div class="flex justify-center pt-3 pb-1 cursor-pointer" @click="showLyrics = false">
          <div class="w-10 h-1 rounded-full bg-white/20"></div>
        </div>

        <!-- 头部：歌曲信息 + 关闭按钮 -->
        <div class="px-5 pt-1 pb-3 flex items-center justify-between">
          <div class="min-w-0 flex-1">
            <h3 class="text-white text-sm font-bold truncate">{{ playerStore.currentTrack?.title }}</h3>
            <p class="text-[10px] text-gray-500">{{ playerStore.currentTrack?.author }}</p>
          </div>
          <button @click="showLyrics = false"
                  class="w-9 h-9 rounded-full bg-white/10 flex items-center justify-center text-gray-300 hover:text-white hover:bg-white/20 cursor-pointer transition-all active:scale-90">
            <i class="fas fa-chevron-down text-sm"></i>
          </button>
        </div>

        <!-- 可拖拽进度条 -->
        <div class="px-5 mb-3">
          <div class="h-1 bg-white/10 rounded-full overflow-hidden cursor-pointer"
               @click="onProgressClick">
            <div class="h-full bg-gradient-to-r from-fuchsia-500 to-purple-500 rounded-full transition-all duration-200" :style="{ width: progressPercent + '%' }"></div>
          </div>
          <div class="flex justify-between mt-1">
            <span class="text-[10px] text-gray-500 font-mono">{{ formatSec(playerStore.currentTime) }}</span>
            <span class="text-[10px] text-gray-500 font-mono">{{ formatSec(playerStore.safeDuration) }}</span>
          </div>
        </div>

        <!-- 歌词滚动区 -->
        <div ref="lyricsContainer" class="flex-1 overflow-y-auto px-6 lyrics-scroll hide-scrollbar">
          <div class="min-h-[35vh]"></div>
          <div v-for="(line, idx) in displayLines" :key="idx"
               :ref="el => { if (el) lyricRefs[idx] = el }"
               class="transition-all duration-500 cursor-pointer select-none"
               :class="getLineClass(idx)"
               :style="getLineStyle(idx)"
               @click="seekToLine(idx)">
            {{ line.text }}
          </div>
          <div class="min-h-[35vh]"></div>
        </div>

        <!-- 底部播放控制 -->
        <div class="px-5 pb-8 pt-3 flex items-center justify-center gap-8">
          <button @click="playerStore.seek(Math.max(0, playerStore.currentTime - 10))" class="text-gray-400 hover:text-white transition-colors cursor-pointer active:scale-90">
            <i class="fas fa-backward text-base"></i>
          </button>
          <button @click="playerStore.togglePlay" class="w-14 h-14 rounded-full bg-gradient-to-br from-fuchsia-500 to-purple-600 flex items-center justify-center text-white cursor-pointer shadow-[0_0_30px_rgba(217,70,239,0.3)] active:scale-95 transition-transform">
            <i :class="playerStore.isPlaying ? 'fas fa-pause text-xl' : 'fas fa-play text-xl pl-1'"></i>
          </button>
          <button @click="playerStore.seek(Math.min(playerStore.safeDuration, playerStore.currentTime + 10))" class="text-gray-400 hover:text-white transition-colors cursor-pointer active:scale-90">
            <i class="fas fa-forward text-base"></i>
          </button>
        </div>
      </div>
    </div>
  </transition>

  <!-- 边听边问弹窗 -->
  <transition name="slide-up">
    <div v-if="isInteracting" class="absolute bottom-0 left-0 w-full h-[60%] bg-zinc-900 rounded-t-3xl z-[110] p-5 shadow-[-0_-10px_30px_rgba(0,0,0,0.5)] flex flex-col">
      <div class="flex justify-between items-center mb-4">
         <h3 class="text-white font-bold text-sm"><i class="fas fa-robot text-[#FF9500]"></i> 边听边问助手</h3>
         <button @click="closeInteraction" class="text-gray-400"><i class="fas fa-times"></i></button>
      </div>
      
      <div class="flex-1 overflow-y-auto mb-4 space-y-4">
         <div v-for="(msg, i) in qaHistory" :key="i" 
              class="p-3 rounded-2xl max-w-[85%] text-sm"
              :class="msg.role === 'user' ? 'bg-[#FF9500] text-black self-end ml-auto rounded-tr-sm' : 'bg-white/10 text-white self-start rounded-tl-sm'">
           {{ msg.content }}
           <span v-if="msg.loading" class="inline-block ml-1 animate-pulse">...</span>
         </div>
      </div>

      <div class="flex justify-center items-center h-24 mb-6">
         <button 
           @touchstart.prevent="startRecord" 
           @touchend.prevent="stopRecord"
           @mousedown.prevent="startRecord"
           @mouseup.prevent="stopRecord"
           @mouseleave.prevent="stopRecord"
           class="relative w-16 h-16 rounded-full flex justify-center items-center text-white text-2xl transition-all select-none"
           :class="recordingPrompt ? 'bg-red-500 scale-110 shadow-[0_0_20px_rgba(239,68,68,0.5)]' : 'bg-white/20 hover:bg-white/30'"
         >
           <i class="fas fa-microphone"></i>
         </button>
      </div>
      <p class="text-center text-gray-500 text-xs">长按说话，松开后自动分析当前音频内容</p>
    </div>
  </transition>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue'
import { usePlayerStore } from '../stores/player'
import { useWebSocket } from '../composables/useWebSocket'

const playerStore = usePlayerStore()

// ==================== 歌词 ====================

const showLyrics = ref(false)
const lyricsContainer = ref(null)
const lyricRefs = ref({})

const hasLyrics = computed(() => {
  return !!playerStore.currentTrack?.lyrics
})

const progressPercent = computed(() => {
  return (playerStore.currentTime / (playerStore.safeDuration || 1)) * 100
})

// ── 段标签正则 ──
const SECTION_RE = /^\[([^\]]+)\]$/

/**
 * ═══════════════════════════════════════════════════════
 * 段落结构化歌词时间分配 (Apple Music / Spotify 思路)
 * ═══════════════════════════════════════════════════════
 *
 * 真实歌曲的结构:
 *  1. 先解析成 Section[]  →  每个 Section 有类型和歌词行
 *  2. 根据 section 类型分配各段时间比例
 *  3. 在每段内部，按歌词字符数量分配每行时间
 *
 * 典型 3:30 歌曲结构:
 *   [Intro]  ~10s    → 5%
 *   [Verse]  ~35s    → 18%
 *   [Verse]  ~35s    → 18%
 *   [Chorus] ~30s    → 15%
 *   [Verse]  ~25s    → 13%
 *   [Chorus] ~30s    → 15%
 *   [Bridge] ~15s    → 8%
 *   [Chorus] ~20s    → 10%
 *   尾奏             → ~3%
 */

/**
 * ★ 是否有真实时间戳（来自 Mureka recognize API）
 * 有 → 精准模式（毫秒级同步，如 QQ 音乐/网易云）
 * 无 → 降级模式（启发式算法 + 同步延迟）
 */
const hasRealTimings = computed(() => {
  const t = playerStore.currentTrack?.lyricTimings
  return Array.isArray(t) && t.length > 0
})

// ★ 降级模式参数（当没有真实时间戳时使用）
const SYNC_DELAY = 1.5
const SECTION_GAP = 1.5
const SECTION_WEIGHTS = {
  intro: 8, outro: 6, interlude: 5,
  verse: 2.5, chorus: 2.5, bridge: 2.5,
  'pre-chorus': 2, hook: 2.5
}

function getSectionWeight(tag) {
  const t = tag.toLowerCase().replace(/\s+\d+$/, '')
  for (const [key, w] of Object.entries(SECTION_WEIGHTS)) {
    if (t.includes(key)) return w
  }
  return 1
}

// 把歌词解析成可展示行
const displayLines = computed(() => {
  // ★ 精准模式：直接用 API 返回的时间戳文本
  if (hasRealTimings.value) {
    return playerStore.currentTrack.lyricTimings.map(t => ({
      text: t.text,
      isTag: SECTION_RE.test(t.text),
      tag: SECTION_RE.test(t.text) ? SECTION_RE.exec(t.text)?.[1] : null
    }))
  }

  // ★ 降级模式：从歌词文本解析
  if (!playerStore.currentTrack?.lyrics) return []
  const raw = playerStore.currentTrack.lyrics
    .split('\n')
    .map(l => l.trim())
    .filter(l => l.length > 0)

  return raw.map(text => {
    const m = SECTION_RE.exec(text)
    return { text, isTag: !!m, tag: m ? m[1] : null }
  })
})

// ═══ 时间分配：精准模式 vs 降级模式 ═══

const lineTimings = computed(() => {
  const lines = displayLines.value
  if (!lines.length) return []

  // ──── 精准模式 ────
  if (hasRealTimings.value) {
    return playerStore.currentTrack.lyricTimings.map(t => ({
      start: t.start / 1000,
      end: t.end / 1000
    }))
  }

  // ──── 降级模式（启发式算法） ────
  const dur = playerStore.safeDuration
  if (!dur) return []

  const sections = []
  let currentSection = { tag: '_default', lines: [], lineIndices: [] }
  lines.forEach((line, idx) => {
    if (line.isTag) {
      if (currentSection.lines.length > 0 || currentSection.tag !== '_default') {
        sections.push(currentSection)
      }
      currentSection = { tag: line.tag, tagIdx: idx, lines: [], lineIndices: [] }
    } else {
      currentSection.lines.push(line.text)
      currentSection.lineIndices.push(idx)
    }
  })
  if (currentSection.lines.length > 0 || currentSection.tag !== '_default') {
    sections.push(currentSection)
  }

  const sectionWeights = sections.map(sec => {
    const tagWeight = getSectionWeight(sec.tag)
    const lyricChars = sec.lines.reduce((sum, l) => sum + l.length, 0)
    return tagWeight + lyricChars * 0.15
  })
  const totalSectionWeight = sectionWeights.reduce((a, b) => a + b, 0) || 1
  const preludeTime = dur * 0.05
  const totalGapTime = Math.max(0, sections.length - 1) * SECTION_GAP
  const availableTime = dur - preludeTime - totalGapTime

  const timings = new Array(lines.length)
  let cursor = preludeTime
  sections.forEach((sec, si) => {
    if (si > 0) cursor += SECTION_GAP
    const sectionDur = (sectionWeights[si] / totalSectionWeight) * availableTime
    const tagWeight = getSectionWeight(sec.tag)
    const lyricWeights = sec.lines.map(l => Math.max(l.length * 0.15, 0.3))
    const totalInSection = tagWeight + lyricWeights.reduce((a, b) => a + b, 0)
    if (sec.tagIdx !== undefined) {
      const tagDur = (tagWeight / totalInSection) * sectionDur
      timings[sec.tagIdx] = { start: cursor, end: cursor + tagDur }
      cursor += tagDur
    }
    sec.lineIndices.forEach((lineIdx, i) => {
      const lineDur = (lyricWeights[i] / totalInSection) * sectionDur
      timings[lineIdx] = { start: cursor, end: cursor + lineDur }
      cursor += lineDur
    })
  })
  for (let i = 0; i < lines.length; i++) {
    if (!timings[i]) {
      timings[i] = { start: cursor, end: cursor + 1 }
      cursor += 1
    }
  }
  return timings
})

// 二分查找当前行
const currentLineIndex = computed(() => {
  // ★ 精准模式不需要延迟；降级模式需要 SYNC_DELAY
  const delay = hasRealTimings.value ? 0 : SYNC_DELAY
  const t = Math.max(0, playerStore.currentTime - delay)
  const timings = lineTimings.value
  if (!timings.length) return 0

  let lo = 0, hi = timings.length - 1, result = 0
  while (lo <= hi) {
    const mid = (lo + hi) >> 1
    if (timings[mid].start <= t) {
      result = mid
      lo = mid + 1
    } else {
      hi = mid - 1
    }
  }
  return result
})

// 歌词行样式 (Apple Music 风格渐变透明度)
function getLineClass(idx) {
  const cur = currentLineIndex.value
  const line = displayLines.value[idx]
  if (!line) return ''

  if (idx === cur) {
    return line.isTag
      ? 'text-fuchsia-400/60 text-xs font-medium uppercase tracking-widest py-2'
      : 'text-white text-lg font-bold py-3'
  }

  if (line.isTag) return 'text-gray-600 text-xs font-medium uppercase tracking-widest py-2'

  const dist = Math.abs(idx - cur)
  if (dist === 1) return 'text-gray-400 text-[15px] py-2.5'
  if (dist === 2) return 'text-gray-600 text-sm py-2'
  return 'text-gray-700/70 text-sm py-2'
}

function getLineStyle(idx) {
  const cur = currentLineIndex.value
  const dist = Math.abs(idx - cur)
  if (idx === cur) return { transform: 'scale(1.02)', transformOrigin: 'left center' }
  if (dist <= 2) return {}
  return { opacity: Math.max(0.2, 1 - dist * 0.12) }
}

const toggleLyrics = () => {
  showLyrics.value = !showLyrics.value
}

const seekToLine = (idx) => {
  const timings = lineTimings.value
  if (!timings.length || idx < 0 || idx >= timings.length) return
  playerStore.seek(timings[idx].start)
}

const onProgressClick = (e) => {
  const rect = e.currentTarget.getBoundingClientRect()
  const ratio = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width))
  playerStore.seek(ratio * playerStore.safeDuration)
}

const formatSec = (s) => {
  if (!s || isNaN(s) || !isFinite(s)) return '0:00'
  const m = Math.floor(s / 60)
  const sec = Math.floor(s % 60)
  return `${m}:${String(sec).padStart(2, '0')}`
}

// ── 下滑关闭手势 ──
let touchStartY = 0
let touchDeltaY = 0

const onTouchStart = (e) => {
  touchStartY = e.touches[0].clientY
  touchDeltaY = 0
}
const onTouchMove = (e) => {
  touchDeltaY = e.touches[0].clientY - touchStartY
}
const onTouchEnd = () => {
  // 下拉超过 80px 关闭
  if (touchDeltaY > 80) {
    showLyrics.value = false
  }
  touchDeltaY = 0
}

// 自动滚动歌词
watch(currentLineIndex, (idx) => {
  nextTick(() => {
    const el = lyricRefs.value[idx]
    if (el && lyricsContainer.value) {
      el.scrollIntoView({ behavior: 'smooth', block: 'center' })
    }
  })
})

// ==================== 边听边问 ====================

const isInteracting = ref(false)
const recordingPrompt = ref(false)
const qaHistory = ref([
  { role: 'ai', content: '您好！针对您正在收听的内容，有什么想问我的吗？' }
])

const interactWs = useWebSocket('/ws/interaction', {
   onOpen: () => {
     // WebSocket 连接成功，恰天备用
   },
   onMessage: (event) => {
      if (typeof event.data === 'string') {
          try {
             const data = JSON.parse(event.data)
             if (data.event === 'text_start') {
                 qaHistory.value.push({ role: 'ai', content: '', loading: true })
             } else if (data.event === 'text_chunk') {
                 const lastMsg = qaHistory.value[qaHistory.value.length - 1]
                 if (lastMsg && lastMsg.role === 'ai') lastMsg.content += data.text
             } else if (data.event === 'text_end') {
                 const lastMsg = qaHistory.value[qaHistory.value.length - 1]
                 if (lastMsg && lastMsg.role === 'ai') lastMsg.loading = false
             }
          } catch(e){}
      } else {
          const audioBlob = new Blob([event.data], { type: 'audio/mp3' })
          const url = URL.createObjectURL(audioBlob)
          const tmpAudio = new Audio(url)
          tmpAudio.play()
      }
   }
})

const toggleInteraction = () => {
    isInteracting.value = !isInteracting.value
    if (isInteracting.value) {
       playerStore.togglePlay()
       interactWs.connect()
    }
}

const closeInteraction = () => {
    isInteracting.value = false
    interactWs.close()
}

let mediaRecorder = null
let audioChunks = []

const startRecord = async () => {
    if (recordingPrompt.value) return
    recordingPrompt.value = true
    try {
        const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
        mediaRecorder = new MediaRecorder(stream, { mimeType: 'audio/webm' })
        audioChunks = []
        mediaRecorder.ondataavailable = e => { if (e.data.size > 0) audioChunks.push(e.data) }
        mediaRecorder.start()
    } catch (e) {
        console.warn('[GlobalPlayer] 获取麦克风失败:', e)
        recordingPrompt.value = false
    }
}

const stopRecord = () => {
    if (!recordingPrompt.value) return
    recordingPrompt.value = false
    if (mediaRecorder && mediaRecorder.state !== 'inactive') {
        mediaRecorder.onstop = () => {
            const blob = new Blob(audioChunks, { type: 'audio/webm' })
            qaHistory.value.push({ role: 'user', content: '[语音消息]' })
            interactWs.send(blob)
        }
        mediaRecorder.stop()
        mediaRecorder.stream.getTracks().forEach(t => t.stop())
    }
}
</script>

<style scoped>
.slide-up-enter-active, .slide-up-leave-active { transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1); }
.slide-up-enter-from, .slide-up-leave-to { transform: translateY(100%); opacity: 0; }

.lyrics-panel-enter-active, .lyrics-panel-leave-active {
  transition: all 0.4s cubic-bezier(0.32, 0.72, 0, 1);
}
.lyrics-panel-enter-from { transform: translateY(100%); }
.lyrics-panel-leave-to { transform: translateY(100%); opacity: 0; }

.lyrics-scroll {
  mask-image: linear-gradient(
    to bottom,
    transparent 0%,
    black 12%,
    black 88%,
    transparent 100%
  );
  -webkit-mask-image: linear-gradient(
    to bottom,
    transparent 0%,
    black 12%,
    black 88%,
    transparent 100%
  );
}
</style>
