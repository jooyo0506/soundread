import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes = [
    {
        path: '/login',
        name: 'Login',
        component: () => import('../views/Login.vue')
    },
    {
        path: '/',
        component: () => import('../components/AdminLayout.vue'),
        meta: { requiresAuth: true },
        children: [
            {
                path: '',
                name: 'Works',
                component: () => import('../views/AdminWorks.vue')
            },
            {
                path: 'policy',
                name: 'Policy',
                component: () => import('../views/AdminPolicy.vue')
            }
        ]
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

router.beforeEach(async (to, from, next) => {
    const authStore = useAuthStore()

    if (authStore.token && !authStore.user) {
        try {
            await authStore.fetchUserInfo()
            if (!authStore.user?.admin) {
                authStore.clearToken()
                return next({ name: 'Login' })
            }
        } catch (e) {
            authStore.clearToken()
        }
    }

    if (to.meta.requiresAuth && !authStore.isLoggedIn) {
        return next({ name: 'Login' })
    }

    next()
})

export default router
