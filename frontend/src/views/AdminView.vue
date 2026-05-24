<template>
  <div class="admin-page">
    <aside class="sidebar">
      <div class="brand-row">
        <div>
          <p class="eyebrow">Admin Console</p>
          <h2>管理后台</h2>
        </div>
      </div>
      <div class="admin-actions">
        <el-button :icon="Back" @click="router.push('/')">返回聊天</el-button>
        <el-button :icon="Refresh" @click="loadAll">刷新数据</el-button>
      </div>
    </aside>

    <main class="admin-main">
      <section class="admin-section">
        <div class="section-head">
          <div>
            <p class="eyebrow">Overview</p>
            <h1>运行概览</h1>
          </div>
        </div>
        <div class="admin-kpi-grid">
          <div class="kpi-card">
            <span>注册用户</span>
            <strong>{{ users.length }}</strong>
          </div>
          <div class="kpi-card">
            <span>知识文档</span>
            <strong>{{ knowledge.length }}</strong>
          </div>
          <div class="kpi-card">
            <span>工作流记录</span>
            <strong>{{ workflowRecords.length }}</strong>
          </div>
          <div class="kpi-card danger">
            <span>高风险记录</span>
            <strong>{{ highRiskCount }}</strong>
          </div>
          <div class="kpi-card">
            <span>长期记忆</span>
            <strong>{{ memories.length }}</strong>
          </div>
          <div class="kpi-card">
            <span>邮件成功</span>
            <strong>{{ emailSuccessCount }}</strong>
          </div>
        </div>
      </section>

      <el-tabs v-model="activeTab" class="admin-tabs">
        <el-tab-pane label="用户信息" name="users">
          <section class="admin-section">
            <div class="section-head">
              <h1>学生与管理员账号</h1>
            </div>
            <el-table :data="users" height="420">
              <el-table-column prop="id" label="ID" width="70" />
              <el-table-column prop="username" label="用户名" width="140" />
              <el-table-column prop="realName" label="姓名" width="120" />
              <el-table-column prop="college" label="学院" min-width="180" show-overflow-tooltip />
              <el-table-column prop="department" label="部门" min-width="180" show-overflow-tooltip />
              <el-table-column prop="email" label="邮箱" min-width="220" show-overflow-tooltip />
              <el-table-column label="角色" width="100">
                <template #default="{ row }">
                  <el-tag :type="row.role === 'ADMIN' ? 'warning' : 'success'" effect="plain">{{ row.role }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createTime" label="创建时间" width="180" />
            </el-table>
          </section>
        </el-tab-pane>

        <el-tab-pane label="知识库" name="knowledge">
          <section class="admin-section">
            <div class="section-head">
              <h1>知识库文档</h1>
              <el-upload :show-file-list="false" :before-upload="handleUpload" accept=".txt,.md">
                <el-button type="primary" :icon="Upload">上传文档</el-button>
              </el-upload>
            </div>
            <el-table :data="knowledge" height="420">
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column prop="fileName" label="文件名" min-width="260" show-overflow-tooltip />
              <el-table-column prop="chunkCount" label="切片数" width="100" />
              <el-table-column prop="createTime" label="创建时间" width="190" />
            </el-table>
          </section>
        </el-tab-pane>

        <el-tab-pane label="工作流记录" name="workflow">
          <section class="admin-section">
            <div class="section-head">
              <h1>工作流记录</h1>
              <div class="admin-actions inline">
                <el-select v-model="workflowIntentFilter" clearable placeholder="意图筛选" style="width: 140px">
                  <el-option label="CHAT" value="CHAT" />
                  <el-option label="CONSULT" value="CONSULT" />
                  <el-option label="KNOWLEDGE" value="KNOWLEDGE" />
                  <el-option label="HIGH_RISK" value="HIGH_RISK" />
                </el-select>
                <el-button type="success" :icon="Download" @click="exportExcel">导出 Excel</el-button>
              </div>
            </div>
            <el-table :data="filteredWorkflowRecords" height="460">
              <el-table-column prop="id" label="ID" width="70" />
              <el-table-column prop="userId" label="用户ID" width="90" />
              <el-table-column prop="userMessage" label="用户问题" min-width="260" show-overflow-tooltip />
              <el-table-column label="意图" width="120">
                <template #default="{ row }">
                  <el-tag effect="plain">{{ row.intent }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="风险" width="110">
                <template #default="{ row }">
                  <el-tag :type="riskTagType(row.riskLevel)" effect="plain">{{ row.riskLevel }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="riskType" label="风险类型" width="130" />
              <el-table-column label="RAG" width="80">
                <template #default="{ row }">
                  <el-tag :type="row.ragHit ? 'success' : 'info'" effect="plain">{{ row.ragHit ? '命中' : '无' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="Excel" width="90">
                <template #default="{ row }">
                  <el-tag :type="row.excelExported ? 'success' : 'info'" effect="plain">{{ row.excelExported ? '已写入' : '未写入' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="邮件" width="90">
                <template #default="{ row }">
                  <el-tag :type="row.emailSent ? 'success' : 'info'" effect="plain">{{ row.emailSent ? '已发送' : '未发送' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createTime" label="创建时间" width="180" />
              <el-table-column label="详情" width="90" fixed="right">
                <template #default="{ row }">
                  <el-button text type="primary" @click="openWorkflowDetail(row)">查看</el-button>
                </template>
              </el-table-column>
            </el-table>
          </section>
        </el-tab-pane>

        <el-tab-pane label="风险记录" name="risk">
          <section class="admin-section">
            <div class="section-head">
              <h1>高风险记录</h1>
              <el-select v-model="riskLevelFilter" clearable placeholder="风险等级" style="width: 140px">
                <el-option label="LOW" value="LOW" />
                <el-option label="MEDIUM" value="MEDIUM" />
                <el-option label="HIGH" value="HIGH" />
              </el-select>
            </div>
            <el-table :data="filteredRiskRecords" height="420">
              <el-table-column prop="id" label="ID" width="70" />
              <el-table-column prop="userId" label="用户ID" width="90" />
              <el-table-column prop="userMessage" label="用户内容" min-width="280" show-overflow-tooltip />
              <el-table-column prop="riskType" label="风险类型" width="130" />
              <el-table-column label="风险等级" width="110">
                <template #default="{ row }">
                  <el-tag :type="riskTagType(row.riskLevel)" effect="plain">{{ row.riskLevel }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="处理状态" width="110">
                <template #default="{ row }">
                  <el-tag :type="row.handled ? 'success' : 'danger'" effect="plain">{{ row.handled ? '已处理' : '待关注' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createTime" label="创建时间" width="180" />
            </el-table>
          </section>
        </el-tab-pane>

        <el-tab-pane label="长期记忆" name="memory">
          <section class="admin-section">
            <div class="section-head">
              <h1>长期记忆摘要</h1>
            </div>
            <el-table :data="memories" height="420">
              <el-table-column prop="id" label="ID" width="70" />
              <el-table-column prop="userId" label="用户ID" width="90" />
              <el-table-column prop="memoryType" label="类型" width="150" />
              <el-table-column prop="content" label="记忆内容" min-width="320" show-overflow-tooltip />
              <el-table-column prop="importance" label="重要度" width="90" />
              <el-table-column prop="updateTime" label="更新时间" width="190" />
            </el-table>
          </section>
        </el-tab-pane>

        <el-tab-pane label="邮件日志" name="email">
          <section class="admin-section">
            <div class="section-head">
              <h1>邮件预警日志</h1>
              <el-button :icon="Message" @click="testEmail">测试邮件</el-button>
            </div>
            <el-table :data="emailLogs" height="420">
              <el-table-column prop="id" label="ID" width="70" />
              <el-table-column prop="receiver" label="收件人" min-width="220" show-overflow-tooltip />
              <el-table-column prop="subject" label="主题" min-width="240" show-overflow-tooltip />
              <el-table-column label="状态" width="110">
                <template #default="{ row }">
                  <el-tag :type="emailStatusType(row.sendStatus)" effect="plain">{{ row.sendStatus }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="errorMessage" label="错误信息" min-width="240" show-overflow-tooltip />
              <el-table-column prop="createTime" label="时间" width="180" />
            </el-table>
          </section>
        </el-tab-pane>

        <el-tab-pane label="模型状态" name="model">
          <section class="admin-section">
            <div class="section-head">
              <h1>模型运行状态</h1>
            </div>
            <div class="model-grid">
              <span>模型提供方</span><strong>{{ modelRuntime.provider || '-' }}</strong>
              <span>聊天模型</span><strong>{{ modelRuntime.chatModel || '-' }}</strong>
              <span>嵌入模型</span><strong>{{ modelRuntime.embeddingModel || '-' }}</strong>
              <span>微调接入</span><strong>{{ modelRuntime.fineTuneEnabled ? '已启用' : '未启用' }}</strong>
              <span>基础模型</span><strong>{{ modelRuntime.fineTuneBaseModel || '-' }}</strong>
              <span>适配器类型</span><strong>{{ modelRuntime.adapterType || '-' }}</strong>
              <span>Ollama 微调模型</span><strong>{{ modelRuntime.ollamaModel || '-' }}</strong>
              <span>训练配置</span><strong>{{ modelRuntime.trainingProfile || '-' }}</strong>
            </div>
          </section>
        </el-tab-pane>
      </el-tabs>
    </main>

    <el-drawer v-model="workflowDrawerVisible" title="工作流详情" size="48%">
      <div v-if="selectedWorkflow" class="drawer-detail">
        <section class="drawer-block">
          <h3>用户问题</h3>
          <p>{{ selectedWorkflow.userMessage }}</p>
        </section>
        <section class="drawer-block">
          <h3>AI 回复</h3>
          <p>{{ selectedWorkflow.aiReply }}</p>
        </section>
        <section class="drawer-block">
          <h3>RAG 参考片段</h3>
          <p>{{ selectedWorkflow.ragReferences || '无' }}</p>
        </section>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Back, Download, Message, Refresh, Upload } from '@element-plus/icons-vue'
import {
  fetchEmailLogs,
  fetchKnowledgeList,
  fetchMemories,
  fetchModelRuntime,
  fetchRiskRecords,
  fetchUsers,
  fetchWorkflowRecords,
  exportWorkflowExcel,
  sendTestEmail,
  uploadKnowledge
} from '../api/admin'

const router = useRouter()
const activeTab = ref('users')
const users = ref([])
const knowledge = ref([])
const workflowRecords = ref([])
const riskRecords = ref([])
const emailLogs = ref([])
const memories = ref([])
const modelRuntime = ref({})
const workflowIntentFilter = ref('')
const riskLevelFilter = ref('')
const workflowDrawerVisible = ref(false)
const selectedWorkflow = ref(null)

const highRiskCount = computed(() => riskRecords.value.filter((item) => item.riskLevel === 'HIGH').length)
const emailSuccessCount = computed(() => emailLogs.value.filter((item) => item.sendStatus === 'SUCCESS').length)
const filteredWorkflowRecords = computed(() => {
  if (!workflowIntentFilter.value) return workflowRecords.value
  return workflowRecords.value.filter((item) => item.intent === workflowIntentFilter.value)
})
const filteredRiskRecords = computed(() => {
  if (!riskLevelFilter.value) return riskRecords.value
  return riskRecords.value.filter((item) => item.riskLevel === riskLevelFilter.value)
})

async function loadAll() {
  const [userRes, knowledgeRes, workflowRes, riskRes, emailRes, memoryRes, modelRes] = await Promise.all([
    fetchUsers(),
    fetchKnowledgeList(),
    fetchWorkflowRecords(),
    fetchRiskRecords(),
    fetchEmailLogs(),
    fetchMemories(),
    fetchModelRuntime()
  ])
  users.value = userRes.data
  knowledge.value = knowledgeRes.data
  workflowRecords.value = workflowRes.data
  riskRecords.value = riskRes.data
  emailLogs.value = emailRes.data
  memories.value = memoryRes.data
  modelRuntime.value = modelRes.data
}

async function handleUpload(file) {
  try {
    await uploadKnowledge(file)
    ElMessage.success('上传成功')
    await loadAll()
  } catch (error) {
    ElMessage.error(error.message)
  }
  return false
}

async function exportExcel() {
  try {
    const blob = await exportWorkflowExcel()
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `workflow-records-${Date.now()}.xlsx`
    link.click()
    URL.revokeObjectURL(url)
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function testEmail() {
  try {
    const { value } = await ElMessageBox.prompt('输入测试邮件收件人，留空则使用配置中的预警邮箱', '测试邮件', {
      confirmButtonText: '发送',
      cancelButtonText: '取消',
      inputPlaceholder: 'receiver@example.com'
    })
    await sendTestEmail(value || '')
    ElMessage.success('邮件测试已执行，请查看邮件日志')
    await loadAll()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '测试邮件发送失败')
    }
  }
}

function openWorkflowDetail(row) {
  selectedWorkflow.value = row
  workflowDrawerVisible.value = true
}

function riskTagType(level) {
  if (level === 'HIGH') return 'danger'
  if (level === 'MEDIUM') return 'warning'
  return 'success'
}

function emailStatusType(status) {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'danger'
  return 'info'
}

onMounted(loadAll)
</script>
