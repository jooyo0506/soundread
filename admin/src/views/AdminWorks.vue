<template>
  <div class="p-6 lg:p-8">
    <!-- Header -->
    <div class="flex items-center justify-between mb-6">
      <div>
        <h2 class="text-lg font-bold text-white">作品管理</h2>
        <p class="text-xs text-gray-500 mt-0.5">审核 · 上下架 · 精选推荐</p>
      </div>
      <button @click="fetchStats" class="px-3 py-1.5 rounded-lg bg-white/5 border border-white/10 text-gray-400 text-xs hover:bg-white/10 transition-colors cursor-pointer flex items-center gap-1.5">
        <i class="fas fa-sync-alt"></i> 刷新统计
      </button>
    </div>

    <!-- Stats Cards -->
    <div class="grid grid-cols-5 gap-3 mb-6">
      <div v-for="card in statCards" :key="card.key"
           class="bg-[#1a1a1c] border border-white/5 rounded-xl px-4 py-3 text-center cursor-pointer transition-all hover:border-white/15"
           :class="{ 'border-[#FF9500]/40 bg-[#FF9500]/5': activeFilter === card.filter }"
           @click="setFilter(card.filter)">
        <div class="text-xl font-bold font-mono" :class="card.color">{{ stats[card.key] ?? '-' }}</div>
        <div class="text-[10px] text-gray-500 mt-0.5">{{ card.label }}</div>
      </div>
    </div>

    <!-- Filters -->
    <div class="flex items-center gap-3 mb-5">
      <div class="flex bg-[#1a1a1c] border border-white/5 rounded-xl overflow-hidden">
        <button v-for="tab in statusTabs" :key="tab.value"
                @click="filters.reviewStatus = tab.value; loadWorks()"
                class="px-4 py-2 text-xs transition-colors cursor-pointer"
                :class="filters.reviewStatus === tab.value ? 'bg-[#FF9500] text-black font-bold' : 'text-gray-400 hover:text-white'">
          {{ tab.label }}
        </button>
      </div>
      <select v-model="filters.sourceType" @change="loadWorks()"
              class="bg-[#1a1a1c] border border-white/5 rounded-xl px-3 py-2 text-xs text-gray-300 outline-none cursor-pointer w-[140px]">
        <option value="">全部类型</option>
        <option value="radio">🌙 情感电台</option>
        <option value="lecture">📚 知识讲解</option>
        <option value="ad">🛒 带货文案</option>
        <option value="picture_book">🎨 有声绘本</option>
        <option value="news">📰 新闻播报</option>
        <option value="drama">🎭 短剧</option>
        <option value="podcast">🎙️ 播客</option>
        <option value="novel">📖 AI小说</option>
        <option value="tts">🔊 TTS</option>
        <option value="emotion">💖 情感合成</option>
        <option value="ai_music">🎵 AI音乐</option>
      </select>
      <div class="flex-1 relative">
        <i class="fas fa-search absolute left-3 top-1/2 -translate-y-1/2 text-gray-500 text-xs"></i>
        <input v-model="filters.keyword" @keyup.enter="loadWorks()"
               placeholder="搜索标题..."
               class="w-full bg-[#1a1a1c] border border-white/5 rounded-xl pl-9 pr-3 py-2 text-xs text-white outline-none focus:border-[#FF9500]/50" />
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="text-center py-20 text-gray-500">
      <i class="fas fa-circle-notch fa-spin text-xl mb-2"></i>
      <p class="text-xs">加载中...</p>
    </div>

    <!-- Empty -->
    <div v-else-if="works.length === 0" class="text-center py-20 text-gray-500">
      <i class="fas fa-inbox text-2xl mb-2"></i>
      <p class="text-xs">暂无作品</p>
    </div>

    <!-- Table -->
    <div v-else>
      <div class="bg-[#1a1a1c] border border-white/5 rounded-2xl overflow-hidden">
        <!-- Header -->
        <div class="grid grid-cols-[1fr_100px_90px_90px_70px_70px_70px_150px_200px] gap-2 px-5 py-3 text-[11px] text-gray-500 font-bold border-b border-white/5 bg-white/[0.02]">
          <span>作品标题</span>
          <span>类型</span>
          <span>审核状态</span>
          <span>上架状态</span>
          <span>播放</span>
          <span>点赞</span>
          <span>热度</span>
          <span>创建时间</span>
          <span class="text-right">操作</span>
        </div>
        <!-- Rows -->
        <div v-for="work in works" :key="work.id"
             class="grid grid-cols-[1fr_100px_90px_90px_70px_70px_70px_150px_200px] gap-2 px-5 py-3 items-center border-b border-white/[0.03] hover:bg-white/[0.02] transition-colors text-xs">
          <div class="min-w-0">
            <div class="font-bold text-white truncate">{{ work.title || '无标题' }}</div>
            <div class="text-[10px] text-gray-600 mt-0.5">UID: {{ work.userId }}</div>
          </div>
          <span class="text-gray-400 text-[11px]">{{ sourceLabel(work.sourceType) }}</span>
          <div>
            <span class="text-[10px] px-2 py-0.5 rounded-full font-bold" :class="reviewBadge(work.reviewStatus)">{{ reviewLabel(work.reviewStatus) }}</span>
          </div>
          <div>
            <span class="text-[10px] px-2 py-0.5 rounded-full font-bold"
                  :class="work.status === 'published' ? 'bg-emerald-500/20 text-emerald-400' : 'bg-gray-500/20 text-gray-400'">
              {{ work.status === 'published' ? '已上架' : '已下架' }}
            </span>
            <span v-if="work.isFeatured" class="text-[10px] px-1.5 py-0.5 rounded-full bg-[#FFD60A]/20 text-[#FFD60A] font-bold ml-1">⭐</span>
          </div>
          <span class="text-gray-400 font-mono">{{ work.playCount || 0 }}</span>
          <span class="text-gray-400 font-mono">{{ work.likeCount || 0 }}</span>
          <span class="text-gray-400 font-mono">{{ work.heatScore || '-' }}</span>
          <span class="text-gray-500 text-[11px]">{{ formatTime(work.createdAt) }}</span>
          <div class="flex gap-1 justify-end flex-wrap">
            <button v-if="work.audioUrl" @click="playAudio(work)" class="px-2 py-1 rounded-md bg-white/5 text-gray-300 text-[10px] hover:bg-white/10 transition-colors cursor-pointer" title="试听">
              <i class="fas fa-headphones"></i>
            </button>
            <button v-if="work.reviewStatus === 'pending'" @click="reviewAction(work.id, 'approve')" class="px-2 py-1 rounded-md bg-green-500/10 text-green-400 text-[10px] hover:bg-green-500/20 transition-colors cursor-pointer font-bold" title="通过">
              <i class="fas fa-check"></i>
            </button>
            <button v-if="work.reviewStatus === 'pending'" @click="openReject(work.id)" class="px-2 py-1 rounded-md bg-red-500/10 text-red-400 text-[10px] hover:bg-red-500/20 transition-colors cursor-pointer font-bold" title="拒绝">
              <i class="fas fa-times"></i>
            </button>
            <button @click="toggleFeatured(work.id)" class="px-2 py-1 rounded-md text-[10px] transition-colors cursor-pointer"
                    :class="work.isFeatured ? 'bg-[#FFD60A]/10 text-[#FFD60A]' : 'bg-white/5 text-gray-400 hover:bg-white/10'"
                    :title="work.isFeatured ? '取消精选' : '精选'">
              <i class="fas fa-star"></i>
            </button>
            <button v-if="work.reviewStatus === 'approved'" @click="togglePublish(work)" class="px-2 py-1 rounded-md text-[10px] transition-colors cursor-pointer"
                    :class="work.status === 'published' ? 'bg-orange-500/10 text-orange-400' : 'bg-emerald-500/10 text-emerald-400'"
                    :title="work.status === 'published' ? '下架' : '上架'">
              <i :class="work.status === 'published' ? 'fas fa-eye-slash' : 'fas fa-eye'"></i>
            </button>
            <button @click="deleteWork(work.id)" class="px-2 py-1 rounded-md bg-white/5 text-gray-500 text-[10px] hover:bg-red-500/10 hover:text-red-400 transition-colors cursor-pointer" title="删除">
              <i class="fas fa-trash"></i>
            </button>
          </div>
        </div>
      </div>

      <!-- Pagination -->
      <div class="flex justify-between items-center pt-4">
        <span class="text-xs text-gray-500">共 {{ totalRecords }} 条</span>
        <div class="flex items-center gap-2">
          <button @click="changePage(-1)" :disabled="filters.page <= 1" class="px-4 py-1.5 rounded-lg bg-white/5 text-gray-400 text-xs disabled:opacity-30 cursor-pointer hover:bg-white/10">上一页</button>
          <span class="text-xs text-gray-400">{{ filters.page }} / {{ totalPages }}</span>
          <button @click="changePage(1)" :disabled="filters.page >= totalPages" class="px-4 py-1.5 rounded-lg bg-white/5 text-gray-400 text-xs disabled:opacity-30 cursor-pointer hover:bg-white/10">下一页</button>
        </div>
      </div>
    </div>

    <!-- Reject Modal -->
    <div v-if="rejectVisible" class="fixed inset-0 bg-black/70 backdrop-blur-sm z-50 flex items-center justify-center" @click.self="rejectVisible = false">
      <div class="bg-[#111] border border-white/10 rounded-2xl w-full max-w-md p-6">
        <h3 class="text-sm font-bold mb-3">拒绝原因</h3>
        <textarea v-model="rejectNote" rows="3" placeholder="请输入拒绝原因..."
                  class="w-full bg-white/5 border border-white/10 rounded-xl px-3 py-2 text-sm text-white outline-none focus:border-red-400/50 resize-none"></textarea>
        <div class="flex justify-end gap-2 mt-4">
          <button @click="rejectVisible = false" class="px-4 py-2 rounded-lg bg-white/5 text-gray-400 text-xs cursor-pointer hover:bg-white/10">取消</button>
          <button @click="confirmReject" class="px-4 py-2 rounded-lg bg-red-500 text-white text-xs font-bold cursor-pointer hover:bg-red-600">确认拒绝</button>
        </div>
      </div>
    </div>

    <!-- Delete Confirm Modal -->
    <div v-if="deleteVisible" class="fixed inset-0 bg-black/70 backdrop-blur-sm z-50 flex items-center justify-center" @click.self="deleteVisible = false">
      <div class="bg-[#111] border border-white/10 rounded-2xl w-full max-w-sm p-6">
        <h3 class="text-sm font-bold mb-2 text-white flex items-center gap-2"><i class="fas fa-exclamation-triangle text-red-400"></i> 确认删除</h3>
        <p class="text-xs text-gray-400 mb-5">删除后无法恢复，确定要删除这条作品吗？</p>
        <div class="flex justify-end gap-2">
          <button @click="deleteVisible = false" class="px-4 py-2 rounded-lg bg-white/5 text-gray-400 text-xs cursor-pointer hover:bg-white/10">取消</button>
          <button @click="confirmDelete" class="px-4 py-2 rounded-lg bg-red-500 text-white text-xs font-bold cursor-pointer hover:bg-red-600">确认删除</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { adminApi } from '../api/admin'
