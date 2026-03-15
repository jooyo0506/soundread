<template>
  <div class="music-page h-full overflow-y-auto pb-32 hide-scrollbar">

    <!-- ── Hero Banner ── -->
    <div class="hero-banner relative px-5 pt-14 pb-6 overflow-hidden">
      <!-- 动态背景光晕 -->
      <div class="hero-orb hero-orb-1"></div>
      <div class="hero-orb hero-orb-2"></div>

      <button @click="$router.back()"
              class="absolute top-5 left-5 w-8 h-8 rounded-full bg-black/30 backdrop-blur-md flex items-center justify-center text-white/70 hover:text-white transition-colors z-10 cursor-pointer">
        <i class="fas fa-chevron-left text-xs"></i>
      </button>

      <div class="relative z-10 text-center">
        <!-- 旋转图标 -->
        <div class="vinyl-disc mx-auto mb-3">
          <div class="vinyl-shine"></div>
        </div>
        <h1 class="text-2xl font-black text-white tracking-tight">AI 音乐</h1>
        <p class="text-xs text-white/50 mt-1">描述灵感，让 AI 为你谱曲</p>
      </div>
    </div>

    <!-- ── 主创作区 ── -->
    <div class="px-5 space-y-3">

      <!-- 模式选择 -->
      <div class="mode-switcher flex gap-2">
        <button @click="mode = 'instrumental'" :disabled="isProcessing"
                class="flex-1 py-2.5 rounded-xl text-xs font-bold transition-all duration-200 flex items-center justify-center gap-1.5"
                :class="mode === 'instrumental'
                  ? 'bg-gradient-to-r from-fuchsia-600 to-purple-600 text-white shadow-[0_4px_16px_rgba(192,80,240,0.35)]'
                  : 'bg-white/5 text-gray-500 hover:text-white hover:bg-white/8'">
          <i class="fas fa-music text-[10px]"></i> 纯音乐
        </button>
        <button @click="mode = 'song'" :disabled="isProcessing"
                class="flex-1 py-2.5 rounded-xl text-xs font-bold transition-all duration-200 flex items-center justify-center gap-1.5"
                :class="mode === 'song'
                  ? 'bg-gradient-to-r from-fuchsia-600 to-purple-600 text-white shadow-[0_4px_16px_rgba(192,80,240,0.35)]'
                  : 'bg-white/5 text-gray-500 hover:text-white hover:bg-white/8'">
          <i class="fas fa-microphone text-[10px]"></i> 带人声
        </button>
      </div>

      <!-- 提示词输入卡 -->
      <div class="input-card">
        <div class="flex items-center gap-2 mb-3">
          <i class="fas fa-wand-magic-sparkles text-fuchsia-400 text-xs"></i>
          <span class="text-xs font-bold text-gray-300">描述你的音乐</span>
          <span class="ml-auto text-[10px] text-gray-600">{{ prompt.length }}/500</span>
        </div>

        <textarea
          v-model="prompt"
          placeholder="例如：r&b, slow, passionate, midnight, melancholy..."
          class="w-full h-[90px] bg-transparent text-sm text-white placeholder-gray-700 resize-none focus:outline-none leading-relaxed hide-scrollbar"
          maxlength="500"
        ></textarea>

        <!-- 风格标签 -->
        <div class="flex flex-wrap gap-1.5 mt-3 pt-3 border-t border-white/5">
          <button v-for="tag in promptTags" :key="tag" @click="toggleTag(tag)"
                  class="genre-tag"
                  :class="hasTag(tag) ? 'genre-tag--active' : ''">
            {{ tagEmoji[tag] || '' }} {{ tag }}
          </button>
        </div>
      </div>

      <!-- 歌词区 (歌曲模式) -->
      <Transition name="slide-down">
        <div v-if="mode === 'song'" class="input-card">
          <div class="flex items-center gap-2 mb-3">
            <i class="fas fa-pen-fancy text-purple-400 text-xs"></i>
            <span class="text-xs font-bold text-gray-300">歌词</span>
            <button @click="aiGenerateLyrics" :disabled="lyricsLoading || !prompt.trim()"
                    class="ml-auto text-[10px] font-bold text-fuchsia-400 bg-fuchsia-500/15 px-2.5 py-1 rounded-full hover:bg-fuchsia-500/25 transition-colors disabled:opacity-30 cursor-pointer flex items-center gap-1">
              <i class="fas fa-wand-magic-sparkles text-[9px]" :class="lyricsLoading ? 'fa-spin' : ''"></i>
              {{ lyricsLoading ? '生成中...' : 'AI 写词' }}
            </button>
          </div>
          <textarea
            v-model="lyrics"
            placeholder="[Verse]&#10;在暴风雨的夜晚&#10;想着你的温柔笑脸&#10;&#10;[Chorus]&#10;..."
            class="w-full h-[130px] bg-transparent text-sm text-white placeholder-gray-700 resize-none focus:outline-none font-mono leading-relaxed hide-scrollbar"
          ></textarea>

          <!-- AI 自动回填的歌名（生成歌词后展示） -->
          <Transition name="slide-down">
            <div v-if="songTitle" class="flex items-center gap-2 pt-2.5 mt-2.5 border-t border-fuchsia-500/20">
              <span class="text-[10px] shrink-0 text-fuchsia-400 font-bold">AI 歌名</span>
              <input v-model="songTitle" maxlength="50"
                     class="flex-1 text-sm font-bold text-white bg-transparent focus:outline-none placeholder-gray-600 min-w-0"
                     placeholder="歌名" />
              <span class="text-[10px] text-gray-600 shrink-0">可修改</span>
            </div>
          </Transition>
        </div>
      </Transition>

      <!-- 生成按钮 -->
      <button @click="startGenerate"
              :disabled="generating || isProcessing || !prompt.trim() || (mode === 'song' && !lyrics.trim())"
              class="generate-btn w-full py-4 rounded-2xl font-black text-base transition-all duration-200 disabled:opacity-40 disabled:cursor-not-allowed">
        <template v-if="generating || (currentTask && currentTask.status === 'processing')">
          <span class="waveform-bars mr-2">
            <span></span><span></span><span></span><span></span><span></span>
          </span>
          AI 正在创作...
        </template>
        <template v-else>
          <i class="fas fa-sparkles mr-2 text-sm"></i>
          {{ mode === 'song' ? '生成歌曲' : '生成纯音乐' }}
        </template>
      </button>

      <!-- 生成进度卡 -->
      <Transition name="slide-down">
        <div v-if="currentTask && currentTask.status === 'processing'"
             class="progress-card flex items-center gap-3">
          <div class="progress-vinyl">
            <div class="inner"></div>
          </div>
          <div class="flex-1">
            <p class="text-xs font-bold text-fuchsia-300">AI 创作中，通常需要 1-2 分钟</p>
            <p class="text-[10px] text-gray-600 mt-0.5 truncate">{{ currentTask.title }}</p>
            <div class="progress-bar mt-2">
              <div class="progress-fill"></div>
            </div>
          </div>
        </div>
      </Transition>

      <!-- 完成提示 -->
      <Transition name="slide-down">
        <div v-if="currentTask && currentTask.status === 'succeeded'"
             class="success-card flex items-center justify-between px-4 py-3 rounded-2xl">
          <div class="flex items-center gap-2">
            <div class="w-6 h-6 rounded-full bg-green-500/20 flex items-center justify-center">
              <i class="fas fa-check text-green-400 text-[10px]"></i>
            </div>
            <span class="text-sm font-bold text-green-400">创作完成</span>
          </div>
          <button @click="playResult(currentTask)"
                  class="text-xs font-bold text-white bg-green-500/80 px-3 py-1.5 rounded-full hover:bg-green-500 transition-colors cursor-pointer flex items-center gap-1.5 shadow-[0_2px_10px_rgba(34,197,94,0.3)]">
            <i class="fas fa-play text-[9px]"></i> 立即播放
          </button>
        </div>
      </Transition>

      <!-- 失败提示 -->
      <Transition name="slide-down">
        <div v-if="currentTask && currentTask.status === 'failed'"
             class="fail-card flex items-center gap-2 px-4 py-3 rounded-2xl">
          <i class="fas fa-exclamation-triangle text-red-400 text-xs"></i>
          <span class="text-xs text-red-400">{{ currentTask.errorMsg || '生成失败，请重试' }}</span>
        </div>
      </Transition>

      <!-- ── 我的作品 ── -->
      <div v-if="tasks.length > 0" class="mt-2">
        <div class="flex items-center justify-between mb-3">
          <h3 class="text-sm font-black text-white flex items-center gap-2">
            <i class="fas fa-compact-disc text-fuchsia-400 text-xs"></i>
            我的作品
            <span class="text-[10px] text-gray-600 font-normal bg-white/5 px-1.5 py-0.5 rounded-full">{{ tasks.length }}</span>
          </h3>
          <button v-if="tasks.length > 1" @click="showAllTasks = !showAllTasks"
                  class="text-[10px] text-fuchsia-400 hover:text-fuchsia-300 transition-colors cursor-pointer flex items-center gap-1">
            {{ showAllTasks ? '收起' : '查看全部' }}
            <i class="fas" :class="showAllTasks ? 'fa-chevron-up' : 'fa-chevron-right'" style="font-size: 8px"></i>
          </button>
        </div>

        <div class="space-y-2">
          <div v-for="task in displayedTasks" :key="task.id"
               class="track-card group"
               :class="{ 'track-card--processing': task.status === 'processing' }">

            <!-- 封面 -->
            <div class="track-cover"
                 :class="{
                   'track-cover--fuchsia': task.status === 'succeeded',
                   'track-cover--amber': task.status === 'processing',
                   'track-cover--red': task.status === 'failed'
                 }"
                 @click="task.status === 'succeeded' && playResult(task)">
              <i v-if="task.status === 'succeeded'" class="fas fa-music text-fuchsia-300 text-sm"></i>
              <i v-else-if="task.status === 'processing'" class="fas fa-circle-notch fa-spin text-amber-400 text-sm"></i>
              <i v-else class="fas fa-times text-red-400 text-sm"></i>
            </div>

            <!-- 信息 -->
            <div class="flex-1 min-w-0 py-1">
              <div class="flex items-center gap-1.5 mb-0.5">
                <h4 class="text-white text-sm font-bold truncate flex-1">{{ task.title || 'AI 音乐' }}</h4>
                <span v-if="task.published" class="shrink-0 text-[8px] font-bold text-green-400 bg-green-500/15 px-1.5 py-0.5 rounded-full flex items-center gap-0.5">
                  <span class="w-1 h-1 rounded-full bg-green-400 animate-pulse"></span>已上架
                </span>
              </div>
              <p class="text-[10px] text-gray-500 flex items-center gap-2">
                <span>{{ task.taskType === 'song' ? '歌曲' : '纯音乐' }}</span>
                <span v-if="task.duration">{{ formatDuration(task.duration) }}</span>
                <span>{{ formatTime(task.createdAt) }}</span>
              </p>
            </div>

            <!-- 操作 -->
            <div class="flex items-center gap-1.5 shrink-0">
              <button v-if="task.status === 'succeeded'" @click.stop="playResult(task)"
                      class="w-8 h-8 rounded-full bg-fuchsia-500/20 flex items-center justify-center text-fuchsia-400 hover:bg-fuchsia-500/35 hover:scale-110 transition-all cursor-pointer">
                <i class="fas fa-play text-[10px] pl-0.5"></i>
              </button>
              <button v-if="task.status === 'succeeded'" @click.stop="openMusicAction(task)"
                      class="w-8 h-8 rounded-full bg-white/5 flex items-center justify-center text-gray-500 hover:text-white hover:bg-white/10 transition-all cursor-pointer">
                <i class="fas fa-ellipsis-h text-[10px]"></i>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else class="empty-state text-center py-10">
        <div class="w-16 h-16 rounded-full bg-fuchsia-500/10 flex items-center justify-center mx-auto mb-3">
          <i class="fas fa-headphones text-fuchsia-500/60 text-2xl"></i>
        </div>
        <p class="text-sm text-gray-600">创作你的第一首 AI 音乐</p>
        <p class="text-xs text-gray-700 mt-1">输入描述，一键生成专属音乐</p>
      </div>
    </div>

    <!-- ── 管理弹窗 ── -->
    <Teleport to="body">
      <Transition name="sheet">
        <div v-if="actionTask" class="fixed inset-0 z-50 flex items-end justify-center" style="max-width: 430px; margin: 0 auto;">
          <div class="absolute inset-0 bg-black/70 backdrop-blur-sm" @click="actionTask = null"></div>
          <div class="relative w-full bg-[#161616] rounded-t-3xl border-t border-white/8 p-5 pb-10">
            <div class="w-10 h-1 rounded-full bg-white/10 mx-auto mb-5"></div>

            <!-- 标题编辑 -->
            <div class="mb-4">
              <label class="text-[10px] text-gray-500 mb-1.5 block font-bold uppercase tracking-wider">作品名称</label>
              <div class="flex items-center gap-2">
                <input v-model="editTitle" maxlength="30"
                       class="flex-1 bg-white/5 border border-white/8 rounded-xl px-4 py-2.5 text-sm text-white placeholder-gray-600 focus:border-fuchsia-500/40 focus:outline-none transition-colors"
                       placeholder="输入作品名称" />
                <button @click="saveRename" :disabled="renaming"
                        class="px-4 py-2.5 rounded-xl text-xs font-bold cursor-pointer transition-all"
                        :class="renaming ? 'bg-gray-700 text-gray-500' : 'bg-fuchsia-500/25 text-fuchsia-400 hover:bg-fuchsia-500/40'">
                  <i v-if="renaming" class="fas fa-circle-notch fa-spin"></i>
                  <span v-else>保存</span>
                </button>
              </div>
            </div>

            <div class="space-y-2">
              <button v-if="!actionTask.published" @click="publishFromSheet"
                      class="action-btn action-btn--green">
                <i class="fas fa-cloud-upload-alt w-5 text-center text-sm"></i>
                <div class="text-left">
                  <div class="text-xs font-bold">上架到发现页</div>
                  <div class="text-[10px] text-gray-500">让更多人听到你的音乐</div>
                </div>
              </button>
              <button v-if="actionTask.published" @click="unpublishFromSheet"
                      class="action-btn action-btn--amber">
                <i class="fas fa-eye-slash w-5 text-center text-sm"></i>
                <div class="text-left">
                  <div class="text-xs font-bold">下架作品</div>
                  <div class="text-[10px] text-gray-500">从发现页移除</div>
                </div>
              </button>
              <button @click="deleteFromSheet" class="action-btn action-btn--red">
                <i class="fas fa-trash-alt w-5 text-center text-sm"></i>
                <div class="text-left">
                  <div class="text-xs font-bold">删除作品</div>
                  <div class="text-[10px] text-gray-500">永久删除此音乐</div>
                </div>
              </button>
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
import { useLoginGuard } from '../composables/useLoginGuard'
import { useAuthStore } from '../stores/auth'

