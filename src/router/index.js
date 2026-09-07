import { createRouter, createWebHistory } from 'vue-router'

const routes = [
    {
        path: '/',
        component: () => import('@/components/FrontendLayout.vue'),
        children: [
            { path: '', name: 'home', component: () => import('@/views/home.vue') },
            { path: 'consultation', name: 'consultation', component: () => import('@/views/consultation.vue') },
            { path: 'emotion-diary', name: 'emotionDiary', component: () => import('@/views/emotionDiary.vue') },
            { path: 'knowledge', name: 'frontendKnowledge', component: () => import('@/views/frontendKnowledge.vue') },
            { path: 'knowledge/article/:id', name: 'articleDetail', component: () => import('@/views/articleDetail.vue'), props: true }
        ]
    },
    {
        path: '/auth',
        component: () => import('@/components/AuthLayout.vue'),
        children: [
            { path: 'login', name: 'login', component: () => import('@/views/login.vue') },
            { path: 'register', name: 'register', component: () => import('@/views/register.vue') }
        ]
    },
    {
        path: '/back',
        component: () => import('@/components/BackendLayout.vue'),
        children: [
            { path: 'dashboard', name: 'dashboard', component: () => import('@/views/dashboard.vue'), meta: { title: '数据看板' } },
            { path: 'knowledge', name: 'knowledge', component: () => import('@/views/knowledge.vue'), meta: { title: '知识库管理' } },
            { path: 'consultations', name: 'consultations', component: () => import('@/views/consultations.vue'), meta: { title: '咨询记录' } },
            { path: 'emotional', name: 'emotional', component: () => import('@/views/emotional.vue'), meta: { title: '情绪日记' } }
        ]
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

// 路由守卫：后台管理路由需要登录
router.beforeEach((to, from, next) => {
    if (to.path.startsWith('/back') && !localStorage.getItem('token')) {
        next('/auth/login')
    } else {
        next()
    }
})

export default router
