import http from './http'

export function sendMessage(data) {
  return http.post('/chat/send', data)
}

export function fetchSessions() {
  return http.get('/chat/sessions')
}

export function fetchMessages(sessionId) {
  return http.get(`/chat/sessions/${sessionId}/messages`)
}

