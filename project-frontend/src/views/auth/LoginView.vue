<!-- 学生登录页 — 高级玻璃态 + 动态背景 -->
<template>
  <div class="auth-page">
    <!-- 动态浮动背景 -->
    <div class="bg-shapes">
      <div class="shape shape-1"></div>
      <div class="shape shape-2"></div>
      <div class="shape shape-3"></div>
      <div class="shape shape-4"></div>
      <div class="shape shape-5"></div>
      <div class="shape shape-6"></div>
    </div>

    <!-- 左侧品牌区 -->
    <div class="auth-brand">
      <div class="brand-glow"></div>
      <div class="brand-icon-wrap">
        <el-icon :size="48" color="#fff"><School /></el-icon>
      </div>
      <h1 class="brand-title">自习室预约系统</h1>
      <p class="brand-desc">高效便捷的校园自习空间管理平台</p>
      <div class="brand-features">
        <div class="feature-item">
          <span class="feature-dot"></span> 在线预约座位
        </div>
        <div class="feature-item">
          <span class="feature-dot"></span> 实时查看可用
        </div>
        <div class="feature-item">
          <span class="feature-dot"></span> 一键签到签退
        </div>
      </div>
    </div>

    <!-- 右侧登录卡片 -->
    <div class="auth-card-wrap">
      <div class="auth-card">
        <div class="card-badge">学生端</div>
        <div class="auth-header">
          <h2>欢迎回来</h2>
          <p>登录您的学生账号</p>
        </div>
        <el-form ref="formRef" :model="form" :rules="rules" size="large" @submit.prevent="handleLogin">
          <el-form-item prop="studentNo">
            <el-input
              v-model="form.studentNo"
              placeholder="请输入学号"
              :prefix-icon="User"
              class="auth-input"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              :prefix-icon="Lock"
              show-password
              class="auth-input"
              @keyup.enter="handleLogin"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" native-type="submit" :loading="loading" class="submit-btn">
              <span v-if="!loading">登 录</span>
            </el-button>
          </el-form-item>
        </el-form>
        <div class="auth-footer">
          <span>还没有账号？</span>
          <router-link to="/register" class="footer-link">立即注册 →</router-link>
        </div>
        <div class="auth-divider"><span>或</span></div>
        <div class="auth-alt">
          <router-link to="/admin/login" class="admin-link">
            <el-icon><Avatar /></el-icon>
            管理员登录
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { User, Lock, Avatar } from '@element-plus/icons-vue'

const authStore = useAuthStore()
const loading = ref(false)
const formRef = ref(null)
const form = reactive({ studentNo: '', password: '' })
const rules = {
  studentNo: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try { await authStore.studentLogin(form) } catch { /* ElMessage已处理 */ }
  finally { loading.value = false }
}
</script>

<style scoped>
/* ===== 页面容器 ===== */
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
  background: linear-gradient(160deg, #0f172a 0%, #1e3a5f 25%, #1a3d7c 50%, #1d4ed8 75%, #2563eb 100%);
  padding: 40px;
  position: relative;
  overflow: hidden;
}

/* ===== 动态浮动背景 ===== */
.bg-shapes { position: absolute; inset: 0; pointer-events: none; z-index: 0; }
.shape {
  position: absolute;
  border-radius: 50%;
  opacity: 0.08;
  background: #fff;
  animation: floatShape 20s infinite ease-in-out;
}
.shape-1 { width: 400px; height: 400px; top: -10%; left: -5%; animation-delay: 0s; }
.shape-2 { width: 300px; height: 300px; top: 60%; left: 10%; animation-delay: -3s; }
.shape-3 { width: 250px; height: 250px; top: 20%; right: -5%; animation-delay: -6s; }
.shape-4 { width: 350px; height: 350px; bottom: -10%; right: 15%; animation-delay: -9s; }
.shape-5 { width: 180px; height: 180px; top: 40%; left: 40%; animation-delay: -12s; }
.shape-6 { width: 220px; height: 220px; bottom: 20%; left: 30%; animation-delay: -15s; }

@keyframes floatShape {
  0%, 100% { transform: translate(0, 0) scale(1); }
  25% { transform: translate(40px, -30px) scale(1.08); }
  50% { transform: translate(-20px, 20px) scale(0.94); }
  75% { transform: translate(-35px, -15px) scale(1.04); }
}

