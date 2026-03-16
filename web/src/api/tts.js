/**
 * TTS 语音合成 API 模块
 *
 * 提供文本转语音的核心接口：短文本同步合成、长文本异步任务。
 * 情感合成 v2 和 AI 台本生成的接口由各 View 组件直接调用 request 实例。
 *
 * @module api/tts
 */
import request from './request'

export const ttsApi = {
    /** 获取系统预置音色列表 */
    getVoices() {
        return request.get('/tts/voices')
    },

    /**
     * 短文本语音合成（同步返回音频 URL）
     * @param {Object} data - { text, voiceId, speedRatio, volumeRatio, pitchRatio }
     */
    synthesizeShort(data) {
        // TTS 合成调用云端 API，单独设置 60s 超时（新闻播报等长文本分片可能需要较久）
        return request.post('/tts/short', data, { timeout: 60000 })
    },

    /**
     * 长文本异步任务提交
     * @param {Object} data - { text, voiceId, useEmotion }
     * @returns {Promise<string>} 任务 ID
     */
    submitLongText(data) {
        // 长文本任务提交，单独设置 30s 超时
        return request.post('/tts/long-text', data, { timeout: 30000 })
    },

    /**
     * 查询长文本异步任务状态
     * @param {string} taskId - 长文本任务 ID
     * @param {boolean} useEmotion - 是否使用情感推断
     */
    queryLongTextTask(taskId, useEmotion = false) {
        return request.get(`/tts/long-text/${taskId}`, {
            params: { useEmotion }
        })
    },

    /**
     * 音色试听（免费，不扣配额）
     * @param {Object} data - { text?, voiceId }
     */
    preview(data) {
        // 试听也调外部 TTS API，单独设置 20s 超时
        return request.post('/tts/preview', data, { timeout: 20000 })
    }
}

/**
 * AI 播客 API 模块
 * @module api/podcast
 */
export const podcastApi = {
    /** 获取播客预设主题列表 */
    getPresets() {
        return request.get('/podcast/presets')
    },

    /**
     * 生成 AI 播客
     * @param {Object} data - { topic, sourceType, sourceContent, voiceA, voiceB }
     */
    generatePodcast(data) {
        // AI 播客生成，单独设置 120s 超时（内容生成+TTS 耐时较长）
        return request.post('/podcast/generate', data, { timeout: 120000 })
    }
}
