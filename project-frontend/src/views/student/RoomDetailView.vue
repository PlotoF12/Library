<!--
  自习室详情页 — 偏好筛选 + 时间选择 + 座位网格 + 预约对话框。
  这是学生选座的核心页面。
-->
<template>
  <div class="student-layout">
    <NavBar />
    <div class="page-container">
      <!-- 面包屑 -->
      <el-breadcrumb separator="→" class="breadcrumb">
        <el-breadcrumb-item :to="{ path: '/student/rooms' }">自习室列表</el-breadcrumb-item>
        <el-breadcrumb-item>{{ room?.roomName || room?.name || '加载中...' }}</el-breadcrumb-item>
      </el-breadcrumb>

      <!-- 自习室信息卡片 -->
      <div class="room-info-bar" v-if="room">
        <div>
          <h1 class="page-title">{{ room.name || room.roomName }}</h1>
          <p class="page-subtitle">{{ room.roomCode }} · {{ room.floor }}F ·
            可用 {{ room.availableSeats }}/{{ room.totalSeats }} 座</p>
        </div>
        <el-tag :type="room.status === 1 ? 'success' : 'info'" size="large" effect="light">
          {{ room.statusText }}
        </el-tag>
      </div>

      <!-- 筛选区 -->
      <div class="filter-section">
        <!-- 偏好筛选 -->
        <div class="filter-row">
          <span class="filter-label">偏好：</span>
          <el-checkbox v-model="filterPower" @change="fetchSeats">有电源 ⚡</el-checkbox>
          <el-checkbox v-model="filterWindow" @change="fetchSeats">靠窗 🪟</el-checkbox>
          <el-checkbox v-model="filterSingle" @change="fetchSeats">独立单座 👤</el-checkbox>
        </div>
        <!-- 时间选择 -->
        <div class="filter-row">
          <span class="filter-label">时段：</span>
          <el-date-picker v-model="reserveDate" type="date" placeholder="日期"
                          :disabled-date="notToday" style="width:140px" @change="fetchSeats" />
          <el-time-select v-model="startTime" placeholder="开始" start="07:00" step="00:30"
                          end="21:30" style="width:120px" @change="fetchSeats" />
          <span style="color:var(--text-secondary)">至</span>
          <el-time-select v-model="endTime" placeholder="结束" start="07:30" step="00:30"
                          end="22:00" style="width:120px" @change="fetchSeats" min-time="08:00" />
        </div>
      </div>

      <!-- 座位网格 -->
      <div v-loading="loading">
        <SeatGrid :seats="seats" @select="showBookDialog" />
      </div>

      <!-- 分页 -->
      <el-pagination v-if="pagination.total > 0" v-model:current-page="page"
                     :page-size="20" :total="pagination.total" layout="prev, pager, next"
                     @current-change="fetchSeats" />
    </div>

    <!-- 预约确认对话框 -->
    <el-dialog v-model="dialogVisible" title="确认预约" width="440px" :close-on-click-modal="false"
               class="book-dialog">
      <div class="book-info" v-if="selectedSeat">
        <div class="book-row"><span>自习室</span><strong>{{ room?.name || room?.roomName }}</strong></div>
        <div class="book-row"><span>座位</span><strong>{{ selectedSeat.seatCode }}</strong></div>
        <div class="book-row"><span>日期</span><strong>{{ reserveDateStr }}</strong></div>
        <div class="book-row"><span>时段</span><strong>{{ startTime }} — {{ endTime }}</strong></div>
        <div class="book-tags" v-if="selectedSeat.hasPower || selectedSeat.isWindow || selectedSeat.isSingle">
          <el-tag v-if="selectedSeat.hasPower" size="small" type="info" effect="plain">⚡ 有电源</el-tag>
          <el-tag v-if="selectedSeat.isWindow" size="small" effect="plain">🪟 靠窗</el-tag>
          <el-tag v-if="selectedSeat.isSingle" size="small" effect="plain">👤 独立单座</el-tag>
        </div>
        <el-alert type="info" :closable="false" show-icon style="margin-top:12px">
          请在 {{ startTime }} 后15分钟内点击确认到达，否则将标记为爽约
        </el-alert>
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="booking" @click="confirmBook">
          确认预约
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { roomApi } from '@/api/room'
import { seatApi } from '@/api/seat'
import { useReservationStore } from '@/stores/reservation'
import { ElMessage } from 'element-plus'
import NavBar from '@/components/common/NavBar.vue'
import SeatGrid from '@/components/common/SeatGrid.vue'

