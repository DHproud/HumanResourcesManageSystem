import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router' // 引入路由，用于跳转登录页

const request = axios.create({
    baseURL: 'http://localhost:8080', // 你的后端地址
    timeout: 5000
})

// ==========================================
// request 拦截器 (发送请求前执行)
// ==========================================
request.interceptors.request.use(config => {
    config.headers['Content-Type'] = 'application/json;charset=utf-8';

    // 1. 从浏览器缓存中获取 Token
    // 注意：这里的 key 'token' 必须和你登录页面存储时使用的 key 一致
    const token = localStorage.getItem('token');

    // 2. 如果有 Token，就添加到请求头 Authorization 中
    if (token) {
        // 注意：有些后端需要 'Bearer ' 前缀，但你的后端 JwtUtils 没写前缀逻辑，所以直接传即可
        config.headers['Authorization'] = token;
    }

    return config
}, error => {
    return Promise.reject(error)
});

// ==========================================
// response 拦截器 (接收响应后执行)
// ==========================================
request.interceptors.response.use(
    response => {
        let res = response.data;

        // 兼容处理：如果返回的是字符串尝试解析成 JSON
        if (typeof res === 'string') {
            try {
                res = res ? JSON.parse(res) : res
            } catch (e) {
                console.error('JSON解析失败', e)
            }
        }

        // 这里的 code === 200 是你在 Result.java 里定义的成功状态码
        // 如果不是 200，说明业务逻辑出错（比如密码错误），但不一定是 HTTP 错误
        return res;
    },
    error => {
        // 处理 HTTP 状态码错误
        if (error.response) {
            const status = error.response.status;

            // 【401 未授权】：Token 过期或无效
            if (status === 401) {
                ElMessage.error('登录凭证已过期，请重新登录');
                localStorage.removeItem('token'); // 清除脏数据
                localStorage.removeItem('user');
                localStorage.removeItem('role');
                router.push('/login'); // 强制跳回登录页
            }
            // 【403 禁止访问】：角色权限不足
            else if (status === 403) {
                ElMessage.warning('您的权限不足，无法执行此操作');
            }
            // 其他错误
            else {
                ElMessage.error(error.response.data.msg || '服务器接口异常');
            }
        } else {
            ElMessage.error('网络连接超时或服务器未启动');
        }
        return Promise.reject(error)
    }
)

export default request