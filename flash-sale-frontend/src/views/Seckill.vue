<template>
  <div class="seckill-page">
    <!-- 秒杀活动信息 -->
    <div class="promotion-header" v-if="promotion">
      <h2>⚡ {{ promotion.title }}</h2>
      <p class="promotion-time">活动时间：{{ formatDate(promotion.startDate) }} - {{ formatDate(promotion.endDate) }}</p>
    </div>

    <!-- 场次标签页 -->
    <div class="session-tabs">
      <el-tabs v-model="activeTab" @tab-click="handleTabClick">
        <!-- 即将开始 -->
        <el-tab-pane label="即将开始" name="upcoming">
          <div v-if="upcomingSessions.length === 0" class="empty-state">
            <div class="empty-icon">⏰</div>
            <p>暂无即将开始的秒杀场次</p>
          </div>
          <div v-else>
            <div v-for="session in upcomingSessions" :key="session.id" class="session-card">
              <div class="session-header">
                <span class="session-time">{{ formatTime(session.startTime) }}</span>
                <span class="session-status upcoming">即将开始</span>
              </div>
              <div class="countdown" v-if="session.id === countdownSessionId">
                距离开始还有：{{ countdownText }}
              </div>
              <div class="session-products">
                <div v-for="product in getProductsBySession(session.id)" :key="product.id" class="product-mini">
                  <img :src="product.pic" :alt="product.name" class="mini-img" />
                  <div class="mini-info">
                    <div class="mini-name">{{ product.name }}</div>
                    <div class="mini-price">
                      <span class="flash">¥{{ product.flashPromotionPrice }}</span>
                      <span class="origin">¥{{ product.price }}</span>
                    </div>
                  </div>
                  <el-button type="primary" disabled>敬请期待</el-button>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>
        
        <!-- 正在进行 -->
        <el-tab-pane label="正在进行" name="current">
          <div v-if="currentSessions.length === 0" class="empty-state">
            <div class="empty-icon">🛒</div>
            <p>暂无正在进行的秒杀</p>
          </div>
          <div v-else>
            <div v-for="session in currentSessions" :key="session.id" class="session-card active">
              <div class="session-header">
                <span class="session-time">{{ formatTime(session.startTime) }} - {{ formatTime(session.endTime) }}</span>
                <span class="session-status current">进行中</span>
              </div>
              <div class="session-products">
                <div v-for="product in getProductsBySession(session.id)" :key="product.id" class="product-mini">
                  <img :src="product.pic" :alt="product.name" class="mini-img" />
                  <div class="mini-info">
                    <div class="mini-name">{{ product.name }}</div>
                    <div class="mini-price">
                      <span class="flash">¥{{ product.flashPromotionPrice }}</span>
                      <span class="origin">¥{{ product.price }}</span>
                    </div>
                    <div class="mini-stock">剩余{{ product.flashPromotionCount }}件</div>
                  </div>
                  <el-button
                      type="danger"
                      :disabled="(product.flashPromotionCount || 0) <= 0"
                      @click="handleSeckill(product)"
                  >
                    {{ (product.flashPromotionCount || 0) <= 0 ? '已抢光' : '立即秒杀' }}
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>
        
        <!-- 已结束 -->
        <el-tab-pane label="已结束" name="ended">
          <div v-if="endedSessions.length === 0" class="empty-state">
            <div class="empty-icon">✅</div>
            <p>暂无已结束的秒杀场次</p>
          </div>
          <div v-else>
            <div v-for="session in endedSessions" :key="session.id" class="session-card ended">
              <div class="session-header">
                <span class="session-time">{{ formatTime(session.startTime) }} - {{ formatTime(session.endTime) }}</span>
                <span class="session-status ended">已结束</span>
              </div>
              <div class="session-products">
                <div v-for="product in getProductsBySession(session.id)" :key="product.id" class="product-mini">
                  <img :src="product.pic" :alt="product.name" class="mini-img" />
                  <div class="mini-info">
                    <div class="mini-name">{{ product.name }}</div>
                    <div class="mini-price">
                      <span class="flash">¥{{ product.flashPromotionPrice }}</span>
                      <span class="origin">¥{{ product.price }}</span>
                    </div>
                  </div>
                  <el-button type="default" disabled>已结束</el-button>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const promotion = ref(null)
const sessions = ref([])
const productsMap = ref({})
const activeTab = ref('current')
const countdownSessionId = ref(null)
const countdownText = ref('')
let countdownTimer = null

