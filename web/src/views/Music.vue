<template>
  <div class="h-full overflow-y-auto px-5 pt-14 pb-24 hide-scrollbar">

    <!-- Header -->
    <div class="flex items-center justify-between mb-6">
      <div class="flex items-center gap-3">
        <button @click="$router.back()"
                class="w-8 h-8 rounded-full bg-white/5 flex items-center justify-center text-gray-400 hover:text-white hover:bg-white/10 transition-colors cursor-pointer">
          <i class="fas fa-chevron-left text-xs"></i>
        </button>
        <div>
          <h1 class="text-2xl font-bold text-white tracking-wide flex items-center gap-2">
            <span class="w-9 h-9 rounded-xl bg-gradient-to-br from-fuchsia-500 to-purple-600 flex items-center justify-center text-sm">🎵</span>
            AI 音乐
          </h1>
          <p class="text-xs text-gray-500 mt-1">用 AI 创作你的专属音乐</p>
        </div>
      </div>
    </div>

    <!-- ==================== 提示词输入 ==================== -->
    <div class="glass-panel rounded-2xl p-4 mb-4 border-fuchsia-500/10">
      <label class="text-xs text-gray-400 mb-2 block flex items-center gap-1.5">
        <i class="fas fa-wand-magic-sparkles text-fuchsia-400"></i> 描述你的音乐
      </label>
      <textarea
        v-model="prompt"
        placeholder="描述你想要的音乐风格，如：r&b, slow, passionate, male vocal, Chinese..."
        class="w-full h-[100px] bg-white/5 rounded-xl px-3 py-2.5 text-sm text-white placeholder-gray-600 resize-none focus:outline-none focus:ring-1 focus:ring-fuchsia-500/50 border border-white/5 hide-scrollbar overflow-y-auto"
        maxlength="500"
      ></textarea>

      <!-- 底部栏：纯音乐开关 -->
      <div class="flex items-center justify-between mt-3 pt-3 border-t border-white/5">
        <span class="text-[10px] text-gray-600">{{ prompt.length }}/500</span>
        <label class="flex items-center gap-2 cursor-pointer" :class="isProcessing ? 'opacity-50 pointer-events-none' : ''">
          <span class="text-xs text-gray-400">纯音乐</span>
          <div @click="!isProcessing && (mode = mode === 'instrumental' ? 'song' : 'instrumental')"
               class="relative w-10 h-[22px] rounded-full transition-colors duration-200"
               :class="mode === 'instrumental' ? 'bg-fuchsia-500' : 'bg-white/15'">
            <div class="absolute top-[3px] w-4 h-4 rounded-full bg-white shadow transition-all duration-200"
                 :class="mode === 'instrumental' ? 'left-[22px]' : 'left-[3px]'"></div>
          </div>
        </label>
      </div>

      <!-- 创作灵感标签 -->
      <div class="mt-3">
        <span class="text-[10px] text-gray-500 mb-1.5 block">创作灵感</span>
        <div class="flex gap-1.5 flex-wrap">
          <button v-for="tag in promptTags" :key="tag" @click="toggleTag(tag)"
                  class="text-[10px] px-2.5 py-1 rounded-full transition-colors cursor-pointer"
                  :class="hasTag(tag)
                    ? 'text-white bg-fuchsia-500/40 border border-fuchsia-400/50'
                    : 'text-fuchsia-400 bg-fuchsia-500/10 hover:bg-fuchsia-500/20'">
            {{ hasTag(tag) ? '✓' : '+' }} {{ tag }}
          </button>
        </div>
      </div>
    </div>

    <!-- ==================== 歌词区 (歌曲模式) ==================== -->
    <Transition name="slide">
      <div v-if="mode === 'song'" class="glass-panel rounded-2xl p-4 mb-4 border-fuchsia-500/10">
        <div class="flex items-center justify-between mb-2">
          <label class="text-xs text-gray-400 flex items-center gap-1.5">
            <i class="fas fa-pen-fancy text-purple-400"></i> 歌词
          </label>
          <button @click="aiGenerateLyrics" :disabled="lyricsLoading || !prompt.trim()"
                  class="text-[10px] font-bold text-fuchsia-400 bg-fuchsia-500/10 px-3 py-1 rounded-full hover:bg-fuchsia-500/20 transition-colors disabled:opacity-30 cursor-pointer flex items-center gap-1">
            <i class="fas fa-wand-magic-sparkles" :class="lyricsLoading ? 'fa-spin' : ''"></i>
            {{ lyricsLoading ? '生成中...' : '✨ AI 写词' }}
          </button>
        </div>
        <textarea
          v-model="lyrics"
          placeholder="[Verse]&#10;在暴风雨的夜晚&#10;想着你的温柔笑脸&#10;&#10;[Chorus]&#10;..."
          class="w-full h-[140px] bg-white/5 rounded-xl px-3 py-2.5 text-sm text-white placeholder-gray-600 resize-none focus:outline-none focus:ring-1 focus:ring-purple-500/50 border border-white/5 font-mono hide-scrollbar overflow-y-auto"
        ></textarea>
      </div>
    </Transition>



    <!-- ==================== 生成按钮 ==================== -->
    <button @click="startGenerate" :disabled="generating || isProcessing || !prompt.trim() || (mode === 'song' && !lyrics.trim())"
            class="w-full py-3.5 rounded-xl font-bold text-sm transition-all disabled:opacity-30 cursor-pointer"
            :class="generating
              ? 'bg-fuchsia-500/30 text-fuchsia-200'
              : 'bg-gradient-to-r from-fuchsia-500 to-purple-500 text-white shadow-[0_4px_20px_rgba(192,80,240,0.3)] hover:shadow-[0_4px_30px_rgba(192,80,240,0.5)] active:scale-[0.98]'">
      <template v-if="generating">
        <i class="fas fa-circle-notch fa-spin mr-2"></i> AI 创作中...
      </template>
      <template v-else>
        <i class="fas fa-music mr-2"></i> {{ mode === 'song' ? '🎤 生成歌曲' : '🎼 生成纯音乐' }}
      </template>
    </button>

    <!-- 紧凑进度条 (紧贴生成按钮下方) -->
    <Transition name="slide">
      <div v-if="currentTask && (currentTask.status === 'processing' || currentTask.status === 'streaming')" class="mt-2 mb-4">
        <div class="flex items-center justify-between mb-1">
          <span class="text-[10px] flex items-center gap-1"
                :class="currentTask.status === 'streaming' ? 'text-green-400' : 'text-fuchsia-400'">
            <i :class="currentTask.status === 'streaming' ? 'fas fa-headphones animate-pulse' : 'fas fa-circle-notch fa-spin'" class="text-[8px]"></i>
            {{ currentTask.status === 'streaming' ? '🎵 边生成边听...' : 'AI 正在创作...' }}
          </span>
          <span class="text-[10px] text-gray-600">{{ currentTask.title }}</span>
        </div>
        <div class="w-full h-1 bg-white/5 rounded-full overflow-hidden">
          <div class="h-full rounded-full animate-progress"
               :class="currentTask.status === 'streaming' ? 'bg-gradient-to-r from-green-500 to-emerald-500' : 'bg-gradient-to-r from-fuchsia-500 to-purple-500'"></div>
        </div>
      </div>
    </Transition>

    <!-- 完成/失败提示 (紧凑) -->
    <Transition name="slide">
      <div v-if="currentTask && currentTask.status === 'succeeded'" class="mt-2 mb-4 flex items-center justify-between bg-green-500/10 rounded-xl px-3 py-2">
        <div class="flex items-center gap-2">
          <i class="fas fa-check-circle text-green-400 text-xs"></i>
          <span class="text-xs text-green-400">创作完成!</span>
        </div>
        <button @click="playResult(currentTask)"
                class="text-[10px] font-bold text-fuchsia-300 bg-fuchsia-500/20 px-3 py-1 rounded-full hover:bg-fuchsia-500/30 transition-colors cursor-pointer flex items-center gap-1">
          <i class="fas fa-play text-[8px]"></i> 播放
        </button>
      </div>
    </Transition>
    <Transition name="slide">
      <div v-if="currentTask && currentTask.status === 'failed'" class="mt-2 mb-4 flex items-center gap-2 bg-red-500/10 rounded-xl px-3 py-2">
        <i class="fas fa-exclamation-circle text-red-400 text-xs"></i>
        <span class="text-xs text-red-400">{{ currentTask.errorMsg || '生成失败，请重试' }}</span>
      </div>
    </Transition>

    <div v-if="!currentTask || !['processing', 'streaming'].includes(currentTask.status)" class="mb-6"></div>

    <!-- ==================== 我的作品 ==================== -->
    <div class="mb-4">
      <div class="flex items-center justify-between mb-3">
        <h3 class="text-sm font-bold text-white flex items-center gap-2">
          <i class="fas fa-compact-disc text-fuchsia-400"></i> 我的作品
          <span v-if="tasks.length > 0" class="text-[10px] text-gray-600 font-normal">{{ tasks.length }} 首</span>
        </h3>
        <button v-if="tasks.length > 1" @click="showAllTasks = !showAllTasks"
                class="text-[10px] text-fuchsia-400 hover:text-fuchsia-300 transition-colors cursor-pointer flex items-center gap-1">
          {{ showAllTasks ? '收起' : '查看全部' }}
          <i class="fas" :class="showAllTasks ? 'fa-chevron-up' : 'fa-chevron-right'" style="font-size: 8px"></i>
        </button>
      </div>

      <div v-if="tasks.length === 0" class="glass-panel rounded-xl p-6 text-center">
        <i class="fas fa-music text-gray-700 text-2xl mb-2"></i>
        <p class="text-xs text-gray-600">还没有作品哦，开始创作吧 ✨</p>
      </div>

      <div v-else class="space-y-2">
        <div v-for="task in displayedTasks" :key="task.id"
             class="glass-panel rounded-xl p-3 group hover:border-fuchsia-500/20 transition-colors">

          <div class="flex items-center gap-3">
            <!-- 图标 -->
            <div class="w-9 h-9 rounded-lg flex items-center justify-center flex-shrink-0 cursor-pointer"
                 :class="{
                   'bg-fuchsia-500/15': task.status === 'succeeded',
                   'bg-amber-500/15 animate-pulse': task.status === 'processing',
                   'bg-red-500/15': task.status === 'failed'
                 }"
                 @click="task.status === 'succeeded' && playResult(task)">
              <i :class="{
                   'fas fa-music text-fuchsia-400': task.status === 'succeeded',
                   'fas fa-circle-notch fa-spin text-amber-400': task.status === 'processing',
                   'fas fa-times text-red-400': task.status === 'failed'
                 }"></i>
            </div>

            <!-- 信息 -->
            <div class="flex-1 min-w-0">
              <h4 class="text-white text-sm font-bold truncate">{{ task.title || 'AI 音乐' }}</h4>
              <p class="text-[10px] text-gray-500 flex items-center gap-2 mt-0.5">
                <span>{{ task.taskType === 'song' ? '🎤 歌曲' : '🎼 纯音乐' }}</span>
                <span v-if="task.duration">{{ formatDuration(task.duration) }}</span>
                <span>{{ formatTime(task.createdAt) }}</span>
              </p>
            </div>

            <!-- 状态标签 + 操作 -->
            <div class="flex items-center gap-1.5">
              <!-- 上架状态 -->
              <span v-if="task.published" class="text-[8px] text-green-400 bg-green-500/10 px-1.5 py-0.5 rounded font-bold flex items-center gap-0.5">
                <span class="w-1 h-1 rounded-full bg-green-400 animate-pulse"></span>已上架
              </span>
              <!-- 播放 -->
              <button v-if="task.status === 'succeeded'" @click.stop="playResult(task)"
                      class="w-7 h-7 rounded-full bg-fuchsia-500/20 flex items-center justify-center text-fuchsia-400 hover:bg-fuchsia-500/30 transition-colors cursor-pointer">
                <i class="fas fa-play text-[10px] pl-0.5"></i>
              </button>
              <!-- 更多操作 -->
              <button v-if="task.status === 'succeeded'" @click.stop="openMusicAction(task)"
                      class="w-7 h-7 rounded-full bg-white/5 flex items-center justify-center text-gray-500 hover:text-white hover:bg-white/10 transition-colors cursor-pointer">
                <i class="fas fa-ellipsis-h text-[10px]"></i>
              </button>
            </div>
          </div>

          <!-- 歌词预览（歌曲类型） -->
          <div v-if="task.taskType === 'song' && task.lyrics && task.status === 'succeeded'"
               class="mt-2 pt-2 border-t border-white/5">
            <p class="text-[10px] text-gray-600 line-clamp-2 leading-relaxed italic">🎤 {{ task.lyrics.substring(0, 80) }}...</p>
          </div>
        </div>
      </div>
    </div>

    <!-- ======== 音乐管理弹窗 ======== -->
    <Teleport to="body">
      <Transition name="sheet">
        <div v-if="actionTask" class="fixed inset-0 z-50 flex items-end justify-center" style="max-width: 430px; margin: 0 auto;">
          <div class="absolute inset-0 bg-black/60 backdrop-blur-sm" @click="actionTask = null"></div>
          <div class="relative w-full bg-[#1a1a1a] rounded-t-2xl border-t border-white/10 p-4 pb-8">
            <div class="w-8 h-1 rounded-full bg-white/10 mx-auto mb-4"></div>

            <!-- 标题编辑 -->
            <div class="mb-4">
              <label class="text-[9px] text-gray-500 mb-1 block">作品名称</label>
              <div class="flex items-center gap-2">
                <input v-model="editTitle" maxlength="30"
                       class="flex-1 bg-white/5 border border-white/10 rounded-lg px-3 py-2 text-sm text-white placeholder-gray-600 focus:border-fuchsia-500/40 focus:outline-none transition-colors"
                       placeholder="输入作品名称" />
                <button @click="saveRename" :disabled="renaming"
                        class="px-3 py-2 rounded-lg text-[11px] font-bold cursor-pointer transition-all"
                        :class="renaming ? 'bg-gray-700 text-gray-500' : 'bg-fuchsia-500/20 text-fuchsia-400 hover:bg-fuchsia-500/30'">
                  <i v-if="renaming" class="fas fa-circle-notch fa-spin"></i>
                  <span v-else>保存</span>
                </button>
              </div>
            </div>

            <!-- 操作列表 -->
            <div class="space-y-2">
              <!-- 发布 -->
              <button v-if="!actionTask.published" @click="publishFromSheet"
                      class="w-full flex items-center gap-3 px-4 py-3 rounded-xl bg-green-500/10 hover:bg-green-500/15 transition-colors cursor-pointer">
                <i class="fas fa-cloud-upload-alt text-green-400 text-sm w-5 text-center"></i>
                <div class="text-left">
                  <div class="text-xs font-bold text-green-400">上架到发现页</div>
                  <div class="text-[10px] text-gray-500">让更多人听到你的音乐</div>
                </div>
              </button>
              <!-- 下架 -->
              <button v-if="actionTask.published" @click="unpublishFromSheet"
                      class="w-full flex items-center gap-3 px-4 py-3 rounded-xl bg-amber-500/10 hover:bg-amber-500/15 transition-colors cursor-pointer">
                <i class="fas fa-eye-slash text-amber-400 text-sm w-5 text-center"></i>
                <div class="text-left">
                  <div class="text-xs font-bold text-amber-400">下架作品</div>
                  <div class="text-[10px] text-gray-500">从发现页移除</div>
                </div>
              </button>
              <!-- 删除 -->
              <button @click="deleteFromSheet"
                      class="w-full flex items-center gap-3 px-4 py-3 rounded-xl bg-red-500/10 hover:bg-red-500/15 transition-colors cursor-pointer">
                <i class="fas fa-trash-alt text-red-400 text-sm w-5 text-center"></i>
                <div class="text-left">
                  <div class="text-xs font-bold text-red-400">删除作品</div>
                  <div class="text-[10px] text-gray-500">永久删除此音乐</div>
                </div>
              </button>
              <!-- 取消 -->
              <button @click="actionTask = null"
                      class="w-full py-3 rounded-xl text-xs text-gray-500 hover:text-white transition-colors cursor-pointer text-center">
                取消
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { musicApi } from '../api/music'
import { usePlayerStore } from '../stores/player'
import { useToastStore } from '../stores/toast'

