<template>
  <div class="h-full bg-[#050505] flex flex-col relative hide-scrollbar">
    
    <!-- 顶栏 -->
    <div class="h-14 flex justify-between items-center px-4 border-b border-white/5 mt-8">
      <button @click="handleBack" class="text-white w-8 h-8 flex justify-center items-center cursor-pointer"><i class="fas fa-chevron-left"></i></button>
      <h1 class="text-white font-bold text-base"><i class="fas fa-podcast text-cyan-400"></i> AI 双播</h1>
      <div class="w-8"></div>
    </div>

    <div class="flex-1 overflow-y-auto px-4 pt-4 pb-28 hide-scrollbar">
      
      <!-- 内容来源切换 -->
      <h2 class="text-white text-sm font-bold mb-3 flex items-center gap-2">
        <i class="fas fa-pen-nib text-cyan-400 text-xs"></i> 内容来源
      </h2>
      <div class="flex p-1 bg-black border border-white/10 rounded-xl mb-4">
        <button 
          v-for="tab in sourceTabs" :key="tab.key"
          @click="sourceType = tab.key"
          :disabled="isGenerating || generationDone"
          class="flex-1 py-2 text-xs font-bold rounded-lg transition-all cursor-pointer disabled:cursor-not-allowed disabled:opacity-50"
          :class="sourceType === tab.key ? 'bg-white/10 text-white' : 'text-gray-500 hover:text-gray-300'"
        >{{ tab.icon }} {{ tab.label }}</button>
      </div>

      <!-- 输入区域 -->
      <div class="rounded-2xl p-4 mb-5 bg-gradient-to-br from-[#0a0f1a] to-[#0f0a18] border border-white/5 transition-all duration-300"
           :class="{ 'border-cyan-500/30 shadow-[0_0_20px_rgba(6,182,212,0.08)]': sourceContent }">
        <textarea 
          v-model="sourceContent"
          :disabled="isGenerating || generationDone"
          class="w-full bg-transparent text-white outline-none resize-none text-[14px] placeholder-gray-600 leading-relaxed disabled:opacity-60 disabled:cursor-not-allowed" 
          rows="4" 
          :placeholder="currentPlaceholder"
        ></textarea>
        <div class="flex justify-between items-center mt-2 pt-2 border-t border-white/5">
          <span class="text-[10px] text-gray-600">
            <i class="fas fa-info-circle"></i>
            {{ sourceType === 'link' ? '支持公众号、知乎等链接' : sourceType === 'text' ? '建议 500~5000 字效果最佳' : '简短描述即可，AI 会自动扩展' }}
          </span>
          <span class="text-[10px] text-gray-600">{{ sourceContent.length }} 字</span>
        </div>
      </div>

      <!-- 主播阵容 -->
      <h2 class="text-white text-sm font-bold mb-3 flex items-center gap-2">
        <i class="fas fa-users text-pink-400 text-xs"></i> 主播阵容
      </h2>
      <div class="flex gap-3 overflow-x-auto pb-2 mb-5 hide-scrollbar">
        <div 
          v-for="preset in presets" 
          :key="preset.id"
          @click="!isGenerating && !generationDone && (selectedPreset = preset.id)"
          class="flex-shrink-0 w-[165px] rounded-2xl p-3.5 transition-all border-2 relative overflow-hidden"
          :class="[
            selectedPreset === preset.id 
              ? 'border-cyan-400/60 bg-gradient-to-br from-cyan-500/10 to-purple-500/5' 
              : 'border-white/5 bg-white/[0.02] hover:border-white/15',
            (isGenerating || generationDone) ? 'cursor-not-allowed opacity-70' : 'cursor-pointer'
          ]"
        >
          <!-- 播报时动态背景 -->
          <div v-if="isGenerating && selectedPreset === preset.id" 
               class="absolute inset-0 bg-gradient-to-t from-cyan-500/15 to-transparent animate-pulse z-0"></div>
          
          <!-- 头像 -->
          <div class="flex items-center justify-center -space-x-2 mb-2.5 relative z-10">
            <div class="w-10 h-10 rounded-full border-2 border-[#121212] flex justify-center items-center text-white text-xs font-bold transition-all" 
                 :class="[preset.colorA, activeSpeaker === preset.voiceA && isGenerating && selectedPreset === preset.id ? 'scale-110 shadow-[0_0_15px_rgba(6,182,212,0.6)] ring-2 ring-cyan-400/50 z-20' : 'z-10']">{{ preset.charA }}</div>
            <div class="w-10 h-10 rounded-full border-2 border-[#121212] flex justify-center items-center text-white text-xs font-bold transition-all" 
                 :class="[preset.colorB, activeSpeaker === preset.voiceB && isGenerating && selectedPreset === preset.id ? 'scale-110 shadow-[0_0_15px_rgba(168,85,247,0.6)] ring-2 ring-purple-400/50 z-20' : '']">{{ preset.charB }}</div>
          </div>
          <h3 class="text-white text-xs font-bold text-center">{{ preset.name }}</h3>
          <p class="text-[9px] text-gray-400 text-center mt-0.5">{{ preset.desc }}</p>
          
          <!-- 选中标记 -->
          <div v-if="selectedPreset === preset.id" class="absolute top-2 right-2">
            <i class="fas fa-check-circle text-cyan-400 text-xs"></i>
          </div>
        </div>
      </div>

      <!-- 音频选项 -->
      <h2 class="text-white text-sm font-bold mb-3 flex items-center gap-2">
        <i class="fas fa-sliders-h text-amber-400 text-xs"></i> 音频选项
      </h2>
      <div class="rounded-2xl p-4 bg-white/[0.02] border border-white/5 flex flex-col gap-3 mb-5"
           :class="{ 'opacity-60 pointer-events-none': isGenerating || generationDone }">
        <div class="flex justify-between items-center cursor-pointer" @click="withHeadMusic = !withHeadMusic">
          <div class="flex items-center gap-2">
            <i class="fas fa-music text-amber-400 text-xs"></i>
            <span class="text-white text-sm select-none">片头音效</span>
          </div>
          <div class="w-10 h-5 rounded-full transition-colors flex items-center px-0.5" 
               :class="withHeadMusic ? 'bg-cyan-500' : 'bg-gray-700'">
            <div class="w-4 h-4 bg-white rounded-full shadow transition-transform" 
                 :class="withHeadMusic ? 'translate-x-5' : 'translate-x-0'"></div>
          </div>
        </div>
        <div class="flex justify-between items-center cursor-pointer" @click="withTailMusic = !withTailMusic">
          <div class="flex items-center gap-2">
            <i class="fas fa-flag-checkered text-amber-400 text-xs"></i>
            <span class="text-white text-sm select-none">片尾音效</span>
          </div>
          <div class="w-10 h-5 rounded-full transition-colors flex items-center px-0.5" 
               :class="withTailMusic ? 'bg-cyan-500' : 'bg-gray-700'">
            <div class="w-4 h-4 bg-white rounded-full shadow transition-transform" 
                 :class="withTailMusic ? 'translate-x-5' : 'translate-x-0'"></div>
          </div>
        </div>
      </div>

      <!-- ═══ Token 消耗提示（生成前） ═══ -->
      <div v-if="!isGenerating && !generationDone && sourceContent.trim()" 
           class="mb-4 flex items-center gap-2 px-3 py-2 rounded-xl bg-amber-500/5 border border-amber-500/10">
        <i class="fas fa-coins text-amber-400 text-[10px]"></i>
        <span class="text-[10px] text-amber-400/80">双播生成将消耗较多 Token，请确认内容后再生成</span>
      </div>

      <!-- 实时对话流 -->
      <div v-if="dialogueRounds.length > 0">
        <h2 class="text-white text-sm font-bold mb-3 flex items-center gap-2">
          <i class="fas fa-comment-dots text-green-400 text-xs"></i> 实时对话
          <span v-if="isGenerating" class="text-[10px] text-cyan-400 font-normal ml-auto">
            <i class="fas fa-circle-notch fa-spin mr-1"></i>第 {{ dialogueRounds.length }} 轮
          </span>
        </h2>
        <div class="rounded-2xl p-4 bg-white/[0.02] border border-white/5 space-y-3 max-h-[300px] overflow-y-auto hide-scrollbar">
          <div v-for="round in dialogueRounds" :key="round.roundId"
               class="flex gap-2 items-start animate-[fadeIn_0.3s_ease-out]">
            <div class="w-6 h-6 rounded-full flex-shrink-0 flex items-center justify-center text-[9px] font-bold mt-0.5"
                 :class="round.isA ? 'bg-gradient-to-br from-cyan-500 to-blue-500 text-white' : 'bg-gradient-to-br from-purple-500 to-pink-500 text-white'">
              {{ round.speakerChar }}
            </div>
            <div class="flex-1">
              <p class="text-[10px] text-gray-500 mb-0.5">{{ round.speakerName }}</p>
              <p class="text-xs text-gray-300 leading-relaxed">{{ round.text || '...' }}</p>
            </div>
          </div>
          <!-- 正在生成的加载指示器 -->
          <div v-if="isGenerating" class="flex items-center gap-2 text-gray-500 text-xs">
            <i class="fas fa-circle-notch fa-spin text-cyan-400"></i>
            <span>AI 正在构思对话...</span>
          </div>
        </div>
      </div>

      <!-- ═══ 生成完成 → 发布卡片 ═══ -->
      <div v-if="generationDone && !isGenerating && dialogueRounds.length > 0" class="mt-5 space-y-3">
        <div class="bg-gradient-to-br from-green-500/10 via-emerald-500/5 to-cyan-500/10 border border-green-500/20 rounded-2xl p-5 relative overflow-hidden">
          <div class="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-green-500/60 via-cyan-400/40 to-blue-500/60"></div>
          <div class="text-center mb-4">
            <div class="text-3xl mb-2">🎙️</div>
            <h3 class="text-base font-black text-white mb-1">播客生成完成！</h3>
            <p class="text-[10px] text-gray-400">共 {{ dialogueRounds.length }} 轮对话 · 发布到发现页让更多人听到</p>
          </div>

          <!-- 标题输入 -->
          <div class="mb-3">
            <label class="text-[10px] text-gray-400 mb-1.5 block">📝 播客标题</label>
            <input v-model="publishTitle" type="text"
                   :placeholder="sourceContent.substring(0, 30) + '...'"
                   class="w-full bg-black/30 border border-white/10 rounded-xl px-3.5 py-2.5 text-sm text-white outline-none focus:border-green-500/50 transition-all" />
          </div>

          <!-- 发布按钮 -->
          <button @click="publishPodcast" :disabled="publishing || published"
                  class="w-full py-3.5 rounded-xl text-sm font-bold cursor-pointer transition-all flex items-center justify-center gap-2 disabled:opacity-40 disabled:cursor-not-allowed active:scale-[0.98]"
                  :class="publishing ? 'bg-gray-700 text-gray-400' : published ? 'bg-green-800/30 text-green-500' : 'bg-gradient-to-r from-green-500 to-emerald-500 text-white hover:shadow-lg hover:shadow-green-500/20'">
            <i v-if="publishing" class="fas fa-circle-notch fa-spin"></i>
            <i v-else-if="published" class="fas fa-check-circle"></i>
            <i v-else class="fas fa-paper-plane"></i>
            {{ published ? '✅ 已发布' : (publishProgress || (publishing ? '发布中...' : '📤 保存并发布')) }}
          </button>

          <!-- 已发布提示 -->
          <div v-if="published" class="mt-3 flex items-center justify-center gap-2 py-2 rounded-xl bg-green-500/10 border border-green-500/20 text-green-400 text-xs font-bold">
            <i class="fas fa-check-circle"></i> 已发布，等待审核上线
          </div>
        </div>

        <!-- 锁定提示 -->
        <div class="flex items-center justify-center gap-2 py-2.5 rounded-xl bg-white/[0.02] border border-white/5 text-gray-500 text-[10px]">
          <i class="fas fa-lock text-[8px]"></i>
          生成已完成，如需创建新播客请返回后重新进入
        </div>
      </div>
    </div>

    <!-- 底部操作栏 -->
    <div class="absolute bottom-0 w-full p-4 bg-gradient-to-t from-black via-black/95 to-transparent pb-8">
      <!-- 生成中 → 中断按钮 -->
      <button v-if="isGenerating"
        @click="stopGeneration"
        class="w-full py-4 rounded-xl font-bold text-base transition-all cursor-pointer flex justify-center items-center gap-2 bg-gradient-to-r from-red-600 to-orange-600 text-white active:scale-[0.97] shadow-[0_4px_20px_rgba(239,68,68,0.25)]"
      >
        <i class="fas fa-stop-circle"></i>
        中断生成 ({{ dialogueRounds.length }} 轮)
      </button>
      <!-- 未生成 → 生成按钮 -->
      <button v-else-if="!generationDone"
        @click="generatePodcast"
        :disabled="!sourceContent.trim()"
        class="w-full py-4 rounded-xl font-bold text-base shadow-[0_4px_20px_rgba(6,182,212,0.25)] transition-all cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed flex justify-center items-center gap-2"
        :class="sourceContent.trim()
          ? 'bg-gradient-to-r from-cyan-500 to-blue-500 text-white active:scale-[0.97]' 
          : 'bg-gray-800 text-gray-500'"
      >
        <i class="fas fa-podcast"></i>
        生成 AI 双播
      </button>
      <!-- 已生成 → 锁定/返回 -->
      <button v-else
        @click="handleBack"
        class="w-full py-4 rounded-xl font-bold text-base transition-all cursor-pointer flex justify-center items-center gap-2 bg-white/[0.05] border border-white/10 text-gray-400 hover:text-white hover:bg-white/[0.08] active:scale-[0.97]"
      >
        <i class="fas fa-arrow-left"></i>
        返回工作台
      </button>
    </div>

    <!-- ═══ 退出确认弹窗 ═══ -->
    <teleport to="body">
      <div v-if="showExitModal" class="fixed inset-0 z-[9999] flex items-center justify-center bg-black/70 backdrop-blur-sm">
        <div class="bg-[#1a1a1c] rounded-2xl p-6 w-[85%] max-w-[340px] border border-white/10 shadow-2xl">
          <div class="text-center mb-4">
            <div class="text-3xl mb-2">⚠️</div>
            <h3 class="text-white font-bold text-base mb-1">确认退出？</h3>
            <p class="text-gray-400 text-xs leading-relaxed">
              {{ isGenerating 
                ? '正在生成播客中，退出将中断生成流程。已消耗的 Token 不可恢复。' 
                : '播客已生成但未发布，退出后需重新生成（消耗 Token）。' }}
            </p>
          </div>
          <div class="flex gap-3">
            <button @click="showExitModal = false"
                    class="flex-1 py-3 rounded-xl text-sm font-bold bg-white/10 text-white cursor-pointer hover:bg-white/15 transition-all">
              继续留下
            </button>
            <button @click="confirmExit"
                    class="flex-1 py-3 rounded-xl text-sm font-bold bg-red-500/20 text-red-400 cursor-pointer hover:bg-red-500/30 transition-all">
              {{ isGenerating ? '中断并退出' : '确认退出' }}
            </button>
          </div>
        </div>
      </div>
    </teleport>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, onBeforeUnmount } from 'vue'
