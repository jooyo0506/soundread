/**
 * 音色管理 API 模块
 *
 * 提供系统音色库查询、音色内购、声音样本上传等接口。
 *
 * @module api/voice
 */
import request from './request'

export const voiceApi = {
    /**
     * 获取当前引擎支持的音色列表 + 用户已购资产
     * @param {string} engine - 引擎类型 ('tts-1.0' | 'tts-2.0' | 'podcast')
     * @returns {Promise<{ list: Array, owned: string[] }>}
     */
    getLibrary(engine = 'tts-1.0') {
        return request.get('/voice/library', { params: { engine } })
    },

    /**
     * 购买高级音色
     * @param {string} voiceId - 目标音色 ID
     * @param {string} payMethod - 支付方式 ('wechat' | 'alipay')
     * @returns {Promise<string>} 订单 ID
     */
    purchase(voiceId, payMethod = 'wechat') {
        return request.post('/voice/purchase', { voiceId, payMethod })
    },

    /**
     * 上传声音样本（声音克隆第一步）
     * @param {FormData} data - 包含音频文件的 FormData
     */
    uploadSample(data) {
        return request.post('/voice/upload-sample', data, {
            headers: { 'Content-Type': 'multipart/form-data' }
        })
    }
}
