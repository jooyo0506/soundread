<template>
  <div class="min-h-screen bg-[#0a0a0a] text-white pb-20">

    <!-- Header -->
    <div class="sticky top-0 z-30 bg-[#0a0a0a]/95 backdrop-blur-sm border-b border-white/5 px-4 py-3.5">
      <div class="max-w-5xl mx-auto flex items-center gap-3">
        <!-- 返回按钮（纯图标） -->
        <button @click="$router.push('/studio')"
                class="w-8 h-8 rounded-full bg-white/5 hover:bg-white/10 flex items-center justify-center cursor-pointer transition-all shrink-0">
          <i class="fas fa-chevron-left text-xs text-gray-400"></i>
        </button>
        <!-- 标题 -->
        <div class="flex-1 min-w-0">
          <h1 class="text-sm font-bold flex items-center gap-1.5 truncate">
            <span>{{ project?.typeCode ? getIcon(project.typeCode) : '📝' }}</span>
            <span class="truncate">{{ project?.title || '加载中...' }}</span>
          </h1>
        </div>
        <!-- 状态 & 操作 -->
        <div class="flex gap-2 items-center shrink-0">
          <!-- 播客项目：显示专属状态 -->
          <template v-if="project?.typeCode === 'podcast'">
            <span v-if="project?.status === 'completed'" class="px-2 py-0.5 text-[9px] rounded-full bg-green-500/20 text-green-400 flex items-center gap-1">
              <span class="w-1 h-1 rounded-full bg-green-400 animate-pulse"></span>已上架
            </span>
            <span v-else class="px-2 py-0.5 text-[9px] rounded-full bg-gray-500/20 text-gray-400">待上架</span>
          </template>
          <!-- 其他项目：通用状态 -->
          <template v-else>
            <span class="px-2 py-0.5 text-[9px] rounded-full" :class="statusBadge(project?.status)">
              {{ statusLabel(project?.status) }}
            </span>
          </template>
          <button v-if="hasSynthesized && project?.status !== 'completed' && project?.typeCode !== 'podcast'" @click="publishProject" :disabled="publishing"
                  class="px-3 py-1.5 rounded-lg text-[10px] font-bold cursor-pointer transition-all"
                  :class="publishing ? 'bg-gray-700 text-gray-400' : 'bg-purple-600 text-white hover:bg-purple-500'">
            <i v-if="publishing" class="fas fa-circle-notch fa-spin"></i>
            {{ publishing ? '...' : '📤 发布' }}
          </button>
          <span v-if="project?.status === 'completed' && project?.typeCode !== 'podcast'" class="text-[9px] text-green-400">✅ 已发布</span>
        </div>
      </div>
    </div>

    <div v-if="loading" class="text-center py-20 text-gray-500">
      <i class="fas fa-circle-notch fa-spin text-2xl mb-3"></i>
      <p>加载项目中...</p>
    </div>

    <div v-else class="max-w-5xl mx-auto px-4">

      <!-- ══════════ 多章节模式（小说、广播剧）══════════ -->
      <template v-if="isMultiSection">

      <!-- ═══════ 结构化创作流程（小说/广播剧共用） ═══════ -->
      <template v-if="isStructuredMode">
        
        <!-- 步骤导航器 -->
        <div class="flex items-center gap-1 py-4 overflow-x-auto hide-scrollbar">
          <template v-for="(step, idx) in novelSteps" :key="step.key">
            <button 
              @click="canGoToStep(step.key) && (novelStep = step.key)"
              class="flex items-center gap-1.5 px-3 py-2 rounded-xl text-[11px] font-bold transition-all whitespace-nowrap shrink-0"
              :class="novelStep === step.key 
                ? 'bg-gradient-to-r from-blue-500 to-cyan-400 text-white shadow-lg shadow-blue-500/20' 
                : isStepLocked(step.key)
                  ? 'bg-green-500/5 text-green-400/60 border border-green-500/10 cursor-pointer'
                  : canGoToStep(step.key)
                    ? 'bg-white/5 text-gray-400 hover:bg-white/10 hover:text-gray-200 cursor-pointer'
                    : 'bg-white/[0.02] text-gray-600 cursor-not-allowed'"
            >
              <span class="w-5 h-5 rounded-full flex items-center justify-center text-[9px] font-black"
                    :class="isStepLocked(step.key) ? 'bg-green-500/20 text-green-400' : isStepDone(step.key) ? 'bg-green-500 text-white' : novelStep === step.key ? 'bg-white/20 text-white' : 'bg-white/5 text-gray-500'">
                <i v-if="isStepLocked(step.key)" class="fas fa-lock text-[7px]"></i>
                <i v-else-if="isStepDone(step.key)" class="fas fa-check"></i>
                <span v-else>{{ idx + 1 }}</span>
              </span>
              {{ step.label }}
            </button>
            <i v-if="idx < novelSteps.length - 1" class="fas fa-chevron-right text-[8px] text-gray-700 shrink-0"></i>
          </template>
        </div>

        <!-- ==================== Step 1: 核心构思 ==================== -->
        <div v-show="novelStep === 'conceive'" class="space-y-4">
          <div v-if="isStepLocked('conceive')" class="bg-green-500/5 border border-green-500/20 rounded-xl px-3 py-2 flex items-center gap-2 text-[10px] text-green-400">
            <i class="fas fa-lock"></i> 已锁定·查看模式
          </div>
          
          <!-- 创作灵感（可编辑） -->
          <div class="bg-[#1a1a1c] border border-white/5 rounded-2xl p-4">
            <h3 class="text-xs font-bold text-gray-400 mb-3 flex items-center gap-1.5">
              <i class="fas fa-lightbulb text-amber-400"></i> 创作灵感
              <span class="text-[9px] text-gray-600 ml-auto">可编辑</span>
            </h3>
            <textarea v-model="editableInspiration"
                      @blur="saveInspiration"
                      rows="2"
                      placeholder="描述你想创作的内容..."
                      class="w-full bg-white/5 border border-white/10 rounded-xl px-3 py-2 text-sm text-white/80 leading-relaxed outline-none focus:border-amber-500/40 focus:shadow-[0_0_8px_rgba(245,158,11,0.1)] resize-none transition-all"></textarea>
          </div>

          <!-- 题材方向 -->
          <div class="bg-[#1a1a1c] border border-white/5 rounded-2xl p-4">
            <h3 class="text-xs font-bold text-gray-400 mb-3 flex items-center gap-1.5">
              <i class="fas fa-fire text-orange-400"></i> 题材方向
              <span class="ml-auto text-[9px] text-gray-600">{{ isDramaType ? 'AI短剧风格' : '番茄热门榜' }}</span>
            </h3>
            <div class="space-y-2">
              <button v-for="genre in activeGenres" :key="genre.id"
                      @click="selectStyle(genre)"
                      class="w-full rounded-xl p-3 text-left cursor-pointer transition-all flex items-start gap-3"
                      :class="stylePreference === genre.id 
                        ? 'bg-gradient-to-br ' + genre.gradient + ' ring-1 ring-white/20 scale-[1.01]' 
                        : 'bg-white/[0.03] border border-white/5 hover:bg-white/[0.06]'">
                <span class="text-2xl mt-0.5">{{ genre.icon }}</span>
                <div class="flex-1 min-w-0">
                  <div class="flex items-center gap-2">
                    <span class="text-[12px] font-bold" :class="stylePreference === genre.id ? 'text-white' : 'text-gray-300'">{{ genre.name }}</span>
                    <span class="text-[9px]">{{ '🔥'.repeat(genre.heat) }}</span>
                    <span class="ml-auto px-1.5 py-0.5 rounded text-[8px] font-bold"
                          :class="stylePreference === genre.id ? 'bg-white/20 text-white' : 'bg-white/5 text-gray-500'">{{ genre.pov }}</span>
                  </div>
                  <div class="text-[10px] mt-0.5 leading-relaxed" :class="stylePreference === genre.id ? 'text-white/70' : 'text-gray-600'">{{ genre.desc }}</div>
                </div>
              </button>
            </div>
          </div>

          <!-- 创作要点提示 -->
          <div v-if="selectedGenre" class="bg-gradient-to-br from-amber-500/5 to-orange-500/5 border border-amber-500/15 rounded-2xl p-4">
            <h3 class="text-xs font-bold text-amber-400 mb-2 flex items-center gap-1.5">
              <i class="fas fa-magic"></i> {{ selectedGenre.name }}创作要点
            </h3>
            <ul class="space-y-1.5">
              <li v-for="(tip, i) in selectedGenre.tips.split('；')" :key="i" class="text-[10px] text-gray-400 leading-relaxed flex items-start gap-1.5">
                <span class="text-amber-500 mt-0.5">•</span>
                <span>{{ tip }}</span>
              </li>
            </ul>
          </div>

          <!-- 目标章/幕数 -->
          <div class="bg-[#1a1a1c] border border-white/5 rounded-2xl p-4">
            <h3 class="text-xs font-bold text-gray-400 mb-3 flex items-center gap-1.5">
              <i class="fas fa-list-ol text-cyan-400"></i> {{ isDramaType ? '目标幕数' : '目标章数' }}
            </h3>
            <div class="flex gap-2">
              <button v-for="opt in activeChapterOptions" :key="opt.value"
                      @click="targetChapters = opt.value"
                      class="flex-1 py-3 rounded-xl text-center cursor-pointer transition-all"
                      :class="targetChapters === opt.value 
                        ? 'bg-cyan-500/15 border border-cyan-500/30 text-cyan-400' 
                        : 'bg-white/[0.03] border border-white/5 text-gray-400 hover:bg-white/[0.06]'">
                <div class="text-xl mb-1">{{ opt.icon }}</div>
                <div class="text-[11px] font-bold">{{ opt.label }}</div>
              </button>
            </div>
          </div>

          <!-- 操作按钮 -->
          <button @click="generateMasterOutline" :disabled="generatingOutline || !stylePreference || isStepLocked('conceive')"
                  class="w-full py-3.5 rounded-xl font-bold text-sm cursor-pointer transition-all flex items-center justify-center gap-2 disabled:opacity-30 disabled:cursor-not-allowed bg-gradient-to-r from-blue-500 to-cyan-400 text-white hover:shadow-lg hover:shadow-blue-500/20 active:scale-[0.98]">
            <i v-if="generatingOutline" class="fas fa-circle-notch fa-spin"></i>
            <i v-else class="fas fa-wand-magic-sparkles"></i>
            {{ generatingOutline ? 'AI 正在构思整体大纲...' : '✨ AI 生成整体大纲' }}
          </button>
        </div>

        <!-- ==================== Step 2: 整体大纲 ==================== -->
        <div v-show="novelStep === 'outline'" class="space-y-4">
          <div v-if="isStepLocked('outline')" class="bg-green-500/5 border border-green-500/20 rounded-xl px-3 py-2 flex items-center gap-2 text-[10px] text-green-400">
            <i class="fas fa-lock"></i> 已锁定·查看模式
          </div>
          
          <!-- 故事梗概 -->
          <div class="bg-gradient-to-br from-[#1a1a1c] to-[#14141a] border border-white/5 rounded-2xl p-4 relative overflow-hidden">
            <div class="absolute top-0 left-0 right-0 h-0.5 bg-gradient-to-r from-amber-500/50 via-orange-500/30 to-transparent"></div>
            <div class="flex items-center justify-between mb-3">
              <h3 class="text-xs font-bold text-white flex items-center gap-1.5">
                <span class="w-5 h-5 rounded-lg bg-amber-500/15 flex items-center justify-center text-[10px]">📜</span>
                故事梗概
              </h3>
              <span class="text-[9px] px-2 py-0.5 rounded-full" :class="masterOutline.synopsis?.length > 50 ? 'bg-green-500/10 text-green-400' : 'bg-white/5 text-gray-600'">{{ masterOutline.synopsis?.length || 0 }} 字</span>
            </div>
            <textarea v-model="masterOutline.synopsis" rows="5"
                      class="w-full bg-white/[0.02] border border-white/5 rounded-xl px-3 py-2.5 text-[13px] text-gray-200 outline-none focus:border-amber-500/30 focus:bg-white/[0.03] resize-none transition-all leading-relaxed appearance-none hide-scrollbar"
                      style="background-color: rgba(255,255,255,0.02) !important; overflow-y: auto;"
                      :disabled="isStepLocked('outline')"
                      placeholder="整体故事走向..."></textarea>
          </div>

          <!-- 核心角色 -->
          <div class="bg-gradient-to-br from-[#1a1a1c] to-[#14141a] border border-white/5 rounded-2xl p-4 relative overflow-hidden">
            <div class="absolute top-0 left-0 right-0 h-0.5 bg-gradient-to-r from-pink-500/50 via-purple-500/30 to-transparent"></div>
            <h3 class="text-xs font-bold text-white mb-3 flex items-center gap-1.5">
              <span class="w-5 h-5 rounded-lg bg-pink-500/15 flex items-center justify-center text-[10px]">👥</span>
              核心角色
              <span class="text-[9px] text-gray-500 ml-auto">{{ masterOutline.characters?.length || 0 }} 人</span>
            </h3>
            <div class="space-y-2">
              <div v-for="(char, i) in masterOutline.characters" :key="i" 
                   class="flex gap-2.5 items-center bg-white/[0.02] border border-white/5 rounded-xl px-3 py-2.5 group hover:border-white/10 transition-all">
                <!-- 头像 -->
                <div class="w-8 h-8 rounded-full flex items-center justify-center text-xs font-black shrink-0"
                     :class="['bg-pink-500/15 text-pink-400', 'bg-blue-500/15 text-blue-400', 'bg-green-500/15 text-green-400', 'bg-amber-500/15 text-amber-400', 'bg-purple-500/15 text-purple-400'][i % 5]">
                  {{ char.name ? char.name[0] : '?' }}
                </div>
                <div class="flex-1 min-w-0">
                  <input v-model="char.name" placeholder="角色名" 
                         :disabled="isStepLocked('outline')"
                         class="w-full bg-transparent text-[12px] text-white font-bold outline-none placeholder:text-gray-600 appearance-none mb-0.5"
                         style="background: transparent !important;" />
                  <input v-model="char.desc" placeholder="性格/身份描述"
                         :disabled="isStepLocked('outline')"
                         class="w-full bg-transparent text-[10px] text-gray-400 outline-none placeholder:text-gray-700 appearance-none"
                         style="background: transparent !important;" />
                </div>
                <button v-if="!isStepLocked('outline')" @click="masterOutline.characters.splice(i, 1)" 
                        class="w-6 h-6 rounded-lg bg-red-500/0 text-red-400/0 text-[9px] flex items-center justify-center cursor-pointer group-hover:bg-red-500/10 group-hover:text-red-400 transition-all shrink-0">
                  <i class="fas fa-trash-alt"></i>
                </button>
              </div>
            </div>
            <button v-if="!isStepLocked('outline')" @click="masterOutline.characters.push({ name: '', desc: '' })"
                    class="w-full py-2.5 rounded-xl bg-white/[0.02] border border-dashed border-white/10 text-gray-500 text-[10px] font-bold cursor-pointer hover:bg-white/[0.05] hover:text-gray-300 hover:border-white/20 transition-all mt-2.5 flex items-center justify-center gap-1.5">
              <i class="fas fa-plus text-[8px]"></i> 添加角色
            </button>
          </div>

          <div class="bg-gradient-to-br from-[#1a1a1c] to-[#14141a] border border-white/5 rounded-2xl p-4 relative overflow-hidden">
            <div class="absolute top-0 left-0 right-0 h-0.5 bg-gradient-to-r from-cyan-500/50 via-blue-500/30 to-transparent"></div>
            <h3 class="text-xs font-bold text-white mb-3 flex items-center gap-1.5">
              <span class="w-5 h-5 rounded-lg bg-cyan-500/15 flex items-center justify-center text-[10px]">📋</span>
              章节大纲
              <span class="text-[9px] px-2 py-0.5 rounded-full bg-cyan-500/10 text-cyan-400 ml-auto">共 {{ chapterOutlines.length }} 章</span>
            </h3>
            <div class="space-y-2 max-h-[50vh] overflow-y-auto hide-scrollbar">
              <div v-for="(co, i) in chapterOutlines" :key="i"
                   class="bg-white/[0.02] border rounded-xl overflow-hidden transition-all"
                   :class="co.plot ? 'border-white/5' : 'border-white/[0.03] opacity-50'">
                <!-- 卡片头 -->
                <div class="flex items-center gap-2.5 px-3.5 py-2.5 cursor-pointer select-none"
                     @click="expandedOutline = expandedOutline === i ? -1 : i">
                  <div class="w-6 h-6 rounded-full flex items-center justify-center text-[10px] font-black shrink-0"
                       :class="co.plot ? 'bg-green-500/15 text-green-400' : 'bg-white/5 text-gray-600'">
                    <i v-if="co.plot" class="fas fa-check text-[8px]"></i>
                    <span v-else>{{ i + 1 }}</span>
                  </div>
                  <div class="flex-1 min-w-0">
                    <p class="text-[11px] font-bold text-white">第 {{ i + 1 }} 章</p>
                    <p v-if="co.plot && expandedOutline !== i" class="text-[9px] text-gray-500 truncate">{{ co.plot }}</p>
                  </div>
                  <i class="fas text-gray-600 text-[9px]" :class="expandedOutline === i ? 'fa-chevron-up' : 'fa-chevron-down'"></i>
                </div>
                <!-- 展开编辑 -->
                <div v-if="expandedOutline === i" class="px-3.5 pb-3 space-y-1.5">
                  <div>
                    <label class="text-[8px] text-gray-600 uppercase tracking-wider">📖 剧情</label>
                    <input v-model="co.plot" :disabled="isStepLocked('outline')"
                           class="w-full bg-black/20 border-b border-white/5 px-0 py-1.5 text-[11px] text-white outline-none focus:border-blue-500/50 transition-all" />
                  </div>
                  <div>
                    <label class="text-[8px] text-gray-600 uppercase tracking-wider">⚡ 事件</label>
                    <input v-model="co.keyEvents" :disabled="isStepLocked('outline')"
                           class="w-full bg-black/20 border-b border-white/5 px-0 py-1.5 text-[11px] text-white outline-none focus:border-blue-500/50 transition-all" />
                  </div>
                  <div class="grid grid-cols-2 gap-2">
                    <div>
                      <label class="text-[8px] text-gray-600 uppercase tracking-wider">👥 人物</label>
                      <input v-model="co.characters" :disabled="isStepLocked('outline')"
                             class="w-full bg-black/20 border-b border-white/5 px-0 py-1 text-[10px] text-white outline-none focus:border-blue-500/50" />
                    </div>
                    <div>
                      <label class="text-[8px] text-gray-600 uppercase tracking-wider">🔮 伏笔</label>
                      <input v-model="co.foreshadowing" :disabled="isStepLocked('outline')"
                             class="w-full bg-black/20 border-b border-white/5 px-0 py-1 text-[10px] text-white outline-none focus:border-blue-500/50" />
                    </div>
                  </div>
                </div>
                <!-- 未展开：显示标签 -->
                <div v-else-if="co.plot" class="px-3.5 pb-2.5">
                  <div class="flex flex-wrap gap-x-3 gap-y-0.5">
                    <span v-if="co.keyEvents" class="text-[9px] text-gray-500">⚡ {{ co.keyEvents }}</span>
                    <span v-if="co.foreshadowing" class="text-[9px] text-purple-400/60">🔮 {{ co.foreshadowing }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div v-if="!isStepLocked('outline')">
            <button @click="confirmOutlineAndNext" :disabled="!masterOutline.synopsis"
                    class="w-full py-3 rounded-xl text-xs font-bold cursor-pointer transition-all flex items-center justify-center gap-1.5 disabled:opacity-30 bg-gradient-to-r from-blue-500 to-cyan-400 text-white hover:shadow-lg active:scale-[0.98]">
              确认大纲 → 开始创作 <i class="fas fa-arrow-right text-[10px]"></i>
            </button>
          </div>
        </div>

        <!-- Step 3 (细纲) 已合并到 Step 2 -->

        <!-- ==================== Step 4: 生成正文 ==================== -->
        <div v-show="novelStep === 'generate'" class="space-y-4">
          
          <!-- 章节选择 + 状态 -->
          <div class="flex gap-2 overflow-x-auto py-1 hide-scrollbar snap-x">
            <button v-for="(co, i) in chapterOutlines" :key="i"
                    @click="novelActiveChapter = i; streamContent = ''; justFinished = false"
                    class="flex-shrink-0 snap-start px-3 py-1.5 rounded-full text-[11px] font-bold cursor-pointer transition-all flex items-center gap-1"
                    :class="novelActiveChapter === i 
                      ? 'bg-[#FF9500]/20 text-[#FF9500] border border-[#FF9500]/30' 
                      : 'bg-white/5 text-gray-400 hover:bg-white/10'">
              第{{ i + 1 }}{{ activeUnitName }}
              <i v-if="getValidSection(i)" class="fas fa-check-circle text-green-400 text-[8px]"></i>
            </button>
          </div>

          <!-- 当前章细纲预览 -->
          <div v-if="chapterOutlines[novelActiveChapter]" class="bg-[#1a1a1c] border border-white/5 rounded-2xl p-4">
            <h3 class="text-xs font-bold text-gray-400 mb-2 flex items-center gap-1.5">
              <i class="fas fa-file-lines text-blue-400"></i> 第 {{ novelActiveChapter + 1 }} {{ activeUnitName }}细纲
            </h3>
            <p class="text-[11px] text-gray-300 leading-relaxed">{{ chapterOutlines[novelActiveChapter].plot }}</p>
            <p v-if="chapterOutlines[novelActiveChapter].keyEvents" class="text-[10px] text-gray-500 mt-1">⚡ {{ chapterOutlines[novelActiveChapter].keyEvents }}</p>
          </div>

          <!-- 正文生成区 -->
          <div class="bg-[#1a1a1c] border border-white/5 rounded-2xl p-4">
            <!-- 已生成 → 锁定状态 -->
            <template v-if="getValidSection(novelActiveChapter) && !generating">
              <div class="flex items-center gap-2 mb-3">
                <i class="fas fa-lock text-green-400 text-[10px]"></i>
                <h3 class="text-xs font-bold text-green-400">✅ 第 {{ novelActiveChapter + 1 }} {{ activeUnitName }}已生成</h3>
                <span class="text-[9px] px-2 py-0.5 rounded-full bg-green-500/10 text-green-400 ml-auto">{{ getValidSection(novelActiveChapter)?.content?.length || 0 }} 字</span>
              </div>
              <!-- 阅读/收起切换 -->
              <div class="flex gap-2 mb-2">
                <button @click="chapterReaderOpen = chapterReaderOpen === novelActiveChapter ? -1 : novelActiveChapter"
                        class="flex-1 py-2 rounded-lg text-[10px] font-bold cursor-pointer transition-all flex items-center justify-center gap-1"
                        :class="chapterReaderOpen === novelActiveChapter ? 'bg-blue-500/15 text-blue-400' : 'bg-white/5 text-gray-400 hover:bg-white/10'">
                  <i class="fas" :class="chapterReaderOpen === novelActiveChapter ? 'fa-eye-slash' : 'fa-book-reader'"></i>
                  {{ chapterReaderOpen === novelActiveChapter ? '收起' : '📖 展开阅读' }}
                </button>
              </div>
              <!-- 全文阅读区 -->
              <div v-if="chapterReaderOpen === novelActiveChapter" class="bg-white/[0.02] border border-white/5 rounded-xl p-3 max-h-[50vh] overflow-y-auto hide-scrollbar mb-2">
                <p class="text-[13px] text-gray-200 leading-[1.8] whitespace-pre-wrap">{{ getValidSection(novelActiveChapter)?.content }}</p>
              </div>
              <!-- 预览（未展开时） -->
              <p v-else class="text-[10px] text-gray-500 leading-relaxed line-clamp-3">{{ getValidSection(novelActiveChapter)?.content?.substring(0, 150) }}...</p>
              <div class="mt-2 px-3 py-2 rounded-xl bg-green-500/5 border border-green-500/10 text-[9px] text-green-400/70 flex items-center gap-1.5">
                <i class="fas fa-info-circle"></i> 该章节已锁定，不可重新生成
              </div>
            </template>
            <!-- 未生成 → 生成区 -->
            <template v-else>
              <h3 class="text-xs font-bold mb-2 flex items-center gap-1.5">
                <i class="fas fa-pen-nib text-[#FF9500]"></i> 
                {{ generating ? '正在创作...' : '准备生成正文' }}
              </h3>
              <textarea v-model="userInput" rows="2" :disabled="generating"
                        :placeholder="'可补充对第' + (novelActiveChapter+1) + activeUnitName + '的额外要求...（留空则纯按细纲生成）'"
                        class="w-full bg-transparent border-b border-white/10 px-1 py-2 text-sm text-gray-200 outline-none focus:border-[#FF9500]/50 resize-none mb-2 disabled:opacity-50 appearance-none hide-scrollbar"
                        style="background: transparent !important;"></textarea>
              <button @click="generateFromOutline" :disabled="generating"
                      class="w-full py-3 rounded-xl bg-gradient-to-r from-[#FF9500] to-[#FF6B00] text-black font-bold text-xs cursor-pointer disabled:opacity-50 transition-all flex items-center justify-center gap-1.5 active:scale-[0.98]">
                <i v-if="generating" class="fas fa-circle-notch fa-spin"></i>
                <i v-else class="fas fa-sparkles"></i>
                {{ generating ? genPhaseText : '✍️ 根据细纲生成第 ' + (novelActiveChapter + 1) + ' ' + activeUnitName + '正文' }}
              </button>
              <button v-if="!generating && chapterOutlines.filter((co, i) => co.plot && !getValidSection(i)).length > 1"
                      @click="batchGenerateChapters" :disabled="generating"
                      class="w-full py-2.5 rounded-xl bg-white/5 border border-white/10 text-gray-300 font-bold text-xs cursor-pointer hover:bg-white/10 transition-all flex items-center justify-center gap-1.5 active:scale-[0.98] mt-2">
                <i class="fas fa-layer-group"></i>
                📚 连续生成剩余 {{ chapterOutlines.filter((co, i) => co.plot && !getValidSection(i)).length }} {{ activeUnitName }}
              </button>
            </template>
          </div>

          <!-- 流式输出 -->
          <div v-if="generating || streamContent" ref="streamOutputRef" class="bg-[#1a1a1c] border border-white/5 rounded-2xl p-4">
            <h3 class="text-[10px] font-bold text-gray-400 mb-2 flex items-center gap-1.5">
              <i class="fas fa-robot text-[#FF9500]"></i>
              {{ generating ? genPhaseText : '✅ 创作完成' }}
              <span v-if="generating" class="animate-pulse text-[#FF9500]">▊</span>
            </h3>
            <div v-if="generating" class="flex items-center gap-3 mb-3">
              <span class="text-[10px] text-gray-500">⏱️ {{ elapsedTime }}s</span>
              <span class="text-[10px] text-[#FF9500] font-bold">{{ streamContent.length }} 字</span>
              <div class="flex-1 h-1.5 bg-white/5 rounded-full overflow-hidden">
                <div class="h-full bg-gradient-to-r from-[#FF9500] to-[#FF6B00] rounded-full transition-all duration-300" :style="{ width: genProgress + '%' }"></div>
              </div>
            </div>
            <div v-if="!streamContent && generating" class="space-y-2 animate-pulse">
              <div class="h-3 bg-white/5 rounded w-4/5"></div><div class="h-3 bg-white/5 rounded w-3/5"></div>
            </div>
            <div v-else class="text-sm text-gray-200 whitespace-pre-wrap leading-relaxed max-h-[50vh] overflow-y-auto hide-scrollbar" ref="streamContentRef">{{ streamContent }}</div>
            <div v-if="justFinished" class="flex gap-2 mt-3">
              <button @click="novelActiveChapter < chapterOutlines.length - 1 ? (novelActiveChapter++, justFinished = false, streamContent = '') : null"
                      class="flex-1 py-2 rounded-lg bg-[#FF9500]/10 text-[#FF9500] text-xs font-bold cursor-pointer hover:bg-[#FF9500]/20 transition-all"
                      v-if="novelActiveChapter < chapterOutlines.length - 1">
                ✍️ 写下一章
              </button>
            </div>
          </div>

          <!-- 已生成章节概览 -->
          <div v-if="sections.length > 0 && !generating && !streamContent" class="bg-[#1a1a1c] border border-white/5 rounded-2xl p-4">
            <h3 class="text-xs font-bold text-gray-400 mb-2">📊 创作进度 · {{ chapterOutlines.filter((co, i) => getValidSection(i)).length }}/{{ chapterOutlines.length }} {{ activeUnitName }}</h3>
            <div class="space-y-1.5">
              <div v-for="(co, i) in chapterOutlines" :key="i" class="flex items-center gap-2 text-[11px]">
                <span class="w-12 truncate text-gray-400">第{{ i+1 }}{{ activeUnitName }}</span>
                <div class="flex-1 h-1.5 bg-white/5 rounded-full overflow-hidden">
                  <div class="h-full rounded-full transition-all"
                       :class="getValidSection(i) ? 'bg-green-500 w-full' : 'bg-gray-600 w-0'"></div>
                </div>
                <span class="text-[9px]" :class="getValidSection(i) ? 'text-green-400' : 'text-gray-600'">
                  {{ getValidSection(i) ? '✅' : '⬜' }}
                </span>
              </div>
            </div>
          </div>

          <!-- 🎉 全部章节完成 — 完整体验 -->
          <div v-if="chapterOutlines.every((co, i) => getValidSection(i)) && !generating && !streamContent"
               class="space-y-4">
            <!-- 庆祝卡片 -->
            <div class="bg-gradient-to-br from-green-500/10 via-emerald-500/5 to-teal-500/10 border border-green-500/20 rounded-2xl p-6 text-center relative overflow-hidden">
              <div class="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-green-500/60 via-emerald-400/40 to-teal-500/60"></div>
              <div class="text-4xl mb-3 animate-bounce">🎉</div>
              <h3 class="text-base font-black text-white mb-1">全部 {{ chapterOutlines.length }} {{ activeUnitName }}创作完成！</h3>
              <p class="text-[11px] text-gray-400 mb-4">{{ isDramaType ? '你的AI短剧已全部创作完毕' : '你的AI小说已全部创作完毕' }}</p>
              
              <!-- 创作统计 -->
              <div class="flex justify-center gap-6 mb-4">
                <div class="text-center">
                  <div class="text-lg font-black text-green-400">{{ sections.reduce((sum, s) => sum + (s.content?.length || 0), 0).toLocaleString() }}</div>
                  <div class="text-[9px] text-gray-500">总字数</div>
                </div>
                <div class="text-center">
                  <div class="text-lg font-black text-cyan-400">{{ chapterOutlines.length }}</div>
                  <div class="text-[9px] text-gray-500">章节数</div>
                </div>
                <div class="text-center">
                  <div class="text-lg font-black text-amber-400">~{{ Math.ceil(sections.reduce((sum, s) => sum + (s.content?.length || 0), 0) / 500) }}</div>
                  <div class="text-[9px] text-gray-500">阅读分钟</div>
                </div>
              </div>
            </div>
            
            <!-- 操作按钮组 -->
            <div class="grid grid-cols-2 gap-2">
              <button @click="showFullReader = !showFullReader"
                      class="py-3 rounded-xl text-xs font-bold cursor-pointer transition-all flex items-center justify-center gap-1.5 active:scale-[0.98]"
                      :class="showFullReader ? 'bg-blue-500/20 border border-blue-500/30 text-blue-400' : 'bg-white/5 border border-white/10 text-gray-300 hover:bg-white/10'">
                <i class="fas" :class="showFullReader ? 'fa-eye-slash' : 'fa-book-open'"></i>
                {{ showFullReader ? '收起全文' : '📖 阅读全文' }}
              </button>
              <button @click="copyFullNovel"
                      class="py-3 rounded-xl bg-white/5 border border-white/10 text-gray-300 text-xs font-bold cursor-pointer hover:bg-white/10 transition-all flex items-center justify-center gap-1.5 active:scale-[0.98]">
                <i class="fas fa-copy"></i> 📋 复制全文
              </button>
            </div>
            
            <!-- 全文阅读器（全屏沉浸式） -->
            <Teleport to="body">
              <div v-if="showFullReader" class="fixed inset-0 z-[9999] bg-[#0a0a0f] flex flex-col">
                <!-- 顶栏 -->
                <div class="shrink-0 px-4 py-3 border-b border-white/5 bg-[#0a0a0f]/95 backdrop-blur flex items-center justify-between">
                  <div class="flex items-center gap-2 min-w-0">
                    <i class="fas fa-book text-amber-400 text-sm"></i>
                    <h3 class="text-sm font-bold text-white truncate">{{ project?.title || '我的小说' }}</h3>
                    <span class="text-[9px] px-2 py-0.5 rounded-full bg-white/5 text-gray-500 shrink-0">全 {{ chapterOutlines.length }} 章</span>
                  </div>
                  <button @click="showFullReader = false" class="w-8 h-8 rounded-full bg-white/5 flex items-center justify-center text-gray-400 hover:bg-white/10 hover:text-white transition-all cursor-pointer shrink-0">
                    <i class="fas fa-times text-sm"></i>
                  </button>
                </div>
                <!-- 正文阅读区 -->
                <div class="flex-1 overflow-y-auto hide-scrollbar">
                  <div class="max-w-2xl mx-auto px-5 py-8 space-y-10">
                    <div v-for="(co, i) in chapterOutlines" :key="'read-'+i">
                      <!-- 章节标题 -->
                      <div class="text-center py-6">
                        <div class="text-[11px] text-gray-600 mb-1">— 第 {{ i + 1 }} 章 —</div>
                        <h2 class="text-lg font-black text-white/90">{{ getValidSection(i)?.title || co.plot?.substring(0, 15) || '第 ' + (i+1) + ' 章' }}</h2>
                        <div class="mt-2 flex items-center justify-center gap-4">
                          <span class="text-[9px] text-gray-600">{{ getValidSection(i)?.content?.length || 0 }} 字</span>
                          <span class="w-12 h-px bg-white/10"></span>
                        </div>
                      </div>
                      <!-- 正文 -->
                      <div class="novel-reader-content text-[15px] text-gray-200/90 leading-[2] whitespace-pre-wrap" style="text-indent: 2em; word-break: break-all;">{{ getValidSection(i)?.content || '（本章未生成）' }}</div>
                      <!-- 章节分隔 -->
                      <div v-if="i < chapterOutlines.length - 1" class="flex items-center justify-center gap-3 pt-6">
                        <span class="w-16 h-px bg-white/5"></span>
                        <span class="text-[10px] text-gray-700">✦</span>
                        <span class="w-16 h-px bg-white/5"></span>
                      </div>
                    </div>
                    <!-- 全文完 -->
                    <div class="text-center py-10">
                      <div class="text-2xl mb-2">📖</div>
                      <p class="text-xs text-gray-600">— 全文完 —</p>
                      <p class="text-[10px] text-gray-700 mt-1">共 {{ sections.reduce((sum, s) => sum + (s.content?.length || 0), 0).toLocaleString() }} 字</p>
                    </div>
                  </div>
                </div>
              </div>
            </Teleport>
            
            
            <!-- 完结发布 -->
            <button v-if="project?.status !== 'completed'"
                    @click="publishNovel"
                    class="w-full py-3.5 rounded-xl bg-gradient-to-r from-amber-500 to-orange-500 text-black text-sm font-black cursor-pointer hover:shadow-lg hover:shadow-amber-500/20 active:scale-[0.98] transition-all flex items-center justify-center gap-2">
              <i class="fas fa-trophy"></i> 🏆 完结发布
            </button>
            <div v-else class="flex items-center justify-center gap-2 py-3 rounded-xl bg-green-500/10 border border-green-500/20 text-green-400 text-xs font-bold">
              <i class="fas fa-check-circle"></i> 已完结发布
            </div>

            <!-- 返回项目 -->
            <button @click="$router.push('/studio')"
                    class="w-full py-3 rounded-xl bg-white/[0.03] border border-white/5 text-gray-500 text-[11px] cursor-pointer hover:bg-white/[0.06] hover:text-gray-300 transition-all flex items-center justify-center gap-1.5">
              <i class="fas fa-arrow-left text-[9px]"></i> 返回创作中心
            </button>
          </div>
        </div>

      </template>

      <!-- ═══════ 非结构化的多章节模式（保持原有逻辑） ═══════ -->
      <template v-if="!isStructuredMode">

      <!-- 段落横向选择条 -->
      <div v-if="sections.length > 0" class="flex gap-2 overflow-x-auto py-3 hide-scrollbar snap-x">
        <button v-for="(s, i) in sections" :key="s.id"
                @click="activeSection = s; activeTab = 'content'"
                class="flex-shrink-0 snap-start px-3 py-1.5 rounded-full text-[11px] font-bold cursor-pointer transition-all flex items-center gap-1"
                :class="activeSection?.id === s.id ? 'bg-[#FF9500]/20 text-[#FF9500] border border-[#FF9500]/30' : 'bg-white/5 text-gray-400 hover:bg-white/10'">
          {{ s.title || `第 ${i + 1} ${mc.unit}` }}
          <i v-if="s.audioUrl" class="fas fa-volume-up text-green-400 text-[8px]"></i>
        </button>
      </div>

      <!-- Tab 切换栏 -->
      <div class="flex border-b border-white/5 mb-4">
        <button v-for="tab in tabs" :key="tab.key"
                @click="activeTab = tab.key"
                class="flex-1 py-2.5 text-xs font-bold text-center cursor-pointer transition-all relative"
                :class="activeTab === tab.key ? 'text-[#FF9500]' : 'text-gray-500 hover:text-gray-300'">
          <i :class="tab.icon" class="mr-1"></i>{{ tab.label }}
          <div v-if="activeTab === tab.key" class="absolute bottom-0 left-1/4 right-1/4 h-0.5 bg-[#FF9500] rounded-full"></div>
        </button>
      </div>

      <!-- ==================== Tab: 创作 ==================== -->
      <div v-show="activeTab === 'create'">

        <!-- 已有内容概览 -->
        <div v-if="sections.length > 0" class="bg-[#1a1a1c] border border-white/5 rounded-2xl p-3 mb-3 flex items-center gap-3">
          <div class="w-9 h-9 rounded-xl bg-green-500/10 flex items-center justify-center text-green-400 shrink-0">
            <i class="fas fa-book-open"></i>
          </div>
          <div class="flex-1 min-w-0">
            <p class="text-xs font-bold text-white">已有 {{ sections.length }} {{ mc.unit }}</p>
            <p class="text-[10px] text-gray-500">共 {{ sections.reduce((sum, s) => sum + (s.content?.length || 0), 0) }} 字 · {{ sections.filter(s => s.audioUrl).length }} {{ mc.unit }}已配音</p>
          </div>
          <button @click="activeTab = 'content'" class="text-[10px] text-[#FF9500] font-bold cursor-pointer whitespace-nowrap">查看内容 →</button>
        </div>

        <!-- AI 生成 -->
        <div class="bg-[#1a1a1c] border border-white/5 rounded-2xl p-4 mb-4">
          <h3 class="text-xs font-bold mb-2 flex items-center gap-1.5">
            {{ sections.length === 0 ? mc.createLabel : mc.continueLabel.replace('{n}', sections.length + 1) }}
          </h3>
          <!-- 章节标题输入（续写时显示） -->
          <div v-if="sections.length > 0" class="mb-2">
            <label class="text-[10px] text-gray-500 mb-1 block">📝 章节标题（留空则 AI 自动生成）</label>
            <input v-model="chapterTitle" :disabled="generating" type="text"
                   :placeholder="`第 ${sections.length + 1} ${mc.unit}：输入标题...`"
                   class="w-full bg-black/30 border border-white/5 rounded-xl px-3 py-2 text-sm text-white outline-none focus:border-[#FF9500]/50 disabled:opacity-50" />
          </div>
          <textarea v-model="userInput" rows="2" :disabled="generating"
                    :placeholder="sections.length === 0 ? (project?.inspiration || '输入灵感关键词...') : (mc.placeholder + '（留空则 AI 自动续写）')"
                    class="w-full bg-black/30 border border-white/5 rounded-xl px-3 py-2 text-sm text-white outline-none focus:border-[#FF9500]/50 resize-none mb-2 disabled:opacity-50"></textarea>
          <button @click="generate" :disabled="generating"
                  class="w-full py-2.5 rounded-xl bg-gradient-to-r from-[#FF9500] to-[#FF6B00] text-black font-bold text-xs cursor-pointer disabled:opacity-50 transition-all flex items-center justify-center gap-1.5">
            <i v-if="generating" class="fas fa-circle-notch fa-spin"></i>
            <i v-else class="fas fa-sparkles"></i>
            {{ generating ? genPhaseText : (sections.length === 0 ? mc.createLabel : mc.continueLabel.replace('{n}', sections.length + 1)) }}
          </button>
        </div>

        <!-- 流式输出 -->
        <div v-if="generating || streamContent" ref="streamOutputRef" class="bg-[#1a1a1c] border border-white/5 rounded-2xl p-4">
          <h3 class="text-[10px] font-bold text-gray-400 mb-2 flex items-center gap-1.5">
            <i class="fas fa-robot text-[#FF9500]"></i>
            {{ generating ? genPhaseText : '✅ 创作完成' }}
            <span v-if="generating" class="animate-pulse text-[#FF9500]">▊</span>
          </h3>
          <!-- 实时统计面板 -->
          <div v-if="generating" class="flex items-center gap-3 mb-3">
            <span class="text-[10px] text-gray-500">⏱️ {{ elapsedTime }}s</span>
            <span class="text-[10px] text-[#FF9500] font-bold">{{ streamContent.length }} / {{ mc.expectedChars || 1500 }} 字</span>
            <div class="flex-1 h-1.5 bg-white/5 rounded-full overflow-hidden">
              <div class="h-full bg-gradient-to-r from-[#FF9500] to-[#FF6B00] rounded-full transition-all duration-300"
                   :style="{ width: genProgress + '%' }"></div>
            </div>
          </div>
          <!-- 骨架屏 -->
          <div v-if="!streamContent && generating" class="space-y-2 animate-pulse">
            <div class="h-3 bg-white/5 rounded w-4/5"></div>
            <div class="h-3 bg-white/5 rounded w-3/5"></div>
            <div class="h-3 bg-white/5 rounded w-2/3"></div>
          </div>
          <!-- 流式内容 -->
          <div v-else class="text-sm text-gray-200 whitespace-pre-wrap leading-relaxed max-h-[50vh] overflow-y-auto hide-scrollbar"
               ref="streamContentRef">{{ cleanStreamContent }}</div>
          <!-- 完成后快捷操作 -->
          <div v-if="justFinished" class="flex gap-2 mt-3">
            <button @click="justFinished = false; activeTab = 'create'; streamContent = ''"
                    class="flex-1 py-2 rounded-lg bg-[#FF9500]/10 text-[#FF9500] text-xs font-bold cursor-pointer hover:bg-[#FF9500]/20 transition-all">
              {{ mc.finishBtn }}
            </button>
            <button @click="justFinished = false; activeTab = 'voice'; streamContent = ''"
                    class="flex-1 py-2 rounded-lg bg-blue-500/10 text-blue-400 text-xs font-bold cursor-pointer hover:bg-blue-500/20 transition-all">
              🎧 去配音
            </button>
          </div>
        </div>
      </div>

      <!-- ==================== Tab: 内容（章节卡片流） ==================== -->
      <div v-show="activeTab === 'content'">
        <div v-if="sections.length === 0" class="text-center py-16 text-gray-600">
          <i class="fas fa-file-alt text-3xl mb-3"></i>
          <p class="text-sm mb-4">还没有内容</p>
          <button @click="activeTab = 'create'" class="px-4 py-2 rounded-xl bg-[#FF9500]/10 text-[#FF9500] text-xs font-bold cursor-pointer hover:bg-[#FF9500]/20 transition-all">
            <i class="fas fa-sparkles mr-1"></i> 去{{ mc.createLabel }}
          </button>
        </div>
        <div v-else class="space-y-3">
          <!-- 章节卡片 -->
          <div v-for="(s, i) in sections" :key="s.id"
               class="bg-[#1a1a1c] border rounded-2xl transition-all overflow-hidden"
               :class="expandedSections.has(s.id) ? 'border-[#FF9500]/20' : 'border-white/5'">
            <!-- 卡片头 -->
            <div class="flex items-center gap-3 px-4 py-3 cursor-pointer select-none"
                 @click="toggleSection(s)">
              <div class="w-7 h-7 rounded-lg flex items-center justify-center text-[11px] font-black shrink-0"
                   :class="s.audioUrl ? 'bg-green-500/15 text-green-400' : 'bg-white/5 text-gray-500'">
                {{ i + 1 }}
              </div>
              <div class="flex-1 min-w-0">
                <p class="text-sm font-bold text-white truncate">{{ s.title || `第 ${i + 1} 章` }}</p>
                <div class="flex items-center gap-2 text-[10px] text-gray-500">
                  <span>{{ (s.content || '').length }} 字</span>
                  <span v-if="s.audioUrl" class="text-green-400 flex items-center gap-0.5"><i class="fas fa-volume-up text-[8px]"></i> 已配音</span>
                </div>
              </div>
              <i class="fas text-gray-500 text-[10px] transition-transform duration-200"
                 :class="expandedSections.has(s.id) ? 'fa-chevron-up' : 'fa-chevron-down'"></i>
            </div>

            <!-- 卡片体（展开时） -->
            <div v-if="expandedSections.has(s.id)">
              <div class="px-4 pb-3">
                <div class="text-sm text-gray-300 whitespace-pre-wrap leading-relaxed max-h-[40vh] overflow-y-auto hide-scrollbar border-t border-white/5 pt-3">{{ s.content }}</div>
              </div>
              <!-- 操作栏 -->
              <div class="flex items-center gap-2 px-4 py-2.5 bg-black/20 border-t border-white/5">
                <!-- 已配音锁定标识 -->
                <div v-if="s.audioUrl" class="flex-1 flex items-center gap-1.5 text-[11px]">
                  <span class="flex items-center gap-1 px-2 py-1 rounded-lg bg-green-500/10 text-green-400 font-bold">
                    <i class="fas fa-lock text-[8px]"></i> 已配音
                  </span>
                  <button @click.stop="activeSection = s; activeTab = 'voice'; playAudio(s)"
                          class="px-2 py-1 rounded-lg bg-blue-500/10 text-blue-400 font-bold hover:bg-blue-500/20 cursor-pointer transition-all">
                    <i class="fas fa-play text-[8px]"></i> 试听
                  </button>
                </div>
                <!-- 待配音 — 快捷配音入口 -->
                <div v-else class="flex-1 flex items-center gap-1.5 text-[11px]">
                  <span class="flex items-center gap-1 px-2 py-1 rounded-lg bg-gray-500/10 text-gray-400 font-bold">
                    <i class="fas fa-microphone-slash text-[8px]"></i> 待配音
                  </span>
                  <button @click.stop="activeSection = s; activeTab = 'voice'"
                          class="px-2 py-1 rounded-lg bg-purple-500/10 text-purple-400 font-bold hover:bg-purple-500/20 cursor-pointer transition-all">
                    <i class="fas fa-microphone text-[8px]"></i> 去配音
                  </button>
                </div>
                <button @click.stop="showSectionDeleteModal(s)" :disabled="generating"
                        class="w-10 py-2 rounded-lg text-[11px] font-bold cursor-pointer transition-all disabled:opacity-30 flex items-center justify-center"
                        :class="s.audioUrl ? 'bg-red-500/5 text-red-300 hover:bg-red-500/15' : 'bg-red-500/10 text-red-400 hover:bg-red-500/20'">
                  <i class="fas fa-trash-alt"></i>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 段落删除确认弹窗 -->
      <Transition name="fade">
        <div v-if="showDeleteModal" class="fixed inset-0 z-[70] flex items-center justify-center p-6" @click.self="showDeleteModal = false">
          <div class="absolute inset-0 bg-black/70 backdrop-blur-sm" @click="showDeleteModal = false"></div>
          <div class="relative bg-[#1a1a1c] border border-white/10 rounded-2xl p-6 w-full max-w-sm shadow-2xl">
            <div class="text-center mb-5">
              <div class="w-14 h-14 mx-auto rounded-full bg-red-500/10 flex items-center justify-center mb-3">
                <i class="fas fa-trash-alt text-red-400 text-xl"></i>
              </div>
              <h3 class="text-base font-bold text-white mb-1.5">删除章节</h3>
              <p class="text-xs text-gray-400">确定删除「<span class="text-white font-bold">{{ deletingSection?.title }}</span>」？<br/>删除后<span class="text-red-400">不可恢复</span>。</p>
              <p v-if="deletingSection?.audioUrl" class="text-[10px] text-amber-400 mt-1.5">
                ⚠️ 该段落已配音，删除后配音也将丢失
              </p>
            </div>
            <div class="grid grid-cols-2 gap-3">
              <button @click="showDeleteModal = false" class="py-2.5 rounded-xl bg-white/5 border border-white/10 text-gray-400 text-sm font-bold cursor-pointer hover:bg-white/10 transition-all">取消</button>
              <button @click="executeDeleteSection" class="py-2.5 rounded-xl bg-red-500 text-white text-sm font-bold cursor-pointer hover:bg-red-600 transition-all"><i class="fas fa-trash-alt mr-1"></i> 确认删除</button>
            </div>
          </div>
        </div>
      </Transition>

      <!-- ==================== Tab: 配音 ==================== -->
      <div v-show="activeTab === 'voice'">
        <div v-if="!activeSection" class="text-center py-16 text-gray-600">
          <i class="fas fa-microphone text-3xl mb-3"></i>
          <p class="text-sm">选择一个段落开始配音</p>
        </div>
        <div v-else class="space-y-4">
          <!-- 当前段落信息 -->
          <div class="bg-[#1a1a1c] border border-white/5 rounded-2xl p-4">
            <h3 class="text-xs font-bold text-gray-400 mb-2">🎯 当前段落</h3>
            <p class="text-sm font-bold text-white">{{ activeSection.title }}</p>
            <p class="text-[10px] text-gray-500 mt-1">{{ (activeSection.content || '').length }} 字</p>
          </div>

          <!-- 配音引擎切换（非广播剧时显示） -->
          <div v-if="!isDramaType" class="bg-[#1a1a1c] border border-white/5 rounded-2xl p-4">
            <h3 class="text-xs font-bold text-gray-400 mb-3 flex items-center gap-1.5">
              <i class="fas fa-cog text-blue-400"></i> 配音引擎
            </h3>
            <div class="grid grid-cols-2 gap-2">
              <button @click="switchEngine('tts-1.0')"
                      class="py-2.5 rounded-xl text-xs font-bold cursor-pointer transition-all text-center"
                      :class="selectedEngine === 'tts-1.0' ? 'bg-[#FF9500]/15 text-[#FF9500] border border-[#FF9500]/30' : 'bg-white/5 text-gray-400 border border-transparent hover:bg-white/10'">
                🔊 基础配音
                <div class="text-[9px] opacity-60 mt-0.5">TTS 1.0 · 免费</div>
              </button>
              <button @click="switchEngine('tts-2.0')"
                      class="py-2.5 rounded-xl text-xs font-bold cursor-pointer transition-all text-center relative"
                      :class="selectedEngine === 'tts-2.0' ? 'bg-purple-500/15 text-purple-400 border border-purple-500/30' : 'bg-white/5 text-gray-400 border border-transparent hover:bg-white/10'">
                🎭 情感配音
                <div class="text-[9px] opacity-60 mt-0.5">TTS 2.0 · VIP</div>
                <span class="absolute -top-1 -right-1 text-[7px] bg-gradient-to-r from-amber-400 to-yellow-500 text-black px-1 rounded font-bold">VIP</span>
              </button>
            </div>
          </div>

          <!-- ═══ 广播剧专用：分角色配音面板 ═══ -->
          <template v-if="isDramaType">
            <!-- 角色解析 -->
            <div class="bg-[#1a1a1c] border border-white/5 rounded-2xl p-4">
              <h3 class="text-xs font-bold text-gray-400 mb-3 flex items-center gap-1.5">
                <i class="fas fa-theater-masks text-pink-400"></i> 分角色配音
                <button @click="parseDramaScript(activeSection)" :disabled="dramaParsingScript"
                        class="ml-auto px-2 py-0.5 rounded-lg text-[9px] font-bold cursor-pointer transition-all"
                        :class="dramaParsingScript ? 'bg-gray-700 text-gray-400' : 'bg-pink-500/20 text-pink-400 hover:bg-pink-500/30'">
                  <i v-if="dramaParsingScript" class="fas fa-circle-notch fa-spin mr-0.5"></i>
                  <i v-else class="fas fa-sync-alt mr-0.5"></i>
                  {{ dramaParsingScript ? '解析中...' : '🔄 重新解析' }}
                </button>
              </h3>

              <!-- 无角色提示 -->
              <div v-if="dramaCharacters.length === 0 && !dramaParsingScript" class="text-center py-6">
                <p class="text-[11px] text-gray-500 mb-2">📜 点击解析按钮提取角色</p>
                <button @click="parseDramaScript(activeSection)"
                        class="px-4 py-2 rounded-xl bg-pink-500/15 text-pink-400 text-xs font-bold cursor-pointer hover:bg-pink-500/25 transition-all">
                  <i class="fas fa-magic mr-1"></i> 一键解析角色
                </button>
              </div>

              <!-- 角色列表 -->
              <div v-else class="space-y-3">
                <p class="text-[10px] text-gray-500">📜 检测到 {{ dramaCharacters.length }} 个角色 · {{ dramaLines.length }} 句台词</p>
                <div v-for="char in dramaCharacters" :key="char"
                     class="bg-white/[0.03] border border-white/5 rounded-xl p-3 space-y-2">
                  <!-- 角色名 -->
                  <div class="flex items-center gap-2">
                    <span class="w-6 h-6 rounded-full bg-pink-500/15 flex items-center justify-center text-[10px] text-pink-400 font-bold">
                      {{ char.substring(0, 1) }}
                    </span>
                    <span class="text-sm font-bold text-white">{{ char }}</span>
                    <span class="text-[9px] text-gray-500 ml-auto">{{ dramaLines.filter(l => l.character === char).length }} 句</span>
                  </div>
                  <!-- 音色选择 -->
                  <button @click="dramaVoicePickerFor = char; showVoicePicker = true"
                          class="w-full flex items-center justify-between px-3 py-2 rounded-lg bg-white/5 border border-white/10 hover:border-pink-500/30 cursor-pointer transition-all text-left">
                    <div class="flex items-center gap-1.5">
                      <i class="fas fa-microphone-alt text-pink-400 text-[10px]"></i>
                      <span class="text-[11px] text-white">{{ characterVoiceNames[char] || '选择音色' }}</span>
                    </div>
                    <i class="fas fa-chevron-right text-gray-600 text-[8px]"></i>
                  </button>
                  <!-- 情感选择 -->
                  <div class="flex flex-wrap gap-1">
                    <button v-for="emo in ['冷漠傲慢', '温柔感伤', '惊慌恐惧', '愤怒爆发', '平稳叙事', '撩气暗示']" :key="emo"
                            @click="characterEmotionMap[char] = characterEmotionMap[char] === emo ? '' : emo"
                            class="px-1.5 py-0.5 rounded text-[9px] cursor-pointer transition-all"
                            :class="characterEmotionMap[char] === emo ? 'bg-pink-500/20 text-pink-400 border border-pink-500/30' : 'bg-white/5 text-gray-500 hover:bg-white/10'">
                      {{ emo }}
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <!-- 分角色合成按钮 -->
            <button v-if="dramaCharacters.length > 0" @click="synthesizeDrama" :disabled="synthesizingSection"
                    class="w-full py-3 rounded-xl text-xs font-bold cursor-pointer transition-all flex items-center justify-center gap-1.5"
                    :class="synthesizingSection ? 'bg-gray-700 text-gray-400' : 'bg-gradient-to-r from-pink-600 to-purple-600 text-white'">
              <i v-if="synthesizingSection" class="fas fa-circle-notch fa-spin"></i>
              <i v-else class="fas fa-theater-masks"></i>
              {{ synthesizingSection ? `分角色合成中 ${synthChunkProgress}/${synthChunkTotal}...` : `🎭 开始分角色配音（${dramaCharacters.length} 个角色）` }}
            </button>
          </template>

          <!-- ═══ 通用配音面板（非广播剧） ═══ -->
          <template v-else>
            <!-- 情感配音特有：语气指令 -->
            <div v-if="selectedEngine === 'tts-2.0'" class="bg-[#1a1a1c] border border-white/5 rounded-2xl p-4">
              <h3 class="text-xs font-bold text-gray-400 mb-3 flex items-center gap-1.5">
                <i class="fas fa-theater-masks text-purple-400"></i> 语气指令
                <button @click="autoEmotionTag" :disabled="autoTagging"
                        class="ml-auto px-2 py-0.5 rounded-lg text-[9px] font-bold cursor-pointer transition-all"
                        :class="autoTagging ? 'bg-gray-700 text-gray-400' : 'bg-purple-500/20 text-purple-400 hover:bg-purple-500/30'">
                  <i v-if="autoTagging" class="fas fa-circle-notch fa-spin mr-0.5"></i>
                  <i v-else class="fas fa-magic mr-0.5"></i>
                  {{ autoTagging ? '分析中...' : '🎭 智能匹配语气' }}
                </button>
              </h3>
              <input v-model="emotionInstruction" type="text"
                     placeholder="例如：用温柔感伤的语气、用愤怒吱喤的语气..."
                     class="w-full bg-black/30 border border-white/5 rounded-xl px-3 py-2 text-sm text-purple-300 outline-none focus:border-purple-500/50 placeholder-gray-600 mb-2" />
              <div class="flex flex-wrap gap-1.5">
                <button v-for="tag in emotionPresets" :key="tag"
                        @click="emotionInstruction = tag"
                        class="px-2 py-1 rounded-lg text-[10px] cursor-pointer transition-all"
                        :class="emotionInstruction === tag ? 'bg-purple-500/20 text-purple-400 border border-purple-500/30' : 'bg-white/5 text-gray-500 hover:bg-white/10'">
                  {{ tag }}
                </button>
              </div>
            </div>

            <!-- 音色选择 -->
            <div class="bg-[#1a1a1c] border border-white/5 rounded-2xl p-4">
              <h3 class="text-xs font-bold text-gray-400 mb-3 flex items-center gap-1.5">
                <i class="fas fa-user-voice text-purple-400"></i> 选择音色
                <span class="text-[9px] px-1.5 py-0.5 rounded-full" :class="selectedEngine === 'tts-2.0' ? 'bg-purple-500/20 text-purple-400' : 'bg-gray-500/20 text-gray-400'">{{ selectedEngine === 'tts-2.0' ? '情感音色' : '基础音色' }}</span>
              </h3>
              <button @click="showVoicePicker = true"
                      class="w-full flex items-center justify-between px-4 py-3 rounded-xl bg-white/5 border border-white/10 hover:border-purple-500/50 cursor-pointer transition-all">
                <div class="flex items-center gap-2">
                  <i class="fas fa-microphone-alt text-purple-400"></i>
                  <span class="text-sm text-white">{{ currentVoiceName }}</span>
                </div>
                <i class="fas fa-chevron-right text-gray-500 text-xs"></i>
              </button>
            </div>

            <!-- 操作按钮 —— 按引擎区分 -->
            <template v-if="selectedEngine === 'tts-1.0'">
              <div class="grid grid-cols-2 gap-3">
                <button @click="synthesizeSection(activeSection)" :disabled="synthesizingSection"
                        class="py-3 rounded-xl text-xs font-bold cursor-pointer transition-all flex items-center justify-center gap-1.5"
                        :class="synthesizingSection ? 'bg-gray-700 text-gray-400' : 'bg-gradient-to-r from-green-600 to-emerald-500 text-white'">
                  <i v-if="synthesizingSection" class="fas fa-circle-notch fa-spin"></i>
                  <i v-else class="fas fa-volume-up"></i>
                  {{ synthesizingSection ? `合成 ${synthChunkProgress}/${synthChunkTotal} 片...` : '🔊 合成当前段' }}
                </button>
                <button @click="synthesizeAll" :disabled="synthesizingAll"
                        class="py-3 rounded-xl text-xs font-bold cursor-pointer transition-all flex items-center justify-center gap-1.5"
                        :class="synthesizingAll ? 'bg-gray-700 text-gray-400' : 'bg-gradient-to-r from-blue-600 to-cyan-500 text-white'">
                  <i v-if="synthesizingAll" class="fas fa-circle-notch fa-spin"></i>
                  <i v-else class="fas fa-headphones"></i>
                  {{ synthesizingAll ? `${synthProgress}/${unsynthesizedCount}` : (unsynthesizedCount === 0 ? '✅ 全部已配音' : `🎧 合成剩余 ${unsynthesizedCount} 段`) }}
                </button>
              </div>
            </template>
            <template v-else>
              <div v-if="activeSection?.content?.length > 1000" class="bg-red-500/10 border border-red-500/30 rounded-xl px-3 py-2 text-[10px] text-red-400 mb-2">
                ⚠️ 当前段落 {{ activeSection.content.length }} 字，情感配音 2.0 单次最大支持 1000 字。超出部分将自动分片合成。
              </div>
              <button @click="synthesizeEmotionV2" :disabled="synthesizingSection"
                      class="w-full py-3 rounded-xl text-xs font-bold cursor-pointer transition-all flex items-center justify-center gap-1.5"
                      :class="synthesizingSection ? 'bg-gray-700 text-gray-400' : 'bg-gradient-to-r from-purple-600 to-pink-500 text-white'">
                <i v-if="synthesizingSection" class="fas fa-circle-notch fa-spin"></i>
                <i v-else class="fas fa-theater-masks"></i>
                {{ synthesizingSection ? `情感合成中 ${synthChunkProgress}/${synthChunkTotal}...` : '🎭 一键情感合成' }}
              </button>
            </template>
          </template>

          <!-- 试听按钮 -->
          <button v-if="activeSection.audioUrl" @click="playAudio(activeSection)"
                  class="w-full py-3 rounded-xl bg-blue-600/20 border border-blue-500/30 text-blue-300 text-xs font-bold cursor-pointer hover:bg-blue-600/30 transition-all flex items-center justify-center gap-2">
            <i class="fas fa-play"></i> ▶ 试听「{{ activeSection.title }}」
          </button>

          <!-- 合成状态一览 -->
          <div class="bg-[#1a1a1c] border border-white/5 rounded-2xl p-4">
            <h3 class="text-xs font-bold text-gray-400 mb-2">📊 合成进度</h3>
            <div class="space-y-1.5">
              <div v-for="(s, i) in sections" :key="s.id" class="flex items-center gap-2 text-[11px]">
                <span class="w-16 truncate text-gray-400">{{ s.title || `第${i+1}段` }}</span>
                <div class="flex-1 h-1.5 bg-white/5 rounded-full overflow-hidden">
                  <div v-if="synthesizingSection && activeSectionIdx === i"
                       class="h-full rounded-full bg-blue-500 transition-all duration-300"
                       :style="{ width: synthChunkTotal > 0 ? (synthChunkProgress / synthChunkTotal * 100) + '%' : '0%' }"></div>
                  <div v-else-if="synthesizingAll && synthProgress === i"
                       class="h-full rounded-full bg-blue-500 animate-pulse w-1/2"></div>
                  <div v-else
                       class="h-full rounded-full transition-all"
                       :class="s.audioUrl ? 'bg-green-500 w-full' : 'bg-gray-600 w-0'"></div>
                </div>
                <span class="text-[9px]" :class="s.audioUrl ? 'text-green-400' : 'text-gray-600'">{{ s.audioUrl ? '✅' : '⬜' }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
      </template><!-- end !isStructuredMode -->
      </template><!-- end isMultiSection -->

      <!-- ══════════ 广播剧 — 对话驱动模式 ══════════ -->
      <template v-else-if="isDramaType">
        <div class="space-y-4 py-3">

          <!-- ═══ 创作设定区（生成后隐藏）═══ -->
          <template v-if="!dramaFinished">

          <!-- ═══ 1. 对话模式选择 ═══ -->
          <div class="bg-[#1a1a1c] border border-white/5 rounded-2xl p-4">
            <h3 class="text-xs font-bold text-gray-400 mb-3 flex items-center gap-1.5">
              <i class="fas fa-masks-theater text-purple-400"></i> 选择对话模式
            </h3>
            <div class="grid grid-cols-3 gap-2">
              <button v-for="mode in dialogueModes" :key="mode.id"
                      @click="dialogueMode = mode.id"
                      :disabled="dramaGenerating"
                      class="p-3 rounded-xl text-center cursor-pointer transition-all border"
                      :class="dialogueMode === mode.id 
                        ? 'bg-purple-500/15 border-purple-500/30 text-purple-300 shadow-lg shadow-purple-500/10' 
                        : 'bg-white/[0.03] border-white/5 text-gray-400 hover:bg-white/[0.06]'">
                <div class="text-2xl mb-1">{{ mode.icon }}</div>
                <div class="text-[11px] font-bold">{{ mode.name }}</div>
                <div class="text-[9px] text-gray-500 mt-0.5">{{ mode.desc }}</div>
              </button>
            </div>
            <p class="text-[9px] text-gray-600 mt-2 text-center">{{ dialogueModes.find(m => m.id === dialogueMode)?.example }}</p>
          </div>

          <!-- ═══ 2. 角色设定 ═══ -->
          <div class="bg-[#1a1a1c] border border-white/5 rounded-2xl p-4">
            <h3 class="text-xs font-bold text-gray-400 mb-3 flex items-center gap-1.5">
              <i class="fas fa-users text-cyan-400"></i> 角色设定
              <span class="text-[9px] text-gray-600 ml-auto">可编辑</span>
            </h3>
            <div class="space-y-2">
              <div v-for="(role, idx) in dramaRoles" :key="idx"
                   class="flex gap-2 items-start bg-white/[0.02] rounded-xl p-2.5 border border-white/5">
                <div class="w-8 h-8 rounded-full bg-gradient-to-br flex items-center justify-center text-white text-[10px] font-bold shrink-0"
                     :class="idx === 0 ? 'from-blue-500 to-cyan-400' : idx === 1 ? 'from-pink-500 to-rose-400' : idx === 2 ? 'from-purple-500 to-violet-400' : 'from-gray-500 to-gray-400'">
                  {{ role.name?.[0] || '?' }}
                </div>
                <div class="flex-1 min-w-0 space-y-1">
                  <div class="flex gap-1.5">
                    <input v-model="role.name" :disabled="dramaGenerating" placeholder="角色名"
                           class="w-16 bg-transparent border-b border-white/10 text-[11px] text-gray-200 outline-none focus:border-purple-400/50 px-0.5 py-0.5" />
                    <input v-model="role.voiceType" :disabled="dramaGenerating" placeholder="声线"
                           class="flex-1 bg-transparent border-b border-white/10 text-[10px] text-gray-400 outline-none focus:border-purple-400/50 px-0.5 py-0.5" />
                  </div>
                  <input v-model="role.desc" :disabled="dramaGenerating" placeholder="性格描述"
                         class="w-full bg-transparent border-b border-white/5 text-[10px] text-gray-500 outline-none focus:border-purple-400/50 px-0.5 py-0.5" />
                </div>
              </div>
            </div>
          </div>

          <!-- ═══ 3. 题材 + 灵感 ═══ -->
          <div class="bg-[#1a1a1c] border border-white/5 rounded-2xl p-4">
            <h3 class="text-xs font-bold text-gray-400 mb-3 flex items-center gap-1.5">
              <i class="fas fa-palette text-orange-400"></i> 题材方向
            </h3>
            <div class="flex flex-wrap gap-1.5 mb-3">
              <button v-for="genre in dramaGenres" :key="genre.id"
                      @click="selectStyle(genre)"
                      :disabled="dramaGenerating"
                      class="px-3 py-2 rounded-xl text-[10px] font-bold cursor-pointer transition-all border flex items-center gap-1"
                      :class="stylePreference === genre.id 
                        ? 'bg-gradient-to-r ' + genre.gradient + ' border-white/10 text-white shadow-lg' 
                        : 'bg-white/[0.03] border-white/5 text-gray-400 hover:bg-white/[0.06]'">
                {{ genre.icon }} {{ genre.name }}
                <span v-if="genre.heat >= 5" class="text-[8px] px-1 py-0.5 rounded bg-red-500/30 text-red-300">🔥</span>
              </button>
            </div>
            <!-- 灵感 -->
            <textarea v-model="editableInspiration" rows="2" :disabled="dramaGenerating"
                      placeholder="描述剧情灵感...例如：霍总装穷追妻被女主打脸"
                      class="w-full bg-transparent border-b border-white/10 px-1 py-2 text-[11px] text-gray-300 outline-none focus:border-orange-400/50 resize-none appearance-none hide-scrollbar"
                      style="background: transparent !important;" @blur="saveInspiration"></textarea>
          </div>

          <!-- ═══ 4. 生成按钮 ═══ -->
          <button @click="generateDramaScript" :disabled="dramaGenerating || !stylePreference || !dramaCanRegenerate"
                  class="w-full py-4 rounded-2xl font-bold text-sm cursor-pointer transition-all flex items-center justify-center gap-2 disabled:opacity-30 disabled:cursor-not-allowed bg-gradient-to-r from-purple-600 via-pink-500 to-orange-500 text-white hover:shadow-lg hover:shadow-purple-500/20 active:scale-[0.98]">
            <i v-if="dramaGenerating" class="fas fa-circle-notch fa-spin"></i>
            <i v-else class="fas fa-clapperboard"></i>
            {{ dramaButtonLabel }}
          </button>

          </template><!-- end 创作设定区 -->

          <!-- ═══ 5. 流式输出 / 完成预览 ═══ -->
          <div v-if="dramaStreamContent || dramaGenerating" class="bg-[#1a1a1c] border border-white/5 rounded-2xl overflow-hidden">
            <!-- 生成中：紧凑流式输出 -->
            <template v-if="!dramaFinished">
              <div class="p-4">
                <h3 class="text-xs font-bold text-purple-400 mb-3 flex items-center gap-1.5">
                  <i class="fas fa-pen-nib"></i> ✍️ 正在创作...
                  <span v-if="dramaStreamContent" class="text-[9px] text-gray-500 ml-auto">{{ dramaStreamContent.length }} 字</span>
                </h3>
                <div class="max-h-[40vh] overflow-y-auto hide-scrollbar space-y-0.5">
                  <template v-for="(line, li) in dramaStreamContent.split('\n').filter(l => l.trim()).slice(-20)" :key="li">
                    <div v-if="line.includes('：') || line.includes(':')" class="flex items-start gap-1.5 py-0.5">
                      <span class="text-[10px] font-bold shrink-0 px-1.5 py-0.5 rounded"
                            :class="li % 4 === 0 ? 'text-blue-300 bg-blue-500/10' : li % 4 === 1 ? 'text-pink-300 bg-pink-500/10' : li % 4 === 2 ? 'text-purple-300 bg-purple-500/10' : 'text-cyan-300 bg-cyan-500/10'">
                        {{ line.split(/[：:]/)[0].trim() }}
                      </span>
                      <span class="text-[11px] text-gray-200 leading-relaxed">{{ line.split(/[：:]/).slice(1).join('：').trim() }}</span>
                    </div>
                    <div v-else class="text-[10px] text-gray-400 py-0.5">{{ line }}</div>
                  </template>
                </div>
              </div>
            </template>

            <!-- 完成后：精美统计卡 + 格式化阅读 -->
            <template v-else>
              <!-- 顶部渐变彩条 -->
              <div class="h-1 bg-gradient-to-r from-purple-500 via-pink-500 to-orange-500"></div>
              <div class="p-4">
                <!-- 完成标题 + 摘要统计 -->
                <div class="flex items-center justify-between mb-3">
                  <h3 class="text-xs font-bold text-green-400 flex items-center gap-1.5">
                    <i class="fas fa-check-circle"></i> ✅ 创作完成
                  </h3>
                  <div class="flex items-center gap-3 text-[9px] text-gray-500">
                    <span class="flex items-center gap-1"><i class="fas fa-font text-purple-400/60"></i>{{ dramaStreamContent.length }} 字</span>
                    <span v-if="dramaCharacters.length" class="flex items-center gap-1"><i class="fas fa-users text-pink-400/60"></i>{{ dramaCharacters.length }} 角色</span>
                    <span v-if="dramaLines.length" class="flex items-center gap-1"><i class="fas fa-comment-dots text-cyan-400/60"></i>{{ dramaLines.length }} 句</span>
                  </div>
                </div>

                <!-- 角色速览条（如果已解析角色） -->
                <div v-if="dramaCharacters.length > 0" class="flex items-center gap-2 mb-3 pb-3 border-b border-white/5 overflow-x-auto hide-scrollbar">
                  <div v-for="(char, ci) in dramaCharacters" :key="char"
                       class="flex items-center gap-1.5 px-2 py-1 rounded-lg shrink-0"
                       :class="ci % 4 === 0 ? 'bg-blue-500/10' : ci % 4 === 1 ? 'bg-pink-500/10' : ci % 4 === 2 ? 'bg-purple-500/10' : 'bg-cyan-500/10'">
                    <span class="text-[10px] font-bold"
                          :class="ci % 4 === 0 ? 'text-blue-300' : ci % 4 === 1 ? 'text-pink-300' : ci % 4 === 2 ? 'text-purple-300' : 'text-cyan-300'">
                      {{ char }}
                    </span>
                    <span v-if="characterVoiceNames[char]" class="text-[8px] text-gray-500">🎤 {{ characterVoiceNames[char] }}</span>
                  </div>
                </div>

                <!-- 格式化对话阅读区（默认展开，可折叠） -->
                <div class="flex items-center justify-between mb-2">
                  <button @click="dramaShowFull = !dramaShowFull"
                          class="text-[10px] font-bold cursor-pointer transition-all flex items-center gap-1"
                          :class="dramaShowFull ? 'text-green-300' : 'text-gray-400 hover:text-green-300'">
                    <i class="fas" :class="dramaShowFull ? 'fa-chevron-down' : 'fa-chevron-right'"></i>
                    📖 {{ dramaShowFull ? '收起全文' : '展开全文阅读' }}
                  </button>
                </div>
                <Transition name="expand">
                  <div v-if="dramaShowFull" class="max-h-[55vh] overflow-y-auto hide-scrollbar space-y-0.5 bg-black/20 rounded-xl p-3">
                    <template v-for="(line, li) in (activeSection?.content || dramaStreamContent).split('\n').filter(l => l.trim())" :key="li">
                      <div v-if="line.trim().startsWith('[') && line.trim().endsWith(']')"
                           class="text-[10px] text-amber-400/60 italic text-center py-1.5">
                        {{ line.trim() }}
                      </div>
                      <div v-else-if="line.trim().startsWith('##')"
                           class="text-[12px] font-bold text-white/80 text-center py-2.5 border-b border-white/5 mb-2">
                        {{ line.trim().replace(/^#+\s*/, '') }}
                      </div>
                      <div v-else-if="line.includes('：') || line.includes(':')"
                           class="flex items-start gap-1.5 py-1 px-1 rounded-lg hover:bg-white/[0.02] transition-colors">
                        <span class="text-[10px] font-bold shrink-0 px-1.5 py-0.5 rounded mt-0.5"
                              :style="{ color: getCharColor(line.split(/[：:]/)[0].trim()), backgroundColor: getCharColor(line.split(/[：:]/)[0].trim()) + '18' }">
                          {{ line.split(/[：:]/)[0].trim() }}
                        </span>
                        <span class="text-[11px] text-gray-200 leading-relaxed">{{ line.split(/[：:]/).slice(1).join('：').trim() }}</span>
                      </div>
                      <div v-else class="text-[10px] text-gray-400 py-0.5 px-1">{{ line }}</div>
                    </template>
                  </div>
                </Transition>
              </div>
            </template>
          </div>


          <!-- ═══ 7. 分角色配音区 ═══ -->
          <div v-if="activeSection?.content && dramaFinished" class="bg-[#1a1a1c] border border-white/5 rounded-2xl p-4">
            <h3 class="text-xs font-bold text-gray-400 mb-3 flex items-center gap-1.5">
              <i class="fas fa-theater-masks text-pink-400"></i> 分角色配音
              <span v-if="activeSection?.audioUrl" class="ml-auto text-[9px] text-green-400 font-bold"><i class="fas fa-check-circle mr-0.5"></i> 已完成</span>
              <button v-else @click="parseDramaScript(activeSection)" :disabled="dramaParsingScript"
                      class="ml-auto px-2 py-0.5 rounded-lg text-[9px] font-bold cursor-pointer transition-all"
                      :class="dramaParsingScript ? 'bg-gray-700 text-gray-400' : 'bg-pink-500/20 text-pink-400 hover:bg-pink-500/30'">
                <i v-if="dramaParsingScript" class="fas fa-circle-notch fa-spin mr-0.5"></i>
                <i v-else class="fas fa-sync-alt mr-0.5"></i>
                {{ dramaParsingScript ? '解析中...' : '🔄 解析角色' }}
              </button>
            </h3>

            <!-- 无角色提示 -->
            <div v-if="dramaCharacters.length === 0 && !dramaParsingScript" class="text-center py-6">
              <p class="text-[11px] text-gray-500 mb-2">📜 点击解析按钮提取角色和台词</p>
              <button @click="parseDramaScript(activeSection)"
                      class="px-4 py-2 rounded-xl bg-pink-500/15 text-pink-400 text-xs font-bold cursor-pointer hover:bg-pink-500/25 transition-all">
                <i class="fas fa-magic mr-1"></i> 一键解析角色
              </button>
            </div>

            <!-- 角色列表 + 音色选择 -->
            <div v-else class="space-y-3">
              <p class="text-[10px] text-gray-500">📜 检测到 {{ dramaCharacters.length }} 个角色 · {{ dramaLines.length }} 句台词</p>
              <div v-for="char in dramaCharacters" :key="char"
                   class="bg-white/[0.03] border border-white/5 rounded-xl p-3 space-y-2">
                <div class="flex items-center gap-2">
                  <span class="w-6 h-6 rounded-full bg-pink-500/15 flex items-center justify-center text-[10px] text-pink-400 font-bold">
                    {{ char.substring(0, 1) }}
                  </span>
                  <span class="text-sm font-bold text-white">{{ char }}</span>
                  <span class="text-[9px] text-gray-500 ml-auto">{{ dramaLines.filter(l => l.character === char).length }} 句</span>
                </div>
                <!-- 音色选择 -->
                <button @click="dramaVoicePickerFor = char; showVoicePicker = true"
                        :disabled="!!activeSection?.audioUrl"
                        class="w-full flex items-center justify-between px-3 py-2 rounded-lg border transition-all text-left"
                        :class="activeSection?.audioUrl ? 'bg-white/[0.02] border-white/5 cursor-default opacity-60' : 'bg-white/5 border-white/10 hover:border-pink-500/30 cursor-pointer'">
                  <div class="flex items-center gap-1.5">
                    <i class="fas fa-microphone-alt text-pink-400 text-[10px]"></i>
                    <span class="text-[11px] text-white">{{ characterVoiceNames[char] || '选择音色' }}</span>
                  </div>
                  <i v-if="!activeSection?.audioUrl" class="fas fa-chevron-right text-gray-600 text-[8px]"></i>
                  <i v-else class="fas fa-lock text-gray-600 text-[8px]"></i>
                </button>
                <!-- 情感选择 -->
                <div v-if="!activeSection?.audioUrl" class="flex flex-wrap gap-1">
                  <button v-for="emo in ['冷漠傲慢', '温柔感伤', '惊慌恐惧', '愤怒爆发', '平稳叙事', '撩气暗示']" :key="emo"
                          @click="characterEmotionMap[char] = characterEmotionMap[char] === emo ? '' : emo"
                          class="px-1.5 py-0.5 rounded text-[9px] cursor-pointer transition-all"
                          :class="characterEmotionMap[char] === emo ? 'bg-pink-500/20 text-pink-400 border border-pink-500/30' : 'bg-white/5 text-gray-500 hover:bg-white/10'">
                    {{ emo }}
                  </button>
                </div>
              </div>
            </div>

            <!-- 分角色合成按钮（已有音频时隐藏） -->
            <button v-if="dramaCharacters.length > 0 && !activeSection?.audioUrl" @click="synthesizeDrama" :disabled="synthesizingSection"
                    class="w-full py-3 rounded-xl text-xs font-bold cursor-pointer transition-all flex items-center justify-center gap-1.5 mt-3"
                    :class="synthesizingSection ? 'bg-gray-700 text-gray-400' : 'bg-gradient-to-r from-pink-600 to-purple-600 text-white'">
              <i v-if="synthesizingSection" class="fas fa-circle-notch fa-spin"></i>
              <i v-else class="fas fa-theater-masks"></i>
              {{ synthesizingSection ? `分角色合成中 ${synthChunkProgress}/${synthChunkTotal}...` : `🎭 开始分角色配音（${dramaCharacters.length} 个角色）` }}
            </button>
          </div>

          <!-- ═══ 8. 发布区 ═══ -->
          <div v-if="activeSection?.content && dramaFinished" class="bg-[#1a1a1c] border border-white/5 rounded-2xl p-4">
            <h3 class="text-xs font-bold text-gray-400 mb-3 flex items-center gap-1.5">
              <i class="fas fa-rocket text-purple-400"></i> 发布管理
            </h3>
            <!-- 未配音提示 -->
            <div v-if="!activeSection?.audioUrl" class="text-center py-4 space-y-2">
              <p class="text-[11px] text-gray-500">🎙️ 完成分角色配音后即可发布</p>
              <div class="flex items-center justify-center gap-2 text-[10px] text-gray-600">
                <span class="flex items-center gap-1"><i class="fas fa-check-circle text-green-400"></i> 剧本已生成</span>
                <span class="text-gray-700">→</span>
                <span class="flex items-center gap-1" :class="dramaCharacters.length > 0 ? 'text-green-400' : 'text-gray-600'">
                  <i class="fas" :class="dramaCharacters.length > 0 ? 'fa-check-circle text-green-400' : 'fa-circle text-gray-600'"></i> 角色已解析
                </span>
                <span class="text-gray-700">→</span>
                <span class="flex items-center gap-1 text-gray-600"><i class="fas fa-circle"></i> 配音合成</span>
                <span class="text-gray-700">→</span>
                <span class="flex items-center gap-1 text-gray-600"><i class="fas fa-circle"></i> 发布</span>
              </div>
            </div>
            <!-- 已配音 → 可发布 -->
            <div v-else class="space-y-3">
              <!-- 播放音频 -->
              <button @click="playAudio(activeSection)"
                      class="w-full flex items-center justify-between px-4 py-3 rounded-xl bg-green-500/10 border border-green-500/20 hover:bg-green-500/15 cursor-pointer transition-all">
                <div class="flex items-center gap-2">
                  <i class="fas fa-play-circle text-green-400"></i>
                  <span class="text-sm font-bold text-green-300">▶ 播放成品音频</span>
                </div>
                <span class="text-[9px] text-gray-500">{{ activeSection.title }}</span>
              </button>
              <!-- 发布按钮 -->
              <button v-if="project?.status !== 'completed'" @click="publishProject" :disabled="publishing"
                      class="w-full py-3 rounded-xl text-sm font-bold cursor-pointer transition-all flex items-center justify-center gap-2"
                      :class="publishing ? 'bg-gray-700 text-gray-400' : 'bg-gradient-to-r from-purple-600 to-blue-600 text-white hover:shadow-lg hover:shadow-purple-500/20'">
                <i v-if="publishing" class="fas fa-circle-notch fa-spin"></i>
                <i v-else class="fas fa-rocket"></i>
                {{ publishing ? '发布中...' : '🚀 发布到发现页' }}
              </button>
              <div v-else class="text-center py-2">
                <span class="text-green-400 font-bold text-sm"><i class="fas fa-check-circle mr-1"></i> ✅ 已发布</span>
              </div>
            </div>
          </div>

        </div>
      </template><!-- end isDramaType -->

      <!-- ══════════ 单次生成模式（播客、电台、讲解、带货、绘本、新闻）══════════ -->
      <template v-else>
        <div class="space-y-4 py-3">

          <!-- ═══ 播客项目详情（优化版） ═══ -->
          <template v-if="project?.typeCode === 'podcast'">

            <!-- 1️⃣ 播客封面卡片 -->
            <div class="relative overflow-hidden rounded-2xl bg-gradient-to-br from-[#0c2233] via-[#0a1628] to-[#0d1f2d] border border-cyan-500/10">
              <!-- 装饰背景 -->
              <div class="absolute inset-0 opacity-20">
                <div class="absolute top-[-30%] right-[-20%] w-[60%] h-[60%] rounded-full bg-cyan-500/30 blur-[80px]"></div>
                <div class="absolute bottom-[-20%] left-[-10%] w-[40%] h-[40%] rounded-full bg-blue-600/30 blur-[60px]"></div>
              </div>
              <div class="relative p-5">
                <!-- 顶部状态条 -->
                <div class="flex items-center justify-between mb-4">
                  <div class="flex items-center gap-2">
                    <span class="px-2 py-0.5 rounded-full text-[9px] font-bold bg-cyan-500/20 text-cyan-400 border border-cyan-500/20">
                      <i class="fas fa-podcast mr-1"></i> AI 双播
                    </span>
                    <span v-if="project?.status === 'completed'" class="px-2 py-0.5 rounded-full text-[9px] font-bold bg-green-500/20 text-green-400 border border-green-500/20">
                      ✅ 已发布
                    </span>
                    <span v-else class="px-2 py-0.5 rounded-full text-[9px] font-bold bg-amber-500/20 text-amber-400 border border-amber-500/20">
                      📝 待发布
                    </span>
                  </div>
                  <span class="text-[9px] text-gray-600">{{ formatDate(project?.createdAt) }}</span>
                </div>

                <!-- 播客信息 -->
                <div class="flex items-start gap-4">
                  <!-- 封面图标 -->
                  <div class="w-20 h-20 rounded-2xl bg-gradient-to-br from-cyan-500 to-blue-600 flex items-center justify-center shrink-0 shadow-[0_4px_20px_rgba(6,182,212,0.3)]">
                    <span class="text-3xl">🎙️</span>
                  </div>
                  <!-- 标题 + 元信息 -->
                  <div class="flex-1 min-w-0">
                    <h2 class="text-base font-bold text-white mb-1.5 leading-tight">{{ project?.title || '未命名播客' }}</h2>
                    <div class="flex flex-wrap gap-x-3 gap-y-1 text-[10px] text-gray-500">
                      <span v-if="sections.length"><i class="fas fa-comments mr-1 text-cyan-500/60"></i>{{ podcastDialogues.length }} 轮对话</span>
                      <span><i class="fas fa-file-alt mr-1 text-cyan-500/60"></i>{{ sections.reduce((a, s) => a + (s.content?.length || 0), 0) }} 字</span>
                      <span v-if="sections.some(s => s.audioUrl)"><i class="fas fa-music mr-1 text-green-500/60"></i>音频就绪</span>
                    </div>
                  </div>
                </div>

                <!-- 播放按钮 -->
                <div v-if="sections.some(s => s.audioUrl)" class="mt-4">
                  <button @click="playPodcastAudio"
                          class="w-full py-3 rounded-xl text-sm font-bold cursor-pointer transition-all flex items-center justify-center gap-2 bg-gradient-to-r from-cyan-500 to-blue-600 text-white hover:shadow-[0_4px_20px_rgba(6,182,212,0.3)] active:scale-[0.98]">
                    <i class="fas fa-play"></i> 播放播客
                  </button>
                </div>

                <!-- 新建播客入口 -->
                <button @click="$router.push('/podcast')"
                        class="w-full mt-3 py-2.5 rounded-xl text-[11px] font-bold cursor-pointer transition-all flex items-center justify-center gap-1.5 bg-white/5 border border-white/10 text-gray-400 hover:bg-white/10 hover:text-white">
                  <i class="fas fa-plus"></i> 创建新播客
                </button>
              </div>
            </div>

            <!-- 2️⃣ 对话内容（气泡式） -->
            <div v-if="podcastDialogues.length > 0" class="bg-[#1a1a1c] border border-white/5 rounded-2xl overflow-hidden">
              <div class="px-4 py-3 border-b border-white/5 flex items-center justify-between">
                <h3 class="text-xs font-bold text-white flex items-center gap-1.5">
                  <i class="fas fa-comments text-cyan-400"></i> 对话内容
                </h3>
                <span class="text-[9px] text-gray-600">{{ podcastDialogues.length }} 轮</span>
              </div>
              <div class="max-h-[40vh] overflow-y-auto hide-scrollbar p-3 space-y-3">
                <div v-for="(msg, idx) in podcastDialogues" :key="idx"
                     class="flex gap-2.5" :class="msg.isHostB ? 'flex-row-reverse' : ''">
                  <!-- 头像 -->
                  <div class="w-7 h-7 rounded-full shrink-0 flex items-center justify-center text-[11px] font-bold"
                       :class="msg.isHostB ? 'bg-gradient-to-br from-pink-500 to-rose-600 text-white' : 'bg-gradient-to-br from-cyan-500 to-blue-600 text-white'">
                    {{ msg.isHostB ? 'B' : 'A' }}
                  </div>
                  <!-- 气泡 -->
                  <div class="max-w-[80%] rounded-2xl px-3.5 py-2.5 text-[12px] leading-relaxed"
                       :class="msg.isHostB ? 'bg-pink-500/10 text-pink-100 rounded-tr-sm' : 'bg-cyan-500/10 text-cyan-100 rounded-tl-sm'">
                    <div class="text-[9px] font-bold mb-0.5" :class="msg.isHostB ? 'text-pink-400' : 'text-cyan-400'">{{ msg.speaker }}</div>
                    {{ msg.text }}
                  </div>
                </div>
              </div>
            </div>

            <!-- 3️⃣ 上架 / 下架 操作 -->
            <div class="bg-gradient-to-br from-white/[0.02] to-white/[0.04] border border-white/10 rounded-2xl overflow-hidden">
              <!-- 已上架 -->
              <template v-if="project?.status === 'completed'">
                <div class="p-4">
                  <div class="flex items-center gap-3 mb-3">
                    <div class="w-10 h-10 rounded-xl bg-green-500/15 flex items-center justify-center shrink-0">
                      <i class="fas fa-signal text-green-400"></i>
                    </div>
                    <div class="flex-1 min-w-0">
                      <div class="text-xs font-bold text-green-400 flex items-center gap-1.5">
                        <span class="w-1.5 h-1.5 rounded-full bg-green-400 animate-pulse"></span>
                        已上架 · 发现页可见
                      </div>
                      <div class="text-[10px] text-gray-500 mt-0.5">作品正在被更多人发现</div>
                    </div>
                  </div>
                  <div class="flex gap-2">
                    <button @click="$router.push('/discover')"
                            class="flex-1 px-3 py-2 rounded-xl text-[11px] font-bold cursor-pointer transition-all bg-white/5 text-gray-300 hover:bg-white/10 flex items-center justify-center gap-1">
                      <i class="fas fa-compass"></i> 查看作品
                    </button>
                    <button @click="unpublishProject" :disabled="publishing"
                            class="px-3 py-2 rounded-xl text-[11px] font-bold cursor-pointer transition-all bg-red-500/10 text-red-400 hover:bg-red-500/20 flex items-center justify-center gap-1">
                      <i v-if="publishing" class="fas fa-circle-notch fa-spin"></i>
                      <i v-else class="fas fa-eye-slash"></i>
                      {{ publishing ? '' : '下架' }}
                    </button>
                  </div>
                </div>
              </template>

              <!-- 未上架 -->
              <template v-else>
                <div class="p-4">
                  <div class="flex items-center gap-3 mb-3">
                    <div class="w-10 h-10 rounded-xl bg-cyan-500/10 flex items-center justify-center shrink-0">
                      <i class="fas fa-cloud-upload-alt text-cyan-400"></i>
                    </div>
                    <div class="flex-1 min-w-0">
                      <div class="text-xs font-bold text-white">发布到发现页</div>
                      <div class="text-[10px] text-gray-500 mt-0.5">让更多人听到你的播客</div>
                    </div>
                  </div>
                  <button @click="publishProject" :disabled="publishing"
                          class="w-full px-4 py-2.5 rounded-xl text-[11px] font-bold cursor-pointer transition-all flex items-center justify-center gap-1.5"
                          :class="publishing ? 'bg-gray-700 text-gray-400' : 'bg-gradient-to-r from-cyan-500 to-emerald-500 text-white hover:shadow-[0_4px_15px_rgba(6,182,212,0.3)] active:scale-[0.98]'">
                    <i v-if="publishing" class="fas fa-circle-notch fa-spin"></i>
                    <i v-else class="fas fa-rocket"></i>
                    {{ publishing ? '发布中...' : '上架到发现页' }}
                  </button>
                </div>
              </template>
            </div>

          </template>

          <!-- ═══ 情感电台 ═══ -->
          <template v-else-if="project?.typeCode === 'radio'">
            <div class="bg-[#1a1a1c] border border-white/5 rounded-2xl p-4">
              <h3 class="text-xs font-bold text-white mb-3"><i class="fas fa-moon text-indigo-400 mr-1.5"></i> 独白内容</h3>
              <textarea v-model="userInput" rows="4" :disabled="generating"
                        placeholder="写下你想表达的情感，例如：给深夜失眠的你，一段温暖的睡前絮语..."
                        class="w-full bg-black/30 border border-white/5 rounded-xl px-3.5 py-3 text-sm text-white outline-none focus:border-indigo-500/50 resize-none transition-all"></textarea>
            </div>
            <div class="bg-[#1a1a1c] border border-white/5 rounded-2xl p-4">
              <h3 class="text-xs font-bold text-white mb-3"><i class="fas fa-heart text-rose-400 mr-1.5"></i> 情绪氛围</h3>
              <div class="grid grid-cols-2 gap-2">
                <button v-for="mood in radioMoods" :key="mood.id" @click="selectedMood = mood"
                        class="rounded-xl p-3 text-left cursor-pointer transition-all"
                        :class="selectedMood?.id === mood.id ? 'bg-gradient-to-br ' + mood.gradient + ' ring-1 ring-white/20' : 'bg-white/[0.03] border border-white/5 hover:bg-white/[0.06]'">
                  <span class="text-lg">{{ mood.icon }}</span>
                  <div class="text-[11px] font-bold mt-1" :class="selectedMood?.id === mood.id ? 'text-white' : 'text-gray-300'">{{ mood.name }}</div>
                  <div class="text-[9px]" :class="selectedMood?.id === mood.id ? 'text-white/60' : 'text-gray-600'">{{ mood.desc }}</div>
                </button>
              </div>
            </div>
            <div class="bg-[#1a1a1c] border border-white/5 rounded-2xl p-4">
              <h3 class="text-xs font-bold text-white mb-3"><i class="fas fa-microphone-alt text-indigo-400 mr-1.5"></i> 演播音色</h3>
              <button @click="showVoicePicker = true" class="w-full flex items-center justify-between px-4 py-3 rounded-xl bg-white/5 border border-white/10 hover:border-indigo-500/30 cursor-pointer transition-all">
                <div class="flex items-center gap-2"><i class="fas fa-microphone-alt text-indigo-400"></i><span class="text-sm text-white">{{ currentVoiceName }}</span></div>
                <i class="fas fa-chevron-right text-gray-500 text-xs"></i>
              </button>
            </div>
          </template>

          <!-- ═══ 知识讲解 ═══ -->
          <template v-else-if="project?.typeCode === 'lecture'">
            <div class="bg-[#1a1a1c] border border-white/5 rounded-2xl p-4">
              <h3 class="text-xs font-bold text-white mb-3"><i class="fas fa-chalkboard-teacher text-green-400 mr-1.5"></i> 讲解主题</h3>
              <textarea v-model="userInput" rows="4" :disabled="generating"
                        placeholder="输入想讲解的知识点，例如：用通俗的方式讲解量子纠缠..."
                        class="w-full bg-black/30 border border-white/5 rounded-xl px-3.5 py-3 text-sm text-white outline-none focus:border-green-500/50 resize-none transition-all"></textarea>
            </div>
            <div class="bg-[#1a1a1c] border border-white/5 rounded-2xl p-4">
              <h3 class="text-xs font-bold text-white mb-3"><i class="fas fa-signal text-green-400 mr-1.5"></i> 难度等级</h3>
              <div class="flex gap-2">
                <button v-for="lv in lectureLevels" :key="lv.id" @click="selectedLevel = lv"
                        class="flex-1 rounded-xl p-3 text-center cursor-pointer transition-all"
                        :class="selectedLevel?.id === lv.id ? 'bg-green-500/15 border border-green-500/30' : 'bg-white/[0.03] border border-white/5 hover:bg-white/[0.06]'">
                  <div class="text-xl mb-1">{{ lv.icon }}</div>
                  <div class="text-[10px] font-bold" :class="selectedLevel?.id === lv.id ? 'text-green-400' : 'text-gray-400'">{{ lv.name }}</div>
                  <div class="text-[8px] mt-0.5" :class="selectedLevel?.id === lv.id ? 'text-green-400/60' : 'text-gray-600'">{{ lv.desc }}</div>
                </button>
              </div>
            </div>
            <div class="bg-[#1a1a1c] border border-white/5 rounded-2xl p-4">
              <h3 class="text-xs font-bold text-white mb-3"><i class="fas fa-microphone-alt text-green-400 mr-1.5"></i> 讲解音色</h3>
              <button @click="showVoicePicker = true" class="w-full flex items-center justify-between px-4 py-3 rounded-xl bg-white/5 border border-white/10 hover:border-green-500/30 cursor-pointer transition-all">
                <div class="flex items-center gap-2"><i class="fas fa-microphone-alt text-green-400"></i><span class="text-sm text-white">{{ currentVoiceName }}</span></div>
                <i class="fas fa-chevron-right text-gray-500 text-xs"></i>
              </button>
            </div>
          </template>

          <!-- ═══ 带货文案 ═══ -->
          <template v-else-if="project?.typeCode === 'ad'">
            <div class="bg-[#1a1a1c] border border-white/5 rounded-2xl p-4">
              <h3 class="text-xs font-bold text-white mb-3"><i class="fas fa-bullhorn text-orange-400 mr-1.5"></i> 产品信息</h3>
              <input v-model="adProductName" placeholder="产品名称，如：XX降噪耳机"
                     class="w-full bg-black/30 border border-white/5 rounded-xl px-3.5 py-2.5 text-sm text-white outline-none focus:border-orange-500/50 mb-3 transition-all" />
              <textarea v-model="userInput" rows="3" :disabled="generating"
                        placeholder="描述产品卖点和使用场景，例如：健身房和通勤场景的使用体验..."
                        class="w-full bg-black/30 border border-white/5 rounded-xl px-3.5 py-3 text-sm text-white outline-none focus:border-orange-500/50 resize-none transition-all"></textarea>
            </div>
            <div class="bg-[#1a1a1c] border border-white/5 rounded-2xl p-4">
              <h3 class="text-xs font-bold text-white mb-3"><i class="fas fa-palette text-orange-400 mr-1.5"></i> 文案风格</h3>
              <div class="grid grid-cols-2 gap-2">
                <button v-for="st in adStyles" :key="st.id" @click="selectedAdStyle = st"
                        class="rounded-xl p-3 text-center cursor-pointer transition-all"
                        :class="selectedAdStyle?.id === st.id ? 'bg-orange-500/15 border border-orange-500/30' : 'bg-white/[0.03] border border-white/5 hover:bg-white/[0.06]'">
                  <div class="text-xl mb-1">{{ st.icon }}</div>
                  <div class="text-[10px] font-bold" :class="selectedAdStyle?.id === st.id ? 'text-orange-400' : 'text-gray-400'">{{ st.name }}</div>
                  <div class="text-[8px] mt-0.5" :class="selectedAdStyle?.id === st.id ? 'text-orange-400/60' : 'text-gray-600'">{{ st.desc }}</div>
                </button>
              </div>
            </div>
            <div class="bg-[#1a1a1c] border border-white/5 rounded-2xl p-4">
              <h3 class="text-xs font-bold text-white mb-3"><i class="fas fa-microphone-alt text-orange-400 mr-1.5"></i> 播音音色</h3>
              <button @click="showVoicePicker = true" class="w-full flex items-center justify-between px-4 py-3 rounded-xl bg-white/5 border border-white/10 hover:border-orange-500/30 cursor-pointer transition-all">
                <div class="flex items-center gap-2"><i class="fas fa-microphone-alt text-orange-400"></i><span class="text-sm text-white">{{ currentVoiceName }}</span></div>
                <i class="fas fa-chevron-right text-gray-500 text-xs"></i>
              </button>
            </div>
          </template>

          <!-- ═══ 有声绘本 ═══ -->
          <template v-else-if="project?.typeCode === 'picture_book'">
            <div class="bg-[#1a1a1c] border border-white/5 rounded-2xl p-4">
              <h3 class="text-xs font-bold text-white mb-3"><i class="fas fa-book-reader text-pink-400 mr-1.5"></i> 故事内容</h3>
              <textarea v-model="userInput" rows="4" :disabled="generating"
                        placeholder="描述你想创作的儿童故事，例如：小兔子不愿意分享胡萝卜..."
                        class="w-full bg-black/30 border border-white/5 rounded-xl px-3.5 py-3 text-sm text-white outline-none focus:border-pink-500/50 resize-none transition-all"></textarea>
            </div>
            <div class="bg-[#1a1a1c] border border-white/5 rounded-2xl p-4">
              <h3 class="text-xs font-bold text-white mb-3"><i class="fas fa-child text-pink-400 mr-1.5"></i> 适龄范围</h3>
              <div class="flex gap-2">
                <button v-for="age in picBookAges" :key="age.id" @click="selectedAge = age"
                        class="flex-1 rounded-xl p-3 text-center cursor-pointer transition-all"
                        :class="selectedAge?.id === age.id ? 'bg-pink-500/15 border border-pink-500/30' : 'bg-white/[0.03] border border-white/5 hover:bg-white/[0.06]'">
                  <div class="text-xl mb-1">{{ age.icon }}</div>
                  <div class="text-[10px] font-bold" :class="selectedAge?.id === age.id ? 'text-pink-400' : 'text-gray-400'">{{ age.name }}</div>
                  <div class="text-[8px] mt-0.5" :class="selectedAge?.id === age.id ? 'text-pink-400/60' : 'text-gray-600'">{{ age.desc }}</div>
                </button>
              </div>
            </div>
            <div class="bg-[#1a1a1c] border border-white/5 rounded-2xl p-4">
              <h3 class="text-xs font-bold text-white mb-3"><i class="fas fa-microphone-alt text-pink-400 mr-1.5"></i> 演读音色</h3>
              <button @click="showVoicePicker = true" class="w-full flex items-center justify-between px-4 py-3 rounded-xl bg-white/5 border border-white/10 hover:border-pink-500/30 cursor-pointer transition-all">
                <div class="flex items-center gap-2"><i class="fas fa-microphone-alt text-pink-400"></i><span class="text-sm text-white">{{ currentVoiceName }}</span></div>
                <i class="fas fa-chevron-right text-gray-500 text-xs"></i>
              </button>
            </div>
          </template>

          <!-- ═══ 新闻播报 ═══ -->
          <template v-else-if="project?.typeCode === 'news'">
            <div class="bg-[#1a1a1c] border border-white/5 rounded-2xl p-4">
              <h3 class="text-xs font-bold text-white mb-3"><i class="fas fa-newspaper text-blue-400 mr-1.5"></i> 新闻内容</h3>
              <textarea v-model="userInput" rows="4" :disabled="generating"
                        placeholder="输入新闻主题或粘贴新闻内容，例如：最新AI芯片突破意味着什么..."
                        class="w-full bg-black/30 border border-white/5 rounded-xl px-3.5 py-3 text-sm text-white outline-none focus:border-blue-500/50 resize-none transition-all"></textarea>
            </div>
            <div class="bg-[#1a1a1c] border border-white/5 rounded-2xl p-4">
              <h3 class="text-xs font-bold text-white mb-3"><i class="fas fa-broadcast-tower text-blue-400 mr-1.5"></i> 播报风格</h3>
              <div class="grid grid-cols-2 gap-2">
                <button v-for="ns in newsStyles" :key="ns.id" @click="selectedNewsStyle = ns"
                        class="rounded-xl p-3 text-center cursor-pointer transition-all"
                        :class="selectedNewsStyle?.id === ns.id ? 'bg-blue-500/15 border border-blue-500/30' : 'bg-white/[0.03] border border-white/5 hover:bg-white/[0.06]'">
                  <div class="text-xl mb-1">{{ ns.icon }}</div>
                  <div class="text-[10px] font-bold" :class="selectedNewsStyle?.id === ns.id ? 'text-blue-400' : 'text-gray-400'">{{ ns.name }}</div>
                  <div class="text-[8px] mt-0.5" :class="selectedNewsStyle?.id === ns.id ? 'text-blue-400/60' : 'text-gray-600'">{{ ns.desc }}</div>
                </button>
              </div>
            </div>
            <div class="bg-[#1a1a1c] border border-white/5 rounded-2xl p-4">
              <h3 class="text-xs font-bold text-white mb-3"><i class="fas fa-microphone-alt text-blue-400 mr-1.5"></i> 主播音色</h3>
              <button @click="showVoicePicker = true" class="w-full flex items-center justify-between px-4 py-3 rounded-xl bg-white/5 border border-white/10 hover:border-blue-500/30 cursor-pointer transition-all">
                <div class="flex items-center gap-2"><i class="fas fa-microphone-alt text-blue-400"></i><span class="text-sm text-white">{{ currentVoiceName }}</span></div>
                <i class="fas fa-chevron-right text-gray-500 text-xs"></i>
              </button>
            </div>
          </template>

          <!-- ═══ 生成按钮（通用，播客除外） ═══ -->
          <button v-if="project?.typeCode !== 'podcast'" @click="generate" :disabled="generating || !userInput.trim()"
                  class="w-full py-3.5 rounded-2xl text-sm font-bold cursor-pointer transition-all flex items-center justify-center gap-2 disabled:opacity-30 disabled:cursor-not-allowed"
                  :class="generating ? 'bg-gray-700 text-gray-400' : (mc.btnGradient || 'bg-gradient-to-r from-[#FF9500] to-[#FF6B00] text-black')">
            <i v-if="generating" class="fas fa-circle-notch fa-spin"></i>
            <i v-else class="fas fa-sparkles"></i>
            {{ generating ? genPhaseText : mc.generateBtn }}
          </button>

          <!-- ═══ 流式输出 / 结果（播客除外） ═══ -->
          <div v-if="project?.typeCode !== 'podcast' && (generating || streamContent)" class="bg-[#1a1a1c] border border-white/5 rounded-2xl p-4">
            <h3 class="text-[10px] font-bold text-gray-400 mb-2 flex items-center gap-1.5">
              <i class="fas fa-robot" :class="mc.iconColor || 'text-[#FF9500]'"></i>
              {{ generating ? genPhaseText : '✅ 生成完成' }}
              <span v-if="generating" class="animate-pulse" :class="mc.iconColor || 'text-[#FF9500]'">▊</span>
            </h3>
            <div v-if="generating" class="flex items-center gap-3 mb-3">
              <span class="text-[10px] text-gray-500">⏱️ {{ elapsedTime }}s</span>
              <span class="text-[10px] font-bold" :class="mc.iconColor || 'text-[#FF9500]'">{{ streamContent.length }} 字</span>
              <div class="flex-1 h-1.5 bg-white/5 rounded-full overflow-hidden">
                <div class="h-full bg-gradient-to-r rounded-full transition-all duration-300" :class="mc.progressGradient || 'from-[#FF9500] to-[#FF6B00]'" :style="{ width: genProgress + '%' }"></div>
              </div>
            </div>
            <div v-if="!streamContent && generating" class="space-y-2 animate-pulse">
              <div class="h-3 bg-white/5 rounded w-4/5"></div><div class="h-3 bg-white/5 rounded w-3/5"></div>
            </div>
            <div v-else class="text-sm text-gray-200 whitespace-pre-wrap leading-relaxed max-h-[50vh] overflow-y-auto hide-scrollbar" ref="streamContentRef">{{ streamContent }}</div>
            <div v-if="justFinished" class="flex gap-2 mt-3">
              <button @click="justFinished = false; userInput = ''; streamContent = ''" class="flex-1 py-2 rounded-lg bg-white/5 text-gray-300 text-xs font-bold cursor-pointer hover:bg-white/10 transition-all">🔄 重新生成</button>
              <button @click="justFinished = false; streamContent = ''" class="flex-1 py-2 rounded-lg bg-blue-500/10 text-blue-400 text-xs font-bold cursor-pointer hover:bg-blue-500/20 transition-all">🎧 去配音</button>
            </div>
          </div>




          <!-- ═══ 已有内容（增强版，播客除外）═══ -->
          <div v-if="project?.typeCode !== 'podcast' && sections.length > 0 && !generating && !streamContent" class="space-y-3">
            <div v-for="(s, i) in sections" :key="s.id" class="bg-[#1a1a1c] border rounded-2xl overflow-hidden transition-all" :class="s.audioUrl ? 'border-green-500/20' : 'border-white/5'">
              <!-- 卡片头 -->
              <div class="flex items-center gap-2 px-4 py-3 cursor-pointer" @click="toggleSingleExpand(s.id)">
                <span v-if="['radio','lecture','ad','picture_book','news'].includes(project?.typeCode)" class="text-xs font-bold" :class="mc.iconColor || 'text-[#FF9500]'">{{ mc.icon }} 作品内容</span>
                <span v-else class="text-xs font-bold" :class="mc.iconColor || 'text-[#FF9500]'">{{ mc.icon }} 第 {{ i + 1 }} {{ mc.unit }}</span>
                <span class="text-[9px] text-gray-500">{{ s.content?.length || 0 }} 字</span>
                <span v-if="s.audioUrl" class="text-[8px] text-green-400 ml-auto"><i class="fas fa-check-circle"></i> 已配音</span>
                <span v-else class="text-[8px] text-gray-600 ml-auto">待配音</span>
                <i class="fas text-gray-500 text-[10px] transition-transform" :class="singleExpandedMap[s.id] ? 'fa-chevron-up' : 'fa-chevron-down'"></i>
              </div>
              <!-- 展开内容 -->
              <div v-if="singleExpandedMap[s.id]" class="px-4 pb-4">
                <div class="text-sm text-gray-200 whitespace-pre-wrap leading-relaxed overflow-y-auto hide-scrollbar bg-black/20 rounded-xl p-3 mb-3" :class="['radio','lecture','ad','picture_book','news'].includes(project?.typeCode) ? 'max-h-[50vh]' : 'max-h-[20vh]'">{{ s.content }}</div>
                <!-- 操作按钮组 -->
                <div class="flex gap-2">
                  <button @click="copyContent(s.content)" class="flex-1 py-2 rounded-lg bg-white/5 text-gray-300 text-[10px] font-bold cursor-pointer hover:bg-white/10 transition-all flex items-center justify-center gap-1">
                    <i class="fas fa-copy"></i> 复制文案
                  </button>
                  <button v-if="!s.audioUrl" @click="activeSection = s; synthesizeSection(s)" :disabled="synthesizingSection"
                          class="flex-1 py-2 rounded-lg text-[10px] font-bold cursor-pointer transition-all flex items-center justify-center gap-1"
                          :class="synthesizingSection ? 'bg-gray-700 text-gray-500' : 'bg-gradient-to-r ' + (mc.progressGradient || 'from-[#FF9500] to-[#FF6B00]') + ' text-white'">
                    <i v-if="synthesizingSection" class="fas fa-circle-notch fa-spin"></i>
                    <i v-else class="fas fa-volume-up"></i>
                    {{ synthesizingSection ? '合成中...' : '🔊 合成语音' }}
                  </button>
                  <button v-if="s.audioUrl" @click="playAudio(s)"
                          class="flex-1 py-2 rounded-lg bg-green-500/10 text-green-400 text-[10px] font-bold cursor-pointer hover:bg-green-500/20 transition-all flex items-center justify-center gap-1">
                    <i class="fas fa-play"></i> ▶ 播放
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- ═══ 底部发布区（配音完成后显示）═══ -->
          <div v-if="project?.typeCode !== 'podcast' && project?.typeCode !== 'drama' && hasSynthesized && !generating && !streamContent" class="bg-gradient-to-br from-[#1a1a2e] to-[#16213e] border border-purple-500/20 rounded-2xl p-5">
            <div class="flex items-center gap-2 mb-3">
              <i class="fas fa-rocket text-purple-400"></i>
              <span class="text-sm font-bold text-white">作品已就绪</span>
              <span v-if="project?.status === 'completed'" class="ml-auto text-[10px] text-green-400 font-bold"><i class="fas fa-check-circle mr-1"></i>已发布</span>
            </div>
            <div class="flex items-center gap-4 mb-4 text-[10px] text-gray-400">
              <span v-if="['radio','lecture','ad','picture_book','news'].includes(project?.typeCode)">✅ 内容已生成</span>
              <span v-else>📝 {{ sections.length }} 段内容</span>
              <span v-if="['radio','lecture','ad','picture_book','news'].includes(project?.typeCode)">🎧 配音完成</span>
              <span v-else>🎧 {{ sections.filter(s => s.audioUrl).length }} 段已配音</span>
              <span>📊 {{ sections.reduce((sum, s) => sum + (s.content?.length || 0), 0) }} 字</span>
            </div>
            <div class="flex gap-2">
              <button v-if="sections.some(s => s.audioUrl)" @click="playAudio(sections.find(s => s.audioUrl))"
                      class="flex-1 py-2.5 rounded-xl bg-white/5 text-gray-300 text-xs font-bold cursor-pointer hover:bg-white/10 transition-all flex items-center justify-center gap-1.5">
                <i class="fas fa-play"></i> 播放试听
              </button>
              <button v-if="project?.status !== 'completed'" @click="publishProject" :disabled="publishing"
                      class="flex-1 py-2.5 rounded-xl text-xs font-bold cursor-pointer transition-all flex items-center justify-center gap-1.5"
                      :class="publishing ? 'bg-gray-700 text-gray-400' : 'bg-gradient-to-r from-purple-600 to-indigo-600 text-white hover:from-purple-500 hover:to-indigo-500'">
                <i v-if="publishing" class="fas fa-circle-notch fa-spin"></i>
                <i v-else class="fas fa-rocket"></i>
                {{ publishing ? '发布中...' : '🚀 发布到发现页' }}
              </button>
              <div v-else class="flex-1 py-2.5 rounded-xl bg-green-500/10 text-green-400 text-xs font-bold flex items-center justify-center gap-1.5">
                <i class="fas fa-check-circle"></i> ✅ 已发布到发现页
              </div>
            </div>
          </div>
        </div>
      </template>

    </div>

    <!-- 音色选择器弹窗 -->
    <VoiceSelector
      v-model:visible="showVoicePicker"
      :engine="selectedEngine"
      :initialVoiceId="activeSection?.voiceId || selectedVoice.voiceId"
      @select="onVoiceSelect"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { studioApi } from '../api/studio'
import { ttsApi } from '../api/tts'
import request from '../api/request'
import { useToastStore } from '../stores/toast'
import { usePlayerStore } from '../stores/player'
import { useAuthStore } from '../stores/auth'
import VoiceSelector from '../components/VoiceSelector.vue'

const route = useRoute()
const router = useRouter()
const toastStore = useToastStore()
const playerStore = usePlayerStore()
const authStore = useAuthStore()

const projectId = ref(route.params.id)
const project = ref(null)
const sections = ref([])
const activeSection = ref(null)
const loading = ref(true)
const generating = ref(false)
const editableInspiration = ref('')
const userInput = ref('')
const chapterTitle = ref('')
const streamContent = ref('')
const activeTab = ref('create')

const showVoicePicker = ref(false)
const selectedVoice = ref({ voiceId: 'zh_female_vv_uranus_bigtts', name: 'vivi 2.0' })
const synthesizingSection = ref(false)
const synthesizingAll = ref(false)
const synthProgress = ref(0)
const synthChunkProgress = ref(0)
const synthChunkTotal = ref(0)
const publishing = ref(false)
const selectedEngine = ref('tts-2.0')
const streamOutputRef = ref(null)
const emotionInstruction = ref('')
const autoTagging = ref(false)

// 章节卡片 & 删除弹窗
const expandedSections = reactive(new Set())
const showDeleteModal = ref(false)
const deletingSection = ref(null)

// AI 生成进度 — 真实字数驱动（终极优化）
const genStartTime = ref(0)
const elapsedTime = ref(0)
let elapsedTimer = null
const justFinished = ref(false)
const showFullReader = ref(false)
const chapterReaderOpen = ref(-1)

// ═══ 广播剧分角色配音 ═══
const dramaCharacters = ref([])       // 解析出的角色列表
const dramaLines = ref([])            // 解析出的台词行 [{character, dialogue}]
const characterVoiceMap = ref({})     // 角色→音色ID映射
const characterVoiceNames = ref({})   // 角色→音色名称映射

// 角色稳定色彩映射（同一角色始终同一颜色）
const charColorPalette = ['#93c5fd', '#f9a8d4', '#c4b5fd', '#67e8f9', '#fca5a5', '#86efac', '#fcd34d', '#a5b4fc']
const charColorCache = {}
const getCharColor = (name) => {
  if (!name) return charColorPalette[0]
  if (charColorCache[name]) return charColorCache[name]
  const idx = Object.keys(charColorCache).length % charColorPalette.length
  charColorCache[name] = charColorPalette[idx]
  return charColorCache[name]
}
const characterEmotionMap = ref({})   // 角色→情感语气映射
const dramaVoicePickerFor = ref('')   // 当前正在为哪个角色选音色
const dramaParsingScript = ref(false) // 解析中
const isDramaType = computed(() => project.value?.typeCode === 'drama')

// ═══ 广播剧对话模式 ═══
const dialogueMode = ref('duo')        // "duo" / "trio" / "ensemble"
const dramaRoles = ref([])             // 角色列表 [{name, desc, voiceType}]
const dramaGenerating = ref(false)     // 生成中
const dramaStreamContent = ref('')     // 流式内容
const dramaFinished = ref(false)       // 生成完成
const dramaShowFull = ref(false)       // 展开全文阅读
const dramaSettingsChanged = ref(false) // 设定是否有变更（用于节省 token）
const dramaElapsed = ref(0)            // 耗时

// 计算属性：是否可以重新生成
const dramaCanRegenerate = computed(() => !dramaFinished.value || dramaSettingsChanged.value)
const dramaButtonLabel = computed(() => {
  if (dramaGenerating.value) return `🎬 AI 正在创作剧本... ${dramaElapsed.value}s`
  if (dramaFinished.value && !dramaSettingsChanged.value) return '✅ 已生成（修改设定后可重新生成）'
  return '🎬 一键生成对话剧本'
})

const dialogueModes = [
  { id: 'duo', icon: '👫', name: '双人对话', chars: 2,
    desc: '两人交锋 · 攻守交替', 
    example: '霸总 vs 女主、闺蜜撕逼、情侣对峙' },
  { id: 'trio', icon: '👥', name: '三人对话', chars: 3,
    desc: '三角博弈 · 信息差反转', 
    example: '正宫+小三+渣男、三角恋修罗场' },
  { id: 'ensemble', icon: '🎭', name: '群像对话', chars: 5,
    desc: '多人群戏 · 阵营博弈', 
    example: '后宫争宠、职场站队、家族会议' }
]

// 初始化角色列表
const initDramaRoles = (mode) => {
  const count = dialogueModes.find(m => m.id === mode)?.chars || 2
  const defaultNames = {
    duo: [
      { name: '男主', desc: '冷傲霸道、强势占有', voiceType: '浑厚男声' },
      { name: '女主', desc: '外柔内刚、聪慧独立', voiceType: '温柔女声' }
    ],
    trio: [
      { name: '男主', desc: '冷傲霸道、强势占有', voiceType: '浑厚男声' },
      { name: '女主', desc: '外柔内刚、聪慧独立', voiceType: '温柔女声' },
      { name: '反派', desc: '伪善腹黑、心机深沉', voiceType: '冷艳女声' }
    ],
    ensemble: [
      { name: '男主', desc: '冷傲霸道', voiceType: '浑厚男声' },
      { name: '女主', desc: '聪慧独立', voiceType: '温柔女声' },
      { name: '反派', desc: '伪善腹黑', voiceType: '冷艳女声' },
      { name: '配角A', desc: '忠心耿耿', voiceType: '沉稳旁白' },
      { name: '配角B', desc: '活泼可爱', voiceType: '活泼少年' }
    ]
  }
  dramaRoles.value = defaultNames[mode] || defaultNames.duo
}

// 切换模式时自动初始化角色
watch(dialogueMode, (mode) => initDramaRoles(mode), { immediate: true })

// 监听设定变化 → 允许重新生成
watch(dialogueMode, () => { if (dramaFinished.value) dramaSettingsChanged.value = true })
watch(dramaRoles, () => { if (dramaFinished.value) dramaSettingsChanged.value = true }, { deep: true })

/** 广播剧一键生成完整对话剧本 */
const generateDramaScript = async () => {
  const genre = activeGenres.value.find(g => g.id === stylePreference.value)
  if (!genre) { toastStore.show('请先选择题材方向'); return }
  if (!dramaRoles.value.length) { toastStore.show('请完善角色设定'); return }

  dramaGenerating.value = true
  dramaStreamContent.value = ''
  dramaFinished.value = false
  dramaElapsed.value = 0
  const startTime = Date.now()
  const timer = setInterval(() => { dramaElapsed.value = Math.round((Date.now() - startTime) / 1000) }, 500)

  try {
    const characters = dramaRoles.value.map(r => `${r.name}（${r.desc}，声线：${r.voiceType}）`).join('\n')
    const resp = await studioApi.generateDrama(projectId.value, {
      dialogueMode: dialogueMode.value,
      genre: genre.id,
      genreTips: genre.tips || '',
      characters,
      inspiration: editableInspiration.value || project.value?.inspiration || ''
    })
    const reader = resp.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    while (true) {
      const { value, done } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() // 保留未完成的行
      for (const line of lines) {
        if (line.startsWith('data:')) {
          dramaStreamContent.value += line.substring(5)
        }
      }
    }
    // 处理缓冲区残留
    if (buffer.startsWith('data:')) dramaStreamContent.value += buffer.substring(5)
    dramaFinished.value = true
    dramaShowFull.value = true // 自动展开全文阅读
    dramaSettingsChanged.value = false // 重置变更标记
    // 刷新 sections 以同步进度
    await loadSections()
    await loadProject()
    if (sections.value.length > 0) {
      activeSection.value = sections.value[0]
    }
    toastStore.show(`🎬 剧本生成完成！${dramaStreamContent.value.length}字 · ${dramaElapsed.value}s`)
  } catch (e) {
    toastStore.show('生成失败: ' + (e.message || '网络错误'))
  } finally {
    clearInterval(timer)
    dramaGenerating.value = false
  }
}

// 广播剧配音自动解析：切换到配音 Tab 时自动解析角色
watch([activeTab, () => activeSection.value?.id], async ([tab, secId]) => {
  if (tab === 'voice' && secId && isDramaType.value) {
    // 强制 TTS 2.0（广播剧必须情感配音）
    if (selectedEngine.value !== 'tts-2.0') selectedEngine.value = 'tts-2.0'
    // 自动解析剧本
    if (dramaCharacters.value.length === 0) {
      await parseDramaScript(activeSection.value)
    }
  }
})

/** 解析广播剧剧本 → 提取角色列表 */
const parseDramaScript = async (section) => {
  if (!section?.id) return
  dramaParsingScript.value = true
  try {
    const data = await studioApi.parseScript(section.id)
    dramaCharacters.value = data.characters || []
    dramaLines.value = data.lines || []
    // 初始化默认音色分配
    for (const char of dramaCharacters.value) {
      if (!characterVoiceMap.value[char]) {
        characterVoiceMap.value[char] = selectedVoice.value.voiceId
        characterVoiceNames.value[char] = selectedVoice.value.name
      }
      if (!characterEmotionMap.value[char]) {
        characterEmotionMap.value[char] = ''
      }
    }
  } catch (e) {
    toastStore.show('剧本解析失败: ' + (e.message || '网络错误'))
  } finally {
    dramaParsingScript.value = false
  }
}

/** 分角色合成广播剧 */
const synthesizeDrama = async () => {
  if (!activeSection.value?.content) { toastStore.show('段落没有内容'); return }
  if (dramaLines.value.length === 0) {
    await parseDramaScript(activeSection.value)
    if (dramaLines.value.length === 0) { toastStore.show('未解析到对白'); return }
  }
  synthesizingSection.value = true
  synthChunkTotal.value = dramaLines.value.length
  synthChunkProgress.value = 0
  try {
    const audioUrls = []
    for (let i = 0; i < dramaLines.value.length; i++) {
      const line = dramaLines.value[i]
      const voiceId = characterVoiceMap.value[line.character] || selectedVoice.value.voiceId
      const emotion = characterEmotionMap.value[line.character] || ''
      synthChunkProgress.value = i + 1
      const url = await synthesizeChunkWithEmotion(line.dialogue, voiceId, emotion)
      if (url) {
        audioUrls.push(url)
        if (i === 0) {
          playerStore.play({ title: activeSection.value.title || 'AI短剧合成中...', author: project.value?.title || '创作工作台', url })
          if (dramaLines.value.length > 1) toastStore.show(`✅ 第1句已就绪，边听边合成剩余 ${dramaLines.value.length - 1} 句...`)
        } else {
          playerStore.enqueue(url)
        }
      }
    }
    if (audioUrls.length > 0) {
      let audioUrl
      if (audioUrls.length > 1) {
        audioUrl = await studioApi.concatAudio(audioUrls)
      } else {
        audioUrl = audioUrls[0]
      }
      activeSection.value.audioUrl = audioUrl
      await studioApi.saveSection(activeSection.value)
      toastStore.show(`🎭 分角色配音完成！${dramaCharacters.value.length} 个角色、${audioUrls.length} 句台词`)
    }
  } catch (e) {
    toastStore.show('配音失败: ' + (e.message || '网络错误'))
  } finally {
    synthesizingSection.value = false
  }
}

/** TTS 合成（支持 per-character 情感覆盖） */
const synthesizeChunkWithEmotion = async (text, voiceId, emotion) => {
  const cleanText = text
    .replace(/^##\s*.+$/gm, '')
    .replace(/^\[.+\]$/gm, '')
    .replace(/^(.{1,10})[：:]\s*/gm, '')
    .replace(/[（(].+?[）)]/g, '')
    .replace(/\*\*(.+?)\*\*/g, '$1')
    .trim()
  if (!cleanText) return null
  const useEmotion = emotion || emotionInstruction.value
  if (selectedEngine.value === 'tts-2.0' || useEmotion) {
    const finalText = useEmotion
      ? `[#用${useEmotion}的语气] ${cleanText}`
      : cleanText
    const data = await request.post('/tts/v2/synthesize', {
      text: finalText,
      voiceType: voiceId,
      mode: useEmotion ? 'voice_command' : 'default',
      userKey: authStore.user?.id?.toString() || 'anonymous'
    })
    return data?.audioUrl
  } else {
    const res = await ttsApi.synthesizeShort({ text: cleanText, voiceId })
    return res?.audioUrl
  }
}

/** 复制全部小说正文到剪贴板 */
const copyFullNovel = async () => {
  const fullText = chapterOutlines.value.map((co, i) => {
    const sec = sections.value.find(s => s.sectionIndex === i)
    return `第${i + 1}章\n\n${sec?.content || '（未生成）'}`
  }).join('\n\n' + '─'.repeat(30) + '\n\n')
  
  const title = project.value?.title || '我的小说'
  const finalText = `${title}\n${'═'.repeat(30)}\n\n${fullText}`
  
  try {
    await navigator.clipboard.writeText(finalText)
    toastStore.show('📋 全文已复制到剪贴板')
  } catch {
    toastStore.show('复制失败，请手动复制')
  }
}

/** 判断章节内容是否为有效小说正文（排除 JSON/代码块等脏数据） */
const isValidChapterContent = (content) => {
  if (!content || content.length < 100) return false
  const trimmed = content.trim()
  // 排除 JSON 对象/数组
  if (trimmed.startsWith('{') || trimmed.startsWith('[{') || trimmed.startsWith('[')) return false
  // 排除 markdown 代码块
  if (trimmed.startsWith('```')) return false
  // 排除明显的 JSON 字段
  if (trimmed.includes('"synopsis"') || trimmed.includes('"plot"')) return false
  return true
}

/** 获取章节的有效正文 section */
const getValidSection = (chapterIdx) => {
  return sections.value.find(s => s.sectionIndex === chapterIdx && isValidChapterContent(s.content))
}

/** 完结发布：标记项目为已完成 */
const publishNovel = async () => {
  try {
    await studioApi.updateProject(projectId.value, { status: 'completed' })
    await loadProject()
    toastStore.show('🎉 小说已完结发布！')
  } catch (e) {
    toastStore.show('发布失败: ' + (e.message || '网络错误'))
  }
}
const genProgress = computed(() => {
  if (!generating.value) return 0
  if (!streamContent.value) return 5
  const expected = mc.value?.expectedChars || 1500
  return Math.min(Math.round(streamContent.value.length / expected * 100), 99)
})
const genPhaseText = computed(() => {
  if (!generating.value) return ''
  const p = mc.value?.phaseNames || ['构思', '落笔', '展开', '推进', '即将完成']
  if (!streamContent.value) return `🔍 ${p[0]}中...`
  if (streamContent.value.length < 200) return `✍️ ${p[1]}中...`
  if (streamContent.value.length < 800) return `📖 ${p[2]}中...`
  if (streamContent.value.length < 1200) return `🎭 ${p[3]}中...`
  return `✨ ${p[4]}...`
})

/** 清理后的流式内容预览 — 去除 ##标题、[音效]、(动作) 等非朗读元素 */
const cleanStreamContent = computed(() => {
  if (!streamContent.value) return ''
  return streamContent.value
    .replace(/^##\s*.+$/gm, '')        // 移除 ##标题
    .replace(/^\[.+\]$/gm, '')         // 移除 [音效] 行
    .replace(/[（(].+?[）)]/g, '')      // 移除 (动作描写)
    .replace(/\*\*(.+?)\*\*/g, '$1')   // 移除 **粗体**
    .replace(/\n{3,}/g, '\n\n')        // 合并多余空行
    .trim()
})

// 动态预设标签（初始值，后续从 Prompt Library 加载）
const emotionPresets = ref(['温柔感伤', '愤怒爆发', '撇嘴撩人', '深情呆滞', '兴奮激动', '低沉叙事', '神秘悬疑', '撩气暗示'])
const loadEmotionPresets = async () => {
  try {
    const data = await request.get('/tts/v2/prompt-library/tree')
    if (data && Array.isArray(data)) {
      const tags = []
      for (const cat of data) {
        if (cat.roles) {
          for (const role of cat.roles) {
            if (role.name) tags.push(role.name)
          }
        }
      }
      if (tags.length > 0) emotionPresets.value = tags
    }
  } catch (e) { /* 静默失败，保持默认预设 */ }
}

const hasSynthesized = computed(() => sections.value.some(s => s.audioUrl))
const activeSectionIdx = computed(() => sections.value.findIndex(s => s.id === activeSection.value?.id))

const tabs = [
  { key: 'create', label: '创作', icon: 'fas fa-wand-magic-sparkles' },
  { key: 'content', label: '内容', icon: 'fas fa-file-alt' },
  { key: 'voice', label: '配音', icon: 'fas fa-microphone' }
]

// ═══ 各模块差异化配置 ═══
const mc = computed(() => {
  const configs = {
    novel:        { unit: '章', icon: '📖', expectedChars: 1500, createLabel: '开始创作第一章',   continueLabel: '续写第 {n} 章',  placeholder: '如：加入反转 / 展开新角色...',    finishBtn: '✍️ 续写下一章',   phaseNames: ['构思', '开篇', '情节展开', '高潮推进', '即将完成'], generateBtn: '✨ 开始创作', btnGradient: 'bg-gradient-to-r from-[#FF9500] to-[#FF6B00] text-black', iconColor: 'text-[#FF9500]', progressGradient: 'from-[#FF9500] to-[#FF6B00]' },
    drama:        { unit: '幕', icon: '🎭', expectedChars: 350, createLabel: '创建第一幕',       continueLabel: '创建第 {n} 幕',  placeholder: '如：加入新角色 / 切换场景...',    finishBtn: '🎬 创建下一幕',   phaseNames: ['构思', '搭建舞台', '角色登场', '冲突升级', '即将完成'], generateBtn: '🎭 开始编排', btnGradient: 'bg-gradient-to-r from-pink-500 to-purple-600 text-white', iconColor: 'text-pink-400', progressGradient: 'from-pink-500 to-purple-600' },
    podcast:      { unit: '段', icon: '🎙️', expectedChars: 1000, createLabel: '生成播客内容',     continueLabel: '生成第 {n} 段',  placeholder: '如：展开讨论 / 嘉宾反驳...',      finishBtn: '🎙️ 继续下一段',   phaseNames: ['筝酿', '开场白', '深入讨论', '精彩观点', '即将完成'], generateBtn: '🎙️ 生成 AI 双播', btnGradient: 'bg-gradient-to-r from-cyan-500 to-blue-600 text-white', iconColor: 'text-cyan-400', progressGradient: 'from-cyan-500 to-blue-600' },
    radio:        { unit: '段', icon: '🌙', expectedChars: 350, createLabel: '生成电台独白',     continueLabel: '续写第 {n} 段',  placeholder: '如：加入回忆 / 情感转折...',      finishBtn: '🌙 继续创作',     phaseNames: ['筝酿', '轻声开场', '情感铺垫', '深情诉说', '即将完成'], generateBtn: '🌙 生成情感独白', btnGradient: 'bg-gradient-to-r from-indigo-500 to-purple-600 text-white', iconColor: 'text-indigo-400', progressGradient: 'from-indigo-500 to-purple-600' },
    lecture:      { unit: '节', icon: '📚', expectedChars: 400, createLabel: '生成讲解内容',     continueLabel: '生成第 {n} 节',  placeholder: '如：举个例子 / 深入分析...',      finishBtn: '📚 继续下一节',   phaseNames: ['备课', '引入话题', '知识展开', '总结提炼', '即将完成'], generateBtn: '📚 生成知识讲解', btnGradient: 'bg-gradient-to-r from-green-500 to-emerald-600 text-white', iconColor: 'text-green-400', progressGradient: 'from-green-500 to-emerald-600' },
    ad:           { unit: '版', icon: '📢', expectedChars: 250, createLabel: '生成带货文案',     continueLabel: '生成第 {n} 版',  placeholder: '如：强调优惠 / 换个卖点...',      finishBtn: '📢 生成新版本',   phaseNames: ['分析', '卖点提炼', '文案撰写', '润色优化', '即将完成'], generateBtn: '📢 生成带货文案', btnGradient: 'bg-gradient-to-r from-orange-500 to-red-500 text-white', iconColor: 'text-orange-400', progressGradient: 'from-orange-500 to-red-500' },
    picture_book: { unit: '页', icon: '🐰', expectedChars: 180, createLabel: '创建第一页',       continueLabel: '创建第 {n} 页',  placeholder: '如：小动物们做了什么...',          finishBtn: '🐰 继续下一页',   phaseNames: ['想象', '画面构思', '故事展开', '温馨结尾', '即将完成'], generateBtn: '🐰 生成有声绘本', btnGradient: 'bg-gradient-to-r from-pink-400 to-rose-500 text-white', iconColor: 'text-pink-400', progressGradient: 'from-pink-400 to-rose-500' },
    news:         { unit: '条', icon: '📰', expectedChars: 300, createLabel: '生成新闻稿',       continueLabel: '生成第 {n} 条',  placeholder: '如：补充背景 / 加入数据...',      finishBtn: '📰 生成下一条',   phaseNames: ['采编', '导语撰写', '正文展开', '结尾收束', '即将完成'], generateBtn: '📰 生成新闻播报', btnGradient: 'bg-gradient-to-r from-blue-500 to-indigo-600 text-white', iconColor: 'text-blue-400', progressGradient: 'from-blue-500 to-indigo-600' },
  }
  return configs[project.value?.typeCode] || configs.novel
})

// ═══ 多章节 vs 单次生成 模式判断 ═══
const isMultiSection = computed(() => ['novel'].includes(project.value?.typeCode))
const isStructuredMode = computed(() => ['novel', 'drama'].includes(project.value?.typeCode))

// ═══════ 小说结构化创作 — 状态与数据 ═══════
const novelStep = ref('conceive') // 'conceive' | 'outline' | 'chapter_outline' | 'generate' | 'voice'
const stylePreference = ref('')

// 广播剧：监听题材和灵感变化 → 允许重新生成（声明在 stylePreference 之后避免初始化顺序错误）
watch(stylePreference, () => { if (dramaFinished.value && isDramaType.value) dramaSettingsChanged.value = true })
watch(editableInspiration, () => { if (dramaFinished.value && isDramaType.value) dramaSettingsChanged.value = true })

/** 选择题材 → 同步灵感描述 */
const selectStyle = (genre) => {
  stylePreference.value = genre.id
  // 如果题材有预设灵感，同步到灵感输入框
  if (genre.inspiration) {
    editableInspiration.value = genre.inspiration
  }
}
const targetChapters = ref(10)
const generatingOutline = ref(false)
const masterOutline = ref({ synopsis: '', characters: [], chapters: [] })
const chapterOutlines = ref([]) // [{ plot, keyEvents, characters, foreshadowing }]
const expandedOutline = ref(-1)
const novelActiveChapter = ref(0)
const outlineGenProgress = ref('')
const outlineStreamText = ref('') // 细纲生成时的实时流式文本

const novelSteps = [
  { key: 'conceive', label: '🎯 核心构思' },
  { key: 'outline', label: '📋 大纲规划' },
  { key: 'generate', label: '✍️ 生成正文' }
]

const novelGenres = [
  // 第一梯队：霸榜核心
  { id: 'heroine', icon: '👑', name: '无CP大女主', heat: 5, pov: '第三人称',
    desc: '拒绝恋爱脑，专注事业/复仇/自我成长',
    tips: '女主必须有清晰的个人目标（事业/复仇/成长）；男性角色可以是伙伴/对手，但不是情感归宿；爽点来自认知升级、实力碾压，而非感情拉扯',
    gradient: 'from-amber-500/60 to-yellow-600/60' },
  { id: 'mindgame', icon: '🧠', name: '人性博弈/智斗', heat: 5, pov: '第三人称',
    desc: '烧脑反转，规则对抗，智力碾压',
    tips: '设计严谨的规则体系；每章结尾必须有认知反转或信息差暴击；主角的武器是"规则漏洞"和"信息差"，不是金手指',
    gradient: 'from-indigo-500/60 to-blue-600/60' },
  // 第二梯队：潜力爆款
  { id: 'brainstorm', icon: '💡', name: '脑洞叠加/系统流', heat: 4, pov: '第一/三人称',
    desc: '传统梗+创新脑洞=爆款（弹幕/心声/穿书）',
    tips: '脑洞必须在第一章就亮出来；"多层信息差"是爽感核心（读者和主角都知道，但反派不知道）；每章结尾必须有钩子',
    gradient: 'from-cyan-500/60 to-teal-600/60' },
  { id: 'xianxia', icon: '⚔️', name: '修仙2.0', heat: 4, pov: '第三人称',
    desc: '职场化修仙，系统化宗门，都市化设定',
    tips: '像打游戏一样升级的系统修仙；宗门像大厂职级体系；可融入都市修仙（神仙在CBD上班）',
    gradient: 'from-purple-500/60 to-violet-600/60' },
  // 第三梯队：潜力热门
  { id: 'crazy', icon: '🤪', name: '发疯文学', heat: 3, pov: '第一人称',
    desc: '荒诞应对压力，反内耗式主角',
    tips: '主角用荒诞方式应对压力，"与其内耗自己，不如外耗别人"；读者通过"发癫"获得精神松弛',
    gradient: 'from-pink-500/60 to-rose-600/60' },
  { id: 'historical', icon: '📜', name: '考据式穿越', heat: 3, pov: '第三人称',
    desc: '历史细节严谨，知识增量型爽文',
    tips: '穿越后不做肥皂玻璃火药，用真实典籍知识破局；历史细节极度严谨；提供知识增量',
    gradient: 'from-orange-500/60 to-amber-600/60' },
  { id: 'vintage', icon: '🏛️', name: '年代高干', heat: 3, pov: '第一/三人称',
    desc: '怀旧+阶层逆袭，高干文替代性选择',
    tips: '港圈大佬文/京圈佛子高干文/校园糖水文风格；怀旧 + 阶层逆袭核心',
    gradient: 'from-slate-500/60 to-gray-600/60' }
]

const dramaGenres = [
  { id: 'ceo', icon: '💔', name: '霸总甜宠', heat: 5, pov: '第一/三人称',
    desc: '豪门追妻、契约恋人、身份反转',
    tips: '强烈的身份反差、高蜜度对白、节奏要快；男主装穷/装傻→暴露真实身份→女主震惊',
    inspiration: '霍总装成快递员追妻，被女主各种嫌弃，结果被发现是全城首富',
    gradient: 'from-pink-500/60 to-rose-600/60' },
  { id: 'rebirth', icon: '⏰', name: '穿越重生', heat: 5, pov: '第一人称',
    desc: '重生复仇、开局王炸、逆天改命',
    tips: '开篇就死/被背叛→重生→复仇打脸；节奏紧凑，每幕必有反转',
    inspiration: '女主被渣男和闺蜜害死后重生回到离婚前一天，这次她要让所有人付出代价',
    gradient: 'from-indigo-500/60 to-blue-600/60' },
  { id: 'revenge', icon: '🔥', name: '复仇逆袭', heat: 4, pov: '第一/三人称',
    desc: '打脸打脸再打脸，爽到飞起',
    tips: '开篇被欺压→反转展示实力→打脸反派，每幕必须有爽点',
    inspiration: '被全公司看不起的实习生，其实是集团背后真正的继承人',
    gradient: 'from-orange-500/60 to-red-600/60' },
  { id: 'flash', icon: '💍', name: '闪婚豪门', heat: 4, pov: '第一/三人称',
    desc: '先婚后爱、隐藏身份、双向暗恋',
    tips: '女主不知道老公的真实身份，慢慢发现反转；制造误会和心动',
    inspiration: '相亲对象看起来平平无奇，闪婚后才发现他竟是全城最神秘的豪门少主',
    gradient: 'from-amber-500/60 to-yellow-600/60' },
  { id: 'ancient', icon: '👑', name: '古装宫斗', heat: 3, pov: '第三人称',
    desc: '后宫斗争、宫廷权谋、逆袭封后',
    tips: '后宫勾心斗角、姆姈情仇、权力博弈，对白要狠',
    inspiration: '冷宫庐女用智谋一步步爬到后位，让当初欺负她的人都跪在她脚下',
    gradient: 'from-slate-500/60 to-gray-600/60' },
  { id: 'campus', icon: '🌸', name: '校园暗恋', heat: 3, pov: '第一人称',
    desc: '暗恋成真、青春遗憾、重逢旧爱',
    tips: '清纯校园感、暗恋心理描写、终于表白的释放感',
    inspiration: '暗恋了三年的同桌，毕业前最后一天终于鼓起勇气表白',
    gradient: 'from-cyan-500/60 to-teal-600/60' },
  { id: 'meme', icon: '🤪', name: '沙雕搞笑', heat: 4, pov: '第一人称',
    desc: '反套路、发疯文学、离谱但上头',
    tips: '穿越成炮灰却主动作死；用荒诞逻辑打破常规剧情；对白要有网感和梗',
    inspiration: '穿越成反派女配，别人都在拼命活命，她却天天给女主送娚妆地址',
    gradient: 'from-yellow-500/60 to-lime-600/60' }
]

const activeGenres = computed(() => isDramaType.value ? dramaGenres : novelGenres)

const selectedGenre = computed(() => activeGenres.value.find(g => g.id === stylePreference.value))

const chapterCountOptions = [
  { value: 5, label: '5 章·短篇', icon: '📄' },
  { value: 10, label: '10 章·中篇', icon: '📑' },
  { value: 20, label: '20 章·长篇', icon: '📚' }
]

const dramaCountOptions = [
  { value: 5, label: '5 幕·短剧', icon: '🎬' },
  { value: 8, label: '8 幕·标准', icon: '🎭' },
  { value: 10, label: '10 幕·长剧', icon: '📺' }
]

const activeChapterOptions = computed(() => isDramaType.value ? dramaCountOptions : chapterCountOptions)
const activeUnitName = computed(() => isDramaType.value ? '幕' : '章')

const canGoToStep = (stepKey) => {
  const order = ['conceive', 'outline', 'generate']
  const current = order.indexOf(novelStep.value)
  const target = order.indexOf(stepKey)
  if (target <= current) return true
  if (target === current + 1 && isStepDone(novelStep.value)) return true
  return false
}

const isStepLocked = (stepKey) => {
  const order = ['conceive', 'outline', 'generate']
  const current = order.indexOf(novelStep.value)
  const target = order.indexOf(stepKey)
  return target < current
}

const isStepDone = (stepKey) => {
  if (stepKey === 'conceive') return !!stylePreference.value
  if (stepKey === 'outline') return !!masterOutline.value.synopsis && chapterOutlines.value.length > 0 && chapterOutlines.value.some(co => co.plot)
  if (stepKey === 'generate') {
    if (chapterOutlines.value.length === 0) return false
    const validSections = sections.value.filter(s => isValidChapterContent(s.content))
    return validSections.length >= chapterOutlines.value.length
  }
  return false
}

const allOutlinesDone = computed(() => chapterOutlines.value.length > 0 && chapterOutlines.value.every(co => co.plot))

const nextOutlineBatchLabel = computed(() => {
  const done = chapterOutlines.value.filter(co => co.plot).length
  const total = chapterOutlines.value.length
  if (done >= total) return '🔄 重新生成全部细纲'
  const remaining = total - done
  const batch = Math.min(5, remaining)
  return `🧠 生成接下来 ${batch} 章细纲（${done}/${total}）`
})

// ═══ 小说结构化创作 — AI 生成函数 ═══

/** 通用：从 SSE 流中读取完整文本（正确处理多行 data 和换行恢复） */
const readSseStreamFull = async (resp) => {
  const reader = resp.body.getReader()
  const decoder = new TextDecoder()
  let fullText = ''
  let buffer = ''
  while (true) {
    const { value, done } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    // 按行处理，SSE 事件以空行分隔
    const lines = buffer.split('\n')
    buffer = lines.pop() // 保留未完成的行
    for (const line of lines) {
      if (line.startsWith('data:')) {
        fullText += line.substring(5)
      } else if (line.trim() === '') {
        // 空行 = SSE 事件边界，不加额外内容
      }
    }
  }
  // 处理缓冲区残留
  if (buffer.startsWith('data:')) fullText += buffer.substring(5)
  return fullText
}

/** 通用：清洗 AI 返回的不规范 JSON 文本 */
const sanitizeJsonFromAI = (raw) => {
  let text = raw
  // 去掉 markdown 代码块标记
  text = text.replace(/```(?:json)?\s*/gi, '').replace(/```/g, '')
  // 中文标点转 ASCII
  text = text.replace(/\u201c|\u201d/g, '"').replace(/\u2018|\u2019/g, "'").replace(/\uff0c/g, ',').replace(/\uff1a/g, ':')
  // 去掉 JSON 内尾部逗号
  text = text.replace(/,\s*([\]\}])/g, '$1')
  // 去掉换行符（JSON 值中不应有裸换行）
  text = text.replace(/[\r\n]+/g, ' ')
  // 修复 JSON 值内的未转义双引号：找到 "key":"value" 模式，转义 value 内部的引号
  text = text.replace(/"(plot|keyEvents|characters|foreshadowing|synopsis|name|desc)"\s*:\s*"((?:[^"\\]|\\.)*)"/g, (match) => match)
  // 更激进的修复：逐字符扫描修复未转义引号
  try {
    JSON.parse(text.match(/[\[\{][\s\S]*[\]\}]/)?.[0] || text)
  } catch {
    // JSON.parse 失败说明有未转义引号，用逐段替换修复
    text = text.replace(/"([^"]{0,20})":\s*"([^"]*(?:"[^":,\}\]]*)*?)"\s*([,\}\]])/g, (m, key, val, end) => {
      const fixedVal = val.replace(/(?<!\\)"/g, '\\"')
      return `"${key}":"${fixedVal}"${end}`
    })
  }
  return text.trim()
}

/** Step 1 → Step 2: 生成整体大纲（后端构建 Prompt，前端零 Prompt） */
const generateMasterOutline = async () => {
  if (!stylePreference.value) { toastStore.show('请先选择题材方向'); return }
  generatingOutline.value = true
  
  try {
    // 前端只传结构化数据，后端 Agent 构建完整 Prompt
    const resp = await studioApi.generateOutline(projectId.value, {
      stylePreference: stylePreference.value,
      targetChapters: targetChapters.value
    })
    // axios 拦截器已解包 Result.data，resp 直接就是数据
    const fullText = typeof resp === 'string' ? resp : (resp?.data || JSON.stringify(resp) || '')
    const cleaned = sanitizeJsonFromAI(fullText)
    console.debug('[Outline] 大纲原始:', fullText.substring(0, 500))
    console.debug('[Outline] 大纲清洗:', cleaned.substring(0, 500))
    
    let data = null
    
    // 策略1: 直接匹配 JSON 对象并解析
    const jsonMatch = cleaned.match(/\{[\s\S]*\}/)
    if (jsonMatch) {
      try {
        data = JSON.parse(jsonMatch[0])
      } catch (e) {
        console.warn('[Novel] 大纲策略1失败:', e.message)
      }
    }
    
    // 策略2: 正则提取各字段
    if (!data) {
      try {
        const synopsisMatch = cleaned.match(/"synopsis"\s*:\s*"((?:[^"\\]|\\.)*)"/s)
        const synopsis = synopsisMatch?.[1] || ''
        
        const characters = []
        const charMatches = cleaned.match(/"name"\s*:\s*"([^"]+)"/g)
        const descMatches = cleaned.match(/"desc"\s*:\s*"([^"]+)"/g)
        if (charMatches) {
          charMatches.forEach((m, i) => {
            const name = m.match(/"name"\s*:\s*"([^"]+)"/)?.[1] || ''
            const desc = descMatches?.[i]?.match(/"desc"\s*:\s*"([^"]+)"/)?.[1] || ''
            characters.push({ name, desc })
          })
        }
        
        // 提取 chapters — 支持对象数组和字符串数组
        const chapters = []
        const plotMatches = cleaned.match(/"plot"\s*:\s*"([^"]+)"/g)
        if (plotMatches && plotMatches.length > 0) {
          // Rich mode: extract plot/keyEvents/foreshadowing per chapter
          const keMatches = cleaned.match(/"keyEvents"\s*:\s*"([^"]+)"/g)
          const fsMatches = cleaned.match(/"foreshadowing"\s*:\s*"([^"]+)"/g)
          plotMatches.forEach((m, i) => {
            chapters.push({
              plot: m.match(/"plot"\s*:\s*"([^"]+)"/)?.[1] || '',
              keyEvents: keMatches?.[i]?.match(/"keyEvents"\s*:\s*"([^"]+)"/)?.[1] || '',
              foreshadowing: fsMatches?.[i]?.match(/"foreshadowing"\s*:\s*"([^"]+)"/)?.[1] || ''
            })
          })
        } else {
          // Fallback: string array
          const chaptersBlock = cleaned.match(/"chapters"\s*:\s*\[([\s\S]*?)\]/)
          if (chaptersBlock) {
            const items = chaptersBlock[1].match(/"([^"]+)"/g)
            if (items) items.forEach(item => chapters.push(item.replace(/^"|"$/g, '')))
          }
        }
        
        if (synopsis || chapters.length > 0) {
          data = { synopsis, characters, chapters }
          console.debug('[Novel] 大纲策略2成功，提取到', chapters.length, '章')
        }
      } catch (e) {
        console.warn('[Novel] 大纲策略2失败:', e.message)
      }
    }
    
    if (data && (data.synopsis || (data.chapters && data.chapters.length > 0))) {
      masterOutline.value = {
        synopsis: data.synopsis || '',
        characters: (data.characters || []).map(c => ({ name: c.name || '', desc: c.desc || '' })),
        chapters: data.chapters || []
      }
      
      // 将 chapters 映射为 chapterOutlines（支持对象和字符串两种格式）
      const chapArr = data.chapters || []
      chapterOutlines.value = chapArr.map((ch, i) => {
        if (typeof ch === 'object' && ch.plot) {
          return { plot: ch.plot, keyEvents: ch.keyEvents || '', characters: '', foreshadowing: ch.foreshadowing || '' }
        } else {
          return { plot: String(ch), keyEvents: '', characters: '', foreshadowing: '' }
        }
      })
      
      // 确保章节数匹配目标
      while (chapterOutlines.value.length < targetChapters.value) {
        chapterOutlines.value.push({ plot: '', keyEvents: '', characters: '', foreshadowing: '' })
      }
      
      novelStep.value = 'outline'
      toastStore.show('\u2728 大纲生成完成（含详细章节规划），请审阅后确认')
    } else {
      toastStore.show('大纲解析失败，请重试')
      console.error('[Novel] 所有解析策略失败:', cleaned.substring(0, 800))
    }
  } catch (e) {
    toastStore.show('大纲生成失败: ' + (e.message || '网络错误'))
  } finally {
    generatingOutline.value = false
  }
}

