<!-- 个人中心页 — 信息展示 + 修改手机号/密码 + 爽约统计 -->
<template>
  <div class="student-layout">
    <NavBar />
    <div class="page-container">
      <h1 class="page-title">个人中心</h1>
      <p class="page-subtitle" style="margin-bottom:24px">查看个人信息与爽约记录</p>

      <div class="profile-grid">
        <!-- 基本信息卡片 -->
        <el-card class="profile-card">
          <template #header><span class="card-header">基本信息</span></template>
          <el-descriptions :column="1" border v-if="profile">
            <el-descriptions-item label="学号">{{ profile.studentNo }}</el-descriptions-item>
            <el-descriptions-item label="姓名">{{ profile.name }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ profile.phone }}</el-descriptions-item>
          </el-descriptions>
          <div style="margin-top:16px;display:flex;gap:8px">
            <el-button size="small" @click="phoneDialog=true">修改手机号</el-button>
            <el-button size="small" @click="pwdDialog=true">修改密码</el-button>
          </div>
        </el-card>

        <!-- 爽约状态卡片 -->
        <el-card class="profile-card">
          <template #header><span class="card-header">爽约记录</span></template>
          <div class="violation-stat" v-if="profile">
            <div class="violation-count" :class="{ danger: profile.violationCount >= 3 }">
              {{ profile.violationCount }}
            </div>
            <div class="violation-label">累计爽约次数</div>
            <el-tag v-if="!profile.canReserve" type="danger" size="default" effect="light" style="margin-top:12px">
              ⛔ 已被限制预约
            </el-tag>
            <el-tag v-else type="success" size="default" effect="light" style="margin-top:12px">
              ✅ 可正常预约
            </el-tag>
            <div v-if="profile.bannedUntil" class="banned-info">
              解禁时间：{{ formatDate(profile.bannedUntil) }}
            </div>
          </div>
        </el-card>
      </div>

      <!-- 修改手机号对话框 -->
      <el-dialog v-model="phoneDialog" title="修改手机号" width="380px">
        <el-form :model="phoneForm" :rules="phoneRules" ref="phoneFormRef">
          <el-form-item prop="phone">
            <el-input v-model="phoneForm.phone" placeholder="新手机号" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="phoneForm.password" type="password" placeholder="输入密码确认" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="phoneDialog=false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="submitPhone">确认修改</el-button>
        </template>
      </el-dialog>

      <!-- 修改密码对话框 -->
      <el-dialog v-model="pwdDialog" title="修改密码" width="380px">
        <el-form :model="pwdForm" :rules="pwdRules" ref="pwdFormRef">
          <el-form-item prop="old">
            <el-input v-model="pwdForm.old" type="password" placeholder="原密码" />
          </el-form-item>
          <el-form-item prop="new1">
            <el-input v-model="pwdForm.new1" type="password" placeholder="新密码（8-32位）" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="pwdDialog=false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="submitPwd">确认修改</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import NavBar from '@/components/common/NavBar.vue'

const profile = ref(null)
const phoneDialog = ref(false)
const pwdDialog = ref(false)
const submitting = ref(false)
const phoneFormRef = ref(null)
const pwdFormRef = ref(null)

const phoneForm = reactive({ phone: '', password: '' })
const pwdForm = reactive({ old: '', new1: '' })
const phoneRules = {
  phone: [{ required: true, pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}
const pwdRules = {
  old: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  new1: [{ required: true, min: 8, message: '新密码至少8位', trigger: 'blur' }]
}

async function fetchProfile() {
  try {
    const res = await request.get('/student/profile')
    profile.value = res.data
  } catch { /* ignore */ }
}
async function submitPhone() {
  const v = await phoneFormRef.value.validate().catch(() => false)
  if (!v) return
  submitting.value = true
  try {
    await request.put('/student/phone', { phone: phoneForm.phone, password: phoneForm.password })
    ElMessage.success('手机号更新成功')
    phoneDialog.value = false
    fetchProfile()
  } catch { /* ElMessage */ } finally { submitting.value = false }
}
async function submitPwd() {
  const v = await pwdFormRef.value.validate().catch(() => false)
  if (!v) return
  submitting.value = true
  try {
    await request.put('/student/password', { oldPassword: pwdForm.old, newPassword: pwdForm.new1 })
    ElMessage.success('密码修改成功')
    pwdDialog.value = false
  } catch { /* ElMessage */ } finally { submitting.value = false }
}
function formatDate(d) {
  if (!d) return ''
  return new Date(d).toLocaleString('zh-CN')
}

onMounted(fetchProfile)
</script>

<style scoped>
.student-layout { min-height: 100vh; background: var(--bg); }
.profile-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; max-width: 800px; }
.card-header { font-weight: 600; }
.violation-stat { text-align: center; padding: 20px; }
.violation-count { font-size: 56px; font-weight: 700; color: var(--primary); }
.violation-count.danger { color: var(--danger); }
.violation-label { font-size: 14px; color: var(--text-secondary); margin-top: 4px; }
.banned-info { font-size: 13px; color: var(--danger); margin-top: 8px; }
</style>
