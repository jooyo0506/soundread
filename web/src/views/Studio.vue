<template>
  <div class="min-h-screen bg-[#0a0a0a] text-white p-4 md:p-6 pb-24">
    <div class="max-w-5xl mx-auto">

      <!-- Header (精简) -->
      <div class="flex justify-between items-center mb-5">
        <div class="flex items-center gap-3">
          <button @click="$router.push('/')" class="w-8 h-8 rounded-full bg-white/5 flex items-center justify-center text-gray-400 hover:text-white hover:bg-white/10 transition-all cursor-pointer shrink-0">
            <i class="fas fa-arrow-left text-xs"></i>
          </button>
          <div>
            <h1 class="text-lg font-bold">有声创作</h1>
            <p class="text-gray-600 text-[10px]">AI 写作 + 自动配音，一站式有声内容制作</p>
          </div>
        </div>
        <button @click="showTypePicker = true"
                class="px-3 py-1.5 rounded-xl bg-gradient-to-r from-[#FF9500] to-[#FF6B00] text-black text-xs font-bold cursor-pointer hover:shadow-lg hover:shadow-orange-500/20 transition-all flex items-center gap-1.5">
          <i class="fas fa-plus"></i> 新建
        </button>
      </div>

      <!-- 项目加载骨溶屏 -->
      <div v-if="loadingProjects && projects.length === 0">
        <div class="mb-5 grid grid-cols-4 gap-2">
          <div v-for="i in 8" :key="i" class="rounded-xl p-2.5 border border-white/5 bg-white/[0.03] animate-pulse">
            <div class="w-7 h-7 rounded-full bg-white/8 mx-auto mb-1.5"></div>
            <div class="h-2.5 rounded-full bg-white/8 w-3/4 mx-auto"></div>
          </div>
        </div>
        <div class="space-y-2">
          <div v-for="i in 3" :key="i" class="rounded-xl px-4 py-3 border border-white/5 bg-[#141416] animate-pulse flex items-center gap-3">
            <div class="w-10 h-10 rounded-lg bg-white/8 shrink-0"></div>
            <div class="flex-1 space-y-2">
              <div class="h-3 rounded-full bg-white/8 w-1/2"></div>
              <div class="h-2 rounded-full bg-white/8 w-3/4"></div>
            </div>
          </div>
        </div>
      </div>

      <div v-else>
        <!-- 创作入口（只展示已开放模块） -->
        <div class="mb-5 grid grid-cols-4 gap-2">
          <div v-for="t in availableTemplates" :key="t.id"
               @click="startCreate(t)"
               class="rounded-xl p-2.5 cursor-pointer transition-all border border-white/5 group text-center relative overflow-hidden"
               :class="getTemplateBg(t.typeCode)">
            <div class="relative">
              <div class="text-xl mb-1">{{ t.icon }}</div>
              <h3 class="text-[10px] font-bold text-gray-300 group-hover:text-white transition-colors truncate">{{ t.typeName }}</h3>
            </div>
          </div>
        </div>

        <!-- 我的创作项目 -->
        <div>
          <div class="flex justify-between items-center mb-2.5">
            <h2 class="text-xs font-bold text-gray-400 flex items-center gap-1.5">
              <i class="fas fa-folder-open text-[#FF9500]"></i> 我的项目
              <span v-if="projects.length" class="text-gray-600">({{ projects.length }})</span>
              <!-- 独立的小 loading 指示 -->
              <i v-if="loadingProjects" class="fas fa-circle-notch fa-spin text-[10px] text-gray-600 ml-1"></i>
            </h2>
            <div class="flex items-center gap-1.5">
              <button v-for="tab in filterTabs" :key="tab.key"
                      @click="activeFilter = tab.key"
                      class="px-2 py-0.5 rounded-md text-[10px] font-bold cursor-pointer transition-all"
                      :class="activeFilter === tab.key
                        ? 'bg-[#FF9500]/15 text-[#FF9500]'
                        : 'text-gray-600 hover:text-gray-400'">
                {{ tab.label }} {{ tab.count > 0 ? tab.count : '' }}
              </button>
            </div>
          </div>

          <!-- 空状态 -->
          <div v-if="filteredProjects.length === 0" class="py-10">
            <div v-if="projects.length === 0" class="rounded-2xl bg-[#111] border border-white/5 p-8 text-center">
              <div class="w-14 h-14 mx-auto rounded-full bg-white/5 flex items-center justify-center mb-4">
                <i class="fas fa-microphone-lines text-2xl text-gray-600"></i>
              </div>
              <h3 class="text-sm font-bold text-white mb-1.5">开始你的第一个创作</h3>
              <p class="text-[10px] text-gray-500 mb-5 max-w-[200px] mx-auto">选择上方任意类型，AI 帮你快速生成</p>
              <button @click="showTypePicker = true"
                      class="px-5 py-2 rounded-xl bg-gradient-to-r from-[#FF9500] to-[#FF6B00] text-black text-xs font-bold cursor-pointer transition-all">
                ✨ 开始创作
              </button>
            </div>
            <div v-else class="text-center py-10">
              <p class="text-gray-600 text-xs">当前筛选无结果</p>
            </div>
          </div>

          <!-- 项目列表（精简卡片） -->
          <div v-else class="space-y-2">
            <div v-for="p in filteredProjects" :key="p.id"
                 @click="openProject(p)"
                 class="border rounded-xl px-4 py-3 flex items-center gap-3 cursor-pointer transition-all group"
                 :class="p.status === 'completed'
                   ? 'bg-green-500/[0.03] border-green-500/20 hover:border-green-500/40'
                   : 'bg-[#141416] border-white/5 hover:border-white/15'">
              
              <!-- 图标 -->
              <div class="w-10 h-10 rounded-lg flex items-center justify-center text-lg shrink-0"
                   :class="getIconBgColor(p.typeCode)">
                {{ getIcon(p.typeCode) }}
              </div>
              
              <div class="flex-1 min-w-0">
                <div class="flex items-center gap-1.5 mb-0.5">
                  <!-- 编辑模式 -->
                  <input v-if="editingProjectId === p.id"
                         ref="titleInputRef"
                         v-model="editingTitle"
                         @blur="saveTitle(p)"
                         @keyup.enter="saveTitle(p)"
                         @keyup.escape="cancelEdit"
                         @click.stop
                         class="text-sm font-bold text-white bg-white/10 border border-[#FF9500]/50 rounded-lg px-2 py-0.5 outline-none w-full max-w-[200px] focus:shadow-[0_0_8px_rgba(255,149,0,0.2)]" />
                  <!-- 展示模式 -->
                  <span v-else
                        @dblclick.stop="startEdit(p)"
                        class="text-sm font-bold text-white truncate group-hover:text-[#FF9500] transition-colors cursor-text"
                        title="双击修改标题">{{ p.title }}</span>
                  <button v-if="editingProjectId !== p.id"
                          @click.stop="startEdit(p)"
                          class="shrink-0 w-5 h-5 rounded flex items-center justify-center text-gray-600 hover:text-[#FF9500] hover:bg-[#FF9500]/10 transition-all cursor-pointer opacity-0 group-hover:opacity-100"
                          title="修改标题">
                    <i class="fas fa-pen text-[8px]"></i>
                  </button>
                  <span v-if="p.status === 'completed'" class="shrink-0 text-[8px] px-1.5 py-0.5 rounded-full bg-green-500/15 text-green-400 font-bold">✅ 已完结</span>
                </div>
                <div class="flex items-center gap-2 text-[10px] text-gray-500">
                  <span class="flex items-center gap-1"><span class="w-1.5 h-1.5 rounded-full" :class="statusBgColor(p.status)"></span>{{ statusLabel(p.status) }}</span>
                  <span>{{ getTypeName(p.typeCode) }}</span>
                  <span>{{ p.totalSections || 0 }} 段</span>
                  <span>{{ formatTime(p.updatedAt || p.createdAt) }}</span>
                </div>
              </div>
              
              <div class="flex items-center gap-1.5 shrink-0">
                <i class="fas fa-chevron-right text-gray-600 text-[10px] group-hover:text-[#FF9500] transition-colors"></i>
                <button @click.stop="deleteProjectConfirm(p)" class="w-7 h-7 rounded-full bg-white/5 flex items-center justify-center text-gray-600 hover:text-red-400 hover:bg-red-500/10 transition-all cursor-pointer" title="删除">
                  <i class="fas fa-trash-alt text-[10px]"></i>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- ✨ 创作类型选择浮层（点击新建后弹出） -->
      <Transition name="sheet">
        <div v-if="showTypePicker" class="fixed inset-0 z-50" @click.self="showTypePicker = false">
          <div class="absolute inset-0 bg-black/60 backdrop-blur-sm" @click="showTypePicker = false"></div>
          <div class="absolute bottom-0 left-0 right-0 bg-[#111] border-t border-white/10 rounded-t-3xl max-h-[80vh] overflow-y-auto p-5 pb-8">
            <div class="w-10 h-1 bg-white/20 rounded-full mx-auto mb-4"></div>
            <h3 class="text-base font-bold mb-1 text-center">✨ 选择创作方向</h3>
            <p class="text-xs text-gray-500 text-center mb-5">选择类型后可直接点击示例快速开始</p>

            <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div v-for="t in availableTemplates" :key="t.id"
                   class="bg-white/[0.03] border border-white/5 rounded-2xl p-4 transition-all group relative hover:border-[#FF9500]/30 hover:bg-[#FF9500]/5">
                <!-- 类型 header -->
                <div class="flex items-center gap-3 mb-2.5 cursor-pointer" @click="isModuleOpen(t.typeCode) ? startCreate(t) : toastStore.show('🚧 ' + t.typeName + ' 敬请期待')">
                  <span class="text-2xl">{{ t.icon }}</span>
                  <div class="flex-1">
                    <div class="text-sm font-bold text-white group-hover:text-[#FF9500] transition-colors">{{ t.typeName }}</div>
                    <div class="text-[10px] text-gray-500 leading-tight">{{ t.description }}</div>
                  </div>
                  <i class="fas fa-arrow-right text-gray-600 text-xs group-hover:text-[#FF9500] transition-colors"></i>
                </div>
                <!-- 示例标签 -->
                <div class="flex flex-wrap gap-1.5">
                  <button v-for="(ex, i) in getExamples(t.typeCode)" :key="i"
                          @click="startCreateWithExample(t, ex)"
                          class="px-2 py-1 rounded-lg bg-white/5 text-[10px] text-gray-400 hover:bg-[#FF9500]/10 hover:text-[#FF9500] transition-all cursor-pointer truncate max-w-[160px]">
                    💡 {{ ex }}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </Transition>

      <!-- 新建项目弹窗（重构） -->
      <div v-if="showCreate" class="fixed inset-0 bg-black/70 backdrop-blur-sm z-[60] flex items-center justify-center p-4"
           @click.self="!creating && (showCreate = false)">
        <div class="bg-[#0d0d10] border border-white/10 rounded-3xl w-full max-w-md overflow-hidden relative shadow-2xl">
          
          <!-- 顶部主题色装饰条 -->
          <div class="h-1.5 w-full" :class="getThemeGradient(createForm.typeCode)"></div>

          <!-- ═══ 输入态 ═══ -->
          <div v-show="!creating" class="p-5">
            <!-- 头部 -->
            <div class="flex items-center gap-3 mb-5">
              <div class="w-11 h-11 rounded-xl flex items-center justify-center text-xl shrink-0"
                   :class="getIconBgColor(createForm.typeCode)">
                {{ createForm.icon }}
              </div>
              <div class="flex-1 min-w-0">
                <h3 class="text-base font-bold text-white">{{ createForm.typeName }}</h3>
                <p class="text-[10px] text-gray-500 truncate">{{ isStructuredType ? '写下灵感，进入工作台后深度创作' : '选择题材 · 输入标题 · 一键开始' }}</p>
              </div>
              <button @click="showCreate = false" class="w-8 h-8 rounded-full bg-white/5 flex items-center justify-center text-gray-500 hover:text-white hover:bg-white/10 transition-all cursor-pointer shrink-0">
                <i class="fas fa-xmark text-sm"></i>
              </button>
            </div>

            <!-- 热门题材选择（结构化类型在工作台里选，这里跳过） -->
            <div v-if="!isStructuredType" class="mb-4">
              <label class="text-xs text-gray-400 font-medium mb-2.5 block flex items-center gap-1.5">
                🔥 选择题材 <span class="text-red-400">*</span>
                <span class="text-[9px] text-gray-600 ml-auto">点选后可修改灵感描述</span>
              </label>
              <div class="grid grid-cols-2 gap-2">
                <button v-for="genre in currentGenres" :key="genre.id"
                        @click="selectGenre(genre)"
                        class="relative overflow-hidden rounded-xl p-3 text-left transition-all duration-200 cursor-pointer group"
                        :class="selectedGenre?.id === genre.id
                          ? 'bg-gradient-to-br ' + genre.gradient + ' shadow-lg scale-[1.02] ring-1 ring-white/20'
                          : 'bg-white/[0.03] hover:bg-white/[0.07] border border-white/5 hover:border-white/10'">
                  <div class="flex items-center gap-2">
                    <span class="text-lg">{{ genre.icon }}</span>
                    <div>
                      <div class="text-xs font-bold" :class="selectedGenre?.id === genre.id ? 'text-white' : 'text-gray-300'">{{ genre.name }}</div>
                      <div class="text-[9px]" :class="selectedGenre?.id === genre.id ? 'text-white/70' : 'text-gray-600'">{{ genre.desc }}</div>
                    </div>
                  </div>
                  <!-- 选中指示器 -->
                  <div v-if="selectedGenre?.id === genre.id"
                       class="absolute top-1.5 right-1.5 w-4 h-4 rounded-full bg-white/20 flex items-center justify-center">
                    <i class="fas fa-check text-white text-[8px]"></i>
                  </div>
                </button>
              </div>
            </div>

            <!-- ═══ 模块专属配置（结构化类型跳过） ═══ -->
            <div v-if="!isStructuredType && selectedGenre && currentExtras" class="mb-4">
              <label class="text-xs text-gray-400 font-medium mb-2 block flex items-center gap-1.5">
                {{ currentExtras.icon }} {{ currentExtras.label }}
              </label>
              <div class="flex gap-2 overflow-x-auto hide-scrollbar pb-1">
                <button v-for="opt in currentExtras.options" :key="opt.id"
                        @click="selectedExtra = opt"
                        class="shrink-0 px-3 py-2 rounded-xl text-xs font-medium transition-all duration-200 cursor-pointer whitespace-nowrap"
                        :class="selectedExtra?.id === opt.id
                          ? 'bg-gradient-to-r ' + currentExtras.gradient + ' text-white shadow-lg scale-[1.03]'
                          : 'bg-white/[0.04] text-gray-400 border border-white/5 hover:bg-white/[0.08] hover:text-gray-200'">
                  {{ opt.icon }} {{ opt.label }}
                </button>
              </div>
              <p v-if="selectedExtra" class="text-[9px] text-gray-500 mt-1.5 pl-0.5">{{ selectedExtra.desc }}</p>
            </div>

            <!-- 项目标题 -->
            <div class="mb-3">
              <div class="flex items-center justify-between mb-1.5">
                <label class="text-xs text-gray-400 font-medium">📝 作品标题</label>
                <span class="text-[9px] text-gray-600">选填 · 留空由 AI 命名</span>
              </div>
              <input v-model="createForm.title" placeholder="给你的作品起个名字..."
                     class="w-full bg-white/5 border border-white/10 rounded-xl px-3.5 py-2.5 text-sm text-white outline-none focus:border-[var(--theme-color,#FF9500)]/50 focus:shadow-[0_0_12px_var(--theme-glow,rgba(255,149,0,0.1))] transition-all" />
            </div>

            <!-- 灵感描述 -->
            <div class="mb-4">
              <div class="flex items-center justify-between mb-1.5">
                <label class="text-xs text-gray-400 font-medium">💡 灵感描述 <span v-if="isStructuredType" class="text-red-400">*</span></label>
                <span class="text-[9px] text-gray-600">{{ createForm.inspiration.length }}/200</span>
              </div>
              <textarea v-model="createForm.inspiration" rows="2"
                        :placeholder="inspirationPlaceholder"
                        class="w-full bg-white/5 border border-white/10 rounded-xl px-3.5 py-2.5 text-sm text-white outline-none focus:border-[#FF9500]/50 focus:shadow-[0_0_12px_rgba(255,149,0,0.1)] resize-none transition-all"></textarea>
              <!-- 灵感提示标签（结构化类型显示，帮助用户快速启动） -->
              <div v-if="isStructuredType" class="flex flex-wrap gap-1.5 mt-2">
                <button v-for="(hint, i) in getExamples(createForm.typeCode)" :key="i"
                        @click="createForm.inspiration = hint"
                        class="px-2.5 py-1 rounded-lg text-[10px] transition-all cursor-pointer"
                        :class="createForm.inspiration === hint
                          ? 'bg-[#FF9500]/20 text-[#FF9500] border border-[#FF9500]/30'
                          : 'bg-white/5 text-gray-500 hover:bg-white/10 hover:text-gray-300 border border-white/5'">
                  💡 {{ hint }}
                </button>
              </div>
            </div>

            <!-- 操作按钮 -->
            <button @click="confirmCreate" :disabled="isStructuredType ? !createForm.inspiration.trim() : !selectedGenre"
                    class="w-full py-3 rounded-xl text-sm font-bold cursor-pointer transition-all flex items-center justify-center gap-2 disabled:opacity-30 disabled:cursor-not-allowed"
                    :class="(isStructuredType ? createForm.inspiration.trim() : selectedGenre)
                      ? getThemeGradient(createForm.typeCode) + ' text-white hover:shadow-lg hover:scale-[1.02] active:scale-[0.98]'
                      : 'bg-white/5 text-gray-500'">
              <i class="fas fa-wand-magic-sparkles"></i>
              {{ isStructuredType ? '✨ 进入工作台' : (createForm.title ? '✨ 开始创作' : '✨ AI 命名并开始') }}
            </button>
          </div>

          <!-- ═══ AI 构思态 ═══ -->
          <div v-show="creating" class="py-16 px-6 flex flex-col items-center justify-center">
            <!-- 发光脉冲球 -->
            <div class="relative mb-8">
              <div class="w-20 h-20 rounded-full flex items-center justify-center relative z-10"
                   :class="getIconBgColor(createForm.typeCode)">
                <span class="text-3xl">{{ createForm.icon }}</span>
              </div>
              <div class="absolute inset-0 rounded-full animate-ping opacity-20" :class="getThemeGradient(createForm.typeCode)"></div>
              <div class="absolute -inset-3 rounded-full animate-pulse opacity-10" :class="getThemeGradient(createForm.typeCode)"></div>
            </div>

            <h3 class="text-base font-bold text-white mb-2">AI 正在构思中...</h3>
            <p class="text-xs text-gray-400 mb-6 transition-all duration-500 h-4 text-center">
              {{ creatingMessages[creatingPhase % creatingMessages.length] }}
            </p>

            <!-- 进度点 -->
            <div class="flex gap-2">
              <div v-for="i in 4" :key="i"
                   class="w-2 h-2 rounded-full transition-all duration-300"
                   :class="i - 1 <= creatingPhase % 4 ? getThemeGradient(createForm.typeCode) : 'bg-white/10'"></div>
            </div>
          </div>
        </div>
      </div>
      <!-- 删除确认弹窗 -->
      <Transition name="sheet">
        <div v-if="showDeleteConfirm" class="fixed inset-0 z-[70] flex items-center justify-center p-6" @click.self="showDeleteConfirm = false">
          <div class="absolute inset-0 bg-black/70 backdrop-blur-sm" @click="showDeleteConfirm = false"></div>
          <div class="relative bg-[#1a1a1c] border border-white/10 rounded-2xl p-6 w-full max-w-sm shadow-2xl">
            <div class="text-center mb-5">
              <div class="w-14 h-14 mx-auto rounded-full bg-red-500/10 flex items-center justify-center mb-3">
                <i class="fas fa-trash-alt text-red-400 text-xl"></i>
              </div>
              <h3 class="text-base font-bold text-white mb-1.5">确认删除</h3>
              <p class="text-xs text-gray-400 leading-relaxed">
                确定删除「<span class="text-white font-bold">{{ deleteTarget?.title }}</span>」？<br/>
                所有段落和音频将一并删除，<span class="text-red-400">此操作不可恢复</span>。
              </p>
            </div>
            <div class="grid grid-cols-2 gap-3">
              <button @click="showDeleteConfirm = false"
                      class="py-2.5 rounded-xl bg-white/5 border border-white/10 text-gray-400 text-sm font-bold cursor-pointer hover:bg-white/10 transition-all">
                取消
              </button>
              <button @click="executeDelete" :disabled="deleting"
                      class="py-2.5 rounded-xl bg-red-500 text-white text-sm font-bold cursor-pointer hover:bg-red-600 transition-all flex items-center justify-center gap-1.5 disabled:opacity-50">
                <i v-if="deleting" class="fas fa-circle-notch fa-spin"></i>
                <i v-else class="fas fa-trash-alt"></i>
                {{ deleting ? '删除中...' : '确认删除' }}
              </button>
            </div>
          </div>
        </div>
      </Transition>

    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { studioApi } from '../api/studio'
