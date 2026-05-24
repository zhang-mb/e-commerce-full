<template>
  <div class="profile-page">
    <!-- 用户信息卡片 -->
    <el-card class="user-card">
      <div class="user-info">
        <div class="avatar">👤</div>
        <div class="user-detail">
          <h2>{{ userInfo.username }}</h2>
          <p class="user-id">用户ID：{{ userInfo.id }}</p>
        </div>
        <div class="user-stats">
          <div class="stat-item">
            <span class="stat-value">{{ userInfo.integration || 0 }}</span>
            <span class="stat-label">积分</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ userInfo.growth || 0 }}</span>
            <span class="stat-label">成长值</span>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 功能菜单 -->
    <el-card class="menu-card">
      <div class="menu-grid">
        <div class="menu-item" @click="goTo('/order')">
          <span class="menu-icon">📋</span>
          <span class="menu-text">我的订单</span>
        </div>
        <div class="menu-item" @click="goToAddress">
          <span class="menu-icon">📍</span>
          <span class="menu-text">收货地址</span>
        </div>
        <div class="menu-item" @click="goToCoupons">
          <span class="menu-icon">🎫</span>
          <span class="menu-text">优惠券</span>
        </div>
        <div class="menu-item" @click="goToSeckillRecord">
          <span class="menu-icon">⚡</span>
          <span class="menu-text">秒杀记录</span>
        </div>
      </div>
    </el-card>

    <!-- 订单统计 -->
    <el-card class="order-stats-card">
      <h3 class="card-title">我的订单</h3>
      <div class="order-stats">
        <div
            v-for="stat in orderStats"
            :key="stat.status"
            class="order-stat-item"
            @click="goToOrder(stat.status)"
        >
          <span class="order-stat-icon">{{ stat.icon }}</span>
          <span class="order-stat-count">{{ stat.count }}</span>
          <span class="order-stat-label">{{ stat.label }}</span>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

const router = useRouter()
const userInfo = ref({})
const orderStats = ref([
  { status: 0, label: '待付款', icon: '⏳', count: 0 },
  { status: 1, label: '待发货', icon: '📦', count: 0 },
  { status: 2, label: '已发货', icon: '🚚', count: 0 },
  { status: 3, label: '已完成', icon: '✅', count: 0 }
])

onMounted(async () => {
  try {
    const res = await request.get('/sso/info')
    userInfo.value = res.data || {}
  } catch (e) {
    console.error('获取用户信息失败:', e)
  }
  loadOrderStats()
})

const loadOrderStats = async () => {
  try {
    const res = await request.get('/order/list', { params: { pageNum: 1, pageSize: 100 } })
    const orders = res.data?.list || []
    orderStats.value.forEach(stat => {
      stat.count = orders.filter(o => o.status === stat.status).length
    })
  } catch (e) {
    console.error('加载订单统计失败:', e)
  }
}

const goTo = (path) => {
  router.push(path)
}

const goToAddress = () => {
  router.push('/address')
}

const goToCoupons = () => {
  router.push('/coupons')
}

const goToSeckillRecord = () => {
  router.push('/seckill-record')
}

const goToOrder = (status) => {
  router.push(`/order?status=${status}`)
}
</script>

<style scoped>
.profile-page {
  padding: 20px;
  background: #f5f5f5;
  min-height: calc(100vh - 80px);
}

.user-card {
  border-radius: 12px;
  margin-bottom: 20px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 20px;
}

.avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, #409eff 0%, #67c23a 100%);
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 40px;
}

.user-detail {
  flex: 1;
}

.user-detail h2 {
  font-size: 20px;
  font-weight: bold;
  color: #303133;
  margin: 0 0 5px 0;
}

.user-id {
  font-size: 14px;
  color: #999;
  margin: 0;
}

.user-stats {
  display: flex;
  gap: 30px;
}

.stat-item {
  text-align: center;
}

.stat-value {
  display: block;
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
}

.stat-label {
  font-size: 12px;
  color: #999;
}

.menu-card {
  border-radius: 12px;
  margin-bottom: 20px;
}

.menu-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  padding: 20px;
}

.menu-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 20px 10px;
  background: #fafafa;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.menu-item:hover {
  background: #e8f4ff;
  transform: translateY(-2px);
}

.menu-icon {
  font-size: 32px;
}

.menu-text {
  font-size: 14px;
  color: #303133;
}

.order-stats-card {
  border-radius: 12px;
}

.card-title {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
  margin: 0 0 20px 0;
  padding-bottom: 15px;
  border-bottom: 1px solid #f0f0f0;
}

.order-stats {
  display: flex;
  justify-content: space-around;
}

.order-stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 15px;
  border-radius: 12px;
  transition: background 0.3s ease;
}

.order-stat-item:hover {
  background: #fafafa;
}

.order-stat-icon {
  font-size: 28px;
}

.order-stat-count {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}

.order-stat-label {
  font-size: 12px;
  color: #999;
}

@media (max-width: 768px) {
  .menu-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .user-info {
    flex-direction: column;
    text-align: center;
  }

  .user-stats {
    margin-top: 15px;
  }
}
</style>