<!--
  通用导航栏组件 — 玻璃态高级设计
  学生端：自习室列表 | 我的预约 | 个人中心 | 退出
  管理员端：预约管理 | 座位管理 | 统计报表 | 系统配置 | 退出
-->
<template>
  <div class="navbar-wrapper">
    <header class="navbar">
      <!-- 顶部渐变线 -->
      <div class="navbar-accent"></div>

      <div class="navbar-inner">
        <!-- Logo -->
        <div class="navbar-brand" @click="goHome">
          <div class="brand-icon">
            <el-icon :size="22" color="#fff"><School /></el-icon>
          </div>
          <span class="brand-text">自习室预约</span>
        </div>

        <!-- 学生菜单 -->
        <nav v-if="authStore.isStudent" class="navbar-nav">
          <router-link to="/student/rooms" class="nav-item" :class="{ active: isActive('/student/rooms') }">
            <el-icon><Grid /></el-icon>
            <span>自习室列表</span>
          </router-link>
          <router-link to="/student/reservations" class="nav-item" :class="{ active: isActive('/student/reservations') }">
            <el-icon><Calendar /></el-icon>
            <span>我的预约</span>
          </router-link>
          <router-link to="/student/profile" class="nav-item" :class="{ active: isActive('/student/profile') }">
            <el-icon><User /></el-icon>
            <span>个人中心</span>
          </router-link>
        </nav>

        <!-- 管理员菜单 -->
        <nav v-if="authStore.isAdmin" class="navbar-nav">
          <router-link to="/admin/reservations" class="nav-item" :class="{ active: isActive('/admin/reservations') }">
            <el-icon><List /></el-icon>
            <span>预约管理</span>
          </router-link>
          <router-link to="/admin/seats" class="nav-item" :class="{ active: isActive('/admin/seats') }">
            <el-icon><OfficeBuilding /></el-icon>
            <span>座位管理</span>
          </router-link>
          <router-link to="/admin/reports" class="nav-item" :class="{ active: isActive('/admin/reports') }">
            <el-icon><DataAnalysis /></el-icon>
            <span>统计报表</span>
          </router-link>
          <router-link to="/admin/config" class="nav-item" :class="{ active: isActive('/admin/config') }">
            <el-icon><Setting /></el-icon>
            <span>系统配置</span>
          </router-link>
        </nav>

        <!-- 右侧用户区 -->
        <div class="navbar-right">
          <div class="user-info">
            <div class="user-avatar">
              <el-icon :size="16"><UserFilled /></el-icon>
            </div>
            <span class="user-name">{{ authStore.userName }}</span>
            <span v-if="authStore.isStudent" class="role-tag student-tag">学生</span>
            <span v-if="authStore.isAdmin" class="role-tag admin-tag">管理员</span>
          </div>
          <button class="logout-btn" @click="handleLogout" title="退出登录">
            <el-icon :size="18"><SwitchButton /></el-icon>
          </button>
        </div>
      </div>
    </header>
    <!-- 占位元素，防止内容被固定导航遮挡 -->
    <div class="navbar-placeholder"></div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessageBox } from 'element-plus'
import {
  School, Grid, Calendar, User, List, OfficeBuilding,
  DataAnalysis, Setting, SwitchButton, UserFilled
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

function isActive(base) {
  return route.path.startsWith(base)
}

function goHome() {
  if (authStore.isAdmin) router.push('/admin/reservations')
  else router.push('/student/rooms')
}

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '退出确认', {
      confirmButtonText: '退出', cancelButtonText: '取消', type: 'warning'
    })
    authStore.logout()
  } catch { /* 取消 */ }
}
</script>

<style scoped>
/* ===== 导航包装器 ===== */
.navbar-wrapper {
  position: sticky;
  top: 5px;
  z-index: 100;
  padding: 5px 24px 0;
}

/* ===== 导航栏主体 ===== */
.navbar {
  position: relative;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border: 1px solid rgba(226, 232, 240, 0.8);
  border-radius: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04), 0 4px 12px rgba(0, 0, 0, 0.03);
  overflow: hidden;
}

/* 顶部渐变色条 */
.navbar-accent {
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 2.5px;
  background: linear-gradient(90deg, #2563eb, #3b82f6, #60a5fa, #818cf8, #60a5fa, #3b82f6, #2563eb);
  background-size: 200% 100%;
  animation: accentShift 8s ease-in-out infinite;
}

@keyframes accentShift {
  0%, 100% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
}

.navbar-inner {
  max-width: 1280px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  height: 58px;
  padding: 0 20px;
  gap: 0;
}

/* 占位 — 抵消 wrapper 的 sticky 空间 */
.navbar-placeholder { height: 5px; }

/* ===== Logo 品牌区 ===== */
.navbar-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  flex-shrink: 0;
  margin-right: 36px;
  user-select: none;
}
.brand-icon {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
  transition: all 0.3s ease;
}
.navbar-brand:hover .brand-icon {
  transform: scale(1.05);
  box-shadow: 0 6px 16px rgba(37, 99, 235, 0.4);
}
.brand-text {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
  white-space: nowrap;
  letter-spacing: -0.3px;
}

/* ===== 导航菜单 ===== */
.navbar-nav {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1;
  height: 100%;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 6px;
  height: 100%;
  padding: 0 16px;
  font-size: 14px;
  font-weight: 500;
  color: #475569;
  text-decoration: none;
  position: relative;
  transition: all 0.2s ease;
  border-radius: 0;
  white-space: nowrap;
}

.nav-item::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 0;
  height: 2.5px;
  background: #2563eb;
  border-radius: 2px 2px 0 0;
  transition: width 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.nav-item:hover {
  color: #1e293b;
  background: rgba(37, 99, 235, 0.04);
}

.nav-item.active {
  color: #2563eb;
  font-weight: 600;
}

.nav-item.active::after {
  width: 100%;
}

/* ===== 右侧用户区 ===== */
.navbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  margin-left: 24px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px 6px 6px;
  background: #f8fafc;
  border-radius: 30px;
  border: 1px solid #e2e8f0;
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.user-name {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
  max-width: 90px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.role-tag {
  font-size: 10px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 10px;
  letter-spacing: 0.5px;
  flex-shrink: 0;
}
.student-tag {
  color: #2563eb;
  background: #eff6ff;
}
.admin-tag {
  color: #d97706;
  background: #fffbeb;
}

.logout-btn {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  background: #fff;
  color: #94a3b8;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  flex-shrink: 0;
}
.logout-btn:hover {
  color: #ef4444;
  border-color: #fecaca;
  background: #fef2f2;
}
</style>