import { useToastStore } from '../stores/toast'

const router = useRouter()
const toastStore = useToastStore()
const loading = ref(false)         // 模板首次加载（已弃用，保留兼容）
const loadingProjects = ref(true)  // 项目列表独立加载标志
const templates = ref([])
const projects = ref([])
const showTypePicker = ref(false)
const showCreate = ref(false)
const creating = ref(false)
const creatingPhase = ref(0)
let creatingTimer = null
const activeFilter = ref('all')
const showDeleteConfirm = ref(false)
const deleteTarget = ref(null)
const deleting = ref(false)

// ── 标题内联编辑 ──
const editingProjectId = ref(null)
const editingTitle = ref('')
const titleInputRef = ref(null)

const startEdit = async (project) => {
    editingProjectId.value = project.id
    editingTitle.value = project.title
    await nextTick()
    // ref 在 v-for 中是数组
    const input = Array.isArray(titleInputRef.value) ? titleInputRef.value[0] : titleInputRef.value
    input?.focus()
    input?.select()
}

const saveTitle = async (project) => {
    const newTitle = editingTitle.value.trim()
    if (!newTitle || newTitle === project.title) {
        cancelEdit()
        return
    }
    try {
        await studioApi.updateProject(project.id, { title: newTitle })
        project.title = newTitle
        toastStore.show('标题已更新 ✏️')
    } catch (e) {
        toastStore.show('标题更新失败: ' + (e.message || '未知错误'))
    }
    cancelEdit()
}

