<script setup lang="ts">
import { RouterView } from 'vue-router'
</script>

<template>
  <el-config-provider namespace="el">
    <div class="app-bg">
      <RouterView />
    </div>
  </el-config-provider>
  
</template>

<style scoped>
.app-bg {
  min-height: 100vh;
  background: #ffffff;
}
</style>
