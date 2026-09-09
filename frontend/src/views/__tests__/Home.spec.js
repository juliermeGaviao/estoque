import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import Home from '../Home.vue'

describe('Home.vue', () => {
  it('renderiza o componente Home e o SaleComponent stubbed', () => {
    const wrapper = mount(Home, {
      global: {
        stubs: {
          SaleComponent: true
        }
      }
    })

    expect(wrapper.exists()).toBe(true)
    expect(wrapper.findComponent({ name: 'SaleComponent' }).exists()).toBe(true)
  })
})