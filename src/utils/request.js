import axios from 'axios'
import JSONbig from 'json-bigint'
import { ElMessage } from 'element-plus'
import router from '@/router' // 引入路由，用于跳转登录页

// helper: encode id to safe string for URL usage
export function encodeId(id) {
    if (id === null || id === undefined) return ''
    return encodeURIComponent(String(id))
}

const request = axios.create({
    baseURL: 'http://localhost:8080', // 你的后端地址
    timeout: 10000,
    // 全局响应解析：使用 json-bigint，storeAsString: true 会把超大整数以字符串形式保留
    transformResponse: [function (data) {
        if (!data) return data
        try {
            return JSONbig({ storeAsString: true }).parse(data)
        } catch (err) {
            // 如果不是 JSON 或解析失败，回退到原始解析或返回原始字符串
            try {
                return JSON.parse(data)
            } catch (e) {
                return data
            }
        }
    }]
})

// ==========================================
// request 拦截器 (发送请求前执行)
// ==========================================
request.interceptors.request.use(config => {
    config.headers['Content-Type'] = 'application/json;charset=utf-8';

    // 1. 从浏览器缓存中获取 Token
    const token = localStorage.getItem('token');

    // 2. 如果有 Token，就添加到请求头 Authorization 中
    if (token) {
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
        // 由于我们在 transformResponse 已经解析了 body，response.data 可能是对象或字符串
        let res = response.data;

        // 兼容：如果 transformResponse 未处理而返回了字符串，再次尝试解析（保守处理）
        if (typeof res === 'string') {
            try {
                res = res ? JSON.parse(res) : res
            } catch (e) {
                // ignore
            }
        }

        // 如果后端统一使用 Result 结构（code/msg/data），直接返回 res
        return res;
    },
    error => {
        // 处理 HTTP 状态码错误
        if (error.response) {
            const status = error.response.status;
            const data = error.response.data || {}
            const serverMsg = data.msg || data.message || (typeof data === 'string' ? data : null)

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
            // 404/500 等
            else {
                ElMessage.error(serverMsg || '服务器接口异常');
            }
        } else {
            ElMessage.error('网络连接超时或服务器未启动');
        }
        return Promise.reject(error)
    }
)

export default request