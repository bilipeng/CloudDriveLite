<template>
    <FloatNav :folder-chain="fileSystemStore.folderChain" />
    <main style="margin-top: 64px">
      <router-view :key="$route.fullPath" />
    </main>
  </template>
  
  <script setup lang="ts">
  import FloatNav from '@/components/FloatNav.vue'
  import { useFileSystemStore } from '@/stores/fileSystem'
  
  const fileSystemStore = useFileSystemStore()
  </script>