const playerStore = usePlayerStore()
const toastStore = useToastStore()

// ==================== 状态 ====================

const mode = ref('instrumental')
const prompt = ref('')
const lyrics = ref('')
const selectedModel = ref('mureka-7.6')
const generating = ref(false)
const lyricsLoading = ref(false)
const currentTask = ref(null)
const tasks = ref([])
const showAllTasks = ref(false)
// 轮询已委托给全局 playerStore，无需本地 timer

// 是否有任务正在处理中（并发控制）
const isProcessing = computed(() => {
  return ['processing', 'streaming'].includes(currentTask.value?.status) || generating.value
})

// 默认只显示最新 1 首，展开后显示全部
const displayedTasks = computed(() => {
  return showAllTasks.value ? tasks.value : tasks.value.slice(0, 1)
})



const promptTags = ['pop', 'r&b', 'rock', 'jazz', 'classical', 'electronic', 'hip-hop', 'folk', 'Chinese', 'slow', 'energetic']

// 解析当前 prompt 为标签数组
const parseTags = () => prompt.value.split(/[,，]/).map(s => s.trim()).filter(Boolean)

// 检查某标签是否已在 prompt 中
const hasTag = (tag) => parseTags().some(t => t.toLowerCase() === tag.toLowerCase())

// 切换标签：已有则移除，没有则追加
const toggleTag = (tag) => {
  const tags = parseTags()
  const idx = tags.findIndex(t => t.toLowerCase() === tag.toLowerCase())
  if (idx >= 0) {
    tags.splice(idx, 1)
  } else {
    tags.push(tag)
  }
  prompt.value = tags.join(', ')
}

