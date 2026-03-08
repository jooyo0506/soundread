<template>
  <div class="h-full flex flex-col relative">
  <div class="h-14 flex justify-between items-center px-4 mt-8 z-40 bg-transparent">
    <button @click="$router.back()" class="w-8 h-8 flex justify-center items-center text-white cursor-pointer"><i class="fas fa-arrow-left text-sm"></i></button>
    <h1 class="text-white font-bold text-lg">文字配音</h1>
    <div class="w-8"></div>
  </div>

  <div class="flex-1 overflow-y-auto px-5 pt-2 pb-32 hide-scrollbar">

    <!-- 模式切换 -->
    <div class="flex justify-center mb-6">
      <div class="bg-black border border-white/10 rounded-full p-1 flex w-full max-w-[400px]">
        <button 
          @click="mode = 'short'"
          class="flex-1 py-1.5 rounded-full text-sm font-bold transition-all truncate px-1"
          :class="mode === 'short' ? 'glass-panel text-white shadow-md' : 'text-gray-500 hover:text-white'"
        >短合 <span class="text-[10px] opacity-70">300字</span></button>
        <button 
          @click="mode = 'ai'"
          class="flex-1 py-1.5 rounded-full text-sm font-bold transition-all flex justify-center items-center gap-1 truncate px-1"
          :class="mode === 'ai' ? 'glass-panel text-[#FF9500] shadow-md' : 'text-gray-500 hover:text-white'"
        >AI 编排 <i class="fas fa-wand-magic-sparkles text-[10px]"></i></button>
        <button 
          @click="$router.push('/drama')"
          class="flex-1 py-1.5 rounded-full text-sm font-bold transition-all text-purple-400 hover:text-purple-300 flex justify-center items-center gap-1 truncate px-1"
        >连贯剧本 <i class="fas fa-masks-theater text-[10px]"></i></button>
      </div>
    </div>

    <!-- AI 提示词输入框 -->
    <div v-show="mode === 'ai'" class="ai-magic-box p-3.5 mb-6 transition-all" :class="{'generating': isGenerating}">
      <div class="flex items-start gap-2.5">
        <i class="fas fa-sparkles text-[#FF9500] mt-1 text-xs opacity-80"></i>
        <div class="flex-1 flex flex-col gap-3 min-w-0">
          <textarea 
            v-model="userPrompt" 
            class="w-full h-20 bg-transparent text-[#FF9500] placeholder-[#FF9500]/40 outline-none resize-none font-medium leading-relaxed no-scrollbar"
            placeholder="输入场景灵感 (例如：深夜电台语录、悬疑电影解说或高燃爽文旁白)，AI 将自动生成带情绪起伏的单人配音稿..."
          ></textarea>
          
          <!-- 底部控制栏 -->
          <div class="flex flex-wrap lg:flex-nowrap justify-between items-center gap-3 border-t border-[#FF9500]/20 pt-3">
            <!-- 快捷灵感标签 -->
            <div class="flex flex-wrap gap-2 flex-1">
              <button 
                @click="userPrompt = '深夜情感电台：一段关于释怀与放下过去的独白，语气从一开始的淡淡忧伤转为最后的坚定释然。'"
                class="text-[10px] px-2.5 py-1 rounded-md bg-[#FF9500]/10 text-[#FF9500]/80 hover:bg-[#FF9500]/25 hover:text-[#FF9500] transition-colors whitespace-nowrap"
              >📻 深夜电台</button>
              <button 
                @click="userPrompt = '悬疑解说开场：一段充满压迫感和神秘感的电影解说起手式，语气低沉诡异，节奏紧凑。'"
                class="text-[10px] px-2.5 py-1 rounded-md bg-[#FF9500]/10 text-[#FF9500]/80 hover:bg-[#FF9500]/25 hover:text-[#FF9500] transition-colors whitespace-nowrap"
              >🔪 悬疑解说</button>
              <button 
                @click="userPrompt = '大女主爽文独白：面对曾经背叛自己的人，语气从一开始的轻描淡写转为极具张力的霸气反击。'"
                class="text-[10px] px-2.5 py-1 rounded-md bg-[#FF9500]/10 text-[#FF9500]/80 hover:bg-[#FF9500]/25 hover:text-[#FF9500] transition-colors whitespace-nowrap"
              >👑 爽文旁白</button>
            </div>
            
            <!-- 触发动作 -->
            <button 
              @click="generateScript" 
              :disabled="isGenerating || !userPrompt" 
              class="shrink-0 bg-gradient-to-r from-[#FF9500] to-[#FFAB33] hover:from-[#FFA61A] hover:to-[#FFB74D] text-black text-[11px] font-bold px-4 py-1.5 rounded-full transition-all active:scale-95 disabled:opacity-40 disabled:grayscale flex items-center justify-center gap-1.5 shadow-[0_2px_10px_rgba(255,149,0,0.2)]"
            >
              <i v-if="!isGenerating" class="fas fa-pen-nib"></i>
              <i v-else class="fas fa-circle-notch fa-spin"></i>
              {{ isGenerating ? 'AI 创作中...' : '让 AI 帮我写' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 主编辑器 -->
    <div class="glass-panel rounded-2xl p-4 flex flex-col min-h-[320px] focus-within:border-[#FF9500]/40 transition-colors relative">

      <textarea 
        v-model="scriptContent" 
        id="main-editor" 
        class="w-full flex-1 bg-transparent text-white placeholder-gray-600 outline-none resize-none text-[15px] leading-relaxed no-scrollbar" 
        :class="{'typing-cursor': isGenerating}" 
        placeholder="输入要转换为语音的文字..."
        :maxlength="300"
      ></textarea>

      <!-- 语速 / 音量 / 音高 控制 (高级参数面板) -->
      <div v-show="showParams" class="mt-1 mb-2 p-4 bg-black/40 rounded-xl border border-white/10 space-y-4 backdrop-blur-md">
        <div class="flex items-center justify-between mb-1">
          <span class="text-xs text-gray-300 font-bold">声音微调</span>
          <button @click="showParams = false" class="text-gray-500 hover:text-white"><i class="fas fa-times"></i></button>
        </div>
        <div class="flex items-center gap-3">
          <span class="text-[11px] text-gray-400 w-10 shrink-0">语速</span>
          <input type="range" v-model.number="speedRatio" min="0.2" max="3" step="0.1" class="slider flex-1" />
          <span class="text-[11px] text-[#FF9500] font-mono w-8 text-right">{{ speedRatio.toFixed(1) }}</span>
        </div>
        <div class="flex items-center gap-3">
          <span class="text-[11px] text-gray-400 w-10 shrink-0">音量</span>
          <input type="range" v-model.number="volumeRatio" min="0.1" max="3" step="0.1" class="slider flex-1" />
          <span class="text-[11px] text-[#FF9500] font-mono w-8 text-right">{{ volumeRatio.toFixed(1) }}</span>
        </div>
        <div class="flex items-center gap-3">
          <span class="text-[11px] text-gray-400 w-10 shrink-0">音高</span>
          <input type="range" v-model.number="pitchRatio" min="0.1" max="3" step="0.1" class="slider flex-1" />
          <span class="text-[11px] text-[#FF9500] font-mono w-8 text-right">{{ pitchRatio.toFixed(1) }}</span>
        </div>
      </div>

      <div class="flex justify-between items-end mt-4 pt-2">
        <div class="flex gap-4">
          <div class="flex flex-col items-center gap-1.5 cursor-pointer group" @click="showVoiceSelector = true">
            <div class="w-10 h-10 rounded-full bg-gradient-to-br from-[#FF9500]/20 to-[#FF6B00]/20 border-2 border-[#FF9500]/60 flex justify-center items-center text-[#FF9500] group-hover:scale-110 group-hover:shadow-[0_0_16px_rgba(255,149,0,0.4)] transition-all relative">
              <i class="fas fa-microphone-alt text-sm"></i>
              <!-- 脉冲提示点 -->
              <span class="absolute -top-0.5 -right-0.5 w-2.5 h-2.5 bg-[#FF9500] rounded-full animate-ping opacity-75"></span>
              <span class="absolute -top-0.5 -right-0.5 w-2.5 h-2.5 bg-[#FF9500] rounded-full"></span>
            </div>
            <span class="text-[10px] text-[#FF9500] font-bold group-hover:text-white flex items-center gap-0.5">
              🎙️ {{ selectedVoice?.name || '选音色' }}
              <i class="fas fa-chevron-right text-[8px] opacity-60"></i>
            </span>
          </div>
          <div class="flex flex-col items-center gap-1.5 cursor-pointer group" @click="showParams = !showParams">
            <div class="w-10 h-10 rounded-full bg-white/5 border flex justify-center items-center group-hover:bg-white/10 transition-colors" :class="showParams ? 'text-[#FF9500] border-[#FF9500]/50' : 'text-gray-400 border-white/10 group-hover:text-white'">
              <i class="fas fa-sliders-h text-sm"></i>
            </div>
            <span class="text-[10px] font-bold group-hover:text-gray-300" :class="showParams ? 'text-[#FF9500]' : 'text-gray-500'">声音微调</span>
          </div>
        </div>

        <span class="text-[11px] font-mono mb-2" :class="scriptContent.length > 300 ? 'text-red-500' : 'text-gray-500'">
          <span class="text-[#FF9500]">{{ scriptContent.length }}</span> / 300
        </span>
      </div>
    </div>

    <!-- 使用小贴士 -->
    <div class="mt-4 mb-2 space-y-2.5">
      <h3 class="text-xs font-bold text-gray-400 mb-2">💡 使用小贴士</h3>
      <div class="flex items-start gap-2.5 p-3 rounded-xl bg-white/[0.03] border border-white/5">
        <i class="fas fa-lightbulb text-[#FF9500]/60 text-xs mt-0.5 shrink-0"></i>
        <p class="text-[11px] text-gray-400 leading-relaxed">选择不同的<span class="text-[#FF9500]">音色</span>可获得截然不同的效果，适合用于配音、朗读、广告等场景</p>
      </div>
      <div class="flex items-start gap-2.5 p-3 rounded-xl bg-white/[0.03] border border-white/5">
        <i class="fas fa-sliders-h text-purple-400/60 text-xs mt-0.5 shrink-0"></i>
        <p class="text-[11px] text-gray-400 leading-relaxed">通过<span class="text-purple-400">声音微调</span>调节语速/音量/音高，让配音更贴合你的内容风格</p>
      </div>
      <div class="flex items-start gap-2.5 p-3 rounded-xl bg-white/[0.03] border border-white/5">
        <i class="fas fa-star text-[#FFD60A]/60 text-xs mt-0.5 shrink-0"></i>
        <p class="text-[11px] text-gray-400 leading-relaxed">切换到 <span class="text-[#FF9500]">AI 编排</span>模式，输入场景关键词就能自动生成配音稿</p>
      </div>
    </div>

  </div>

  <!-- 底部操作栏 -->
  <div class="absolute bottom-0 w-full px-5 pb-8 bg-gradient-to-t from-black via-black/90 to-transparent z-50">
    <button @click="handleSynthesize" :disabled="synthesizing" class="w-full py-3.5 rounded-xl font-bold text-[16px] shadow-[0_4px_20px_rgba(255,149,0,0.3)] transition-all flex justify-center items-center gap-2 cursor-pointer" :class="synthesizing ? 'bg-gray-700 text-gray-300 scale-[0.98] cursor-not-allowed' : 'bg-gradient-to-r from-[#FF9500] to-[#FFD60A] text-black active:scale-95'">
      <i v-if="!synthesizing" class="fas fa-pen text-sm"></i>
      <i v-else class="fas fa-circle-notch fa-spin text-sm"></i>
      {{ synthesizing ? synthStatusText : '\u2712 立即合成配音' }}
    </button>
  </div>

  <!-- 合成中遮罩层 -->
  <Transition name="fade">
    <div v-if="synthesizing" class="fixed inset-0 bg-black/80 backdrop-blur-sm z-[999] flex flex-col items-center justify-center gap-6">
      <div class="synth-pulse"></div>
      <p class="text-white text-lg font-bold animate-pulse">{{ synthStatusText }}</p>
      <p class="text-gray-400 text-xs">文本越长，合成时间越久，请耐心等待</p>
    </div>
  </Transition>
  <!-- 音色选择弹窗抽屉 -->
  <VoiceSelector 
    v-model:visible="showVoiceSelector" 
    engine="tts-1.0" 
    :initialVoiceId="selectedVoice?.voiceId || 'BV001_streaming'" 
    @select="selectedVoice = $event" 
  />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { usePlayerStore } from '../stores/player'
import { useToastStore } from '../stores/toast'
import { ttsApi } from '../api/tts'
import VoiceSelector from '../components/VoiceSelector.vue'

const router = useRouter()
const authStore = useAuthStore()
const playerStore = usePlayerStore()
const toastStore = useToastStore()

const mode = ref('short') // 'short' | 'ai'
const userPrompt = ref('深夜情感电台：一段关于释怀与放下过去的独白，语气从一开始的淡淡忧伤转为最后的坚定释然。')
const scriptContent = ref('')
const lastSynthSignature = ref('') // 记录上次成功合成的参数与文本指纹，防止无脑重复提交
const isGenerating = ref(false)

const showVoiceSelector = ref(false)
const selectedVoice = ref({ voiceId: 'BV001_streaming', name: '通用女声' })

// 语速 / 音量 / 音高 (火山引擎参数)
const showParams = ref(false)
const speedRatio = ref(1.0)
const volumeRatio = ref(1.0)
const pitchRatio = ref(1.0)

// 合成状态
const synthesizing = ref(false)
const synthStatusText = ref('准备合成中...')

// 调用真实 SSE 接口打字机回显
const generateScript = async () => {
  if (!authStore.isLoggedIn) {
    toastStore.show('请先登录即可为您量身定制专属 AI 剧本 ✨')
    router.replace({ name: 'Login', query: { redirect: '/create' } })
    return
  }

  if (!authStore.hasFeature('ai_script')) {
    toastStore.show('当前权限不足，请访问会员台解锁 Pro 特权 👑')
    return router.push('/vip')
  }

  const scriptQuota = authStore.getQuota('ai_script_daily_count')
  if (scriptQuota === 0) {
    toastStore.show('今日 AI 剧本生成配额已耗尽或未开通权限，请升级会员或明日再来 ✨')
    return
  }

  const currentPrompt = userPrompt.value.trim()
  if (!currentPrompt || isGenerating.value) return

  isGenerating.value = true
  scriptContent.value = ''

  try {
    const token = authStore.token
    const authHeader = token ? (token.startsWith('Bearer ') ? token : `Bearer ${token}`) : ''
    
    // 使用 fetch 获取流而不是用 axios
    const res = await fetch('/api/tts/ai-script', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': authHeader
      },
      body: JSON.stringify({ prompt: userPrompt.value })
    })

    if (!res.ok) {
      let errorMsg = '网络请求错误 ' + res.status
      try {
        const errData = await res.json()
        if (errData.message) errorMsg = errData.message
      } catch (e) {} // 忽略 JSON 解析错误
      throw new Error(errorMsg)
    }

    // 后端如果未通过异常外发 HTTP 4XX，而是返回了全局拦截器的 200 OK JSON (例如没有权限)
    const contentType = res.headers.get('content-type') || ''
    if (contentType.includes('application/json')) {
      const errData = await res.json()
      if (errData.code && errData.code !== 200) {
        throw new Error(errData.message || '接口调用异常')
      }
    }

    const reader = res.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''

    // ★ 流式 <think> 标签过滤状态机
    let insideThink = false

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      
      buffer += decoder.decode(value, { stream: true })
      
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''
      
      for (const line of lines) {
        if (!line.trim()) continue
        
        if (line.startsWith('data:')) {
           let data = line.substring(5)
           if (data.startsWith(' ')) data = data.substring(1)
           
           // ★ 过滤 <think>...</think> 思维链内容
           if (data.includes('<think>')) { insideThink = true }
           if (!insideThink) {
             scriptContent.value += data
           }
           if (data.includes('</think>')) { insideThink = false }
        }
      }
    }
    // ★ 流结束后，兜底清理（防止残留标签片段）
    scriptContent.value = scriptContent.value
      .replace(/<think>[\s\S]*?<\/think>/g, '')
      .replace(/<\/?think>/g, '')
      .trim()
  } catch (error) {
    console.error('AI 剧本生成错误:', error)
    toastStore.show(error.message || 'AI 生成失败，请稍候重试')
  } finally {
    isGenerating.value = false
  }
}

