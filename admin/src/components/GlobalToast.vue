<template>
  <Transition name="toast">
    <div v-if="toastStore.visible"
         class="fixed top-6 left-1/2 -translate-x-1/2 z-[9999] px-5 py-2.5 bg-[#1a1a1c] border border-white/10 rounded-xl shadow-2xl text-sm text-white backdrop-blur-sm">
      {{ toastStore.message }}
    </div>
  </Transition>
</template>

<script setup>
import { useToastStore } from '../stores/toast'
const toastStore = useToastStore()
</script>

<style scoped>
.toast-enter-active, .toast-leave-active { transition: all 0.3s ease; }
.toast-enter-from, .toast-leave-to { opacity: 0; transform: translate(-50%, -12px); }
</style>
