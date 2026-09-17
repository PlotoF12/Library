<!--
  座位网格组件 — 以网格形式展示座位。
  不同状态不同颜色：
    - 可用（蓝绿色边框）→ 可点击预约
    - 已占用/已预约（红色）→ 不可点击
    - 维修/禁用（灰色）→ 不可点击
  每个座位卡片显示偏好图标：⚡电源 🪟靠窗 👤单座
-->
<template>
  <div class="seat-grid">
    <div v-if="!seats || seats.length === 0" class="empty-state">
      <el-empty description="暂无可选座位" />
    </div>
    <div
      v-for="seat in seats"
      :key="seat.seatId"
      :class="['seat-card', { available: seat.available, occupied: !seat.available }]"
      @click="seat.available && $emit('select', seat)"
    >
      <!-- 座位编号 -->
      <div class="seat-code">{{ seat.seatCode }}</div>
      <!-- 偏好标签 -->
      <div class="seat-tags">
        <span v-if="seat.hasPower" class="tag power" title="有电源">⚡</span>
        <span v-if="seat.isWindow" class="tag window" title="靠窗">🪟</span>
        <span v-if="seat.isSingle" class="tag single" title="独立单座">👤</span>
      </div>
      <!-- 状态 -->
      <div :class="['seat-status', seat.available ? 'text-success' : 'text-danger']">
        {{ seat.available ? '可预约' : '已占用' }}
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  seats: { type: Array, default: () => [] }
})
defineEmits(['select'])
</script>

<style scoped>
.seat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 12px;
}
.seat-card {
  background: #fff;
  border: 2px solid var(--border);
  border-radius: var(--radius-sm);
  padding: 16px 12px;
  text-align: center;
  cursor: pointer;
  transition: all var(--transition);
  user-select: none;
}
.seat-card.available:hover {
  border-color: var(--primary);
  background: var(--primary-bg);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.15);
}
.seat-card.occupied {
  background: #fef2f2;
  border-color: #fecaca;
  cursor: not-allowed;
  opacity: 0.7;
}
.seat-code {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 6px;
}
.seat-tags {
  display: flex;
  justify-content: center;
  gap: 4px;
  margin-bottom: 6px;
}
.tag {
  font-size: 14px;
  line-height: 1;
}
.seat-status {
  font-size: 12px;
  font-weight: 600;
}
.text-success { color: var(--primary); }
.text-danger { color: var(--danger); }
.empty-state {
  grid-column: 1 / -1;
  padding: 40px;
}
</style>