import { useToastStore } from '../stores/toast'

const toastStore = useToastStore()

const loading = ref(true)
const works = ref([])
const totalRecords = ref(0)
const stats = ref({})
const rejectVisible = ref(false)
const rejectNote = ref('')
const rejectTargetId = ref(null)
const deleteVisible = ref(false)
const deleteTargetId = ref(null)
const activeFilter = ref(null)

// 简易音频播放
const audioEl = ref(null)

const filters = reactive({ reviewStatus: '', sourceType: '', keyword: '', page: 1, size: 20 })

const statusTabs = [
  { label: '全部', value: '' },
  { label: '待审核', value: 'pending' },
  { label: '已通过', value: 'approved' },
  { label: '已拒绝', value: 'rejected' }
]

const statCards = [
  { key: 'totalWorks', label: '总作品', color: 'text-white', filter: null },
  { key: 'pendingCount', label: '待审核', color: 'text-orange-400', filter: 'pending' },
  { key: 'approvedCount', label: '已通过', color: 'text-green-400', filter: 'approved' },
  { key: 'rejectedCount', label: '已拒绝', color: 'text-red-400', filter: 'rejected' },
  { key: 'featuredCount', label: '精选', color: 'text-[#FFD60A]', filter: null }
]

const totalPages = computed(() => Math.max(1, Math.ceil(totalRecords.value / filters.size)))

