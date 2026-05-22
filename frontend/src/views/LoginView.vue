<template>
  <main class="login-page">
    <section class="login-panel">
      <div>
        <p class="eyebrow">Mental Companion Assistant</p>
        <h1>心理陪伴助手</h1>
      </div>

      <el-segmented v-model="mode" :options="modeOptions" class="auth-switch" />

      <el-form v-if="mode === 'login'" :model="loginForm" label-position="top" @submit.prevent="handleLogin">
        <el-form-item label="用户名">
          <el-input v-model="loginForm.username" size="large" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="loginForm.password" size="large" type="password" autocomplete="current-password" show-password />
        </el-form-item>
        <el-button type="primary" size="large" class="full-button" :loading="loading" @click="handleLogin">
          登录
        </el-button>
      </el-form>

      <el-form v-else :model="registerForm" label-position="top" @submit.prevent="handleRegister">
        <el-form-item label="用户名">
          <el-input v-model="registerForm.username" size="large" autocomplete="username" placeholder="3-32 位用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="registerForm.password" size="large" type="password" autocomplete="new-password" show-password placeholder="至少 6 位" />
        </el-form-item>
        <el-form-item label="学生姓名">
          <el-input v-model="registerForm.realName" size="large" placeholder="例如：张同学" />
        </el-form-item>
        <el-form-item label="学院">
          <el-input v-model="registerForm.college" size="large" placeholder="例如：人工智能学院" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="registerForm.email" size="large" type="email" autocomplete="email" placeholder="用于账号资料展示，可选" />
        </el-form-item>
        <el-button type="primary" size="large" class="full-button" :loading="loading" @click="handleRegister">
          注册并进入
        </el-button>
      </el-form>

      <div class="login-hints">
        <span>管理员：admin / admin123</span>
        <span>普通用户：user / user123</span>
      </div>
    </section>
  </main>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, register } from '../api/auth'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const mode = ref('login')
const modeOptions = [
  { label: '登录', value: 'login' },
  { label: '注册', value: 'register' }
]

const loginForm = reactive({
  username: 'admin',
  password: 'admin123'
})

const registerForm = reactive({
  username: '',
  password: '',
  realName: '',
  college: '',
  email: ''
})

async function handleLogin() {
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
    ElMessage.warning('请填写用户名、密码、学生姓名和学院')
    return
  }
  loading.value = true
  try {
    const res = await register(registerForm)
    auth.setSession(res.data)
    ElMessage.success('注册成功')
    router.push('/')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}
</script>
