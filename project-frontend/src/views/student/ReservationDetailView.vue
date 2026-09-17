<!-- 预约详情页 -->
<template>
  <div class="student-layout">
    <NavBar />
    <div class="page-container">
      <el-breadcrumb separator="→" class="breadcrumb">
        <el-breadcrumb-item :to="{ path: '/student/reservations' }">我的预约</el-breadcrumb-item>
        <el-breadcrumb-item>预约详情</el-breadcrumb-item>
      </el-breadcrumb>
      <div v-loading="loading">
        <el-card v-if="detail" class="detail-card">
          <template #header>
            <div class="card-title-row">
              <span>预约 #{{ detail.reservationId }}</span>
              <el-tag :type="statusTagType(detail.status)" size="large" effect="light">
                {{ detail.statusText }}
              </el-tag>
            </div>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="自习室">{{ detail.roomName || '—' }}</el-descriptions-item>
            <el-descriptions-item label="座位编号">{{ detail.seatCode || '—' }}</el-descriptions-item>
            <el-descriptions-item label="预约日期">{{ detail.reserveDate }}</el-descriptions-item>
            <el-descriptions-item label="时段">{{ detail.startTime }} — {{ detail.endTime }}</el-descriptions-item>
            <el-descriptions-item label="签到时间">{{ detail.actualSignTime || '未签到' }}</el-descriptions-item>
            <el-descriptions-item label="离座时间">{{ detail.actualLeaveTime || '未离座' }}</el-descriptions-item>
            <el-descriptions-item v-if="detail.duration" label="使用时长">{{ detail.duration }}</el-descriptions-item>
          </el-descriptions>
          <div class="detail-actions" v-if="detail.status === 1 || detail.status === 2">
            <el-button v-if="detail.status === 1" type="primary" @click="signIn">确认到达座位</el-button>
            <el-button v-if="detail.status === 1" @click="cancel">取消预约</el-button>
            <el-button v-if="detail.status === 2" type="warning" @click="leave">确认离座</el-button>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { reservationApi } from '@/api/reservation'
import { useReservationStore } from '@/stores/reservation'
import NavBar from '@/components/common/NavBar.vue'

const route = useRoute()
const router = useRouter()
const store = useReservationStore()

const loading = ref(false)
const detail = ref(null)

function statusTagType(s) {
  const m = { 1: 'warning', 2: '', 3: 'success', 4: 'danger', 5: 'info' }
  return m[s] || 'info'
}
async function fetchDetail() {
  loading.value = true
  try {
    const res = await reservationApi.detail(route.params.id)
    detail.value = res.data
  } finally { loading.value = false }
}
async function signIn() {
  await store.signIn(detail.value.reservationId)
  fetchDetail()
}
async function leave() {
  await store.leave(detail.value.reservationId)
  fetchDetail()
}
async function cancel() {
  await store.cancelReservation(detail.value.reservationId)
  fetchDetail()
}
onMounted(fetchDetail)
</script>

<style scoped>
.student-layout { min-height: 100vh; background: var(--bg); }
.breadcrumb { margin-bottom: 16px; }
.detail-card { max-width: 700px; }
.card-title-row { display: flex; justify-content: space-between; align-items: center; }
.detail-actions { display: flex; gap: 12px; margin-top: 20px; }
</style>
