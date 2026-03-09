<template>
  <div class="absolute bottom-0 w-full h-[88px] bg-[#0A0A0A]/90 backdrop-blur-xl border-t border-white/5 flex justify-around items-start pt-3 px-2 z-[40]">
    <router-link to="/" class="flex flex-col items-center gap-1.5 w-14" :class="isActive('Home') ? 'text-[#FF9500] font-bold' : 'text-gray-600'">
      <i class="fas fa-home text-xl"></i>
      <span class="text-[10px]">首页</span>
    </router-link>
    <router-link to="/discover" class="flex flex-col items-center gap-1.5 w-14" :class="isActive('Discover') ? 'text-[#FF9500] font-bold' : 'text-gray-600'">
      <i class="fas fa-compass text-xl"></i>
      <span class="text-[10px]">发现</span>
    </router-link>
    <router-link to="/voices" class="flex flex-col items-center gap-1.5 w-14" :class="isActive('VoiceLibrary') ? 'text-[#FF9500] font-bold' : 'text-gray-600'">
      <i class="fas fa-microphone-lines text-xl"></i>
      <span class="text-[10px]">音色</span>
    </router-link>
    <router-link to="/studio" class="flex flex-col items-center gap-1.5 w-14" :class="isActive('Studio') ? 'text-[#FF9500] font-bold' : 'text-gray-600'">
      <i class="fas fa-pen-nib text-xl"></i>
      <span class="text-[10px]">创作</span>
    </router-link>
    <router-link to="/profile" class="flex flex-col items-center gap-1.5 w-14" :class="isActive('Profile') ? 'text-[#FF9500] font-bold' : 'text-gray-600'">
      <i class="fas fa-user text-xl"></i>
      <span class="text-[10px]">我的</span>
    </router-link>
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()

const isActive = (name) => {
  return route.name === name
}
</script>