import { useRouter, onBeforeRouteLeave } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { usePlayerStore } from '../stores/player'
import { useWebSocket } from '../composables/useWebSocket'
import { useToastStore } from '../stores/toast'
import { studioApi } from '../api/studio'

const router = useRouter()
const toastStore = useToastStore()
const authStore = useAuthStore()
const playerStore = usePlayerStore()

// ==================== 表单状态 ====================

const sourceType = ref('topic')
const sourceContent = ref('')
const selectedPreset = ref(1)
const withHeadMusic = ref(true)
const withTailMusic = ref(false)

const sourceTabs = [
  { key: 'topic', icon: '💡', label: '输入主题' },
  { key: 'link', icon: '🔗', label: '提取链接' },
  { key: 'text', icon: '📝', label: '原文直读' },
]

const currentPlaceholder = computed(() => {
  switch (sourceType.value) {
    case 'topic': return '输入你想让主播聊的主题\n例如：年轻人为什么要存钱、AI 时代如何学习...'
    case 'link': return '粘贴文章链接，AI 自动总结成播客\n支持公众号、知乎、头条等主流平台...'
    case 'text': return '输入准备好的播客原稿\nAI 将智能分配角色进行对话式朗读...'
    default: return ''
  }
})

// ==================== 预设音色 ====================

