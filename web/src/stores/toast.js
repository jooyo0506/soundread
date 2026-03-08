/**
 * 全局轻提示 Store
 *
 * 提供全局 Toast 消息展示，自动消失。
 * 配合 GlobalToast.vue 组件使用。
 *
 * @module stores/toast
 * @example
 * const toastStore = useToastStore()
 * toastStore.show('操作成功')
 * toastStore.show('请稍后重试', 5000)
 */
import { defineStore } from 'pinia'

/** 默认消息展示时长（毫秒） */
const DEFAULT_DURATION = 3000

export const useToastStore = defineStore('toast', {
    state: () => ({
        /** 当前提示消息文本 */
        message: '',
        /** 是否可见 */
        visible: false,
        /** 自动隐藏定时器 ID（用于防抖） */
        timeoutId: null
    }),

    actions: {
        /**
         * 展示一条轻提示消息
         *
         * 连续调用时，前一条消息会被立即覆盖（防抖）。
         *
         * @param {string} msg - 提示消息文本
         * @param {number} duration - 展示时长（毫秒），默认 3000ms
         */
        show(msg, duration = DEFAULT_DURATION) {
            this.message = msg
            this.visible = true

            // 防抖：清除上一条的自动隐藏计时器
            if (this.timeoutId) {
                clearTimeout(this.timeoutId)
            }

            this.timeoutId = setTimeout(() => {
                this.visible = false
            }, duration)
        }
    }
})
