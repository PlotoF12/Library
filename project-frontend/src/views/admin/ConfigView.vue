<!--
  管理员系统配置页 — 调整爽约阈值、禁约天数、最晚预约时间等参数。
-->
<template>
  <div class="admin-layout">
    <NavBar />
    <div class="page-container" style="max-width:600px">
      <div class="page-header">
        <div>
          <h1 class="page-title">系统配置</h1>
          <p class="page-subtitle">调整预约规则参数</p>
        </div>
      </div>

      <el-card shadow="never">
        <el-form :model="config" label-width="160px" size="default">
          <el-form-item label="爽约超时时间（分钟）">
            <el-input-number v-model="config.noShowTimeoutMinutes" :min="1" :max="60" />
            <span style="margin-left:8px;font-size:13px;color:var(--text-secondary)">超时未签到视为爽约</span>
          </el-form-item>
          <el-form-item label="爽约触发惩罚阈值（次）">
            <el-input-number v-model="config.violationThreshold" :min="1" :max="20" />
            <span style="margin-left:8px;font-size:13px;color:var(--text-secondary)">累计达到此次数后禁止预约</span>
          </el-form-item>
          <el-form-item label="惩罚禁约天数">
            <el-input-number v-model="config.violationBanDays" :min="1" :max="365" />
            <span style="margin-left:8px;font-size:13px;color:var(--text-secondary)">禁止预约的天数</span>
          </el-form-item>
          <el-form-item label="最晚预约结束时间">
            <el-time-select v-model="config.maxReserveEndTime" placeholder="选择时间"
                            start="18:00" step="00:30" end="22:00" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="saving" @click="saveConfig">保存配置</el-button>
            <el-button @click="resetConfig">恢复默认</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { adminApi } from '@/api/admin'
import { ElMessage } from 'element-plus'
import NavBar from '@/components/common/NavBar.vue'

const saving = ref(false)

const defaults = {
  noShowTimeoutMinutes: 15,
  violationThreshold: 3,
  violationBanDays: 10,
  maxReserveEndTime: '22:00'
}

const config = reactive({ ...defaults })

async function saveConfig() {
  saving.value = true
  try {
    await adminApi.updateConfig({ ...config })
    ElMessage.success('配置已保存')
  } catch { /* ElMessage */ } finally { saving.value = false }
}

function resetConfig() {
  Object.assign(config, defaults)
  ElMessage.info('已恢复默认值（需点击保存生效）')
}
</script>

<style scoped>
.admin-layout { min-height: 100vh; background: var(--bg); }
</style>
