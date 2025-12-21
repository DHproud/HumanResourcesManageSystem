import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Layout from '../views/Layout.vue' // 假设你有一个公共布局组件

const routes = [
    {
        path: '/login',
        name: 'Login',
        component: Login
    },
    {
        path: '/',
        component: Layout,
        redirect: '/dashboard',
        children: [
            {
                path: 'dashboard',
                name: 'Dashboard',
                component: () => import('../views/Dashboard.vue'),
                meta: { title: '首页' }
            },
            // --- 管理员路由 ---
            {
                path: 'sys/org',
                name: 'OrgManage',
                component: () => import('../views/sys/OrgManage.vue'),
                meta: { title: '机构管理' }
            },
            {
                path: 'sys/position', // 对应菜单的 index
                name: 'PositionManage',
                component: () => import('../views/sys/PositionManage.vue'), // 对应您的文件路径
                meta: { title: '职位管理' }
            },
            // --- 人事经理路由 ---
            {
                path: 'archive/review',
                name: 'ArchiveReview',
                component: () => import('../views/archive/ArchiveReviewList.vue'), // 确保文件存在
                meta: { title: '档案复核' }
            },
            // --- 人事专员路由 (你的 test 账号会跳这里) ---
            {
                path: 'archive/register',
                name: 'ArchiveRegister',
                component: () => import('../views/archive/ArchiveRegister.vue'),
                meta: { title: '档案登记' }
            }
        ]
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

// 简单的路由守卫
router.beforeEach((to, from, next) => {
    const token = localStorage.getItem('token')
    if (to.path !== '/login' && !token) {
        next('/login')
    } else {
        next()
    }
})

export default router