/** Step 2 → Step 3: 确认大纲，直接进入正文创作 */
const confirmOutlineAndNext = () => {
  if (!masterOutline.value.synopsis) { toastStore.show('大纲不能为空'); return }
  if (chapterOutlines.value.length === 0 || !chapterOutlines.value.some(co => co.plot)) {
    toastStore.show('请确保至少有一章有内容'); return
  }
  saveOutlinesToBackend()
  novelStep.value = 'generate'
  toastStore.show('大纲已确认 ✅ 开始逐章创作')
}

/** 保存大纲 + 细纲到后端 */
const saveOutlinesToBackend = () => {
  const data = {
    ...masterOutline.value,
    chapterOutlines: chapterOutlines.value,
    stylePreference: stylePreference.value
  }
  studioApi.updateProject(projectId.value, {
    outline: JSON.stringify(data)
  }).catch(e => console.warn('保存大纲失败:', e))
}

/** Step 3: 批量生成细纲（每次 5 章）+ RAG 前文记忆 + 流式进度 */
const generateChapterOutlines = async () => {
  generatingOutline.value = true
  outlineStreamText.value = ''
  
  let startIdx = chapterOutlines.value.findIndex(co => !co.plot)
  if (startIdx === -1) startIdx = 0
  const endIdx = Math.min(startIdx + 5, chapterOutlines.value.length)
  outlineGenProgress.value = `第${startIdx + 1}-${endIdx}章`
  
  // RAG 前文记忆
  const contextParts = []
  contextParts.push(`【核心设定】\n故事梗概：${masterOutline.value.synopsis}`)
  if (masterOutline.value.characters.length > 0) {
    contextParts.push(`角色设定：${masterOutline.value.characters.map(c => `${c.name}（${c.desc}）`).join('；')}`)
  }
  contextParts.push(`【整体章节大纲】\n${masterOutline.value.chapters.map((ch, i) => `第${i+1}章：${ch}`).join('\n')}`)
  
  const prevOutlines = chapterOutlines.value.slice(0, startIdx).filter(co => co.plot)
  if (prevOutlines.length > 0) {
    contextParts.push(`【前文细纲摘要（前${prevOutlines.length}章）】\n${prevOutlines.map((co, i) => `第${i+1}章：${co.plot}`).join('\n')}`)
  }
  
  const prompt = `${contextParts.join('\n\n')}

你是一位资深小说编辑。请为第 ${startIdx + 1} 章到第 ${endIdx} 章生成详细细纲。

每章细纲必须包含4个维度：
- plot: 剧情梗概（本章发生了什么，100-150字）
- keyEvents: 核心事件（关键转折点/冲突，50字以内）
- characters: 人物互动（本章出场角色及互动关系，50字以内）
- foreshadowing: 伏笔埋设（为后续章节的铺垫，30字以内）

严格按以下 JSON 数组格式输出（不要输出其他内容）：
[{"plot":"...", "keyEvents":"...", "characters":"...", "foreshadowing":"..."}, ...]
数组长度必须为 ${endIdx - startIdx}，对应第 ${startIdx + 1} 到第 ${endIdx} 章。`

  try {
    const resp = await studioApi.generateContent(projectId.value, prompt)
    const reader = resp.body.getReader()
    const decoder = new TextDecoder()
    let fullText = ''
    let buffer = ''
    // 流式读取，实时显示进度
    while (true) {
      const { value, done } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop()
      for (const line of lines) {
        if (line.startsWith('data:')) {
          const token = line.substring(5)
          fullText += token
          outlineStreamText.value = fullText
        }
      }
    }
    if (buffer.startsWith('data:')) {
      fullText += buffer.substring(5)
      outlineStreamText.value = fullText
    }
    
    console.debug('[Novel] 细纲原始:', fullText.substring(0, 500))
    const cleaned = sanitizeJsonFromAI(fullText)
    console.debug('[Novel] 细纲清洗:', cleaned.substring(0, 500))
    
    // 多级 JSON 解析回退
    let outlines = null
    
    // 策略1: 直接匹配 JSON 数组
    const arrayMatch = cleaned.match(/\[[\s\S]*\]/)
    if (arrayMatch) {
      try {
        outlines = JSON.parse(arrayMatch[0])
      } catch (e) {
        console.warn('[Novel] 策略1失败:', e.message)
      }
    }
    
    // 策略2: 逐个提取 JSON 对象
    if (!outlines) {
      try {
        const objectMatches = cleaned.match(/\{[^{}]{20,}\}/g)
        if (objectMatches && objectMatches.length > 0) {
          outlines = objectMatches.map(m => {
            try { return JSON.parse(m) } catch { return null }
          }).filter(Boolean)
          if (outlines.length === 0) outlines = null
        }
      } catch (e) {
        console.warn('[Novel] 策略2失败:', e.message)
      }
    }
    
    // 策略3: 正则提取字段
    if (!outlines) {
      try {
        const plotMatches = cleaned.match(/"plot"\s*:\s*"([^"]+)"/g)
        if (plotMatches && plotMatches.length > 0) {
          outlines = plotMatches.map(pm => {
            const plotVal = pm.match(/"plot"\s*:\s*"([^"]+)"/)?.[1] || ''
            return { plot: plotVal, keyEvents: '', characters: '', foreshadowing: '' }
          })
          // 尝试提取 keyEvents
          const keMatches = cleaned.match(/"keyEvents"\s*:\s*"([^"]+)"/g)
          if (keMatches) keMatches.forEach((m, i) => {
            if (outlines[i]) outlines[i].keyEvents = m.match(/"keyEvents"\s*:\s*"([^"]+)"/)?.[1] || ''
          })
          const chMatches = cleaned.match(/"characters"\s*:\s*"([^"]+)"/g)
          if (chMatches) chMatches.forEach((m, i) => {
            if (outlines[i]) outlines[i].characters = m.match(/"characters"\s*:\s*"([^"]+)"/)?.[1] || ''
          })
          const fsMatches = cleaned.match(/"foreshadowing"\s*:\s*"([^"]+)"/g)
          if (fsMatches) fsMatches.forEach((m, i) => {
            if (outlines[i]) outlines[i].foreshadowing = m.match(/"foreshadowing"\s*:\s*"([^"]+)"/)?.[1] || ''
          })
        }
      } catch (e) {
        console.warn('[Novel] 策略3失败:', e.message)
      }
    }
    
    if (outlines && outlines.length > 0) {
      outlines.forEach((ol, i) => {
        const idx = startIdx + i
        if (idx < chapterOutlines.value.length) {
          chapterOutlines.value[idx] = {
            plot: ol.plot || '', keyEvents: ol.keyEvents || '',
            characters: ol.characters || '', foreshadowing: ol.foreshadowing || ''
          }
        }
      })
      expandedOutline.value = startIdx
      const doneCount = chapterOutlines.value.filter(co => co.plot).length
      toastStore.show(`✅ 细纲生成完成（${doneCount}/${chapterOutlines.value.length}）`)
      saveOutlinesToBackend()
    } else {
      toastStore.show('细纲解析失败，请重试')
      console.error('[Novel] All parse strategies failed. Cleaned:', cleaned.substring(0, 800))
    }
  } catch (e) {
    toastStore.show('细纲生成失败: ' + (e.message || '网络错误'))
  } finally {
    generatingOutline.value = false
    outlineGenProgress.value = ''
    outlineStreamText.value = ''
  }
}

