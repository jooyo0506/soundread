/**
 * @module api/creation
 * @description 创作中心 API — 统一管理所有模块的创作记录
 */
import request from './request'

export const creationApi = {
    /**
     * 查询我的创作列表（分页）
     * @param {Object} params - 查询参数
     * @param {string} [params.type] - 类型筛选: tts/emotion/drama/podcast/novel
     * @param {number} [params.page=1] - 页码
     * @param {number} [params.size=20] - 每页数量
     * @returns {Promise} 分页创作列表
     */
    list(params = {}) {
        return request.get('/creation/list', { params })
    },

    /**
     * 删除创作记录（联动释放存储）
     * @param {number} id - 创作记录ID
     * @returns {Promise}
     */
    delete(id) {
        return request.delete(`/creation/${id}`)
    },

    /**
     * 发布创作到发现页
     * @param {number} id - 创作记录ID
     * @param {Object} data - 发布信息
     * @param {string} data.title - 发布标题
     * @param {string} data.category - 分类
     * @returns {Promise<number>} 发布后的 work ID
     */
    publish(id, data) {
        return request.post(`/creation/${id}/publish`, data)
    },

    /**
     * 重命名创作标题
     * @param {number} id - 创作记录ID
     * @param {string} title - 新标题
     */
    rename(id, title) {
        return request.put(`/creation/${id}/rename`, { title })
    },

    /**
     * 下架已发布的创作
     * @param {number} id - 创作记录ID
     */
    unpublish(id) {
        return request.post(`/creation/${id}/unpublish`)
    },

    /**
     * 查询我的存储用量
     * @returns {Promise<{usedBytes, maxBytes, fileCount, creationCount}>}
     */
    getStorage() {
        return request.get('/creation/storage')
    }
}
