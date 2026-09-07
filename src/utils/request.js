import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
    baseURL: '',
    timeout: 30000
})

// 请求拦截器：附加 Token 请求头
request.interceptors.request.use(
    config => {
        const token = localStorage.getItem('token')
        if (token) {
            config.headers['Token'] = token
        }
        return config
    },
    error => Promise.reject(error)
)

// 401 只处理一次，避免并发请求重复弹窗/跳转
let handlingUnauthorized = false

// 响应拦截器：统一错误处理并解包 Result.data
request.interceptors.response.use(
    response => {
        const res = response.data
        if (res.code !== '200') {
            ElMessage.error(res.msg || '请求失败')
            return Promise.reject(new Error(res.msg || '请求失败'))
        }
        return res.data
    },
    error => {
        if (error.response) {
            const { status, data } = error.response
            if (status === 401) {
                if (!handlingUnauthorized) {
                    handlingUnauthorized = true
                    ElMessage.error('登录已过期，请重新登录')
                    localStorage.removeItem('token')
                    localStorage.removeItem('userInfo')
                    router.push('/auth/login').finally(() => {
                        handlingUnauthorized = false
                    })
                }
            } else {
                ElMessage.error((data && data.msg) || '服务器异常，请稍后重试')
            }
        } else {
            ElMessage.error('网络异常，请稍后重试')
        }
        return Promise.reject(error)
    }
)

export default request
