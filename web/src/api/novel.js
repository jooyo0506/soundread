/**
 * @module api/novel
 * @description 有声书 API — 项目管理、Pipeline 控制、章节/分段查询
 */
import request from './request'

export const novelApi = {
    /**
     * 创建有声书项目
     * @param {Object} data
     * @param {string} data.title - 项目名称
     * @param {string} data.voiceId - 主音色ID
     * @param {string} data.rawText - 完整原文
     * @returns {Promise<Object>} 项目对象
     */
    create(data) {
        return request.post('/novel/create', data)
    },

    /**
     * 启动 AI Pipeline（异步）
     * @param {number} id - 项目ID
     * @param {string} rawText - 原文
     * @returns {Promise}
     */
    startPipeline(id, rawText) {
        return request.post(`/novel/${id}/start`, { rawText })
    },

    /**
     * 查询用户项目列表
     * @returns {Promise<Array>}
     */
    list() {
        return request.get('/novel/list')
    },

    /**
     * 查询项目详情
     * @param {number} id - 项目ID
     * @returns {Promise<Object>}
     */
    detail(id) {
        return request.get(`/novel/${id}`)
    },

    /**
     * 查询项目章节列表
     * @param {number} id - 项目ID
     * @returns {Promise<Array>}
     */
    chapters(id) {
        return request.get(`/novel/${id}/chapters`)
    },

    /**
     * 查询章节分段详情
     * @param {number} chapterId - 章节ID
     * @returns {Promise<Array>}
     */
    segments(chapterId) {
        return request.get(`/novel/chapter/${chapterId}/segments`)
    },

    /**
     * 查询项目进度
     * @param {number} id - 项目ID
     * @returns {Promise<{status, progress, totalChapters, audioUrl}>}
     */
    progress(id) {
        return request.get(`/novel/${id}/progress`)
    },

    /**
     * 删除项目
     * @param {number} id - 项目ID
     * @returns {Promise}
     */
    delete(id) {
        return request.delete(`/novel/${id}`)
    }
}
