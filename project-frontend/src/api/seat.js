/**
 * 座位 API — 可用座位查询（支持偏好筛选：电源、靠窗、单座）。
 */
import request from '@/utils/request'

export const seatApi = {
  list: (params) => request.get('/seats', { params })
}
