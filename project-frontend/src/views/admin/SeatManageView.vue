<!--
  管理员座位管理页 — 自习室下拉 + 座位表格 + 新增/编辑/删除操作。
  支持偏好字段（电源/靠窗/单座）的编辑。
-->
<template>
  <div class="admin-layout">
    <NavBar />
    <div class="page-container">
      <div class="page-header">
        <div>
          <h1 class="page-title">座位管理</h1>
          <p class="page-subtitle">管理自习室座位信息与状态</p>
        </div>
        <el-button type="primary" @click="openCreate"><el-icon><Plus /></el-icon>新增座位</el-button>
      </div>

      <el-card shadow="never" style="margin-bottom:20px">
        <el-form :inline="true">
          <el-form-item label="自习室">
            <el-select v-model="selectedRoom" placeholder="选择自习室" style="width:200px"
                       @change="fetchSeats">
              <el-option v-for="r in roomList" :key="r.roomId" :label="r.name || r.roomName"
                         :value="r.roomId" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button @click="fetchSeats">刷新</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card shadow="never">
        <el-table :data="seats" v-loading="loading" stripe>
          <el-table-column prop="seatId" label="ID" width="70" />
          <el-table-column prop="seatCode" label="座位编号" width="130" />
          <el-table-column label="电源" width="80">
            <template #default="{ row }">{{ row.hasPower ? '⚡ 有' : '—' }}</template>
          </el-table-column>
          <el-table-column label="靠窗" width="80">
            <template #default="{ row }">{{ row.isWindow ? '🪟 是' : '—' }}</template>
          </el-table-column>
          <el-table-column label="单座" width="80">
            <template #default="{ row }">{{ row.isSingle ? '👤 是' : '—' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : row.status === 2 ? 'warning' : 'info'"
                      size="small" effect="light">{{ row.statusText }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200">
            <template #default="{ row }">
              <el-button size="small" @click="openEdit(row)">编辑</el-button>
              <el-button size="small" type="danger" @click="handleDelete(row.seatId)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 新增/编辑对话框 -->
      <el-dialog v-model="dialogVisible" :title="editingSeat ? '编辑座位' : '新增座位'" width="440px">
        <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
          <el-form-item v-if="!editingSeat" label="自习室">
            <el-select v-model="form.roomId" placeholder="选择自习室" style="width:100%">
              <el-option v-for="r in roomList" :key="r.roomId" :label="r.name||r.roomName" :value="r.roomId" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="!editingSeat" label="座位编号" prop="seatCode">
            <el-input v-model="form.seatCode" placeholder="如 S025" />
          </el-form-item>
          <el-form-item label="电源"><el-switch v-model="form.hasPower" :active-value="1" :inactive-value="0" /></el-form-item>
          <el-form-item label="靠窗"><el-switch v-model="form.isWindow" :active-value="1" :inactive-value="0" /></el-form-item>
          <el-form-item label="单座"><el-switch v-model="form.isSingle" :active-value="1" :inactive-value="0" /></el-form-item>
          <el-form-item v-if="editingSeat" label="状态">
            <el-radio-group v-model="form.status">
              <el-radio :value="1">可预约</el-radio>
              <el-radio :value="2">维修中</el-radio>
              <el-radio :value="3">已禁用</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="save">{{ editingSeat ? '保存' : '创建' }}</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { adminApi } from '@/api/admin'
import { roomApi } from '@/api/room'
import { ElMessage, ElMessageBox } from 'element-plus'
import NavBar from '@/components/common/NavBar.vue'

const roomList = ref([])
const seats = ref([])
const loading = ref(false)
const selectedRoom = ref(null)
const dialogVisible = ref(false)
const editingSeat = ref(null)
const saving = ref(false)
const formRef = ref(null)

const form = reactive({ roomId: null, seatCode: '', hasPower: 0, isWindow: 0, isSingle: 0, status: 1 })
const rules = { seatCode: [{ required: true, message: '座位编号不能为空', trigger: 'blur' }] }

async function loadRooms() {
  const res = await roomApi.list({ status: 1, size: 100 })
  roomList.value = res.data.list || []
  if (roomList.value.length > 0 && !selectedRoom.value) {
    selectedRoom.value = roomList.value[0].roomId
    fetchSeats()
  }
}

async function fetchSeats() {
  if (!selectedRoom.value) return
  loading.value = true
  try {
    // 通过自习室详情获取座位列表
    const res = await roomApi.detail(selectedRoom.value)
    seats.value = res.data.seats || []
  } catch (e) { /* ignore */ } finally { loading.value = false }
}

function openCreate() {
  if (!selectedRoom.value) { ElMessage.warning('请先选择自习室'); return }
  editingSeat.value = null
  form.roomId = selectedRoom.value
  form.seatCode = ''
  form.hasPower = 0
  form.isWindow = 0
  form.isSingle = 0
  form.status = 1
  dialogVisible.value = true
}

function openEdit(row) {
  editingSeat.value = row
  form.hasPower = row.hasPower
  form.isWindow = row.isWindow
  form.isSingle = row.isSingle
  form.status = row.status
  dialogVisible.value = true
}

async function save() {
  saving.value = true
  try {
    if (editingSeat.value) {
      await adminApi.updateSeat(editingSeat.value.seatId, {
        status: form.status, hasPower: form.hasPower,
        isWindow: form.isWindow, isSingle: form.isSingle
      })
      ElMessage.success('座位更新成功')
    } else {
      await adminApi.createSeat({
        roomId: form.roomId, seatCode: form.seatCode,
        hasPower: form.hasPower, isWindow: form.isWindow, isSingle: form.isSingle
      })
      ElMessage.success('座位添加成功')
    }
    dialogVisible.value = false
    fetchSeats()
  } catch { /* ElMessage */ } finally { saving.value = false }
}

async function handleDelete(seatId) {
  try {
    await ElMessageBox.confirm('确定要删除此座位吗？建议禁用而非删除。', '确认', { type: 'warning' })
    await adminApi.deleteSeat(seatId)
    ElMessage.success('操作成功')
    fetchSeats()
  } catch { /* 取消或报错 */ }
}

onMounted(loadRooms)
</script>

<style scoped>
.admin-layout { min-height: 100vh; background: var(--bg); }
</style>
