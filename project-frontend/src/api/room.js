/**
 * 自习室 API — 自习室列表和详情查询。
 */
import request from '@/utils/request'

export const roomApi = {
  /** 自习室列表（支持楼层和状态筛选） */
  list: (params) => request.get('/rooms', { params }),
  /** 自习室详情（含座位布局和实时状态） */
  detail: (roomId) => request.get(`/rooms/${roomId}`)
}