const setFilter = (status) => { activeFilter.value = status; filters.reviewStatus = status || ''; filters.page = 1; loadWorks() }

const loadWorks = async () => {
  loading.value = true
  try {
    const res = await adminApi.listWorks(filters)
    works.value = res.records || []
    totalRecords.value = res.total || 0
  } catch (e) { toastStore.show('加载失败: ' + (e.message || '未知错误')) }
  finally { loading.value = false }
}

const fetchStats = async () => { try { stats.value = await adminApi.getWorksStats() } catch (e) { console.error(e) } }

const reviewAction = async (id, action) => {
  try { await adminApi.reviewWork(id, action); toastStore.show(action === 'approve' ? '已通过 ✅' : '已拒绝 ❌'); await loadWorks(); await fetchStats() }
  catch (e) { toastStore.show('操作失败: ' + e.message) }
}

const openReject = (id) => { rejectTargetId.value = id; rejectNote.value = ''; rejectVisible.value = true }
const confirmReject = async () => { await adminApi.reviewWork(rejectTargetId.value, 'reject', rejectNote.value); rejectVisible.value = false; toastStore.show('已拒绝 ❌'); await loadWorks(); await fetchStats() }

const toggleFeatured = async (id) => {
  try { await adminApi.toggleFeatured(id); toastStore.show('精选状态已切换 ⭐'); await loadWorks(); await fetchStats() }
  catch (e) { toastStore.show('操作失败: ' + e.message) }
}

