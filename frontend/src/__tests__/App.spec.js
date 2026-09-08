// @vitest-environment jsdom
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import App from '../App.vue'

describe('App.vue', () => {
  it('deve renderizar o componente sem erros', () => {
    const wrapper = mount(App, {
      global: {
        stubs: {
          'router-view': true
        }
      }
    })

    expect(wrapper.exists()).toBe(true)
  })

  it('deve conter o componente router-view', () => {
    const wrapper = mount(App, {
      global: {
        stubs: {
          'router-view': true
        }
      }
    })

    expect(wrapper.findComponent({ name: 'RouterView' }).exists() || wrapper.find('router-view-stub').exists()).toBe(true)
  })
})