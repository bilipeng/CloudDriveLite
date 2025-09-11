<template>
  <el-button :icon="ArrowLeft" @click="$emit('back')" :disabled="disabled">
    返回上一级
  </el-button>
</template>

<script setup lang="ts">
import { ArrowLeft } from '@element-plus/icons-vue'

defineProps<{ disabled?: boolean }>()
defineEmits<{ (e: 'back'): void }>()
</script>

<style scoped>
</style>


