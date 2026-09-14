import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, h, nextTick } from 'vue'
import List from '../List.vue'

const mockToastAdd = vi.fn()
const mockConfirmRequire = vi.fn()
const mockRouterPush = vi.fn()

vi.mock('primevue/usetoast', () => ({
  useToast: () => ({ add: mockToastAdd })
}))

vi.mock('primevue/useconfirm', () => ({
  useConfirm: () => ({ require: mockConfirmRequire })
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: mockRouterPush })
}))

vi.mock('@/util/api', () => ({
  default: {
    get: vi.fn(),
    delete: vi.fn()
  }
}))

import api from '@/util/api'

describe('List.vue - src/views/pages/register/user/List.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()

    api.get.mockImplementation((url) => {
      if (url === '/user/list') {
        return Promise.resolve({
          data: {
            content: [
              { id: 1, email: 'user1@test.com', perfis: 'Admin', tabelas: 'T1', pontos: 'P1' }
            ],
            totalElements: 1
          }
        })
      }
      return Promise.resolve({ data: {} })
    })
  })

  function mountComponent() {
    return mount(List, {
      global: {
        stubs: {
          ConfirmDialog: { render() { return h('div') } },
          Card: {
            render() {
              return h('div', [this.$slots.title?.(), this.$slots.content?.()])
            }
          },
          Form: defineComponent({
            name: 'Form',
            render() {
              return h('form', {
                onSubmit: (e) => { e.preventDefault(); this.$emit('submit', e) },
                onReset: (e) => { e.preventDefault(); this.$emit('reset', e) }
              }, [this.$slots.default?.()])
            }
          }),
          FloatLabel: { render() { return h('div', [this.$slots.default?.()]) } },
          InputText: {
            props: ['modelValue'],
            render() {
              return h('input', {
                value: this.modelValue,
                onInput: (e) => this.$emit('update:modelValue', e.target.value)
              })
            }
          },
          Button: {
            props: ['label', 'icon'],
            render() {
              return h('button', {
                type: 'button',
                'data-icon': this.icon,
                onClick: (e) => this.$emit('click', e)
              }, [this.label, this.$slots.default?.()])
            }
          },
          DataTable: defineComponent({
            name: 'DataTable',
            props: ['value'],
            render() {
              return h('div', [this.$slots.default?.()])
            }
          }),
          Column: defineComponent({
            name: 'Column',
            render() {
              const parentTable = this.$parent?.$props?.value
              const items = (parentTable && parentTable.length > 0) 
                ? parentTable 
                : [{ id: 1, email: 'user1@test.com', perfis: 'Admin', tabelas: 'T1', pontos: 'P1' }]
              return h('div', [
                this.$slots.header?.(),
                items.map((item, index) => h('div', { key: index }, [
                  this.$slots.body?.({ data: item })
                ]))
              ])
            }
          })
        },
        directives: {
          tooltip: {}
        }
      }
    })
  }

  it('carrega lista de usuários com sucesso no onMounted', async () => {
    mountComponent()
    await nextTick()
    await nextTick()

    expect(api.get).toHaveBeenCalledWith('/user/list', {
      params: { page: 0, size: 20 }
    })
  })

  it('trata erro ao carregar lista de usuários', async () => {
    api.get.mockRejectedValueOnce({ response: { data: 'Erro na API' } })
    mountComponent()
    await nextTick()
    await nextTick()

    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Carga de Usuários' }))
  })

  it('executa onPage corretamente', async () => {
    const wrapper = mountComponent()
    await nextTick()

    wrapper.vm.onPage({ page: 2, rows: 40 })
    await nextTick()

    expect(api.get).toHaveBeenCalledWith('/user/list', {
      params: { page: 2, size: 40 }
    })
  })

  it('executa onSort com sortOrder 1 (asc), diferente de 1 (desc) e sem sortOrder (null)', async () => {
    const wrapper = mountComponent()
    await nextTick()

    wrapper.vm.onSort({ sortField: 'email', sortOrder: 1 })
    await nextTick()
    expect(api.get).toHaveBeenCalledWith('/user/list', {
      params: { page: 0, size: 20, sort: 'email,asc' }
    })

    wrapper.vm.onSort({ sortField: 'email', sortOrder: -1 })
    await nextTick()
    expect(api.get).toHaveBeenCalledWith('/user/list', {
      params: { page: 0, size: 20, sort: 'email,desc' }
    })

    wrapper.vm.sortField = 'email'
    wrapper.vm.sortOrder = null
    await wrapper.vm.load()
    expect(api.get).toHaveBeenCalledWith('/user/list', {
      params: { page: 0, size: 20, sort: 'email' }
    })
  })

  it('executa onFilter e onClear com email preenchido', async () => {
    const wrapper = mountComponent()
    await nextTick()

    wrapper.vm.email = 'test@test.com'
    wrapper.vm.onFilter()
    await nextTick()

    expect(api.get).toHaveBeenCalledWith('/user/list', {
      params: { page: 0, size: 20, email: 'test@test.com' }
    })

    wrapper.vm.onClear()
    await nextTick()
    expect(wrapper.vm.email).toBeNull()
    expect(api.get).toHaveBeenCalledWith('/user/list', {
      params: { page: 0, size: 20 }
    })
  })

  it('executa edit com usuário válido e nulo', async () => {
    const wrapper = mountComponent()
    await nextTick()

    wrapper.vm.edit({ id: 123 })
    expect(mockRouterPush).toHaveBeenCalledWith('/register/user/edit?id=123')

    wrapper.vm.edit(null)
    expect(mockRouterPush).toHaveBeenCalledWith('/register/user/insert')
  })

  it('executa confirmDelete com sucesso e com erro na remoção', async () => {
    const wrapper = mountComponent()
    await nextTick()

    wrapper.vm.confirmDelete({ id: 1 })
    expect(mockConfirmRequire).toHaveBeenCalled()

    const confirmConfig = mockConfirmRequire.mock.calls[0][0]

    api.delete.mockResolvedValueOnce({})
    await confirmConfig.accept()
    expect(api.delete).toHaveBeenCalledWith('/user?id=1')
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Sucesso' }))

    api.delete.mockRejectedValueOnce({ response: { data: 'Erro ao remover' } })
    await confirmConfig.accept()
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Remoção de Usuário' }))
  })

  it('interage com os botões e formulário do template, incluindo slots de cabeçalho e corpo', async () => {
    const wrapper = mountComponent()
    await nextTick()
    await nextTick()

    const form = wrapper.find('form')
    await form.trigger('submit')
    await form.trigger('reset')

    const buttons = wrapper.findAll('button')
    const plusButton = buttons.find(b => b.attributes('data-icon') === 'pi pi-plus')
    if (plusButton) await plusButton.trigger('click')

    const pencilButton = buttons.find(b => b.attributes('data-icon') === 'pi pi-pencil')
    if (pencilButton) await pencilButton.trigger('click')

    const trashButton = buttons.find(b => b.attributes('data-icon') === 'pi pi-trash')
    if (trashButton) await trashButton.trigger('click')

    expect(mockRouterPush).toHaveBeenCalled()
    expect(mockConfirmRequire).toHaveBeenCalled()
  })

  it('atualiza o v-model de email ao digitar no campo (cobre a linha 123 e o handler)', async () => {
    const wrapper = mountComponent()
    await nextTick()
    const input = wrapper.find('input')
    await input.setValue('busca@teste.com')
    expect(wrapper.vm.email).toBe('busca@teste.com')
  })
})