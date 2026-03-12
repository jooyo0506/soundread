<template>
  <div class="h-full flex flex-col bg-[#0a0a0a]">

    <!-- ═══ 顶部（紧凑） ═══ -->
    <div class="px-4 pt-12 pb-2">
      <!-- 标题行：发现 + 搜索图标 -->
      <div class="flex items-center justify-between mb-3">
        <h1 class="text-xl font-bold text-white tracking-wide">发现</h1>
        <div class="flex items-center gap-2">
          <button class="w-8 h-8 rounded-full bg-white/5 flex items-center justify-center">
            <i class="fas fa-search text-xs text-gray-400"></i>
          </button>
        </div>
      </div>

      <!-- ═══ 精选轮播（横向滚动卡片） ═══ -->
      <div class="flex gap-3 overflow-x-auto pb-3 hide-scrollbar -mx-4 px-4 snap-x snap-mandatory">
        <!-- 卡片1: 每日推荐 -->
        <div class="min-w-[72%] snap-start rounded-2xl overflow-hidden relative h-[120px] shrink-0 cursor-pointer group"
             @click="switchCategory(categories.find(c => c.key === 'all'))">
          <div class="absolute inset-0 bg-gradient-to-br from-[#FF9500]/80 via-[#FF6B00]/60 to-[#E85D04]/80"></div>
          <div class="absolute top-0 right-0 w-28 h-28 bg-yellow-400/20 rounded-full blur-2xl -mr-8 -mt-8"></div>
          <div class="absolute bottom-0 left-0 w-20 h-20 bg-orange-600/30 rounded-full blur-xl -ml-6 -mb-6"></div>
          <div class="relative z-10 p-4 h-full flex flex-col justify-between">
            <span class="text-[9px] font-bold text-white/80 bg-black/20 px-2 py-0.5 rounded-full w-fit backdrop-blur-sm">每日推荐</span>
            <div>
              <h3 class="text-white font-bold text-base leading-tight mb-0.5">发现好声音</h3>
              <p class="text-white/70 text-[10px]">AI 创作 · 播客 · 小说 · 音乐</p>
            </div>
          </div>
        </div>
        
        <!-- 卡片2: AI 双播精选 -->
        <div class="min-w-[72%] snap-start rounded-2xl overflow-hidden relative h-[120px] shrink-0 cursor-pointer group"
             @click="switchCategory(categories.find(c => c.key === 'podcast'))">
          <div class="absolute inset-0 bg-gradient-to-br from-emerald-700/80 via-teal-700/60 to-cyan-800/80"></div>
          <div class="absolute top-0 right-0 w-24 h-24 bg-emerald-400/15 rounded-full blur-2xl -mr-6 -mt-6"></div>
          <!-- 波形装饰 -->
          <div class="absolute bottom-3 right-4 flex items-end gap-[3px] opacity-40">
            <div v-for="(h, i) in bannerWaves" :key="i" class="w-[3px] rounded-full bg-emerald-300 animate-wave"
                 :style="{ height: h + 'px', animationDelay: (i * 0.15) + 's' }"></div>
          </div>
          <div class="relative z-10 p-4 h-full flex flex-col justify-between">
            <span class="text-[9px] font-bold text-emerald-200 bg-black/20 px-2 py-0.5 rounded-full w-fit backdrop-blur-sm"><i class="fas fa-podcast mr-1"></i>AI 双播</span>
            <div>
              <h3 class="text-white font-bold text-base leading-tight mb-0.5">播客精选</h3>
              <p class="text-white/70 text-[10px]">智能双人对话播客</p>
            </div>
          </div>
        </div>

        <!-- 卡片3: 小说世界 -->
        <div class="min-w-[72%] snap-start rounded-2xl overflow-hidden relative h-[120px] shrink-0 cursor-pointer group"
             @click="switchCategory(categories.find(c => c.key === 'novel'))">
          <div class="absolute inset-0 bg-gradient-to-br from-indigo-800/80 via-blue-800/60 to-violet-900/80"></div>
          <div class="absolute top-0 right-0 w-24 h-24 bg-indigo-400/15 rounded-full blur-2xl -mr-6 -mt-6"></div>
          <div class="relative z-10 p-4 h-full flex flex-col justify-between">
            <span class="text-[9px] font-bold text-indigo-200 bg-black/20 px-2 py-0.5 rounded-full w-fit backdrop-blur-sm"><i class="fas fa-book-open mr-1"></i>小说</span>
            <div>
              <h3 class="text-white font-bold text-base leading-tight mb-0.5">AI 小说世界</h3>
              <p class="text-white/70 text-[10px]">智能创作无限想象</p>
            </div>
          </div>
        </div>
      </div>

      <!-- ═══ 分类标签（全部可见） ═══ -->
      <div class="flex gap-2 pt-2 pb-1">
        <button
          v-for="cat in categories" :key="cat.key"
          @click="switchCategory(cat)"
          class="px-3 py-1.5 rounded-full text-[11px] font-bold whitespace-nowrap transition-all cursor-pointer"
          :class="activeCategory === cat.key
            ? 'bg-[#FF9500] text-black shadow-[0_2px_10px_rgba(255,149,0,0.25)]'
            : 'bg-white/5 text-gray-400 hover:text-white hover:bg-white/10'"
        >
          {{ cat.name }}
        </button>
      </div>
    </div>

    <!-- ═══ 作品列表 ═══ -->
    <div ref="scrollEl" class="flex-1 overflow-y-auto px-4 pb-24 hide-scrollbar" @scroll.passive="handleScroll">
      <div class="flex justify-between items-center mb-3 mt-1">
        <h3 class="text-white font-bold text-sm">{{ activeCategoryLabel }}</h3>
        <span class="text-[10px] text-gray-600">{{ works.length }} 项</span>
      </div>

      <div class="grid grid-cols-2 gap-2.5">
        <!-- 骨架屏（首次加载） -->
        <template v-if="isFirstLoading">
          <div v-for="n in 6" :key="'sk-' + n" class="rounded-xl overflow-hidden bg-[#141414] border border-white/[0.04]">
            <div class="h-24 skeleton-box"></div>
            <div class="px-2.5 py-2 space-y-1.5">
              <div class="h-2 rounded skeleton-box w-3/4"></div>
              <div class="h-2 rounded skeleton-box w-1/2"></div>
            </div>
          </div>
        </template>

        <!-- 作品列表 -->
        <template v-else>
          <div v-for="work in works" :key="work.id"
               v-memo="[work.isLiked, work.likeCount, work.playCount]"
               @click="handleClick(work)"
               class="rounded-xl overflow-hidden cursor-pointer group transition-all duration-150 active:scale-[0.97] hover:scale-[1.02] bg-[#141414] border border-white/[0.04] hover:border-white/10 relative">

            <!-- 作者管理按钮（仅自己的作品可见） -->
            <div v-if="isMyWork(work)" class="absolute top-1.5 right-1.5 z-20 flex items-center gap-1">
              <span class="text-[7px] font-bold text-white/60 bg-black/50 backdrop-blur px-1.5 py-0.5 rounded">我的</span>
              <button @click.stop="openActionSheet(work)"
                      class="w-6 h-6 rounded-full bg-black/50 backdrop-blur flex items-center justify-center hover:bg-black/70 transition-colors cursor-pointer">
                <i class="fas fa-ellipsis-v text-[9px] text-white/70"></i>
              </button>
            </div>

            <!-- ════ 小说卡片 ════ -->
            <template v-if="work.contentType === 'novel'">
              <div class="h-24 p-3 relative overflow-hidden bg-gradient-to-br from-indigo-900/50 via-blue-900/30 to-cyan-900/20">
                <div class="absolute top-0 right-0 w-16 h-16 bg-cyan-400/8 rounded-full blur-xl -mr-4 -mt-4"></div>
                <div class="relative z-10 h-full flex flex-col justify-between">
                  <span class="inline-flex items-center gap-1 text-[8px] font-bold text-cyan-300/80 bg-cyan-500/10 px-1.5 py-0.5 rounded w-fit">
                    <i class="fas fa-book-open"></i> 小说
                  </span>
                  <h4 class="text-white text-xs font-bold line-clamp-2 leading-snug">{{ work.title }}</h4>
                </div>
              </div>
              <div class="px-2.5 py-2">
                <p v-if="work.description" class="text-[9px] text-gray-500 line-clamp-1 mb-1.5">{{ work.description }}</p>
                <div class="flex items-center justify-between text-[9px] text-gray-600">
                  <span class="flex items-center gap-1"><i class="fas fa-file-alt"></i> {{ work.chapterCount || 0 }}章</span>
                  <button @click.stop="handleLike(work)" class="flex items-center gap-0.5 hover:text-rose-400 transition-colors cursor-pointer" :class="work.isLiked ? 'text-rose-500' : ''">
                    <i :class="work.isLiked ? 'fas fa-heart' : 'far fa-heart'" class="text-[8px]"></i> {{ formatNumber(work.likeCount) }}
                  </button>
                </div>
              </div>
            </template>

            <!-- ════ 播客卡片 ════ -->
            <template v-else-if="work.contentType === 'podcast'">
              <div class="h-24 relative overflow-hidden bg-gradient-to-br from-emerald-900/50 via-teal-900/30 to-cyan-900/20">
                <div class="absolute top-0 right-0 w-16 h-16 bg-emerald-400/8 rounded-full blur-xl -mr-4 -mt-4"></div>
                <div class="absolute bottom-1.5 left-2.5 flex items-end gap-[2px] opacity-25">
                  <div v-for="i in 7" :key="i" class="w-[2px] rounded-full bg-emerald-400 animate-wave"
                       :style="{ height: (4 + Math.random() * 12) + 'px', animationDelay: (i * 0.12) + 's' }"></div>
                </div>
                <div class="absolute inset-0 flex flex-col justify-between p-3">
                  <span class="inline-flex items-center gap-1 text-[8px] font-bold text-emerald-300/80 bg-emerald-500/10 px-1.5 py-0.5 rounded w-fit">
                    <i class="fas fa-podcast"></i> 双播
                  </span>
                  <h4 class="text-white text-xs font-bold line-clamp-2 leading-snug">{{ work.title }}</h4>
                </div>
                <div class="absolute inset-0 bg-black/40 flex justify-center items-center opacity-0 group-hover:opacity-100 transition-opacity">
                  <div class="w-9 h-9 rounded-full bg-emerald-500 flex justify-center items-center text-black pl-0.5 shadow-lg shadow-emerald-500/30">
                    <i class="fas fa-play text-xs"></i>
                  </div>
                </div>
              </div>
              <div class="px-2.5 py-2">
                <p class="text-[9px] text-gray-500 line-clamp-1 mb-1.5">{{ work.description || 'AI 智能双人播客' }}</p>
                <div class="flex items-center justify-between text-[9px] text-gray-600">
                  <div class="flex items-center gap-1.5">
                    <span class="flex items-center gap-0.5"><i class="fas fa-play text-[7px]"></i> {{ formatNumber(work.playCount) }}</span>
                    <span v-if="work.audioDuration" class="flex items-center gap-0.5"><i class="far fa-clock text-[7px]"></i> {{ formatDuration(work.audioDuration) }}</span>
                  </div>
                  <button @click.stop="handleLike(work)" class="flex items-center gap-0.5 hover:text-rose-400 transition-colors cursor-pointer" :class="work.isLiked ? 'text-rose-500' : ''">
                    <i :class="work.isLiked ? 'fas fa-heart' : 'far fa-heart'" class="text-[8px]"></i> {{ formatNumber(work.likeCount) }}
                  </button>
                </div>
              </div>
            </template>

            <!-- ════ 音乐卡片 ════ -->
            <template v-else-if="work.contentType === 'music'">
              <div class="h-24 relative overflow-hidden bg-gradient-to-br from-fuchsia-900/50 via-pink-900/30 to-violet-900/20">
                <div class="absolute top-0 right-0 w-16 h-16 bg-fuchsia-400/8 rounded-full blur-xl -mr-4 -mt-4"></div>
                <div class="absolute inset-0 flex flex-col justify-between p-3">
                  <span class="inline-flex items-center gap-1 text-[8px] font-bold text-fuchsia-300/80 bg-fuchsia-500/10 px-1.5 py-0.5 rounded w-fit">
                    <i class="fas fa-music"></i> 音乐
                  </span>
                  <h4 class="text-white text-xs font-bold line-clamp-2 leading-snug">{{ work.title }}</h4>
                </div>
                <div class="absolute inset-0 bg-black/40 flex justify-center items-center opacity-0 group-hover:opacity-100 transition-opacity">
                  <div class="w-9 h-9 rounded-full bg-fuchsia-500 flex justify-center items-center text-black pl-0.5 shadow-lg shadow-fuchsia-500/30">
                    <i class="fas fa-play text-xs"></i>
                  </div>
                </div>
              </div>
              <div class="px-2.5 py-2">
                <p class="text-[9px] text-gray-500 line-clamp-1 mb-1.5">{{ work.description || 'AI 音乐' }}</p>
                <div class="flex items-center justify-between text-[9px] text-gray-600">
                  <div class="flex items-center gap-1.5">
                    <span class="flex items-center gap-0.5"><i class="fas fa-play text-[7px]"></i> {{ formatNumber(work.playCount) }}</span>
                    <span v-if="work.audioDuration" class="flex items-center gap-0.5"><i class="far fa-clock text-[7px]"></i> {{ formatDuration(work.audioDuration) }}</span>
                  </div>
                  <button @click.stop="handleLike(work)" class="flex items-center gap-0.5 hover:text-rose-400 transition-colors cursor-pointer" :class="work.isLiked ? 'text-rose-500' : ''">
                    <i :class="work.isLiked ? 'fas fa-heart' : 'far fa-heart'" class="text-[8px]"></i> {{ formatNumber(work.likeCount) }}
                  </button>
                </div>
              </div>
            </template>

            <!-- ════ 默认语音卡片 ════ -->
            <template v-else>
              <div class="h-24 relative overflow-hidden bg-gradient-to-br from-[#4F46E5]/40 via-[#6D28D9]/25 to-[#7C3AED]/20">
                <div class="absolute top-0 right-0 w-16 h-16 bg-violet-400/8 rounded-full blur-xl -mr-4 -mt-4"></div>
                <img v-if="work.coverUrl" :src="work.coverUrl" loading="lazy" decoding="async" class="absolute inset-0 w-full h-full object-cover opacity-50 group-hover:opacity-70 transition-opacity" />
                <div class="absolute inset-0 flex flex-col justify-between p-3" :class="work.coverUrl ? 'bg-gradient-to-t from-black/70 via-transparent to-transparent' : ''">
                  <span class="inline-flex items-center gap-1 text-[8px] font-bold text-violet-300/80 bg-violet-500/10 px-1.5 py-0.5 rounded w-fit">
                    <i class="fas fa-headphones"></i> 语音
                  </span>
                  <h4 class="text-white text-xs font-bold line-clamp-2 leading-snug">{{ work.title }}</h4>
                </div>
                <div class="absolute inset-0 bg-black/40 flex justify-center items-center opacity-0 group-hover:opacity-100 transition-opacity">
                  <div class="w-9 h-9 rounded-full bg-[#FF9500] flex justify-center items-center text-black pl-0.5 shadow-lg shadow-orange-500/30">
                    <i class="fas fa-play text-xs"></i>
                  </div>
                </div>
              </div>
              <div class="px-2.5 py-2">
                <p class="text-[9px] text-gray-500 line-clamp-1 mb-1.5">{{ work.description || 'AI 创作' }}</p>
                <div class="flex items-center justify-between text-[9px] text-gray-600">
                  <div class="flex items-center gap-1.5">
                    <span class="flex items-center gap-0.5"><i class="fas fa-play text-[7px]"></i> {{ formatNumber(work.playCount) }}</span>
                    <span v-if="work.audioDuration" class="flex items-center gap-0.5"><i class="far fa-clock text-[7px]"></i> {{ formatDuration(work.audioDuration) }}</span>
                  </div>
                  <button @click.stop="handleLike(work)" class="flex items-center gap-0.5 hover:text-rose-400 transition-colors cursor-pointer" :class="work.isLiked ? 'text-rose-500' : ''">
                    <i :class="work.isLiked ? 'fas fa-heart' : 'far fa-heart'" class="text-[8px]"></i> {{ formatNumber(work.likeCount) }}
                  </button>
                </div>
              </div>
            </template>
          </div>

          <!-- 空状态 -->
          <div v-if="works.length === 0" class="col-span-2 py-12 text-center">
            <div class="w-12 h-12 mx-auto mb-3 rounded-xl bg-white/[0.03] flex items-center justify-center">
              <i class="fas fa-compass text-lg text-gray-700"></i>
            </div>
            <p class="text-gray-500 text-xs mb-0.5">暂无{{ activeCategoryLabel.replace('作品','') }}作品</p>
            <p class="text-gray-700 text-[10px]">快去创作你的第一个作品吧</p>
          </div>

          <!-- 底部加载更多 -->
          <div v-if="loading" class="col-span-2 py-4 text-center">
            <i class="fas fa-circle-notch fa-spin text-gray-600"></i>
          </div>
        </template>
      </div>
    </div>

    <!-- ═══ 作品管理弹窗 ═══ -->
    <Teleport to="body">
      <Transition name="sheet">
        <div v-if="actionWork" class="fixed inset-0 z-50 flex items-end justify-center" style="max-width: 430px; margin: 0 auto;">
          <div class="absolute inset-0 bg-black/60 backdrop-blur-sm" @click="actionWork = null"></div>
          <div class="relative w-full bg-[#1a1a1a] rounded-t-2xl border-t border-white/10 p-4 pb-8">
            <div class="w-8 h-1 rounded-full bg-white/10 mx-auto mb-4"></div>
            <div class="mb-4">
              <h4 class="text-white text-sm font-bold truncate">{{ actionWork?.title }}</h4>
              <p class="text-[10px] text-gray-500 mt-0.5">管理你的作品</p>
            </div>
            <div class="space-y-2">
              <button @click="handleUnpublish(actionWork)"
                      class="w-full flex items-center gap-3 px-4 py-3 rounded-xl bg-red-500/10 hover:bg-red-500/15 transition-colors cursor-pointer">
                <i class="fas fa-eye-slash text-red-400 text-sm w-5 text-center"></i>
                <div class="text-left">
                  <div class="text-xs font-bold text-red-400">下架作品</div>
                  <div class="text-[10px] text-gray-500">从发现页移除，不影响原始创作</div>
                </div>
              </button>
              <button v-if="actionWork?.sourceProjectId" @click="goToProject(actionWork)"
                      class="w-full flex items-center gap-3 px-4 py-3 rounded-xl bg-white/5 hover:bg-white/10 transition-colors cursor-pointer">
                <i class="fas fa-edit text-gray-400 text-sm w-5 text-center"></i>
                <div class="text-left">
                  <div class="text-xs font-bold text-gray-300">查看项目</div>
                  <div class="text-[10px] text-gray-500">打开原始创作项目</div>
                </div>
              </button>
              <button @click="actionWork = null"
                      class="w-full py-3 rounded-xl text-xs text-gray-500 hover:text-white transition-colors cursor-pointer text-center">
                取消
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- ═══ 小说阅读器 ═══ -->
    <Teleport to="body">
      <Transition name="reader">
        <div v-if="readerVisible"
             class="fixed inset-0 z-50 bg-[#0a0a0a] flex flex-col"
             style="max-width: 430px; margin: 0 auto;">
          <div class="px-4 pt-12 pb-3 border-b border-white/5 flex items-center gap-3">
            <button @click="readerVisible = false" class="w-8 h-8 rounded-full bg-white/5 hover:bg-white/10 flex items-center justify-center cursor-pointer transition-all shrink-0">
              <i class="fas fa-chevron-left text-xs text-gray-400"></i>
            </button>
            <div class="flex-1 min-w-0">
              <h2 class="text-white text-sm font-bold truncate">{{ readerWork?.title }}</h2>
              <p v-if="readerWork?.chapterCount" class="text-[9px] text-gray-500">{{ readerWork.chapterCount }}章 · {{ formatWordCount(readerWork.wordCount) }}字</p>
            </div>
          </div>
          <div v-if="readerWork?.description" class="px-5 py-2">
            <p class="text-[10px] text-gray-500 italic leading-relaxed bg-white/[0.02] rounded-lg p-2.5 border border-white/5">「{{ readerWork.description }}」</p>
          </div>
          <div class="flex-1 overflow-y-auto px-5 pb-20 hide-scrollbar">
            <div v-if="readerLoading" class="py-20 text-center">
              <i class="fas fa-circle-notch fa-spin text-cyan-400 text-xl mb-3"></i>
              <p class="text-gray-500 text-xs">加载中...</p>
            </div>
            <div v-else-if="readerSections.length">
              <div v-for="(section, idx) in readerSections" :key="section.id || idx" class="mb-6">
                <h3 class="text-white font-bold text-sm mb-2 flex items-center gap-2">
                  <span class="w-5 h-5 rounded bg-cyan-500/15 text-cyan-400 text-[9px] font-bold flex items-center justify-center">{{ idx + 1 }}</span>
                  {{ section.title || `第${idx + 1}章` }}
                </h3>
                <p class="text-gray-300 text-xs leading-[1.9] whitespace-pre-wrap">{{ section.content }}</p>
              </div>
            </div>
            <div v-else class="py-20 text-center text-gray-600 text-xs">暂无内容</div>
          </div>
        </div>
      </Transition>
    </Teleport>

  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { discoverApi } from '../api/discover'
