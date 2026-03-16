<template>
  <div class="h-full overflow-y-auto hide-scrollbar bg-gradient-to-b from-[#0a0a1a] to-[#050510]">
    <!-- Header -->
    <div class="sticky top-0 z-30 px-4 pt-12 pb-2.5 bg-gradient-to-b from-[#0a0a1a] to-[#0a0a1a]/95 backdrop-blur-xl">
      <div class="flex items-center gap-2.5">
        <button @click="goBack" class="w-8 h-8 rounded-full bg-white/5 border border-white/10 flex justify-center items-center text-white cursor-pointer active:scale-95 transition-transform">
          <i class="fas fa-chevron-left text-xs"></i>
        </button>
        <div class="flex-1 min-w-0">
          <h1 class="text-base font-bold text-white">AI 声音工坊</h1>
          <p class="text-[9px] text-gray-500">一句话完成有声内容制作</p>
        </div>
        <button @click="clearChat" v-if="messages.length > 1"
                class="text-[10px] text-gray-500 hover:text-white transition-colors px-2 py-1 rounded-lg bg-white/5 border border-white/10 active:scale-95">
          <i class="fas fa-redo-alt mr-0.5"></i>重置
        </button>
      </div>
    </div>

    <!-- Scene Selector -->
    <div v-if="!activeScene && !hasUserMessages" class="px-4 pt-1 pb-3">
      <h3 class="text-[11px] font-bold text-gray-400 mb-2.5 tracking-wider">选择创作场景</h3>
      <div class="grid grid-cols-3 gap-2">
        <div v-for="scene in scenes" :key="scene.id" @click="applyScene(scene)"
             class="relative rounded-xl p-2.5 cursor-pointer transition-all active:scale-[0.95] border overflow-hidden"
             :class="scene.borderClass">
          <div class="w-8 h-8 rounded-xl flex items-center justify-center mb-1.5" :class="scene.iconBg">
            <i :class="[scene.icon, 'text-sm']" class="text-white/80"></i>
          </div>
          <h4 class="text-white text-xs font-bold leading-tight">{{ scene.title }}</h4>
          <p class="text-[9px] text-gray-500 mt-0.5 leading-tight line-clamp-1">{{ scene.desc }}</p>
        </div>
      </div>
    </div>

    <!-- Selected Scene Badge -->
    <div v-if="activeScene && !hasUserMessages" class="px-4 pt-1 pb-1">
      <div class="flex items-center gap-2">
        <span class="text-xs bg-white/5 border border-white/10 rounded-full px-2.5 py-1 text-gray-300 flex items-center gap-1">
          {{ activeScene.title }}
        </span>
        <button @click="resetScene" class="text-[10px] text-gray-600 hover:text-gray-400 transition-colors">
          <i class="fas fa-times"></i> 换场景
        </button>
      </div>
    </div>

    <!-- Chat Messages -->
    <div ref="chatContainer" class="px-4 pb-32">
      <div v-for="(msg, idx) in messages" :key="idx" class="mb-3">

        <!-- AI Message -->
        <div v-if="msg.role === 'ai'" class="flex gap-2">
          <div class="w-7 h-7 rounded-[8px] bg-white/8 border border-white/10 flex justify-center items-center shrink-0 mt-0.5">
            <span class="text-[9px] font-black text-white/60 tracking-tight">SR</span>
          </div>
          <div class="flex-1 min-w-0">
            <div class="text-[9px] text-gray-600 mb-0.5">AI 声音制作人</div>
            <!-- 流式打字中显示光标 -->
            <div v-if="msg.streaming && !msg.text" class="ai-msg-content bg-white/[0.04] border border-white/[0.08] rounded-2xl rounded-tl-sm p-3 text-[13px] text-gray-400 leading-relaxed">
              <span class="typing-dots">思考中</span>
            </div>
            <div v-else-if="msg.text" class="ai-msg-content bg-white/[0.04] border border-white/[0.08] rounded-2xl rounded-tl-sm p-3 text-[13px] text-gray-200 leading-relaxed" v-html="renderMarkdown(msg.text)"></div>

            <!-- ★ Action Buttons (welcome / post-synthesis) -->
            <div v-if="msg.actions && msg.actions.length" class="mt-2 flex flex-wrap gap-1.5">
              <button v-for="action in msg.actions" :key="action.label"
                      @click="handleAction(action)"
                      class="px-3 py-1.5 rounded-full text-[11px] font-medium border transition-all active:scale-95 cursor-pointer"
                      :class="action.style || 'bg-white/5 border-white/10 text-gray-300 active:bg-cyan-500/10 active:border-cyan-500/30 active:text-cyan-300'">
                <i v-if="action.icon" :class="action.icon" class="mr-1 text-[10px]"></i>{{ action.label }}
              </button>
            </div>

            <!-- ★ Enhanced Audio Player -->
            <div v-if="msg.audioUrl" class="mt-1.5 bg-gradient-to-r from-cyan-500/10 to-blue-500/10 border border-cyan-500/20 rounded-xl p-2.5">
              <div class="flex items-center gap-2.5">
                <button @click="playAudio(msg.audioUrl)"
                        class="w-9 h-9 rounded-full bg-gradient-to-br from-cyan-400 to-blue-500 flex justify-center items-center text-white shadow-[0_0_12px_rgba(34,211,238,0.35)] active:scale-90 transition-transform cursor-pointer shrink-0">
                  <i :class="currentAudio === msg.audioUrl ? 'fas fa-pause' : 'fas fa-play'" class="text-xs"></i>
                </button>
                <div class="flex-1 min-w-0">
                  <div class="text-[11px] text-white font-bold">语音已生成</div>
                  <!-- Progress bar -->
                  <div class="mt-1 h-1 bg-white/10 rounded-full overflow-hidden cursor-pointer" @click="seekAudio($event, msg.audioUrl)">
                    <div class="h-full bg-gradient-to-r from-cyan-400 to-blue-400 rounded-full transition-all duration-200"
                         :style="{ width: currentAudio === msg.audioUrl ? audioProgress + '%' : '0%' }"></div>
                  </div>
                  <div class="flex justify-between mt-0.5">
                    <span class="text-[9px] text-gray-500">{{ currentAudio === msg.audioUrl ? formatTime(audioCurrentTime) : '0:00' }}</span>
                    <span class="text-[9px] text-gray-500">{{ currentAudio === msg.audioUrl && audioDuration ? formatTime(audioDuration) : '--:--' }}</span>
                  </div>
                </div>
                <a :href="msg.audioUrl" download class="text-cyan-400 text-xs hover:text-cyan-300 transition-colors p-1">
                  <i class="fas fa-download"></i>
                </a>
              </div>
              <!-- ★ Saved to library hint -->
              <div class="mt-1.5 flex items-center gap-1 text-[9px] text-green-400/70">
                <i class="fas fa-check-circle"></i> 已保存到创作库
              </div>
            </div>

            <!-- Pipeline Steps — 仅在有实质工具调用时显示 -->
            <div v-if="msg.steps && msg.steps.length > 1" class="mt-1.5 space-y-0.5">
              <div v-for="(step, si) in msg.steps" :key="si"
                   class="flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-[10px]"
                   :class="step.done ? 'bg-green-500/5 text-green-400' : 'bg-white/[0.02] text-gray-500'">
                <i :class="step.done ? 'fas fa-check-circle text-green-400' : 'far fa-circle text-gray-600'" class="text-[10px]"></i>
                <span>{{ step.label }}</span>
                <span v-if="step.detail" class="text-gray-500 truncate flex-1 text-right">{{ step.detail }}</span>
              </div>
            </div>

            <!-- ★ Regenerate button (on non-welcome AI messages) -->
            <div v-if="idx > 0 && msg.role === 'ai' && !loading" class="mt-1 flex items-center gap-2">
              <button @click="regenerate(idx)"
                      class="text-[9px] text-gray-600 hover:text-gray-400 transition-colors flex items-center gap-1 cursor-pointer">
                <i class="fas fa-redo text-[8px]"></i> 重新生成
              </button>
            </div>
          </div>
        </div>

        <!-- User Message -->
        <div v-else class="flex justify-end">
          <div class="max-w-[78%]">
            <div class="bg-gradient-to-br from-[#4F46E5] to-[#6366F1] rounded-2xl rounded-tr-sm px-3.5 py-2 text-[13px] text-white leading-relaxed">
              {{ msg.text }}
            </div>
          </div>
        </div>
      </div>

      <!-- ★ 简洁思考提示（替代旧的假管线进度卡）-->
      <!-- SSE 流式模式下，文字已经在 streamMsg 气泡中实时显示，这里不再需要多步进度 -->
    </div>

    <!-- Quick Tags -->
    <div v-if="activeScene && !hasUserMessages"
         class="fixed left-0 right-0 px-4 pb-1.5 z-20"
         :style="{ bottom: inputBarHeight + 'px' }">
      <div class="flex gap-1.5 overflow-x-auto hide-scrollbar">
        <button v-for="tag in activeScene.quickTags" :key="tag" @click="sendMessage(tag)"
                class="shrink-0 px-2.5 py-1.5 rounded-full text-[11px] font-medium bg-white/5 border border-white/10 text-gray-300 active:bg-cyan-500/10 active:border-cyan-500/30 active:text-cyan-300 transition-all cursor-pointer active:scale-95">
          {{ tag }}
        </button>
      </div>
    </div>

    <!-- Input Bar -->
    <div ref="inputBar" class="fixed bottom-0 left-0 right-0 bg-[#0a0a1a]/95 backdrop-blur-xl border-t border-white/5 px-3.5 z-30"
         :class="activeScene && !hasUserMessages ? 'py-2' : 'py-2.5'">
      <div class="flex gap-2 items-end">
        <div class="flex-1 relative">
          <textarea ref="inputEl" v-model="inputText"
            @keydown.enter.exact.prevent="sendMessage(inputText)"
            @input="autoResize" :placeholder="inputPlaceholder" rows="1"
            class="w-full bg-white/5 border border-white/10 rounded-2xl px-3.5 py-2 text-[13px] text-white placeholder-gray-600 resize-none focus:outline-none focus:border-cyan-500/50 transition-colors overflow-hidden"
            style="max-height: 80px;"></textarea>
        </div>
        <!-- ★ 语音输入按钮 -->
        <button @click="toggleVoiceInput"
          class="w-9 h-9 rounded-full flex justify-center items-center shrink-0 transition-all cursor-pointer mb-0.5"
          :class="isListening
            ? 'bg-red-500 text-white shadow-[0_0_12px_rgba(239,68,68,0.5)] animate-pulse'
            : 'bg-white/5 border border-white/10 text-gray-400 active:bg-white/10'">
          <i class="fas fa-microphone text-xs"></i>
        </button>
        <button @click="sendMessage(inputText)" :disabled="!inputText.trim() || loading"
          class="w-9 h-9 rounded-full bg-gradient-to-br from-cyan-500 to-blue-600 flex justify-center items-center text-white shrink-0 shadow-[0_0_10px_rgba(34,211,238,0.25)] active:scale-90 transition-all disabled:opacity-30 disabled:shadow-none cursor-pointer mb-0.5">
          <i class="fas fa-paper-plane text-xs"></i>
        </button>
      </div>
    </div>

    <audio ref="audioPlayer" @ended="onAudioEnded" @timeupdate="onTimeUpdate" @loadedmetadata="onMetadataLoaded"></audio>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useToastStore } from '../stores/toast'
