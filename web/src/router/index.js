import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes = [
    {
        path: '/',
        name: 'Home',
        component: () => import('../views/Home.vue')
    },
    {
        path: '/discover',
        name: 'Discover',
        component: () => import('../views/Discover.vue')
    },
    {
        path: '/create',
        name: 'Create',
        component: () => import('../views/Create.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/profile',
        name: 'Profile',
        component: () => import('../views/Profile.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/login',
        name: 'Login',
        component: () => import('../views/Login.vue')
    },
    {
        path: '/emotion',
        name: 'Emotion',
        component: () => import('../views/Emotion.vue'),
        meta: { requiresAuth: true }
    },

    {
        path: '/podcast',
        name: 'Podcast',
        component: () => import('../views/Podcast.vue'),
        meta: { requiresAuth: true }
    },

    {
        path: '/vip',
        name: 'Vip',
        component: () => import('../views/Vip.vue'),
        meta: { requiresAuth: true }
    },

    {
        path: '/creations',
        name: 'MyCreations',
        component: () => import('../views/MyCreations.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/studio',
        name: 'Studio',
        component: () => import('../views/Studio.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/studio/:id',
        name: 'StudioWorkbench',
        component: () => import('../views/StudioWorkbench.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/music',
        name: 'Music',
        component: () => import('../views/Music.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/workshop',
        name: 'AiWorkshop',
        component: () => import('../views/AiWorkshop.vue'),
        meta: { requiresAuth: true }
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

// 添加路由导航守卫
router.beforeEach(async (to, from, next) => {
    const authStore = useAuthStore()

    // 1. 如果有 token 但还没用户信息，尝试拉取（后端未启动时不阻断导航）
    if (authStore.token && !authStore.user) {
        try {
            await authStore.fetchUserInfo()
        } catch (e) {
            // 拉取失败（后端未启动/token 过期），清除无效 token
            authStore.clearToken()
        }
    }

    if (to.meta.requiresAuth && !authStore.isLoggedIn) {
        if (to.name === 'Login') return next()
        return next({ name: 'Login', query: { redirect: to.fullPath } })
    }

    next()
})

export default router