/** Step 4: 根据细纲生成正文（上下文由后端 RAG 引擎注入） */
const generateFromOutline = async () => {
  const chapterIdx = novelActiveChapter.value
  const outline = chapterOutlines.value[chapterIdx]
  if (!outline?.plot) { toastStore.show('该' + activeUnitName.value + '细纲为空，请先生成细纲'); return }
  
  const unit = activeUnitName.value
  
  // 前端零 Prompt：只传结构化数据，后端 Agent 构建完整用户消息
  const structuredData = {
    chapterIndex: chapterIdx,
    outlinePlot: outline.plot,
    keyEvents: outline.keyEvents || '',
    foreshadowing: outline.foreshadowing || '',
    userExtra: userInput.value.trim() || null
  }
  
  // 复用现有 generate 逻辑
  generating.value = true
  streamContent.value = ''
  justFinished.value = false
  genStartTime.value = Date.now()
  elapsedTime.value = 0
  elapsedTimer = setInterval(() => { elapsedTime.value = Math.round((Date.now() - genStartTime.value) / 1000) }, 500)

  const controller = new AbortController()
  const timeout = setTimeout(() => controller.abort(), 120000)

  try {
    const resp = await studioApi.generateContent(projectId.value, structuredData)
    const reader = resp.body.getReader()
    const decoder = new TextDecoder()
    controller.signal.addEventListener('abort', () => { try { reader.cancel() } catch {} })

    while (true) {
      const { value, done } = await reader.read()
      if (done) break
      clearTimeout(timeout)
      const chunk = decoder.decode(value, { stream: true })
      for (const line of chunk.split('\n')) {
        if (line.startsWith('data:')) streamContent.value += line.substring(5)
      }
      nextTick(() => { const el = streamContentRef.value; if (el) el.scrollTop = el.scrollHeight })
    }
    userInput.value = ''
    chapterTitle.value = ''
    await loadSections()
    await loadProject()
    // 自动展开新章节
    if (sections.value.length > 0) {
      const lastSection = sections.value[sections.value.length - 1]
      expandedSections.add(lastSection.id)
      activeSection.value = lastSection
    }
    justFinished.value = true
    toastStore.show(`第${chapterIdx + 1}${unit}创作完成 ✨ ${streamContent.value.length}字 · ${elapsedTime.value}s`)
  } catch (e) {
    const msg = e.name === 'AbortError' ? '请求超时，请重试' : (e.message || '网络错误')
    toastStore.show('生成失败: ' + msg)
  } finally {
    clearTimeout(timeout)
    clearInterval(elapsedTimer)
    generating.value = false
  }
}