import { studioApi } from '../api/studio'
import { useAuthStore } from '../stores/auth'
import { usePlayerStore } from '../stores/player'
import { useToastStore } from '../stores/toast'

const router = useRouter()
const isFirstLoading = computed(() => loading.value && works.value.length === 0)
const toastStore = useToastStore()
const authStore = useAuthStore()
const playerStore = usePlayerStore()

// ==================== 波形预计算（只初始化一次，防止重渲染跳变） ====================

/** 顶部横幅波形高度（6条，固定值） */
const bannerWaves = Array.from({ length: 6 }, () => 6 + Math.random() * 18)

/** 播客/音乐卡片波形缓存，key=workId，每个作品只算一次 */
const _waveCache = new Map()
const getCardWaves = (workId) => {
    if (!_waveCache.has(workId)) {
        _waveCache.set(workId, Array.from({ length: 7 }, () => 4 + Math.random() * 12))
    }
    return _waveCache.get(workId)
}

// ==================== 无限滚动 ====================

/** 滚动容器 ref */
const scrollEl = ref(null)

/** 距底部 80px 时自动加载下一页（替代「加载更多」按钮） */
const handleScroll = () => {
    const el = scrollEl.value
    if (!el || loading.value || !hasMore.value) return
    if (el.scrollHeight - el.scrollTop - el.clientHeight < 80) {
        loadWorks()
    }
}

