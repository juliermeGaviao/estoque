import SaleComponent from '@/components/SaleComponent.vue'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import Edit from '../Edit.vue'

const mockRoute = {
  query: {}
}

vi.mock('vue-router', () => ({
  useRoute: () => mockRoute
}))

describe('sale/Edit.vue', () => {
  beforeEach(() => {
    mockRoute.query = {}
  })

  function mountComponent() {
    return mount(Edit, {
      global: {
        stubs: {
          SaleComponent: {
            name: 'SaleComponent',
            props: ['id', 'backEndpoint'],
            template: '<div class="sale-component-stub" />'
          }
        }
      }
    })
  }

  it('passa o parâmetro id da query de rota para o SaleComponent quando presente', () => {
    mockRoute.query = { id: '123' }

    const wrapper = mountComponent()
    const saleComponent = wrapper.findComponent(SaleComponent)

    expect(saleComponent.exists()).toBe(true)
    expect(saleComponent.props('id')).toBe('123')
    expect(saleComponent.props('backEndpoint')).toBe('/core/sale')
  })

  it('passa id como undefined para o SaleComponent quando não houver id na query', () => {
    mockRoute.query = {}

    const wrapper = mountComponent()
    const saleComponent = wrapper.findComponent(SaleComponent)

    expect(saleComponent.exists()).toBe(true)
    expect(saleComponent.props('id')).toBeUndefined()
    expect(saleComponent.props('backEndpoint')).toBe('/core/sale')
  })
})