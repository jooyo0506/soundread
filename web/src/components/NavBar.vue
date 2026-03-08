<template>
  <div class="h-14 flex justify-between items-center px-4 mt-8 z-40 bg-transparent" :class="customClass">
    <button @click="handleBack" class="w-8 h-8 flex justify-center items-center text-white cursor-pointer active:scale-90 transition-transform">
      <i class="fas fa-chevron-left text-sm"></i>
    </button>
    <h1 class="text-white font-bold text-base flex items-center gap-2">
      <slot name="title">{{ title }}</slot>
    </h1>
    <div class="w-8 flex justify-end">
      <slot name="right"></slot>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const props = defineProps({
  title: {
    type: String,
    default: ''
  },
  customClass: {
    type: String,
    default: ''
  },
  backRoute: {
    type: String,
    default: ''
  }
})

const handleBack = () => {
  if (props.backRoute) {
    router.push(props.backRoute)
  } else {
    router.back()
  }
}
</script>