const playerStore = usePlayerStore()
const toastStore = useToastStore()
const { requireLogin } = useLoginGuard()
const authStore = useAuthStore()

const mode = ref('instrumental')
const prompt = ref('')
const lyrics = ref('')
const songTitle = ref('')       // AI 生成或手动输入的歌名
const selectedModel = ref('mureka-7.6')
const generating = ref(false)
const lyricsLoading = ref(false)
const currentTask = ref(null)
const tasks = ref([])
const showAllTasks = ref(false)

const isProcessing = computed(() =>
  ['processing', 'streaming'].includes(currentTask.value?.status) || generating.value
)

const displayedTasks = computed(() =>
  showAllTasks.value ? tasks.value : tasks.value.slice(0, 1)
)

const promptTags = ['pop', 'r&b', 'rock', 'jazz', 'classical', 'electronic', 'hip-hop', 'folk', 'Chinese', 'slow', 'energetic']
const tagEmoji = {}

const parseTags = () => prompt.value.split(/[,，]/).map(s => s.trim()).filter(Boolean)
const hasTag = (tag) => parseTags().some(t => t.toLowerCase() === tag.toLowerCase())
const toggleTag = (tag) => {
  const tags = parseTags()
  const idx = tags.findIndex(t => t.toLowerCase() === tag.toLowerCase())
  if (idx >= 0) { tags.splice(idx, 1) } else { tags.push(tag) }
  prompt.value = tags.join(', ')
}

