/**
 * 路由配置 — 含全局导航守卫（认证 + 角色鉴权）。
 *
 * 路由 meta 说明：
 *   guest: true   → 仅未登录可访问（登录/注册页）
 *   auth: true    → 需要登录
 *   role: '...'   → 需要指定角色
 */
import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/student/rooms' },
  // ---- 认证页（仅未登录可访问） ----
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { guest: true, title: '学生登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/RegisterView.vue'),
    meta: { guest: true, title: '学生注册' }
  },
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: () => import('@/views/auth/AdminLoginView.vue'),
    meta: { guest: true, title: '管理员登录' }
  },
  // ---- 学生端 ----
  {
    path: '/student/rooms',
    name: 'Rooms',
    component: () => import('@/views/student/RoomListView.vue'),
    meta: { auth: true, role: 'STUDENT', title: '自习室列表' }
  },
  {
    path: '/student/rooms/:roomId',
    name: 'RoomDetail',
    component: () => import('@/views/student/RoomDetailView.vue'),
    meta: { auth: true, role: 'STUDENT', title: '选择座位' }
  },
  {
    path: '/student/reservations',
    name: 'MyReservations',
    component: () => import('@/views/student/MyReservationView.vue'),
    meta: { auth: true, role: 'STUDENT', title: '我的预约' }
  },
  {
    path: '/student/reservations/:id',
    name: 'ReservationDetail',
    component: () => import('@/views/student/ReservationDetailView.vue'),
    meta: { auth: true, role: 'STUDENT', title: '预约详情' }
  },
  {
    path: '/student/profile',
    name: 'Profile',
    component: () => import('@/views/student/ProfileView.vue'),
    meta: { auth: true, role: 'STUDENT', title: '个人中心' }
  },
  // ---- 管理员端 ----
  {
    path: '/admin/reservations',
    name: 'AdminReservations',
    component: () => import('@/views/admin/ReservationManageView.vue'),
    meta: { auth: true, role: 'ADMIN', title: '预约管理' }
  },
  {
    path: '/admin/seats',
    name: 'AdminSeats',
    component: () => import('@/views/admin/SeatManageView.vue'),
    meta: { auth: true, role: 'ADMIN', title: '座位管理' }
  },
  {
    path: '/admin/reports',
    name: 'AdminReports',
    component: () => import('@/views/admin/ReportView.vue'),
    meta: { auth: true, role: 'ADMIN', title: '统计报表' }
  },
  {
    path: '/admin/config',
    name: 'AdminConfig',
    component: () => import('@/views/admin/ConfigView.vue'),
    meta: { auth: true, role: 'ADMIN', title: '系统配置' }
  },
  // ---- 404 ----
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFoundView.vue'),
    meta: { title: '404' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

// ---- 全局路由守卫 ----
router.beforeEach((to, from, next) => {
  document.title = (to.meta.title || '自习室预约') + ' — 校园自习室座位预约系统'

  const token = localStorage.getItem('accessToken')
  const role = localStorage.getItem('userRole')

  // 1. 仅游客页面（登录/注册）— 已登录则跳到对应首页
  if (to.meta.guest) {
    if (token) {
      return next(role === 'ADMIN' ? '/admin/reservations' : '/student/rooms')
    }
    return next()
  }

  // 2. 需要认证的页面 — 未登录跳转登录页
  if (to.meta.auth && !token) {
    return next('/login')
  }

  // 3. 角色校验 — 学生访问管理员页面 / 反之
  if (to.meta.role && role && to.meta.role !== role) {
    return next(role === 'ADMIN' ? '/admin/reservations' : '/student/rooms')
  }

  next()
})

export default router
