/**
 * 认证状态管理 — Pinia Store。
 * 管理 Token、用户信息、角色，并持久化到 localStorage。
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth'
import { ElMessage } from 'element-plus'
import router from '@/router'

export const useAuthStore = defineStore('auth', () => {
  // ---- 状态 ----
  const accessToken = ref(localStorage.getItem('accessToken') || '')
  const refreshToken = ref(localStorage.getItem('refreshToken') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))
  const userRole = ref(localStorage.getItem('userRole') || '')

  // ---- 计算属性 ----
  const isLoggedIn = computed(() => !!accessToken.value)
  const isStudent = computed(() => userRole.value === 'STUDENT')
  const isAdmin = computed(() => userRole.value === 'ADMIN')
  const userName = computed(() => userInfo.value?.name || userInfo.value?.username || '')

  // ---- 学生登录 ----
  async function studentLogin(credentials) {
    const res = await authApi.studentLogin(credentials)
    const { accessToken: at, refreshToken: rt, userInfo: info } = res.data
    saveAuthData(at, rt, info, 'STUDENT')
    ElMessage.success(`欢迎回来，${info.name}`)
    router.push('/student/rooms')
  }

  // ---- 管理员登录 ----
  async function adminLogin(credentials) {
    const res = await authApi.adminLogin(credentials)
    const { accessToken: at, refreshToken: rt, userInfo: info } = res.data
    saveAuthData(at, rt, { name: info.name, username: info.username, adminId: info.adminId }, 'ADMIN')
    ElMessage.success(`欢迎，${info.name}`)
    router.push('/admin/reservations')
  }

  // ---- 注册 ----
  async function register(data) {
    await authApi.register(data)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  }

  // ---- 登出 ----
  async function logout() {
    try {
      await authApi.logout()
    } catch (e) {
      // 忽略登出接口报错
    }
    clearAuthData()
    router.push('/login')
    ElMessage.info('已退出登录')
  }

  // ---- 内部方法 ----
  function saveAuthData(at, rt, info, role) {
    accessToken.value = at
    refreshToken.value = rt
    userInfo.value = info
    userRole.value = role
    localStorage.setItem('accessToken', at)
    localStorage.setItem('refreshToken', rt)
    localStorage.setItem('userInfo', JSON.stringify(info))
    localStorage.setItem('userRole', role)
  }

  function clearAuthData() {
    accessToken.value = ''
    refreshToken.value = ''
    userInfo.value = null
    userRole.value = ''
    localStorage.clear()
  }

  return {
    accessToken, refreshToken, userInfo, userRole,
    isLoggedIn, isStudent, isAdmin, userName,
    studentLogin, adminLogin, register, logout
  }
})
