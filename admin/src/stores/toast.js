import { defineStore } from 'pinia'

export const useToastStore = defineStore('toast', {
    state: () => ({ message: '', visible: false, timeoutId: null }),
    actions: {
        show(msg, duration = 3000) {
            this.message = msg
            this.visible = true
            if (this.timeoutId) clearTimeout(this.timeoutId)
            this.timeoutId = setTimeout(() => { this.visible = false }, duration)
        }
    }
})
