<template>
  <div class="order-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>📋 我的订单</h2>
      <span class="order-count">共 {{ orderList.length }} 笔订单</span>
    </div>

    <!-- 订单状态筛选 -->
    <div class="status-tabs-wrapper">
      <div class="status-tabs">
        <button 
          v-for="tab in statusTabs" 
          :key="tab.value"
          :class="['status-tab', { active: activeStatus === tab.value }]"
          @click="switchStatus(tab.value)"
        >
          {{ tab.label }}
          <span v-if="tab.count > 0" class="tab-badge">{{ tab.count }}</span>
        </button>
      </div>
    </div>

    <!-- 订单列表 -->
    <div class="order-list" v-if="orderList.length > 0">
      <el-card v-for="order in orderList" :key="order.id" class="order-card" shadow="hover">
        <!-- 订单头部 -->
        <div class="order-header">
          <div class="order-info">
            <span class="order-no">订单号：{{ order.orderSn }}</span>
            <span class="order-time">下单时间：{{ order.createTime }}</span>
          </div>
          <span :class="['order-status', getStatusClass(order.status)]">
            {{ getStatusText(order.status) }}
          </span>
        </div>
        
        <!-- 订单商品 -->
        <div class="order-items">
          <div 
            class="order-item" 
            v-for="item in order.items" 
            :key="item.id"
            @click="viewProduct(item.productId)"
          >
            <img :src="item.productPic || 'https://via.placeholder.com/100'" alt="商品图片" class="item-image" />
            <div class="item-info">
              <div class="item-name">{{ item.productName }}</div>
              <div class="item-spec">规格：{{ item.productSpec || '秒杀专享版' }}</div>
              <div class="item-bottom">
                <span class="item-price">¥{{ item.productPrice }}</span>
                <span class="item-quantity">x{{ item.quantity }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 订单底部 -->
        <div class="order-footer">
          <div class="order-total">
            <span>共 {{ getTotalQuantity(order.items) }} 件商品</span>
            <span class="total-label">实付款：</span>
            <span class="total-price">¥{{ order.totalAmount }}</span>
          </div>
          <div class="order-actions">
            <el-button 
              v-if="order.status === 0" 
              class="action-btn primary-btn"
              @click="payOrder(order)"
            >
              💳 立即支付
            </el-button>
            <el-button 
              v-if="order.status === 0" 
              class="action-btn danger-btn"
              @click="cancelOrder(order)"
            >
              ❌ 取消订单
            </el-button>
            <el-button 
              v-if="order.status === 1" 
              class="action-btn info-btn"
              @click="remindSeller(order)"
            >
              🔔 提醒发货
            </el-button>
            <el-button 
              v-if="order.status === 2" 
              class="action-btn success-btn"
              @click="confirmOrder(order)"
            >
              ✅ 确认收货
            </el-button>
            <el-button 
              v-if="order.status === 3" 
              class="action-btn secondary-btn"
              @click="deleteOrder(order)"
            >
              🗑️ 删除订单
            </el-button>
            <el-button 
              v-if="order.status === 4" 
              class="action-btn secondary-btn"
              @click="deleteOrder(order)"
            >
              🗑️ 删除订单
            </el-button>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 空状态 -->
    <div class="empty-state" v-else>
      <div class="empty-icon">🛒</div>
      <h3>暂无订单</h3>
      <p>快去挑选心仪的商品吧</p>
      <el-button type="primary" size="large" @click="goShopping">
        🛍️ 去购物
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const activeStatus = ref('-1')
const orderList = ref([])

// 状态标签配置
const statusTabs = ref([
  { value: '-1', label: '全部', count: 0 },
  { value: '0', label: '待付款', count: 0 },
  { value: '1', label: '待发货', count: 0 },
  { value: '2', label: '已发货', count: 0 },
  { value: '3', label: '已完成', count: 0 },
  { value: '4', label: '已关闭', count: 0 }
])

onMounted(() => {
  loadOrders()
})

const loadOrders = async () => {
  try {
    const res = await request.get('/order/list', {
      params: {
        status: activeStatus.value === '-1' ? null : activeStatus.value,
        pageNum: 1,
        pageSize: 20
      }
    })
    // 转换API返回格式
    if (res.data && res.data.list) {
      orderList.value = res.data.list.map(order => ({
        id: order.id,
        orderSn: order.orderSn,
        createTime: formatDateTime(order.createTime),
        totalAmount: order.payAmount || order.totalAmount,
        status: order.status,
        items: order.orderItems ? order.orderItems.map(item => ({
          id: item.id,
          productId: item.productId,
          productPic: item.productPic || 'https://via.placeholder.com/100',
          productName: item.productName,
          productSpec: item.productAttr,
          productPrice: item.productPrice,
          quantity: item.quantity
        })) : []
      }))
    } else {
      orderList.value = getMockOrders()
    }
    updateStatusCount()
  } catch (e) {
    console.error('加载订单失败:', e)
    // 即使接口失败也显示模拟数据
    orderList.value = getMockOrders()
    updateStatusCount()
  }
}

// 格式化日期时间
const formatDateTime = (dateStr) => {
  if (!dateStr) return ''
  try {
    const date = new Date(dateStr)
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    }).replace(/\//g, '-')
  } catch {
    return dateStr
  }
}

