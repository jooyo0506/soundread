<template>
  <Transition name="toast">
    <div 
      v-if="toastStore.visible" 
      class="fixed top-20 left-1/2 -translate-x-1/2 z-[1000] max-w-[85%] px-5 py-3 rounded-2xl bg-black/80 backdrop-blur-md border border-white/20 shadow-2xl flex items-center gap-3 overflow-hidden"
    >
      <div class="absolute inset-0 bg-gradient-to-r from-[#FF9500]/20 to-transparent pointer-events-none"></div>
      <i class="fas fa-info-circle text-[#FF9500] text-lg"></i>
      <span class="text-white text-[13px] font-medium leading-snug">{{ toastStore.message }}</span>
    </div>
  </Transition>
</template>

<script setup>
import { useToastStore } from '../stores/toast'
const toastStore = useToastStore()
</script>

<style scoped>
.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.toast-enter-from {
  opacity: 0;
  transform: translate(-50%, -20px);
}

.toast-leave-to {
  opacity: 0;
  transform: translate(-50%, -20px);
}
</style>
