<!--
  我的预约列表页 — 状态Tab筛选 + 预约卡片 + 签到/离座操作。
  签到成功后弹窗提示"已完成二维码扫描确认"。
-->
<template>
  <div class="student-layout">
    <NavBar />
    <div class="page-container">
      <div class="page-header">
        <h1 class="page-title">我的预约</h1>
      </div>

      <!-- 状态Tab -->
      <el-tabs v-model="activeTab" @tab-change="fetchList" class="status-tabs">
        <el-tab-pane label="全部" name="" />
        <el-tab-pane label="待签到" name="1" />
        <el-tab-pane label="使用中" name="2" />
        <el-tab-pane label="已完成" name="3" />
        <el-tab-pane label="已爽约" name="4" />
        <el-tab-pane label="已取消" name="5" />
      </el-tabs>

      <!-- 预约列表 -->
      <div v-loading="loading">
        <div v-if="reservations.length === 0 && !loading" class="empty-block">
          <el-empty description="暂无预约记录">
            <el-button type="primary" @click="$router.push('/student/rooms')">去预约座位</el-button>
          </el-empty>
        </div>
        <div v-for="r in reservations" :key="r.reservationId" :class="['resv-card', statusClass(r.status)]">
          <div class="card-main">
            <div class="card-left">
              <div class="seat-badge">{{ r.seatCode || '—' }}</div>
              <div class="room-info">{{ r.roomName || '' }}</div>
            </div>
            <div class="card-center">
              <div class="time-row">
                <el-icon><Clock /></el-icon>
                <span>{{ r.startTime }} — {{ r.endTime }}</span>
              </div>
              <div class="date-row">{{ r.reserveDate }}</div>
              <div v-if="r.status === 1 && r.deadline" class="deadline">
                ⏰ 请在 {{ formatDeadline(r.deadline) }} 前确认到达
              </div>
              <div v-if="r.status === 2 && r.actualSignTime" class="signed">
                ✅ {{ r.actualSignTime }} 已签到
              </div>
              <div v-if="r.status === 3 && r.duration" class="completed">
                📊 使用时长：{{ r.duration }}
              </div>
            </div>
            <div class="card-right">
              <el-tag :type="statusTagType(r.status)" size="small" effect="light">
                {{ r.statusText }}
              </el-tag>
            </div>
          </div>
          <div class="card-actions" v-if="r.status === 1 || r.status === 2">
            <el-button v-if="r.status === 1" type="primary" size="small"
                       @click="handleSignIn(r.reservationId)">
              确认到达座位
            </el-button>
            <el-button v-if="r.status === 1" size="small"
                       @click="handleCancel(r.reservationId)">
              取消预约
            </el-button>
            <el-button v-if="r.status === 2" type="warning" size="small"
                       @click="handleLeave(r.reservationId)">
              确认离座
            </el-button>
          </div>
        </div>
      </div>

      <el-pagination v-if="pagination.total > 0" v-model:current-page="page"
                     :page-size="20" :total="pagination.total" layout="prev, pager, next"
                     @current-change="fetchList" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useReservationStore } from '@/stores/reservation'
import NavBar from '@/components/common/NavBar.vue'

const store = useReservationStore()
const activeTab = ref('')
const page = ref(1)

const reservations = ref([])
const pagination = ref({ total: 0 })
const loading = ref(false)

async function fetchList() {
  loading.value = true
  try {
    const params = { page: page.value, size: 20 }
    if (activeTab.value) params.status = parseInt(activeTab.value)
    await store.fetchMyReservations(params)
    reservations.value = store.myReservations
    pagination.value = store.pagination
  } finally { loading.value = false }
}

async function handleSignIn(id) {
  await store.signIn(id)
  fetchList()
}
async function handleLeave(id) {
  await store.leave(id)
  fetchList()
}
async function handleCancel(id) {
  await store.cancelReservation(id)
  fetchList()
}

function statusClass(status) {
  return { 'status-no-show': status === 4 }
}
function statusTagType(status) {
  const m = { 1: 'warning', 2: '', 3: 'success', 4: 'danger', 5: 'info' }
  return m[status] || 'info'
}
function formatDeadline(d) {
  if (!d) return ''
  const dt = new Date(d)
  return `${String(dt.getHours()).padStart(2,'0')}:${String(dt.getMinutes()).padStart(2,'0')}`
}

onMounted(fetchList)
</script>

<style scoped>
.student-layout { min-height: 100vh; background: var(--bg); }
.status-tabs { background: #fff; border-radius: var(--radius); padding: 0 16px; border: 1px solid var(--border); }

.resv-card {
  background: #fff;
  border-radius: var(--radius);
  padding: 16px 20px;
  margin-bottom: 12px;
  border: 1px solid var(--border);
  box-shadow: var(--shadow);
  transition: all var(--transition);
}
.resv-card:hover { box-shadow: var(--shadow-md); }
.resv-card.status-no-show { background: #fff7ed; border-color: #fed7aa; }

.card-main { display: flex; align-items: center; gap: 16px; }
.card-left { flex-shrink: 0; text-align: center; min-width: 80px; }
.seat-badge {
  font-size: 20px; font-weight: 700; color: var(--primary);
  background: var(--primary-bg); padding: 6px 12px; border-radius: 8px;
}
.room-info { font-size: 13px; color: var(--text-secondary); margin-top: 4px; }
.card-center { flex: 1; }
.time-row { display: flex; align-items: center; gap: 6px; font-size: 16px; font-weight: 600; }
.date-row { font-size: 13px; color: var(--text-secondary); margin-top: 2px; }
.deadline { font-size: 13px; color: var(--warning); margin-top: 4px; }
.signed { font-size: 13px; color: var(--success); margin-top: 4px; }
.completed { font-size: 13px; color: var(--text-secondary); margin-top: 4px; }
.card-right { flex-shrink: 0; }
.card-actions { display: flex; gap: 8px; margin-top: 12px; padding-top: 12px; border-top: 1px solid var(--border); }
.empty-block { padding: 60px 0; text-align: center; }
</style>
