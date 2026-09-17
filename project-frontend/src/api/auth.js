/**
 * 认证 API — 注册、登录、刷新Token、登出。
 */
import request from '@/utils/request'

export const authApi = {
  register: (data) => request.post('/auth/register', data),
  studentLogin: (data) => request.post('/auth/login', data),
  adminLogin: (data) => request.post('/auth/admin/login', data),
  refreshToken: (data) => request.post('/auth/refresh', data),
  logout: () => request.post('/auth/logout')
}