const cancelEdit = () => {
    editingProjectId.value = null
    editingTitle.value = ''
}

const creatingMessages = [
  '正在解析你的灵感设定...',
  '正在构建故事大纲与世界观...',
  '正在分配角色与音色...',
  '正在打磨细节与台词...',
  '即将完成，请稍等片刻...'
]

const createForm = ref({
  typeCode: '', typeName: '', icon: '', description: '', title: '', inspiration: ''
})

// 每种类型的示例灵感（降低创作门槛）
const typeExamples = {
  novel: ['重生大女主商战复仇', '高智商死亡游戏博弈', '穿书获得弹幕系统'],
  drama: ['霍总装穷追妻被女主打脸', '重生回到离婚前一天复仇渣男', '闪婚老公竟是全城首富', '室友为了一个外卖展开维权大战'],
  podcast: ['AI对就业市场的影响', '年轻人该不该躺平', '独居生活指南'],
  radio: ['给失恋朋友的治愈信', '深夜孤独时的独白', '毕业季的回忆'],
  lecture: ['通俗讲解量子计算', '5分钟读懂区块链', '经济学入门趣谈'],
  ad: ['降噪耳机运动场景', '智能手表健康监测', '新款咖啡机推荐'],
  picture_book: ['小兔子学会分享', '勇敢的小恐龙冒险', '星空下的萤火虫'],
  news: ['中国航天最新突破', 'AI芯片行业格局', '全球气候变化进展']
}

