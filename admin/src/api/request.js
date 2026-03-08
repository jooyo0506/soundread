import axios from 'axios'
import { useAuthStore } from '../stores/auth'
import router from '../router'

const request = axios.create({
    baseURL: '/api',
    timeout: 60000,
    headers: { 'Content-Type': 'application/json' }
})

request.interceptors.request.use(config => {
    const authStore = useAuthStore()
    if (authStore.token) {
        config.headers.Authorization = authStore.token.startsWith('Bearer ')
            ? authStore.token
            : `Bearer ${authStore.token}`
    }
    return config
})

request.interceptors.response.use(
    response => {
        const res = response.data
        if (res.code === 200 || res.code === 0 || res.code === '200') {
            return res.data
        }
        if ([401, 403].includes(res.code)) {
            useAuthStore().clearToken()
            router.replace({ name: 'Login' })
        }
        return Promise.reject(new Error(res.message || 'Error'))
    },
    error => {
        if ([401, 403].includes(error.response?.status)) {
            useAuthStore().clearToken()
            router.replace({ name: 'Login' })
        }
        const msg = error.response?.data?.message
        return Promise.reject(msg ? new Error(msg) : error)
    }
)

export default request
