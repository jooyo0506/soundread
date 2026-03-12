<template>
  <div class="h-full bg-[#050505] relative flex flex-col pt-14 hide-scrollbar">

    <!-- 顶部导航 -->
    <div class="absolute top-0 left-0 w-full h-14 bg-gradient-to-b from-black/80 to-transparent z-40 flex items-center px-4">
      <button @click="$router.back()" class="text-white text-sm active:opacity-50 transition-opacity">
        <i class="fas fa-chevron-left mr-1"></i> 返回
      </button>
      <h1 class="flex-1 text-center text-white font-bold text-base">合成历史</h1>
      <div class="w-12"></div>
    </div>

    <div class="flex-1 overflow-y-auto px-4 pb-24 z-10 hide-scrollbar">

      <!-- 存储用量 -->
      <div v-if="storage" class="glass-panel rounded-2xl p-4 mb-4">
        <div class="flex justify-between text-xs text-gray-400 mb-2">
          <span>存储: {{ formatBytes(storage.usedBytes) }} / {{ storage.maxBytes === -1 ? '无限' : formatBytes(storage.maxBytes) }}</span>
          <span>{{ storage.creationCount }} 个作品</span>
        </div>
        <div v-if="storage.maxBytes > 0" class="w-full h-1.5 bg-white/5 rounded-full overflow-hidden">
          <div class="h-full bg-gradient-to-r from-[#FF9500] to-[#FFD60A] rounded-full transition-all duration-500"
               :style="{ width: Math.min(100, storage.usedBytes / storage.maxBytes * 100) + '%' }"></div>
        </div>
      </div>

      <!-- 类型筛选 -->
      <div class="flex gap-2 mb-4 overflow-x-auto hide-scrollbar pb-1">
        <button v-for="t in types" :key="t.value"
                @click="currentType = t.value; loadList()"
                :class="[
                  'shrink-0 px-3.5 py-1.5 rounded-full text-xs font-medium transition-all',
                  currentType === t.value
                    ? 'bg-[#FF9500] text-black'
                    : 'bg-white/5 text-gray-400 hover:bg-white/10'
                ]">
          {{ t.label }}
        </button>
      </div>

      <!-- 创作列表 -->
      <div class="space-y-3">
        <div v-for="item in creations" :key="item.id"
             class="glass-panel rounded-2xl p-4">
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 rounded-xl bg-white/5 flex items-center justify-center shrink-0">
              <i :class="typeIcon(item.type)" class="text-sm text-gray-400"></i>
            </div>
            <!-- 信息 -->
            <div class="flex-1 min-w-0">
              <p class="text-white text-sm font-medium truncate">{{ item.title || '未命名作品' }}</p>
              <p class="text-gray-500 text-[10px] mt-0.5">
                {{ typeLabel(item.type) }} · {{ item.audioDuration || 0 }}秒 · {{ formatDate(item.createdAt) }}
                <span v-if="item.isPublished === 1" class="ml-1 text-green-400">· 已发布</span>
              </p>
            </div>
            <!-- 操作按钮 -->
            <div class="flex items-center gap-1.5 shrink-0">
              <button v-if="item.audioUrl" @click="playAudio(item)"
                      class="w-8 h-8 rounded-lg bg-green-500/10 text-green-400 flex items-center justify-center text-xs hover:bg-green-500/20 transition-colors"
                      title="播放">
                <i class="fas fa-play"></i>
              </button>
              <a v-if="item.audioUrl" :href="item.audioUrl" download
                 class="w-8 h-8 rounded-lg bg-blue-500/10 text-blue-400 flex items-center justify-center text-xs hover:bg-blue-500/20 transition-colors"
                 title="下载">
                <i class="fas fa-download"></i>
              </a>
              <!-- 查看对话 (仅播客) -->
              <button v-if="item.type === 'podcast' && getTranscript(item)"
                      @click="toggleTranscript(item.id)"
                      class="w-8 h-8 rounded-lg bg-purple-500/10 text-purple-400 flex items-center justify-center text-xs hover:bg-purple-500/20 transition-colors"
                      title="查看对话">
                <i class="fas fa-comments"></i>
              </button>
              <!-- 发布按钮 -->
              <button v-if="item.audioUrl && item.isPublished !== 1"
                      @click="publishItem(item)"
                      :disabled="publishing === item.id"
                      class="w-8 h-8 rounded-lg bg-[#FF9500]/10 text-[#FF9500] flex items-center justify-center text-xs hover:bg-[#FF9500]/20 transition-colors disabled:opacity-30"
                      title="发布到发现页">
                <i :class="publishing === item.id ? 'fas fa-spinner fa-spin' : 'fas fa-share'"></i>
              </button>
              <button @click="deleteItem(item)"
                      class="w-8 h-8 rounded-lg bg-red-500/10 text-red-400 flex items-center justify-center text-xs hover:bg-red-500/20 transition-colors"
                      title="删除">
                <i class="fas fa-trash"></i>
              </button>
            </div>
          </div>

          <!-- 播客对话内容展开区 -->
          <div v-if="item.type === 'podcast' && expandedTranscript === item.id && getTranscript(item)"
               class="mt-3 pt-3 border-t border-white/5">
            <div class="flex items-center justify-between mb-2">
              <span class="text-[10px] text-gray-500 font-medium">
                <i class="fas fa-comments mr-1"></i>对话记录 ({{ getRoundCount(item) }} 轮)
              </span>
              <button @click="copyTranscript(item)" class="text-[10px] text-cyan-400 hover:text-cyan-300 transition-colors">
                <i class="fas fa-copy mr-0.5"></i>复制
              </button>
            </div>
            <div class="max-h-52 overflow-y-auto text-xs text-gray-300 space-y-1.5 pr-1 hide-scrollbar">
              <div v-for="(line, idx) in getTranscriptLines(item)" :key="idx"
                   class="leading-relaxed">
                <span :class="line.startsWith(getSpeakerA(item)) ? 'text-cyan-400' : 'text-orange-400'" class="font-medium">{{ line.split(': ')[0] }}:</span>
                <span class="text-gray-300"> {{ line.split(': ').slice(1).join(': ') }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-if="!creations.length" class="flex flex-col items-center justify-center py-20 text-gray-600">
          <i class="fas fa-folder-open text-4xl mb-4 opacity-30"></i>
          <p class="text-sm">暂无合成记录</p>
          <p class="text-[10px] text-gray-700 mt-1">合成配音后将自动保存在这里</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { creationApi } from '../api/creation'
import { usePlayerStore } from '../stores/player'
import { useToastStore } from '../stores/toast'

const toast = useToastStore()
const player = usePlayerStore()
const creations = ref([])
const storage = ref(null)
const currentType = ref(null)
const publishing = ref(null)
const expandedTranscript = ref(null)

const types = [
  { value: null, label: '全部' },
  { value: 'tts', label: '配音' },
  { value: 'emotion', label: '情感' },
  { value: 'drama', label: '导演' },
  { value: 'podcast', label: '播客' },
  { value: 'novel', label: '小说' }
]

onMounted(() => { loadList(); loadStorage() })

async function loadList() {
  try {
    const params = { page: 1, size: 50 }
    if (currentType.value) params.type = currentType.value
    const res = await creationApi.list(params)
    creations.value = res.records || res
  } catch (e) { console.warn('加载创作列表失败', e) }
}

async function loadStorage() {
  try { storage.value = await creationApi.getStorage() } catch (e) { console.warn('加载存储信息失败', e) }
}

function playAudio(item) {
  player.play({ title: item.title || '未命名作品', author: typeLabel(item.type), url: item.audioUrl })
}

async function deleteItem(item) {
  if (!confirm('确定删除？删除后将释放存储空间')) return
  try {
    await creationApi.delete(item.id)
    toast.show('已删除')
    loadList()
    loadStorage()
  } catch (e) { toast.show(e.message || '删除失败') }
}

async function publishItem(item) {
  if (!confirm(`确定将 "${item.title || '未命名作品'}" 发布到发现页？`)) return
  publishing.value = item.id
  try {
    await creationApi.publish(item.id, {
      title: item.title || '未命名作品',
      category: item.type === 'podcast' ? 'podcast' : 'latest'
    })
    toast.show('发布成功')
    item.isPublished = 1
  } catch (e) {
    toast.show(e.message || '发布失败')
  } finally {
    publishing.value = null
  }
}

// ===== 播客对话相关 =====

function toggleTranscript(itemId) {
  expandedTranscript.value = expandedTranscript.value === itemId ? null : itemId
}

function getTranscript(item) {
  if (!item.extraJson) return null
  try {
    const extra = typeof item.extraJson === 'string' ? JSON.parse(item.extraJson) : item.extraJson
    return extra.transcript || null
  } catch { return null }
}

function getRoundCount(item) {
  if (!item.extraJson) return 0
  try {
    const extra = typeof item.extraJson === 'string' ? JSON.parse(item.extraJson) : item.extraJson
    return extra.roundCount || 0
  } catch { return 0 }
}

function getTranscriptLines(item) {
  const t = getTranscript(item)
  return t ? t.split('\n').filter(l => l.trim()) : []
}

function getSpeakerA(item) {
  const lines = getTranscriptLines(item)
  if (lines.length > 0) return lines[0].split(':')[0]
  return ''
}

async function copyTranscript(item) {
  const t = getTranscript(item)
  if (!t) return
  try {
    await navigator.clipboard.writeText(t)
    toast.show('对话内容已复制')
  } catch {
    toast.show('复制失败')
  }
}

function typeIcon(type) {
  return { tts: 'fas fa-microphone', emotion: 'fas fa-theater-masks', drama: 'fas fa-film', podcast: 'fas fa-podcast', novel: 'fas fa-book' }[type] || 'fas fa-file-audio'
}
function typeLabel(type) {
  return { tts: '配音', emotion: '情感合成', drama: '剧情导演', podcast: 'AI播客', novel: '有声小说' }[type] || type
}
function formatDate(d) {
  if (!d) return ''
  return new Date(d).toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}
function formatBytes(bytes) {
  if (!bytes) return '0 B'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}
</script>