// ==================== 操作 ====================

const startGenerate = async () => {
  generating.value = true
  try {
    const res = await musicApi.generate({
      type: mode.value,
      prompt: prompt.value,
      lyrics: mode.value === 'song' ? lyrics.value : undefined,
      model: selectedModel.value
    })
    currentTask.value = { id: res.taskId, status: res.status, title: prompt.value.substring(0, 20) }
    toastStore.show('🎵 任务已提交，AI 正在创作中...')
    startPolling()
    loadTasks()
  } catch (e) {
    toastStore.show('提交失败: ' + (e.message || '请重试'))
  } finally {
    generating.value = false
  }
}

const aiGenerateLyrics = async () => {
  lyricsLoading.value = true
  try {
    const res = await musicApi.generateLyrics(prompt.value)
    lyrics.value = res.lyrics || ''
    toastStore.show('✨ 歌词生成完成')
  } catch (e) {
    toastStore.show('歌词生成失败: ' + (e.message || '请重试'))
  } finally {
    lyricsLoading.value = false
  }
}

const playResult = async (task) => {
  if (!task.resultUrl) return

  // ★ 如果本地缓存没有歌词时间戳，重新拉取最新数据（异步 recognize 可能已完成）
  let freshTask = task
  if (!task.lyricTimings && task.taskType === 'song' && task.id) {
    try {
      freshTask = await musicApi.getTask(task.id)
      // 同步更新本地缓存，下次播放不再重复请求
      const idx = tasks.value.findIndex(t => t.id === task.id)
      if (idx !== -1) tasks.value[idx] = { ...tasks.value[idx], ...freshTask }
    } catch { /* 静默降级，使用原始 task */ }
  }

  // ★ 解析歌词时间戳 (来自 Mureka recognize API)
  let lyricTimings = null
  if (freshTask.lyricTimings) {
    try {
      lyricTimings = typeof freshTask.lyricTimings === 'string'
        ? JSON.parse(freshTask.lyricTimings)
        : freshTask.lyricTimings
    } catch (e) { /* 解析失败则降级 */ }
  }
  playerStore.play({
    title: freshTask.title || 'AI 音乐',
    author: 'AI Music',
    url: freshTask.resultUrl,
    lyrics: freshTask.taskType === 'song' ? freshTask.lyrics : null,
    lyricTimings
  })
}

