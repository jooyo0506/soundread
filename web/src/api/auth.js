/**
 * 认证 API 模块
 *
 * 提供用户登录、注册、验证码发送、用户信息获取等认证相关接口。
 *
 * @module api/auth
 */
import request from './request'

export const authApi = {
    /**
     * 密码登录
     * @param {Object} data - { phone, password }
     * @returns {Promise<{ token, userId, nickname, vip, admin, policy }>}
     */
    login(data) {
        return request.post('/auth/login', data)
    },

    /**
     * 获取短信验证码
     * @param {string} phone - 手机号
     * @returns {Promise<string>} 验证码（测试环境直接返回明文）
     */
    getSmsCode(phone) {
        return request.post(`/auth/sms-code?phone=${phone}`)
    },

    /**
     * 验证码登录 / 注册（新手机号自动注册）
     * @param {Object} data - { phone, code, password, confirmPassword }
     * @returns {Promise<{ token, userId, nickname, vip, admin, policy }>}
     */
    smsLogin(data) {
        return request.post('/auth/sms-login', data)
    },

    /**
     * 获取当前登录用户信息
     * @returns {Promise<{ id, nickname, vipLevel, role, policy }>}
     */
    getUserInfo() {
        return request.get('/auth/me')
    },

    /** 退出登录 */
    logout() {
        return request.post('/auth/logout')
    },

    /**
     * 注册新用户
     * @param {Object} data - { phone, password }
     */
    register(data) {
        return request.post('/auth/register', data)
    },

    /**
     * 获取当前用户配额使用情况
     * @returns {Promise<{ ttsChars, aiScript, podcast, ask, tier }>}
     */
    getQuotaUsage() {
        return request.get('/auth/quota-usage')
    }
}
