/**
 * VIP 会员 API 模块
 *
 * 提供会员套餐查询、订单创建、会员状态查询等接口。
 *
 * @module api/vip
 */
import request from './request'

export const vipApi = {
    /**
     * 获取所有可用的 VIP 套餐
     * @returns {Promise<Array<{ id, name, price, duration }>>}
     */
    getPlans() {
        return request.get('/vip/plans')
    },

    /**
     * 创建会员订单
     * @param {string|number} planId - 套餐 ID
     * @returns {Promise<{ orderId, payUrl }>}
     */
    createOrder(planId) {
        return request.post('/vip/orders', { planId })
    },

    /**
     * 查询当前会员状态
     * @returns {Promise<{ vipLevel, expireTime, tierCode }>}
     */
    getStatus() {
        return request.get('/vip/status')
    }
}
