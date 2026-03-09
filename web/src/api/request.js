/**
 * Axios HTTP 请求封装
 *
 * 基于 axios 创建统一的请求实例，负责：
 * - 自动注入 Authorization 令牌（Sa-Token Bearer）
 * - 解包后端 Result<T> 响应体，直接返回 data 部分
 * - 统一处理 401/403 认证失效（自动清 token + 跳转登录页）
 * - 深度提取后端标准错误消息（res.message），避免前端二次解析
 *
 * @module api/request
 */
import axios from 'axios'
import { useAuthStore } from '../stores/auth'
import router from '../router'

/** 后端统一响应成功码 */
const SUCCESS_CODE = 200

/** 需要跳转登录的 HTTP 状态码 */
const AUTH_FAIL_CODES = [401, 403]

/** 不触发自动登录跳转的白名单路径（如静默探测接口） */
const AUTH_WHITELIST = ['/auth/me']

// 创建 Axios 实例
const request = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
    timeout: 60000,     // AI 生成可能时间较长，设置 60s
    headers: {
        'Content-Type': 'application/json'
    }
})

/**
 * 请求拦截器 — 自动注入 Authorization 头
 *
 * Sa-Token 要求格式: "Bearer {token}"
 */
request.interceptors.request.use(
    config => {
        const authStore = useAuthStore()
        if (authStore.token) {
            config.headers.Authorization = authStore.token.startsWith('Bearer ')
                ? authStore.token
                : `Bearer ${authStore.token}`
        }
        return config
    },
    error => Promise.reject(error)
)

/**
 * 判断请求路径是否在认证白名单中
 * @param {string} url - 请求 URL
 * @returns {boolean}
 */
const isAuthWhitelisted = (url) => AUTH_WHITELIST.some(path => url?.includes(path))

/**
 * 处理认证失效 — 清除 token 并跳转登录页（携带当前路径作为回跳参数）
 */
const handleAuthFailure = () => {
    const authStore = useAuthStore()
    authStore.clearToken()
    // 携带 redirect 参数，让用户登录后回到原页面
    const currentPath = router.currentRoute?.value?.fullPath || '/'
    if (currentPath !== '/login') {
        router.replace({ name: 'Login', query: { redirect: currentPath } })
    }
}

/**
 * 响应拦截器 — 解包 Result<T> 并处理错误
 *
 * 后端返回格式: { code: 200, message: "success", data: T }
 * 成功时直接返回 data，失败时抛出 Error(message)
 */
request.interceptors.response.use(
    response => {
        const res = response.data

        // 后端 Result 成功码判定（兼容字符串 '200' 和数字 0）
        if (res.code === SUCCESS_CODE || res.code === 0 || res.code === '200') {
            return res.data
        }

        // 处理认证失效（排除白名单接口）
        if (AUTH_FAIL_CODES.includes(res.code) && !isAuthWhitelisted(response.config.url)) {
            handleAuthFailure()
        }

        return Promise.reject(new Error(res.message || 'Error'))
    },
    error => {
        const url = error.config?.url || ''
        const status = error.response?.status

        // HTTP 层面的认证失效
        if (AUTH_FAIL_CODES.includes(status) && !isAuthWhitelisted(url)) {
            handleAuthFailure()
        }

        // 深度提取后端标准错误消息
        const backendMsg = error.response?.data?.message
        if (backendMsg) {
            return Promise.reject(new Error(backendMsg))
        }

        return Promise.reject(error)
    }
)

export default request