/** Step 4: 批量连续生成全部剩余章节 */
const batchGenerateChapters = async () => {
  const pendingIndices = chapterOutlines.value
    .map((co, i) => ({ co, i }))
    .filter(({ co, i }) => co.plot && !getValidSection(i))
    .map(({ i }) => i)
  
  if (pendingIndices.length === 0) {
    toastStore.show('所有章节已生成 ✅')
    return
  }
  
  toastStore.show(`开始连续生成 ${pendingIndices.length} 章...`)
  
  for (const idx of pendingIndices) {
    novelActiveChapter.value = idx
    streamContent.value = ''
    justFinished.value = false
    await nextTick()
    await generateFromOutline()
    // 等待一小段时间避免请求过快
    await new Promise(r => setTimeout(r, 500))
  }
  
  toastStore.show(`🎉 全部 ${pendingIndices.length} 章生成完成！`)
}

// ═══ 各模块专属数据 ═══

// 播客：对话解析 + 音频播放

/** 解析播客对话内容为结构化气泡 */
const podcastDialogues = computed(() => {
  if (!sections.value.length) return []
  const fullText = sections.value.map(s => s.content || '').join('\n')
  const lines = fullText.split('\n').filter(l => l.trim())
  const dialogues = []
  // 识别说话人的名字集合
  const speakerNames = new Set()
  
  for (const line of lines) {
    // 匹配 "名字：内容" 或 "名字: 内容" 格式
    const match = line.match(/^([^：:]{1,15})[：:]\s*(.+)/)
    if (match) {
      const speaker = match[1].trim()
      const text = match[2].trim()
      if (text) {
        speakerNames.add(speaker)
        dialogues.push({ speaker, text, isHostB: false })
      }
    }
  }
  
  // 用出现顺序标记 A/B 主播
  const speakerList = [...speakerNames]
  if (speakerList.length >= 2) {
    const hostB = speakerList[1]
    dialogues.forEach(d => {
      if (d.speaker === hostB || speakerList.indexOf(d.speaker) % 2 === 1) {
        d.isHostB = true
      }
    })
  }
  
  return dialogues
})

