<template>
  <div class="novel-page">
    <nav class="novel-nav">
      <button class="back-btn" @click="$router.back()">← 返回</button>
      <h1>📚 AI 有声书工作台</h1>
    </nav>

    <!-- 创建区域 -->
    <section v-if="!currentProject" class="create-section">
      <div class="input-group">
        <input v-model="title" placeholder="项目名称" class="title-input" />
      </div>
      <textarea v-model="rawText" placeholder="请粘贴小说原文..." class="text-area" rows="12"></textarea>
      <div class="controls">
        <div class="voice-select">
          🎙️ 音色: <select v-model="voiceId">
            <option value="BV701_streaming">擎苍（浑厚有力）</option>
            <option value="BV102_streaming">儒雅青年</option>
            <option value="BV113_streaming">甜宠少御</option>
            <option value="BV700_streaming">灿灿</option>
          </select>
        </div>
        <button class="start-btn" @click="createAndStart" :disabled="!rawText.trim()">
          ✨ 开始 AI 制作
        </button>
      </div>
      <p class="char-count">{{ rawText.length }} 字</p>
    </section>

    <!-- 项目进度 -->
    <section v-else class="progress-section">
      <h2>{{ currentProject.title }}</h2>
      <div class="progress-bar">
        <div class="progress-fill" :style="{ width: currentProject.progress + '%' }"></div>
      </div>
      <p class="progress-text">{{ statusText }} {{ currentProject.progress }}%</p>

      <!-- 章节列表 -->
      <div class="chapter-list" v-if="chapters.length">
        <h3>📖 章节列表</h3>
        <div v-for="ch in chapters" :key="ch.id" class="chapter-item">
          <span class="chapter-status">{{ ch.status === 'completed' ? '✅' : ch.status === 'failed' ? '❌' : '🔄' }}</span>
          <span class="chapter-title">第{{ ch.chapterIndex }}章 {{ ch.title }}</span>
          <span class="chapter-duration">{{ ch.audioDuration ? formatDuration(ch.audioDuration) : '--:--' }}</span>
        </div>
      </div>

      <!-- 完成后的下载按钮 -->
      <div v-if="currentProject.status === 'completed'" class="download-section">
        <a :href="currentProject.audioUrl" class="download-btn">📥 下载完整音频</a>
        <a v-if="currentProject.subtitleUrl" :href="currentProject.subtitleUrl" class="download-btn">📝 下载字幕</a>
      </div>
    </section>

    <!-- 历史项目列表 -->
    <section class="history-section">
      <h3>📁 我的项目</h3>
      <div v-for="p in projects" :key="p.id" class="project-item" @click="selectProject(p)">
        <div class="project-info">
          <span class="project-title">{{ p.title }}</span>
          <span class="project-meta">{{ p.totalChars }}字 · {{ p.totalChapters || 0 }}章</span>
        </div>
        <span class="project-status" :class="p.status">{{ p.status }}</span>
      </div>
      <p v-if="!projects.length" class="empty-hint">暂无项目，在上方粘贴文本开始创作</p>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { novelApi } from '../api/novel'
import { useToastStore } from '../stores/toast'

const toast = useToastStore()
const title = ref('')
const rawText = ref('')
const voiceId = ref('BV701_streaming')
const projects = ref([])
const currentProject = ref(null)
const chapters = ref([])
let pollTimer = null

const statusText = computed(() => {
  const map = { draft: '草稿', analyzing: 'AI 分章中...', annotating: '情感标注中...', synthesizing: 'TTS 合成中...', completed: '已完成', failed: '处理失败' }
  return map[currentProject.value?.status] || ''
})

onMounted(() => loadProjects())

async function loadProjects() {
  try {
    projects.value = await novelApi.list()
  } catch (e) {
    console.warn('加载项目列表失败', e)
  }
}

async function createAndStart() {
  try {
    const project = await novelApi.create({ title: title.value || '有声书 ' + new Date().toLocaleDateString(), voiceId: voiceId.value, rawText: rawText.value })
    currentProject.value = project
    await novelApi.startPipeline(project.id, rawText.value)
    toast.show('🚀 AI 制作已启动')
    startPolling(project.id)
  } catch (e) {
    toast.show(e.message || '创建失败')
  }
}

