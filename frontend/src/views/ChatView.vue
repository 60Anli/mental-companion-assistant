<template>
  <div class="workspace">
    <aside class="sidebar">
      <div class="brand-row">
        <div>
          <p class="eyebrow">MindCare</p>
          <h2>心理陪伴助手</h2>
        </div>
        <el-tooltip content="退出登录">
          <el-button :icon="SwitchButton" circle @click="logout" />
        </el-tooltip>
      </div>
      <el-button class="full-button" type="primary" :icon="Plus" @click="newSession">新会话</el-button>
      <div class="session-list">
        <button
          v-for="session in sessions"
          :key="session.id"
          class="session-item"
          :class="{ active: session.id === sessionId }"
          @click="selectSession(session)"
        >
          <el-icon><ChatDotRound /></el-icon>
          <span>{{ session.title }}</span>
        </button>
      </div>
      <el-button v-if="auth.role === 'ADMIN'" :icon="Setting" @click="router.push('/admin')">管理后台</el-button>
    </aside>

    <main class="chat-main">
      <header class="topbar">
        <div>
          <p class="eyebrow">Chat Workflow</p>
          <h1>陪伴对话</h1>
        </div>
        <el-tag effect="plain">{{ auth.username }}</el-tag>
      </header>

      <section class="message-list">
        <div v-for="(item, index) in messages" :key="index" class="message-row" :class="item.role.toLowerCase()">
          <div class="message-bubble">{{ item.content }}</div>
        </div>
      </section>

      <footer class="composer">
        <el-input
          v-model="input"
          type="textarea"
          :autosize="{ minRows: 2, maxRows: 5 }"
          resize="none"
          placeholder="输入你想聊的话..."
          @keydown.enter.exact.prevent="submit"
        />
        <el-button type="primary" :icon="Promotion" :loading="sending" @click="submit">发送</el-button>
      </footer>
    </main>

    <aside class="status-panel">
      <div class="panel-head">
        <div>
          <p class="eyebrow">Workflow</p>
          <h2>执行状态</h2>
        </div>
      </div>
      <div class="status-grid">
        <span>当前意图类型</span><strong>{{ state.intent || '-' }}</strong>
        <span>当前风险等级</span><strong :class="riskClass">{{ state.riskLevel || '-' }}</strong>
        <span>是否执行 RAG</span><strong>是</strong>
        <span>RAG 命中文档数</span><strong>{{ state.references?.length || 0 }}</strong>
        <span>是否写入 Excel</span><strong>{{ hasAction('WRITE_EXCEL') ? '是' : '否' }}</strong>
        <span>是否发送邮件</span><strong>{{ hasAction('SEND_EMAIL_ALERT') ? '是' : '否' }}</strong>
      </div>
      <el-divider />
      <div class="action-list">
        <el-tag v-for="action in state.actions" :key="action" effect="plain">{{ action }}</el-tag>
      </div>
      <el-divider />
      <div class="reference-list">
        <div v-for="(ref, index) in state.references" :key="index" class="reference-item">
          <strong>{{ ref.documentName }}</strong>
          <p>{{ ref.content }}</p>
        </div>
      </div>
    </aside>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ChatDotRound, Plus, Promotion, Setting, SwitchButton } from '@element-plus/icons-vue'
import { fetchMessages, fetchSessions, sendMessage } from '../api/chat'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const sessions = ref([])
const sessionId = ref(null)
const messages = ref([])
const input = ref('')
const sending = ref(false)
const state = reactive({
  intent: '',
  riskLevel: '',
  riskType: '',
  references: [],
  actions: []
})

const riskClass = computed(() => ({
  low: state.riskLevel === 'LOW',
  medium: state.riskLevel === 'MEDIUM',
  high: state.riskLevel === 'HIGH'
}))

function hasAction(action) {
  return state.actions?.includes(action)
}

function newSession() {
  sessionId.value = null
  messages.value = []
}

async function selectSession(session) {
  sessionId.value = session.id
  const res = await fetchMessages(session.id)
  messages.value = res.data
}

async function loadSessions() {
  const res = await fetchSessions()
  sessions.value = res.data
}

async function submit() {
  const text = input.value.trim()
  if (!text) return
  messages.value.push({ role: 'USER', content: text })
  input.value = ''
  sending.value = true
  try {
    const res = await sendMessage({ sessionId: sessionId.value, message: text })
    const data = res.data
    sessionId.value = data.sessionId
    messages.value.push({ role: 'ASSISTANT', content: data.reply })
    Object.assign(state, data)
    await loadSessions()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    sending.value = false
  }
}

function logout() {
  auth.logout()
  router.push('/login')
}

onMounted(loadSessions)
</script>
