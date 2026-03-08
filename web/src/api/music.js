/**
 * AI 音乐 API 模块
 *
 * 提供音乐生成、歌词生成、任务查询、列表、删除等接口。
 *
 * @module api/music
 */
import request from './request'

export const musicApi = {
    /**
     * 提交音乐生成任务
     * @param {Object} data - { type: 'song'|'instrumental', prompt, lyrics?, model? }
     * @returns {Promise<{ taskId, status }>}
     */
    generate(data) {
        return request.post('/music/generate', data)
    },

    /**
     * AI 生成歌词
     * @param {string} prompt - 歌词描述
     * @returns {Promise<{ title, lyrics }>}
     */
    generateLyrics(prompt) {
        return request.post('/music/lyrics', { prompt })
    },

    /**
     * 查询任务状态
     * @param {number|string} taskId
     * @returns {Promise<MusicTask>}
     */
    getTask(taskId) {
        return request.get(`/music/task/${taskId}`)
    },

    /**
     * 用户任务列表
     * @returns {Promise<MusicTask[]>}
     */
    listTasks() {
        return request.get('/music/list')
    },

    /**
     * 删除任务
     * @param {number|string} taskId
     */
    deleteTask(taskId) {
        return request.delete(`/music/task/${taskId}`)
    },

    /** 发布音乐到发现页 */
    publishTask(taskId) {
        return request.post(`/music/task/${taskId}/publish`)
    },

    /** 重命名音乐 */
    renameTask(taskId, title) {
        return request.post(`/music/task/${taskId}/rename`, { title })
    },

    /** 下架音乐 */
    unpublishTask(taskId) {
        return request.post(`/music/task/${taskId}/unpublish`)
    }
}