function selectProject(p) {
  currentProject.value = p
  loadChapters(p.id)
  if (p.status !== 'completed' && p.status !== 'failed') {
    startPolling(p.id)
  }
}

async function loadChapters(projectId) {
  try {
    chapters.value = await novelApi.chapters(projectId)
  } catch (e) {
    console.warn('加载章节失败', e)
  }
}

function startPolling(projectId) {
  if (pollTimer) clearInterval(pollTimer)
  pollTimer = setInterval(async () => {
    try {
      const info = await novelApi.progress(projectId)
      if (currentProject.value) {
        currentProject.value.progress = info.progress
        currentProject.value.status = info.status
        currentProject.value.audioUrl = info.audioUrl
      }
      if (info.status === 'completed' || info.status === 'failed') {
        clearInterval(pollTimer)
        loadChapters(projectId)
        loadProjects()
      }
    } catch (e) {
      console.warn('轮询进度失败', e)
    }
  }, 3000)
}

function formatDuration(seconds) {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}
</script>

<style scoped>
.novel-page { max-width: 640px; margin: 0 auto; padding: 20px; padding-bottom: 100px; }
.novel-nav { display: flex; align-items: center; gap: 12px; margin-bottom: 20px; }
.novel-nav h1 { font-size: 20px; margin: 0; }
.back-btn { background: none; border: none; font-size: 16px; cursor: pointer; color: var(--text-secondary, #666); }
.create-section { margin-bottom: 30px; }
.title-input { width: 100%; padding: 12px; border: 1px solid #ddd; border-radius: 8px; font-size: 16px; margin-bottom: 12px; box-sizing: border-box; }
.text-area { width: 100%; padding: 12px; border: 1px solid #ddd; border-radius: 8px; font-size: 14px; resize: vertical; box-sizing: border-box; }
.controls { display: flex; justify-content: space-between; align-items: center; margin-top: 12px; }
.voice-select select { padding: 6px 10px; border-radius: 6px; border: 1px solid #ddd; }
.start-btn { padding: 10px 24px; background: linear-gradient(135deg, #667eea, #764ba2); color: white; border: none; border-radius: 8px; font-size: 16px; cursor: pointer; }
.start-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.char-count { text-align: right; color: #999; font-size: 13px; margin-top: 4px; }
.progress-section { background: #f8f9fa; border-radius: 12px; padding: 20px; margin-bottom: 20px; }
.progress-bar { width: 100%; height: 8px; background: #e0e0e0; border-radius: 4px; overflow: hidden; margin: 12px 0; }
.progress-fill { height: 100%; background: linear-gradient(90deg, #667eea, #764ba2); border-radius: 4px; transition: width 0.5s ease; }
.progress-text { text-align: center; font-size: 14px; color: #666; }
.chapter-list { margin-top: 16px; }
.chapter-item { display: flex; align-items: center; gap: 8px; padding: 10px 0; border-bottom: 1px solid #eee; }
.chapter-title { flex: 1; }
.chapter-duration { color: #999; font-size: 13px; }
.download-section { display: flex; gap: 12px; margin-top: 16px; }
.download-btn { padding: 10px 20px; background: #28a745; color: white; border-radius: 8px; text-decoration: none; font-size: 14px; }
.history-section { margin-top: 20px; }
.project-item { display: flex; justify-content: space-between; align-items: center; padding: 14px; background: white; border-radius: 8px; margin-bottom: 8px; cursor: pointer; border: 1px solid #eee; }
.project-item:hover { border-color: #667eea; }
.project-info { display: flex; flex-direction: column; gap: 4px; }
.project-title { font-weight: 600; }
.project-meta { font-size: 12px; color: #999; }
.project-status { font-size: 12px; padding: 4px 8px; border-radius: 4px; background: #e8e8e8; }
.project-status.completed { background: #d4edda; color: #155724; }
.project-status.failed { background: #f8d7da; color: #721c24; }
.project-status.analyzing, .project-status.annotating, .project-status.synthesizing { background: #fff3cd; color: #856404; }
.empty-hint { text-align: center; color: #999; padding: 20px; }
</style>