const startGenerate = async () => {
  if (requireLogin('使用AI音乐生成')) return
  generating.value = true
  try {
    const res = await musicApi.generate({
      type: mode.value,
      prompt: prompt.value,
      lyrics: mode.value === 'song' ? lyrics.value : undefined,
      model: selectedModel.value,
      title: songTitle.value || undefined    // 传入 AI 生成的歌名
    })
    const displayTitle = songTitle.value || prompt.value.substring(0, 20)
    currentTask.value = { id: res.taskId, status: res.status, title: displayTitle }
    toastStore.show('任务已提交，AI 正在创作中...')
    startPolling()
    loadTasks()
  } catch (e) {
    toastStore.show('提交失败: ' + (e.message || '请重试'))
  } finally {
    generating.value = false
  }
}

const aiGenerateLyrics = async () => {
  if (requireLogin('使用AI写词')) return
  lyricsLoading.value = true
  try {
    const res = await musicApi.generateLyrics(prompt.value)
    lyrics.value = res.lyrics || ''
    // 自动回填 AI 生成的歌名
    if (res.title) {
      songTitle.value = res.title
      toastStore.show('歌词和歌名已生成')
    } else {
      toastStore.show('歌词生成完成')
    }
  } catch (e) {
    toastStore.show('歌词生成失败: ' + (e.message || '请重试'))
  } finally {
    lyricsLoading.value = false
  }
}