// ==================== 分类 ====================

const categories = ref([
  { key: 'all', name: '全部', icon: 'fas fa-fire' },
  { key: 'audio', name: '语音', icon: 'fas fa-headphones' },
  { key: 'novel', name: '小说', icon: 'fas fa-book-open' },
  { key: 'podcast', name: '播客', icon: 'fas fa-podcast' },
  { key: 'music', name: '音乐', icon: 'fas fa-music' },
])

const activeCategory = ref('all')

const activeCategoryLabel = computed(() => {
  const cat = categories.value.find(c => c.key === activeCategory.value)
  return cat ? (cat.key === 'all' ? '热门作品' : cat.name + '作品') : '热门作品'
})

const switchCategory = (cat) => {
  if (!cat) return
  activeCategory.value = cat.key
  works.value = []
  page.value = 1
  hasMore.value = true
  loadWorks()
}

// ==================== 作品列表 ====================

const works = ref([])
const hasMore = ref(true)
const page = ref(1)
const loading = ref(false)

const loadWorks = async () => {
    if (loading.value || !hasMore.value) return
    loading.value = true
    try {
        const params = { page: page.value, size: 10, sort: 'hot' }
        if (activeCategory.value !== 'all') {
            params.contentType = activeCategory.value
        }
        const res = await discoverApi.getWorks(params)
        if (res.records && res.records.length > 0) {
            works.value.push(...res.records)
            page.value++
            if (res.records.length < 10) hasMore.value = false
        } else {
            hasMore.value = false
        }
    } catch (e) {
        console.error('获取作品库失败:', e)
    } finally {
        loading.value = false
    }
}