/* ===== 左侧品牌区 ===== */
.auth-brand {
  position: relative;
  z-index: 1;
  flex: 0 0 380px;
  padding: 60px 40px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
}
.brand-glow {
  position: absolute;
  top: 30%;
  left: 20%;
  width: 200px;
  height: 200px;
  background: radial-gradient(circle, rgba(59,130,246,0.4) 0%, transparent 70%);
  border-radius: 50%;
  filter: blur(40px);
  animation: pulseGlow 4s ease-in-out infinite;
}
@keyframes pulseGlow {
  0%, 100% { opacity: 0.6; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.2); }
}
.brand-icon-wrap {
  width: 80px;
  height: 80px;
  border-radius: 22px;
  background: linear-gradient(135deg, rgba(255,255,255,0.2), rgba(255,255,255,0.05));
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255,255,255,0.18);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 28px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.2);
}
.brand-title {
  font-size: 30px;
  font-weight: 800;
  color: #fff;
  letter-spacing: -0.5px;
  margin-bottom: 10px;
  line-height: 1.3;
}
.brand-desc {
  font-size: 15px;
  color: rgba(255,255,255,0.7);
  line-height: 1.6;
  margin-bottom: 32px;
}
.brand-features { display: flex; flex-direction: column; gap: 14px; }
.feature-item {
  font-size: 14px;
  color: rgba(255,255,255,0.8);
  display: flex;
  align-items: center;
  gap: 10px;
}
.feature-dot {
  width: 8px; height: 8px;
  border-radius: 50%;
  background: #60a5fa;
  box-shadow: 0 0 10px rgba(96,165,250,0.6);
}

/* ===== 右侧卡片 ===== */
.auth-card-wrap {
  position: relative;
  z-index: 1;
}
.auth-card {
  width: 440px;
  background: rgba(255,255,255,0.95);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  padding: 48px 44px;
  box-shadow:
    0 25px 60px rgba(0,0,0,0.25),
    0 0 0 1px rgba(255,255,255,0.1) inset;
  position: relative;
  overflow: hidden;
}
.auth-card::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 4px;
  background: linear-gradient(90deg, #2563eb, #3b82f6, #60a5fa, #3b82f6, #2563eb);
}
.card-badge {
  position: absolute;
  top: 20px; right: 20px;
  font-size: 11px;
  font-weight: 600;
  color: #2563eb;
  background: #eff6ff;
  padding: 4px 12px;
  border-radius: 20px;
  letter-spacing: 1px;
}
.auth-header { margin-bottom: 36px; }
.auth-header h2 {
  font-size: 26px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 6px;
  letter-spacing: -0.3px;
}
.auth-header p {
  font-size: 14px;
  color: #64748b;
}

.auth-input :deep(.el-input__wrapper) {
  border-radius: 12px !important;
  padding: 4px 16px;
  background: #f8fafc;
  border: 1.5px solid #e2e8f0;
  transition: all 0.25s ease;
  box-shadow: none !important;
}
.auth-input :deep(.el-input__wrapper:hover) {
  border-color: #93c5fd;
  background: #fff;
}
.auth-input :deep(.el-input__wrapper.is-focus) {
  border-color: #2563eb;
  box-shadow: 0 0 0 4px rgba(37,99,235,0.08) !important;
  background: #fff;
}
.auth-input :deep(.el-input__inner) { font-size: 15px; }

.submit-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 3px;
  border-radius: 12px;
  margin-top: 8px;
  background: linear-gradient(135deg, #2563eb, #1d4ed8) !important;
  border: none !important;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}
.submit-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 25px rgba(37,99,235,0.4) !important;
}
.submit-btn:active { transform: translateY(0); }

.auth-footer {
  text-align: center;
  font-size: 14px;
  color: #64748b;
}
.footer-link { font-weight: 600; }

.auth-divider {
  display: flex; align-items: center;
  margin: 20px 0;
  color: #94a3b8;
  font-size: 13px;
}
.auth-divider::before, .auth-divider::after {
  content: ''; flex: 1; height: 1px;
  background: #e2e8f0;
}
.auth-divider span { padding: 0 16px; }

.auth-alt { text-align: center; }
.admin-link {
  display: inline-flex; align-items: center; gap: 6px;
  font-size: 14px; color: #64748b;
  padding: 8px 20px; border-radius: 8px;
  transition: all 0.2s;
  border: 1px solid #e2e8f0;
}
.admin-link:hover {
  color: #f59e0b;
  border-color: #fde68a;
  background: #fffbeb;
}

@media (max-width: 860px) {
  .auth-page { flex-direction: column; padding: 20px; }
  .auth-brand { flex: none; padding: 30px 20px; align-items: center; text-align: center; }
  .auth-card { width: 100%; max-width: 440px; padding: 36px 28px; }
}
</style>