const playResult = async (task) => {
  if (!task.resultUrl) return
  let freshTask = task
  if (!task.lyricTimings && task.taskType === 'song' && task.id) {
    try {
      freshTask = await musicApi.getTask(task.id)
      const idx = tasks.value.findIndex(t => t.id === task.id)
      if (idx !== -1) tasks.value[idx] = { ...tasks.value[idx], ...freshTask }
    } catch { }
  }
  let lyricTimings = null
  if (freshTask.lyricTimings) {
    try {
      lyricTimings = typeof freshTask.lyricTimings === 'string'
        ? JSON.parse(freshTask.lyricTimings) : freshTask.lyricTimings
    } catch { }
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
  } catch { }
}

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
    const t = tasks.value.find(t => t.id === actionTask.value.id)
    if (t) t.title = editTitle.value.trim()
    actionTask.value.title = editTitle.value.trim()
    toastStore.show('已重命名')
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
    toastStore.show('已上架到发现页')
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

const startPolling = () => {
  if (!currentTask.value) return
  playerStore.startTaskPolling(currentTask.value.id, async (completedTask) => {
    currentTask.value = completedTask
    await loadTasks()
    if (completedTask.status === 'succeeded') {
      toastStore.show('音乐创作完成')
    } else if (completedTask.status === 'failed') {
      toastStore.show('音乐生成失败')
    }
  })
}

