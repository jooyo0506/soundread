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
        return request.post('/tts/short', data)
    },

    /**
     * 长文本异步任务提交
     * @param {Object} data - { text, voiceId, useEmotion }
     * @returns {Promise<string>} 任务 ID
     */
    submitLongText(data) {
        return request.post('/tts/long-text', data)
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
        return request.post('/podcast/generate', data)
    }
}
/**
 * AI \u5267\u672c\u914d\u97f3 API \u6a21\u5757
 *
 * TTS 2.0 \u591a\u89d2\u8272\u4e32\u884c\u914d\u97f3\uff0c\u652f\u6301 section_id \u5173\u8054\u4e0a\u4e0b\u6587\u60c5\u611f\u3002
 *
 * @module api/tts-drama
 */
export const dramaApi = {
    /**
     * \u591a\u89d2\u8272\u5267\u672c\u5408\u6210
     * @param {Object} data - { globalContext, lines: [{ speakerVoiceType, content }] }
     * @returns {Promise<{ audioUrl: string }>}
     */
    synthesize(data) {
        return request.post('/tts/drama/synthesize', data)
    }
}