const deleteTask = async (taskId) => {
  try {
    await musicApi.deleteTask(taskId)
    tasks.value = tasks.value.filter(t => t.id !== taskId)
    if (currentTask.value?.id === taskId) currentTask.value = null
    toastStore.show('已删除')
  } catch {}
}

// ==================== 管理弹窗 ====================

const actionTask = ref(null)
const editTitle = ref('')
const renaming = ref(false)

const openMusicAction = (task) => {
  actionTask.value = task
  editTitle.value = task.title || task.prompt?.substring(0, 20) || 'AI 音乐'
}

const saveRename = async () => {
  if (!editTitle.value.trim() || !actionTask.value) return
  renaming.value = true
  try {
    await musicApi.renameTask(actionTask.value.id, editTitle.value.trim())
    // 更新本地状态
    const t = tasks.value.find(t => t.id === actionTask.value.id)
    if (t) t.title = editTitle.value.trim()
    actionTask.value.title = editTitle.value.trim()
    toastStore.show('✅ 已重命名')
  } catch (e) {
    toastStore.show('重命名失败: ' + (e.response?.data?.message || e.message))
  } finally {
    renaming.value = false
  }
}

const publishFromSheet = async () => {
  if (!actionTask.value) return
  try {
    await musicApi.publishTask(actionTask.value.id)
    actionTask.value.published = true
    const t = tasks.value.find(t => t.id === actionTask.value.id)
    if (t) t.published = true
    toastStore.show('🎉 已上架到发现页')
    actionTask.value = null
  } catch (e) {
    toastStore.show('上架失败: ' + (e.response?.data?.message || e.message || '请重试'))
  }
}

