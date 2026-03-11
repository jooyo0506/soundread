<template>
  <div class="w-[390px] h-[844px] bg-[#050505] rounded-[40px] overflow-hidden relative shadow-2xl flex flex-col border-[8px] border-gray-800">
    <!-- 主体路由出口 -->
    <div class="flex-1 overflow-y-auto relative hide-scrollbar">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <keep-alive :include="['Home', 'Discover', 'VoiceLibrary']">
            <component :is="Component" />
          </keep-alive>
        </transition>
      </router-view>
    </div>
    
    <!-- 全局组件，例如音频播放控制条 / TabBar / 轻提示 -->
    <GlobalToast />
    <GlobalPlayer />
    <TabBar v-if="showTabBar" />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import TabBar from './components/TabBar.vue'
import GlobalPlayer from './components/GlobalPlayer.vue'
import GlobalToast from './components/GlobalToast.vue'

const route = useRoute()

// 仅在四个主页签中显示底部 TabBar
const showTabBar = computed(() => {
  const mainTabs = ['Home', 'Discover', 'Workshop', 'Studio', 'Profile']
  return mainTabs.includes(route.name)
})
</script>

<style>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