const handleSynthesize = async () => {
  if (!authStore.isLoggedIn) {
    toastStore.show('请先登录以唤醒您的创作档案 ✨')
    router.replace({ name: 'Login', query: { redirect: '/create' } })
    return
  }
  
  const currentText = scriptContent.value.trim()
  if (!currentText) {
    toastStore.show('请先投喂一点创作灵感或基础文字素材哦 ✨')
    return
  }
  
  // 生成当前合成请求的状态指纹 (文本+音色+参数)，允许修改参数后重新合成同样文本
  const currentSignature = `${currentText}|${selectedVoice.value.voiceId}|${speedRatio.value}|${volumeRatio.value}|${pitchRatio.value}`
  
  if (currentSignature === lastSynthSignature.value) {
    toastStore.show('该内容与参数已合成完毕，无需重复提交 🚀')
    return
  }
  
  if (synthesizing.value) return // 防重复点击

  synthesizing.value = true
  synthStatusText.value = '请求构建中...'

  try {
    // 统一全部走极速短文本同步合成 (暂时屏蔽长文分段异步逻辑，后续将在 V2 中重构)
    setTimeout(() => { if (synthesizing.value) synthStatusText.value = '正在极速合成，请稍候...' }, 500)
    
    const res = await ttsApi.synthesizeShort({
        text: currentText,
        voiceId: selectedVoice.value.voiceId,
        speed: speedRatio.value,
        volume: volumeRatio.value,
        pitch: pitchRatio.value
    })
    
    if (res.audioUrl) {
      completeSynthesis(res.audioUrl, '极速短文字合成', currentSignature)
    }

  } catch (error) {
    console.error(error)
    toastStore.show(error.message || '合成失败，请稍后重试')
  } finally {
    synthesizing.value = false
  }
}