const unpublishFromSheet = async () => {
  if (!actionTask.value) return
  try {
    await musicApi.unpublishTask(actionTask.value.id)
    actionTask.value.published = false
    const t = tasks.value.find(t => t.id === actionTask.value.id)
    if (t) t.published = false
    toastStore.show('已下架')
    actionTask.value = null
  } catch (e) {
    toastStore.show('下架失败: ' + (e.response?.data?.message || e.message || '请重试'))
  }
}

const deleteFromSheet = async () => {
  if (!actionTask.value) return
  const taskId = actionTask.value.id
  actionTask.value = null
  await deleteTask(taskId)
}

// ==================== 轮询（委托给全局 player store） ====================

const startPolling = () => {
  if (!currentTask.value) return

  // ★ 委托给全局 player store，离开页面也能继续轮询
  playerStore.startTaskPolling(currentTask.value.id, async (completedTask) => {
    // 回调：任务完成或失败
    currentTask.value = completedTask
    await loadTasks()
    if (completedTask.status === 'succeeded') {
      toastStore.show('🎉 音乐创作完成！')
    } else if (completedTask.status === 'failed') {
      toastStore.show('❌ 音乐生成失败')
    }
  })
}

const stopPolling = () => {
  playerStore.stopTaskPolling()
}

// ==================== 数据加载 ====================

