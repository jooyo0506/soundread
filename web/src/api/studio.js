import request from './request'

/** Token 键名 (与 auth store 保持一致) */
const TOKEN_KEY = 'sr_token'

/**
 * 获取完整的 Authorization 头值
 * 统一处理 Bearer 前缀，避免各处重复逻辑
 */
function getAuthHeader() {
    const raw = localStorage.getItem(TOKEN_KEY) || ''
    if (!raw) return ''
    return raw.startsWith('Bearer ') ? raw : `Bearer ${raw}`
}

/**
 * SSE fetch 统一错误处理
 * - 401/403 → 跳转登录页
 * - JSON 响应 → 抛出业务错误
 */
async function handleSseResponse(resp) {
    // HTTP 层面的认证失败 → 直接跳转登录
    if (resp.status === 401 || resp.status === 403) {
        localStorage.removeItem(TOKEN_KEY)
        window.location.href = '/login?redirect=' + encodeURIComponent(window.location.pathname)
        throw new Error('请先登录')
    }

    // Content-Type 嗅探: 如果返回 JSON 说明触发了业务异常
    const ct = resp.headers.get('content-type') || ''
    if (ct.includes('application/json')) {
        const err = await resp.json()
        throw new Error(err.message || '请求失败')
    }

    return resp
}

export const studioApi = {
    /** 获取所有创作类型 */
    getTemplates: () => request.get('/studio/templates'),

    /** AI 灵感种子生成 */
    getInspiration: (typeCode) => request.get(`/studio/templates/${typeCode}/inspiration`),

    /** 创建新项目 */
    createProject: (data) => request.post('/studio/projects', data),

    /** 获取我的项目列表 */
    listProjects: () => request.get('/studio/projects'),

    /** 获取项目详情 */
    getProject: (id) => request.get(`/studio/projects/${id}`),

    /** AI 大纲+角色生成（支持结构化参数） */
    generateOutline: (id, params) => request.post(`/studio/projects/${id}/outline`, params || {}, { timeout: 60000 }),

    /** 更新项目 (标题/大纲/角色) */
    updateProject: (id, data) => request.put(`/studio/projects/${id}`, data),

    /** AI 创作生成 (SSE 流式，支持结构化参数) */
    generateContent: async (projectId, data) => {
        // data 可以是 string（兼容旧模式）或 object（结构化模式）
        const body = typeof data === 'string' ? { input: data } : data
        const resp = await fetch(`/api/studio/projects/${projectId}/generate`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': getAuthHeader()
            },
            body: JSON.stringify(body)
        })
        return handleSseResponse(resp)
    },

    /** 广播剧一键生成 — 对话驱动模式（SSE 流式） */
    generateDrama: async (projectId, data) => {
        const resp = await fetch(`/api/studio/projects/${projectId}/drama-generate`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': getAuthHeader()
            },
            body: JSON.stringify(data)
        })
        return handleSseResponse(resp)
    },

    /** AI 改写段落 (SSE 流式) */
    rewriteSection: async (sectionId, instruction) => {
        const resp = await fetch(`/api/studio/sections/${sectionId}/rewrite`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': getAuthHeader()
            },
            body: JSON.stringify({ instruction })
        })
        return handleSseResponse(resp)
    },

    /** 获取项目的所有段落 */
    listSections: (projectId) => request.get(`/studio/projects/${projectId}/sections`),

    /** 保存/更新段落 */
    saveSection: (data) => request.post('/studio/sections', data),

    /** 删除段落 */
    deleteSection: (id) => request.delete(`/studio/sections/${id}`),

    /** 删除项目（含所有段落） */
    deleteProject: (id) => request.delete(`/studio/projects/${id}`),

    /** 发布项目到发现页 */
    publishProject: (id) => request.post(`/studio/projects/${id}/publish`),

    /** 下架作品（从发现页移除） */
    unpublishProject: (id) => request.post(`/studio/projects/${id}/unpublish`),

    /** 解析广播剧剧本 — 提取角色+对白 */
    parseScript: (sectionId) => request.post('/studio/parse-script', { sectionId }),

    /** 清理文本元数据 — 返回纯朗读文本 */
    stripForTTS: (content) => request.post('/studio/strip-for-tts', { content }),

    /** 音频拼接：多段 MP3 → 单文件 */
    concatAudio: (audioUrls) => request.post('/studio/concat-audio', { audioUrls }),

    /** 上传音频文件到 R2（播客发布用） */
    uploadAudio: (blob, filename = 'podcast.mp3') => {
        const formData = new FormData()
        formData.append('file', blob, filename)
        return request.post('/studio/upload-audio', formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        })
    }
}
