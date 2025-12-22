import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Layout from '../views/Layout.vue'
import { ElMessage } from 'element-plus'

const routes = [
    { path: '/login', name: 'Login', component: Login },

    {
        path: '/',
        component: Layout,
        redirect: '/dashboard',
        children: [
            { path: 'dashboard', name: 'Dashboard', component: () => import('../views/Dashboard.vue'), meta: { title: '首页' } },

            // 管理员专属
            { path: 'sys/org', name: 'OrgManage', component: () => import('../views/sys/OrgManage.vue'), meta: { title: '机构管理', roles: ['ADMIN'] } },
            { path: 'sys/position', name: 'PositionManage', component: () => import('../views/sys/PositionManage.vue'), meta: { title: '职位管理', roles: ['ADMIN'] } },

            // 人事经理（复核）
            { path: 'archive/review', name: 'ArchiveReview', component: () => import('../views/archive/ArchiveReviewList.vue'), meta: { title: '档案复核', roles: ['MANAGER'] } },

            // 新增：仅用于“已有档案修改”的复核（独立页面，不改原复核页）
            { path: 'archive/modify-review', name: 'ArchiveModifyReview', component: () => import('../views/archive/ArchiveModifyReviewList.vue'), meta: { title: '已存档修改复核', roles: ['MANAGER'] } },
            { path: 'archive/modify-review/:id', name: 'ArchiveModifyReviewDetail', component: () => import('../views/archive/ArchiveModifyReviewDetail.vue'), props: true, meta: { title: '已存档修改复核明细', roles: ['MANAGER'] } },

            // 人事专员（登记）
            { path: 'archive/register', name: 'ArchiveRegister', component: () => import('../views/archive/ArchiveRegister.vue'), meta: { title: '档案登记', roles: ['SPECIALIST'] } },

            // 档案查询：三类都可以看（不声明 roles => 所有人已登录可见）
            { path: 'archive/query', name: 'ArchiveQuery', component: () => import('../views/archive/ArchiveSearch.vue'), meta: { title: '档案查询' } },

            // 明细页：所有登录用户可查看（你也可以限制）
            { path: 'archive/detail/:id', name: 'ArchiveDetail', component: () => import('../views/archive/ArchiveDetail.vue'), props: true, meta: { title: '档案明细' } },

            // 编辑页：仅人事专员可访问
            { path: 'archive/edit/:id', name: 'ArchiveEditPage', component: () => import('../views/archive/ArchiveEditPage.vue'), props: true, meta: { title: '档案编辑', roles: ['SPECIALIST'] } }
        ]
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

router.beforeEach((to, from, next) => {
    const token = localStorage.getItem('token') // 你的登录逻辑把 token 存在这里
    const role = localStorage.getItem('role')   // Login.vue 已经保存 role

    // 未登录且访问非登录页 -> 强制跳转登录
    if (!token && to.name !== 'Login') {
        ElMessage.warning('请先登录')
        return next({ name: 'Login' })
    }

    // 已登录却访问登录页 -> 跳转首页
    if (token && to.name === 'Login') {
        return next({ path: '/' })
    }

    // 如果路由没有设置 roles，则默认允许（只要已登录）
    const roles = to.meta && to.meta.roles
    if (!roles || roles.length === 0) {
        return next()
    }

    // 路由声明了 roles：判断当前用户 role 是否包含在内
    // 注意：role 可能为 null/undefined，如果没有则视为无权限
    if (role && roles.includes(role)) {
        return next()
    } else {
        // 无权限访问
        ElMessage.warning('您的权限不足，无法访问此页面')
        // 取消导航（保持在当前页），或者跳转到首页或 403 页面：
        // return next({ name: 'Dashboard' })
        return next(false)
    }
})

export default router