const loadTasks = async () => {
  try { tasks.value = await musicApi.listTasks() || [] } catch { }
}

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

onMounted(async () => {
  if (!authStore.isLoggedIn) return  // 游客模式：跳过加载个人任务列表
  await loadTasks()
  const processingTask = tasks.value.find(t => ['processing', 'streaming'].includes(t.status))
  if (processingTask) {
    currentTask.value = processingTask
    if (!playerStore.pollingTaskId) startPolling()
  }
})
onUnmounted(() => { })
</script>

<style scoped>
/* ── Page ── */
.music-page {
  background: #0a0a0f;
}

/* ── Hero ── */
.hero-banner {
  background: linear-gradient(180deg, #1a0a2e 0%, #0a0a0f 100%);
}
.hero-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  pointer-events: none;
}
.hero-orb-1 {
  width: 200px; height: 200px;
  background: rgba(192, 80, 240, 0.18);
  top: -40px; right: -40px;
  animation: float1 6s ease-in-out infinite;
}
.hero-orb-2 {
  width: 160px; height: 160px;
  background: rgba(109, 40, 217, 0.15);
  bottom: -20px; left: -20px;
  animation: float2 8s ease-in-out infinite;
}
@keyframes float1 { 0%,100%{transform:translate(0,0)} 50%{transform:translate(-15px,10px)} }
@keyframes float2 { 0%,100%{transform:translate(0,0)} 50%{transform:translate(10px,-8px)} }

