<template>
  <div class="workspace">
    <!-- ========== 侧栏 ========== -->
    <aside class="sidebar">
      <div class="brand-row">
        <div>
          <p class="eyebrow">MindCare</p>
          <h2>心理陪伴助手</h2>
        </div>
      </div>

      <el-button class="full-button" type="primary" @click="newSession">
        <el-icon><Plus /></el-icon>
        新对话
      </el-button>

      <div class="session-list">
        <div
          v-for="s in sessions"
          :key="s.id"
          class="session-item"
          :class="{ active: s.id === sessionId }"
          @click="selectSession(s)"
        >
          <el-icon class="session-icon"><ChatDotRound /></el-icon>
          <span class="session-title">{{ s.title || '新对话' }}</span>
          <el-button
            class="session-delete"
            text
            size="small"
            :icon="Delete"
            @click.stop="handleDelete(s)"
          />
        </div>
        <p v-if="!sessions.length" class="empty-hint">暂无对话记录</p>
      </div>

      <div class="sidebar-footer">
        <el-button v-if="auth.role === 'ADMIN'" text :icon="Setting" @click="router.push('/admin')">
          管理后台
        </el-button>
        <el-button text :icon="SwitchButton" @click="logout">退出登录</el-button>
      </div>
    </aside>

    <!-- ========== 聊天主区 ========== -->
    <main class="chat-main">
      <!-- 顶栏 -->
      <header class="topbar">
        <div>
          <p class="eyebrow">{{ auth.role === 'ADMIN' ? '管理员' : '学生' }}</p>
          <h1>{{ currentTitle }}</h1>
        </div>
        <el-tag effect="plain" size="small" round>{{ auth.username }}</el-tag>
      </header>

      <!-- 消息区 / 欢迎页 -->
      <section class="message-list" v-if="messages.length">
        <div
          v-for="(item, index) in messages"
          :key="index"
          class="message-row"
          :class="item.role.toLowerCase()"
        >
          <span class="message-sender">{{ item.role === 'USER' ? '你' : 'MindCare' }}</span>
          <div class="message-bubble">{{ item.content }}</div>
          <!-- 用户消息的图片展示 -->
          <div v-if="item.role === 'USER' && item.mediaPreviews && item.mediaPreviews.length" class="message-images">
            <img
              v-for="(preview, pi) in item.mediaPreviews"
              :key="pi"
              :src="preview"
              class="message-image-thumb"
              @click="previewImage(preview)"
            />
          </div>
          <!-- 助手消息底部：RAG 参考提示 -->
          <div
            v-if="item.role === 'ASSISTANT' && item.refs && item.refs.length"
            class="message-refs"
            @click="item.showRefs = !item.showRefs"
          >
            <el-icon><Collection /></el-icon>
            参考了 {{ item.refs.length }} 篇知识库文档
            <span v-if="item.showRefs" style="color: var(--warm-400)">▲</span>
            <span v-else style="color: var(--warm-400)">▼</span>
          </div>
          <!-- 展开的知识库参考 -->
          <div v-if="item.showRefs && item.refs" class="refs-expand">
            <div v-for="(ref, ri) in item.refs" :key="ri" class="refs-expand-item">
              <span class="refs-expand-name">{{ ref.documentName }}</span>
              <p>{{ ref.content?.slice(0, 200) }}{{ ref.content?.length > 200 ? '...' : '' }}</p>
            </div>
          </div>
        </div>

        <!-- 思考中动画 -->
        <div v-if="thinking" class="message-row assistant">
          <span class="message-sender">MindCare</span>
          <div class="think-placeholder">
            <span>思考中</span>
            <span class="dot-pulse">
              <span></span><span></span><span></span>
            </span>
          </div>
        </div>
      </section>

      <!-- 欢迎页 -->
      <section v-else class="welcome-center">
        <div class="welcome-icon">🌿</div>
        <h2>你好，我在这里</h2>
        <p>无论你现在感觉如何，都可以放心地和我说说。我会认真倾听，陪你一起梳理。</p>
      </section>

      <!-- ===== 媒体预览条 ===== -->
      <div v-if="uploadFiles.length" class="media-preview-bar">
        <div v-for="(file, fi) in uploadFiles" :key="fi" class="media-preview-item">
          <img v-if="file.type.startsWith('image/')" :src="filePreviews[fi]" class="media-preview-thumb" />
          <video v-else-if="file.type.startsWith('video/')" :src="filePreviews[fi]" class="media-preview-thumb" muted />
          <span class="media-preview-name">{{ file.name }}</span>
          <el-button class="media-preview-remove" text size="small" :icon="Close" @click="removeFile(fi)" />
        </div>
      </div>

      <!-- 输入区 -->
      <footer class="composer">
        <!-- 上传按钮组 -->
        <div class="composer-tools">
          <input
            ref="imageInput"
            type="file"
            accept="image/jpeg,image/png,image/gif,image/webp"
            multiple
            hidden
            @change="onFilesPicked"
          />
          <input
            ref="videoInput"
            type="file"
            accept="video/mp4,video/webm,video/quicktime"
            hidden
            @change="onFilesPicked"
          />
          <el-button text :icon="Picture" title="上传图片" @click="$refs.imageInput.click()" />
          <el-button text :icon="VideoCamera" title="上传视频" @click="$refs.videoInput.click()" />
          <span v-if="uploadFiles.length" class="upload-count">{{ uploadFiles.length }} 个文件</span>
        </div>
        <div class="composer-input-row">
          <el-input
            v-model="input"
            type="textarea"
            :autosize="{ minRows: 1, maxRows: 4 }"
            resize="none"
            placeholder="说说你想聊的...（也可以上传图片或视频）"
            @keydown.enter.exact.prevent="submit"
          />
          <el-button
            type="primary"
            :disabled="(!input.trim() && !uploadFiles.length) || sending"
            :loading="sending"
            @click="submit"
          >
            <el-icon v-if="!sending"><Promotion /></el-icon>
            发送
          </el-button>
        </div>
      </footer>
    </main>

    <!-- ===== 图片预览弹窗 ===== -->
    <el-dialog v-model="imagePreviewVisible" title="图片预览" :close-on-click-modal="true" width="auto">
      <img :src="imagePreviewSrc" style="max-width: 80vw; max-height: 70vh; border-radius: 8px;" />
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ChatDotRound, Close, Collection, Delete, Picture, Plus, Promotion, Setting, SwitchButton, VideoCamera } from '@element-plus/icons-vue'
import { deleteSession, fetchMessages, fetchSessions, sendMessage } from '../api/chat'
import { useAuthStore } from '../stores/auth'