// ==================== 作品管理 ====================

const isMyWork = (work) => {
    return authStore.user?.userId && work.userId && String(work.userId) === String(authStore.user.userId)
}

const actionWork = ref(null)

const openActionSheet = (work) => {
    actionWork.value = work
}

const handleUnpublish = async (work) => {
    try {
        await discoverApi.unpublishWork(work.id)
        toastStore.show('作品已下架')
        // 从列表中移除
        works.value = works.value.filter(w => w.id !== work.id)
        actionWork.value = null
    } catch (e) {
        toastStore.show('下架失败: ' + (e.response?.data?.message || e.message))
    }
}

const goToProject = (work) => {
    actionWork.value = null
    router.push('/studio/' + work.sourceProjectId)
}

// ==================== 交互 ====================

const handleClick = (work) => {
    if (work.contentType === 'novel') {
        openReader(work)
    } else {
        handlePlay(work)
    }
}

const handlePlay = async (work) => {
    if (!work.audioUrl) return toastStore.show('该作品音频正在生成中')
    
    playerStore.play({
        title: work.title,
        author: work.authorId || '@System',
        cover: work.coverUrl,
        url: work.audioUrl
    })
    
    try {
        await discoverApi.playWork(work.id)
        work.playCount = (work.playCount || 0) + 1
    } catch (e) {
        console.warn('播放量计数失败:', e.message)
    }
}

