<template>
  <div class="app-container bg-[#050505] relative flex flex-col overflow-hidden
              sm:w-[390px] sm:h-[844px] sm:rounded-[40px] sm:shadow-2xl sm:border-[8px] sm:border-gray-800">

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
/* 移动端全屏容器 */
.app-container {
  /* 高度：用 dvh (dynamic viewport height) 适配 Safari 地址栏的动态显示/隐藏 */
  height: 100vh;          /* 旧版浏览器 fallback */
  height: 100svh;         /* small viewport height，iOS 15.4+ */
  height: 100dvh;         /* dynamic viewport height，最新 */
  width: 100%;
}

/* 桌面端：sm 断点 (640px+) 由 Tailwind sm: 类覆盖宽高，此处不限制 */
@media (min-width: 640px) {
  .app-container {
    width: 390px;
    height: 844px;
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