/** 播放播客音频 */
const playPodcastAudio = () => {
  const audioUrls = sections.value.filter(s => s.audioUrl).map(s => s.audioUrl)
  if (!audioUrls.length) return
  
  // 用 playerStore 播放，queue 属性用于多段连续播放
  playerStore.play({
    title: project.value?.title || '播客',
    url: audioUrls[0],
    queue: audioUrls.slice(1)
  })
  
  toastStore.show('🎧 开始播放播客')
}

/** 格式化日期 */
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)} 分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)} 小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)} 天前`
  return `${d.getMonth() + 1}月${d.getDate()}日`
}

// 情感电台：情绪氛围（每种情绪携带完整的创作指令修饰符）
const selectedMood = ref(null)
const radioMoods = [
  {
    id: 'gentle', icon: '🌸', name: '温柔治愈', desc: '轻声细语，如春风拂面',
    gradient: 'from-pink-500/60 to-rose-500/60',
    promptHint: '【情绪：温柔治愈】\n写作指引：\n- 节奏：像溪水一样缓缓流淌，句子轻柔、短小，每句不超过15字\n- 意象：月光、微风、棉花糖、暖阳、猫咪蜷缩、毛毯、热可可\n- 语气：像在耳边低语，像拥抱，像轻轻拍肩膀\n- 结构：先描绘一个安静的画面→承认听者的疲惫→温柔地给予认可→以一句拥抱感的话收束\n- 金句方向："你已经很努力了""允许自己休息""没关系的"'
  },
  {
    id: 'sad', icon: '🌧️', name: '感伤回忆', desc: '低沉深情，触动心弦',
    gradient: 'from-slate-500/60 to-gray-600/60',
    promptHint: '【情绪：感伤回忆】\n写作指引：\n- 节奏：缓慢、沉重，句与句之间有呼吸般的停顿，善用破折号（——）制造留白\n- 意象：旧照片、下雨天、空荡的车站、褪色的信、窗上的雾气、背影\n- 语气：克制的深情——不哭出来比大哭更动人，用画面代替直接抒情\n- 结构：以一个具体的回忆场景起笔→慢慢把画面展开→在最痛的地方轻轻一笔带过→最后一段转向释怀的光亮\n- 金句方向："有些人走了就是走了""想念是一个人的事""所有的告别都是为了重逢"'
  },
  {
    id: 'passion', icon: '🔥', name: '热血励志', desc: '激昂有力，点燃斗志',
    gradient: 'from-orange-500/60 to-red-500/60',
    promptHint: '【情绪：热血励志】\n写作指引：\n- 节奏：由慢到快，逐步加速，像心跳在加快，最后几段要短促有力\n- 意象：黎明、火焰、拳头、奔跑、破茧、风暴中的灯塔\n- 语气：不是空洞的喊口号——先承认困难和痛苦的真实性，再从废墟中站起来\n- 结构：先共情低谷（"我知道你现在很难"）→用一个真实的转折点（"但是你看——"）→层层递进的力量感→最后一句话像一拳击在胸口\n- 金句方向："你不是一个人在扛""咬牙的日子终会发光""普通人的英雄主义就是认清生活后依然热爱"'
  },
  {
    id: 'quiet', icon: '🌿', name: '安静陪伴', desc: '不急不缓，静静守候',
    gradient: 'from-green-500/60 to-teal-500/60',
    promptHint: '【情绪：安静陪伴】\n写作指引：\n- 节奏：极慢，像坐在老朋友旁边，不说话也不尴尬，偶尔才说一句\n- 意象：深夜的台灯、茶杯里的热气、窗外的虫鸣、公园的长椅、慢慢走的路\n- 语气：不给建议、不讲道理、不说"加油"——只是安静地陪着，让听者知道有人在\n- 结构：极短的开场（"嘿，我在"）→描述此刻的安静→几段不带目的的闲话→结尾只说一个字——"在"\n- 金句方向："不用说话，我在这""什么都不做也是可以的""陪你坐一会儿"'
  },
  {
    id: 'night', icon: '🌃', name: '深夜独白', desc: '一个人的夜，和自己对话',
    gradient: 'from-indigo-500/60 to-violet-600/60',
    promptHint: '【情绪：深夜独白】\n写作指引：\n- 节奏：像自言自语，断断续续，有时一句话说到一半停住，换了个方向\n- 意象：天花板、手机屏幕的光、凌晨的钟声、空了的酒杯、城市的远景\n- 语气：诚实到近乎赤裸——白天不敢说的话，深夜全都倒出来\n- 结构：以"睡不着"或"又到了这个时间"起笔→一个人的内心独白→对自己的审问与和解→最后选择原谅自己然后睡去\n- 金句方向："白天的我和夜晚的我是两个人""想太多是因为在乎太多""明天的我会替今晚的我撑住"'
  },
  {
    id: 'morning', icon: '☀️', name: '早安能量', desc: '新的一天，轻盈出发',
    gradient: 'from-amber-400/60 to-orange-500/60',
    promptHint: '【情绪：早安能量】\n写作指引：\n- 节奏：轻快、明亮，像拉开窗帘的一瞬间，句子干净利落\n- 意象：阳光穿过窗帘、咖啡的香气、鸟鸣、露水、伸懒腰、第一口清新的空气\n- 语气：温暖但不腻——像一个总是笑着的朋友在敲你的门\n- 结构：以一个清晨的画面唤醒→轻快地聊今天可以期待的小事→给一个小小的挑战→以"出发吧"结束\n- 金句方向："新的一天值得一个深呼吸""今天也要好好吃早餐""你值得每一个早晨"'
  }
]

// 情感电台：时长选择
const selectedDuration = ref(null)
const radioDurations = [
  { id: 'short',  icon: '🎧', name: '短篇', desc: '1-2 分钟', chars: '200-350字', promptHint: '请控制在200-350字。' },
  { id: 'medium', icon: '📻', name: '标准', desc: '3-4 分钟', chars: '400-600字', promptHint: '请控制在400-600字。' },
  { id: 'long',   icon: '🌙', name: '长篇', desc: '5-8 分钟', chars: '800-1200字', promptHint: '请控制在800-1200字，可以展开更多情感层次和画面。' }
]

// 知识讲解：难度等级
const selectedLevel = ref(null)
const lectureLevels = [
  {
    id: 'easy', icon: '🌱', name: '入门科普', desc: '零基础秒懂',
    promptHint: '【讲解风格：入门科普】\n教学方法论：\n- 费曼技巧：假设听者是完全没有背景知识的朋友，用最日常的语言解释\n- 每个概念必须用一个生活类比开场（比如用快递分拣解释算法，用水波纹解释电磁波）\n- 禁用任何专业术语，如果必须出现，立即用括号加一句也就是说来翻译\n- 节奏：短句为主，一个概念一段话，段与段之间留呼吸\n- 互动感：适当使用你有没有想过、你猜结果是什么来拉住听者注意力\n- 结尾用一句话总结核心，让听者觉得原来这么简单'
  },
  {
    id: 'mid', icon: '📖', name: '进阶解读', desc: '知其所以然',
    promptHint: '【讲解风格：进阶解读】\n教学方法论：\n- 苏格拉底式追问：不直接给答案，先抛出一个反直觉的现象或问题，引导听者思考\n- 每个知识点要回答三个问题：是什么、为什么、那又怎样\n- 可以引入学科交叉视角，用经济学解释生物学，用物理学解释社会现象\n- 关键转折处用但是、有意思的是制造认知冲突，先建立直觉再打破它\n- 适当引用数据、实验、论文结论来增加权威感，但用口语化方式表达\n- 结尾要有一个升维视角，帮听者从更高层次理解这个知识点的位置'
  },
  {
    id: 'pro', icon: '🔬', name: '专业深度', desc: '第一性原理',
    promptHint: '【讲解风格：专业深度】\n教学方法论：\n- 第一性原理：从最基本的公理和假设出发，逐步推导出结论，不跳过任何逻辑环节\n- 像理查德费曼讲课一样：用思想实验代替公式，用如果我们想象开启推理\n- 不回避复杂性，承认这个问题其实很难，然后带听者一步步拆解\n- 展示科学家的思维过程：假设、预测、验证、修正，让听者学会思维方式而不只是结论\n- 可以提及学术争议和未解之谜，目前主流观点是A但也有科学家认为B\n- 结尾要有前沿展望，这个领域接下来最激动人心的问题是什么'
  }
]

// 带货文案
const adProductName = ref('')
const selectedAdStyle = ref(null)
const adStyles = [
  {
    id: 'seed', icon: '🌟', name: '种草安利', desc: '真实感染力',
    promptHint: '【文案风格：种草安利】\n写作方法：\n- 像闺蜜在分享好物，不是在卖东西，是在真心安利\n- 开头用自己的真实体验切入，比如我前两天发现了一个好东西\n- 卖点用恋爱式表达，不说参数单说感受，比如开模具的质感而不是重量200g\n- 场景化描写，让听者在脑子里看见自己在用这个产品\n- 结尾用非给推荐式的结束语，而不是硬广式的快去买'
  },
  {
    id: 'review', icon: '📊', name: '测评对比', desc: '理性信服力',
    promptHint: '【文案风格：测评对比】\n写作方法：\n- 像专业测评博主一样，用数据和对比说话\n- 开头抛出一个常见误区，比如很多人以为XX越贵越好其实不是\n- 用真实测试场景对比，比如我试了A和B两款结果是\n- 列举2-3个关键维度对比，让听者觉得答案很明显\n- 结尾给出一个清晰的购买建议，不用所有人都买而是如果你是XX的人选这个'
  },
  {
    id: 'story', icon: '📖', name: '故事植入', desc: '软性种草',
    promptHint: '【文案风格：故事植入】\n写作方法：\n- 用一个真实的生活场景或小故事开头，让听者先代入情感\n- 产品自然地出现在故事里，不是硬插而是的确是解决了韮个问题\n- 全篇像在讲故事，产品只占三分之一的篇幅\n- 故事要有反转，比如从踩坑到发现好东西\n- 结尾随意带一句产品信息，让听者自己好奇去搜'
  },
  {
    id: 'live', icon: '🔥', name: '直播开叫', desc: '爆发式转化',
    promptHint: '【文案风格：直播开叫】\n写作方法：\n- 像李佳琪在直播间一样，充满激情和紧迫感\n- 开头就是爆炸式开场，比如所有女生听我说今天这个价格我跟品牌方磨了三个月\n- 用快节奏短句，制造紧迫感，只有今晚、就剩XX单\n- 卖点用算账式表达，比如一天只要X块钱连咖啡都不如\n- 结尾用赶紧下单式的行动号召'
  }
]

// 有声绘本：适龄
const selectedAge = ref(null)
const picBookAges = [
  {
    id: 'baby', icon: '🍼', name: '2-4岁', desc: '重复韵律感',
    promptHint: '【适龄：2-4岁宝宝】\n写故事的方法：\n- 这个年龄的孩子爱重复，故事要有重复的句式和节奏，比如小兔子跳啊跳啊跳\n- 句子要短，每句不超过10个字，词汇用孩子每天能听到的\n- 大量使用拟声词，哗啦啦、咕噜咕噜、吣吣吣，让耳朵开心\n- 角色用小动物，比如小兔子、小熊、小鸭子，不要用抽象的角色\n- 故事结尾要温暨安全，像被妈妈抱着一样\n- 通篇不超过300字'
  },
  {
    id: 'kid', icon: '🧒', name: '4-6岁', desc: '互动探索型',
    promptHint: '【适龄：4-6岁小朋友】\n写故事的方法：\n- 这个年龄的孩子开始有了社交意识，故事要有朋友之间的互动\n- 可以加入简单的问题和选择，比如你猜小猴子会怎么做，让孩子参与\n- 情节可以有小小的困难和解决过程，培养解决问题的能力\n- 角色可以有性格差异，比如勇敢的小狮子和害羞的小兔子\n- 每句不超过15个字，仍然要有韵律感和拟声词\n- 通篇400-500字'
  },
  {
    id: 'child', icon: '📚', name: '6-8岁', desc: '小小冒险家',
    promptHint: '【适龄：6-8岁大孩子】\n写故事的方法：\n- 这个年龄的孩子已经能听懂更复杂的故事，可以有转折和悬念\n- 情节可以像迷你冒险，有任务、有挑战、有克服困难的过程\n- 可以融入简单的道德选择，比如该不该帮助陷入困境的陌生人\n- 词汇可以丰富一些，但仍然要口语化，适合朗读\n- 角色可以是孩子自己的年龄，让听者代入\n- 通篇500-600字'
  }
]

// 新闻播报：播报风格
const selectedNewsStyle = ref(null)
const newsStyles = [
  {
    id: 'formal', icon: '🎙️', name: '正式播报', desc: '央视新闻联播风',
    promptHint: '【播报风格：正式播报】\n播报方法：\n- 像央视新闻联播一样，庄重、客观、有分量\n- 开头是一句话导语，涵盖事件的核心信息\n- 用倒金字塔结构，最重要的信息放在最前面\n- 语言简洁准确，每句不超过25字，绝不拖泥带水\n- 数据引用要准确，转述用据悉、相关负责人表示\n- 结尾不加主观评价，让事实说话'
  },
  {
    id: 'casual', icon: '💬', name: '通俗解读', desc: '白岩松式解读',
    promptHint: '【播报风格：通俗解读】\n播报方法：\n- 像白岩松的新闻1+1一样，把新闻用老百姓听得懂的话说\n- 开头用一句大白话概括事件本质，比如简单来说就是\n- 专业术语必须立即翻译，比如GDP也就是一个国家一年挚多少钱\n- 用类比帮助理解，比如这个量级相当于XX个西湖\n- 语气亲切自然，像在跟朋友聊今天的新闻\n- 结尾可以加一句点评性总结'
  },
  {
    id: 'comment', icon: '🧐', name: '评论分析', desc: '深度观点输出',
    promptHint: '【播报风格：评论分析】\n播报方法：\n- 像财经评论员一样，有观点、有逻辑、有洞察\n- 开头先用两句话降述事实，然后立即抛出观点\n- 用三个层次拆解：表面现象是什么、背后原因是什么、对我们意味着什么\n- 可以引用不同方观点，比如也有人认为，体现多元思考\n- 用数据和历史类比增强说服力\n- 结尾某种意义上说或者值得我们思考的是，给听者留下思考空间'
  },
  {
    id: 'breaking', icon: '⚡', name: '突发快讯', desc: '极速简洁报道',
    promptHint: '【播报风格：突发快讯】\n播报方法：\n- 像突发新闻插播一样，简短、急迫、信息密度极高\n- 开头直接插播式开场，比如据最新消息、我们接到报道\n- 每句话都是关键信息，不要任何废话和过渡\n- 全篇控制在300字以内，宁精勿滥\n- 结尾用我们将持续关注此事进展收束'
  }
]

// ═══ 单次生成模式 - 内容展开/收起 ═══
const singleExpandedMap = ref({})
const toggleSingleExpand = (id) => {
  singleExpandedMap.value[id] = !singleExpandedMap.value[id]
}
const copyContent = async (text) => {
  try {
    await navigator.clipboard.writeText(text)
    toastStore.show('✅ 已复制到剪贴板', 'success')
  } catch { toastStore.show('复制失败，请手动复制', 'error') }
}

const currentVoiceName = computed(() => {
  if (activeSection.value?.voiceId) {
    return activeSection.value.voiceId === selectedVoice.value.voiceId
      ? selectedVoice.value.name : activeSection.value.voiceId
  }
  return selectedVoice.value.name
})

// ==================== 数据加载 ====================

const loadProject = async () => {
  try {
    project.value = await studioApi.getProject(projectId.value)
    if (!project.value) { toastStore.show('项目不存在'); router.push('/studio'); return }
    editableInspiration.value = project.value.inspiration || ''
    if (project.value.inspiration && project.value.status === 'draft') userInput.value = project.value.inspiration
  } catch (e) { toastStore.show('加载项目失败') }
}

/** 保存编辑后的灵感描述 */
const saveInspiration = async () => {
  if (!project.value || editableInspiration.value === project.value.inspiration) return
  try {
    await studioApi.updateProject(projectId.value, { inspiration: editableInspiration.value })
    project.value.inspiration = editableInspiration.value
    toastStore.show('灵感已更新 ✨')
  } catch (e) { console.error('保存灵感失败:', e) }
}

const loadSections = async () => {
  try {
    sections.value = await studioApi.listSections(projectId.value)
    if (sections.value.length > 0 && !activeSection.value) {
      activeSection.value = sections.value[sections.value.length - 1]
    }
  } catch (e) { console.error('加载段落失败:', e) }
}

// ==================== AI 生成 ====================

const streamContentRef = ref(null)

const generate = async () => {
  let input = userInput.value.trim()
  // 第一段必须有灵感；续写可以留空 → AI 自动续写
  if (!input && sections.value.length === 0) {
    toastStore.show('请输入创作灵感'); return
  }
  if (!input && sections.value.length > 0) {
    // 自动续写：生成智能续写指令
    const unitName = mc.value?.unit || '章'
    input = `请续写第${sections.value.length + 1}${unitName}，承接上一${unitName}的剧情发展，推动角色关系和情节冲突`
  }
  // 如果用户填写了标题，将其作为续写指令的一部分
  const titleHint = chapterTitle.value.trim()
  // 情感电台：注入情绪氛围 + 字数硬性约束
  if (project.value?.typeCode === 'radio') {
    const moodHint = selectedMood.value?.promptHint || ''
    input = (moodHint ? moodHint + '\n' : '') + '【硬性要求】全文不得超过600字，这是铁律。\n\n' + input
  }
  // 知识讲解：注入教学风格修饰符
  if (project.value?.typeCode === 'lecture') {
    const levelHint = selectedLevel.value?.promptHint || ''
    input = (levelHint ? levelHint + '\n' : '') + '【硬性要求】全文不得超过600字，这是铁律。\n\n' + input
  }
  // 带货文案：注入产品名 + 文案风格修饰符
  if (project.value?.typeCode === 'ad') {
    const styleHint = selectedAdStyle.value?.promptHint || ''
    const productHint = adProductName.value?.trim() ? `【产品名称】${adProductName.value.trim()}\n` : ''
    input = productHint + (styleHint ? styleHint + '\n' : '') + '【硬性要求】全文不得超过600字，这是铁律。\n\n' + input
  }
  // 有声绘本：注入适龄范围修饰符
  if (project.value?.typeCode === 'picture_book') {
    const ageHint = selectedAge.value?.promptHint || ''
    input = (ageHint ? ageHint + '\n' : '') + input
  }
  // 新闻播报：注入播报风格修饰符
  if (project.value?.typeCode === 'news') {
    const newsHint = selectedNewsStyle.value?.promptHint || ''
    input = (newsHint ? newsHint + '\n' : '') + '【硬性要求】全文不得超过600字。\n\n' + input
  }
  const fullInput = titleHint
    ? `【章节标题：${titleHint}】${input}`
    : input
  generating.value = true; streamContent.value = ''; justFinished.value = false
  genStartTime.value = Date.now(); elapsedTime.value = 0
  elapsedTimer = setInterval(() => { elapsedTime.value = Math.round((Date.now() - genStartTime.value) / 1000) }, 500)
  // 自动滚动到输出区域
  nextTick(() => {
    streamOutputRef.value?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  })

  const controller = new AbortController()
  const timeout = setTimeout(() => controller.abort(), 60000)

  try {
    const resp = await studioApi.generateContent(projectId.value, fullInput)
    const reader = resp.body.getReader(); const decoder = new TextDecoder()
    controller.signal.addEventListener('abort', () => { try { reader.cancel() } catch {} })

    while (true) {
      const { value, done } = await reader.read(); if (done) break
      clearTimeout(timeout)
      const chunk = decoder.decode(value, { stream: true })
      for (const line of chunk.split('\n')) {
        if (line.startsWith('data:')) {
          let text = line.substring(5)
          // 情感电台：实时过滤 markdown 标记（## 标题、** 加粗、* 斜体）
          if (project.value?.typeCode === 'radio') {
            text = text.replace(/^#{1,3}\s*/gm, '').replace(/\*{1,2}([^*]+)\*{1,2}/g, '$1')
          }
          streamContent.value += text
        }
      }
      // 自动滚动到最新内容
      nextTick(() => { const el = streamContentRef.value; if (el) el.scrollTop = el.scrollHeight })
    }
    userInput.value = ''; chapterTitle.value = ''; await loadSections(); await loadProject()
    // 自动展开新章节
    if (sections.value.length > 0) {
      const lastSection = sections.value[sections.value.length - 1]
      expandedSections.add(lastSection.id)
      activeSection.value = lastSection
    }
    justFinished.value = true
    toastStore.show(`创作完成 ✨ ${streamContent.value.length}字 · ${elapsedTime.value}s`)
  } catch (e) {
    const msg = e.name === 'AbortError' ? '请求超时，请重试' : (e.message || '网络错误')
    toastStore.show('生成失败: ' + msg)
  }
  finally { clearTimeout(timeout); clearInterval(elapsedTimer); generating.value = false }
}

// ==================== AI 改写 ====================

const rewrite = async (instruction) => {
  if (!activeSection.value) return
  generating.value = true; streamContent.value = ''; activeTab.value = 'create'

  const controller = new AbortController()
  const timeout = setTimeout(() => controller.abort(), 60000)

  try {
    const resp = await studioApi.rewriteSection(activeSection.value.id, instruction)
    const reader = resp.body.getReader(); const decoder = new TextDecoder()
    controller.signal.addEventListener('abort', () => { try { reader.cancel() } catch {} })
    while (true) {
      const { value, done } = await reader.read(); if (done) break
      clearTimeout(timeout)
      const chunk = decoder.decode(value, { stream: true })
      for (const line of chunk.split('\n')) { if (line.startsWith('data:')) streamContent.value += line.substring(5) }
    }
    await loadSections()
    activeSection.value = sections.value.find(s => s.id === activeSection.value.id) || null
    toastStore.show('改写完成 ✏️'); activeTab.value = 'content'
  } catch (e) {
    const msg = e.name === 'AbortError' ? '请求超时，请重试' : (e.message || '网络错误')
    toastStore.show('改写失败: ' + msg)
  }
  finally { clearTimeout(timeout); generating.value = false }
}

// ==================== 段落删除 ====================

const showSectionDeleteModal = (s) => {
  deletingSection.value = s
  showDeleteModal.value = true
}

const executeDeleteSection = async () => {
  if (!deletingSection.value) return
  try {
    await studioApi.deleteSection(deletingSection.value.id)
    expandedSections.delete(deletingSection.value.id)
    showDeleteModal.value = false
    toastStore.show('已删除 🗑️')
    await loadSections()
    activeSection.value = sections.value.length > 0 ? sections.value[sections.value.length - 1] : null
    if (!activeSection.value) activeTab.value = 'create'
  } catch (e) { toastStore.show('删除失败: ' + (e.message || '网络错误')) }
  finally { deletingSection.value = null }
}

// 章节折叠/展开
const toggleSection = (s) => {
  if (expandedSections.has(s.id)) {
    expandedSections.delete(s.id)
  } else {
    expandedSections.add(s.id)
  }
  activeSection.value = s
}

// ==================== 音色绑定 ====================

const onVoiceSelect = async (voice) => {
  // 广播剧模式：分配给特定角色
  if (dramaVoicePickerFor.value && isDramaType.value) {
    const char = dramaVoicePickerFor.value
    characterVoiceMap.value[char] = voice.voiceId
    characterVoiceNames.value[char] = voice.name
    dramaVoicePickerFor.value = ''
    toastStore.show(`🎭 ${char} → ${voice.name}`)
    return
  }
  // 通用模式：全局音色绑定
  selectedVoice.value = voice
  if (activeSection.value) {
    activeSection.value.voiceId = voice.voiceId
    try {
      await studioApi.saveSection({ id: activeSection.value.id, projectId: activeSection.value.projectId, sectionIndex: activeSection.value.sectionIndex, title: activeSection.value.title, content: activeSection.value.content, voiceId: voice.voiceId, status: activeSection.value.status })
      toastStore.show(`已绑定: ${voice.name} 🎤`)
    } catch (e) { toastStore.show('保存失败') }
  }
}

// ==================== TTS 合成 ====================

/** 智能分片：按句子边界切分，每片 ≤ 280 字（留 20 字给语气指令前缀） */
const splitTextForTTS = (text, maxLen = 280) => {
  if (!text || text.length <= maxLen) return [text]
  const chunks = []
  let remaining = text
  while (remaining.length > 0) {
    if (remaining.length <= maxLen) { chunks.push(remaining); break }
    // 在 maxLen 范围内找最后一个句子分隔符
    let cutPos = -1
    const delimiters = ['。', '！', '？', '…', '\n', '；', '，']
    for (const d of delimiters) {
      const idx = remaining.lastIndexOf(d, maxLen)
      if (idx > cutPos) cutPos = idx
    }
    if (cutPos <= 0) cutPos = maxLen // 找不到标点则硬切
    const chunk = remaining.substring(0, cutPos + 1).trim()
    if (chunk) chunks.push(chunk)
    remaining = remaining.substring(cutPos + 1).trim()
  }
  return chunks.filter(c => c.length > 0)
}

/** 调用 TTS 合成单个文本片段 */
const synthesizeChunk = async (text, voiceId) => {
  // 对所有类型：清理非朗读元素（角色名前缀、音效标注、markdown）
  const cleanText = text
    .replace(/^##\s*.+$/gm, '')         // 移除 ##标题
    .replace(/^\[.+\]$/gm, '')          // 移除 [音效] 行
    .replace(/^(.{1,10})[：:]\s*/gm, '') // 移除 角色名：前缀
    .replace(/[（(].+?[）)]/g, '')       // 移除 (动作描写)
    .replace(/\*\*(.+?)\*\*/g, '$1')    // 移除 **粗体**
    .trim()
  if (!cleanText) return null

  if (selectedEngine.value === 'tts-2.0') {
    const finalText = emotionInstruction.value
      ? `[#用${emotionInstruction.value}的语气] ${cleanText}`
      : cleanText
    const data = await request.post('/tts/v2/synthesize', {
      text: finalText,
      voiceType: voiceId,
      mode: emotionInstruction.value ? 'voice_command' : 'default',
      userKey: authStore.user?.id?.toString() || 'anonymous'
    })
    return data?.audioUrl
  } else {
    const res = await ttsApi.synthesizeShort({ text: cleanText, voiceId })
    return res?.audioUrl
  }
}