import request from '../api/request'

const router = useRouter()
const authStore = useAuthStore()
const toastStore = useToastStore()

// ═══════════════════════════════════════════════════════
// Lightweight markdown renderer
// ═══════════════════════════════════════════════════════
function renderMarkdown(text) {
  if (!text) return ''
  // 安全：转义 HTML 标签（防 XSS）
  let html = text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')

  // 处理各种 markdown 格式
  html = html
    // ``` 代码块
    .replace(/```([\s\S]*?)```/g, '<pre class="bg-white/5 rounded-lg p-2 my-1 text-[11px] text-cyan-300 overflow-x-auto">$1</pre>')
    // `行内代码`
    .replace(/`([^`]+)`/g, '<code class="bg-white/8 px-1 py-0.5 rounded text-[11px] text-cyan-300">$1</code>')
    // **粗体**
    .replace(/\*\*(.+?)\*\*/g, '<strong class="text-white font-bold">$1</strong>')
    // *斜体*
    .replace(/(?<![*])\*([^*]+)\*(?![*])/g, '<em class="italic text-gray-300">$1</em>')
    // --- 分割线
    .replace(/^---$/gm, '<hr class="border-white/10 my-2">')
    // > 引用块
    .replace(/^&gt;\s?(.*)$/gm, '<div class="border-l-2 border-cyan-500/40 pl-2.5 my-0.5 text-gray-400 text-[12px]">$1</div>')
    // [text](url) — 移除 javascript: 链接（安全）
    .replace(/\[([^\]]+)\]\(javascript:[^)]*\)/g, '<span class="text-cyan-400">$1</span>')
    // [text](url) — 正常链接
    .replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank" class="text-cyan-400 underline hover:text-cyan-300">$1</a>')
    // 换行
    .replace(/\n/g, '<br>')

  return html
}

const inputText = ref('')
const loading = ref(false)
const loadingText = ref('思考中...')
const currentAudio = ref(null)
const audioProgress = ref(0)
const audioCurrentTime = ref(0)
const audioDuration = ref(0)
const chatContainer = ref(null)
const audioPlayer = ref(null)
const inputEl = ref(null)
const inputBar = ref(null)
const activeScene = ref(null)
const inputBarHeight = ref(56)
let loadingTimer = null
const loadingStageIdx = ref(0)
const loadingSeconds = ref(0)
const isListening = ref(false)
let speechRecognition = null

// ═══════════════════════════════════════════════════════
// Welcome message with action buttons
// ═══════════════════════════════════════════════════════

const WELCOME_ACTIONS = [
  { label: '写台本', id: 'script', icon: 'fas fa-pen-nib',
    style: 'bg-cyan-500/10 border-cyan-500/20 text-cyan-300 active:bg-cyan-500/20' },
  { label: '合成语音', id: 'synth', icon: 'fas fa-microphone',
    style: 'bg-blue-500/10 border-blue-500/20 text-blue-300 active:bg-blue-500/20' }
]

function buildWelcomeMessage() {
  return {
    role: 'ai',
    text: '你好呀 👋 我是小声，你的 AI 声音制作人！\n\n告诉我一个场景（如"深夜电台"），我来帮你写词 + 配音 🎙️',
    actions: [...WELCOME_ACTIONS]
  }
}

// Message list
const messages = ref([buildWelcomeMessage()])

const hasUserMessages = computed(() => messages.value.some(m => m.role === 'user'))

const inputPlaceholder = computed(() =>
  activeScene.value ? activeScene.value.placeholder : '描述你想制作的内容...'
)

// ═══════════════════════════════════════════════════════
// Scene definitions
// ═══════════════════════════════════════════════════════

const scenes = [
  {
    id: 'radio', icon: 'fas fa-headphones', iconBg: 'bg-cyan-500/20', title: '深夜电台', desc: '治愈独白·情感',
    borderClass: 'bg-gradient-to-br from-cyan-900/20 to-[#0a0a1a] border-cyan-500/15 active:border-cyan-500/40',
    placeholder: '描述你的电台主题...',
    quickTags: ['深夜雨天的独白', '一个人的咋啡馆', '给过去的自己写信', '城市夜归人的故事']
  },
  {
    id: 'blessing', icon: 'fas fa-envelope', iconBg: 'bg-pink-500/20', title: '情感祝福', desc: '生日·表白·问候',
    borderClass: 'bg-gradient-to-br from-pink-900/20 to-[#0a0a1a] border-pink-500/15 active:border-pink-500/40',
    placeholder: '想送给谁？什么场合？',
    quickTags: ['给女朋友的生日祝福', '送给妈妈的母亲节寄语', '毕业季给好友的话', '表白独白']
  },
  {
    id: 'video', icon: 'fas fa-video', iconBg: 'bg-orange-500/20', title: '视频配音', desc: '解说·旁白·Vlog',
    borderClass: 'bg-gradient-to-br from-orange-900/20 to-[#0a0a1a] border-orange-500/15 active:border-orange-500/40',
    placeholder: '描述视频内容和风格...',
    quickTags: ['美食探店解说', '旅行Vlog旁白', '知识科普讲解', '产品开笜评测']
  },
  {
    id: 'story', icon: 'fas fa-book', iconBg: 'bg-purple-500/20', title: '有声故事', desc: '睡前·童话·绘本',
    borderClass: 'bg-gradient-to-br from-purple-900/20 to-[#0a0a1a] border-purple-500/15 active:border-purple-500/40',
    placeholder: '想听什么故事？',
    quickTags: ['给孩子的睡前童话', '一个温暖的冬日故事', '森林里的小动物', '星空下的冒险']
  },
  {
    id: 'commerce', icon: 'fas fa-store', iconBg: 'bg-emerald-500/20', title: '带货口播', desc: '产品·直播话术',
    borderClass: 'bg-gradient-to-br from-emerald-900/20 to-[#0a0a1a] border-emerald-500/15 active:border-emerald-500/40',
    placeholder: '什么产品？卖点？',
    quickTags: ['护肤品种草文案', '美食零食推荐', '数码产品评测口播', '服装穿搭解说']
  },
  {
    id: 'free', icon: 'fas fa-pen', iconBg: 'bg-yellow-500/20', title: '自由创作', desc: '聊天·AI帮你想',
    borderClass: 'bg-gradient-to-br from-yellow-900/20 to-[#0a0a1a] border-yellow-500/15 active:border-yellow-500/40',
    placeholder: '告诉我你想做什么...',
    quickTags: ['有哪些音色可以选？', '帮我写段深情独白', '我有哪些作品？', '合成一段语音试试']
  }
]

// ═══════════════════════════════════════════════════════
// Action handlers
// ═══════════════════════════════════════════════════════

function handleAction(action) {
  const actionMap = {
    script:  { local: true, reply: '好的！你想写一段什么主题的台本？\n\n比如：\n• 深夜电台独白\n• 给朋友的生日祝福\n• 产品评测解说\n\n告诉我主题和风格，我马上帮你写' },
    synth:   { local: true, reply: '好的！请发一段文字给我，我帮你合成语音\n\n你可以：\n• 直接粘贴一段文字\n• 或者先点「写台本」让我帮你创作' },
    emotion: { local: true, reply: '好的！请发一段文字给我，我来分析它的情感方向\n\n分析结果包含：情感标签、推荐语气、适合的音色类型。' },
    works:   { agent: true, command: '请帮我查看我的创作作品列表' },
    change_voice: { agent: true, command: '请帮我用另一个音色重新合成刚才的内容，换一个风格不同的声音' },
    rewrite:      { agent: true, command: '请帮我重写刚才的台本，换一种风格，然后重新合成语音' }
  }

  const matched = actionMap[action.id]
  if (!matched) return

  if (matched.local) {
    messages.value.push({ role: 'ai', text: matched.reply })
    scrollToBottom()
  } else {
    sendMessage(matched.command)
  }
}

// ═══════════════════════════════════════════════════════
// Scene & navigation
// ═══════════════════════════════════════════════════════

function applyScene(scene) {
  activeScene.value = scene
  const intros = {
    radio:    '深夜电台模式\n\n我来帮你制作治愈系的电台内容。\n告诉我你的主题和心情，我会为你写词、选声音、录制成品。\n\n试试下方的快捷标签，或直接描述你想要的内容。',
    blessing: '情感祝福模式\n\n想送一份特别的声音礼物吗？\n告诉我你想送给谁、什么场合，我帮你写词+选声音+录制！',
    video:    '视频配音模式\n\n描述你的视频内容和风格，我来帮你写解说词并配音。',
    story:    '有声故事模式\n\n想听什么样的故事？给孩子的睡前故事还是温暖的治愈故事？告诉我，我来创作。',
    commerce: '带货口播模式\n\n告诉我你要推荐的产品和卖点，我帮你写口播文案并配上专业的声音！',
    free:     '自由创作模式\n\n随便告诉我你想做什么！查音色、写台本、分析情感、合成语音，都可以。'
  }
  messages.value = [{ role: 'ai', text: intros[scene.id] }]
  nextTick(() => updateInputBarHeight())
}

function resetScene() {
  activeScene.value = null
  messages.value = [buildWelcomeMessage()]
}

function goBack() {
  if (window.history.length > 1) router.back()
  else router.replace('/')
}

// ═══════════════════════════════════════════════════════
// Progressive loading text
// ═══════════════════════════════════════════════════════

// 意图感知管线 — 根据用户消息类型动态切换阶段

function detectIntent(text) {
  const t = text
  if (/查看|查询|列表|有哪些|我的|作品|多少|历史|音色/.test(t)) return 'query'
  if (/情感|分析|语气|感觉/.test(t)) return 'emotion'
  if (/合成|录音|录制|台本|配音|帮我读|帮我写|生成/.test(t) || text.length > 30) return 'synth'
  return 'general'
}

const PIPELINE_MAP = {
  query: {
    stages: [
      { label: '理解你的需求', threshold: 0 },
      { label: '查询数据库',   threshold: 2 },
      { label: '整理结果',     threshold: 5 },
    ],
    texts: ['正在理解你的需求...', '正在查询相关数据...', '整理结果中，马上好...'],
    tip: '通常需要 2–5 秒',
  },
  emotion: {
    stages: [
      { label: '理解你的需求', threshold: 0 },
      { label: 'AI 情感分析', threshold: 3 },
      { label: '生成分析报告', threshold: 8 },
    ],
    texts: ['正在理解你的需求...', 'AI 正在分析情感方向...', '生成分析报告中...'],
    tip: '通常需要 5–12 秒',
  },
  synth: {
    stages: [
      { label: '理解你的需求', threshold: 0  },
      { label: 'AI 创作台本', threshold: 4  },
      { label: '情感解析匹配', threshold: 12 },
      { label: '合成语音中',   threshold: 20 },
      { label: '上传音频文件', threshold: 38 },
    ],
    texts: [
      '正在理解你的需求...',
      'AI 正在创作台本...',
      '情感解析，匹配最佳音色...',
      '正在合成语音，请稍候...',
      '收尾处理，马上就好...',
    ],
    tip: '通常需要 20–40 秒',
  },
  general: {
    stages: [
      { label: '理解你的需求', threshold: 0 },
      { label: 'AI 处理中',   threshold: 5 },
      { label: '生成回复',    threshold: 12 },
    ],
    texts: ['正在理解你的需求...', 'AI 思考处理中...', '即将为你生成回复...'],
    tip: '通常需要 5–20 秒',
  },
}

const pipelineStages = ref(PIPELINE_MAP.general.stages)
const pipelineTip    = ref(PIPELINE_MAP.general.tip)

function startLoadingStages(userText = '') {
  const intent = detectIntent(userText)
  const plan   = PIPELINE_MAP[intent]

  pipelineStages.value  = plan.stages
  pipelineTip.value     = plan.tip
  loadingStageIdx.value = 0
  loadingSeconds.value  = 0
  loadingText.value     = plan.texts[0]

  loadingTimer = setInterval(() => {
    if (!loading.value) return
    loadingSeconds.value++
    const next = plan.stages.findLastIndex(s => s.threshold <= loadingSeconds.value)
    if (next > loadingStageIdx.value) {
      loadingStageIdx.value = next
      loadingText.value = plan.texts[next] || loadingText.value
    }
  }, 1000)
}

function stopLoadingStages() {
  if (loadingTimer) {
    clearInterval(loadingTimer)
    loadingTimer = null
  }
}

// ═══════════════════════════════════════════════════════
// 语音输入（Web Speech API，纯前端免费）
// ═══════════════════════════════════════════════════════

function toggleVoiceInput() {
  if (isListening.value) {
    // 停止录音
    speechRecognition?.stop()
    isListening.value = false
    return
  }

  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
  if (!SpeechRecognition) {
    toastStore.show('当前浏览器不支持语音输入，请使用 Chrome')
    return
  }

  speechRecognition = new SpeechRecognition()
  speechRecognition.lang = 'zh-CN'
  speechRecognition.interimResults = true
  speechRecognition.continuous = false

  speechRecognition.onstart = () => {
    isListening.value = true
  }

  speechRecognition.onresult = (event) => {
    let transcript = ''
    for (let i = 0; i < event.results.length; i++) {
      transcript += event.results[i][0].transcript
    }
    inputText.value = transcript
  }

  speechRecognition.onend = () => {
    isListening.value = false
    // 语音结束后自动发送
    if (inputText.value.trim()) {
      sendMessage(inputText.value)
    }
  }

  speechRecognition.onerror = (e) => {
    isListening.value = false
    if (e.error === 'not-allowed') {
      toastStore.show('请允许麦克风权限')
    }
  }

  speechRecognition.start()
}

// ═══════════════════════════════════════════════════════
// Send message (core logic)
// ═══════════════════════════════════════════════════════

async function sendMessage(text) {
  if (!text || !text.trim() || loading.value) return

  if (!authStore.isLoggedIn) {
    toastStore.show('请先登录')
    router.replace({ name: 'Login', query: { redirect: '/workshop' } })
    return
  }

  let userMsg = text.trim()
  inputText.value = ''
  if (inputEl.value) inputEl.value.style.height = 'auto'
  messages.value.push({ role: 'user', text: userMsg })
  await scrollToBottom()

  // Intercept numbered inputs — ONLY as first interaction (no prior user messages)
  const isFirstMessage = !messages.value.some((m, i) => m.role === 'user' && i < messages.value.length - 1)
  const numberMap = { '1': 'script', '2': 'synth', '3': 'emotion', '4': 'works' }
  if (isFirstMessage && numberMap[userMsg]) {
    handleAction({ id: numberMap[userMsg] })
    return
  }

  loading.value = true
  loadingText.value = 'AI 思考中...'
  loadingSeconds.value = 0
  loadingStageIdx.value = 0

  // 实时显示 AI 回复的 streaming message
  const streamMsg = { role: 'ai', text: '', streaming: true }
  messages.value.push(streamMsg)
  await scrollToBottom()

  // 计时器
  loadingTimer = setInterval(() => { loadingSeconds.value++ }, 1000)

  const SSE_BASE = import.meta.env.VITE_API_BASE_URL || '/api'
  const TOKEN_KEY = 'sr_token'
  const raw = localStorage.getItem(TOKEN_KEY) || ''
  const authHeader = raw ? (raw.startsWith('Bearer ') ? raw : `Bearer ${raw}`) : ''

  try {
    const resp = await fetch(`${SSE_BASE}/agent/chat-stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': authHeader
      },
      body: JSON.stringify({ message: userMsg, scene: activeScene.value?.title || '' })
    })

    if (!resp.ok) {
      // Fallback to sync endpoint
      throw new Error(`SSE failed: ${resp.status}`)
    }

    const reader = resp.body.getReader()
    const decoder = new TextDecoder()
    let fullReply = ''
    let doneReply = null

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      const chunk = decoder.decode(value, { stream: true })
      // Parse SSE: lines like "data:xxx" or "event:done\ndata:xxx"
      const lines = chunk.split('\n')
      for (const line of lines) {
        if (line.startsWith('event:done')) {
          // Next data line is the final cleaned reply
          continue
        }
        if (line.startsWith('data:')) {
          const data = line.substring(5)
          if (doneReply === null && !chunk.includes('event:done')) {
            // Streaming token
            fullReply += data
            streamMsg.text = fullReply
            loadingText.value = 'AI 正在回复...'
            await nextTick()
            scrollToBottom()
          } else {
            // Done event — this is the cleaned final reply
            doneReply = data
          }
        }
      }
    }

    // Use doneReply if available, otherwise use accumulated fullReply
    let reply = doneReply || fullReply || '抱歉，我没有理解你的意思。'
    reply = reply.replace(/<think>[\s\S]*?<\/think>/gi, '').trim()

    // Parse audio URL
    let audioUrl = null
    const m1 = reply.match(/音频地址[：:]\s*(https?:\/\/[^\s\n\])]+)/i)
    const m2 = reply.match(/\[.*?\]\((https?:\/\/[^\s)]*audio[^\s)]*)\)/i)
    const m3 = reply.match(/(https?:\/\/[^\s\n\])]*(?:audio|tts|\.mp3|\.wav)[^\s\n\])]*)/i)
    audioUrl = m1?.[1] || m2?.[1] || m3?.[1] || null

    // Pipeline steps
    const steps = []
    if (reply.includes('台本')) steps.push({ label: '台本创作', done: true, detail: '✓ 已生成' })
    if (reply.includes('情感')) steps.push({ label: '情感解析', done: true, detail: '✓ 已分析' })
    if (reply.includes('音色')) steps.push({ label: '智能选角', done: true, detail: '✓ 已匹配' })
    if (audioUrl) steps.push({ label: '语音合成', done: true, detail: '✓ 已完成' })

    // Clean reply
    let cleanReply = reply
    if (audioUrl) {
      cleanReply = cleanReply.replace(/\[.*?\]\(https?:\/\/[^\s)]*(?:audio|tts)[^\s)]*\)/gi, '').trim()
      cleanReply = cleanReply.replace(/音频地址[：:]\s*https?:\/\/[^\s\n]+/gi, '').trim()
      cleanReply = cleanReply.replace(/https?:\/\/[^\s\n]*(?:audio|tts|\.mp3|\.wav)[^\s\n]*/gi, '').trim()
      cleanReply = cleanReply.replace(/（合成完毕.*?）/g, '').trim()
    }
    cleanReply = cleanReply.replace(/（查询完毕.*?）/g, '').trim()
    cleanReply = cleanReply.replace(/（分析完毕.*?）/g, '').trim()
    cleanReply = cleanReply.replace(/（生成完毕.*?）/g, '').trim()
    cleanReply = cleanReply.replace(/\n{3,}/g, '\n\n').trim()

    // Post-synthesis quick actions
    const postActions = audioUrl ? [
      { label: '换个音色', id: 'change_voice', style: 'bg-cyan-500/10 border-cyan-500/20 text-cyan-300' },
      { label: '重写台本', id: 'rewrite', style: 'bg-purple-500/10 border-purple-500/20 text-purple-300' }
    ] : null

    // Update the streaming message in-place
    streamMsg.text = cleanReply
    streamMsg.audioUrl = audioUrl
    streamMsg.steps = steps.length > 0 ? steps : null
    streamMsg.actions = postActions
    streamMsg.streaming = false

    // Auto-play
    if (audioUrl) {
      await nextTick()
      autoPlayAudio(audioUrl)
    }

  } catch (err) {
    console.error('Agent SSE error, trying sync fallback:', err)
    // Remove the streaming message
    const idx = messages.value.indexOf(streamMsg)
    if (idx > -1) messages.value.splice(idx, 1)

    // Fallback to sync endpoint
    try {
      const res = await request.post('/agent/chat',
        { message: userMsg, scene: activeScene.value?.title || '' },
        { timeout: 120000 }
      )
      if (res) {
        let reply = res.reply || '抱歉，我没有理解你的意思。'
        reply = reply.replace(/<think>[\s\S]*?<\/think>/gi, '').trim()
        let audioUrl = null
        const m1 = reply.match(/音频地址[：:]\s*(https?:\/\/[^\s\n\])]+)/i)
        const m2 = reply.match(/\[.*?\]\((https?:\/\/[^\s)]*audio[^\s)]*)\)/i)
        const m3 = reply.match(/(https?:\/\/[^\s\n\])]*(?:audio|tts|\.mp3|\.wav)[^\s\n\])]*)/i)
        audioUrl = m1?.[1] || m2?.[1] || m3?.[1] || null
        let cleanReply = reply
        if (audioUrl) {
          cleanReply = cleanReply.replace(/\[.*?\]\(https?:\/\/[^\s)]*(?:audio|tts)[^\s)]*\)/gi, '').trim()
          cleanReply = cleanReply.replace(/音频地址[：:]\s*https?:\/\/[^\s\n]+/gi, '').trim()
          cleanReply = cleanReply.replace(/https?:\/\/[^\s\n]*(?:audio|tts|\.mp3|\.wav)[^\s\n]*/gi, '').trim()
        }
        cleanReply = cleanReply.replace(/\n{3,}/g, '\n\n').trim()
        messages.value.push({
          role: 'ai', text: cleanReply, audioUrl,
          actions: audioUrl ? [
            { label: '换个音色', id: 'change_voice', style: 'bg-cyan-500/10 border-cyan-500/20 text-cyan-300' },
            { label: '重写台本', id: 'rewrite', style: 'bg-purple-500/10 border-purple-500/20 text-purple-300' }
          ] : null
        })
        if (audioUrl) { await nextTick(); autoPlayAudio(audioUrl) }
      } else {
        messages.value.push({ role: 'ai', text: '请求失败，请稍后重试' })
      }
    } catch (e2) {
      messages.value.push({
        role: 'ai',
        text: '网络或服务异常，请稍后重试\n' + (e2.response?.data?.message || e2.message || '')
      })
    }
  } finally {
    loading.value = false
    stopLoadingStages()
    saveChat()
    await scrollToBottom()
  }
}

