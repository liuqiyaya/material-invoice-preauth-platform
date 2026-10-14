<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => {
  if (route.path.startsWith('/invoice')) return '/invoice/list'
  return route.path
})

function handleLogout(): void {
  userStore.logout()
  void router.push('/login')
}
</script>

<template>
  <el-container class="layout">
    <el-header class="layout-header">
      <div class="brand">材料费发票智能预审与管理平台</div>
      <el-button type="primary" link @click="handleLogout">退出登录</el-button>
    </el-header>
    <el-container>
      <el-aside width="220px" class="layout-aside">
        <el-menu :default-active="activeMenu" router>
          <el-menu-item index="/invoice/list">发票列表</el-menu-item>
          <el-menu-item index="/category-rule/list">品类规则</el-menu-item>
        </el-menu>
      </el-aside>
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout {
  min-height: 100vh;
}

.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #1f2d3d;
  color: #fff;
}

.brand {
  font-size: 16px;
  font-weight: 600;
}

.layout-aside {
  border-right: 1px solid var(--el-border-color);
}

.layout-main {
  background: #f5f7fa;
}
</style>