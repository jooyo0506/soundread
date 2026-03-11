/**
 * 认证状态管理 Store
 *
 * 管理用户登录态、令牌持久化、等级策略缓存。
 * 所有需要认证信息的组件都应通过此 Store 获取。
 *
 * @module stores/auth
 */
import { defineStore } from 'pinia'
import { authApi } from '../api/auth'

/** localStorage 持久化键名 */
const STORAGE_KEYS = {
    TOKEN: 'sr_token',
    POLICY: 'sr_policy'
}

export const useAuthStore = defineStore('auth', {
    state: () => ({
        /** Sa-Token 令牌 */
        token: localStorage.getItem(STORAGE_KEYS.TOKEN) || '',
        /** 当前用户信息 { userId, nickname, vip, admin } */
        user: null,
        /** 动态策略配置（后端登录时一次性下发，包含功能开关和配额限制） */
        policy: JSON.parse(localStorage.getItem(STORAGE_KEYS.POLICY) || 'null')
    }),

    getters: {
        /** 是否已登录 */
        isLoggedIn: (state) => !!state.token,

        /** 是否为 VIP 用户 */
        isVip: (state) => state.user?.vip || false,

        /**
         * 功能开关：判断当前用户是否有某项功能权限
         * @example authStore.hasFeature('ai_script')
         */
        hasFeature: (state) => (featureName) => {
            if (state.policy?.featureFlags) {
                const flag = state.policy.featureFlags[featureName]
                if (flag === true) return true   // 明确开启
                if (flag === false) return false  // 明确关闭
                // 未配置 → 降级到 VIP 状态判断（兼容手动改库、policy 过期）
            }
            return state.user?.vip || false
        },

        /**
         * 配额限制：获取某项配额的数值上限
         * @example authStore.getQuota('tts_daily_chars')
         */
        getQuota: (state) => (quotaName) => {
            if (!state.policy?.quotaLimits) { return 0 }
            return state.policy.quotaLimits[quotaName] ?? 0
        },

        /** 当前等级显示名称（如 "VIP月度用户"） */
        tierName: (state) => state.policy?.tierName || '普通用户'
    },

    actions: {
        /**
         * 保存令牌到 state + localStorage
         * @param {string} token - Sa-Token 令牌
         */
        setToken(token) {
            this.token = token
            localStorage.setItem(STORAGE_KEYS.TOKEN, token)
        },

        /**
         * 保存策略配置到 state + localStorage
         * @param {Object|null} policy - 策略配置对象
         */
        setPolicy(policy) {
            this.policy = policy
            if (policy) {
                localStorage.setItem(STORAGE_KEYS.POLICY, JSON.stringify(policy))
            } else {
                localStorage.removeItem(STORAGE_KEYS.POLICY)
            }
        },

        /** 清除所有认证状态（退出登录） */
        clearToken() {
            this.token = ''
            this.user = null
            this.policy = null
            localStorage.removeItem(STORAGE_KEYS.TOKEN)
            localStorage.removeItem(STORAGE_KEYS.POLICY)
        },

        /**
         * 拉取当前用户信息（由路由守卫调用）
         * @throws {Error} 令牌无效或网络异常时抛出
         */
        async fetchUserInfo() {
            if (!this.token) { return }
            try {
                const userInfo = await authApi.getUserInfo()
                this.user = {
                    // 兼容新格式(LoginResponse: userId/vip/admin)和旧格式(User: id/vipLevel/role)
                    userId: userInfo.userId || userInfo.id,
                    nickname: userInfo.nickname,
                    vip: userInfo.vip ?? (userInfo.vipLevel > 0),
                    admin: userInfo.admin ?? (userInfo.role === 'admin')
                }
                // /me 现在始终返回最新 policy，强制更新（运营端改策略后自动生效）
                if (userInfo?.policy) {
                    this.setPolicy(userInfo.policy)
                }
            } catch (e) {
                console.error('Failed to fetch user info', e)
                throw e
            }
        },

        /**
         * 密码登录
         * @param {Object} params - { phone, password }
         * @returns {Promise<Object>} 登录响应数据
         */
        async login(params) {
            const res = await authApi.login(params)
            this.setToken(res.token)
            this.user = {
                userId: res.userId,
                nickname: res.nickname,
                vip: res.vip,
                admin: res.admin
            }
            if (res.policy) {
                this.setPolicy(res.policy)
            }
            return res
        }
    }
})