// ═══════════════════════════════════════════════════════
// ★ Regenerate
// ═══════════════════════════════════════════════════════

async function regenerate(aiMsgIdx) {
  // Find the user message right before this AI message
  let userMsgIdx = aiMsgIdx - 1
  while (userMsgIdx >= 0 && messages.value[userMsgIdx].role !== 'user') {
    userMsgIdx--
  }
  if (userMsgIdx < 0) return

  const originalUserMsg = messages.value[userMsgIdx].text
  // Remove the AI response we're regenerating
  messages.value.splice(aiMsgIdx, 1)
  await scrollToBottom()

  // Re-send
  loading.value = true
  startLoadingStages()

  try {
    const res = await request.post('/agent/chat',
      { message: originalUserMsg, scene: activeScene.value?.title || '' },
      { timeout: 120000 }
    )

    if (res) {
      let reply = res.reply || '抱歉，我没有理解你的意思。'
      reply = reply.replace(/<think>[\s\S]*?<\/think>/gi, '').trim()

      let audioUrl = null
      const m1 = reply.match(/音频地址[：:]\s*(https?:\/\/[^\s\n\])]+)/i)
      const m2 = reply.match(/\[.*?\]\((https?:\/\/[^\s)]*audio[^\s)]*)\)/i)
      const m3 = reply.match(/(https?:\/\/[^\s\n\])]*(?:audio|tts|\.mp3|\.wav)[^\s\n\])]*)/i)
      audioUrl = m1?.[1] || m2?.[1] || m3?.[1] || null

      const steps = []
      if (reply.includes('台本')) steps.push({ label: '台本创作', done: true, detail: '✓ 已生成' })
      if (reply.includes('情感')) steps.push({ label: '情感解析', done: true, detail: '✓ 已分析' })
      if (reply.includes('音色')) steps.push({ label: '智能选角', done: true, detail: '✓ 已匹配' })
      if (audioUrl) steps.push({ label: '语音合成', done: true, detail: '✓ 已完成' })

      let cleanReply = reply
      if (audioUrl) {
        cleanReply = cleanReply.replace(/\[.*?\]\(https?:\/\/[^\s)]*(?:audio|tts)[^\s)]*\)/gi, '').trim()
        cleanReply = cleanReply.replace(/音频地址[：:]\s*https?:\/\/[^\s\n]+/gi, '').trim()
        cleanReply = cleanReply.replace(/https?:\/\/[^\s\n]*(?:audio|tts|\.mp3|\.wav)[^\s\n]*/gi, '').trim()
        cleanReply = cleanReply.replace(/（合成完毕.*?）/g, '').trim()
      }
      cleanReply = cleanReply.replace(/（查询完毕.*?）/g, '').replace(/（分析完毕.*?）/g, '').replace(/（生成完毕.*?）/g, '').trim()
      cleanReply = cleanReply.replace(/\n{3,}/g, '\n\n').trim()

      const postActions = audioUrl ? [
        { label: '换个音色', id: 'change_voice', icon: 'fas fa-redo', style: 'bg-cyan-500/10 border-cyan-500/20 text-cyan-300' },
        { label: '重写台本', id: 'rewrite', icon: 'fas fa-pen', style: 'bg-purple-500/10 border-purple-500/20 text-purple-300' }
      ] : null

      messages.value.push({
        role: 'ai', text: cleanReply, audioUrl,
        steps: steps.length > 0 ? steps : null,
        actions: postActions
      })

      if (audioUrl) {
        await nextTick()
        autoPlayAudio(audioUrl)
      }
    }
  } catch (err) {
    messages.value.push({ role: 'ai', text: '重新生成失败，请稍后重试' })
  } finally {
    loading.value = false
    stopLoadingStages()
    saveChat()
    await scrollToBottom()
  }
}

