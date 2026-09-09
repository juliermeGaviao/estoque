import * as auth from '@/util/auth'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import router from '../index'

// Stub dos módulos de visualização para evitar carregamento de componentes reais do DOM
vi.mock('@/layout/AppLayout.vue', () => ({ default: { name: 'AppLayout', template: '<div />' } }))
vi.mock('@/views/Home.vue', () => ({ default: { name: 'Home', template: '<div />' } }))
vi.mock('@/views/pages/register/user/List.vue', () => ({ default: { template: '<div />' } }))
vi.mock('@/views/pages/register/user/Insert.vue', () => ({ default: { template: '<div />' } }))
vi.mock('@/views/pages/register/user/Edit.vue', () => ({ default: { template: '<div />' } }))
vi.mock('@/views/pages/stock/purchase-order/List.vue', () => ({ default: { template: '<div />' } }))
vi.mock('@/views/pages/stock/transfer/List.vue', () => ({ default: { template: '<div />' } }))
vi.mock('@/views/pages/register/provider/List.vue', () => ({ default: { template: '<div />' } }))
vi.mock('@/views/pages/register/provider/Edit.vue', () => ({ default: { template: '<div />' } }))
vi.mock('@/views/pages/register/sale-point/List.vue', () => ({ default: { template: '<div />' } }))
vi.mock('@/views/pages/register/company/List.vue', () => ({ default: { template: '<div />' } }))
vi.mock('@/views/pages/register/company/Edit.vue', () => ({ default: { template: '<div />' } }))
vi.mock('@/views/pages/register/person/List.vue', () => ({ default: { template: '<div />' } }))
vi.mock('@/views/pages/register/person/Edit.vue', () => ({ default: { template: '<div />' } }))
vi.mock('@/views/pages/register/product-type/List.vue', () => ({ default: { template: '<div />' } }))
vi.mock('@/views/pages/register/product/List.vue', () => ({ default: { template: '<div />' } }))
vi.mock('@/views/pages/register/price-table/List.vue', () => ({ default: { template: '<div />' } }))
vi.mock('@/views/pages/core/sale/List.vue', () => ({ default: { template: '<div />' } }))
vi.mock('@/views/pages/core/sale/Edit.vue', () => ({ default: { template: '<div />' } }))
vi.mock('@/views/pages/dashboard/Home.vue', () => ({ default: { template: '<div />' } }))
vi.mock('@/views/pages/auth/Login.vue', () => ({ default: { template: '<div />' } }))

describe('Router Config (index.js)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('redireciona para /auth/login ao tentar acessar rota protegida sem autenticação', async () => {
    vi.spyOn(auth, 'isAuthenticated').mockReturnValue(false)

    await router.push('/')
    await router.isReady()

    expect(router.currentRoute.value.path).toBe('/auth/login')
  })

  it('permite o acesso a rota protegida quando o usuário está autenticado', async () => {
    vi.spyOn(auth, 'isAuthenticated').mockReturnValue(true)

    await router.push('/')
    await router.isReady()

    expect(router.currentRoute.value.path).toBe('/')
  })

  it('permite o acesso direto a rota pública de login', async () => {
    vi.spyOn(auth, 'isAuthenticated').mockReturnValue(false)

    await router.push('/auth/login')
    await router.isReady()

    expect(router.currentRoute.value.path).toBe('/auth/login')
  })

  it('executa a importação dinâmica (lazy loading) de todas as rotas declaradas', async () => {
    vi.spyOn(auth, 'isAuthenticated').mockReturnValue(true)

    // Extrai todas as rotas registradas e executa a função de importação dos componentes
    const routes = router.getRoutes()

    for (const route of routes) {
      const componentSupplier = route.components?.default
      if (typeof componentSupplier === 'function') {
        const loadedComponent = await componentSupplier()
        expect(loadedComponent).toBeDefined()
      }
    }
  })
})