const synthesizeSection = async (section) => {
  if (!section.content) { toastStore.show('段落没有内容'); return }
  // 防重复合成：已有音频时需确认
  if (section.audioUrl) {
    if (!confirm(`「${section.title || '当前段'}」已有配音，重新合成会消耗额度，确定吗？`)) return
  }
  const voiceId = section.voiceId || selectedVoice.value.voiceId
  synthesizingSection.value = true
  try {
    // 情感电台：更小的分片（~100字）让首片更快就绪，边听边合成
    const maxLen = ['radio','lecture','ad','picture_book','news'].includes(project.value?.typeCode) ? 100 : 280
    const chunks = splitTextForTTS(section.content, maxLen)
    synthChunkTotal.value = chunks.length
    synthChunkProgress.value = 0

    const audioUrls = []
    for (let i = 0; i < chunks.length; i++) {
      synthChunkProgress.value = i + 1
      const url = await synthesizeChunk(chunks[i], voiceId)
      if (url) {
        audioUrls.push(url)
        if (i === 0) {
          playerStore.play({ title: section.title || '合成中...', author: project.value?.title || '创作工作台', url })
          if (chunks.length > 1) toastStore.show(`✅ 第1片已就绪，边听边合成剩余 ${chunks.length - 1} 片...`)
        } else {
          playerStore.enqueue(url)
        }
      }
    }

    if (audioUrls.length > 0) {
      // 多片拼接为单文件
      let audioUrl
      if (audioUrls.length > 1) {
        synthChunkProgress.value = chunks.length  // 显示“拼接中”
        synthChunkTotal.value = chunks.length + 1  // +1 for concat step
        const merged = await studioApi.concatAudio(audioUrls)
        audioUrl = merged
      } else {
        audioUrl = audioUrls[0]
      }
      section.audioUrl = audioUrl; section.voiceId = voiceId; section.status = 'synthesized'
      await studioApi.saveSection({ id: section.id, projectId: section.projectId, sectionIndex: section.sectionIndex, title: section.title, content: section.content, voiceId, audioUrl, status: 'synthesized' })
      await loadSections()
      // 合成完成后自动展开该段落，方便查阅和播放
      singleExpandedMap[section.id] = true
      toastStore.show('合成完成 🎧 可查阅内容或发布')
    } else {
      toastStore.show('合成失败：未返回音频')
    }
  } catch (e) { toastStore.show('合成失败: ' + (e.message || '网络错误')) }
  finally {
    synthesizingSection.value = false
    synthChunkProgress.value = 0
    synthChunkTotal.value = 0
  }
}

