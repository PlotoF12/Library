/**
 * Axios 实例 — 统一拦截器（JWT注入 + 错误处理 + Token自动刷新）。
 *
 * 请求拦截：自动从 authStore 注入 Authorization header
 * 响应拦截：统一处理 code !== 0 的业务异常 + 401自动刷新Token
 */
import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' }
})

// ---- 请求拦截器 — 注入JWT ----
request.interceptors.request.use(
  (config) => {
    // 从 localStorage 获取 token（避免循环依赖 Store）
    const token = localStorage.getItem('accessToken')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    // 注入幂等键（仅 POST/PUT/PATCH 且未自带时）
    if (['post', 'put', 'patch'].includes(config.method) && !config.headers['Idempotency-Key']) {
      config.headers['Idempotency-Key'] = crypto.randomUUID()
    }
    return config
  },
  (error) => Promise.reject(error)
)

// ---- 刷新Token相关 ----
let isRefreshing = false
let refreshSubscribers = []

function onRefreshed(newToken) {
  refreshSubscribers.forEach(cb => cb(newToken))
  refreshSubscribers = []
}
function addRefreshSubscriber(cb) {
  refreshSubscribers.push(cb)
}

async function tryRefreshToken() {
  const refreshToken = localStorage.getItem('refreshToken')
  if (!refreshToken) return null
  try {
    const res = await axios.post('/api/v1/auth/refresh', { refreshToken })
    if (res.data.code === 0) {
      const { accessToken, refreshToken: newRefresh } = res.data.data
      localStorage.setItem('accessToken', accessToken)
      localStorage.setItem('refreshToken', newRefresh)
      return accessToken
    }
  } catch (e) {
    // refresh 失败
  }
  return null
}

// ---- 响应拦截器 — 统一错误处理 ----
request.interceptors.response.use(
  (response) => {
    const body = response.data
    // 业务成功
    if (body.code === 0) {
      return body
    }
    // 业务异常
    ElMessage.error(body.message || '操作失败')
    return Promise.reject(new Error(body.message))
  },
  async (error) => {
    // 网络错误 / 超时
    if (!error.response) {
      ElMessage.error('网络异常，请检查网络连接')
      return Promise.reject(error)
    }

    const { status, config } = error.response

    if (status === 401) {
      // Token 过期 → 尝试刷新
      if (!isRefreshing) {
        isRefreshing = true
        const newToken = await tryRefreshToken()
        isRefreshing = false
        if (newToken) {
          onRefreshed(newToken)
          config.headers.Authorization = `Bearer ${newToken}`
          return request(config) // 重试原请求
        }
        // 刷新失败 → 跳转登录
        localStorage.clear()
        ElMessage.error('登录已过期，请重新登录')
        setTimeout(() => {
          window.location.href = '/login'
        }, 500)
        return Promise.reject(error)
      }
      // 正在刷新中，等待新Token
      return new Promise(resolve => {
        addRefreshSubscriber((newToken) => {
          config.headers.Authorization = `Bearer ${newToken}`
          resolve(request(config))
        })
      })
    }

    const body = error.response.data

    if (status === 400) {
      // 参数校验失败 / 请求格式错误 — 读取后端返回的具体原因
      ElMessage.warning(body?.message || '请求参数有误')
    } else if (status === 403) {
      ElMessage.error(body?.message || '权限不足，无法执行此操作')
    } else if (status === 429) {
      ElMessage.warning(body?.message || '操作过于频繁，请稍后重试')
    } else if (status === 409) {
      // 409 冲突由调用方处理（如预约冲突）
      if (body?.message) ElMessage.warning(body.message)
    } else if (status >= 500) {
      ElMessage.error(body?.message || '服务器异常，请稍后重试')
    } else {
      // 兜底：处理所有其他未预期的 HTTP 错误
      ElMessage.error(body?.message || `请求异常 (${status})`)
    }

    return Promise.reject(error)
  }
)

export default request
