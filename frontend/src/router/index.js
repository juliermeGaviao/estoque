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
                },
                {
                    path: '/register/provider',
                    component: () => import('@/views/pages/register/provider/List.vue')
                },
                {
                    path: '/register/provider/edit',
                    component: () => import('@/views/pages/register/provider/Edit.vue')
                },
                {
                    path: '/register/company',
                    component: () => import('@/views/pages/register/company/List.vue')
                },
                {
                    path: '/register/company/edit',
                    component: () => import('@/views/pages/register/company/Edit.vue')
                },
                {
                    path: '/register/person',
                    component: () => import('@/views/pages/register/person/List.vue')
                },
                {
                    path: '/register/person/edit',
                    component: () => import('@/views/pages/register/person/Edit.vue')
                },
                {
                    path: '/register/product-type',
                    component: () => import('@/views/pages/register/product-type/List.vue')
                },
                {
                    path: '/register/product-type/edit',
                    component: () => import('@/views/pages/register/product-type/Edit.vue')
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