const presets = ref([
  {
    id: 1,
    name: '黑猫侦探社',
    desc: '咪仔 & 大一先生',
    charA: '咪',
    charB: '大',
    colorA: 'bg-pink-500',
    colorB: 'bg-blue-600',
    voiceA: 'zh_female_mizaitongxue_v2_saturn_bigtts',
    voiceB: 'zh_male_dayixiansheng_v2_saturn_bigtts',
  },
  {
    id: 2,
    name: '科技脱口秀',
    desc: '刘飞 & 潇磊',
    charA: '飞',
    charB: '磊',
    colorA: 'bg-amber-500',
    colorB: 'bg-slate-600',
    voiceA: 'zh_male_liufei_v2_saturn_bigtts',
    voiceB: 'zh_male_xiaolei_v2_saturn_bigtts',
  },
])

// ==================== 生成状态 ====================

const isGenerating = ref(false)
const activeSpeaker = ref('')
const dialogueRounds = ref([])
const generationDone = ref(false)

// 发布相关状态
const publishTitle = ref('')
const publishing = ref(false)
const publishProgress = ref('')
const published = ref(false)

// 退出确认弹窗
const showExitModal = ref(false)
let pendingNavigation = null

// 音频块收集器 (每轮次收集后合成播放)
let currentRoundAudioChunks = []
// 全部轮次的完整音频块（用于最终合并上传）
let allAudioChunks = []