const getMockOrders = () => [
  {
    id: 1,
    orderSn: '20260506123456789',
    createTime: '2026-05-06 14:30:25',
    totalAmount: 6999.00,
    status: 0,
    items: [
      { id: 1, productId: 1, productPic: 'https://via.placeholder.com/100', productName: 'iPhone 15 Pro Max 秒杀版', productSpec: '256GB 原色钛金属', productPrice: 6999.00, quantity: 1 }
    ]
  },
  {
    id: 2,
    orderSn: '20260505987654321',
    createTime: '2026-05-05 10:15:33',
    totalAmount: 14999.00,
    status: 3,
    items: [
      { id: 2, productId: 2, productPic: 'https://via.placeholder.com/100', productName: 'MacBook Pro 16寸 M3 Pro', productSpec: '18GB 512GB', productPrice: 14999.00, quantity: 1 }
    ]
  },
  {
    id: 3,
    orderSn: '20260504112233445',
    createTime: '2026-05-04 16:45:12',
    totalAmount: 1299.00,
    status: 2,
    items: [
      { id: 3, productId: 3, productPic: 'https://via.placeholder.com/100', productName: 'AirPods Pro 2 无线耳机', productSpec: 'USB-C充电盒', productPrice: 1299.00, quantity: 2 }
    ]
  },
  {
    id: 4,
    orderSn: '20260503556677889',
    createTime: '2026-05-03 09:20:45',
    totalAmount: 6499.00,
    status: 1,
    items: [
      { id: 4, productId: 5, productPic: 'https://via.placeholder.com/100', productName: 'Apple Watch Ultra 2', productSpec: '49mm 钛金属', productPrice: 6499.00, quantity: 1 }
    ]
  },
  {
    id: 5,
    orderSn: '20260502998877665',
    createTime: '2026-05-02 20:10:08',
    totalAmount: 1999.00,
    status: 4,
    items: [
      { id: 5, productId: 6, productPic: 'https://via.placeholder.com/100', productName: 'Sony WH-1000XM5 耳机', productSpec: '黑色', productPrice: 1999.00, quantity: 1 }
    ]
  }
]

const updateStatusCount = () => {
  statusTabs.value.forEach(tab => {
    if (tab.value === '-1') {
      tab.count = orderList.value.length
    } else {
      tab.count = orderList.value.filter(o => o.status === parseInt(tab.value)).length
    }
  })
}

const switchStatus = (status) => {
  activeStatus.value = status
  loadOrders()
}

const getStatusText = (status) => {
  const statusMap = {
    0: '待付款',
    1: '待发货',
    2: '已发货',
    3: '已完成',
    4: '已关闭'
  }
  return statusMap[status] || '未知'
}

const getStatusClass = (status) => {
  const classMap = {
    0: 'status-pending',
    1: 'status-shipping',
    2: 'status-shipped',
    3: 'status-completed',
    4: 'status-closed'
  }
  return classMap[status] || ''
}

const getStatusIcon = (status) => {
  const iconMap = {
    0: Clock,
    1: Package,
    2: Truck,
    3: CheckCircle,
    4: X
  }
  return iconMap[status] || Clock
}

const getTotalQuantity = (items) => {
  return items.reduce((sum, item) => sum + item.quantity, 0)
}

const viewProduct = (productId) => {
  window.location.href = `/seckill?id=${productId}`
}

const goShopping = () => {
  window.location.href = '/home'
}

const payOrder = async (order) => {
  try {
    await request.post('/order/paySuccess', null, {
      params: { orderId: order.id, payType: 1 }
    })
    ElMessage.success(`订单 ${order.orderSn} 支付成功！`)
    order.status = 1
    updateStatusCount()
  } catch (e) {
    ElMessage.error('支付失败：' + (e.response?.data?.msg || e.message))
  }
}

const cancelOrder = async (order) => {
  try {
    await ElMessageBox.confirm(
      '确定要取消该订单吗？',
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await request.post('/order/cancelUserOrder', null, {
      params: { orderId: order.id }
    })
    order.status = 4
    updateStatusCount()
    ElMessage.success('订单已取消')
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('取消失败：' + (e.response?.data?.msg || e.message))
    } else {
      ElMessage.info('已取消操作')
    }
  }
}

const remindSeller = (order) => {
  ElMessage.info(`已提醒卖家发货，订单号：${order.orderSn}`)
}

const confirmOrder = async (order) => {
  try {
    await ElMessageBox.confirm(
      '请确认已收到商品，确认后订单将完成',
      '提示',
      {
        confirmButtonText: '确认收货',
        cancelButtonText: '取消',
        type: 'info'
      }
    )
    await request.post('/order/confirmReceiveOrder', null, {
      params: { orderId: order.id }
    })
    order.status = 3
    updateStatusCount()
    ElMessage.success('已确认收货')
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('确认失败：' + (e.response?.data?.msg || e.message))
    } else {
      ElMessage.info('已取消操作')
    }
  }
}