const getExamples = (typeCode) => typeExamples[typeCode] || []

// 各模块差异化题材预设（资深产品设计）
const selectedGenre = ref(null)

const typeGenres = {
  // ═══ AI小说创作：热门题材方向 ═══
  novel: [
    { id: 'heroine', icon: '👑', name: '无CP大女主', desc: '专注事业/复仇/成长', gradient: 'from-amber-500/80 to-yellow-600/80',
      inspiration: '女主重生后拒绝恋爱脑，以商业天才的手腕一步步建立自己的商业帝国' },
    { id: 'mindgame', icon: '🧠', name: '人性博弈/智斗', desc: '烧脑反转，智力碾压', gradient: 'from-indigo-500/80 to-blue-600/80',
      inspiration: '一场高智商的死亡游戏，参与者必须在严密的规则中找到破局之法' },
    { id: 'brainstorm', icon: '💡', name: '脑洞叠加/系统流', desc: '传统梗+创新脑洞', gradient: 'from-cyan-500/80 to-teal-600/80',
      inspiration: '穿越到古代宅斗世界，却获得了一个来自未来的弹幕系统' },
    { id: 'xianxia', icon: '⚔️', name: '修仙2.0', desc: '职场化修仙，系统升级', gradient: 'from-purple-500/80 to-violet-600/80',
      inspiration: '修仙界也有KPI考核，主角在宗门中像打工人一样从底层逆袭' },
    { id: 'crazy', icon: '🤪', name: '发疯文学', desc: '荒诞反内耗式主角', gradient: 'from-pink-500/80 to-rose-600/80',
      inspiration: '穿越成炮灰配角后，主角决定摆烂发疯，结果所有人都被整破防' },
    { id: 'historical', icon: '📜', name: '考据式穿越', desc: '历史严谨知识爽文', gradient: 'from-orange-500/80 to-amber-600/80',
      inspiration: '穿越到唐朝，用《周礼》和真实典籍知识在朝堂上步步为营' },
    { id: 'vintage', icon: '🏛️', name: '年代高干', desc: '怀旧+阶层逆袭', gradient: 'from-slate-500/80 to-gray-600/80',
      inspiration: '重生回到80年代，凭借前世记忆在改革开放浪潮中从小镇青年到商界大佬' }
  ],

  // ═══ AI短剧：爆款情景对话 ═══
  drama: [
    { id: 'sweet',    icon: '❤️', name: '霸总甜宠', desc: '霍总追妻·闪婚反转', gradient: 'from-pink-500/80 to-rose-600/80',
      inspiration: '霍总装穷追妻被女主打脸' },
    { id: 'revenge',  icon: '🔥', name: '复仇逆袭', desc: '重生复仇·打脸打脸', gradient: 'from-red-500/80 to-orange-600/80',
      inspiration: '重生回到离婚前一天复仇渣男' },
    { id: 'reborn',   icon: '🔮', name: '穿越重生', desc: '带着记忆重来一次', gradient: 'from-purple-500/80 to-violet-600/80',
      inspiration: '穿越成反派女配后戒句戒句主动作死' },
    { id: 'hidden',   icon: '👑', name: '闪婚豪门', desc: '闪婚老公竟是大佬', gradient: 'from-amber-500/80 to-yellow-600/80',
      inspiration: '闪婚老公竟是全城首富' },
    { id: 'campus',   icon: '🎓', name: '校园暗恋', desc: '暗恋·表白·青春疼痛', gradient: 'from-cyan-500/80 to-blue-600/80',
      inspiration: '暗恋了三年的学姐毕业那天终于说出口' },
    { id: 'ancient',  icon: '🎯', name: '古装宫斗', desc: '后宫·权谋·女尊', gradient: 'from-emerald-500/80 to-teal-600/80',
      inspiration: '后宫贵妃们的权力博弈，每个人都有不可告人的秘密' },
    { id: 'silly',    icon: '🤪', name: '沙雕搞笑', desc: '反套路·爆笑·深度发疯', gradient: 'from-lime-500/80 to-green-600/80',
      inspiration: '三个室友为了一个外卖的归属权展开维权大战' }
  ],

  // ═══ AI播客：话题讨论 ═══
  podcast: [
    { id: 'tech',     icon: '💻', name: '科技前沿', desc: 'AI/芯片/互联网', gradient: 'from-blue-500/80 to-cyan-500/80',
      inspiration: '2026年AI对程序员就业市场的真实影响' },
    { id: 'career',   icon: '📈', name: '职场成长', desc: '升职/跳槽/副业', gradient: 'from-green-500/80 to-emerald-500/80',
      inspiration: '年轻人到底该卷还是该躺平？聊聊职场生存之道' },
    { id: 'life',     icon: '🧘', name: '生活方式', desc: '独居/旅行/效率', gradient: 'from-purple-500/80 to-pink-500/80',
      inspiration: '独居生活指南：如何把一个人的日子过得精彩' },
    { id: 'finance',  icon: '💰', name: '理财投资', desc: '基金/股票/房产', gradient: 'from-yellow-500/80 to-amber-500/80',
      inspiration: '月薪一万如何做好理财规划？从零开始的投资指南' },
    { id: 'culture',  icon: '🎬', name: '影视文化', desc: '电影/剧集/综艺', gradient: 'from-red-500/80 to-rose-500/80',
      inspiration: '聊聊近期最火的国产剧，为什么能爆？' },
    { id: 'social',   icon: '🌍', name: '社会热点', desc: '现象/趋势/观点', gradient: 'from-indigo-500/80 to-blue-500/80',
      inspiration: '为什么越来越多年轻人选择不结婚？' }
  ],

  // ═══ 情感电台：情感场景 ═══
  radio: [
    { id: 'heal',     icon: '🌙', name: '深夜治愈', desc: '温柔陪伴入眠', gradient: 'from-indigo-600/80 to-purple-600/80',
      inspiration: '给深夜失眠的你，一段温暖的睡前絮语' },
    { id: 'breakup',  icon: '💔', name: '失恋疗愈', desc: '走出情感低谷', gradient: 'from-rose-500/80 to-pink-600/80',
      inspiration: '给刚失恋的朋友写一段温暖的治愈信' },
    { id: 'letter',   icon: '✉️', name: '情感书信', desc: '写给重要的人', gradient: 'from-amber-500/80 to-orange-500/80',
      inspiration: '写给十年后自己的一封信：关于梦想和坚持' },
    { id: 'memory',   icon: '🎓', name: '青春回忆', desc: '毕业季的感动', gradient: 'from-green-500/80 to-teal-500/80',
      inspiration: '致我们终将逝去的青春——毕业季的回忆与不舍' },
    { id: 'courage',  icon: '⭐', name: '励志鼓励', desc: '给自己打气', gradient: 'from-yellow-500/80 to-orange-500/80',
      inspiration: '致正在低谷中的你：每一次跌倒都是下一次飞翔的起点' },
    { id: 'lonely',   icon: '🌃', name: '孤独独白', desc: '一个人的夜晚', gradient: 'from-slate-600/80 to-gray-700/80',
      inspiration: '城市霓虹灯下，一个异乡人的深夜独白' }
  ],

  // ═══ 知识讲解：学科领域 ═══
  lecture: [
    { id: 'science',  icon: '🔬', name: '自然科学', desc: '物理/化学/生物', gradient: 'from-blue-500/80 to-indigo-500/80',
      inspiration: '用通俗的方式讲解量子纠缠：超越时空的神秘联系' },
    { id: 'history',  icon: '📜', name: '历史人文', desc: '故事化讲历史', gradient: 'from-amber-600/80 to-yellow-600/80',
      inspiration: '5分钟讲清楚唐朝的兴衰：从贞观之治到安史之乱' },
    { id: 'econ',     icon: '📊', name: '经济商业', desc: '商业/金融/趋势', gradient: 'from-green-500/80 to-emerald-500/80',
      inspiration: '经济学入门趣谈：为什么超市总把牛奶放在最里面' },
    { id: 'tech_lec', icon: '⚙️', name: '科技解读', desc: 'AI/区块链/芯片', gradient: 'from-cyan-500/80 to-blue-500/80',
      inspiration: '5分钟读懂大模型：ChatGPT到底是怎么工作的' },
    { id: 'psych',    icon: '🧠', name: '心理学', desc: '认知/行为/情绪', gradient: 'from-purple-500/80 to-violet-500/80',
      inspiration: '为什么我们总是拖延？心理学告诉你背后的真相' },
    { id: 'astro',    icon: '🌌', name: '天文宇宙', desc: '星系/黑洞/探索', gradient: 'from-slate-600/80 to-indigo-700/80',
      inspiration: '如果太阳突然消失，地球会怎样？' }
  ],

  // ═══ 带货文案：产品场景 ═══
  ad: [
    { id: 'digital',  icon: '📱', name: '数码科技', desc: '手机/耳机/智能', gradient: 'from-blue-500/80 to-cyan-500/80',
      inspiration: '一款主动降噪耳机在健身房和通勤场景的使用体验' },
    { id: 'beauty',   icon: '💄', name: '美妆护肤', desc: '护肤/彩妆/个护', gradient: 'from-pink-500/80 to-rose-500/80',
      inspiration: '一款氨基酸洗面奶的温和清洁体验和使用场景' },
    { id: 'food',     icon: '🍜', name: '美食饮品', desc: '零食/饮料/餐饮', gradient: 'from-orange-500/80 to-red-500/80',
      inspiration: '一款手工精酿咖啡的独特风味和冲泡仪式感' },
    { id: 'home',     icon: '🏡', name: '家居生活', desc: '家电/收纳/装饰', gradient: 'from-amber-500/80 to-yellow-500/80',
      inspiration: '一台智能扫地机器人如何改变你的居家生活' },
    { id: 'fashion',  icon: '👗', name: '服饰穿搭', desc: '服装/鞋包/配饰', gradient: 'from-purple-500/80 to-pink-500/80',
      inspiration: '一件轻薄羽绒服从办公室到户外的多场景穿搭' },
    { id: 'health',   icon: '💪', name: '健康运动', desc: '健身/营养/保健', gradient: 'from-green-500/80 to-emerald-500/80',
      inspiration: '一款蛋白粉在运动后恢复和增肌方面的专业表现' }
  ],

  // ═══ 有声绘本：儿童故事主题 ═══
  picture_book: [
    { id: 'share',    icon: '🐰', name: '学会分享', desc: '友善与关爱', gradient: 'from-pink-400/80 to-rose-400/80',
      inspiration: '小兔子不愿意分享胡萝卜，直到发现分享带来更大的快乐' },
    { id: 'brave',    icon: '🦕', name: '勇气冒险', desc: '克服恐惧探索', gradient: 'from-green-400/80 to-emerald-400/80',
      inspiration: '一只害怕高处的小恐龙鼓起勇气攀上高山寻找彩虹' },
    { id: 'dream',    icon: '🌟', name: '追逐梦想', desc: '坚持与努力', gradient: 'from-yellow-400/80 to-amber-400/80',
      inspiration: '一只想飞的小企鹅，经过无数次尝试终于实现了梦想' },
    { id: 'enviro',   icon: '🌍', name: '环保自然', desc: '爱护地球家园', gradient: 'from-teal-400/80 to-cyan-400/80',
      inspiration: '森林里的小动物们一起合作清理河流里的垃圾' },
    { id: 'emotion',  icon: '🎭', name: '情绪管理', desc: '认识和表达感受', gradient: 'from-purple-400/80 to-violet-400/80',
      inspiration: '小熊今天很生气，妈妈教他认识和管理自己的情绪' },
    { id: 'bedtime',  icon: '🌙', name: '睡前故事', desc: '温馨安睡', gradient: 'from-indigo-400/80 to-blue-400/80',
      inspiration: '月亮婆婆带着星星宝宝们在夜空中巡游的温馨故事' }
  ],

  // ═══ 新闻播报：新闻领域 ═══
  news: [
    { id: 'tech_news',icon: '🤖', name: 'AI与科技', desc: '人工智能/芯片', gradient: 'from-blue-500/80 to-cyan-500/80',
      inspiration: '最新AI芯片突破：算力提升10倍意味着什么' },
    { id: 'space',    icon: '🛰️', name: '航天航空', desc: '火箭/卫星/探月', gradient: 'from-slate-500/80 to-indigo-600/80',
      inspiration: '中国航天最新突破：载人月球探测任务关键进展' },
    { id: 'economy',  icon: '📉', name: '财经金融', desc: '股市/政策/趋势', gradient: 'from-green-500/80 to-emerald-500/80',
      inspiration: '央行最新政策解读：对普通人的钱袋子有什么影响' },
    { id: 'world',    icon: '🌐', name: '国际时事', desc: '外交/地缘/格局', gradient: 'from-red-500/80 to-orange-500/80',
      inspiration: '全球供应链变局：各国如何重新布局产业格局' },
    { id: 'climate',  icon: '🌡️', name: '气候环境', desc: '碳中和/极端天气', gradient: 'from-teal-500/80 to-green-500/80',
      inspiration: '极端天气频发：全球气候变化的最新科学发现' },
    { id: 'society',  icon: '🏫', name: '社会民生', desc: '教育/医疗/就业', gradient: 'from-amber-500/80 to-yellow-500/80',
      inspiration: '2026年就业市场趋势报告：哪些行业逆势增长' }
  ]
}

