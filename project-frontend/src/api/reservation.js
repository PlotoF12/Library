/**
 * 预约 API — 创建、签到、离座、取消、查询。
 * 创建请求自动携带 Idempotency-Key（由 request.js 拦截器注入）。
 */
import request from '@/utils/request'

export const reservationApi = {
  create: (data) => request.post('/reservations', data),
  signIn: (reservationId) => request.post(`/reservations/${reservationId}/sign-in`),
  leave: (reservationId) => request.post(`/reservations/${reservationId}/leave`),
  cancel: (reservationId, reason) =>
    request.post(`/reservations/${reservationId}/cancel`, { reason }),
  listMy: (params) => request.get('/reservations/my', { params }),
  detail: (reservationId) => request.get(`/reservations/${reservationId}`)
}
