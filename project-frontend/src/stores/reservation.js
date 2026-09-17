/**
 * 预约状态管理 — Pinia Store。
 * 管理当前用户的预约列表和当前选中的预约。
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { reservationApi } from '@/api/reservation'
import { ElMessage, ElMessageBox } from 'element-plus'

export const useReservationStore = defineStore('reservation', () => {
  const myReservations = ref([])
  const currentReservation = ref(null)
  const loading = ref(false)
  const pagination = ref({ page: 1, size: 20, total: 0, totalPages: 0 })

  // ---- 获取个人预约列表 ----
  async function fetchMyReservations(params = {}) {
    loading.value = true
    try {
      const res = await reservationApi.listMy(params)
      myReservations.value = res.data.list
      pagination.value = res.data.pagination
    } finally {
      loading.value = false
    }
  }

  // ---- 创建预约 ----
  async function createReservation(data) {
    const res = await reservationApi.create(data)
    ElMessage.success(res.message || '预约成功')
    return res.data
  }

  // ---- 签到确认 ----
  async function signIn(reservationId) {
    await ElMessageBox.confirm(
      '确认您已到达座位？点击确定后将完成签到。',
      '确认签到',
      { confirmButtonText: '确认到达座位', cancelButtonText: '取消', type: 'info' }
    )
    const res = await reservationApi.signIn(reservationId)
    ElMessage.success(res.message || '已完成二维码扫描确认')
    return res.data
  }

  // ---- 确认离座 ----
  async function leave(reservationId) {
    await ElMessageBox.confirm(
      '确认要离开座位吗？离开后座位将释放给其他同学使用。',
      '确认离座',
      { confirmButtonText: '确认离座', cancelButtonText: '取消', type: 'warning' }
    )
    const res = await reservationApi.leave(reservationId)
    ElMessage.success(res.message || '离座确认成功')
    return res.data
  }

  // ---- 取消预约 ----
  async function cancelReservation(reservationId, reason) {
    await ElMessageBox.confirm(
      '确定要取消此预约吗？',
      '取消预约',
      { confirmButtonText: '确认取消', cancelButtonText: '返回', type: 'warning' }
    )
    const res = await reservationApi.cancel(reservationId, reason)
    ElMessage.success('预约已取消')
    return res.data
  }

  return {
    myReservations, currentReservation, loading, pagination,
    fetchMyReservations, createReservation, signIn, leave, cancelReservation
  }
})
