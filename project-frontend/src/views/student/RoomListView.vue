<!-- 自习室列表页 — 楼层筛选 + 卡片网格 -->
<template>
  <div class="student-layout">
    <NavBar />
    <div class="page-container">
      <div class="page-header">
        <div>
          <h1 class="page-title">自习室列表</h1>
          <p class="page-subtitle">选择自习室查看可用座位</p>
        </div>
      </div>

      <!-- 楼层筛选 -->
      <div class="filter-bar">
        <el-radio-group v-model="filterFloor" size="default" @change="fetchRooms">
          <el-radio-button :value="null">全部楼层</el-radio-button>
          <el-radio-button v-for="f in floors" :key="f" :value="f">{{ f }}F</el-radio-button>
        </el-radio-group>
      </div>

      <!-- 自习室卡片 -->
      <div v-loading="loading" class="room-grid">
        <div v-for="room in rooms" :key="room.roomId" class="room-card" @click="goRoom(room.roomId)">
          <div class="room-card-header">
            <span class="room-code">{{ room.roomCode }}</span>
            <el-tag :type="room.status === 1 ? 'success' : 'info'" size="small" effect="light">
              {{ room.statusText }}
            </el-tag>
          </div>
          <h3 class="room-name">{{ room.name }}</h3>
          <div class="room-meta">
            <span><el-icon><OfficeBuilding /></el-icon> {{ room.floor }}F</span>
          </div>
          <div class="room-seats">
            <div class="seat-stat">
              <span class="num available-num">{{ room.availableSeats }}</span>
              <span class="label">可用</span>
            </div>
            <div class="seat-divider">/</div>
            <div class="seat-stat">
              <span class="num">{{ room.totalSeats }}</span>
              <span class="label">总数</span>
            </div>
          </div>
          <el-progress
            :percentage="room.totalSeats ? Math.round(room.availableSeats / room.totalSeats * 100) : 0"
            :color="progressColor(room)"
            :stroke-width="6"
          />
        </div>
        <el-empty v-if="!loading && rooms.length === 0" description="暂无可用的自习室" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { roomApi } from '@/api/room'
import NavBar from '@/components/common/NavBar.vue'

const router = useRouter()
const rooms = ref([])
const loading = ref(false)
const filterFloor = ref(null)
const floors = ref([1, 2, 3, 4, 5])

async function fetchRooms() {
  loading.value = true
  try {
    const params = { status: 1, size: 100 }
    if (filterFloor.value) params.floor = filterFloor.value
    const res = await roomApi.list(params)
    rooms.value = res.data.list
  } finally { loading.value = false }
}

function goRoom(roomId) { router.push(`/student/rooms/${roomId}`) }

function progressColor(room) {
  const pct = room.totalSeats ? room.availableSeats / room.totalSeats : 0
  if (pct > 0.5) return '#10b981'
  if (pct > 0.2) return '#f59e0b'
  return '#ef4444'
}

onMounted(fetchRooms)
</script>

<style scoped>
.student-layout { min-height: 100vh; background: var(--bg); }
.room-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}
.room-card {
  background: #fff;
  border-radius: var(--radius);
  padding: 20px 24px;
  border: 1px solid var(--border);
  cursor: pointer;
  transition: all var(--transition);
}
.room-card:hover {
  box-shadow: 0 8px 25px rgba(37,99,235,0.12);
  transform: translateY(-2px);
  border-color: var(--primary-border);
}
.room-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.room-code {
  font-size: 13px;
  color: var(--primary);
  font-weight: 600;
  background: var(--primary-bg);
  padding: 2px 10px;
  border-radius: 4px;
}
.room-name { font-size: 18px; font-weight: 600; margin-bottom: 8px; color: var(--text-primary); }
.room-meta { font-size: 13px; color: var(--text-secondary); margin-bottom: 16px; display: flex; align-items: center; gap: 4px; }
.room-seats { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.seat-stat { text-align: center; }
.seat-stat .num { display: block; font-size: 24px; font-weight: 700; }
.seat-stat .label { font-size: 12px; color: var(--text-secondary); }
.available-num { color: var(--primary); }
.seat-divider { font-size: 20px; color: var(--border); }
</style>
