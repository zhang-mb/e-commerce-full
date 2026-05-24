<template>
  <div class="home">
    <h2>全部商品</h2>
    <el-row :gutter="20">
      <el-col :span="6" v-for="item in list" :key="item.id" class="product-card">
        <el-card shadow="hover">
          <div class="product-image">
            <img :src="item.pic" :alt="item.name" class="product-img" />
          </div>
          <div class="title">{{ item.name }}</div>
          <div class="price">¥{{ item.price }}</div>
          <div class="flash-price">秒杀价 ¥{{ item.flashPromotionPrice }}</div>
          <el-button type="primary" @click="goSeckill(item.id)">
            立即秒杀
          </el-button>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'

const list = ref([])

onMounted(async () => {
  try {
    // 调用秒杀商品列表API（数据库中有数据）
    const res = await request.get('/seckill/goods/list')
    list.value = res.data || []
  } catch (error) {
    console.error('获取商品列表失败:', error)
    list.value = []
  }
})

const goSeckill = (goodsId) => {
  // 跳转到秒杀专区
  window.location.href = `/seckill?id=${goodsId}`
}
</script>

<style scoped>
.home { padding: 20px; }
.product-image {
  width: 100%;
  height: 150px;
  overflow: hidden;
  margin-bottom: 10px;
}
.product-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.title { font-size: 14px; margin-bottom: 8px; height: 40px; overflow: hidden; }
.price { color: #999; text-decoration: line-through; margin-bottom: 5px; font-size: 12px; }
.flash-price { color: red; font-weight: bold; margin-bottom: 10px; font-size: 16px; }
</style>