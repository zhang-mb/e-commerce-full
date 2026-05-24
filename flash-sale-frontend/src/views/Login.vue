<template>
  <div class="login-container">
    <!-- 背景装饰 -->
    <div class="bg-decoration">
      <div class="decoration-circle circle-1"></div>
      <div class="decoration-circle circle-2"></div>
      <div class="decoration-circle circle-3"></div>
    </div>

    <!-- 登录卡片 -->
    <div class="login-card">
      <!-- Logo区域 -->
      <div class="logo-section">
        <div class="logo">
          <span class="logo-icon">⚡</span>
        </div>
        <h1 class="title">秒杀商城</h1>
        <p class="subtitle">限时特惠 抢购不停</p>
      </div>

      <!-- 表单区域 -->
      <el-form 
        ref="loginForm" 
        :model="form" 
        :rules="rules" 
        class="login-form"
      >
        <el-form-item prop="username">
            <el-input 
            v-model="form.username" 
            placeholder="请输入账号" 
            size="large"
            :disabled="loading"
          />
        </el-form-item>

        <el-form-item prop="password">
            <el-input 
            v-model="form.password" 
            type="password" 
            placeholder="请输入密码" 
            size="large"
            :disabled="loading"
            @keyup.enter="login"
          />
        </el-form-item>

        <el-form-item class="form-actions">
          <el-button 
            type="primary" 
            size="large" 
            class="login-btn"
            :loading="loading"
            @click="login"
          >
            {{ loading ? '登录中...' : '立即登录' }}
          </el-button>
        </el-form-item>

        <div class="login-tips">
          <span>测试账号：admin / admin</span>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'


const router = useRouter()
const loginForm = ref(null)
const loading = ref(false)

const form = ref({
  username: '',
  password: ''
})

const rules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 3, max: 20, message: '账号长度在3到20个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在6到20个字符', trigger: 'blur' }
  ]
}

const login = async () => {
  // 表单验证
  const valid = await loginForm.value.validate()
  if (!valid) return

  loading.value = true
  try {
    const res = await request.post('/sso/login', null, {
      params: {
        username: form.value.username,
        password: form.value.password
      }
    })
    
    if (res.code === 200) {
      localStorage.setItem('token', res.data.token)
      ElMessage.success('登录成功！')
      setTimeout(() => {
        router.push('/home')
      }, 1000)
    } else {
      ElMessage.error(res.msg || '登录失败')
    }
  } catch (e) {
    console.error('登录错误:', e)
    const errorMsg = e.response?.data?.msg || e.message || '登录失败'
    ElMessage.error(errorMsg)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: relative;
  overflow: hidden;
}

/* 背景装饰 */
.bg-decoration {
  position: absolute;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.decoration-circle {
  position: absolute;
  border-radius: 50%;
  opacity: 0.3;
}

.circle-1 {
  width: 400px;
  height: 400px;
  background: #ff4d4f;
  top: -100px;
  right: -100px;
  animation: float 6s ease-in-out infinite;
}

.circle-2 {
  width: 300px;
  height: 300px;
  background: #409eff;
  bottom: -50px;
  left: -50px;
  animation: float 8s ease-in-out infinite reverse;
}

.circle-3 {
  width: 200px;
  height: 200px;
  background: #52c41a;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  animation: float 5s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0px); }
  50% { transform: translateY(-20px); }
}

/* 登录卡片 */
.login-card {
  width: 420px;
  padding: 40px;
  background: white;
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  z-index: 1;
}

/* Logo区域 */
.logo-section {
  text-align: center;
  margin-bottom: 30px;
}

.logo {
  width: 80px;
  height: 80px;
  margin: 0 auto 20px;
  background: linear-gradient(135deg, #ff4d4f 0%, #fa541c 100%);
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  box-shadow: 0 8px 24px rgba(255, 77, 79, 0.4);
}

.logo-icon {
  font-size: 40px;
  color: white;
}

.title {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
  margin: 0 0 10px 0;
}

.subtitle {
  font-size: 14px;
  color: #999;
  margin: 0;
}

/* 表单 */
.login-form {
  width: 100%;
}

.input-wrapper {
  position: relative;
}

.input-icon {
  position: absolute;
  left: 15px;
  top: 50%;
  transform: translateY(-50%);
  color: #999;
  font-size: 18px;
}

.login-form .el-input {
  padding-left: 45px;
  height: 48px;
  border-radius: 12px;
}

.login-form .el-input__inner {
  border-radius: 12px;
  font-size: 15px;
}

.form-actions {
  margin-top: 20px;
}

.login-btn {
  width: 100%;
  height: 50px;
  font-size: 16px;
  font-weight: bold;
  border-radius: 12px;
  background: linear-gradient(135deg, #ff4d4f 0%, #fa541c 100%);
  border: none;
}

.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(255, 77, 79, 0.4);
}

/* 登录提示 */
.login-tips {
  text-align: center;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}

.login-tips span {
  font-size: 13px;
  color: #999;
}

/* 响应式 */
@media (max-width: 500px) {
  .login-card {
    width: 90%;
    padding: 30px 20px;
  }

  .title {
    font-size: 24px;
  }
}
</style>