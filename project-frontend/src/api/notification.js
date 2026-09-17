/**
 * 通知 API — 查询短信通知历史。
 */
import request from '@/utils/request'

export const notificationApi = {
  list: (params) => request.get('/notifications', { params })
}