const completeSynthesis = async (url, titleLabel, currentSignature) => {
  lastSynthSignature.value = currentSignature // 记录成功合成的签名
  synthStatusText.value = '合成完成 ✅'
  await new Promise(r => setTimeout(r, 400))
  playerStore.play({
    title: titleLabel,
    author: selectedVoice.value.name,
    url: url
  })
}
</script>

<style scoped>
/* AI 魔法框流光边框 */
.ai-magic-box { position: relative; border-radius: 1rem; background: rgba(0,0,0,0.6); border: 1px solid rgba(255, 149, 0, 0.2); transition: all 0.3s ease; }
.ai-magic-box:focus-within { border-color: rgba(255, 149, 0, 0.6); box-shadow: 0 0 15px rgba(255, 149, 0, 0.15); }
.ai-magic-box.generating::before {
    content: ""; position: absolute; inset: -1px; border-radius: 1rem;
    background: linear-gradient(90deg, #FF9500, #8B5CF6, #FFD60A, #FF9500);
    background-size: 200% 100%; z-index: -1; filter: blur(3px);
    animation: moveGradient 2s linear infinite;
}
@keyframes moveGradient { 0% { background-position: 100% 0; } 100% { background-position: -100% 0; } }

/* 打字机光标 */
.typing-cursor::after { content: '|'; animation: blink 1s step-end infinite; color: #FF9500; }
@keyframes blink { 0%, 100% { opacity: 1; } 50% { opacity: 0; } }

/* 滑块样式 */
.slider { -webkit-appearance: none; appearance: none; height: 4px; border-radius: 2px; background: rgba(255,255,255,0.1); outline: none; }
.slider::-webkit-slider-thumb { -webkit-appearance: none; appearance: none; width: 16px; height: 16px; border-radius: 50%; background: #FF9500; cursor: pointer; border: 2px solid rgba(0,0,0,0.3); box-shadow: 0 0 6px rgba(255,149,0,0.4); }
.slider::-moz-range-thumb { width: 16px; height: 16px; border-radius: 50%; background: #FF9500; cursor: pointer; border: 2px solid rgba(0,0,0,0.3); }

/* 合成中圆形声波脉冲动画 */
.synth-pulse {
  width: 80px; height: 80px; border-radius: 50%;
  background: radial-gradient(circle, #FF9500 0%, transparent 70%);
  animation: synthPulse 1.5s ease-in-out infinite;
  box-shadow: 0 0 40px rgba(255,149,0,0.3), 0 0 80px rgba(255,149,0,0.1);
}
@keyframes synthPulse {
  0%, 100% { transform: scale(0.8); opacity: 0.6; }
  50% { transform: scale(1.2); opacity: 1; }
}

/* 遮罩层过渡 */
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

/* 彻底隐藏滚动条但保留滚动能力 (打造顺滑瀑布流观感) */
.no-scrollbar {
  -ms-overflow-style: none;  /* IE and Edge */
  scrollbar-width: none;  /* Firefox */
}
.no-scrollbar::-webkit-scrollbar {
  display: none; /* Chrome, Safari and Opera */
}
</style>
