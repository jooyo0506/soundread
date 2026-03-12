<template>
  <div class="absolute bottom-0 w-full bg-[#09090f]/95 backdrop-blur-xl border-t border-white/5 flex justify-around items-start pt-2.5 px-4 z-[40] tabbar-wrap">


    <router-link to="/" class="tab-item" :class="isActive('Home') ? 'tab-active' : 'tab-inactive'">
      <i class="fas fa-home text-[22px]"></i>
      <span>首页</span>
    </router-link>

    <router-link to="/discover" class="tab-item" :class="isActive('Discover') ? 'tab-active' : 'tab-inactive'">
      <i class="fas fa-compass text-[22px]"></i>
      <span>发现</span>
    </router-link>

    <!-- 中间主创作按钮（突出样式） -->
    <router-link to="/workshop" class="center-wrap">
      <div class="center-icon" :class="isActive('Workshop') ? 'center-active' : ''">
        <i class="fas fa-microphone text-[18px] text-white"></i>
      </div>
      <span class="text-[9px] mt-0.5" :class="isActive('Workshop') ? 'text-[#FF9500]' : 'text-gray-600'">创作</span>
    </router-link>

    <router-link to="/studio" class="tab-item" :class="isActive('Studio') ? 'tab-active' : 'tab-inactive'">
      <i class="fas fa-layer-group text-[22px]"></i>
      <span>工作台</span>
    </router-link>

    <router-link to="/profile" class="tab-item" :class="isActive('Profile') ? 'tab-active' : 'tab-inactive'">
      <i class="fas fa-user text-[22px]"></i>
      <span>我的</span>
    </router-link>

  </div>
</template>

<script setup>
import { useRoute } from 'vue-router'
const route = useRoute()
const isActive = (name) => route.name === name
</script>

<style scoped>
/* TabBar 高度 = 内容区 82px + iOS 底部安全区（iPhone X+ Home Bar）*/
.tabbar-wrap {
  height: calc(82px + env(safe-area-inset-bottom, 0px));
  padding-bottom: env(safe-area-inset-bottom, 0px);
}

.tab-item {
  display: flex; flex-direction: column;
  align-items: center; gap: 4px;
  width: 48px; padding-top: 2px;
  transition: all 0.15s;
}
.tab-active { color: #FF9500; font-weight: 700; }
.tab-inactive { color: #4b5563; }
.tab-item span { font-size: 10px; }

/* 中间创作按钮 */
.center-wrap {
  display: flex; flex-direction: column;
  align-items: center; gap: 0;
  position: relative; top: -14px;
}
.center-icon {
  width: 52px; height: 52px;
  border-radius: 16px;
  background: linear-gradient(135deg, #FF9500, #FF6B00);
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 4px 20px rgba(255,149,0,0.4);
  transition: all 0.2s;
}
.center-icon:hover { transform: scale(1.05); box-shadow: 0 6px 24px rgba(255,149,0,0.6); }
.center-active { box-shadow: 0 4px 24px rgba(255,149,0,0.7); }
</style>
