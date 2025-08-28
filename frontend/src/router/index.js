import AppLayout from '@/layout/AppLayout.vue'
import { createRouter, createWebHistory } from 'vue-router'
import { isAuthenticated } from '../util/auth'

const router = createRouter({
    history: createWebHistory(),
    routes: [
        {
            path: '/',
            component: AppLayout,
            meta: { requiresAuth: true },
            children: [
                {
                    path: '/',
                    component: () => import('@/views/Home.vue')
                },
                {
                    path: '/management/user',
                    component: () => import('@/views/pages/management/user/List.vue')
                },
                {
                    path: '/management/user/insert',
                    component: () => import('@/views/pages/management/user/Insert.vue')
                },
                {
                    path: '/management/user/edit',
                    component: () => import('@/views/pages/management/user/Edit.vue')
                }
            ]
        },
        {
            path: '/auth/login',
            component: () => import('@/views/pages/auth/Login.vue')
        }
    ]
})

router.beforeEach((to, from, next) => {
  if (to.meta.requiresAuth && !isAuthenticated()) {
    next('/auth/login')
  } else {
    next()
  }
})

export default router