/* ── 黑胶唱片 ── */
.vinyl-disc {
  width: 60px; height: 60px;
  border-radius: 50%;
  background: conic-gradient(
    from 0deg,
    #1a1a2e, #2d1b4e, #3b1f6e, #2d1b4e, #1a1a2e,
    #2a0a44, #3f1470, #2a0a44, #1a1a2e
  );
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 0 30px rgba(192, 80, 240, 0.25), 0 0 60px rgba(109, 40, 217, 0.15);
  animation: vinyl-spin 8s linear infinite;
  position: relative;
}
.vinyl-disc::after {
  content: '';
  position: absolute;
  width: 16px; height: 16px;
  border-radius: 50%;
  background: #0a0a0f;
  box-shadow: 0 0 0 2px rgba(192, 80, 240, 0.4);
}
.vinyl-shine {
  position: absolute;
  inset: 4px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(255,255,255,0.08) 0%, transparent 60%);
  pointer-events: none;
}
@keyframes vinyl-spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* ── Mode Switcher ── */
.mode-switcher button {
  border: 1px solid transparent;
}
.mode-switcher button:not([class*="from-fuchsia"]) {
  border-color: rgba(255,255,255,0.05);
}

/* ── Input Card ── */
.input-card {
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.07);
  border-radius: 1.25rem;
  padding: 1rem;
  backdrop-filter: blur(10px);
}

/* ── Genre Tags ── */
.genre-tag {
  font-size: 11px;
  padding: 5px 12px;
  border-radius: 9999px;
  border: 1px solid rgba(192, 80, 240, 0.2);
  color: rgba(192, 80, 240, 0.8);
  background: rgba(192, 80, 240, 0.06);
  transition: all 0.15s ease;
  cursor: pointer;
  white-space: nowrap;
}
.genre-tag:hover {
  background: rgba(192, 80, 240, 0.15);
  border-color: rgba(192, 80, 240, 0.4);
  color: #d07aef;
}
.genre-tag--active {
  background: rgba(192, 80, 240, 0.2);
  border-color: rgba(192, 80, 240, 0.6);
  color: #e4a8f7;
  font-weight: 700;
  box-shadow: 0 0 8px rgba(192, 80, 240, 0.2);
}