const getActivePreset = () => presets.value.find(p => p.id === selectedPreset.value) || presets.value[0]

const getSpeakerInfo = (speakerVoiceId) => {
  const preset = getActivePreset()
  if (speakerVoiceId === preset.voiceA || speakerVoiceId?.includes('female') || speakerVoiceId?.includes('mizai')) {
    return { name: preset.desc.split(' & ')[0] || '主播A', char: preset.charA, isA: true }
  }
  return { name: preset.desc.split(' & ')[1] || '主播B', char: preset.charB, isA: false }
}

// ==================== WebSocket ====================

const podcastWs = useWebSocket('/ws/podcast', {
  onOpen: () => {
    isGenerating.value = true
    generationDone.value = false
    published.value = false
    dialogueRounds.value = []
    currentRoundAudioChunks = []
    allAudioChunks = []

    const preset = getActivePreset()
    
    podcastWs.send({
      action: 'generate',
      text: sourceContent.value,
      sourceType: sourceType.value,
      voiceA: preset.voiceA,
      voiceB: preset.voiceB,
      headMusic: withHeadMusic.value,
      tailMusic: withTailMusic.value,
    })
  },
  onMessage: (event) => {
    if (typeof event.data === 'string') {
      try {
        const msg = JSON.parse(event.data)

        if (msg.event === 'round_start') {
          // 新轮次开始
          const info = getSpeakerInfo(msg.speaker)
          activeSpeaker.value = msg.speaker
          currentRoundAudioChunks = []

          // 跳过 head/tail music 的文本展示
          if (msg.roundId >= 0 && msg.roundId < 9999) {
            dialogueRounds.value.push({
              roundId: msg.roundId,
              speaker: msg.speaker,
              speakerName: info.name,
              speakerChar: info.char,
              isA: info.isA,
              text: msg.text || '',
            })
          }
        } else if (msg.event === 'round_end') {
          activeSpeaker.value = ''
          // 将本轮收集的音频合并播放
          if (currentRoundAudioChunks.length > 0) {
            const mergedBlob = new Blob(currentRoundAudioChunks, { type: 'audio/mpeg' })
            // 保存到全局列表（用于发布时合并上传）
            allAudioChunks.push(...currentRoundAudioChunks)
            const url = URL.createObjectURL(mergedBlob)
            const roundInfo = dialogueRounds.value.find(r => r.roundId === msg.roundId)
            if (dialogueRounds.value.length <= 1 || msg.roundId < 0) {
              // 第一轮或音效 → play
              playerStore.play({ title: 'AI 双播', author: roundInfo?.speakerName || '音效', url })
            } else {
              // 后续轮次 → enqueue
              playerStore.enqueue(url)
            }
            currentRoundAudioChunks = []
          }
        } else if (msg.event === 'complete') {
          isGenerating.value = false
          generationDone.value = true
          publishTitle.value = sourceContent.value.length > 30 
            ? sourceContent.value.substring(0, 30) + '...' 
            : sourceContent.value
          toastStore.show('🎙️ 播客生成完成！可发布到发现页')
          podcastWs.close()
        } else if (msg.event === 'error') {
          toastStore.show('生成出错: ' + msg.message)
          isGenerating.value = false
          podcastWs.close()
        }
      } catch (e) {
        console.error('解析播客消息失败', e)
      }
    } else {
      // 收到二进制音频片段 → 收集到当前轮次
      currentRoundAudioChunks.push(event.data)
    }
  },
  onClose: () => {
    isGenerating.value = false
    activeSpeaker.value = ''
  },
})