const unsynthesizedCount = computed(() => sections.value.filter(s => s.content && !s.audioUrl).length)

const synthesizeAll = async () => {
  if (sections.value.length === 0) return
  const pending = sections.value.filter(s => s.content && !s.audioUrl)
  if (pending.length === 0) {
    toastStore.show('所有段落已配音，无需重复合成 ✅')
    return
  }
  const skipped = sections.value.length - pending.length
  if (skipped > 0) {
    toastStore.show(`跳过 ${skipped} 个已配音段落，合成剩余 ${pending.length} 段`)
  }
  const voiceId = selectedVoice.value.voiceId
  synthesizingAll.value = true; synthProgress.value = 0
  for (const section of pending) {
    try {
      const v = section.voiceId || voiceId
      const maxLen = ['radio','lecture','ad','picture_book','news'].includes(project.value?.typeCode) ? 100 : 280
      const chunks = splitTextForTTS(section.content, maxLen)
      const audioUrls = []
      for (const chunk of chunks) {
        const url = await synthesizeChunk(chunk, v)
        if (url) audioUrls.push(url)
      }
      if (audioUrls.length > 0) {
        const audioUrl = audioUrls.length > 1
          ? await studioApi.concatAudio(audioUrls)
          : audioUrls[0]
        section.audioUrl = audioUrl; section.voiceId = v; section.status = 'synthesized'
        await studioApi.saveSection({ id: section.id, projectId: section.projectId, sectionIndex: section.sectionIndex, title: section.title, content: section.content, voiceId: v, audioUrl, status: 'synthesized' })
      }
    } catch (e) { console.error(`段落合成失败:`, e) }
    synthProgress.value++
  }
  synthesizingAll.value = false
  toastStore.show(`全部合成完成 🎉`); await loadSections()
}

