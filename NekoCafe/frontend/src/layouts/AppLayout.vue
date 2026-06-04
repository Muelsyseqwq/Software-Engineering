<template>
  <el-container class="app-layout">
    <el-aside width="220px" class="aside">
      <h2>NekoCafé</h2>
      <el-menu router :default-active="route.path">
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          {{ item.label }}
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-main>
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { filterMenusByRoles } from '@/router/permissions'

const route = useRoute()
const auth = useAuthStore()
const menus = computed(() => filterMenusByRoles(auth.roles))
</script>

<style scoped>
.app-layout {
  min-height: 100vh;
}
.aside {
  background: #fff;
  border-right: 1px solid #f1dfce;
  padding: 16px;
}
</style>
