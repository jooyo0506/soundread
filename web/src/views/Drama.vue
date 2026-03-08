<template>
  <div class="h-full bg-black text-white px-4 pt-12 pb-[100px] overflow-y-auto no-scrollbar relative font-sans">
    
    <!-- 头部栏 -->
    <div class="flex items-center justify-between mb-8 relative z-10 block">
      <div class="flex items-center gap-4">
        <button @click="router.back()" class="w-10 h-10 rounded-full bg-white/5 border border-white/10 flex items-center justify-center active:scale-95 transition-transform backdrop-blur-md">
          <i class="fas fa-chevron-left text-sm text-gray-300"></i>
        </button>
        <div>
          <h1 class="text-2xl font-bold tracking-tight text-white mb-0.5">连贯剧本</h1>
          <p class="text-[11px] text-[#FF9500] uppercase tracking-wider font-medium opacity-80">TTS 2.0 Drama MVP</p>
        </div>
      </div>
    </div>

    <!-- 全局背景配置 -->
    <div class="mb-6 p-4 rounded-3xl bg-white/5 border border-white/10 backdrop-blur-xl">
      <h3 class="text-sm font-semibold text-white/80 mb-3 flex items-center gap-2">
        <i class="fas fa-clapperboard text-[#FF9500]"></i> 全局背景设定
      </h3>
      <textarea
        v-model="globalContext"
        placeholder="例如：在一个阴森的午夜走廊，四周极其安静..."
        class="w-full bg-black/30 border border-white/5 rounded-2xl p-4 text-white text-[15px] placeholder-white/20 focus:outline-none focus:border-[#FF9500]/50 transition-colors resize-none h-20"
      ></textarea>
    </div>

    <!-- 角色台词列表 -->
    <div class="space-y-4 mb-6">
      <div v-for="(line, index) in lines" :key="index" class="p-4 rounded-3xl bg-white/5 border border-white/10 relative backdrop-blur-xl group">
        <!-- 头部栏：角色和删除按钮 -->
        <div class="flex items-center justify-between mb-3 border-b border-white/5 pb-2">
          <span class="text-xs font-bold text-gray-400 bg-white/5 px-3 py-1 rounded-full uppercase tracking-widest">幕 {{ index + 1 }}</span>
          <button @click="removeLine(index)" class="text-red-400/70 hover:text-red-400 p-2" v-if="lines.length > 1">
            <i class="fas fa-trash-alt text-sm"></i>
          </button>
        </div>
        
        <!-- 音色选择 (简单版 MVP，手动输入或绑定常用 ID) -->
        <div class="mb-3">
          <label class="block text-xs font-medium text-white/60 mb-1.5 ml-1">角色音色 (默认: BV001_male/BV002_female)</label>
          <div class="relative">
            <select v-model="line.speakerVoiceType" class="w-full bg-black/40 border border-white/10 rounded-xl p-3 text-white text-sm focus:outline-none focus:border-[#FF9500]/50 appearance-none">
              <option value="BV001_male">灿烂男声 (BV001_male)</option>
              <option value="BV002_female">温柔女声 (BV002_female)</option>
              <option value="BV004_male">霸道男声 (BV004_male)</option>
            </select>
            <i class="fas fa-chevron-down absolute right-4 top-1/2 -translate-y-1/2 text-white/30 text-xs pointer-events-none"></i>
          </div>
        </div>

        <!-- 台词内容 -->
        <div>
          <label class="block text-xs font-medium text-white/60 mb-1.5 ml-1">角色台词 (支持 [#语气指令])</label>
          <textarea
            v-model="line.content"
            placeholder="[#语气急促] 你为什么一定要这么做？"
            class="w-full bg-black/40 border border-white/10 rounded-xl p-3 text-white text-[15px] placeholder-white/20 focus:outline-none focus:border-white/20 transition-colors resize-none h-24"
          ></textarea>
        </div>
      </div>
    </div>

    <!-- 操作区 -->
    <div class="flex gap-3 mb-24">
      <button 
        @click="addLine" 
        class="flex-1 py-4 border border-dashed border-white/20 rounded-2xl text-white/60 font-medium text-sm hover:bg-white/5 hover:text-white transition-all flex items-center justify-center gap-2">
        <i class="fas fa-plus"></i> 追加一幕角色
      </button>

      <button 
        @click="handleSynthesize" 
        :disabled="isGenerating"
        class="flex-1 py-4 bg-gradient-to-r from-[#FF9500] to-[#FF5E00] rounded-2xl text-white font-bold text-[15px] shadow-lg shadow-[#FF9500]/20 active:scale-[0.98] transition-all disabled:opacity-50 disabled:grayscale flex items-center justify-center gap-2">
        <template v-if="!isGenerating">
          <i class="fas fa-wand-magic-sparkles"></i> 渲染剧情
        </template>
        <template v-else>
          <i class="fas fa-circle-notch fa-spin"></i> 录制中...
        </template>
      </button>
    </div>

  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useToastStore } from '@/stores/toast'
import { dramaApi } from '@/api/tts'
import { usePlayerStore } from '@/stores/player'

const router = useRouter()
const toast = useToastStore()
const player = usePlayerStore()

const globalContext = ref('')
const lines = ref([
  { speakerVoiceType: 'BV001_male', content: '[#冷笑] 呵，你以为这就算完了吗？' },
  { speakerVoiceType: 'BV002_female', content: '[#哽咽] 你...你到底想干什么！' }
])
const isGenerating = ref(false)

const addLine = () => {
  lines.value.push({
    speakerVoiceType: 'BV001_male',
    content: ''
  })
}

const removeLine = (index) => {
  lines.value.splice(index, 1)
}

const handleSynthesize = async () => {
  const validLines = lines.value.filter(l => l.content.trim())
  if (validLines.length === 0) {
    toast.show('剧本太空了，写点台词吧')
    return
  }
  
  try {
    isGenerating.value = true
    // 使用封装好的 dramaApi，符合前端规范（禁止组件内直接调 axios）
    const data = await dramaApi.synthesize({
      globalContext: globalContext.value,
      lines: validLines
    })

    if (data?.audioUrl) {
      toast.show('🎉 短剧录制完成！')
      player.playUrl(data.audioUrl)
    } else {
      toast.show('合成失败，请重试')
    }
  } catch (error) {
    console.error('[Drama] 剧情合成异常:', error)
    toast.show(error.response?.data?.msg || '请求服务失败，请重试')
  } finally {
    isGenerating.value = false
  }
}
</script>

<style scoped>
.no-scrollbar::-webkit-scrollbar {
  display: none;
}
.no-scrollbar {
  -ms-overflow-style: none;
  scrollbar-width: none;
}
</style>