// 结构化类型：题材选择在工作台里做，创建弹窗只需灵感
const structuredTypes = ['novel', 'drama']
const isStructuredType = computed(() => structuredTypes.includes(createForm.value.typeCode))

// 根据当前类型动态获取题材列表
const currentGenres = computed(() => typeGenres[createForm.value.typeCode] || typeGenres.novel)

const selectGenre = (genre) => {
  selectedGenre.value = genre
  selectedExtra.value = null // 切换题材时重置专属配置
  createForm.value.inspiration = genre.inspiration
}

// ═══ 各模块专属配置项 ═══
const selectedExtra = ref(null)

const typeExtraOptions = {
  // 广播剧：角色数量
  drama: {
    icon: '🎬', label: '角色数量', gradient: 'from-pink-500/80 to-purple-600/80',
    options: [
      { id: 'duo',   icon: '👥', label: '双人对话', desc: '两个角色的深度对话，适合情感细腻的故事' },
      { id: 'trio',  icon: '👥+', label: '三人群演', desc: '三角关系、团队协作等多维度互动' },
      { id: 'multi', icon: '🎭', label: '多角色群像', desc: '4个以上角色，适合宏大叙事' }
    ]
  },
  // AI播客：节目风格
  podcast: {
    icon: '🗣️', label: '节目风格', gradient: 'from-cyan-500/80 to-blue-500/80',
    options: [
      { id: 'casual',  icon: '☕', label: '轻松闲聊', desc: '朋友之间的自然对话，氛围轻松' },
      { id: 'deep',    icon: '🎯', label: '深度访谈', desc: '有深度的一问一答，专业感强' },
      { id: 'debate',  icon: '⚡', label: '辩论对决', desc: '正反方激烈讨论，观点碰撞' }
    ]
  },
  // 情感电台：情绪氛围
  radio: {
    icon: '💫', label: '情绪氛围', gradient: 'from-indigo-500/80 to-purple-500/80',
    options: [
      { id: 'gentle', icon: '🌸', label: '温柔治愈', desc: '轻声细语，如春风拂面' },
      { id: 'sad',    icon: '🌧️', label: '感伤回忆', desc: '低沉深情，触动心弦' },
      { id: 'passion',icon: '🔥', label: '热血励志', desc: '激昂有力，点燃斗志' },
      { id: 'quiet',  icon: '🌿', label: '安静陪伴', desc: '不急不缓，静静守候' }
    ]
  },
  // 知识讲解：难度等级
  lecture: {
    icon: '🎯', label: '难度等级', gradient: 'from-green-500/80 to-emerald-500/80',
    options: [
      { id: 'easy',   icon: '🌱', label: '入门科普', desc: '零基础也能听懂，生动有趣' },
      { id: 'mid',    icon: '📖', label: '进阶解读', desc: '有一定基础，深入浅出' },
      { id: 'pro',    icon: '🔬', label: '专业深度', desc: '面向专业人士，严谨详实' }
    ]
  },
  // 带货文案：文案风格
  ad: {
    icon: '📣', label: '文案风格', gradient: 'from-orange-500/80 to-red-500/80',
    options: [
      { id: 'seed',   icon: '🌟', label: '种草安利', desc: '分享型，强调个人体验和真实感受' },
      { id: 'review', icon: '📊', label: '测评对比', desc: '客观分析，突出产品优势数据' },
      { id: 'story',  icon: '📖', label: '故事植入', desc: '用故事场景巧妙带出产品卖点' }
    ]
  },
  // 有声绘本：适龄范围
  picture_book: {
    icon: '👶', label: '适龄范围', gradient: 'from-pink-400/80 to-rose-400/80',
    options: [
      { id: 'baby',   icon: '🍼', label: '2-4岁', desc: '简单句式，重复节奏，色彩鲜明' },
      { id: 'kid',    icon: '🧒', label: '4-6岁', desc: '情节完整，角色丰富，寓教于乐' },
      { id: 'child',  icon: '📚', label: '6-8岁', desc: '故事稍长，引导思考，培养阅读习惯' }
    ]
  },
  // 新闻播报：播报风格
  news: {
    icon: '📺', label: '播报风格', gradient: 'from-blue-500/80 to-indigo-500/80',
    options: [
      { id: 'formal',  icon: '🎙️', label: '正式播报', desc: '央视风格，庄重权威' },
      { id: 'casual',  icon: '💬', label: '通俗解读', desc: '用大白话讲明白新闻背后的事' },
      { id: 'comment', icon: '🧐', label: '评论分析', desc: '加入独到见解和深度分析' }
    ]
  }
}

