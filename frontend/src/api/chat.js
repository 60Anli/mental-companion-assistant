import http from './http'

/**
 * Send a chat message with optional media files.
 * @param {{ sessionId: number|null, message: string, files: File[] }} params
 * @returns {Promise<object>}
 */
export function sendMessage({ sessionId, message, files }) {
  const formData = new FormData()
  if (sessionId) {
    formData.append('sessionId', sessionId)
  }
  formData.append('message', message || '')
  if (files && files.length > 0) {
    files.forEach((file) => formData.append('files', file))
  }
  return http.post('/chat/send', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function fetchSessions() {
  return http.get('/chat/sessions')
}

export function fetchMessages(sessionId) {
  return http.get(`/chat/sessions/${sessionId}/messages`)
}

export function deleteSession(sessionId) {
  return http.delete(`/chat/sessions/${sessionId}`)
}