/* ── Generate Button ── */
.generate-btn {
  background: linear-gradient(135deg, #c050f0 0%, #7c3aed 100%);
  color: white;
  box-shadow: 0 6px 24px rgba(192, 80, 240, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border: none;
  letter-spacing: 0.01em;
}
.generate-btn:not(:disabled):hover {
  box-shadow: 0 8px 32px rgba(192, 80, 240, 0.5);
  transform: translateY(-1px);
}
.generate-btn:not(:disabled):active {
  transform: scale(0.98);
}

/* ── Waveform bars (生成中动画) ── */
.waveform-bars {
  display: inline-flex; align-items: center; gap: 2px; height: 18px;
}
.waveform-bars span {
  display: block;
  width: 3px; border-radius: 2px;
  background: rgba(255,255,255,0.8);
}
.waveform-bars span:nth-child(1) { height: 8px; animation: wave 1s ease-in-out infinite 0s; }
.waveform-bars span:nth-child(2) { height: 14px; animation: wave 1s ease-in-out infinite 0.15s; }
.waveform-bars span:nth-child(3) { height: 18px; animation: wave 1s ease-in-out infinite 0.3s; }
.waveform-bars span:nth-child(4) { height: 14px; animation: wave 1s ease-in-out infinite 0.45s; }
.waveform-bars span:nth-child(5) { height: 8px; animation: wave 1s ease-in-out infinite 0.6s; }
@keyframes wave {
  0%,100% { transform: scaleY(1); }
  50% { transform: scaleY(0.4); }
}

/* ── Progress Card ── */
.progress-card {
  background: rgba(192, 80, 240, 0.08);
  border: 1px solid rgba(192, 80, 240, 0.15);
  border-radius: 1.25rem;
  padding: 1rem;
}
.progress-vinyl {
  width: 40px; height: 40px;
  border-radius: 50%;
  border: 2px solid rgba(192, 80, 240, 0.4);
  display: flex; align-items: center; justify-content: center;
  animation: vinyl-spin 3s linear infinite;
  flex-shrink: 0;
}
.progress-vinyl .inner {
  width: 12px; height: 12px;
  border-radius: 50%;
  background: rgba(192, 80, 240, 0.6);
}
.progress-bar {
  height: 3px;
  background: rgba(255,255,255,0.06);
  border-radius: 9999px;
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  border-radius: 9999px;
  background: linear-gradient(90deg, #c050f0, #7c3aed);
  animation: progress-sweep 2s ease-in-out infinite;
}
@keyframes progress-sweep {
  0% { width: 10%; }
  50% { width: 85%; }
  100% { width: 10%; }
}

/* ── Status Cards ── */
.success-card {
  background: rgba(34, 197, 94, 0.08);
  border: 1px solid rgba(34, 197, 94, 0.18);
}
.fail-card {
  background: rgba(239, 68, 68, 0.08);
  border: 1px solid rgba(239, 68, 68, 0.18);
}

/* ── Track Card ── */
.track-card {
  display: flex;
  align-items: center;
  gap: 12px;
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.06);
  border-radius: 1rem;
  padding: 10px 12px;
  transition: border-color 0.2s, background 0.2s;
}
.track-card:hover {
  border-color: rgba(192, 80, 240, 0.2);
  background: rgba(192, 80, 240, 0.05);
}
.track-card--processing {
  border-color: rgba(251, 191, 36, 0.2);
}
.track-cover {
  width: 44px; height: 44px;
  border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
  cursor: pointer;
  transition: transform 0.2s;
}
.track-cover:hover { transform: scale(1.08); }
.track-cover--fuchsia {
  background: linear-gradient(135deg, rgba(192,80,240,0.25), rgba(124,58,237,0.25));
  border: 1px solid rgba(192,80,240,0.2);
}
.track-cover--amber {
  background: rgba(251,191,36,0.1);
  border: 1px solid rgba(251,191,36,0.2);
}
.track-cover--red {
  background: rgba(239,68,68,0.1);
  border: 1px solid rgba(239,68,68,0.2);
}

/* ── Action Sheet Buttons ── */
.action-btn {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.15s;
  text-align: left;
}
.action-btn--green { color: #4ade80; background: rgba(34,197,94,0.08); }
.action-btn--green:hover { background: rgba(34,197,94,0.14); }
.action-btn--amber { color: #fbbf24; background: rgba(251,191,36,0.08); }
.action-btn--amber:hover { background: rgba(251,191,36,0.14); }
.action-btn--red { color: #f87171; background: rgba(239,68,68,0.08); }
.action-btn--red:hover { background: rgba(239,68,68,0.14); }

/* ── Transitions ── */
.slide-down-enter-active, .slide-down-leave-active {
  transition: all 0.3s ease;
}
.slide-down-enter-from, .slide-down-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* Sheet */
.sheet-enter-active, .sheet-leave-active { transition: opacity 0.25s ease; }
.sheet-enter-active > div:last-child, .sheet-leave-active > div:last-child {
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.sheet-enter-from { opacity: 0; }
.sheet-enter-from > div:last-child { transform: translateY(100%); }
.sheet-leave-to { opacity: 0; }
.sheet-leave-to > div:last-child { transform: translateY(100%); }

/* scrollbar */
.hide-scrollbar { -ms-overflow-style: none; scrollbar-width: none; }
.hide-scrollbar::-webkit-scrollbar { display: none; }
</style>
