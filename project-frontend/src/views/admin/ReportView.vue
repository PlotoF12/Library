<!--
  管理员统计报表页 — 日期范围 + 爽约统计卡片 + TOP榜表格。
-->
<template>
  <div class="admin-layout">
    <NavBar />
    <div class="page-container">
      <div class="page-header">
        <div>
          <h1 class="page-title">统计报表</h1>
          <p class="page-subtitle">爽约趋势与违规学生排行</p>
        </div>
      </div>

      <!-- 日期筛选 -->
      <el-card shadow="never" style="margin-bottom:20px">
        <el-form :inline="true">
          <el-form-item label="日期范围">
            <el-date-picker v-model="dateRange" type="daterange"
                            range-separator="至" start-placeholder="开始" end-placeholder="结束"
                            style="width:280px" @change="fetchReport" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="fetchReport">查询</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 统计卡片 -->
      <div class="stat-cards">
        <div class="stat-card danger">
          <div class="stat-value">{{ report.totalViolations }}</div>
          <div class="stat-label">爽约总数</div>
        </div>
        <div class="stat-card warning">
          <div class="stat-value">{{ report.currentlyBanned }}</div>
          <div class="stat-label">当前被禁人数</div>
        </div>
      </div>

      <!-- TOP榜 -->
      <el-card shadow="never">
        <template #header><span style="font-weight:600">爽约 TOP 10</span></template>
        <el-table :data="report.topViolators || []" stripe>
          <el-table-column type="index" label="排名" width="70" />
          <el-table-column prop="studentNo" label="学号" width="130" />
          <el-table-column prop="studentName" label="姓名" width="100" />
          <el-table-column prop="violationCount" label="爽约次数" width="110">
            <template #default="{ row }">
              <el-tag :type="row.violationCount >= 5 ? 'danger' : 'warning'" effect="dark">
                {{ row.violationCount }} 次
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="禁约状态" width="130">
            <template #default="{ row }">
              <el-tag v-if="row.isCurrentlyBanned" type="danger" size="small" effect="plain">
                ⛔ 已禁约
              </el-tag>
              <el-tag v-else type="success" size="small" effect="plain">正常</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="bannedUntil" label="禁约截止" width="170">
            <template #default="{ row }">
              {{ row.bannedUntil ? new Date(row.bannedUntil).toLocaleString('zh-CN') : '—' }}
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!report.topViolators || report.topViolators.length === 0"
                  description="暂无爽约记录" style="padding:40px" />
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { adminApi } from '@/api/admin'
import NavBar from '@/components/common/NavBar.vue'

const dateRange = ref([])
const report = reactive({ totalViolations: 0, currentlyBanned: 0, topViolators: [] })

async function fetchReport() {
  const params = {}
  if (dateRange.value && dateRange.value.length === 2) {
    params.startDate = new Date(dateRange.value[0]).toISOString().slice(0, 10)
    params.endDate = new Date(dateRange.value[1]).toISOString().slice(0, 10)
  }
  const res = await adminApi.violationReport(params)
  Object.assign(report, res.data)
}

onMounted(fetchReport)
</script>

<style scoped>
.admin-layout { min-height: 100vh; background: var(--bg); }
</style>