// 将时间字符串转换为当天的秒数（0-86400）
const timeToSeconds = (timeStr) => {
  if (!timeStr) return 0
  const parts = timeStr.split(':')
  return parseInt(parts[0]) * 3600 + parseInt(parts[1]) * 60 + parseInt(parts[2])
}

// 获取当前时间的秒数
const getCurrentSeconds = () => {
  const now = new Date()
  return now.getHours() * 3600 + now.getMinutes() * 60 + now.getSeconds()
}

// 计算属性：按状态分组场次
const upcomingSessions = computed(() => {
  const currentSeconds = getCurrentSeconds()
  return sessions.value.filter(s => timeToSeconds(s.startTime) > currentSeconds)
})

const currentSessions = computed(() => {
  const currentSeconds = getCurrentSeconds()
  return sessions.value.filter(s => {
    const startSeconds = timeToSeconds(s.startTime)
    const endSeconds = timeToSeconds(s.endTime)
    return startSeconds <= currentSeconds && currentSeconds <= endSeconds
  })
})

const endedSessions = computed(() => {
  const currentSeconds = getCurrentSeconds()
  return sessions.value.filter(s => timeToSeconds(s.endTime) < currentSeconds)
})

// 获取指定场次的商品
const getProductsBySession = (sessionId) => {
  return productsMap.value[sessionId] || []
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

// 格式化时间
const formatTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

// 更新倒计时
const updateCountdown = () => {
  if (!countdownSessionId.value && upcomingSessions.value.length > 0) {
    countdownSessionId.value = upcomingSessions.value[0].id
  }
  
  const session = sessions.value.find(s => s.id === countdownSessionId.value)
  if (!session) {
    countdownText.value = ''
    return
  }
  
  const now = Date.now()
  const startTime = new Date(session.startTime).getTime()
  const diff = startTime - now
  
  if (diff <= 0) {
    loadData()
    return
  }
  
  const hours = Math.floor(diff / (1000 * 60 * 60))
  const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))
  const seconds = Math.floor((diff % (1000 * 60)) / 1000)
  
  countdownText.value = `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
}

// 加载数据
const loadData = async () => {
  try {
    // 获取秒杀活动信息
    const promotionRes = await request.get('/seckill/promotion')
    promotion.value = promotionRes.data || null
    
    // 获取场次列表
    const sessionsRes = await request.get('/seckill/session/list')
    sessions.value = sessionsRes.data || []
    
    // 获取每个场次的商品
    for (const session of sessions.value) {
      const goodsRes = await request.get(`/seckill/goods/list?sessionId=${session.id}`)
      productsMap.value[session.id] = goodsRes.data || []
    }
    
    // 如果没有真实数据，使用模拟数据
    if (sessions.value.length === 0) {
      loadMockData()
    }
  } catch (error) {
    console.error('加载秒杀数据失败:', error)
    loadMockData()
  }
}

// 加载模拟数据
const loadMockData = () => {
  promotion.value = {
    title: '限时秒杀活动',
    startDate: new Date(),
    endDate: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000)
  }
  
  const now = Date.now()
  sessions.value = [
    {
      id: 1,
      startTime: new Date(now - 2 * 60 * 60 * 1000),
      endTime: new Date(now + 2 * 60 * 60 * 1000)
    },
    {
      id: 2,
      startTime: new Date(now + 3 * 60 * 60 * 1000),
      endTime: new Date(now + 7 * 60 * 60 * 1000)
    },
    {
      id: 3,
      startTime: new Date(now - 5 * 60 * 60 * 1000),
      endTime: new Date(now - 1 * 60 * 60 * 1000)
    }
  ]
  
  productsMap.value = {
    1: [
      { id: 1, name: 'Apple iPhone 15 Pro', pic: 'http://macro-oss.oss-cn-shenzhen.aliyuncs.com/mall/images/20221104/redmi_k50_01.jpg', price: 7999, flashPromotionPrice: 5999, flashPromotionCount: 10 },
      { id: 2, name: 'Sony WH-1000XM5 耳机', pic: 'http://macro-oss.oss-cn-shenzhen.aliyuncs.com/mall/images/20221104/huawei_mate50_01.jpg', price: 2999, flashPromotionPrice: 1999, flashPromotionCount: 5 },
      { id: 3, name: 'MacBook Air M3', pic: 'http://macro-oss.oss-cn-shenzhen.aliyuncs.com/mall/images/20221108/oppo_r8_01.jpg', price: 11999, flashPromotionPrice: 8999, flashPromotionCount: 0 }
    ],
    2: [
      { id: 4, name: 'Nike Air Max 运动鞋', pic: 'http://macro-oss.oss-cn-shenzhen.aliyuncs.com/mall/images/20180615/5b19403eN9f0b3cb8.jpg', price: 1299, flashPromotionPrice: 699, flashPromotionCount: 50 },
      { id: 5, name: 'iPad Pro 12.9', pic: 'http://macro-oss.oss-cn-shenzhen.aliyuncs.com/mall/images/20221104/redmi_k50_01.jpg', price: 9999, flashPromotionPrice: 7999, flashPromotionCount: 20 }
    ],
    3: [
      { id: 6, name: 'Samsung Galaxy S24', pic: 'http://macro-oss.oss-cn-shenzhen.aliyuncs.com/mall/images/20221104/huawei_mate50_01.jpg', price: 6999, flashPromotionPrice: 4999, flashPromotionCount: 0 }
    ]
  }
}

// 秒杀操作
const handleSeckill = async (item) => {
  if (!item.id || (item.flashPromotionCount || 0) <= 0) return

  await ElMessageBox.confirm(
      `确定要秒杀「${item.name}」吗？`,
      '确认秒杀',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
  )

  try {
    const res = await request.post('/seckill/order', {
      goodsId: item.id,
      memberReceiveAddressId: 1,
      payType: 1
    })
    if (res.code === 200) {
      ElMessage.success('秒杀成功！即将跳转到订单页')
      for (const sessionId of Object.keys(productsMap.value)) {
        const products = productsMap.value[sessionId]
        const index = products.findIndex(p => p.id === item.id)
        if (index !== -1) {
          products[index].flashPromotionCount--
          break
        }
      }
      setTimeout(() => {
        window.location.href = '/order'
      }, 1500)
    } else {
      ElMessage.error(res.msg || '秒杀失败')
    }
  } catch (e) {
    console.error('秒杀失败:', e)
    ElMessage.error('秒杀失败：' + (e.response?.data?.msg || e.message || '未知错误'))
  }
}

const handleTabClick = () => {
  if (activeTab.value === 'upcoming') {
    countdownSessionId.value = upcomingSessions.value[0]?.id || null
  }
}

onMounted(() => {
  loadData()
  countdownTimer = setInterval(updateCountdown, 1000)
})

onUnmounted(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
  }
})
</script>

<style scoped>
.seckill-page {
  padding: 20px;
}

.promotion-header {
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a24 100%);
  padding: 20px;
  border-radius: 12px;
  margin-bottom: 20px;
  color: white;
}

.promotion-header h2 {
  font-size: 24px;
  margin: 0 0 10px 0;
}

.promotion-time {
  font-size: 14px;
  opacity: 0.9;
  margin: 0;
}

.session-tabs {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.session-card {
  padding: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.session-card:last-child {
  border-bottom: none;
}

.session-card.active {
  background: linear-gradient(135deg, #fff5f5 0%, #fff0e6 100%);
}

.session-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.session-time {
  font-size: 16px;
  font-weight: bold;
  color: #333;
}

.session-status {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: bold;
}

.session-status.upcoming {
  background: #e6f7ff;
  color: #1890ff;
}

.session-status.current {
  background: #fff1f0;
  color: #ff4d4f;
  animation: pulse 1.5s infinite;
}

.session-status.ended {
  background: #f5f5f5;
  color: #999;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

.countdown {
  text-align: center;
  padding: 10px;
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
  color: white;
  font-size: 18px;
  font-weight: bold;
  border-radius: 8px;
  margin-bottom: 15px;
}

.session-products {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
}

.product-mini {
  flex: 1;
  min-width: 200px;
  max-width: 250px;
  background: white;
  border-radius: 8px;
  padding: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  display: flex;
  flex-direction: column;
}

.mini-img {
  width: 100%;
  height: 120px;
  object-fit: cover;
  border-radius: 6px;
  margin-bottom: 10px;
}

.mini-info {
  flex: 1;
}

.mini-name {
  font-size: 14px;
  font-weight: bold;
  color: #333;
  margin-bottom: 8px;
  height: 40px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.mini-price {
  margin-bottom: 5px;
}

.mini-price .flash {
  font-size: 18px;
  font-weight: bold;
  color: #ff4d4f;
  margin-right: 8px;
}

.mini-price .origin {
  font-size: 12px;
  color: #999;
  text-decoration: line-through;
}

.mini-stock {
  font-size: 12px;
  color: #ff4d4f;
  margin-bottom: 10px;
}

.product-mini .el-button {
  width: 100%;
}

.empty-state {
  text-align: center;
  padding: 60px 0;
  color: #999;
}

.empty-icon {
  font-size: 60px;
  margin-bottom: 20px;
}
</style>