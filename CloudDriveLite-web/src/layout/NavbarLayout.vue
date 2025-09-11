<template>
    <FloatNav :folder-chain="fileSystemStore.folderChain.value" />
    <main style="margin-top: 64px">
      <router-view />
    </main>
  </template>
  
  <script setup lang="ts">
  import FloatNav from '@/components/FloatNav.vue'
  import { useFileSystemStore } from '@/stores/fileSystem'
  
  const fileSystemStore = useFileSystemStore()
  </script>