// 当前类型的专属配置
const currentExtras = computed(() => typeExtraOptions[createForm.value.typeCode] || null)

const filterTabs = computed(() => {
  const all = projects.value.length
  const drafts = projects.value.filter(p => p.status === 'draft').length
  const active = projects.value.filter(p => p.status === 'creating' || p.status === 'editing').length
  const done = projects.value.filter(p => p.status === 'completed').length
  return [
    { key: 'all', label: '全部', count: all },
    { key: 'draft', label: '草稿', count: drafts },
    { key: 'active', label: '进行中', count: active },
    { key: 'completed', label: '已完成', count: done }
  ]
})

const filteredProjects = computed(() => {
  if (activeFilter.value === 'all') return projects.value
  if (activeFilter.value === 'draft') return projects.value.filter(p => p.status === 'draft')
  if (activeFilter.value === 'active') return projects.value.filter(p => p.status === 'creating' || p.status === 'editing')
  if (activeFilter.value === 'completed') return projects.value.filter(p => p.status === 'completed')
  return projects.value
})

const inspirationPlaceholder = computed(() => {
  const map = {
    novel: '例如：一个程序员穿越到修仙世界，用编程思维修炼...',
    drama: '例如：霍总装穷追妻被女主打脸，结果被日日打脸...',
    podcast: '例如：聊聊2026年AI对程序员就业的影响',
    radio: '例如：给刚失恋的朋友写一段温暖的话',
    lecture: '例如：用通俗的方式讲解量子计算',
    ad: '例如：推荐一款降噪耳机，主打运动场景',
    picture_book: '例如：小兔子学会分享的故事，适合4岁孩子',
    news: '例如：2026年中国航天最新进展'
  }
  return map[createForm.value.typeCode] || '描述你想创作的内容...'
})

