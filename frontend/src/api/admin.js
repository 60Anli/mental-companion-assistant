import http from './http'

export function uploadKnowledge(file) {
  const form = new FormData()
  form.append('file', file)
  return http.post('/admin/knowledge/upload', form)
}

export function fetchKnowledgeList() {
  return http.get('/admin/knowledge/list')
}

export function fetchWorkflowRecords() {
  return http.get('/admin/workflow-records')
}

export function exportWorkflowExcel() {
  return http.get('/admin/workflow-records/export', { responseType: 'blob' })
}

export function fetchRiskRecords() {
  return http.get('/admin/risk-records')
}

export function fetchMemories() {
  return http.get('/admin/memories')
}

export function fetchModelRuntime() {
  return http.get('/admin/model/runtime')
}

export function sendTestEmail(receiver) {
  return http.post('/admin/email/test', { receiver })
}

export function fetchEmailLogs() {
  return http.get('/admin/email/logs')
}