const deleteWork = (id) => {
  deleteTargetId.value = id
  deleteVisible.value = true
}
const confirmDelete = async () => {
  deleteVisible.value = false
  try { await adminApi.deleteWork(deleteTargetId.value); toastStore.show('已删除 🗑️'); await loadWorks(); await fetchStats() }
  catch (e) { toastStore.show('删除失败: ' + e.message) }
}

const playAudio = (work) => {
  if (!audioEl.value) { audioEl.value = new Audio() }
  audioEl.value.src = work.audioUrl
  audioEl.value.play()
  toastStore.show('🎧 试听: ' + (work.title || ''))
}

const togglePublish = async (work) => {
  const s = work.status === 'published' ? 'unpublished' : 'published'
  try { await adminApi.togglePublish(work.id, s); toastStore.show(s === 'published' ? '已上架 ✅' : '已下架 ⚠️'); await loadWorks() }
  catch (e) { toastStore.show('操作失败: ' + e.message) }
}

const sourceLabel = (type) => {
  const m = { radio:'🌙 情感电台', lecture:'📚 知识讲解', ad:'🛒 带货文案', picture_book:'🎨 有声绘本', news:'📰 新闻播报', drama:'🎭 短剧', podcast:'🎙️ 播客', novel:'📖 AI小说', tts:'🔊 TTS', emotion:'💖 情感合成', ai_music:'🎵 AI音乐' }
  return m[type] || type
}

const changePage = (d) => { filters.page = Math.max(1, Math.min(totalPages.value, filters.page + d)); loadWorks() }
const formatTime = (t) => t ? t.replace('T',' ').substring(0,16) : '-'
const reviewBadge = (s) => ({ pending:'bg-orange-500/20 text-orange-400', approved:'bg-green-500/20 text-green-400', rejected:'bg-red-500/20 text-red-400' }[s] || 'bg-white/10 text-gray-400')
const reviewLabel = (s) => ({ pending:'待审核', approved:'已通过', rejected:'已拒绝' }[s] || s || '未知')

onMounted(async () => { await Promise.all([loadWorks(), fetchStats()]) })
</script>