// ═══════════════════════════════════════════════════════
// ★ Enhanced Audio Player
// ═══════════════════════════════════════════════════════

function playAudio(url) {
  if (currentAudio.value === url) {
    audioPlayer.value.pause()
    currentAudio.value = null
    return
  }
  currentAudio.value = url
  audioProgress.value = 0
  audioCurrentTime.value = 0
  audioDuration.value = 0
  audioPlayer.value.src = url
  audioPlayer.value.play().catch(() => { /* user interaction required */ })
}

// ★ Auto-play with preload — start loading audio in background
function autoPlayAudio(url) {
  currentAudio.value = url
  audioProgress.value = 0
  audioCurrentTime.value = 0
  audioDuration.value = 0
  // Preload via hidden Audio element first
  const preloader = new Audio(url)
  preloader.addEventListener('canplaythrough', () => {
    // Audio is fully buffered, now play via the real player
    if (audioPlayer.value && currentAudio.value === url) {
      audioPlayer.value.src = url
      audioPlayer.value.play().catch(() => {})
    }
  }, { once: true })
  // Fallback: if preload takes too long, play anyway after 2s
  setTimeout(() => {
    if (audioPlayer.value && currentAudio.value === url && audioPlayer.value.src !== url) {
      audioPlayer.value.src = url
      audioPlayer.value.play().catch(() => {})
    }
  }, 2000)
}