const handleLike = async (work) => {
    if (!authStore.isLoggedIn) return router.replace('/login')
    work.isLiked = !work.isLiked
    work.likeCount += (work.isLiked ? 1 : -1)
    try {
        await discoverApi.likeWork(work.id)
    } catch (e) {
        work.isLiked = !work.isLiked
        work.likeCount += (work.isLiked ? 1 : -1)
    }
}

// ==================== 小说阅读器 ====================

const readerVisible = ref(false)
const readerWork = ref(null)
const readerSections = ref([])
const readerLoading = ref(false)

const openReader = async (work) => {
    readerWork.value = work
    readerVisible.value = true
    readerSections.value = []
    readerLoading.value = true
    try { discoverApi.playWork(work.id); work.playCount = (work.playCount || 0) + 1 } catch {}
    if (work.sourceProjectId) {
        try {
            const sections = await studioApi.listSections(work.sourceProjectId)
            readerSections.value = sections || []
        } catch (e) {
            toastStore.show('加载失败，请重试')
        }
    }
    readerLoading.value = false
}

// ==================== 工具 ====================

const formatNumber = (num) => {
    if (!num) return 0
    return num > 9999 ? (num / 10000).toFixed(1) + 'w' : num
}

const formatWordCount = (num) => {
    if (!num) return '0'
    return num > 9999 ? (num / 10000).toFixed(1) + '万' : num.toLocaleString()
}

