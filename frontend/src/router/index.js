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
                    name: 'home',
                    component: () => import('@/views/Home.vue')
                },
                {
                    path: '/management/user',
                    name: 'userCRUD',
                    component: () => import('@/views/pages/management/user/List.vue')
                },
                {
                    path: '/management/user/insert',
                    name: 'userInsert',
                    component: () => import('@/views/pages/management/user/Insert.vue')
                },
                {
                    path: '/management/user/edit',
                    name: 'userEdit',
                    component: () => import('@/views/pages/management/user/Edit.vue')
                }
            ]
        },
        {
            path: '/auth/login',
            name: 'login',
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
