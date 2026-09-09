import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import AppFooter from '../AppFooter.vue'

describe('AppFooter.vue', () => {
  it('renderiza o componente e exibe o texto e link corretos', () => {
    const wrapper = mount(AppFooter)

    // Valida se o contêiner principal foi renderizado
    expect(wrapper.find('.layout-footer').exists()).toBe(true)

    // Valida o texto contido no componente
    expect(wrapper.text()).toContain('Estoque por')
    expect(wrapper.text()).toContain('Dinâmica Tecnologia Informática Ltda.')

    // Valida as propriedades do link <a>
    const link = wrapper.find('a')
    expect(link.exists()).toBe(true)
    expect(link.attributes('href')).toBe('https://primevue.org')
    expect(link.attributes('target')).toBe('_blank')
    expect(link.attributes('rel')).toBe('noopener noreferrer')
    expect(link.classes()).toContain('text-primary')
    expect(link.classes()).toContain('font-bold')
  })
})