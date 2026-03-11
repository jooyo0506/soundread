/**
 * 认证 API 模块
 */
import request from './request'

export const authApi = {
    /**
     * 密码登录
     * @param {Object} data - { phone, password }
     */
    login(data) {
        return request.post('/auth/login', data)
    },

    /**
     * 邀请码注册（替代原 SMS 注册方案）
     * @param {Object} data - { phone, inviteCode, password, confirmPassword }
     */
    register(data) {
        return request.post('/auth/register', data)
    },

    /**
     * 获取当前登录用户信息
     */
    getUserInfo() {
        return request.get('/auth/me')
    },

    /** 退出登录 */
    logout() {
        return request.post('/auth/logout')
    },

    /**
     * 获取当前用户配额使用情况
     */
    getQuotaUsage() {
        return request.get('/auth/quota-usage')
    }
}
