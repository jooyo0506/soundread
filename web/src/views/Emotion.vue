<template>
  <div class="h-full bg-[#050505] flex flex-col relative hide-scrollbar overflow-hidden">
    <!-- 背景氛围光斑动效 -->
    <div class="absolute -top-[10%] -left-[10%] w-[50%] h-[30%] bg-[#FF9500]/20 blur-[100px] rounded-full pointer-events-none animate-pulse-slow"></div>
    <div class="absolute top-[40%] -right-[20%] w-[60%] h-[40%] bg-blue-500/10 blur-[120px] rounded-full pointer-events-none animate-float"></div>

    <div class="h-14 flex justify-between items-center px-4 bg-transparent z-40 mt-8">
      <button @click="$router.back()" class="text-white w-8 h-8 flex justify-center items-center cursor-pointer transition-transform hover:scale-110"><i class="fas fa-chevron-left text-sm"></i></button>
      <h1 class="text-white font-bold text-base tracking-wider drop-shadow-md">情感朗读</h1>
      <button @click="openLibrary" class="text-xs font-bold text-gray-400 hover:text-white transition-colors cursor-pointer">创作库</button>
    </div>

    <div class="flex-1 overflow-y-auto px-5 pt-4 pb-32 hide-scrollbar z-10 relative">
      
      <!-- ========== 双模式切换器 ========== -->
      <div class="flex items-center gap-2 mb-5">
        <button 
          @click="creativeMode = 'quick'"
          class="flex-1 py-3 rounded-2xl text-xs font-black tracking-wider transition-all duration-300 flex items-center justify-center gap-2 border"
          :class="creativeMode === 'quick' 
            ? 'bg-gradient-to-br from-[#FF9500]/20 to-[#FFD60A]/10 border-[#FF9500]/40 text-[#FF9500] shadow-[0_0_20px_rgba(255,149,0,0.1)]' 
            : 'bg-white/[0.02] border-white/5 text-gray-500 hover:text-gray-300 hover:border-white/10'"
        >
          <i class="fas fa-bolt"></i> 快速配音
        </button>
        <button 
          @click="creativeMode = 'director'"
          class="flex-1 py-3 rounded-2xl text-xs font-black tracking-wider transition-all duration-300 flex items-center justify-center gap-2 border"
          :class="creativeMode === 'director' 
            ? 'bg-gradient-to-br from-blue-500/20 to-purple-500/10 border-blue-500/40 text-blue-400 shadow-[0_0_20px_rgba(59,130,246,0.1)]' 
            : 'bg-white/[0.02] border-white/5 text-gray-500 hover:text-gray-300 hover:border-white/10'"
        >
          <i class="fas fa-film"></i> 剧情导演
        </button>
      </div>

      <!-- ========== ⚡ 快速配音模式 ========== -->
      <div v-if="creativeMode === 'quick'" class="mb-5">
        <div class="relative group">
          <div class="absolute inset-0 bg-gradient-to-br from-[#FF9500]/5 to-transparent rounded-2xl blur-sm transition-opacity group-focus-within:opacity-100 opacity-50"></div>
          <div class="relative glass-panel rounded-2xl p-4 border border-white/5 border-t-white/10 shadow-lg transition-all group-focus-within:border-[#FF9500]/30">
            <div class="flex items-center justify-between mb-3">
              <div class="flex items-center gap-2">
                <div class="w-6 h-6 rounded-lg bg-[#FF9500]/20 flex items-center justify-center text-[#FF9500] text-[10px] shadow-inner ring-1 ring-white/10"><i class="fas fa-magic"></i></div>
                <span class="text-[11px] font-black tracking-wider text-[#FF9500] opacity-90">语气指令</span>
              </div>
              <button v-if="instruction" @click="instruction = ''" class="w-6 h-6 flex items-center justify-center text-[10px] text-gray-500 hover:text-red-400 bg-white/5 hover:bg-white/10 rounded-full transition-colors border border-white/5"><i class="fas fa-times"></i></button>
            </div>
            <textarea 
              v-model="instruction"
              class="w-full bg-transparent text-[#FFD60A] placeholder-[#FF9500]/30 outline-none resize-none text-sm font-medium leading-relaxed custom-scrollbar" 
              rows="1" 
              placeholder="例如：用极度悲伤且带点哭腔的声音读..."
            ></textarea>
            
            <!-- 热门语气速选 -->
            <div class="flex flex-wrap items-center gap-2 mt-3 pt-3 border-t border-white/5">
              <button @click="instruction = '用暧昧低沉、若即若离的语气，像在耳边轻声说情话'; sceneTheme = '暧昧撩人'" class="px-2.5 py-1 rounded-md bg-pink-500/10 border border-pink-500/20 text-pink-400 text-[10px] hover:bg-pink-500/20 transition-colors flex items-center gap-1.5 shadow-sm">
                <i class="fas fa-heart text-pink-400"></i> 暧昧撩人
              </button>
              <button @click="instruction = '用气急败坏、越说越激动的语气，像和人当面吵架一样'; sceneTheme = '吵架怼人'" class="px-2.5 py-1 rounded-md bg-red-500/10 border border-red-500/20 text-red-400 text-[10px] hover:bg-red-500/20 transition-colors flex items-center gap-1.5 shadow-sm">
                <i class="fas fa-fire text-red-400"></i> 吵架怼人
              </button>
              <button @click="instruction = '用四川话、慵懒随意的方言语气'; sceneTheme = '四川话'" class="px-2.5 py-1 rounded-md bg-green-500/10 border border-green-500/20 text-green-400 text-[10px] hover:bg-green-500/20 transition-colors flex items-center gap-1.5 shadow-sm">
                <i class="fas fa-pepper-hot text-green-400"></i> 四川话
              </button>
              <button @click="instruction = '用东北话、豪爽热情大嗓门的语气，语速快'; sceneTheme = '东北话'" class="px-2.5 py-1 rounded-md bg-blue-500/10 border border-blue-500/20 text-blue-400 text-[10px] hover:bg-blue-500/20 transition-colors flex items-center gap-1.5 shadow-sm">
                <i class="fas fa-snowflake text-blue-400"></i> 东北话
              </button>
              <button @click="instruction = '用阴森恐怖、压低声线、若有若无的语气，像在讲鬼故事'; sceneTheme = '鬼故事'" class="px-2.5 py-1 rounded-md bg-purple-500/10 border border-purple-500/20 text-purple-400 text-[10px] hover:bg-purple-500/20 transition-colors flex items-center gap-1.5 shadow-sm">
                <i class="fas fa-ghost text-purple-400"></i> 鬼故事
              </button>
              <button @click="instruction = '用甜甜的、嗲嗲的、有点任性的撒娇语气'; sceneTheme = '撒娇卖萌'" class="px-2.5 py-1 rounded-md bg-[#FF9500]/10 border border-[#FF9500]/20 text-[#FF9500] text-[10px] hover:bg-[#FF9500]/20 transition-colors flex items-center gap-1.5 shadow-sm">
                <i class="fas fa-cat text-[#FFD60A]"></i> 撒娇
              </button>
              <button @click="instruction = '用霸道冷酷、不容置疑的语气，像霸道总裁一样'; sceneTheme = '霸总语录'" class="px-2.5 py-1 rounded-md bg-gray-500/10 border border-gray-500/20 text-gray-300 text-[10px] hover:bg-gray-500/20 transition-colors flex items-center gap-1.5 shadow-sm">
                <i class="fas fa-crown text-gray-300"></i> 霸总
              </button>
              <button @click="instruction = '用极轻极柔、像在耳边吹气的 ASMR 耳语'; sceneTheme = 'ASMR耳语'" class="px-2.5 py-1 rounded-md bg-cyan-500/10 border border-cyan-500/20 text-cyan-400 text-[10px] hover:bg-cyan-500/20 transition-colors flex items-center gap-1.5 shadow-sm">
                <i class="fas fa-headphones text-cyan-400"></i> ASMR
              </button>
            </div>

            <!-- 指令库展开（快速配音模式共享） -->
            <div class="mt-3 pt-3 border-t border-white/5">
              <button 
                @click="showPromptLibrary = !showPromptLibrary" 
                class="w-full py-1.5 flex items-center justify-center gap-2 text-xs font-bold text-[#FF9500]/70 hover:text-[#FF9500] hover:bg-[#FF9500]/10 rounded-lg transition-colors cursor-pointer"
              >
                <i class="fas fa-book-open"></i> {{ showPromptLibrary ? '收起指令库' : '更多语气 · 展开指令库' }} <i :class="showPromptLibrary ? 'fas fa-chevron-up' : 'fas fa-chevron-down'" class="text-[10px] ml-1"></i>
              </button>
              
              <div v-if="showPromptLibrary" class="mt-3 animate-fade-in">
                <div class="flex flex-wrap items-center gap-2 pb-2 mb-2 border-b border-white/5">
                  <button 
                    v-for="(cat, index) in promptLibrary" 
                    :key="'quick-cat-'+index"
                    @click="selectedCategory = index"
                    class="px-3 py-1.5 rounded-lg text-[11px] font-bold transition-all cursor-pointer flex items-center justify-center gap-1.5 flex-1 min-w-[30%] sm:min-w-fit sm:flex-none"
                    :class="selectedCategory === index ? 'bg-[#FF9500] text-black shadow-md' : 'bg-white/5 text-gray-400 hover:bg-white/10 hover:text-gray-200 border border-white/5'"
                  >
                    <i :class="cat.icon"></i> {{ cat.category }}
                  </button>
                </div>
                <div class="grid grid-cols-2 gap-2 mt-1 max-h-[160px] overflow-y-auto custom-scrollbar pr-1">
                  <div 
                    v-for="(role, rIndex) in promptLibrary[selectedCategory]?.roles" 
                    :key="'quick-role-'+rIndex"
                    @click="instruction = `用${role.tags}的语气`; sceneTheme = role.name"
                    class="bg-black/30 border border-white/5 hover:border-[#FF9500]/40 hover:bg-[#FF9500]/10 rounded-xl p-2.5 cursor-pointer transition-all group relative overflow-hidden"
                  >
                    <div class="absolute -right-2 -bottom-2 opacity-5 text-4xl group-hover:opacity-10 group-hover:scale-110 transition-all group-hover:text-[#FF9500] pointer-events-none">
                      <i :class="promptLibrary[selectedCategory]?.icon"></i>
                    </div>
                    <h4 class="text-xs font-bold text-gray-200 group-hover:text-white mb-1 drop-shadow-sm">{{ role.name }}</h4>
                    <p class="text-[9px] text-gray-500 group-hover:text-gray-400 leading-tight line-clamp-2" :title="role.description || role.desc">{{ role.description || role.desc }}</p>
                  </div>
                </div>
              </div>
            </div>

            <!-- 快速模式：一键 AI 写本（固定100字以内） -->
            <div class="mt-3 pt-3 border-t border-white/5 flex items-center justify-between">
              <span class="text-[9px] text-gray-500 flex items-center gap-1"><i class="fas fa-info-circle"></i> 快速配音 · 100字以内 · 单指令</span>
              <button 
                @click="generateScene" 
                :disabled="isGeneratingScene || !instruction"
                class="px-4 py-1.5 rounded-xl bg-gradient-to-r from-[#FF9500] to-[#FFD60A] text-black font-bold text-xs hover:shadow-[0_0_15px_rgba(255,149,0,0.4)] transition-all active:scale-95 disabled:opacity-50 disabled:grayscale disabled:cursor-not-allowed flex items-center gap-2"
              >
                <i v-if="isGeneratingScene" class="fas fa-spinner fa-spin"></i>
                <i v-else class="fas fa-magic"></i>
                {{ isGeneratingScene ? '构思中...' : '✨ AI 写本' }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- ========== 🎬 剧情导演模式 ========== -->
      <div v-if="creativeMode === 'director'" class="mb-5 space-y-4">
        
        <!-- 场景预设卡片（方案 B：一键填充上下文 + 匹配语气） -->
        <div class="relative">
          <div class="flex items-center gap-2 mb-2.5">
            <div class="w-5 h-5 rounded bg-purple-500/20 flex items-center justify-center text-purple-400 text-[9px]"><i class="fas fa-th-large"></i></div>
            <span class="text-[11px] font-black tracking-wider text-purple-400/80">场景快选</span>
            <span class="text-[9px] text-gray-600 ml-1">一键生成上文 + 语气</span>
          </div>
          <div class="grid grid-cols-2 gap-2">
            <button 
              v-for="(preset, idx) in scenePresets" :key="idx"
              @click="applyPreset(preset)"
              class="relative overflow-hidden rounded-xl p-3 border transition-all duration-300 cursor-pointer group text-left"
              :class="activePresetIdx === idx 
                ? 'bg-gradient-to-br border-blue-500/40 shadow-[0_0_15px_rgba(59,130,246,0.15)]' + ' ' + preset.activeBg
                : 'bg-white/[0.02] border-white/5 hover:border-white/15 hover:bg-white/[0.04]'"
              :style="activePresetIdx === idx ? '' : ''"
            >
              <div class="text-lg mb-1">{{ preset.emoji }}</div>
              <h4 class="text-[11px] font-bold text-gray-200 group-hover:text-white mb-0.5">{{ preset.name }}</h4>
              <p class="text-[9px] text-gray-500 group-hover:text-gray-400 leading-tight line-clamp-2">{{ preset.desc }}</p>
              <div v-if="activePresetIdx === idx" class="absolute top-2 right-2 w-4 h-4 rounded-full bg-blue-500 flex items-center justify-center"><i class="fas fa-check text-[8px] text-white"></i></div>
            </button>
          </div>
        </div>

        <!-- Step 1: 剧情上文 + AI 智能推荐语气按钮 -->
        <div class="relative group">
          <div class="absolute inset-0 bg-gradient-to-br from-blue-500/5 to-transparent rounded-2xl blur-sm transition-opacity group-focus-within:opacity-100 opacity-50"></div>
          <div class="relative glass-panel rounded-2xl p-4 border border-white/5 border-t-white/10 shadow-lg transition-all group-focus-within:border-blue-500/30">
            <div class="flex items-center gap-2 mb-2">
              <div class="w-6 h-6 rounded-lg bg-blue-500/20 flex items-center justify-center text-blue-400 text-[10px] shadow-inner ring-1 ring-white/10"><i class="fas fa-quote-left"></i></div>
              <span class="text-[11px] font-black tracking-wider text-blue-400 opacity-90">STEP 1 · 剧情上文</span>
              <button 
                v-if="contextText.trim()"
                @click="analyzeContext"
                :disabled="isAnalyzingMood"
                class="ml-auto px-2.5 py-1 text-[9px] font-bold rounded-lg transition-all flex items-center gap-1.5"
                :class="isAnalyzingMood 
                  ? 'bg-purple-500/20 text-purple-300 cursor-wait' 
                  : 'bg-gradient-to-r from-purple-500/20 to-blue-500/20 border border-purple-500/30 text-purple-300 hover:text-purple-200 hover:border-purple-400/40 hover:shadow-[0_0_10px_rgba(168,85,247,0.15)] cursor-pointer'"
              >
                <i :class="isAnalyzingMood ? 'fas fa-spinner fa-spin' : 'fas fa-brain'"></i>
                {{ isAnalyzingMood ? 'AI 分析中...' : '🧠 AI 推荐语气' }}
              </button>
            </div>
            <textarea 
              v-model="contextText"
              class="w-full bg-transparent text-cyan-200 placeholder-blue-400/30 outline-none resize-none text-sm font-medium leading-relaxed custom-scrollbar" 
              rows="2" 
              placeholder="输入前情提要。如：'她终于说出了这三年隐瞒的真相...'"
              maxlength="200"
            ></textarea>
          </div>
        </div>

        <!-- Step 2: 语气指令 -->
        <div class="relative group">
          <div class="absolute inset-0 bg-gradient-to-br from-[#FF9500]/5 to-transparent rounded-2xl blur-sm transition-opacity group-focus-within:opacity-100 opacity-50"></div>
          <div class="relative glass-panel rounded-2xl p-4 border border-white/5 border-t-white/10 shadow-lg transition-all group-focus-within:border-[#FF9500]/30">
            <div class="flex items-center justify-between mb-3">
              <div class="flex items-center gap-2">
                <div class="w-6 h-6 rounded-lg bg-[#FF9500]/20 flex items-center justify-center text-[#FF9500] text-[10px] shadow-inner ring-1 ring-white/10"><i class="fas fa-magic"></i></div>
                <span class="text-[11px] font-black tracking-wider text-[#FF9500] opacity-90">STEP 2 · 语气指令</span>
              </div>
              <button v-if="directorInstruction" @click="directorInstruction = ''" class="w-6 h-6 flex items-center justify-center text-[10px] text-gray-500 hover:text-red-400 bg-white/5 hover:bg-white/10 rounded-full transition-colors border border-white/5"><i class="fas fa-times"></i></button>
            </div>
            <textarea 
              v-model="directorInstruction"
              class="w-full bg-transparent text-[#FFD60A] placeholder-[#FF9500]/30 outline-none resize-none text-sm font-medium leading-relaxed custom-scrollbar" 
              rows="1" 
              placeholder="例如：用悲伤无奈的语气，像独自在雨中回忆的人"
            ></textarea>
            
            <!-- AI 推荐语气芯片（当有推荐结果时优先展示） -->
            <div v-if="aiMoodSuggestions.length" class="flex flex-wrap items-center gap-2 mt-3 pt-3 border-t border-purple-500/20">
              <span class="text-[9px] text-purple-400/60 font-bold tracking-wider w-full mb-1"><i class="fas fa-brain mr-1"></i>AI 推荐匹配语气：</span>
              <button 
                v-for="(mood, mIdx) in aiMoodSuggestions" :key="mIdx"
                @click="directorInstruction = mood.instruction; sceneTheme = mood.theme"
                class="px-2.5 py-1.5 rounded-lg text-[10px] font-bold transition-all cursor-pointer flex items-center gap-1.5 border"
                :class="directorInstruction === mood.instruction 
                  ? 'bg-gradient-to-r from-purple-500/30 to-blue-500/20 border-purple-500/40 text-purple-200 shadow-[0_0_8px_rgba(168,85,247,0.2)]' 
                  : 'bg-purple-500/10 border-purple-500/15 text-purple-300 hover:bg-purple-500/20 hover:border-purple-500/30'"
              >
                <span>{{ mood.emoji }}</span> {{ mood.label }}
              </button>
            </div>

            <!-- 默认快捷芯片（无 AI 推荐时显示） -->
            <div v-else class="flex flex-wrap items-center gap-2 mt-3 pt-3 border-t border-white/5">
              <button @click="directorInstruction = '用低沉深情、节奏舒缓的语气，像深夜电台主播一样'; sceneTheme = '深夜电台'" class="px-2.5 py-1 rounded-md bg-[#FF9500]/10 border border-[#FF9500]/20 text-[#FF9500] text-[10px] hover:bg-[#FF9500]/20 transition-colors flex items-center gap-1.5 shadow-sm">
                <i class="fas fa-radio text-gray-400"></i> 深夜电台
              </button>
              <button @click="directorInstruction = '用紧张神秘、压低声音、引人入胜的语气，像悬疑解说一样'; sceneTheme = '悬疑解说'" class="px-2.5 py-1 rounded-md bg-[#FF9500]/10 border border-[#FF9500]/20 text-[#FF9500] text-[10px] hover:bg-[#FF9500]/20 transition-colors flex items-center gap-1.5 shadow-sm">
                <i class="fas fa-search text-gray-400"></i> 悬疑解说
              </button>
              <button @click="directorInstruction = '用激情澎湃、抑扬顿挫的语气，像爽文男主旁白一样'; sceneTheme = '爽文小说旁白'" class="px-2.5 py-1 rounded-md bg-[#FF9500]/10 border border-[#FF9500]/20 text-[#FF9500] text-[10px] hover:bg-[#FF9500]/20 transition-colors flex items-center gap-1.5 shadow-sm">
                <i class="fas fa-crown text-[#FFD60A]"></i> 爽文旁白
              </button>
            </div>

            <!-- 指令库展开 -->
            <div class="mt-3 pt-3 border-t border-white/5">
              <button 
                @click="showPromptLibrary = !showPromptLibrary" 
                class="w-full py-1.5 flex items-center justify-center gap-2 text-xs font-bold text-[#FF9500]/70 hover:text-[#FF9500] hover:bg-[#FF9500]/10 rounded-lg transition-colors cursor-pointer"
              >
                <i class="fas fa-book-open"></i> {{ showPromptLibrary ? '收起指令库' : '展开指令库' }} <i :class="showPromptLibrary ? 'fas fa-chevron-up' : 'fas fa-chevron-down'" class="text-[10px] ml-1"></i>
              </button>
              
              <div v-if="showPromptLibrary" class="mt-3 animate-fade-in">
                <div class="flex flex-wrap items-center gap-2 pb-2 mb-2 border-b border-white/5">
                  <button 
                    v-for="(cat, index) in promptLibrary" 
                    :key="index"
                    @click="selectedCategory = index"
                    class="px-3 py-1.5 rounded-lg text-[11px] font-bold transition-all cursor-pointer flex items-center justify-center gap-1.5 flex-1 min-w-[30%] sm:min-w-fit sm:flex-none"
                    :class="selectedCategory === index ? 'bg-[#FF9500] text-black shadow-md' : 'bg-white/5 text-gray-400 hover:bg-white/10 hover:text-gray-200 border border-white/5'"
                  >
                    <i :class="cat.icon"></i> {{ cat.category }}
                  </button>
                </div>
                <div class="grid grid-cols-2 gap-2 mt-1 max-h-[160px] overflow-y-auto custom-scrollbar pr-1">
                  <div 
                    v-for="(role, rIndex) in promptLibrary[selectedCategory]?.roles" 
                    :key="rIndex"
                    @click="directorInstruction = `用${role.tags}的语气`; sceneTheme = role.name"
                    class="bg-black/30 border border-white/5 hover:border-[#FF9500]/40 hover:bg-[#FF9500]/10 rounded-xl p-2.5 cursor-pointer transition-all group relative overflow-hidden"
                  >
                    <div class="absolute -right-2 -bottom-2 opacity-5 text-4xl group-hover:opacity-10 group-hover:scale-110 transition-all group-hover:text-[#FF9500] pointer-events-none">
                      <i :class="promptLibrary[selectedCategory]?.icon"></i>
                    </div>
                    <h4 class="text-xs font-bold text-gray-200 group-hover:text-white mb-1 drop-shadow-sm">{{ role.name }}</h4>
                    <p class="text-[9px] text-gray-500 group-hover:text-gray-400 leading-tight line-clamp-2" :title="role.description || role.desc">{{ role.description || role.desc }}</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Step 3: 智能写本 -->
        <div class="flex items-center justify-end">
          <button 
            @click="generateScene" 
            :disabled="isGeneratingScene || !directorInstruction"
            class="px-5 py-2 rounded-xl bg-gradient-to-r from-blue-500 to-purple-500 text-white font-bold text-xs hover:shadow-[0_0_15px_rgba(59,130,246,0.4)] transition-all active:scale-95 disabled:opacity-50 disabled:grayscale disabled:cursor-not-allowed flex items-center gap-2"
          >
            <i v-if="isGeneratingScene" class="fas fa-spinner fa-spin"></i>
            <i v-else class="fas fa-magic"></i>
            {{ isGeneratingScene ? '构思中...' : '✨ 根据剧情 AI 写本' }}
          </button>
        </div>
      </div>

      <!-- ========== 共享主编辑器 ========== -->
      <div class="glass-panel rounded-3xl p-5 flex flex-col min-h-[260px] border border-white/10 border-t-white/20 shadow-2xl relative group focus-within:border-white/20 transition-all">
        
        <!-- 顶部工具栏 -->
        <div class="flex items-center justify-between mb-4 pb-3 border-b border-white/5">
          <button @click="showVoiceSelector = true" class="flex shrink-0 items-center justify-center px-3 py-1.5 rounded-lg bg-gradient-to-r from-white/10 to-transparent border border-white/10 hover:border-white/20 text-white text-[11px] font-bold cursor-pointer transition-all shadow-sm">
            <i class="fas fa-user-circle text-[#FF9500] mr-1.5 text-sm"></i>
            {{ selectedVoice?.name || '选择发音人' }}
          </button>
          
          <div class="flex items-center gap-2">
            <!-- P5: 一键清空 -->
            <button v-if="content.length > 0" @click="content = ''" class="w-6 h-6 flex items-center justify-center text-[10px] text-gray-500 hover:text-red-400 bg-white/5 hover:bg-white/10 rounded-full transition-colors border border-white/5 cursor-pointer" title="清空内容">
              <i class="fas fa-trash-alt"></i>
            </button>
          </div>
        </div>


        <!-- 核心输入域 -->
        <div class="relative w-full flex-1 mt-1 flex flex-col min-h-[120px]">
          <textarea 
            v-model="content"
            :maxlength="maxContentLength"
            class="w-full h-full absolute inset-0 bg-transparent text-white outline-none resize-none text-[16px] leading-relaxed placeholder-gray-600 custom-scrollbar z-10" 
            :placeholder="creativeMode === 'quick' ? '输入配音台词（100字以内）...' : '输入台词剧本（最多300字，支持多段情绪控制）...'"
          ></textarea>
        </div>
        
        <!-- 字数统计提示 -->
        <div class="absolute bottom-3 right-4 text-[10px] font-mono text-gray-500 flex items-center gap-1">
           <span :class="content.length >= maxContentLength ? 'text-red-400' : content.length > 0 ? 'text-[#FF9500]' : ''">{{ content.length }}</span>
           <span class="text-gray-600">/</span>
           <span>{{ maxContentLength }}</span>
        </div>
      </div>
    </div>

    <!-- 底部超维操作栏 -->
    <div class="absolute bottom-0 w-full p-5 bg-gradient-to-t from-black via-[#050505]/95 to-transparent z-40 pb-10">
      <div class="relative group">
        <!-- 按钮发光底座 -->
        <div v-if="!isSynthesizing" class="absolute -inset-1 bg-gradient-to-r from-[#FF9500] to-[#FFD60A] rounded-2xl blur opacity-30 group-hover:opacity-50 transition duration-1000 group-hover:duration-200"></div>
        <button 
          @click="synthesize"
          :disabled="isSynthesizing"
          class="relative w-full py-4 rounded-2xl font-bold text-[17px] shadow-2xl transition-all duration-300 flex justify-center items-center gap-2 cursor-pointer disabled:opacity-80 disabled:cursor-wait"
          :class="isSynthesizing ? 'bg-[#1a1a1a] text-[#FF9500] border border-[#FF9500]/30 shadow-[0_0_20px_rgba(255,149,0,0.2)]' : 'bg-gradient-to-r from-[#FF9500] to-[#FFD60A] text-black active:scale-[0.98] active:shadow-inner'"
        >
          <template v-if="isSynthesizing">
            <div class="audio-waves flex gap-1 mr-1">
              <span></span><span></span><span></span><span></span>
            </div>
            全维流式合成中...
          </template>
          <template v-else>
            <i class="fas fa-play text-sm"></i> 开始合成情感配音
          </template>
        </button>
      </div>
    </div>
    
    <!-- 音色选择弹窗抽屉 (引擎限定在 tts-2.0 情感模型) -->
    <VoiceSelector 
      v-model:visible="showVoiceSelector" 
      engine="tts-2.0" 
      :initialVoiceId="selectedVoice?.voiceId || 'zh_female_vv_uranus_bigtts'" 
      @select="selectedVoice = $event" 
    />

    <!-- ═══════════ 创作库全屏面板 ═══════════ -->
    <Teleport to="body">
      <Transition name="lib">
        <div v-if="libVisible" class="fixed inset-0 z-50 bg-[#0a0a0a] flex flex-col" style="max-width: 430px; margin: 0 auto;">
          <!-- 顶栏 -->
          <div class="px-4 mt-8 shrink-0">
            <div class="flex items-center gap-3 h-14">
              <button @click="libVisible = false" class="w-8 h-8 rounded-full bg-white/5 hover:bg-white/10 flex items-center justify-center cursor-pointer transition-all">
                <i class="fas fa-chevron-left text-xs text-gray-400"></i>
              </button>
              <h2 class="text-white font-bold text-base">创作库</h2>
              <div class="ml-auto flex items-center gap-3">
                <span class="text-[10px] text-gray-600">{{ libList.length }} 件作品</span>
              </div>
            </div>
            <!-- 统计条 -->
            <div v-if="libList.length > 0" class="flex items-center gap-4 pb-3 border-b border-white/5">
              <div class="flex items-center gap-1.5 text-[10px] text-gray-500">
                <i class="far fa-clock"></i>
                <span>总时长 {{ formatTotalDuration }}</span>
              </div>
              <div class="flex items-center gap-1.5 text-[10px] text-gray-500">
                <i class="fas fa-cloud-upload-alt"></i>
                <span>{{ publishedCount }} 已发布</span>
              </div>
            </div>
          </div>

          <!-- 列表 -->
          <div class="flex-1 overflow-y-auto px-4 pb-24 hide-scrollbar">
            <div v-if="libLoading" class="py-20 text-center">
              <i class="fas fa-circle-notch fa-spin text-[#FF9500] text-xl"></i>
            </div>

            <!-- 空状态 -->
            <div v-else-if="libList.length === 0" class="py-16 text-center">
              <div class="w-20 h-20 mx-auto mb-5 rounded-2xl bg-gradient-to-br from-[#FF9500]/10 to-[#FFD60A]/5 flex items-center justify-center">
                <i class="fas fa-microphone-alt text-3xl text-[#FF9500]/40"></i>
              </div>
              <p class="text-white text-sm font-bold mb-1">还没有作品</p>
              <p class="text-gray-600 text-[11px] mb-5">合成一段情感配音，它会出现在这里</p>
              <button @click="libVisible = false" class="px-6 py-2.5 rounded-full bg-gradient-to-r from-[#FF9500] to-[#FFD60A] text-black text-xs font-bold cursor-pointer hover:brightness-110 transition-all shadow-lg shadow-orange-500/20">
                <i class="fas fa-plus mr-1"></i> 开始创作
              </button>
            </div>

            <!-- 卡片列表 -->
            <div v-else class="space-y-2 mt-3">
              <div v-for="(item, idx) in libList" :key="item.id"
                   @click="playItem(item)"
                   class="rounded-xl overflow-hidden cursor-pointer group transition-all duration-200 hover:scale-[1.01]"
                   :class="isPlaying(item) ? 'bg-[#1a1500] border border-[#FF9500]/20' : 'bg-[#141414] border border-white/[0.04] hover:border-white/10'">
                <div class="flex items-start gap-3 p-3">
                  <!-- 播放按钮 / 正在播放动画 -->
                  <div class="w-10 h-10 shrink-0 rounded-xl flex items-center justify-center transition-all"
                       :class="isPlaying(item)
                         ? 'bg-[#FF9500] shadow-lg shadow-orange-500/30'
                         : 'bg-gradient-to-br from-[#FF9500]/15 to-[#FFD60A]/5 group-hover:from-[#FF9500]/25'">
                    <div v-if="isPlaying(item)" class="lib-waves">
                      <span></span><span></span><span></span>
                    </div>
                    <i v-else class="fas fa-play text-[#FF9500] text-[10px] pl-0.5" :class="isPlaying(item) ? 'text-black' : ''"></i>
                  </div>

                  <!-- 内容区 -->
                  <div class="flex-1 min-w-0">
                    <!-- 标题 -->
                    <h4 class="text-white text-xs font-bold truncate leading-tight">{{ smartTitle(item) }}</h4>
                    <!-- 台词预览（核心改进：显示实际内容） -->
                    <p v-if="item.inputText" class="text-[10px] text-gray-500 line-clamp-2 leading-relaxed mt-0.5">
                      「{{ item.inputText }}」
                    </p>
                    <!-- 元信息行 -->
                    <div class="flex items-center gap-2 mt-1.5 flex-wrap">
                      <span class="inline-flex items-center gap-0.5 text-[9px] text-gray-600">
                        <i class="far fa-clock"></i> {{ formatDuration(item.audioDuration) }}
                      </span>
                      <span class="inline-flex items-center gap-0.5 text-[9px] text-gray-600">
                        <i class="far fa-calendar"></i> {{ formatRelativeTime(item.createdAt) }}
                      </span>
                      <span v-if="item.voiceId" class="inline-flex items-center gap-0.5 text-[9px] text-[#FF9500]/50">
                        <i class="fas fa-user-circle"></i> {{ shortVoice(item.voiceId) }}
                      </span>
                    </div>
                  </div>

                  <!-- 右侧：状态 + 菜单 -->
                  <div class="flex flex-col items-end gap-1.5 shrink-0 pt-0.5">
                    <span v-if="item.isPublished === 1"
                          class="text-[7px] font-bold text-emerald-400 bg-emerald-500/10 px-1.5 py-0.5 rounded leading-none">已上架</span>
                    <button @click.stop="openAction(item)" class="w-6 h-6 rounded-full hover:bg-white/10 flex items-center justify-center cursor-pointer transition-colors">
                      <i class="fas fa-ellipsis-v text-[9px] text-gray-600"></i>
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- ═══════════ 操作菜单 ActionSheet ═══════════ -->
    <Teleport to="body">
      <Transition name="sheet">
        <div v-if="actionItem" class="fixed inset-0 z-[60] flex items-end justify-center" style="max-width: 430px; margin: 0 auto;">
          <div class="absolute inset-0 bg-black/60 backdrop-blur-sm" @click="actionItem = null"></div>
          <div class="relative w-full bg-[#1a1a1a] rounded-t-2xl border-t border-white/10 p-4 pb-8">
            <div class="w-8 h-1 rounded-full bg-white/10 mx-auto mb-4"></div>
            <h4 class="text-white text-sm font-bold truncate mb-1">{{ actionItem?.title || '未命名' }}</h4>
            <p class="text-[10px] text-gray-600 mb-4">管理你的创作</p>

            <div class="space-y-2">
              <!-- 编辑标题 -->
              <button @click="startRename" class="w-full flex items-center gap-3 px-4 py-3 rounded-xl bg-white/5 hover:bg-white/10 transition-colors cursor-pointer">
                <i class="fas fa-pen text-[#FF9500] text-sm w-5 text-center"></i>
                <div class="text-left"><div class="text-xs font-bold text-gray-300">编辑标题</div></div>
              </button>

              <!-- 发布/下架 -->
              <button v-if="actionItem?.isPublished !== 1" @click="publishItem" class="w-full flex items-center gap-3 px-4 py-3 rounded-xl bg-emerald-500/10 hover:bg-emerald-500/15 transition-colors cursor-pointer">
                <i class="fas fa-rocket text-emerald-400 text-sm w-5 text-center"></i>
                <div class="text-left">
                  <div class="text-xs font-bold text-emerald-400">发布到发现页</div>
                  <div class="text-[10px] text-gray-500">展示给其他用户</div>
                </div>
              </button>
              <button v-else @click="unpublishItem" class="w-full flex items-center gap-3 px-4 py-3 rounded-xl bg-amber-500/10 hover:bg-amber-500/15 transition-colors cursor-pointer">
                <i class="fas fa-eye-slash text-amber-400 text-sm w-5 text-center"></i>
                <div class="text-left">
                  <div class="text-xs font-bold text-amber-400">从发现页下架</div>
                  <div class="text-[10px] text-gray-500">不影响本地记录</div>
                </div>
              </button>

              <!-- 删除 -->
              <button @click="deleteItem" class="w-full flex items-center gap-3 px-4 py-3 rounded-xl bg-red-500/10 hover:bg-red-500/15 transition-colors cursor-pointer">
                <i class="fas fa-trash-alt text-red-400 text-sm w-5 text-center"></i>
                <div class="text-left">
                  <div class="text-xs font-bold text-red-400">删除</div>
                  <div class="text-[10px] text-gray-500">不可恢复，释放存储空间</div>
                </div>
              </button>

              <button @click="actionItem = null" class="w-full py-3 rounded-xl text-xs text-gray-500 hover:text-white transition-colors cursor-pointer text-center">
                取消
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- ═══════════ 重命名弹窗 ═══════════ -->
    <Teleport to="body">
      <Transition name="sheet">
        <div v-if="renameVisible" class="fixed inset-0 z-[70] flex items-center justify-center px-8" style="max-width: 430px; margin: 0 auto;">
          <div class="absolute inset-0 bg-black/70 backdrop-blur-sm" @click="renameVisible = false"></div>
          <div class="relative w-full bg-[#1c1c1c] rounded-2xl border border-white/10 p-5">
            <h4 class="text-white text-sm font-bold mb-3">编辑标题</h4>
            <input v-model="renameTitle" ref="renameInput" maxlength="30"
                   class="w-full px-4 py-3 rounded-xl bg-black/40 border border-white/10 text-white text-sm focus:outline-none focus:border-[#FF9500]/50 transition-colors placeholder-gray-600"
                   placeholder="输入新标题..." @keyup.enter="confirmRename" />
            <div class="flex gap-2 mt-4">
              <button @click="renameVisible = false" class="flex-1 py-2.5 rounded-xl bg-white/5 text-gray-400 text-xs font-bold cursor-pointer hover:bg-white/10 transition-colors">取消</button>
              <button @click="confirmRename" class="flex-1 py-2.5 rounded-xl bg-[#FF9500] text-black text-xs font-bold cursor-pointer hover:brightness-110 transition-all">确认</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { usePlayerStore } from '../stores/player'
import VoiceSelector from '../components/VoiceSelector.vue'
import { useToastStore } from '../stores/toast'
import { creationApi } from '../api/creation'
import request from '@/api/request'

const router = useRouter()
const route = useRoute()
const toastStore = useToastStore()
const authStore = useAuthStore()
const playerStore = usePlayerStore()

const creativeMode = ref('quick') // 'quick' 或 'director'
const instruction = ref('')
const contextText = ref('')
const content = ref('')
const isSynthesizing = ref(false)
const isEnhancing = ref(false)
const isGeneratingScene = ref(false)

// 导演模式：单指令控制
const directorInstruction = ref('')
const sceneTheme = ref('')

// 动态字数上限
const maxContentLength = computed(() => creativeMode.value === 'quick' ? 100 : 300)

// P1：模式切换时智能清理/迁移数据
watch(creativeMode, (newMode, oldMode) => {
  if (newMode === 'quick') {
    // 从导演切回快速：如果超100字智能截断
    if (content.value.length > 100) {
      const cut = content.value.lastIndexOf('。', 100)
      content.value = cut > 0 ? content.value.slice(0, cut + 1) : content.value.slice(0, 100)
      toastStore.show('已智能截断至100字以内')
    }
  }
  if (newMode === 'director') {
    // 从快速切到导演：迁移指令
    if (instruction.value && !directorInstruction.value) {
      directorInstruction.value = instruction.value
    }
  }
})

const selectedCategory = ref(0)
const showPromptLibrary = ref(false)

// ========== 场景预设卡片数据（方案 B：上文 + 语气天然配对） ==========
const activePresetIdx = ref(-1)
const scenePresets = ref([
  {
    emoji: '💔', name: '心碎失恋', desc: '被最亲的人欺骗后的崩溃独白',
    context: '她终于说出了这三年隐瞒的真相，而你还傻傻地以为一切都是真的',
    instruction: '用压抑克制、逐渐崩溃的语气，像独自在雨中回忆的人',
    theme: '情感独白', activeBg: 'from-pink-500/10 to-purple-500/5'
  },
  {
    emoji: '🔥', name: '爽文逆袭', desc: '被看不起后的霸气反击',
    context: '三年前你们把我从家族除名，今天我带着千亿市值的公司回来了',
    instruction: '用霸气低沉、步步压迫的语气，像王者归来俯视一切',
    theme: '爽文旁白', activeBg: 'from-red-500/10 to-orange-500/5'
  },
  {
    emoji: '🔫', name: '悬疑反转', desc: '所有线索指向最不可能的人',
    context: '监控画面中出现的那个身影，分明就是已经"死去"三年的她',
    instruction: '用紧张压迫、低沉克制的语气，像在黑暗中慢慢逼近真相',
    theme: '悬疑解说', activeBg: 'from-gray-500/10 to-blue-500/5'
  },
  {
    emoji: '🌙', name: '深夜治愈', desc: '夜深人静时和自己对话',
    context: '凌晨三点，城市安静下来，你终于有时间听听自己的心',
    instruction: '用温柔轻缓、充满治愈感的语气，像深夜电台主播在耳边低语',
    theme: '深夜电台', activeBg: 'from-blue-500/10 to-cyan-500/5'
  },
  {
    emoji: '⚔️', name: '热血战斗', desc: '决战前最后的宣言',
    context: '敌军已经兵临城下，身后是你誓死保护的人和家园',
    instruction: '用慷慨激昂、热血沸腾的语气，像战场上将军最后的战前动员',
    theme: '热血战斗', activeBg: 'from-orange-500/10 to-red-500/5'
  },
  {
    emoji: '😂', name: '吐槽日常', desc: '社畜内心的真实独白',
    context: '周一早上闹钟响了，你看了眼时间，还有五分钟迟到',
    instruction: '用轻快吐槽、自嘲幽默的语气，像和朋友吐槽的碎碎念',
    theme: '搞笑吐槽', activeBg: 'from-yellow-500/10 to-green-500/5'
  }
])

// ========== AI 智能推荐语气（方案 A） ==========
const isAnalyzingMood = ref(false)
const aiMoodSuggestions = ref([])

/** 选中场景预设卡片：一键填充上文 + 语气（保证情感一致性） */
const applyPreset = (preset) => {
  const idx = scenePresets.value.indexOf(preset)
  activePresetIdx.value = idx
  contextText.value = preset.context
  directorInstruction.value = preset.instruction
  sceneTheme.value = preset.theme
  aiMoodSuggestions.value = [] // 清空之前的 AI 推荐
}

/**
 * AI 分析剧情上文 → 推荐匹配的语气方向
 *
 * 先尝试调后端 AI 接口，失败则走前端关键词匹配兜底
 */
const analyzeContext = async () => {
  if (!contextText.value.trim()) return
  
  isAnalyzingMood.value = true
  aiMoodSuggestions.value = []
  
  const text = contextText.value
  
  // ======== 第一层：前端关键词快速匹配（秒出结果，不调 API） ========
  const suggestions = []
  
  if (/欺骗|背叛|谎言|出轨|分手|离开|失去|隐瞒|真相/.test(text)) {
    suggestions.push({ emoji: '💔', label: '心碎崩溃', instruction: '用压抑克制、逐渐崩溃的语气，像独自在雨中哭泣', theme: '情感独白' })
    suggestions.push({ emoji: '😢', label: '悲伤哽咽', instruction: '用低沉悲伤、带点哭腔的语气，像回忆过去的美好', theme: '情感独白' })
    suggestions.push({ emoji: '😤', label: '愤怒质问', instruction: '用愤怒压抑、咬牙切齿的语气，像终于爆发的沉默', theme: '情感爆发' })
  } else if (/复仇|回来|崛起|逆袭|看不起|除名|千亿|归来/.test(text)) {
    suggestions.push({ emoji: '👑', label: '霸气归来', instruction: '用霸气低沉、步步压迫的语气，像王者归来俯视一切', theme: '爽文旁白' })
    suggestions.push({ emoji: '🔥', label: '热血宣言', instruction: '用慷慨激昂、不可一世的语气，像站在巅峰宣告天下', theme: '爽文旁白' })
  } else if (/监控|线索|死去|消失|秘密|真凶|嫌疑/.test(text)) {
    suggestions.push({ emoji: '🔍', label: '紧张悬疑', instruction: '用紧张压迫、低沉克制的语气，像在黑暗中慢慢逼近真相', theme: '悬疑解说' })
    suggestions.push({ emoji: '😱', label: '惊恐发现', instruction: '用颤抖惊恐、不敢置信的语气，像发现了不该看到的东西', theme: '悬疑解说' })
  } else if (/深夜|凌晨|安静|孤独|想念|回忆/.test(text)) {
    suggestions.push({ emoji: '🌙', label: '深夜温柔', instruction: '用温柔轻缓、充满治愈感的语气，像深夜电台主播在耳边低语', theme: '深夜电台' })
    suggestions.push({ emoji: '💭', label: '内心独白', instruction: '用平静舒缓、带点感慨的语气，像和自己内心对话', theme: '深夜电台' })
  } else if (/战斗|敌人|保护|牺牲|战场|决战|兵临城下|誓死/.test(text)) {
    suggestions.push({ emoji: '⚔️', label: '热血激昂', instruction: '用慷慨激昂、热血沸腾的语气，像战场上的最后宣言', theme: '热血战斗' })
    suggestions.push({ emoji: '🛡️', label: '坚定守护', instruction: '用坚定沉稳、不容置疑的语气，像誓死守护的战士', theme: '热血战斗' })
  } else if (/搞笑|吐槽|迟到|闹钟|社畜|打工|老板|加班/.test(text)) {
    suggestions.push({ emoji: '😂', label: '吐槽自嘲', instruction: '用轻快吐槽、自嘲幽默的语气，像和朋友碎碎念', theme: '搞笑吐槽' })
    suggestions.push({ emoji: '🤦', label: '无奈叹气', instruction: '用无奈叹气、哭笑不得的语气，像社畜的日常独白', theme: '搞笑吐槽' })
  }
  
  // 关键词匹配到了 → 直接返回，不调 API
  if (suggestions.length > 0) {
    aiMoodSuggestions.value = suggestions
    directorInstruction.value = suggestions[0].instruction
    sceneTheme.value = suggestions[0].theme
    isAnalyzingMood.value = false
    return
  }
  
  // ======== 第二层：关键词未命中 → 调后端 AI 分析接口 ========
  try {
    const data = await request.post('/tts/v2/analyze-mood', {
      context: contextText.value
    })
    if (data && Array.isArray(data) && data.length > 0) {
      aiMoodSuggestions.value = data
      directorInstruction.value = data[0].instruction
      sceneTheme.value = data[0].theme
      isAnalyzingMood.value = false
      return
    }
  } catch (err) {
    console.warn('AI 语气分析失败，使用通用推荐', err)
  }
  
  // ======== 第三层：API 也失败了 → 通用推荐兜底 ========
  aiMoodSuggestions.value = [
    { emoji: '🎭', label: '深情演绎', instruction: '用深情饱满、情感丰富的语气，像在讲述一个打动人心的故事', theme: '情感独白' },
    { emoji: '📖', label: '沉稳叙述', instruction: '用沉稳大气、娓娓道来的语气，像专业播音员在解说', theme: '专业解说' },
    { emoji: '🌊', label: '起伏跌宕', instruction: '用情感起伏、张弛有度的语气，像在经历一场跌宕的旅程', theme: '戏剧独白' }
  ]
  directorInstruction.value = aiMoodSuggestions.value[0].instruction
  sceneTheme.value = aiMoodSuggestions.value[0].theme
  isAnalyzingMood.value = false
}

const promptLibrary = ref([])

onMounted(async () => {
  try {
    const data = await request.get('/tts/v2/prompt-library/tree')
    if (data) {
      promptLibrary.value = data
    }
  } catch (err) {
    console.error('获取指令库树失败', err)
  }
})

const showVoiceSelector = ref(false)
const selectedVoice = ref(
  route.query.voiceId
    ? { voiceId: route.query.voiceId, name: route.query.voiceName || route.query.voiceId }
    : { voiceId: 'zh_female_vv_uranus_bigtts', name: 'vivi 2.0' }
)


const insertTag = (tag) => {
  content.value += tag
}

const fillDemo = (type) => {
  const demos = {
    suspense: '[#用惊恐和颤抖的语气] 谁...谁在那里？别过来！我知道你在那儿！求求你放过我...',
    emotional: '[#用悲痛欲绝的语气] 为什么会变成这样...我们明明约定好的，可是现在只剩我一个人了...',
    documentary: '[#用沉稳专业的播音腔] 浩瀚的宇宙中，地球就像一颗蓝色的玻璃珠。它孕育了生命，也见证了无数奇迹。'
  }
  content.value = demos[type] || ''
  creativeMode.value = 'quick'
  instruction.value = ''
}

const generateScene = async () => {
  // 游客点击 AI 写本 → 提示登录
  if (!authStore.isLoggedIn) {
    toastStore.show('请先登录后使用 AI 写本')
    router.push({ name: 'Login', query: { redirect: route.fullPath } })
    return
  }

  // 根据模式取指令
  const activeInstruction = creativeMode.value === 'quick' 
    ? instruction.value 
    : directorInstruction.value
  
  if (!activeInstruction.trim()) return toastStore.show('请先选择或输入一个语气指令')

  // 捕获发起时的模式，防止异步返回时模式已切换导致数据错乱
  const modeAtInvoke = creativeMode.value
  
  isGeneratingScene.value = true
  content.value = '' // 清空主输入框
  
  // 快速模式固定80字，导演模式固定250字
  const wordCount = modeAtInvoke === 'quick' ? 80 : 250
  
  try {
    const data = await request.post('/tts/v2/generate-scene', { 
      instruction: activeInstruction,
      wordCount: wordCount,
      theme: sceneTheme.value || '日常随机配音',
      context: contextText.value,
      mode: modeAtInvoke
    })
    // 异步返回后检查：如果用户已经切换了模式，丢弃旧结果
    if (creativeMode.value !== modeAtInvoke) {
      console.warn('模式已切换，丢弃旧请求结果')
      return
    }
    if (data) {
      // P2: 快速模式AI写本超限时智能截断
      if (modeAtInvoke === 'quick' && data.length > 100) {
        const cut = data.lastIndexOf('。', 100)
        content.value = cut > 0 ? data.slice(0, cut + 1) : data.slice(0, 100)
        toastStore.show('已智能截断至100字以内')
      } else {
        content.value = data
      }
    }
  } catch (err) {
    console.error('Scene generation error:', err)
    // request.js 里已经有通用的 401 和错误拦截 Toast
  } finally {
    isGeneratingScene.value = false
  }
}

const enhanceTags = async () => {
  if (!authStore.isLoggedIn) {
    toastStore.show('请先登录以唤醒您的创作档案 ✨')
    return router.replace({ name: 'Login', query: { redirect: '/emotion' } })
  }
  if (!content.value.trim()) return toastStore.show('请输入需要 AI 润色的台词内容')
  
  isEnhancing.value = true
  try {
    const data = await request.post('/tts/v2/enhance-tags', { text: content.value })
    if (data) {
      content.value = data
      // 清空全局指令框，因为 AI 已经把生成的全局指令插入到正文里了
      instruction.value = ''
      // 保持当前模式不变（原代码引用了未定义的 mode 变量）
      toastStore.show('✨ AI 情感注入完成')
    }
  } catch (err) {
    console.error('AI 情感润色请求异常', err)
    toastStore.show('AI 引擎处理失败: ' + err.message)
  } finally {
    isEnhancing.value = false
  }
}

const synthesize = async () => {
  if (!authStore.isLoggedIn) {
    toastStore.show('请先登录以唤醒您的创作档案 ✨')
    return router.replace({ name: 'Login', query: { redirect: '/emotion' } })
  }
  if (!authStore.hasFeature('tts_emotion_v2')) {
    toastStore.show('当前权限不足，请访问会员台解锁 Pro 特权 👑')
    router.push({ name: 'Vip' })
    return
  }
  if (!content.value.trim()) return toastStore.show('请输入核心台词内容')
  
  isSynthesizing.value = true
  try {
    // 根据模式拼接指令
    let finalInstruction = ''
    if (creativeMode.value === 'quick') {
      finalInstruction = instruction.value ? `[#${instruction.value}] ` : ''
    } else {
      finalInstruction = directorInstruction.value ? `[#${directorInstruction.value}] ` : ''
    }
    
    // 根据当前模式获取对应的指令值来判断 mode
    const activeInstructionForMode = creativeMode.value === 'quick' 
      ? instruction.value 
      : directorInstruction.value
    
    const reqBody = {
       text: finalInstruction + content.value,
       // 双向注入：发向前沿大模型
       contextText: contextText.value ? contextText.value : null,
       voiceType: selectedVoice.value.voiceId,
       mode: (activeInstructionForMode || contextText.value) ? 'voice_command' : 'default',
       userKey: authStore.user?.id?.toString() || 'anonymous'
    }

    const data = await request.post('/tts/v2/synthesize', reqBody)
    
    if (data && data.audioUrl) {
      playerStore.play({
        title: '情感维系 V2',
        author: selectedVoice.value?.name || 'Volcengine',
        url: data.audioUrl
      })
      toastStore.show('极致音频已就位！正为您沉浸播放')
    } else {
      toastStore.show(data?.message || '合成遇到了未知阻碍')
    }
  } catch (err) {
    console.error('合成请求异常', err)
    toastStore.show('网络或者服务端响应产生错误: ' + err.message)
  } finally {
    isSynthesizing.value = false
  }
}
// ==================== 创作库 ====================

const libVisible = ref(false)
const libList = ref([])
const libLoading = ref(false)
const actionItem = ref(null)
const renameVisible = ref(false)
const renameTitle = ref('')
const renameItemId = ref(null)
const renameInput = ref(null)

const openLibrary = async () => {
  libVisible.value = true
  libLoading.value = true
  try {
    const res = await creationApi.list({ type: 'emotion', page: 1, size: 50 })
    libList.value = res.records || []
  } catch (e) {
    toastStore.show('加载失败: ' + e.message)
  } finally {
    libLoading.value = false
  }
}

const playItem = (item) => {
  if (!item.audioUrl) return toastStore.show('音频不可用')
  playerStore.play({
    title: item.title || '情感配音',
    author: item.voiceId || 'AI',
    url: item.audioUrl
  })
}

const openAction = (item) => { actionItem.value = item }

const startRename = () => {
  renameItemId.value = actionItem.value?.id
  renameTitle.value = actionItem.value?.title || ''
  actionItem.value = null
  renameVisible.value = true
  nextTick(() => renameInput.value?.focus())
}

const confirmRename = async () => {
  if (!renameTitle.value.trim()) return toastStore.show('标题不能为空')
  try {
    await creationApi.rename(renameItemId.value, renameTitle.value.trim())
    const item = libList.value.find(i => i.id === renameItemId.value)
    if (item) item.title = renameTitle.value.trim()
    renameVisible.value = false
    toastStore.show('标题已更新')
  } catch (e) {
    toastStore.show('重命名失败: ' + e.message)
  }
}

const publishItem = async () => {
  const item = actionItem.value
  actionItem.value = null
  try {
    await creationApi.publish(item.id, { title: item.title, category: 'audio' })
    item.isPublished = 1
    toastStore.show('已发布到发现页')
  } catch (e) {
    toastStore.show('发布失败: ' + e.message)
  }
}

const unpublishItem = async () => {
  const item = actionItem.value
  actionItem.value = null
  try {
    await creationApi.unpublish(item.id)
    item.isPublished = 0
    item.workId = null
    toastStore.show('已从发现页下架')
  } catch (e) {
    toastStore.show('下架失败: ' + e.message)
  }
}

const deleteItem = async () => {
  const item = actionItem.value
  actionItem.value = null
  try {
    await creationApi.delete(item.id)
    libList.value = libList.value.filter(i => i.id !== item.id)
    toastStore.show('已删除')
  } catch (e) {
    toastStore.show('删除失败: ' + e.message)
  }
}

const formatDuration = (seconds) => {
  if (!seconds) return '0:00'
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${m}:${String(s).padStart(2, '0')}`
}

// 智能标题：如果是默认生成的标题（"情感合成 2026-xx-xx"），取 inputText 的前 15 字
const smartTitle = (item) => {
  const t = item.title || ''
  // 检测是否为系统自动标题
  if (/^(情感合成|配音|创作)\s*\d{4}/.test(t) && item.inputText) {
    // 去掉标签 [#xxx] 和 【xxx】，取前 20 字
    const clean = item.inputText.replace(/\[#[^\]]*\]\s*/g, '').replace(/【[^】]*】/g, '').trim()
    return clean.length > 20 ? clean.slice(0, 20) + '…' : clean
  }
  return t || '未命名'
}

// 相对时间
const formatRelativeTime = (dateStr) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const now = new Date()
  const diffMs = now - d
  const diffMin = Math.floor(diffMs / 60000)
  const diffHour = Math.floor(diffMs / 3600000)
  const diffDay = Math.floor(diffMs / 86400000)

  if (diffMin < 1) return '刚刚'
  if (diffMin < 60) return `${diffMin}分钟前`
  if (diffHour < 24) return `${diffHour}小时前`
  if (diffDay === 1) return '昨天'
  if (diffDay < 7) return `${diffDay}天前`
  return `${d.getMonth() + 1}/${d.getDate()}`
}

// 音色简称
const shortVoice = (voiceId) => {
  if (!voiceId) return ''
  // "zh_female_vv_uranus_bigtts" → "uranus"
  const parts = voiceId.split('_')
  if (parts.length >= 4) return parts[3]
  return voiceId.length > 10 ? voiceId.slice(0, 8) + '…' : voiceId
}

// 是否正在播放
const isPlaying = (item) => {
  return playerStore.currentTrack?.url === item.audioUrl && playerStore.isPlaying
}

// 统计数据
const formatTotalDuration = computed(() => {
  const totalSec = libList.value.reduce((sum, i) => sum + (i.audioDuration || 0), 0)
  if (totalSec < 60) return `${totalSec}秒`
  const m = Math.floor(totalSec / 60)
  const s = totalSec % 60
  return s > 0 ? `${m}分${s}秒` : `${m}分钟`
})

const publishedCount = computed(() => {
  return libList.value.filter(i => i.isPublished === 1).length
})
</script>

<style scoped>
/* 慢呼吸与浮动灯效 */
.animate-pulse-slow { animation: slowPulse 8s cubic-bezier(0.4, 0, 0.6, 1) infinite; }
.animate-float { animation: float 10s ease-in-out infinite; }
@keyframes slowPulse { 0%, 100% { opacity: 0.3; transform: scale(1); } 50% { opacity: 0.6; transform: scale(1.1); } }
@keyframes float { 0%, 100% { transform: translateY(0) scale(1); } 50% { transform: translateY(-20px) scale(1.05); } }

/* 滑动渐变组件切换 */
.slide-fade-enter-active { transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1); }
.slide-fade-leave-active { transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); position: absolute; width: 100%; top: 0;}
.slide-fade-enter-from { opacity: 0; transform: translateY(10px) scale(0.98); }
.slide-fade-leave-to { opacity: 0; transform: translateY(-10px) scale(0.98); }

/* 导演面板抽屉动画 */
.drawer-enter-active { transition: all 0.35s cubic-bezier(0.16, 1, 0.3, 1); }
.drawer-leave-active { transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1); }
.drawer-enter-from,
.drawer-leave-to { opacity: 0; }
.drawer-enter-from > div:last-child,
.drawer-leave-to > div:last-child { transform: translateY(100%); }
.drawer-enter-active > div:last-child { transition: transform 0.35s cubic-bezier(0.16, 1, 0.3, 1); }
.drawer-leave-active > div:last-child { transition: transform 0.25s cubic-bezier(0.16, 1, 0.3, 1); }

/* 音频波浪加载动画 */
.audio-waves {
  display: flex; align-items: center; justify-content: center; height: 16px;
}
.audio-waves span {
  display: block; width: 3px; height: 100%; border-radius: 2px;
  background: #FF9500; margin: 0 2px;
  animation: wave 1.2s ease-in-out infinite;
}
.audio-waves span:nth-child(2) { animation-delay: 0.2s; }
.audio-waves span:nth-child(3) { animation-delay: 0.4s; }
.audio-waves span:nth-child(4) { animation-delay: 0.6s; }
@keyframes wave {
  0%, 100% { transform: scaleY(0.4); opacity: 0.5; }
  50% { transform: scaleY(1); opacity: 1; }
}

/* 玻璃卡片通用基础 */
.glass-panel { background: rgba(30,30,30,0.4); backdrop-filter: blur(20px); -webkit-backdrop-filter: blur(20px); }

/* 优雅的隐藏横向滚动条 */
.custom-scrollbar::-webkit-scrollbar { width: 4px; height: 4px; }
.custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: rgba(255, 255, 255, 0.1); border-radius: 4px; }
.custom-scrollbar::-webkit-scrollbar-thumb:hover { background: rgba(255, 255, 255, 0.2); }

/* 创作库卡片播放动画 */
.lib-waves { display: flex; align-items: center; gap: 2px; height: 14px; }
.lib-waves span {
  display: block; width: 2px; border-radius: 1px; background: #000;
  animation: libWave 0.8s ease-in-out infinite;
}
.lib-waves span:nth-child(1) { height: 40%; animation-delay: 0s; }
.lib-waves span:nth-child(2) { height: 70%; animation-delay: 0.15s; }
.lib-waves span:nth-child(3) { height: 50%; animation-delay: 0.3s; }
@keyframes libWave {
  0%, 100% { transform: scaleY(0.5); }
  50% { transform: scaleY(1.2); }
}

/* 创作库面板滑入动画 */
.lib-enter-active { transition: transform 0.35s cubic-bezier(0.16, 1, 0.3, 1), opacity 0.2s; }
.lib-leave-active { transition: transform 0.25s cubic-bezier(0.7, 0, 0.84, 0), opacity 0.2s; }
.lib-enter-from { transform: translateX(100%); opacity: 0; }
.lib-leave-to { transform: translateX(100%); opacity: 0; }

/* ActionSheet 动画 */
.sheet-enter-active { transition: opacity 0.25s ease; }
.sheet-leave-active { transition: opacity 0.25s ease; }
.sheet-enter-active > div:last-child,
.sheet-leave-active > div:last-child { transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1); }
.sheet-enter-from { opacity: 0; }
.sheet-enter-from > div:last-child { transform: translateY(100%); }
.sheet-leave-to { opacity: 0; }
.sheet-leave-to > div:last-child { transform: translateY(100%); }
</style>
