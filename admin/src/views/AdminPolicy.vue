<template>
  <div class="p-6 lg:p-8">
    <!-- Header -->
    <div class="flex justify-between items-center mb-6">
      <div>
        <h2 class="text-lg font-bold text-white">等级策略管理</h2>
        <p class="text-xs text-gray-500 mt-0.5">动态配置各等级的功能权限、配额限制、资源分配规则</p>
      </div>
      <button @click="refreshCache" class="px-3 py-1.5 rounded-lg bg-white/5 border border-white/10 text-gray-400 text-xs hover:bg-white/10 transition-colors cursor-pointer flex items-center gap-1.5">
        <i class="fas fa-sync-alt" :class="{'fa-spin': refreshing}"></i> 刷新缓存
      </button>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="text-center py-20 text-gray-500">
      <i class="fas fa-circle-notch fa-spin text-2xl mb-3"></i>
      <p>加载策略数据中...</p>
    </div>

    <div v-else class="space-y-8">
      <!-- Tab 选单 -->
      <div class="flex gap-8 border-b border-white/10 w-full overflow-x-auto hide-scrollbar">
        <button v-for="policy in policies" :key="policy.id"
                @click="activePolicyId = policy.id"
                class="pb-4 text-sm font-bold whitespace-nowrap transition-all cursor-pointer relative flex items-center gap-2"
                :class="activePolicyId === policy.id ? 'text-[#FF9500]' : 'text-gray-400 hover:text-white'">
          {{ policy.tierName }}
          <span class="text-[10px] px-1.5 py-0.5 rounded-full" :class="activePolicyId === policy.id ? 'bg-[#FF9500]/20 text-[#FF9500]' : 'bg-white/10 text-gray-500'">{{ policy.tierCode }}</span>
          <div v-if="activePolicyId === policy.id" class="absolute bottom-0 left-0 w-full h-[2px] bg-[#FF9500] rounded-t-full"></div>
        </button>
      </div>

      <!-- 策略详情 -->
      <div v-if="activePolicy" class="bg-[#1a1a1c] border border-white/5 rounded-3xl p-8 relative shadow-2xl">
        <div class="absolute top-0 right-0 w-64 h-64 bg-[#FF9500]/5 rounded-full blur-3xl -z-10 translate-x-1/2 -translate-y-1/2"></div>

        <div class="flex justify-between items-center mb-10 pb-6 border-b border-white/5">
          <div>
            <h2 class="text-2xl font-bold text-white">{{ activePolicy.tierName }} 权限策略</h2>
            <p class="text-sm text-gray-500 mt-2">最后更新: {{ activePolicy.updatedAt ? activePolicy.updatedAt.replace('T', ' ') : '-' }}</p>
          </div>
          <button @click="openEdit(activePolicy)" class="px-6 py-2.5 rounded-full bg-white text-black font-bold text-sm hover:bg-gray-200 transition-colors shadow-lg shadow-white/10 flex items-center gap-2 cursor-pointer">
            <i class="fas fa-sliders-h"></i> 修改配置
          </button>
        </div>

        <div class="space-y-10">
          <!-- 功能矩阵 -->
          <div>
            <h3 class="text-white font-bold mb-5 flex items-center gap-2"><i class="fas fa-cube text-gray-400"></i> 功能矩阵</h3>
            <div class="grid gap-4" style="grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));">
              <div v-for="(val, key) in activePolicy.featureFlags" :key="key"
                   class="p-5 rounded-2xl border flex flex-col items-center justify-center gap-3 transition-all text-center"
                   :class="val ? 'bg-[#FF9500]/5 border-[#FF9500]/30' : 'bg-black/30 border-white/5 opacity-50'">
                <i :class="val ? 'fas fa-check-circle text-[#FF9500] text-2xl' : 'fas fa-lock text-gray-600 text-xl'"></i>
                <span class="text-xs font-bold" :class="val ? 'text-white' : 'text-gray-500'">{{ featureLabel(key) }}</span>
              </div>
            </div>
          </div>

          <!-- 配额与资源 -->
          <div class="grid gap-8" style="grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));">
            <div class="bg-black/20 rounded-3xl p-7 border border-white/5">
              <h3 class="text-white font-bold mb-6 flex items-center gap-2"><i class="fas fa-chart-pie text-gray-400"></i> 配额</h3>
              <div class="grid gap-4" style="grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));">
                <div v-for="(val, key) in activePolicy.quotaLimits" :key="key" class="bg-white/[0.02] border border-white/5 rounded-2xl p-4 flex flex-col justify-center gap-1.5 hover:bg-white/[0.04]">
                  <span class="text-xs text-gray-500 truncate">{{ quotaLabel(key) }}</span>
                  <span class="text-xl font-mono font-bold leading-none" :class="val === -1 ? 'text-[#FFD60A]' : 'text-white'">{{ val === -1 ? '无限' : val }}</span>
                </div>
              </div>
            </div>
            <div class="bg-black/20 rounded-3xl p-7 border border-white/5">
              <h3 class="text-white font-bold mb-6 flex items-center gap-2"><i class="fas fa-server text-gray-400"></i> 资源</h3>
              <div class="grid gap-4" style="grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));">
                <div v-for="(val, key) in activePolicy.resourceRules" :key="key" class="bg-white/[0.02] border border-white/5 rounded-2xl p-4 flex flex-col justify-center gap-1.5 hover:bg-white/[0.04]">
                  <span class="text-xs text-gray-500 truncate">{{ resourceLabel(key) }}</span>
                  <span class="text-base font-bold text-white leading-tight truncate">{{ val === -1 ? '无限' : val }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 编辑弹窗 -->
    <div v-if="editVisible" class="fixed inset-0 bg-black/70 backdrop-blur-sm z-50 flex items-center justify-center p-4" @click.self="editVisible = false">
      <div class="bg-[#111] border border-white/10 rounded-2xl w-full max-w-2xl max-h-[90vh] overflow-y-auto hide-scrollbar">
        <div class="sticky top-0 bg-[#111] border-b border-white/10 px-6 py-4 flex justify-between items-center z-10">
          <h2 class="text-lg font-bold">编辑策略 · <span class="text-[#FF9500]">{{ editForm.tierName }}</span></h2>
          <button @click="editVisible = false" class="w-8 h-8 rounded-lg bg-white/5 flex items-center justify-center text-gray-400 hover:text-white cursor-pointer">
            <i class="fas fa-xmark"></i>
          </button>
        </div>
        <div class="px-6 py-5 space-y-6">
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="text-xs text-gray-400 mb-1 block">等级代号 <span class="text-gray-600">· 不可修改</span></label>
              <input v-model="editForm.tierCode" readonly class="w-full bg-white/5 border border-white/10 rounded-lg px-3 py-2 text-sm text-gray-500 outline-none cursor-not-allowed" />
            </div>
            <div>
              <label class="text-xs text-gray-400 mb-1 block">等级名称</label>
              <input v-model="editForm.tierName" class="w-full bg-white/5 border border-white/10 rounded-lg px-3 py-2 text-sm text-white outline-none focus:border-[#FF9500]/50" />
            </div>
          </div>
          <div>
            <h3 class="text-sm font-bold mb-3 flex items-center gap-2"><i class="fas fa-toggle-on text-green-400"></i> 功能开关</h3>
            <div class="grid grid-cols-2 gap-3">
              <div v-for="(val, key) in editForm.featureFlags" :key="key"
                   class="flex justify-between items-center bg-white/[0.03] border border-white/5 rounded-lg px-3 py-2.5 cursor-pointer hover:bg-white/[0.06]"
                   @click="editForm.featureFlags[key] = !editForm.featureFlags[key]">
                <span class="text-xs text-gray-300">{{ featureLabel(key) }}</span>
                <div class="w-9 h-5 rounded-full relative transition-colors" :class="editForm.featureFlags[key] ? 'bg-green-500' : 'bg-gray-600'">
                  <div class="absolute w-3.5 h-3.5 bg-white rounded-full top-[3px] transition-all" :class="editForm.featureFlags[key] ? 'right-[3px]' : 'left-[3px]'"></div>
                </div>
              </div>
            </div>
          </div>
          <div>
            <h3 class="text-sm font-bold mb-3 flex items-center gap-2"><i class="fas fa-gauge-high text-blue-400"></i> 配额限制 <span class="text-[10px] text-gray-500 font-normal">(-1 = 无限)</span></h3>
            <div class="grid grid-cols-2 gap-3">
              <div v-for="(val, key) in editForm.quotaLimits" :key="key" class="flex flex-col gap-1">
                <label class="text-[10px] text-gray-500">{{ quotaLabel(key) }}</label>
                <input type="number" v-model.number="editForm.quotaLimits[key]" class="bg-white/5 border border-white/10 rounded-lg px-3 py-2 text-sm text-white outline-none focus:border-blue-400/50 w-full" />
              </div>
            </div>
          </div>
          <div>
            <h3 class="text-sm font-bold mb-3 flex items-center gap-2"><i class="fas fa-microchip text-purple-400"></i> 资源规则</h3>
            <div class="grid grid-cols-2 gap-3">
              <div v-for="(val, key) in editForm.resourceRules" :key="key" class="flex flex-col gap-1">
                <label class="text-[10px] text-gray-500">{{ resourceLabel(key) }}</label>
                <input v-model="editForm.resourceRules[key]" class="bg-white/5 border border-white/10 rounded-lg px-3 py-2 text-sm text-white outline-none focus:border-purple-400/50 w-full" />
              </div>
            </div>
          </div>
        </div>
        <div class="sticky bottom-0 bg-[#111] border-t border-white/10 px-6 py-4 flex justify-end gap-3">
          <button @click="editVisible = false" class="px-5 py-2 rounded-lg bg-white/5 text-gray-400 text-sm hover:bg-white/10 cursor-pointer">取消</button>
          <button @click="savePolicy" :disabled="saving" class="px-5 py-2 rounded-lg bg-[#FF9500] text-black font-bold text-sm hover:bg-[#FFB340] disabled:opacity-50 cursor-pointer flex items-center gap-2">
            <i v-if="saving" class="fas fa-circle-notch fa-spin"></i>
            {{ saving ? '保存中...' : '保存并生效' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { adminApi } from '../api/admin'
import { useToastStore } from '../stores/toast'

const toastStore = useToastStore()
const policies = ref([])
const activePolicyId = ref(null)
const loading = ref(true)
const refreshing = ref(false)
const saving = ref(false)
const editVisible = ref(false)
const editForm = ref({})
const editId = ref(null)

const activePolicy = computed(() => {
  if (!policies.value?.length) return null
  return policies.value.find(p => p.id === activePolicyId.value) || policies.value[0]
})

const featureLabelMap = { tts_basic:'基础合成', tts_emotion_v2:'情感合成', ai_podcast:'智能播客', voice_clone:'声音复刻', ai_script:'智能编排', multi_language:'多语言', ai_novel:'有声小说', auto_publish:'自动发布', voice_tier:'音色库' }
const quotaLabelMap = { tts_daily_chars:'基础合成日字数', tts_v2_daily_chars:'情感合成日字数', clone_total_count:'克隆限额', ask_daily_count:'日对话数', podcast_daily_count:'日播客数', ai_script_daily_count:'AI编排日次数', storage_max_mb:'存储上限(MB)', max_projects:'作品上限', data_retention_days:'保留天数', novel_daily_chars:'小说合成日字数', novel_max_projects:'小说项目上限' }
const resourceLabelMap = { llm_provider:'模型供应商', llm_model:'大模型', llm_base_url:'接口地址', task_priority:'优先级', qps_limit:'并发上限', voice_tier:'音色库' }

const featureLabel = (key) => featureLabelMap[key] || key
const quotaLabel = (key) => quotaLabelMap[key] || key
const resourceLabel = (key) => resourceLabelMap[key] || key

const fetchPolicies = async () => {
  loading.value = true
  try {
    policies.value = await adminApi.getPolicies()
    if (policies.value.length > 0 && !activePolicyId.value) activePolicyId.value = policies.value[0].id
  } catch (e) { toastStore.show('加载策略失败: ' + (e.message || '未知错误')) }
  finally { loading.value = false }
}

const refreshCache = async () => {
  refreshing.value = true
  try { await adminApi.refreshCache(); toastStore.show('缓存已刷新 ✅') }
  catch (e) { toastStore.show('刷新失败: ' + e.message) }
  finally { refreshing.value = false }
}

const openEdit = (policy) => { editId.value = policy.id; editForm.value = JSON.parse(JSON.stringify(policy)); editVisible.value = true }

const savePolicy = async () => {
  saving.value = true
  try { await adminApi.updatePolicy(editId.value, editForm.value); editVisible.value = false; await fetchPolicies(); toastStore.show('策略已保存并立即生效 ✅') }
  catch (e) { toastStore.show('保存失败: ' + e.message) }
  finally { saving.value = false }
}

onMounted(fetchPolicies)
</script>
