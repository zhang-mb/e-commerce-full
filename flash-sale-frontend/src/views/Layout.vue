<template>
  <div class="app-layout">
    <!-- 顶部导航 -->
    <header class="app-header">
      <div class="header-content">
        <!-- Logo -->
        <div class="logo" @click="goHome">
          <span class="logo-icon">⚡</span>
          <span class="logo-text">秒杀商城</span>
        </div>

        <!-- 导航菜单 -->
        <nav class="nav-menu">
          <router-link
              v-for="item in menuItems"
              :key="item.path"
              :to="item.path"
              :class="['nav-item', { active: currentPath === item.path }]"
          >
            {{ item.label }}
          </router-link>
        </nav>

        <!-- 用户操作 -->
        <div class="user-actions">
          <el-button link class="logout-btn" @click="confirmLogout">
            退出登录
          </el-button>
        </div>
      </div>
    </header>

    <!-- 主内容区 -->
    <main class="app-main">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const route = useRoute()

// 修改：添加「个人中心」菜单
const menuItems = [
  { path: '/home', label: '首页' },
  { path: '/seckill', label: '秒杀专区' },
  { path: '/order', label: '我的订单' },
  { path: '/profile', label: '个人中心' }  // 新增
]

const currentPath = computed(() => route.path)

const goHome = () => {
  router.push('/home')
}

const confirmLogout = async () => {
  try {
    await ElMessageBox.confirm(
        '确定要退出登录吗？',
        '退出确认',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
    )
    localStorage.removeItem('token')
    ElMessage.success('退出成功')
    router.push('/login')
  } catch {
    ElMessage.info('已取消退出')
  }
}
</script>

<style scoped>
.app-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-header {
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a24 100%);
  padding: 0 20px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 60px;
}

.logo {
  display: flex;
  align-items: center;
  cursor: pointer;
  color: white;
}

.logo-icon {
  font-size: 24px;
  margin-right: 8px;
}

.logo-text {
  font-size: 20px;
  font-weight: bold;
}

.nav-menu {
  display: flex;
  gap: 30px;
}

.nav-item {
  color: white;
  text-decoration: none;
  padding: 8px 16px;
  border-radius: 20px;
  transition: all 0.3s ease;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.2);
}

.nav-item.active {
  background: white;
  color: #ff6b6b;
  font-weight: bold;
}

.user-actions {
  display: flex;
  align-items: center;
}

.logout-btn {
  color: white !important;
  border-color: white !important;
}

.app-main {
  flex: 1;
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  padding: 20px;
}
</style>