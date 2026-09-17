/**
 * 管理员 API — 座位CRUD、预约管理、统计报表、系统配置。
 */
import request from '@/utils/request'

export const adminApi = {
  // 座位管理
  createSeat: (data) => request.post('/admin/seats', data),
  updateSeat: (seatId, data) => request.put(`/admin/seats/${seatId}`, data),
  deleteSeat: (seatId) => request.delete(`/admin/seats/${seatId}`),
  // 预约管理
  listReservations: (params) => request.get('/admin/reservations', { params }),
  // 按学号/手机号搜索学生预约
  searchByStudent: (params) => request.get('/admin/students/search', { params }),
  // 管理员确认签到
  adminSignIn: (reservationId) => request.post(`/admin/reservations/${reservationId}/sign-in`),
  // 管理员确认离座
  adminLeave: (reservationId) => request.post(`/admin/reservations/${reservationId}/leave`),
  // 统计报表
  violationReport: (params) => request.get('/admin/reports/violations', { params }),
  // 系统配置
  updateConfig: (data) => request.put('/admin/config', data)
}