// 当前开放的模块（后续调整只改这里）
const openModules = ['novel', 'podcast', 'drama', 'radio', 'lecture', 'ad', 'picture_book', 'news']
const isModuleOpen = (typeCode) => openModules.includes(typeCode)

// 只返回已开放的模块
const availableTemplates = computed(() => {
  return templates.value.filter(t => isModuleOpen(t.typeCode))
})

// 已开放的排前面（兼容）
const sortedTemplates = computed(() => {
  const open = templates.value.filter(t => isModuleOpen(t.typeCode))
  const locked = templates.value.filter(t => !isModuleOpen(t.typeCode))
  return [...open, ...locked]
})

// 模块功能标签
const getModuleTags = (typeCode) => {
  const map = {
    novel: ['AI 续写', '多题材', '自动配音'],
    podcast: ['双人对话', 'AI 配音', '流式生成'],
    drama: ['AI短剧', '情景对话', '爆款剧本'],
    radio: ['情感独白', '氛围设计', '治愈系'],
    lecture: ['知识科普', '深入浅出', '专业解读'],
    ad: ['种草文案', '场景植入', '卖点提炼'],
    picture_book: ['儿童故事', '教育启蒙', '亲子共读'],
    news: ['新闻撰稿', '播报风格', '热点解读'],
  }
  return map[typeCode] || []
}

// 模板缓存：localStorage TTL 1小时，静态数据不需要每次请求接口
const TEMPLATE_CACHE_KEY = 'sr_studio_templates'
const TEMPLATE_CACHE_TTL = 3600_000 // 1h ms

const loadTemplates = async () => {
  // 先读缓存
  try {
    const cached = localStorage.getItem(TEMPLATE_CACHE_KEY)
    if (cached) {
      const { ts, data } = JSON.parse(cached)
      if (Date.now() - ts < TEMPLATE_CACHE_TTL) {
        templates.value = data
        return // 命中缓存，直接返回
      }
    }
  } catch (e) { /* ignore */ }

  // 缓存未命中，请求接口
  try {
    const data = await studioApi.getTemplates()
    templates.value = data
    localStorage.setItem(TEMPLATE_CACHE_KEY, JSON.stringify({ ts: Date.now(), data }))
  } catch (e) {
    toastStore.show('加载创作类型失败')
  }
}


const loadProjects = async () => {
  try { projects.value = await studioApi.listProjects() } catch (e) { console.error('加载项目列表失败:', e) }
}

const startCreate = (template) => {
  // AI 双播直接跳转到独立页面
  if (template.typeCode === 'podcast') {
    showTypePicker.value = false
    router.push('/podcast')
    return
  }
  createForm.value = {
    typeCode: template.typeCode, typeName: template.typeName,
    icon: template.icon, description: template.description,
    title: '', inspiration: ''
  }
  showTypePicker.value = false
  showCreate.value = true
  selectedGenre.value = null
}

/** 点击示例标签 → 直接预填灵感 */
const startCreateWithExample = (template, example) => {
  // AI 双播直接跳转到独立页面
  if (template.typeCode === 'podcast') {
    showTypePicker.value = false
    router.push('/podcast')
    return
  }
  createForm.value = {
    typeCode: template.typeCode, typeName: template.typeName,
    icon: template.icon, description: template.description,
    title: '', inspiration: example
  }
  showTypePicker.value = false
  showCreate.value = true
}

/** ⚡ 灵感标签一键开始 — 填入灵感后直接创建 */
const quickStartWithInspiration = (inspiration) => {
  createForm.value.inspiration = inspiration
  confirmCreate()
}

/** 🎲 AI 智能灵感生成 */
const rollInspiration = async () => {
  if (loadingInspirations.value) return
  loadingInspirations.value = true
  try {
    const res = await studioApi.getInspiration(createForm.value.typeCode)
    // 后端返回 JSON 数组字符串
    const data = typeof res === 'string' ? JSON.parse(res) : (res.data ? (typeof res.data === 'string' ? JSON.parse(res.data) : res.data) : res)
    if (Array.isArray(data) && data.length > 0) {
      aiInspirations.value = data
      // 自动填入第一个灵感
      createForm.value.inspiration = data[0]
    }
  } catch (e) {
    console.error('AI 灵感生成失败:', e)
    // 回退到本地示例
    const examples = getExamples(createForm.value.typeCode)
    if (examples.length) {
      createForm.value.inspiration = examples[Math.floor(Math.random() * examples.length)]
    }
    toastStore.show('灵感生成失败，已使用预设示例')
  } finally {
    loadingInspirations.value = false
  }
}