// ==================== 生成操作 ====================

const generatePodcast = () => {
  if (!authStore.isLoggedIn) {
    toastStore.show('请先登录 ✨')
    return router.replace({ name: 'Login', query: { redirect: '/podcast' } })
  }
  if (!authStore.hasFeature('ai_podcast')) {
    toastStore.show('该功能需要 VIP 权限 👑')
    return router.push('/vip')
  }
  if (!sourceContent.value.trim()) {
    return toastStore.show('请输入内容')
  }
  // 生成完成后，锁定不允许重新生成
  if (generationDone.value) {
    toastStore.show('已生成完成，请发布或返回后新建')
    return
  }
  
  podcastWs.connect()
}

// ==================== 中断生成 ====================

const stopGeneration = () => {
  if (!isGenerating.value) return
  podcastWs.close()
  isGenerating.value = false
  activeSpeaker.value = ''
  // 停止音频播放 + 清空队列
  playerStore.close()
  
  // 如果已有一些对话轮次和音频，标记为完成（可发布已生成的部分）
  if (dialogueRounds.value.length > 0 && allAudioChunks.length > 0) {
    generationDone.value = true
    publishTitle.value = sourceContent.value.length > 30 
      ? sourceContent.value.substring(0, 30) + '...' 
      : sourceContent.value
    toastStore.show(`⏹️ 已中断，已生成 ${dialogueRounds.value.length} 轮对话，可保存发布`)
  } else {
    toastStore.show('⏹️ 已中断生成')
  }
}