function onAudioEnded() {
  currentAudio.value = null
  audioProgress.value = 0
  audioCurrentTime.value = 0
}

function onTimeUpdate() {
  if (audioPlayer.value) {
    audioCurrentTime.value = audioPlayer.value.currentTime
    if (audioDuration.value > 0) {
      audioProgress.value = (audioPlayer.value.currentTime / audioDuration.value) * 100
    }
  }
}

function onMetadataLoaded() {
  if (audioPlayer.value && isFinite(audioPlayer.value.duration)) {
    audioDuration.value = audioPlayer.value.duration
  }
}

function seekAudio(event, url) {
  if (currentAudio.value !== url || !audioDuration.value) return
  const rect = event.currentTarget.getBoundingClientRect()
  const percent = (event.clientX - rect.left) / rect.width
  audioPlayer.value.currentTime = percent * audioDuration.value
}

function formatTime(seconds) {
  if (!seconds || !isFinite(seconds)) return '0:00'
  const m = Math.floor(seconds / 60)
  const s = Math.floor(seconds % 60)
  return `${m}:${s.toString().padStart(2, '0')}`
}

// ═══════════════════════════════════════════════════════
// Chat persistence (localStorage)
// ═══════════════════════════════════════════════════════

const CHAT_STORAGE_KEY = 'ai_workshop_chat'