const MAX_FILES = 5
const MAX_FILE_SIZE_MB = 10

const router = useRouter()
const auth = useAuthStore()

const sessions = ref([])
const sessionId = ref(null)
const messages = ref([])
const input = ref('')
const sending = ref(false)
const thinking = ref(false)

// ── Media upload state ──
const uploadFiles = ref([])        // File[]
const filePreviews = ref([])       // string[] (object URLs)

// ── Image preview dialog ──
const imagePreviewVisible = ref(false)
const imagePreviewSrc = ref('')

const currentTitle = computed(() => {
  if (!sessionId.value) return '新对话'
  const s = sessions.value.find(item => item.id === sessionId.value)
  return s?.title || '陪伴对话'
})

function newSession() {
  sessionId.value = null
  // Revoke old message image URLs before clearing
  messages.value.forEach((m) => {
    (m.mediaPreviews || []).forEach((url) => URL.revokeObjectURL(url))
  })
  messages.value = []
  clearUploads()
}

async function handleDelete(session) {
  try {
    await ElMessageBox.confirm(
      `确定要删除会话「${session.title || '新对话'}」吗？删除后无法恢复。`,
      '删除确认',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
    await deleteSession(session.id)
    sessions.value = sessions.value.filter(s => s.id !== session.id)
    if (sessionId.value === session.id) {
      newSession()
    }
    ElMessage.success('已删除')
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

async function selectSession(session) {
  sessionId.value = session.id
  try {
    const res = await fetchMessages(session.id)
    messages.value = (res.data || []).map(item => ({ ...item, showRefs: false, refs: [], mediaPreviews: [] }))
  } catch {
    messages.value = []
  }
}

async function loadSessions() {
  try {
    const res = await fetchSessions()
    sessions.value = res.data || []
  } catch {
    sessions.value = []
  }
}

// ── File handling ──

function onFilesPicked(event) {
  const files = Array.from(event.target.files || [])
  event.target.value = '' // reset so same file can be re-picked

  for (const file of files) {
    if (uploadFiles.value.length >= MAX_FILES) {
      ElMessage.warning(`最多同时上传 ${MAX_FILES} 个文件`)
      break
    }
    if (file.size > MAX_FILE_SIZE_MB * 1024 * 1024) {
      ElMessage.warning(`${file.name}: 文件大小超过 ${MAX_FILE_SIZE_MB}MB 限制`)
      continue
    }
    if (!file.type.startsWith('image/') && !file.type.startsWith('video/')) {
      ElMessage.warning(`${file.name}: 不支持的文件类型`)
      continue
    }
    uploadFiles.value.push(file)
    filePreviews.value.push(URL.createObjectURL(file))
  }
}

function removeFile(index) {
  revokeFilePreview(index)
}

function clearUploads() {
  // Do NOT revoke Object URLs here — they are now referenced
  // by the message's mediaPreviews and need to stay alive.
  uploadFiles.value = []
  filePreviews.value = []
}

/** Revoke a single file's preview URL (used when removing before send). */
function revokeFilePreview(index) {
  URL.revokeObjectURL(filePreviews.value[index])
  uploadFiles.value.splice(index, 1)
  filePreviews.value.splice(index, 1)
}

function previewImage(src) {
  imagePreviewSrc.value = src
  imagePreviewVisible.value = true
}

// ── Submit ──

async function submit() {
  const text = input.value.trim()
  const hasFiles = uploadFiles.value.length > 0
  if (!text && !hasFiles) return
  if (sending.value) return

  // Build local message preview
  const localMsg = {
    role: 'USER',
    content: text || '[图片/视频]',
    mediaPreviews: [...filePreviews.value],
  }
  messages.value.push(localMsg)

  const filesToSend = [...uploadFiles.value]
  input.value = ''
  clearUploads()
  sending.value = true
  thinking.value = true

  // 滚动到底部
  await nextTick()
  const listEl = document.querySelector('.message-list')
  if (listEl) listEl.scrollTop = listEl.scrollHeight

  try {
    const res = await sendMessage({
      sessionId: sessionId.value,
      message: text,
      files: filesToSend,
    })
    const data = res.data
    sessionId.value = data.sessionId

    // 取后端返回的 references，组装助手消息
    const refs = data.references || []
    messages.value.push({
      role: 'ASSISTANT',
      content: data.reply,
      showRefs: false,
      refs,
      intent: data.intent,
      riskLevel: data.riskLevel,
    })
    await loadSessions()
  } catch (error) {
    ElMessage.error(error.message || '发送失败，请稍后重试')
  } finally {
    sending.value = false
    thinking.value = false
    await nextTick()
    const listEl = document.querySelector('.message-list')
    if (listEl) listEl.scrollTop = listEl.scrollHeight
  }
}

function logout() {
  auth.logout()
  router.push('/login')
}

onMounted(loadSessions)
</script>

<style scoped>
.empty-hint {
  padding: 20px 0;
  text-align: center;
  font-size: 13px;
  color: var(--warm-400);
}

/* session item 内部布局 */
.session-item {
  position: relative;
}

.session-item .session-icon {
  flex-shrink: 0;
}

.session-item .session-title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-item .session-delete {
  flex-shrink: 0;
  opacity: 0;
  transition: opacity 0.15s;
  color: var(--warm-400);
}

.session-item:hover .session-delete {
  opacity: 1;
}

.session-item .session-delete:hover {
  color: var(--danger);
  background: var(--danger-soft);
}

.sidebar-footer {
  margin-top: auto;
  padding-top: 14px;
  border-top: 1px solid var(--warm-200);
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.sidebar-footer .el-button {
  justify-content: flex-start;
  color: var(--warm-500);
}

.refs-expand {
  margin-top: 4px;
  padding: 0 4px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-width: 100%;
}

.refs-expand-item {
  padding: 10px 14px;
  border: 1px solid var(--warm-200);
  border-radius: var(--radius-sm);
  background: var(--warm-50);
}

.refs-expand-name {
  font-size: 12px;
  font-weight: 600;
  color: var(--green-600);
}

.refs-expand-item p {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--warm-500);
  line-height: 1.6;
}

/* ── Media preview bar (above composer) ── */
.media-preview-bar {
  display: flex;
  gap: 10px;
  padding: 8px 16px 0;
  flex-wrap: wrap;
}

.media-preview-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  background: var(--warm-50);
  border: 1px solid var(--warm-200);
  border-radius: var(--radius-sm);
  max-width: 220px;
}

.media-preview-thumb {
  width: 40px;
  height: 40px;
  object-fit: cover;
  border-radius: 4px;
  flex-shrink: 0;
}

.media-preview-name {
  font-size: 12px;
  color: var(--warm-500);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  min-width: 0;
}

.media-preview-remove {
  flex-shrink: 0;
  color: var(--warm-400);
}
.media-preview-remove:hover {
  color: var(--danger);
  background: var(--danger-soft);
}

/* ── Message images (user messages) ── */
.message-images {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-top: 6px;
}

.message-image-thumb {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 6px;
  border: 1px solid var(--warm-200);
  cursor: pointer;
  transition: transform 0.15s;
}
.message-image-thumb:hover {
  transform: scale(1.05);
  border-color: var(--green-400);
}

/* ── Composer tools (upload buttons) ── */
.composer-tools {
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 0 4px 4px;
}

.composer-tools .el-button {
  color: var(--warm-400);
  font-size: 18px;
}
.composer-tools .el-button:hover {
  color: var(--green-600);
  background: var(--green-50);
}

.upload-count {
  font-size: 12px;
  color: var(--warm-400);
  margin-left: 6px;
}

.composer-input-row {
  display: flex;
  gap: 10px;
  align-items: flex-end;
}
</style>
