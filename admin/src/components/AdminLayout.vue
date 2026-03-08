<template>
  <div class="fixed inset-0 flex bg-[#0a0a0a]">
    <!-- 左侧边栏 -->
    <aside class="w-[220px] bg-[#111] border-r border-white/5 flex flex-col flex-shrink-0">
      <!-- Logo -->
      <div class="px-5 py-5 border-b border-white/5">
        <h1 class="text-sm font-bold text-white flex items-center gap-2">
          <i class="fas fa-shield-alt text-[#FF9500]"></i>
          SoundRead 运营后台
        </h1>
      </div>

      <!-- 导航 -->
      <nav class="flex-1 py-3 px-3 space-y-1">
        <router-link v-for="item in navItems" :key="item.to" :to="item.to"
                     class="flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm transition-all"
                     :class="$route.path === item.to ? 'bg-[#FF9500]/10 text-[#FF9500] font-bold' : 'text-gray-400 hover:text-white hover:bg-white/5'">
          <i :class="item.icon" class="w-4 text-center"></i>
          {{ item.label }}
        </router-link>
      </nav>

      <!-- 底部用户 -->
      <div class="px-4 py-4 border-t border-white/5">
        <div class="flex items-center gap-2 mb-3">
          <div class="w-8 h-8 rounded-full bg-[#FF9500]/20 flex items-center justify-center text-[#FF9500] text-xs font-bold">
            {{ authStore.user?.nickname?.charAt(0) || 'A' }}
          </div>
          <div class="min-w-0">
            <div class="text-xs font-bold text-white truncate">{{ authStore.user?.nickname || '管理员' }}</div>
            <div class="text-[10px] text-gray-500">运营管理员</div>
          </div>
        </div>
        <button @click="logout" class="w-full text-left px-3 py-2 rounded-lg text-xs text-gray-500 hover:text-red-400 hover:bg-red-500/5 transition-colors cursor-pointer">
          <i class="fas fa-sign-out-alt mr-1.5"></i> 退出登录
        </button>
      </div>
    </aside>

    <!-- 右侧内容区 -->
    <main class="flex-1 overflow-y-auto">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { useAuthStore } from '../stores/auth'
import { useRouter } from 'vue-router'

const authStore = useAuthStore()
const router = useRouter()

const navItems = [
  { to: '/', icon: 'fas fa-th-list', label: '作品管理' },
  { to: '/policy', icon: 'fas fa-sliders-h', label: '策略管理' }
]

const logout = () => {
  authStore.clearToken()
  router.replace({ name: 'Login' })
}
</script>