const confirmCreate = async () => {
  // 结构化类型只需灵感，非结构化类型需要题材
  if (isStructuredType.value) {
    if (!createForm.value.inspiration.trim()) {
      toastStore.show('请输入创作灵感 💡')
      return
    }
  } else {
    if (!selectedGenre.value) {
      toastStore.show('请先选择一个题材 🔥')
      return
    }
    if (!createForm.value.inspiration.trim()) {
      toastStore.show('请输入创作灵感 💡')
      return
    }
  }
  // 标题留空时从灵感词自动截取
  if (!createForm.value.title.trim()) {
    const insp = createForm.value.inspiration.trim()
    createForm.value.title = insp.length > 12 ? insp.substring(0, 12) + '...' : insp
  }
  // 启动 AI 构思态
  creating.value = true
  creatingPhase.value = 0
  creatingTimer = setInterval(() => { creatingPhase.value++ }, 2000)
  try {
    const project = await studioApi.createProject({
      typeCode: createForm.value.typeCode,
      title: createForm.value.title,
      inspiration: createForm.value.inspiration
    })
    showCreate.value = false
    router.push(`/studio/${project.id}`)
  } catch (e) {
    toastStore.show('创建失败: ' + (e.message || '未知错误'))
  } finally {
    creating.value = false
    clearInterval(creatingTimer)
  }
}

const openProject = (p) => router.push(`/studio/${p.id}`)

const deleteProjectConfirm = (p) => {
  deleteTarget.value = p
  showDeleteConfirm.value = true
}

const executeDelete = async () => {
  if (!deleteTarget.value || deleting.value) return
  deleting.value = true
  try {
    await studioApi.deleteProject(deleteTarget.value.id)
    toastStore.show('项目已删除 🗑️')
    showDeleteConfirm.value = false
    deleteTarget.value = null
    await loadProjects()
  } catch (e) {
    toastStore.show('删除失败: ' + (e.message || '未知错误'))
  } finally {
    deleting.value = false
  }
}
const getIcon = (typeCode) => { const t = templates.value.find(t => t.typeCode === typeCode); return t?.icon || '📝' }
const getTypeName = (typeCode) => { const t = templates.value.find(t => t.typeCode === typeCode); return t?.typeName || typeCode }

const getTemplateBg = (typeCode) => {
  const map = {
    novel: 'bg-gradient-to-br from-blue-900/40 to-indigo-900/40 hover:border-indigo-500/30',
    drama: 'bg-gradient-to-br from-purple-900/40 to-fuchsia-900/40 hover:border-fuchsia-500/30',
    podcast: 'bg-gradient-to-br from-teal-900/40 to-emerald-900/40 hover:border-emerald-500/30',
    radio: 'bg-gradient-to-br from-orange-900/40 to-yellow-900/40 hover:border-orange-500/30',
    lecture: 'bg-gradient-to-br from-rose-900/40 to-pink-900/40 hover:border-rose-500/30',
  }
  return map[typeCode] || 'bg-gradient-to-br from-gray-800 to-gray-900 hover:border-gray-500/30'
}

const getIconBgColor = (typeCode) => {
  const map = {
    novel: 'bg-gradient-to-br from-blue-500 to-cyan-400 text-white shadow-[0_0_15px_rgba(59,130,246,0.3)]',
    drama: 'bg-gradient-to-br from-purple-500 to-pink-500 text-white shadow-[0_0_15px_rgba(168,85,247,0.3)]',
    podcast: 'bg-gradient-to-br from-teal-400 to-emerald-500 text-white shadow-[0_0_15px_rgba(45,212,191,0.3)]',
    radio: 'bg-gradient-to-br from-orange-400 to-yellow-500 text-white shadow-[0_0_15px_rgba(249,115,22,0.3)]',
    lecture: 'bg-gradient-to-br from-rose-400 to-red-500 text-white shadow-[0_0_15px_rgba(251,113,133,0.3)]'
  }
  return map[typeCode] || 'bg-white/10 text-white'
}

const getThemeGradient = (typeCode) => {
  const map = {
    novel: 'bg-gradient-to-r from-blue-500 to-cyan-400',
    drama: 'bg-gradient-to-r from-purple-500 to-pink-500',
    podcast: 'bg-gradient-to-r from-teal-400 to-emerald-500',
    radio: 'bg-gradient-to-r from-orange-400 to-yellow-500',
    lecture: 'bg-gradient-to-r from-rose-400 to-red-500'
  }
  return map[typeCode] || 'bg-gradient-to-r from-gray-500 to-gray-400'
}

const statusLabel = (s) => ({ draft: '草稿', creating: 'AI创作中', editing: '编辑中', completed: '已完成' }[s] || s)
const statusColor = (s) => ({ draft: 'text-gray-500', creating: 'text-orange-400', editing: 'text-blue-400', completed: 'text-green-400' }[s] || '')
const statusBgColor = (s) => ({ draft: 'bg-gray-500', creating: 'bg-orange-400 animate-pulse', editing: 'bg-blue-400', completed: 'bg-green-400' }[s] || 'bg-gray-500')
const formatTime = (t) => {
  if (!t) return '-'
  const d = t.replace('T', ' ').substring(0, 16)
  const today = new Date().toISOString().substring(0, 10)
  if (d.startsWith(today)) return '今天 ' + d.substring(11)
  return d.substring(5)
}

onMounted(async () => {
  // 模板优先从缓存加载（同步，命中则无网络请求）
  await loadTemplates()

  // 项目列表独立异步加载，不阻塞模板展示
  loadingProjects.value = true
  loadProjects().finally(() => { loadingProjects.value = false })
})

onUnmounted(() => {
  if (creatingTimer) clearInterval(creatingTimer)
})
</script>

<style scoped>
.hide-scrollbar { -ms-overflow-style: none; scrollbar-width: none; }
.hide-scrollbar::-webkit-scrollbar { display: none; }
/* 底部浮层动画 */
.sheet-enter-active, .sheet-leave-active { transition: all 0.3s ease; }
.sheet-enter-active > :last-child, .sheet-leave-active > :last-child { transition: transform 0.3s ease; }
.sheet-enter-from, .sheet-leave-to { opacity: 0; }
.sheet-enter-from > :last-child { transform: translateY(100%); }
.sheet-leave-to > :last-child { transform: translateY(100%); }
/* 输入框抖动 */
@keyframes shake {
  0%, 100% { transform: translateX(0); }
  15%, 45%, 75% { transform: translateX(-6px); }
  30%, 60%, 90% { transform: translateX(6px); }
}
.animate-shake { animation: shake 0.5s ease-in-out; }
</style>
