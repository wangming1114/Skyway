import { createRouter, createWebHashHistory } from 'vue-router'
import { getToken } from '@/utils/auth'
import Layout from '@/components/Layout.vue'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', noAuth: true }
  },
  {
    path: '/',
    component: Layout,
    meta: { requireAuth: true },
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('@/views/Home.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/Profile.vue'),
        meta: { title: '个人中心' }
      },
      {
        path: 'product',
        name: 'Product',
        component: () => import('@/views/Product.vue'),
        meta: { title: '产品中心' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

const appTitle = import.meta.env.VITE_APP_TITLE || 'NetCloud'
router.beforeEach((to, _from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - ${appTitle}` : appTitle
  const token = getToken()
  if (to.meta.noAuth) {
    if (token && to.path === '/login') {
      next({ path: '/' })
    } else {
      next()
    }
  } else if (!token && to.matched.some(r => r.meta.requireAuth)) {
    next({ path: '/login', query: { redirect: to.fullPath } })
  } else {
    next()
  }
})

export default router
