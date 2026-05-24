import { createRouter, createWebHistory } from 'vue-router'

const routes = [
    {
        path: '/login',
        name: 'Login',
        component: () => import('@/views/Login.vue')
    },
    {
        path: '/',
        component: () => import('@/views/Layout.vue'),
        redirect: '/home',
        children: [
            { path: 'home', component: () => import('@/views/Home.vue') },
            { path: 'seckill', component: () => import('@/views/Seckill.vue') },
            { path: 'order', component: () => import('@/views/Order.vue') },
            { path: 'profile', component: () => import('@/views/Profile.vue') }  // 新增路由
        ]
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

router.beforeEach((to, from, next) => {
    const token = localStorage.getItem('token')
    if (to.path !== '/login' && !token) {
        next('/login')
    } else {
        next()
    }
})

export default router