// ==================== 退出拦截 ====================

/** 统一处理返回操作 */
const handleBack = () => {
  // 场景1: 生成中 → 强警告
  if (isGenerating.value) {
    showExitModal.value = true
    return
  }
  // 场景2: 已生成但未发布 → 中等警告
  if (generationDone.value && !published.value) {
    showExitModal.value = true
    return
  }
  // 场景3: 已发布 / 未生成 → 直接返回
  router.back()
}

/** 确认退出 */
const confirmExit = () => {
  showExitModal.value = false
  // 中断 WebSocket + 停止播放
  if (isGenerating.value) {
    podcastWs.close()
    isGenerating.value = false
  }
  playerStore.close()
  // 执行导航
  if (pendingNavigation) {
    pendingNavigation()
    pendingNavigation = null
  } else {
    router.back()
  }
}

/** Vue Router 路由守卫：拦截所有导航（浏览器回退/路由跳转） */
onBeforeRouteLeave((to, from, next) => {
  // 生成中或已生成未发布 → 拦截
  if (isGenerating.value || (generationDone.value && !published.value)) {
    pendingNavigation = next
    showExitModal.value = true
    return false // 阻止导航
  }
  next()
})

/** 浏览器关闭/刷新拦截 */
const handleBeforeUnload = (e) => {
  if (isGenerating.value || (generationDone.value && !published.value)) {
    e.preventDefault()
    e.returnValue = '播客正在生成或未发布，确认离开？'
  }
}

onMounted(() => {
  window.addEventListener('beforeunload', handleBeforeUnload)
})

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload)
})

// ==================== 发布 ====================

const publishPodcast = async () => {
  if (publishing.value || published.value) return
  if (allAudioChunks.length === 0) {
    toastStore.show('没有可发布的音频')
    return
  }

  publishing.value = true
  publishProgress.value = '🔄 合并音频中...'

  try {
    // 1. 合并所有音频块为一个 Blob
    const mergedBlob = new Blob(allAudioChunks, { type: 'audio/mpeg' })
    
    // 2. 上传到 R2 持久化
    publishProgress.value = '☁️ 上传音频中...'
    const uploadRes = await studioApi.uploadAudio(mergedBlob, `podcast_${Date.now()}.mp3`)
    const audioUrl = uploadRes.audioUrl || uploadRes
    if (!audioUrl) throw new Error('上传失败：未返回音频地址')

    // 3. 创建 Studio 项目
    publishProgress.value = '📝 创建项目中...'
    const title = publishTitle.value.trim() || sourceContent.value.substring(0, 30)
    const project = await studioApi.createProject({
      typeCode: 'podcast',
      title: title,
      inspiration: sourceContent.value.substring(0, 200)
    })

    // 4. 保存段落（对话全文 + 音频地址）
    publishProgress.value = '💾 保存内容中...'
    const allText = dialogueRounds.value
      .map(r => `${r.speakerName}：${r.text}`)
      .join('\n\n')
    await studioApi.saveSection({
      projectId: project.id,
      sectionIndex: 0,
      title: title,
      content: allText,
      voiceId: getActivePreset().voiceA,
      audioUrl: audioUrl,
      status: 'synthesized'
    })

    // 5. 发布到发现页
    publishProgress.value = '📤 发布中...'
    await studioApi.publishProject(project.id)

    published.value = true
    toastStore.show('🎉 播客已发布到发现页！')
  } catch (e) {
    toastStore.show('发布失败: ' + (e.message || '网络错误'))
  } finally {
    publishing.value = false
    publishProgress.value = ''
  }
}

onUnmounted(() => {
  podcastWs.close()
})
</script>

<style scoped>
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
