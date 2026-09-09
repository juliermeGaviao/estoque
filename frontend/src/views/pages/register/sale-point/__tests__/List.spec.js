import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, h, nextTick } from 'vue'
import List from '../List.vue'

const mockToastAdd = vi.fn()
const mockConfirmRequire = vi.fn()

vi.mock('primevue/usetoast', () => ({
  useToast: () => ({ add: mockToastAdd })
}))

vi.mock('primevue/useconfirm', () => ({
  useConfirm: () => ({ require: mockConfirmRequire })
}))

vi.mock('@/util/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    delete: vi.fn()
  }
}))

import api from '@/util/api'

describe('List.vue - src/views/pages/register/sale-point/List.vue', () => {
  const mockSalePoints = [
    { id: 1, nome: 'Ponto A', empresa: { id: 10, nome: 'Empresa A' } },
    { id: 2, nome: 'Ponto B', empresa: { id: 20, nome: 'Empresa B' } }
  ]

  const mockCompanies = [
    { id: 10, nome: 'Empresa A' },
    { id: 20, nome: 'Empresa B' }
  ]

  beforeEach(() => {
    vi.clearAllMocks()

    api.get.mockImplementation((url) => {
      if (url === '/sale-point/list') {
        return Promise.resolve({
          data: {
            content: JSON.parse(JSON.stringify(mockSalePoints)),
            totalElements: 2
          }
        })
      }
      if (url === '/client/list-companies') {
        return Promise.resolve({
          data: {
            content: JSON.parse(JSON.stringify(mockCompanies)),
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
            render() {
              return h('div', { class: 'column-stub' }, [
                this.$slots.header?.(),
                this.$slots.body?.({ data: { id: 1, nome: 'Ponto A', empresa: { id: 10, nome: 'Empresa A' }, editando: false, edicao: { nome: 'Ponto A', idEmpresa: 10 } } })
              ])
            }
          },
          Button: {
            props: ['label', 'icon', 'disabled'],
            render() {
              return h('button', {
                type: 'button',
                'data-icon': this.icon,
                disabled: this.disabled,
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
          Select: {
            props: ['modelValue'],
            render() {
              return h('select', {
                value: this.modelValue,
                onChange: (e) => this.$emit('update:modelValue', e.target.value)
              })
            }
          },
          Form: defineComponent({
            name: 'Form',
            methods: {
              setValues(vals) { this.internalValues = vals }
            },
            data() { return { internalValues: {} } },
            render() {
              return h('form', {
                onSubmit: (e) => {
                  e.preventDefault()
                  this.$emit('submit', { valid: true, values: this.internalValues })
                },
                onReset: (e) => {
                  e.preventDefault()
                  this.$emit('reset', e)
                }
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

  it('carrega dados e empresas com sucesso no onMounted', async () => {
    const wrapper = mountComponent()
    await nextTick()
    await nextTick()

    expect(api.get).toHaveBeenCalledWith('/sale-point/list', expect.any(Object))
    expect(api.get).toHaveBeenCalledWith('/client/list-companies', expect.any(Object))
    expect(wrapper.vm.data.length).toBe(2)
    expect(wrapper.vm.companies.length).toBe(3) // Inclui o "Nenhuma"
  })

  it('trata erros de API ao carregar lista de pontos de venda e empresas (com e sem response)', async () => {
    api.get.mockRejectedValueOnce({ response: { data: 'Erro load' } })
    api.get.mockRejectedValueOnce(new Error('Erro genérico sem response'))

    mountComponent()
    await nextTick()
    await nextTick()

    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Carga de Pontos de Venda' }))
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Carga de Empresas' }))
  })

  it('filtra e limpa os filtros corretamente', async () => {
    const wrapper = mountComponent()
    await nextTick()

    await wrapper.vm.filter({ valid: true, values: { nome: 'Ponto A' } })
    expect(wrapper.vm.page).toBe(0)
    expect(api.get).toHaveBeenCalledTimes(3) // 2 do onMounted + 1 do filter

    // Teste de filtro inválido
    await wrapper.vm.filter({ valid: false, values: {} })

    // Teste de limpeza
    wrapper.vm.limpar()
    await nextTick()
    await nextTick()
    expect(wrapper.vm.page).toBe(0)
    expect(wrapper.vm.sortField).toBeNull()
  })

  it('executa onPage com sucesso e com salvamento bloqueado', async () => {
    const wrapper = mountComponent()
    await nextTick()

    // Sucesso no saveAll interno do onPage
    api.post.mockResolvedValueOnce({ status: 200 })
    await wrapper.vm.onPage({ page: 1, rows: 40, first: 40 })
    expect(wrapper.vm.page).toBe(1)
    expect(wrapper.vm.size).toBe(40)
    expect(wrapper.vm.first).toBe(40)

    // Falha/bloqueio no saveAll (item em edição sem nome)
    wrapper.vm.data[0].editando = true
    wrapper.vm.data[0].edicao.nome = '   '

    await wrapper.vm.onPage({ page: 2, rows: 20, first: 20 })
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Dados Insuficientes' }))
  })

  it('executa onSort com sucesso, com salvamento bloqueado e com event.originalEvent', async () => {
    const wrapper = mountComponent()
    await nextTick()

    // Sucesso no onSort
    api.post.mockResolvedValueOnce({ status: 200 })
    await wrapper.vm.onSort({ sortField: 'nome', sortOrder: 1 })
    expect(wrapper.vm.sortField).toBe('nome')
    expect(wrapper.vm.sortOrder).toBe(1)

    // Bloqueio no onSort (item editando sem nome) + originalEvent presente
    wrapper.vm.data[0].editando = true
    wrapper.vm.data[0].edicao.nome = ''
    const preventDefault = vi.fn()

    await wrapper.vm.onSort({ sortField: 'nome', sortOrder: 1, originalEvent: { preventDefault } })
    expect(preventDefault).toHaveBeenCalled()

    // Cobre o caso onde sortField era null no ramo alternativo de reset
    wrapper.vm.sortField = null
    wrapper.vm.sortOrder = 0
    await wrapper.vm.onSort({ sortField: 'nome', sortOrder: 1, originalEvent: { preventDefault: () => {} } })
  })

  it('gerencia edição, cancelamento e adição de novos itens', async () => {
    const wrapper = mountComponent()
    await nextTick()

    const item = wrapper.vm.data[0]
    wrapper.vm.edit(item)
    expect(item.editando).toBe(true)
    expect(item.edicao.nome).toBe(item.nome)

    // Cancela edição de item existente
    wrapper.vm.cancel(item)
    expect(item.editando).toBe(false)

    // Adiciona novo item e cancela (deve remover da lista)
    wrapper.vm.addItem()
    const newItem = wrapper.vm.data[wrapper.vm.data.length - 1]
    expect(newItem.editando).toBe(true)
    wrapper.vm.cancel(newItem)
    expect(wrapper.vm.data.includes(newItem)).toBe(false)
  })

  it('consolida (commit) item com sucesso, dados insuficientes e erro de API (com e sem response)', async () => {
    const wrapper = mountComponent()
    await nextTick()

    const item = wrapper.vm.data[0]
    wrapper.vm.edit(item)

    // Dados insuficientes (nome vazio)
    item.edicao.nome = '   '
    await wrapper.vm.commit(item)
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Dados Insuficientes' }))

    // Sucesso na consolidação (atualização de item existente com id)
    item.edicao.nome = 'Ponto A Atualizado'
    item.edicao.idEmpresa = 10
    api.post.mockResolvedValueOnce({ status: 200, data: { id: 1, empresa: { id: 10 } } })
    await wrapper.vm.commit(item)
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Sucesso' }))

    // Erro de API na consolidação (com response)
    item.editando = true
    item.edicao.nome = 'Outro Nome'
    api.post.mockRejectedValueOnce({ response: { data: 'Erro ao consolidar' } })
    await wrapper.vm.commit(item)
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Gravação de Ponto de Venda' }))

    // Erro de API na consolidação (sem response / erro genérico opcional)
    item.editando = true
    item.edicao.nome = 'Outro Nome 2'
    api.post.mockRejectedValueOnce(new Error('Network Error'))
    await wrapper.vm.commit(item)
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Gravação de Ponto de Venda' }))
  })

  it('salva todos (clickAndSaveAll e saveAll) com sucesso e erro', async () => {
    const wrapper = mountComponent()
    await nextTick()

    // Sucesso em clickAndSaveAll
    api.post.mockResolvedValueOnce({ status: 200 })
    await wrapper.vm.clickAndSaveAll()
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Sucesso', detail: 'Pontos de venda salvos com sucesso' }))

    // Erro em saveAll
    api.post.mockRejectedValueOnce({ response: { data: 'Erro save-all' } })
    const res = await wrapper.vm.saveAll(true)
    expect(res).toBe(false)
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Gravação de Ponto de Venda' }))
  })

  it('exclui ponto de venda via confirmDelete com sucesso e tratamento de erro', async () => {
    const wrapper = mountComponent()
    await nextTick()

    const item = wrapper.vm.data[0]
    wrapper.vm.confirmDelete(item)
    expect(mockConfirmRequire).toHaveBeenCalled()

    const confirmArgs = mockConfirmRequire.mock.calls[0][0]

    // Sucesso na exclusão
    api.delete.mockResolvedValueOnce({})
    await confirmArgs.accept()
    expect(api.delete).toHaveBeenCalledWith('/sale-point?id=1')
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Sucesso', detail: 'Ponto de venda removido com sucesso' }))

    // Erro na exclusão
    wrapper.vm.confirmDelete(item)
    api.delete.mockRejectedValueOnce({ response: { data: 'Erro exclusão' } })
    await mockConfirmRequire.mock.calls[1][0].accept()
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Remoção de Ponto de Venda' }))
  })

  it('interage com botões e formulários do template', async () => {
    const wrapper = mountComponent()
    await nextTick()

    const buttons = wrapper.findAll('button')
    for (const btn of buttons) {
      await btn.trigger('click')
    }

    const form = wrapper.find('form')
    if (form.exists()) {
      await form.trigger('submit')
      await form.trigger('reset')
    }

    expect(api.get).toHaveBeenCalled()
  })
})