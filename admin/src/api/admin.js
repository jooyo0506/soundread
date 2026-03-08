import request from './request'

export const adminApi = {
    getPolicies() {
        return request.get('/admin/policy/list')
    },
    updatePolicy(id, data) {
        return request.put(`/admin/policy/${id}`, data)
    },
    createPolicy(data) {
        return request.post('/admin/policy', data)
    },
    deletePolicy(id) {
        return request.delete(`/admin/policy/${id}`)
    },
    refreshCache() {
        return request.post('/admin/policy/refresh')
    },

    // ═══ 作品管理 ═══
    listWorks(params = {}) {
        return request.get('/admin/works/list', { params })
    },
    reviewWork(id, action, reviewNote = '') {
        return request.put(`/admin/works/${id}/review`, { action, reviewNote })
    },
    toggleFeatured(id) {
        return request.put(`/admin/works/${id}/feature`)
    },
    deleteWork(id) {
        return request.delete(`/admin/works/${id}`)
    },
    getWorksStats() {
        return request.get('/admin/works/stats')
    },
    togglePublish(id, status) {
        return request.put(`/admin/works/${id}/publish`, { status })
    }
}