function saveChat() {
  try {
    const data = {
      messages: messages.value,
      scene: activeScene.value?.id || null
    }
    localStorage.setItem(CHAT_STORAGE_KEY, JSON.stringify(data))
  } catch (e) { /* ignore */ }
}

function restoreChat() {
  try {
    const raw = localStorage.getItem(CHAT_STORAGE_KEY)
    if (!raw) return
    const data = JSON.parse(raw)
    if (data.messages && data.messages.length > 0) {
      messages.value = data.messages
      if (data.scene) {
        activeScene.value = scenes.find(s => s.id === data.scene) || null
      }
    }
  } catch (e) { /* ignore */ }
}

// ═══════════════════════════════════════════════════════
// Clear chat
// ═══════════════════════════════════════════════════════

async function clearChat() {
  activeScene.value = null
  messages.value = [buildWelcomeMessage()]
  localStorage.removeItem(CHAT_STORAGE_KEY)
  try { await request.post('/agent/reset') } catch (e) { /* ignore */ }
}

// ═══════════════════════════════════════════════════════
// Utilities
// ═══════════════════════════════════════════════════════

function autoResize() {
  const el = inputEl.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 80) + 'px'
}

function updateInputBarHeight() {
  if (inputBar.value) inputBarHeight.value = inputBar.value.offsetHeight
}