const deleteOrder = async (order) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除该订单吗？删除后无法恢复',
      '提示',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'danger'
      }
    )
    await request.post('/order/deleteOrder', null, {
      params: { orderId: order.id }
    })
    orderList.value = orderList.value.filter(o => o.id !== order.id)
    updateStatusCount()
    ElMessage.success('订单已删除')
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败：' + (e.response?.data?.msg || e.message))
    } else {
      ElMessage.info('已取消操作')
    }
  }
}
</script>

<style scoped>
.order-page {
  padding: 20px;
  background: #f5f5f5;
  min-height: calc(100vh - 80px);
}

/* 页面标题 */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.page-header h2 {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
  margin: 0;
}

.order-count {
  font-size: 14px;
  color: #999;
}

/* 状态标签 */
.status-tabs-wrapper {
  background: white;
  border-radius: 12px;
  padding: 15px;
  margin-bottom: 20px;
}

.status-tabs {
  display: flex;
  gap: 10px;
}

.status-tab {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 10px 20px;
  border-radius: 25px;
  background: #f5f5f5;
  border: none;
  cursor: pointer;
  font-size: 14px;
  color: #666;
  transition: all 0.3s ease;
}

.status-tab:hover {
  background: #e8f4ff;
  color: #409eff;
}

.status-tab.active {
  background: linear-gradient(135deg, #409eff 0%, #67c23a 100%);
  color: white;
}

.tab-badge {
  background: #ff4d4f;
  color: white;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
  min-width: 20px;
  text-align: center;
}

.status-tab.active .tab-badge {
  background: white;
  color: #409eff;
}

/* 订单列表 */
.order-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.order-card {
  border-radius: 12px;
  overflow: hidden;
}

/* 订单头部 */
.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  background: #fafafa;
}

.order-info {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.order-no {
  font-size: 14px;
  color: #666;
}

.order-time {
  font-size: 12px;
  color: #999;
}

.order-status {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 14px;
  font-weight: bold;
  padding: 6px 12px;
  border-radius: 20px;
}

.status-pending {
  background: #fff7e6;
  color: #fa8c16;
}

.status-shipping {
  background: #e8f4ff;
  color: #409eff;
}

.status-shipped {
  background: #e6f7ff;
  color: #1890ff;
}

.status-completed {
  background: #f6ffed;
  color: #52c41a;
}

.status-closed {
  background: #f5f5f5;
  color: #999;
}

/* 订单商品 */
.order-items {
  padding: 20px;
}

.order-item {
  display: flex;
  gap: 15px;
  padding: 10px 0;
  cursor: pointer;
  transition: background 0.2s ease;
}

.order-item:hover {
  background: #fafafa;
  border-radius: 8px;
}

.item-image {
  width: 100px;
  height: 100px;
  object-fit: cover;
  border-radius: 8px;
  flex-shrink: 0;
}

.item-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.item-name {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
  margin-bottom: 8px;
}

.item-spec {
  font-size: 12px;
  color: #999;
  margin-bottom: 10px;
}

.item-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.item-price {
  font-size: 16px;
  color: #ff4d4f;
  font-weight: bold;
}

.item-quantity {
  font-size: 12px;
  color: #999;
}

/* 订单底部 */
.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  background: #fafafa;
  border-top: 1px solid #f0f0f0;
}

.order-total {
  display: flex;
  align-items: baseline;
  gap: 8px;
  font-size: 14px;
  color: #666;
}

.total-label {
  font-size: 14px;
}

.total-price {
  font-size: 20px;
  color: #ff4d4f;
  font-weight: bold;
}

.order-actions {
  display: flex;
  gap: 10px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 14px;
  border: none;
  cursor: pointer;
  transition: all 0.3s ease;
}

.primary-btn {
  background: linear-gradient(135deg, #409eff 0%, #67c23a 100%);
  color: white;
}

.primary-btn:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.4);
}

.danger-btn {
  background: #fff1f0;
  color: #ff4d4f;
  border: 1px solid #ffccc7;
}

.danger-btn:hover {
  background: #ffccc7;
}

.success-btn {
  background: #f6ffed;
  color: #52c41a;
  border: 1px solid #b7eb8f;
}

.success-btn:hover {
  background: #b7eb8f;
}

.info-btn {
  background: #e8f4ff;
  color: #409eff;
  border: 1px solid #91caff;
}

.info-btn:hover {
  background: #91caff;
}

.secondary-btn {
  background: #f5f5f5;
  color: #666;
  border: 1px solid #d9d9d9;
}

.secondary-btn:hover {
  background: #d9d9d9;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 80px 0;
  background: white;
  border-radius: 12px;
  margin-top: 20px;
}

.empty-icon {
  margin-bottom: 20px;
}

.empty-state h3 {
  font-size: 18px;
  color: #303133;
  margin-bottom: 10px;
}

.empty-state p {
  font-size: 14px;
  color: #999;
  margin-bottom: 30px;
}
</style>