const route = useRoute()
const router = useRouter()
const resvStore = useReservationStore()

const room = ref(null)
const seats = ref([])
const loading = ref(false)
const pagination = ref({ total: 0 })
const page = ref(1)

// 筛选条件
const today = new Date()
const filterPower = ref(false)
const filterWindow = ref(false)
const filterSingle = ref(false)
const reserveDate = ref(today)
const startTime = ref('')
const endTime = ref('')
const reserveDateStr = computed(() => {
  const d = reserveDate.value
  return d ? `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}` : ''
})

const notToday = (d) => {
  const t = new Date()
  return d.getFullYear() !== t.getFullYear() ||
         d.getMonth() !== t.getMonth() ||
         d.getDate() !== t.getDate()
}

// 预约对话框
const dialogVisible = ref(false)
const selectedSeat = ref(null)
const booking = ref(false)

async function fetchRoom() {
  const id = route.params.roomId
  const res = await roomApi.detail(id)
  room.value = res.data
}
async function fetchSeats() {
  if (!startTime.value || !endTime.value) return
  loading.value = true
  try {
    const params = {
      roomId: route.params.roomId,
      date: reserveDateStr.value,
      startTime: startTime.value,
      endTime: endTime.value,
      page: page.value,
      size: 100
    }
    if (filterPower.value) params.hasPower = 1
    if (filterWindow.value) params.isWindow = 1
    if (filterSingle.value) params.isSingle = 1
    const res = await seatApi.list(params)
    seats.value = res.data.list
    pagination.value = res.data.pagination
  } finally { loading.value = false }
}

function showBookDialog(seat) {
  if (!startTime.value || !endTime.value) {
    ElMessage.warning('请先选择预约时段')
    return
  }
  selectedSeat.value = seat
  dialogVisible.value = true
}

async function confirmBook() {
  booking.value = true
  try {
    await resvStore.createReservation({
      seatId: selectedSeat.value.seatId,
      reserveDate: reserveDateStr.value,
      startTime: startTime.value,
      endTime: endTime.value
    })
    dialogVisible.value = false
    router.push('/student/reservations')
  } catch { /* ElMessage已处理 */ }
  finally { booking.value = false }
}

onMounted(async () => {
  await fetchRoom()
  // 默认时段设为当前时间后最近的可选时段
  const now = new Date()
  const h = now.getHours()
  const m = now.getMinutes()
  const roundedM = Math.ceil((m + 1) / 30) * 30
  const sh = roundedM >= 60 ? h + 1 : h
  const sm = roundedM >= 60 ? roundedM - 60 : roundedM
  startTime.value = `${String(Math.max(7, sh)).padStart(2,'0')}:${String(sm).padStart(2,'0')}`
  const eh = sh + 2
  endTime.value = `${String(Math.min(22, eh)).padStart(2,'0')}:00`
  if (startTime.value && endTime.value) fetchSeats()
})
</script>

<style scoped>
.student-layout { min-height: 100vh; background: var(--bg); }
.breadcrumb { margin-bottom: 16px; }
.room-info-bar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}
.filter-section {
  background: #fff;
  border-radius: var(--radius);
  padding: 16px 20px;
  margin-bottom: 20px;
  border: 1px solid var(--border);
}
.filter-row { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; }
.filter-row:last-child { margin-bottom: 0; }
.filter-label { font-size: 14px; font-weight: 600; color: var(--text-secondary); min-width: 50px; }

.book-dialog .book-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid var(--border);
  font-size: 15px;
}
.book-dialog .book-row span { color: var(--text-secondary); }
.book-dialog .book-tags { display: flex; gap: 6px; margin-top: 10px; }
</style>