/** TTS 2.0 情感合成 —— 单次最大 1000 字，超出自动分片 */
const synthesizeEmotionV2 = async () => {
  if (!activeSection.value?.content) { toastStore.show('段落没有内容'); return }
  if (activeSection.value.audioUrl) {
    if (!confirm(`「${activeSection.value.title || '当前段'}」已有配音，重新合成会消耗额度，确定吗？`)) return
  }
  const voiceId = activeSection.value.voiceId || selectedVoice.value.voiceId
  synthesizingSection.value = true
  try {
    const chunks = splitTextForTTS(activeSection.value.content, 1000)
    synthChunkTotal.value = chunks.length
    synthChunkProgress.value = 0

    // 边合成边播放
    const audioUrls = []
    for (let i = 0; i < chunks.length; i++) {
      synthChunkProgress.value = i + 1
      const finalText = emotionInstruction.value
        ? `[#用${emotionInstruction.value}的语气] ${chunks[i]}`
        : chunks[i]
      const data = await request.post('/tts/v2/synthesize', {
        text: finalText,
        voiceType: voiceId,
        mode: emotionInstruction.value ? 'voice_command' : 'default',
        userKey: authStore.user?.id?.toString() || 'anonymous'
      })
      if (data?.audioUrl) {
        audioUrls.push(data.audioUrl)
        if (i === 0) {
          // 第一片合成完，立即播放
          playerStore.play({
            title: activeSection.value.title || '情感合成中...',
            author: project.value?.title || '创作工作台',
            url: data.audioUrl
          })
          toastStore.show(`✅ 第1片已就绪，边听边合成剩余 ${chunks.length - 1} 片...`)
        } else {
          playerStore.enqueue(data.audioUrl)
        }
      }
    }

    if (audioUrls.length > 0) {
      let audioUrl
      if (audioUrls.length > 1) {
        synthChunkProgress.value = chunks.length
        synthChunkTotal.value = chunks.length + 1
        audioUrl = await studioApi.concatAudio(audioUrls)
      } else {
        audioUrl = audioUrls[0]
      }
      activeSection.value.audioUrl = audioUrl
      activeSection.value.voiceId = voiceId
      activeSection.value.status = 'synthesized'
      await studioApi.saveSection({
        id: activeSection.value.id,
        projectId: activeSection.value.projectId,
        sectionIndex: activeSection.value.sectionIndex,
        title: activeSection.value.title,
        content: activeSection.value.content,
        voiceId, audioUrl, status: 'synthesized'
      })
      await loadSections()
      toastStore.show('🎭 情感合成完成 🎧')
    } else {
      toastStore.show('合成失败：未返回音频')
    }
  } catch (e) { toastStore.show('情感合成失败: ' + (e.message || '网络错误')) }
  finally {
    synthesizingSection.value = false
    synthChunkProgress.value = 0
    synthChunkTotal.value = 0
  }
}

// ==================== 引擎切换 ====================

const switchEngine = (engine) => {
  if (engine === 'tts-2.0') {
    if (!authStore.hasFeature('tts_emotion_v2')) {
      toastStore.show('情感配音功能未开放，请联系管理员开通 🎭')
      return
    }
    // 切换到 2.0 情感音色默认值
    selectedVoice.value = { voiceId: 'zh_female_vv_uranus_bigtts', name: 'vivi 2.0' }
  } else {
    // 切换到 1.0 基础音色默认值
    selectedVoice.value = { voiceId: 'BV700_streaming', name: '灿灿' }
  }
  // 同步更新当前段落的音色，避免显示旧引擎的 voiceId
  if (activeSection.value) {
    activeSection.value.voiceId = selectedVoice.value.voiceId
  }
  selectedEngine.value = engine
  toastStore.show(engine === 'tts-2.0' ? '已切换到情感配音引擎 🎭 请选择情感音色' : '已切换到基础配音引擎 🔊')
}

// ==================== 智能情感标注 ====================

const autoEmotionTag = async () => {
  if (!activeSection.value?.content) { toastStore.show('请先选择有内容的段落'); return }
  autoTagging.value = true
  try {
    const data = await request.post('/tts/v2/smart-tone', {
      content: activeSection.value.content.substring(0, 500),
      moduleType: project.value?.typeCode || 'general'
    })
    if (data && data.instruction) {
      emotionInstruction.value = data.instruction
      const src = data.source === 'rag+llm' ? '知识库+AI' : 'AI分析'
      const conf = data.confidence ? Math.round(data.confidence * 100) + '%' : ''
      toastStore.show(`已智能匹配：${data.instruction} (${src} ${conf}) 🎭`)
    } else {
      // fallback to old API
      const text = await request.post('/tts/v2/enhance-tags', { text: activeSection.value.content.substring(0, 300) })
      if (text) {
        const match = String(text).match(/\[#用(.+?)的语气\]/)
        if (match) {
          emotionInstruction.value = match[1]
          toastStore.show(`已智能匹配语气：${match[1]} 🎭`)
        } else {
          toastStore.show('未能识别情感，请手动输入语气指令')
        }
      }
    }
  } catch (e) { toastStore.show('智能情感分析失败: ' + (e.message || '网络错误')) }
  finally { autoTagging.value = false }
}

// ==================== 播放 & 发布 ====================

const playAudio = (section) => {
  if (!section.audioUrl) return
  playerStore.play({
    title: section.title || '创作段落',
    author: project.value?.title || '创作工作台',
    url: section.audioUrl
  })
}

const publishProject = async () => {
  publishing.value = true
  try {
    const res = await studioApi.publishProject(projectId.value)
    toastStore.show(res.message || '发布成功 🎉')
    await loadProject()
  } catch (e) {
    console.error('发布失败:', e)
    toastStore.show('发布失败: ' + (e.response?.data?.message || e.message || '网络错误'))
  } finally {
    publishing.value = false
  }
}

const unpublishProject = async () => {
  publishing.value = true
  try {
    const res = await studioApi.unpublishProject(projectId.value)
    toastStore.show(res.message || '已下架')
    await loadProject()
  } catch (e) {
    console.error('下架失败:', e)
    toastStore.show('下架失败: ' + (e.response?.data?.message || e.message || '网络错误'))
  } finally {
    publishing.value = false
  }
}

// ==================== 工具函数 ====================

const getIcon = (typeCode) => ({ novel: '📖', drama: '🎭', podcast: '🎙️', radio: '🌙', lecture: '📚', ad: '🛒', picture_book: '🎨', news: '📰' }[typeCode] || '📝')
const statusLabel = (s) => ({ draft: '草稿', creating: 'AI创作中', editing: '编辑中', completed: '已完成' }[s] || s || '')
const statusBadge = (s) => ({ draft: 'bg-gray-500/20 text-gray-400', creating: 'bg-orange-500/20 text-orange-400', editing: 'bg-blue-500/20 text-blue-400', completed: 'bg-green-500/20 text-green-400' }[s] || 'bg-white/10 text-gray-400')

onMounted(async () => {
  loading.value = true
  await Promise.all([loadProject(), loadSections()])
  loading.value = false
  
  // ═══ 小说模式：恢复保存的大纲 + 细纲状态 ═══
  if (project.value?.typeCode === 'novel' && project.value.outline) {
    try {
      const saved = typeof project.value.outline === 'string' 
        ? JSON.parse(project.value.outline) 
        : project.value.outline
      if (saved.synopsis) {
        masterOutline.value = {
          synopsis: saved.synopsis || '',
          characters: saved.characters || [],
          chapters: saved.chapters || []
        }
        // 恢复细纲数组
        if (saved.chapterOutlines && saved.chapterOutlines.length > 0) {
          chapterOutlines.value = saved.chapterOutlines
        } else {
          chapterOutlines.value = masterOutline.value.chapters.map(() => ({
            plot: '', keyEvents: '', characters: '', foreshadowing: ''
          }))
        }
        // 恢复 stylePreference（如果大纲中保存了）
        if (saved.stylePreference) {
          stylePreference.value = saved.stylePreference
        }
        // 自动跳转到合适的步骤
        const allGenerated = sections.value.length >= chapterOutlines.value.length && chapterOutlines.value.length > 0
        if (allGenerated) {
          novelStep.value = 'generate'
        } else if (sections.value.length > 0) {
          novelStep.value = 'generate'
        } else if (chapterOutlines.value.some(co => co.plot)) {
          novelStep.value = 'chapter_outline'
        } else {
          novelStep.value = 'outline'
        }
      }
    } catch (e) {
      console.warn('恢复小说大纲失败:', e)
    }
  }

  // ═══ 广播剧模式：恢复已有内容状态 ═══
  if (project.value?.typeCode === 'drama' && sections.value.length > 0) {
    dramaFinished.value = true
    activeSection.value = sections.value[0]
    // 恢复内容到 dramaStreamContent 以显示格式化阅读区
    if (activeSection.value?.content) {
      dramaStreamContent.value = activeSection.value.content
      dramaShowFull.value = true // 自动展开全文
      // 自动解析角色列表（静默，不弹 toast）
      try {
        const data = await studioApi.parseScript(activeSection.value.id)
        dramaCharacters.value = data.characters || []
        dramaLines.value = data.lines || []
        for (const char of dramaCharacters.value) {
          if (!characterVoiceMap.value[char]) {
            characterVoiceMap.value[char] = selectedVoice.value.voiceId
            characterVoiceNames.value[char] = selectedVoice.value.name
          }
          if (!characterEmotionMap.value[char]) {
            characterEmotionMap.value[char] = ''
          }
        }
      } catch (e) { console.warn('自动解析角色失败:', e) }
    }
    // 自动匹配题材
    if (!stylePreference.value && project.value.inspiration) {
      const insp = project.value.inspiration
      const match = dramaGenres.find(g => g.inspiration && insp.includes(g.inspiration.substring(0, 6)))
      if (match) stylePreference.value = match.id
      else stylePreference.value = dramaGenres[0]?.id
    }
    // 初始化时 watcher 会误触发 settingsChanged，需要 nextTick 重置
    nextTick(() => { dramaSettingsChanged.value = false })
  }

  // ═══ 小说模式：根据灵感描述自动匹配题材（避免重复选择） ═══
  if (project.value?.typeCode === 'novel' && !stylePreference.value && project.value.inspiration) {
    const insp = project.value.inspiration
    // Studio.vue 创建时会将 genre.inspiration 填入 project.inspiration
    // 通过关键词匹配找到对应的 genre 并自动选中
    const genreKeywords = {
      heroine:    ['拒绝恋爱脑', '商业帝国', '大女主'],
      mindgame:   ['死亡游戏', '智商', '破局之法'],
      brainstorm: ['弹幕系统', '穿越到古代宅斗', '脑洞'],
      xianxia:    ['KPI考核', '修仙界', '宗门'],
      crazy:      ['发疯', '外耗别人', '摆烂发疯'],
      historical: ['典籍知识', '周礼', '考据'],
      vintage:    ['80年代', '改革开放', '小镇青年']
    }
    let matchedId = null
    for (const [id, keywords] of Object.entries(genreKeywords)) {
      if (keywords.some(kw => insp.includes(kw))) {
        matchedId = id
        break
      }
    }
    if (matchedId) {
      stylePreference.value = matchedId
    }
  }
  
  // 有内容时设置 activeSection（但不覆盖小说模式的 Tab）
  if (sections.value.length > 0) {
    if (!isStructuredMode.value) {
      activeTab.value = 'content'
    }
    expandedSections.add(sections.value[0].id)
    activeSection.value = sections.value[0]
  }
  // 异步加载情感预设标签
  loadEmotionPresets()
})
</script>

<style scoped>
.hide-scrollbar { -ms-overflow-style: none; scrollbar-width: none; }
.hide-scrollbar::-webkit-scrollbar { display: none; }
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