const formatDuration = (seconds) => {
    if (!seconds) return ''
    const m = Math.floor(seconds / 60)
    const s = seconds % 60
    return m > 0 ? `${m}:${String(s).padStart(2, '0')}` : `0:${String(s).padStart(2, '0')}`
}

onMounted(() => loadWorks())
</script>

<style scoped>
/* 骨架屏闪烁 */
@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}
.skeleton-box {
  background: linear-gradient(90deg,
    rgba(255,255,255,0.04) 25%,
    rgba(255,255,255,0.08) 50%,
    rgba(255,255,255,0.04) 75%
  );
  background-size: 200% 100%;
  animation: shimmer 1.4s ease-in-out infinite;
}

.reader-enter-active,
.reader-leave-active {
  transition: transform 0.3s ease, opacity 0.3s ease;
}
.reader-enter-from {
  transform: translateY(100%);
  opacity: 0;
}
.reader-leave-to {
  transform: translateY(100%);
  opacity: 0;
}
@keyframes wave {
  0%, 100% { height: 4px; }
  50% { height: 16px; }
}
.animate-wave {
  animation: wave 1.2s ease-in-out infinite;
}
.sheet-enter-active,
.sheet-leave-active {
  transition: opacity 0.25s ease;
}
.sheet-enter-active > div:last-child,
.sheet-leave-active > div:last-child {
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.sheet-enter-from {
  opacity: 0;
}
.sheet-enter-from > div:last-child {
  transform: translateY(100%);
}
.sheet-leave-to {
  opacity: 0;
}
.sheet-leave-to > div:last-child {
  transform: translateY(100%);
}
</style>
