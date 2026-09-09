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

vi.mock('@/util/util', () => ({
  formatCpfCnpj: (val) => val ? `CNPJ:${val}` : '',
  formatPhone: (val) => val ? `TEL:${val}` : '',
  onlyDigits: (val) => val ? val.replace(/\D/g, '') : null
}))

vi.mock('@/util/api', () => ({
  default: {
    get: vi.fn(),
    delete: vi.fn()
  }
}))

import api from '@/util/api'

describe('List.vue - src/views/pages/register/provider/List.vue', () => {
  const mockProviders = [
    { id: 1, razaoSocial: 'Fornecedor A LTDA', fantasia: 'Fornecedor A', cnpj: '12345678000195', fone: '11999998888' },
    { id: 2, razaoSocial: 'Fornecedor B SA', fantasia: 'Fornecedor B', cnpj: '98765432000100', fone: '11888887777' }
  ]

  beforeEach(() => {
    vi.clearAllMocks()
    api.get.mockImplementation((url) => {
      if (url === '/provider/list') {
        return Promise.resolve({
          data: {
            content: JSON.parse(JSON.stringify(mockProviders)),
            totalElements: 2
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
          Card: {
            render() {
              return h('div', [this.$slots.title?.(), this.$slots.content?.()])
            }
          },
          DataTable: {
            props: ['value'],
            render() {
              return h('div', [
                this.$slots.default?.(),
                (this.value || []).map((item, index) =>
                  h('div', { key: index, class: 'data-table-row' }, [
                    this.$slots.default?.({ data: item })
                  ])
                )
              ])
            }
          },
          Column: {
            props: ['field'],
            render() {
              return h('div', { class: 'column-stub' }, [
                this.$slots.header?.(),
                this.$slots.body?.({ data: { id: 1, razaoSocial: 'Fornecedor A', fantasia: 'Fantasia A', cnpj: '12345678000195', fone: '11999998888' } })
              ])
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
          InputText: {
            props: ['modelValue'],
            render() {
              return h('input', {
                value: this.modelValue,
                onInput: (e) => this.$emit('update:modelValue', e.target.value)
              })
            }
          },
          InputMask: {
            props: ['modelValue'],
            render() {
              return h('input', {
                value: this.modelValue,
                onInput: (e) => this.$emit('update:modelValue', e.target.value)
              })
            }
          },
          Form: defineComponent({
            name: 'Form',
            render() {
              return h('form', {
                onSubmit: (e) => {
                  e.preventDefault()
                  this.$emit('submit', { valid: true, values: {} })
                },
                onReset: () => this.$emit('reset')
              }, [this.$slots.default?.()])
            }
          }),
          FormField: {
            render() { return h('div', [this.$slots.default?.()]) }
          },
          FloatLabel: {
            render() { return h('div', [this.$slots.default?.()]) }
          },
          ConfirmDialog: true
        },
        directives: {
          tooltip: {}
        }
      }
    })
  }

  it('carrega dados no onMounted com sucesso e aplica sanitize nos filtros', async () => {
    const wrapper = mountComponent()
    await nextTick()
    await nextTick()

    expect(api.get).toHaveBeenCalledWith('/provider/list', {
      params: {
        page: 0,
        size: 20,
        cnpj: null,
        fone: null
      }
    })
    expect(wrapper.vm.data.length).toBe(2)
  })

  it('trata erros de API ao carregar fornecedores no onMounted', async () => {
    api.get.mockRejectedValueOnce({ response: { data: 'Erro de conexao' } })
    mountComponent()
    await nextTick()
    await nextTick()

    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Carga de Fornecedores' }))
  })

  it('filtra fornecedores sanitizando CNPJ e Fone e limpa filtros', async () => {
    const wrapper = mountComponent()
    await nextTick()

    const filterData = {
      valid: true,
      values: {
        razaoSocial: 'Empresa',
        cnpj: '12.345.678/0001-95',
        fone: '(11) 99999-8888'
      }
    }

    await wrapper.vm.filter(filterData)

    expect(wrapper.vm.page).toBe(0)
    expect(api.get).toHaveBeenLastCalledWith('/provider/list', {
      params: expect.objectContaining({
        razaoSocial: 'Empresa',
        cnpj: '12345678000195',
        fone: '11999998888'
      })
    })

    await wrapper.vm.filter({ valid: false, values: {} })

    wrapper.vm.limpar()
    await new Promise((r) => setTimeout(r, 0))
    expect(wrapper.vm.page).toBe(0)
    expect(wrapper.vm.sortField).toBeNull()
  })

  it('executa a paginação (onPage)', async () => {
    const wrapper = mountComponent()
    await nextTick()

    wrapper.vm.onPage({ page: 2, rows: 40 })

    expect(wrapper.vm.page).toBe(2)
    expect(wrapper.vm.size).toBe(40)
    expect(api.get).toHaveBeenLastCalledWith('/provider/list', expect.objectContaining({
      params: expect.objectContaining({ page: 2, size: 40 })
    }))
  })

  it('executa a ordenação (onSort) com ordenação asc, desc e com sortField nulo/ausente cobrindo a linha 32', async () => {
    const wrapper = mountComponent()
    await nextTick()

    // Ordenação ASC
    wrapper.vm.onSort({ sortField: 'razaoSocial', sortOrder: 1 })
    expect(wrapper.vm.page).toBe(0)
    expect(api.get).toHaveBeenLastCalledWith('/provider/list', {
      params: expect.objectContaining({ sort: 'razaoSocial,asc' })
    })

    // Ordenação DESC
    wrapper.vm.onSort({ sortField: 'razaoSocial', sortOrder: -1 })
    expect(api.get).toHaveBeenLastCalledWith('/provider/list', {
      params: expect.objectContaining({ sort: 'razaoSocial,desc' })
    })

    // Cobertura total da linha 32 (quando sortField é nulo ou ausente)
    wrapper.vm.onSort({ sortField: null, sortOrder: null })
    expect(api.get).toHaveBeenLastCalledWith('/provider/list', {
      params: expect.not.objectContaining({ sort: expect.any(String) })
    })
  })

  it('navega para criação ou edição de fornecedor ao chamar edit', async () => {
    const wrapper = mountComponent()
    await nextTick()

    wrapper.vm.edit({ id: 10 })
    expect(mockRouterPush).toHaveBeenCalledWith('/register/provider/edit?id=10')

    wrapper.vm.edit(null)
    expect(mockRouterPush).toHaveBeenCalledWith('/register/provider/edit')
  })

  it('exclui fornecedor via confirmDelete com sucesso e tratamento de erro', async () => {
    const wrapper = mountComponent()
    await nextTick()

    const item = wrapper.vm.data[0]
    wrapper.vm.confirmDelete(item)
    expect(mockConfirmRequire).toHaveBeenCalled()

    const confirmArgs = mockConfirmRequire.mock.calls[0][0]

    api.delete.mockResolvedValueOnce({})
    await confirmArgs.accept()
    expect(api.delete).toHaveBeenCalledWith('/provider?id=1')
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ severity: 'success' }))

    wrapper.vm.confirmDelete(item)
    api.delete.mockRejectedValueOnce({ response: { data: 'Erro ao remover' } })
    await mockConfirmRequire.mock.calls[1][0].accept()
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Remoção de Usuário' }))
  })

  it('interage com botões do template e envia os formulários', async () => {
    const wrapper = mountComponent()
    await nextTick()

    const form = wrapper.find('form')
    await form.trigger('submit')
    await form.trigger('reset')

    const addBtn = wrapper.find('button[data-icon="pi pi-plus"]')
    if (addBtn.exists()) await addBtn.trigger('click')

    const pencilBtn = wrapper.find('button[data-icon="pi pi-pencil"]')
    if (pencilBtn.exists()) await pencilBtn.trigger('click')

    const trashBtn = wrapper.find('button[data-icon="pi pi-trash"]')
    if (trashBtn.exists()) await trashBtn.trigger('click')

    expect(mockRouterPush).toHaveBeenCalled()
  })

  it('executa a ordenação com sortField presente mas sortOrder nulo', async () => {
    const wrapper = mountComponent()
    await nextTick()

    wrapper.vm.onSort({ sortField: 'razaoSocial', sortOrder: null })
    expect(api.get).toHaveBeenLastCalledWith('/provider/list', {
      params: expect.objectContaining({ sort: 'razaoSocial' })
    })
  })

})
