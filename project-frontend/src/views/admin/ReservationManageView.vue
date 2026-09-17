<!--
  管理员预约管理页 — 学生搜索 + 多条件筛选 + 表格 + 签到/离座操作。
-->
<template>
  <div class="admin-layout">
    <NavBar />
    <div class="page-container">
      <div class="page-header">
        <div>
          <h1 class="page-title">预约管理</h1>
          <p class="page-subtitle">按学号/手机号搜索学生，查看与管理预约</p>
        </div>
      </div>

      <!-- ========== 学生搜索栏 ========== -->
      <el-card shadow="never" class="search-card">
        <div class="search-row">
          <el-input v-model="searchKeyword" placeholder="输入学号或手机号" clearable
                    style="width: 280px" size="default" @keyup.enter="handleSearch">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-button type="primary" @click="handleSearch" :loading="searchLoading">
            <el-icon><Search /></el-icon> 查询学生预约
          </el-button>
          <el-button v-if="searchResult.total > 0" @click="clearSearch">清除结果</el-button>
        </div>
        <div v-if="searchResult.total !== undefined" class="search-tip">
          <template v-if="searchResult.total > 0">
            找到 <strong>{{ searchResult.total }}</strong> 条待签到/使用中的预约记录
          </template>
          <template v-else>
            未找到该学生的待签到或使用中预约
          </template>
        </div>
      </el-card>

      <!-- 学生搜索结果表格 -->
      <el-card v-if="searchResult.total > 0" shadow="never" style="margin-bottom:24px">
        <template #header><span style="font-weight:600">搜索结果</span></template>
        <el-table :data="searchResult.list" stripe style="width:100%"
                  :row-class-name="rowClass">
          <el-table-column prop="reservationId" label="ID" width="70" />
          <el-table-column prop="studentNo" label="学号" width="110" />
          <el-table-column prop="studentName" label="姓名" width="90" />
          <el-table-column prop="roomName" label="自习室" width="120" />
          <el-table-column prop="seatCode" label="座位" width="90" />
          <el-table-column prop="reserveDate" label="日期" width="110" />
          <el-table-column label="时段" width="140">
            <template #default="{ row }">{{ row.startTime }} — {{ row.endTime }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" size="small" effect="light">
                {{ row.statusText }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.status === 1" type="primary" size="small"
                         @click="handleAdminSignIn(row.reservationId)">
                确认已签到
              </el-button>
              <el-button v-if="row.status === 2" type="warning" size="small"
                         @click="handleAdminLeave(row.reservationId)">
                确认已离座
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination v-if="searchResult.total > 20"
                       v-model:current-page="searchPage"
                       :page-size="20" :total="searchResult.total"
                       layout="prev, pager, next" @current-change="handleSearch" />
      </el-card>

      <!-- ========== 全局筛选栏 ========== -->
      <el-card class="filter-card" shadow="never">
        <el-form :inline="true" :model="filters" size="default">
          <el-form-item label="日期">
            <el-date-picker v-model="filters.date" type="date" placeholder="选择日期"
                            :disabled-date="notToday" clearable style="width:150px" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="filters.status" placeholder="全部状态" clearable style="width:130px">
              <el-option label="待签到" :value="1" />
              <el-option label="使用中" :value="2" />
              <el-option label="已完成" :value="3" />
              <el-option label="已爽约" :value="4" />
              <el-option label="已取消" :value="5" />
            </el-select>
          </el-form-item>
          <el-form-item label="自习室">
            <el-select v-model="filters.roomId" placeholder="全部" clearable style="width:150px">
              <el-option v-for="r in roomList" :key="r.roomId" :label="r.name||r.roomName" :value="r.roomId" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-checkbox v-model="filters.onlyNoShow" @change="fetchList">
              仅显示爽约记录
            </el-checkbox>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="fetchList">查询</el-button>
            <el-button @click="resetFilters">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 统计卡片 -->
      <div class="stat-cards">
        <div class="stat-card"><div class="stat-value">{{ stats.total }}</div><div class="stat-label">今日预约总数</div></div>
        <div class="stat-card warning"><div class="stat-value">{{ stats.noShow }}</div><div class="stat-label">爽约数</div></div>
        <div class="stat-card success"><div class="stat-value">{{ stats.active }}</div><div class="stat-label">使用中</div></div>
      </div>

      <!-- 预约表格 -->
      <el-card shadow="never">
        <el-table :data="reservations" v-loading="loading" stripe style="width:100%"
                  :row-class-name="rowClass">
          <el-table-column prop="reservationId" label="ID" width="70" />
          <el-table-column prop="studentNo" label="学号" width="120" />
          <el-table-column prop="studentName" label="姓名" width="100" />
          <el-table-column prop="phone" label="手机号" width="120" />
          <el-table-column prop="roomName" label="自习室" width="120" />
          <el-table-column prop="seatCode" label="座位" width="100" />
          <el-table-column prop="reserveDate" label="日期" width="110" />
          <el-table-column label="时段" width="140">
            <template #default="{ row }">{{ row.startTime }} — {{ row.endTime }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" size="small" effect="light">
                {{ row.statusText }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="actualSignTime" label="签到时间" width="160" />
          <el-table-column prop="actualLeaveTime" label="离座时间" width="160" />
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.status === 1" type="primary" size="small"
                         @click="handleAdminSignIn(row.reservationId)">
                确认已签到
              </el-button>
              <el-button v-if="row.status === 2" type="warning" size="small"
                         @click="handleAdminLeave(row.reservationId)">
                确认已离座
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination v-model:current-page="page" :page-size="size" :total="total"
                       layout="total, prev, pager, next" @current-change="fetchList" />
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { adminApi } from '@/api/admin'
import { roomApi } from '@/api/room'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import NavBar from '@/components/common/NavBar.vue'

const reservations = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const roomList = ref([])
const stats = reactive({ total: 0, noShow: 0, active: 0 })

const filters = reactive({ date: '', status: null, roomId: null, onlyNoShow: false })
const notToday = (d) => {
  const t = new Date()
  return d.getFullYear() !== t.getFullYear() || d.getMonth() !== t.getMonth() || d.getDate() !== t.getDate()
}

// ==================== 学生搜索 ====================
const searchKeyword = ref('')
const searchLoading = ref(false)
const searchPage = ref(1)
const searchResult = reactive({ list: [], total: undefined })

async function handleSearch() {
  const kw = searchKeyword.value.trim()
  if (!kw) { ElMessage.warning('请输入学号或手机号'); return }
  searchLoading.value = true
  try {
    const res = await adminApi.searchByStudent({ keyword: kw, page: searchPage.value, size: 20 })
    searchResult.list = res.data.list || []
    searchResult.total = res.data.pagination?.total || 0
  } catch { /* ElMessage handled */ }
  finally { searchLoading.value = false }
}

function clearSearch() {
  searchKeyword.value = ''
  searchPage.value = 1
  searchResult.list = []
  searchResult.total = undefined
}

// ==================== 管理员签到/离座 ====================
async function handleAdminSignIn(id) {
  try {
    await ElMessageBox.confirm(
      '确认该学生已到达座位？系统将记录实际签到时间。',
      '确认签到', { confirmButtonText: '确认已签到', cancelButtonText: '取消', type: 'info' }
    )
    await adminApi.adminSignIn(id)
    ElMessage.success('已确认签到')
    refreshAll()
  } catch { /* 取消 */ }
}

async function handleAdminLeave(id) {
  try {
    await ElMessageBox.confirm(
      '确认该学生已离开座位？系统将记录实际离座时间并释放座位。',
      '确认离座', { confirmButtonText: '确认已离座', cancelButtonText: '取消', type: 'warning' }
    )
    await adminApi.adminLeave(id)
    ElMessage.success('已确认离座，座位已释放')
    refreshAll()
  } catch { /* 取消 */ }
}

function refreshAll() {
  fetchList()
  if (searchKeyword.value.trim()) handleSearch()
}

// ==================== 全局列表 ====================
async function fetchList() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (filters.date) params.date = new Date(filters.date).toISOString().slice(0, 10)
    if (filters.status) params.status = filters.status
    if (filters.roomId) params.roomId = filters.roomId
    if (filters.onlyNoShow) params.onlyNoShow = true
    const res = await adminApi.listReservations(params)
    reservations.value = res.data.list
    total.value = res.data.pagination?.total || 0
    stats.total = total.value
    stats.noShow = reservations.value.filter(r => r.isNoShow).length
    stats.active = reservations.value.filter(r => r.status === 2).length
  } finally { loading.value = false }
}

function resetFilters() {
  filters.date = ''
  filters.status = null
  filters.roomId = null
  filters.onlyNoShow = false
  page.value = 1
  fetchList()
}

function statusTagType(s) {
  return { 1: 'warning', 2: '', 3: 'success', 4: 'danger', 5: 'info' }[s] || 'info'
}

function rowClass({ row }) {
  return row.isNoShow ? 'row-no-show' : ''
}

onMounted(async () => {
  const res = await roomApi.list({ status: 1, size: 100 })
  roomList.value = res.data.list || []
  fetchList()
})
</script>

<style scoped>
.admin-layout { min-height: 100vh; background: var(--bg); }
.filter-card { margin-bottom: 20px; }
.search-card { margin-bottom: 20px; }
.search-row { display: flex; align-items: center; gap: 12px; }
.search-tip { margin-top: 10px; font-size: 14px; color: var(--text-secondary); }
.search-tip strong { color: var(--primary); }
:deep(.row-no-show) { background: #fff7ed !important; }
:deep(.row-no-show:hover) { background: #ffedd5 !important; }
</style>
