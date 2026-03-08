import { defineStore } from 'pinia'
import { authApi } from '../api/auth'

const STORAGE_KEYS = { TOKEN: 'sr_admin_token' }

export const useAuthStore = defineStore('auth', {
    state: () => ({
        token: localStorage.getItem(STORAGE_KEYS.TOKEN) || '',
        user: null
    }),

    getters: {
        isLoggedIn: (state) => !!state.token
    },

    actions: {
        setToken(token) {
            this.token = token
            localStorage.setItem(STORAGE_KEYS.TOKEN, token)
        },
        clearToken() {
            this.token = ''
            this.user = null
            localStorage.removeItem(STORAGE_KEYS.TOKEN)
        },
        async fetchUserInfo() {
            if (!this.token) return
            const info = await authApi.getUserInfo()
            this.user = {
                userId: info.id,
                nickname: info.nickname,
                admin: info.role === 'admin'
            }
        },
        async login(params) {
            const res = await authApi.login(params)
            this.setToken(res.token)
            this.user = {
                userId: res.userId,
                nickname: res.nickname,
                admin: res.admin
            }
            if (!res.admin) {
                this.clearToken()
                throw new Error('该账号没有管理员权限')
            }
            return res
        }
    }
})