const loadTasks = async () => {
  try {
    tasks.value = await musicApi.listTasks() || []
  } catch {}
}

// ==================== 工具 ====================

const statusLabel = (s) => ({ processing: '创作中...', streaming: '🎵 边生成边听...', succeeded: '已完成', failed: '失败' }[s] || s)

const formatDuration = (ms) => {
  if (!ms) return ''
  const s = Math.round(ms / 1000)
  return `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')}`
}

const formatTime = (t) => {
  if (!t) return ''
  const d = new Date(t)
  return `${d.getMonth() + 1}/${d.getDate()} ${d.getHours()}:${String(d.getMinutes()).padStart(2, '0')}`
}

// ==================== 生命周期 ====================

onMounted(async () => {
  await loadTasks()
  // 自动恢复轮询：如果有 processing/streaming 状态的任务
  const processingTask = tasks.value.find(t => ['processing', 'streaming'].includes(t.status))
  if (processingTask) {
    currentTask.value = processingTask
    // 只有全局 store 没在轮询时才启动（避免重复轮询）
    if (!playerStore.pollingTaskId) {
      startPolling()
    }
  }
})
// ★ 不再在 onUnmounted 停止轮询，让全局 store 继续工作
onUnmounted(() => { /* polling continues in playerStore */ })
</script>

<style scoped>
.slide-enter-active,
.slide-leave-active {
  transition: all 0.3s ease;
}
.slide-enter-from,
.slide-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

/* 进度条动画 */
.animate-progress {
  animation: progress-sweep 2s ease-in-out infinite;
}
@keyframes progress-sweep {
  0% { width: 10%; }
  50% { width: 80%; }
  100% { width: 10%; }
}

/* 管理弹窗动画 */
.sheet-enter-active,
.sheet-leave-active {
  transition: opacity 0.25s ease;
}
.sheet-enter-active > div:last-child,
.sheet-leave-active > div:last-child {
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.sheet-enter-from { opacity: 0; }
.sheet-enter-from > div:last-child { transform: translateY(100%); }
.sheet-leave-to { opacity: 0; }
.sheet-leave-to > div:last-child { transform: translateY(100%); }
</style>
