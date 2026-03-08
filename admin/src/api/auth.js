import request from './request'

export const authApi = {
    login(data) {
        return request.post('/auth/login', data)
    },
    getUserInfo() {
        return request.get('/auth/me')
    },
    logout() {
        return request.post('/auth/logout')
    }
}
