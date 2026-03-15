/**
 * 游客操作拦截 — 组合式函数
 *
 * 在游客可浏览但不可操作的页面中使用。
 * 当未登录用户点击需要权限的按钮时，弹出提示并引导跳转登录页。
 *
 * @example
 * const { requireLogin } = useLoginGuard()
 * function onSynthesize() {
 *   if (requireLogin('使用配音')) return
 *   // ... 正常合成逻辑
 * }
 */
import { useAuthStore } from '../stores/auth'
import { useToastStore } from '../stores/toast'
import { useRouter, useRoute } from 'vue-router'

export function useLoginGuard() {
    const authStore = useAuthStore()
    const toastStore = useToastStore()
    const router = useRouter()
    const route = useRoute()

    /**
     * 检查是否已登录，未登录时弹出提示并跳转登录页
     * @param {string} actionName - 操作名称，用于提示文案（如 '使用配音'、'生成播客'）
     * @returns {boolean} true = 已拦截（未登录），false = 已登录可继续
     */
    function requireLogin(actionName = '使用该功能') {
        if (authStore.isLoggedIn) {
            return false
        }
        toastStore.show(`请先登录后${actionName}`)
        router.push({ name: 'Login', query: { redirect: route.fullPath } })
        return true
    }

    /** 当前用户是否为游客（未登录） */
    const isGuest = () => !authStore.isLoggedIn

    return { requireLogin, isGuest }
}
