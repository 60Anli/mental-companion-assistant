import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import LoginView from '../views/LoginView.vue'
import ChatView from '../views/ChatView.vue'
import AdminView from '../views/AdminView.vue'
import ResumeView from '../views/ResumeView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: LoginView },
    { path: '/', component: ChatView },
    { path: '/admin', component: AdminView },
    { path: '/about', component: ResumeView }
  ]
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (!auth.token && to.path !== '/login' && to.path !== '/about') {
    return '/login'
  }
  if (to.path === '/admin' && auth.role !== 'ADMIN') {
    return '/'
  }
})

export default router

