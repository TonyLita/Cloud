import { createRouter, createWebHistory } from 'vue-router'
import Login from '../components/Login.vue'
import Accueil from '../components/Accueil.vue'
import RegisterProfile from '../components/RegisterProfile.vue'

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', component: Login },
  { path: '/accueil', component: Accueil },
  { path: '/register', component: RegisterProfile }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