async function scrollToBottom() {
  await nextTick()
  if (chatContainer.value) {
    chatContainer.value.parentElement.scrollTop = chatContainer.value.parentElement.scrollHeight
  }
}

// ═══════════════════════════════════════════════════════
// Lifecycle
// ═══════════════════════════════════════════════════════

onMounted(() => {
  // 每次进入工坊从空白开始（不恢复历史对话）
  updateInputBarHeight()
  window.addEventListener('resize', updateInputBarHeight)

  // 从首页场景入口跳转过来时，自动发送预设文本
  const initText = sessionStorage.getItem('workshop_init_text')
  if (initText) {
    sessionStorage.removeItem('workshop_init_text')
    nextTick(() => {
      inputText.value = initText
      sendMessage(initText)
    })
  }
})

onBeforeUnmount(() => {
  stopLoadingStages()
  window.removeEventListener('resize', updateInputBarHeight)
})
</script>

<style scoped>
/* 打字思考动画 */
.typing-dots::after {
  content: '...';
  animation: tdots 1.5s steps(4, end) infinite;
}
@keyframes tdots {
  0%   { content: ''; }
  25%  { content: '.'; }
  50%  { content: '..'; }
  75%  { content: '...'; }
}

.hide-scrollbar { -ms-overflow-style: none; scrollbar-width: none; }
.hide-scrollbar::-webkit-scrollbar { display: none; }
</style>
