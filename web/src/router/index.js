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
        path: '/voices',
        name: 'VoiceLibrary',
        component: () => import('../views/VoiceLibrary.vue')
    },
    {
        // 游客可浏览 UI，合成操作在页面内拦截引导登录
        path: '/create',
        name: 'Create',
        component: () => import('../views/Create.vue')
    },
    {
        // 个人中心：纯个人数据，必须登录
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
        // 游客可浏览 UI，合成操作在页面内拦截引导登录
        path: '/emotion',
        name: 'Emotion',
        component: () => import('../views/Emotion.vue')
    },

    {
        // 游客可浏览 UI，生成操作在页面内拦截引导登录
        path: '/podcast',
        name: 'Podcast',
        component: () => import('../views/Podcast.vue')
    },

    {
        // VIP 套餐完全公开，让游客看到权益对比
        path: '/vip',
        name: 'Vip',
        component: () => import('../views/Vip.vue')
    },

    {
        // 我的创作：纯个人数据，必须登录
        path: '/creations',
        name: 'MyCreations',
        component: () => import('../views/MyCreations.vue'),
        meta: { requiresAuth: true }
    },
    {
        // 工作台项目列表：纯个人数据，必须登录
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
        // 游客可浏览 UI，生成操作在页面内拦截引导登录
        path: '/music',
        name: 'Music',
        component: () => import('../views/Music.vue')
    },
    {
        // AI 工坊：空聊天界面对游客无展示价值 + Agent 调用成本最高，保持需登录
        path: '/workshop',
        name: 'AiWorkshop',
        component: () => import('../views/AiWorkshop.vue'),
        meta: { requiresAuth: true }
    },
    {
        // 支付宝 return_url 跳回，必须公开（不需要 requiresAuth，有 orderNo 即可查询）
        path: '/pay-result',
        name: 'PayResult',
        component: () => import('../views/PayResult.vue')
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

// 添加路由导航守卫
router.beforeEach(async (to, from, next) => {
    const authStore = useAuthStore()

    // 1. 首次进入：token 存在但 user 未加载 → 阻塞等待（必须知道身份才能判断权限）
    if (authStore.token && !authStore.user) {
        try {
            await authStore.fetchUserInfo()
        } catch (e) {
            // token 失效 / 后端未启动 → 清除无效 token
            authStore.clearToken()
        }
    }
    // 2. 已有 user → 后台静默刷新 policy（stale-while-revalidate）
    //    不阻塞导航，用户立即看到页面；policy 在下次导航前已更新
    //    企业级做法：Apollo/Nacos 长连接推送；本项目用轻量级 /me 轮询代替
    else if (authStore.token && authStore.user) {
        authStore.fetchUserInfo().catch(() => { })
    }

    if (to.meta.requiresAuth && !authStore.isLoggedIn) {
        if (to.name === 'Login') return next()
        return next({ name: 'Login', query: { redirect: to.fullPath } })
    }

    next()
})


export default router
