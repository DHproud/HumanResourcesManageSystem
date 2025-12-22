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
            { path: 'sys/salary', name: 'SalaryManage', component: () => import('../views/sys/SalaryManage.vue'), meta: { title: '薪酬项目', roles: ['ADMIN'] } },

            // 薪酬标准复核（管理员） — 保持 path 为 /salary/review（ADMIN 专属）
            { path: 'salary/review', name: 'SalaryReviewList', component: () => import('../views/salary/SalaryReviewList.vue'), meta: { title: '薪酬复核', roles: ['ADMIN'] } },
            { path: 'salary/review/:id', name: 'SalaryReviewDetail', component: () => import('../views/salary/SalaryReviewDetail.vue'), props: true, meta: { title: '薪酬复核明细', roles: ['ADMIN'] } },

            // 薪酬标准（薪酬专员/薪酬经理）
            { path: 'salary/standard', name: 'SalaryStandard', component: () => import('../views/salary/SalaryStandard.vue'), meta: { title: '薪酬标准', roles: ['SPECIALIST','MANAGER'] } },

            // 薪酬发放：列表与登记（薪酬专员）
            { path: 'salary/payrun', name: 'PayRunList', component: () => import('../views/salary/PayRunList.vue'), meta: { title: '薪酬发放单', roles: ['SPECIALIST'] } },
            { path: 'salary/payrun/register/:id', name: 'PayRunRegister', component: () => import('../views/salary/PayRunRegister.vue'), props: true, meta: { title: '发放登记', roles: ['SPECIALIST'] } },

            // 薪酬发放复核（经理） — 改为 /salary/pushreview，避免与管理员复核路径冲突
            { path: 'salary/pushreview', name: 'PayRunReviewList', component: () => import('@/views/salary/PayRunReviewList.vue'), meta: { title: '薪酬发放复核', roles: ['MANAGER'] }},
            { path: 'salary/pushreview/:id', name: 'PayRunReviewDetail', component: () => import('@/views/salary/PayRunReviewDetail.vue'), props: true, meta: { title: '薪酬发放复核明细', roles: ['MANAGER'] }},

            { path: 'salary/query', name: 'PayRunSearch', component: () => import('@/views/salary/PayRunSearch.vue'), meta: { title: '薪酬发放查询' } },
            { path: 'salary/query/:id', name: 'PayRunQueryDetail', component: () => import('@/views/salary/PayRunQueryDetail.vue'), props: true, meta: { title: '薪酬发放明细' } },

            // 新增（创建）薪酬标准 —— 无参数
            { path: 'salary/standard/edit', name: 'SalaryStandardEdit', component: () => import('../views/salary/SalaryStandardEdit.vue'), meta: { title: '新增薪酬标准', roles: ['SPECIALIST','MANAGER'] } },

            // 编辑（专用修改页面，按 code 查找并编辑）
            { path: 'salary/standard/change/:code', name: 'SalaryStandardChange', component: () => import('../views/salary/SalaryStandardChange.vue'), props: true, meta: { title: '编辑薪酬标准', roles: ['SPECIALIST','MANAGER'] } },

            // 查看（只读）
            { path: 'salary/standard/view/:code', name: 'SalaryStandardView', component: () => import('../views/salary/SalaryStandardChange.vue'), props: route => ({ code: route.params.code, readonly: true }), meta: { title: '查看薪酬标准', roles: ['SPECIALIST','MANAGER'] } },

            // 档案相关
            { path: 'archive/review', name: 'ArchiveReview', component: () => import('../views/archive/ArchiveReviewList.vue'), meta: { title: '档案复核', roles: ['MANAGER'] } },

            // 新增：仅用于“已有档案修改”的复核（独立页面）
            { path: 'archive/modify-review', name: 'ArchiveModifyReview', component: () => import('../views/archive/ArchiveModifyReviewList.vue'), meta: { title: '已存档修改复核', roles: ['MANAGER'] } },
            { path: 'archive/modify-review/:id', name: 'ArchiveModifyReviewDetail', component: () => import('../views/archive/ArchiveModifyReviewDetail.vue'), props: true, meta: { title: '已存档修改复核明细', roles: ['MANAGER'] } },

            // 人事专员（登记）
            { path: 'archive/register', name: 'ArchiveRegister', component: () => import('../views/archive/ArchiveRegister.vue'), meta: { title: '档案登记', roles: ['SPECIALIST'] } },

            // 档案查询：所有登录用户可见
            { path: 'archive/query', name: 'ArchiveQuery', component: () => import('../views/archive/ArchiveSearch.vue'), meta: { title: '档案查询' } },

            // 明细页：所有登录用户可查看
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

// 更健壮的路由守卫：大小写不敏感，兼容未设置 role 的情况
router.beforeEach((to, from, next) => {
    const token = localStorage.getItem('token') // 登录 token
    const rawRole = (localStorage.getItem('role') || '').toString().trim()
    const role = rawRole ? rawRole.toUpperCase() : ''

    // 未登录 & 访问非登录页 -> 强制跳登录
    if (!token && to.name !== 'Login') {
        ElMessage.warning('请先登录')
        return next({ name: 'Login' })
    }

    // 已登录但访问登录页 -> 跳转首页
    if (token && to.name === 'Login') {
        return next({ path: '/' })
    }

    // 如果路由没有 roles 定义，允许访问
    const roles = to.meta && to.meta.roles
    if (!roles || roles.length === 0) {
        return next()
    }

    // 将路由允许角色统一转为大写比较
    const allowed = roles.map(r => (r || '').toString().trim().toUpperCase())

    if (role && allowed.includes(role)) {
        return next()
    } else {
        ElMessage.warning('您的权限不足，无法访问此页面')
        return next(false)
    }
})

export default router