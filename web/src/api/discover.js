/**
 * 发现页 API 模块
 *
 * 提供作品流、轮播图、播放计数、点赞等社区互动接口。
 *
 * @module api/discover
 */
import request from './request'

export const discoverApi = {
    /** 获取首页轮播图数据 */
    getBanners() {
        return request.get('/discover/banners')
    },

    /**
     * 获取作品列表（分页 + 排序）
     * @param {Object} params - { page, size, sort: 'hot'|'new' }
     * @returns {Promise<{ records: Array, total: number }>}
     */
    getWorks(params) {
        return request.get('/discover/works', { params })
    },

    /**
     * 记录一次播放（后端累加播放量）
     * @param {number|string} id - 作品 ID
     */
    playWork(id) {
        return request.post(`/discover/works/${id}/play`)
    },

    /**
     * 点赞 / 取消点赞（后端自动切换状态）
     * @param {number|string} id - 作品 ID
     */
    likeWork(id) {
        return request.post(`/discover/works/${id}/like`)
    },

    /** 下架自己的作品 */
    unpublishWork(id) {
        return request.post(`/discover/works/${id}/unpublish`)
    },

    /** 重新上架自己的作品 */
    republishWork(id) {
        return request.post(`/discover/works/${id}/republish`)
    }
}
