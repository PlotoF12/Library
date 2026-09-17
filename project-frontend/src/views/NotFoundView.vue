<!-- 404 页面 -->
<template>
  <div class="not-found-page">
    <el-result icon="warning" title="404" sub-title="页面未找到">
      <template #extra>
        <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup>
</script>

<style scoped>
.not-found-page {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: var(--bg);
}
</style>
