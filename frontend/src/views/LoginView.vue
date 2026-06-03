<template>
  <main class="login-page">
    <section class="login-panel">
      <div class="login-header">
        <p class="eyebrow">Mental Companion Assistant</p>
        <h1>心理陪伴助手</h1>
        <p class="login-desc">一个安全、温暖的倾诉空间</p>
      </div>

      <el-segmented v-model="mode" :options="modeOptions" class="auth-switch" block />

      <!-- 登录表单 -->
      <el-form v-if="mode === 'login'" :model="loginForm" label-position="top" @submit.prevent="handleLogin">
        <el-form-item label="用户名">
          <el-input
            v-model="loginForm.username"
            size="large"
            autocomplete="username"
            placeholder="请输入用户名"
            :prefix-icon="User"
          />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="loginForm.password"
            size="large"
            type="password"
            autocomplete="current-password"
            show-password
            placeholder="请输入密码"
            :prefix-icon="Lock"
          />
        </el-form-item>
        <el-button
          type="primary"
          size="large"
          class="full-button login-btn"
          :loading="loading"
          @click="handleLogin"
        >
          登录
        </el-button>
        <p class="login-hint">测试账号：admin / admin123 或 user / user123</p>
      </el-form>

      <!-- 注册表单 -->
      <el-form v-else :model="registerForm" label-position="top" @submit.prevent="handleRegister">
        <el-form-item label="用户名">
          <el-input v-model="registerForm.username" size="large" placeholder="3-32 位用户名" maxlength="32" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="registerForm.password" size="large" type="password" show-password placeholder="至少 6 位" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="registerForm.realName" size="large" placeholder="你的真实姓名" />
        </el-form-item>
        <el-form-item label="学院">
          <el-input v-model="registerForm.college" size="large" placeholder="例如：人工智能学院" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="registerForm.email" size="large" type="email" placeholder="可选，用于账号资料" />
        </el-form-item>
        <el-button
          type="primary"
          size="large"
          class="full-button login-btn"
          :loading="loading"
          @click="handleRegister"
        >
          注册并进入
        </el-button>
      </el-form>
    </section>
  </main>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { login, register } from '../api/auth'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const mode = ref('login')

const modeOptions = [
  { label: '登录', value: 'login' },
  { label: '注册', value: 'register' },
]

const loginForm = reactive({ username: '', password: '' })

const registerForm = reactive({
  username: '',
  password: '',
  realName: '',
  college: '',
  email: '',
})

async function handleLogin() {
  if (!loginForm.username || !loginForm.password) {
    ElMessage.warning('请填写用户名和密码')
    return
  }
  loading.value = true
  try {
    const res = await login(loginForm)
    auth.setSession(res.data)
    router.push('/')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  if (!registerForm.username || !registerForm.password || !registerForm.realName || !registerForm.college) {
    ElMessage.warning('请填写用户名、密码、姓名和学院')
    return
  }
  if (registerForm.password.length < 6) {
    ElMessage.warning('密码至少 6 位')
    return
  }
  loading.value = true
  try {
    const res = await register(registerForm)
    auth.setSession(res.data)
    ElMessage.success('注册成功，欢迎你！')
    router.push('/')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-header {
  text-align: center;
  margin-bottom: 8px;
}

.login-desc {
  margin: 8px 0 0;
  font-size: 14px;
  color: var(--warm-400);
}

.login-btn {
  margin-top: 8px;
  height: 48px !important;
  font-size: 16px !important;
  font-weight: 600 !important;
  border-radius: var(--radius-md) !important;
  background: var(--green-500) !important;
  border-color: var(--green-500) !important;
}

.login-btn:hover {
  background: var(--green-600) !important;
  border-color: var(--green-600) !important;
}

.login-hint {
  margin: 16px 0 0;
  text-align: center;
  font-size: 12px;
  color: var(--warm-400);
}
</style>
