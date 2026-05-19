<template>
  <div class="admin-page">
    <aside class="sidebar">
      <div class="brand-row">
        <div>
          <p class="eyebrow">Admin</p>
          <h2>工作流后台</h2>
        </div>
      </div>
      <el-button :icon="Back" @click="router.push('/')">返回聊天</el-button>
      <el-button :icon="Refresh" @click="loadAll">刷新</el-button>
    </aside>

    <main class="admin-main">
      <section class="admin-section">
        <div class="section-head">
          <h1>知识库</h1>
          <el-upload :show-file-list="false" :before-upload="handleUpload" accept=".txt,.md">
            <el-button type="primary" :icon="Upload">上传</el-button>
          </el-upload>
        </div>
        <el-table :data="knowledge" height="220">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="fileName" label="文件名" />
          <el-table-column prop="chunkCount" label="切片数" width="100" />
          <el-table-column prop="createTime" label="创建时间" width="190" />
        </el-table>
      </section>

      <section class="admin-section">
        <div class="section-head">
          <h1>工作流记录</h1>
          <el-button type="success" :icon="Download" @click="exportExcel">导出 Excel</el-button>
        </div>
        <el-table :data="workflowRecords" height="300">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="userMessage" label="用户问题" min-width="240" show-overflow-tooltip />
          <el-table-column prop="intent" label="意图" width="110" />
          <el-table-column prop="riskLevel" label="风险" width="100" />
          <el-table-column prop="riskType" label="类型" width="120" />
          <el-table-column prop="ragHit" label="RAG" width="80" />
          <el-table-column prop="excelExported" label="Excel" width="90" />
          <el-table-column prop="emailSent" label="邮件" width="90" />
          <el-table-column prop="createTime" label="创建时间" width="190" />
        </el-table>
      </section>

      <section class="admin-section">
        <div class="section-head">
          <h1>长期记忆</h1>
        </div>
        <el-table :data="memories" height="260">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="userId" label="用户ID" width="90" />
          <el-table-column prop="memoryType" label="类型" width="140" />
          <el-table-column prop="content" label="记忆内容" min-width="280" show-overflow-tooltip />
          <el-table-column prop="importance" label="重要度" width="90" />
          <el-table-column prop="updateTime" label="更新时间" width="190" />
        </el-table>
      </section>

      <section class="admin-section two-col">
        <div>
          <div class="section-head">
            <h1>风险记录</h1>
          </div>
          <el-table :data="riskRecords" height="260">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="userMessage" label="内容" show-overflow-tooltip />
            <el-table-column prop="riskLevel" label="等级" width="90" />
            <el-table-column prop="handled" label="已处理" width="90" />
          </el-table>
        </div>
        <div>
          <div class="section-head">
            <h1>邮件日志</h1>
            <el-button :icon="Message" @click="testEmail">测试</el-button>
          </div>
          <el-table :data="emailLogs" height="260">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="receiver" label="收件人" />
            <el-table-column prop="sendStatus" label="状态" width="100" />
            <el-table-column prop="createTime" label="时间" width="180" />
          </el-table>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Back, Download, Message, Refresh, Upload } from '@element-plus/icons-vue'
import {
  fetchEmailLogs,
  fetchKnowledgeList,
  fetchMemories,
  fetchRiskRecords,
  fetchWorkflowRecords,
  exportWorkflowExcel,
  sendTestEmail,
  uploadKnowledge
} from '../api/admin'

const router = useRouter()
const knowledge = ref([])
const workflowRecords = ref([])
const riskRecords = ref([])
const emailLogs = ref([])
const memories = ref([])

async function loadAll() {
  const [knowledgeRes, workflowRes, riskRes, emailRes, memoryRes] = await Promise.all([
    fetchKnowledgeList(),
    fetchWorkflowRecords(),
    fetchRiskRecords(),
    fetchEmailLogs(),
    fetchMemories()
  ])
  knowledge.value = knowledgeRes.data
  workflowRecords.value = workflowRes.data
  riskRecords.value = riskRes.data
  emailLogs.value = emailRes.data
  memories.value = memoryRes.data
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
    await sendTestEmail('')
    ElMessage.success('已写入邮件测试日志')
    await loadAll()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

onMounted(loadAll)
</script>
