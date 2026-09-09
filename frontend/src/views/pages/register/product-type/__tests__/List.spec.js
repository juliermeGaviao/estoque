import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, nextTick } from 'vue'
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

describe('List.vue - src/views/pages/register/product-type/List.vue', () => {
  const mockTypeList = [
    { id: 1, nome: 'Eletrônicos' },
    { id: 2, nome: 'Acessórios' }
  ]

  beforeEach(() => {
    vi.clearAllMocks()
    api.get.mockImplementation((url) => {
      if (url === '/product-type/list') {
        return Promise.resolve({
          data: {
            content: JSON.parse(JSON.stringify(mockTypeList)),
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
          Card: { template: '<div><slot name="title" /><slot name="content" /></div>' },
          DataTable: {
            props: ['value', 'first', 'sortField', 'sortOrder'],
            template: `
              <div>
                <slot />
                <div v-for="(item, index) in value" :key="index" class="data-table-row">
                  <slot name="default" :data="item" />
                </div>
              </div>
            `
          },
          Column: {
            props: ['field'],
            template: `
              <div class="column-stub">
                <slot name="header" />
              </div>
            `
          },
          Button: {
            props: ['label', 'icon'],
            template: '<button type="button" :data-icon="icon" @click="$emit(\'click\', $event)">{{ label }}<slot /></button>'
          },
          InputText: {
            props: ['modelValue'],
            template: '<input :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />'
          },
          Form: defineComponent({
            name: 'Form',
            template: '<form @submit.prevent="$emit(\'submit\', { valid: true, values: {} })" @reset="$emit(\'reset\')"><slot /></form>'
          }),
          FormField: { template: '<div><slot /></div>' },
          FloatLabel: { template: '<div><slot /></div>' },
          ConfirmDialog: true
        },
        directives: {
          tooltip: {}
        }
      }
    })
  }

  it('carrega dados no onMounted com sucesso', async () => {
    const wrapper = mountComponent()
    await nextTick()
    await nextTick()

    expect(api.get).toHaveBeenCalledWith('/product-type/list', expect.any(Object))
    expect(wrapper.vm.data.length).toBe(2)
  })

  it('trata erros de API ao carregar a lista no onMounted', async () => {
    api.get.mockRejectedValueOnce({ response: { data: 'Erro na API' } })
    mountComponent()
    await nextTick()
    await nextTick()

    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Carga de Tipos de Produtos' }))
  })

  it('filtra tipos de produtos e limpa os campos de busca', async () => {
    const wrapper = mountComponent()
    await nextTick()

    const filterData = { valid: true, values: { nome: 'Eletrônicos' } }
    await wrapper.vm.filter(filterData)

    expect(wrapper.vm.page).toBe(0)
    expect(api.get).toHaveBeenLastCalledWith('/product-type/list', { params: expect.objectContaining({ nome: 'Eletrônicos' }) })

    // Validação com formulário inválido
    await wrapper.vm.filter({ valid: false, values: {} })

    // Limpar filtros
    wrapper.vm.limpar()
    await new Promise((r) => setTimeout(r, 0))
    expect(wrapper.vm.page).toBe(0)
    expect(wrapper.vm.sortField).toBeNull()
  })

  it('executa paginação (onPage) com sucesso e trata cancelamento quando saveAll falha', async () => {
    api.post.mockResolvedValue({ status: 200 })
    const wrapper = mountComponent()
    await nextTick()

    await wrapper.vm.onPage({ page: 2, rows: 20, first: 40 })
    expect(wrapper.vm.page).toBe(2)

    // Invalida um item editando para falhar no saveAll
    wrapper.vm.addItem()
    wrapper.vm.data[wrapper.vm.data.length - 1].edicao.nome = ''
    await wrapper.vm.onPage({ page: 3, rows: 20, first: 60 })
    await nextTick()
    expect(wrapper.vm.first).toBe(40)
  })

  it('executa ordenação (onSort) com asc/desc e fallback ao falhar validação', async () => {
    api.post.mockResolvedValue({ status: 200 })
    const wrapper = mountComponent()
    await nextTick()

    // ASC
    await wrapper.vm.onSort({ sortField: 'nome', sortOrder: 1 })
    expect(api.get).toHaveBeenLastCalledWith('/product-type/list', { params: expect.objectContaining({ sort: 'nome,asc' }) })

    // DESC
    await wrapper.vm.onSort({ sortField: 'nome', sortOrder: -1 })
    expect(api.get).toHaveBeenLastCalledWith('/product-type/list', { params: expect.objectContaining({ sort: 'nome,desc' }) })

    // Invalida para disparar o caminho do else no onSort
    const preventDefaultMock = vi.fn()
    wrapper.vm.addItem()
    wrapper.vm.data[wrapper.vm.data.length - 1].edicao.nome = ''
    await wrapper.vm.onSort({
      sortField: 'nome',
      sortOrder: 1,
      originalEvent: { preventDefault: preventDefaultMock }
    })
    await nextTick()
    expect(preventDefaultMock).toHaveBeenCalled()
  })

  it('permite alternar estado de edição com edit e cancelar alteração/criação', async () => {
    const wrapper = mountComponent()
    await nextTick()

    const item = wrapper.vm.data[0]
    wrapper.vm.edit(item)
    expect(item.editando).toBe(true)

    // Cancelar item existente
    wrapper.vm.cancel(item)
    expect(item.editando).toBe(false)

    // Cancelar novo item adicionado
    wrapper.vm.addItem()
    const initialLen = wrapper.vm.data.length
    const newItem = wrapper.vm.data[initialLen - 1]
    
    wrapper.vm.cancel(newItem)
    expect(wrapper.vm.data.length).toBe(initialLen - 1)
  })

  it('valida regras de negócio no commit individual e salva o tipo de produto', async () => {
    const wrapper = mountComponent()
    await nextTick()

    const item = wrapper.vm.data[0]
    wrapper.vm.edit(item)

    // Nome em branco
    item.edicao.nome = '   '
    await wrapper.vm.commit(item)
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Dados Insuficientes' }))

    // Sucesso em atualizar item existente
    item.edicao.nome = 'Eletrônicos Modificados'
    api.post.mockResolvedValueOnce({ status: 200, data: { id: 1 } })
    await wrapper.vm.commit(item)
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Sucesso' }))

    // Sucesso em criar novo item
    wrapper.vm.addItem()
    const newItem = wrapper.vm.data[wrapper.vm.data.length - 1]
    newItem.edicao.nome = 'Novo Tipo'
    api.post.mockResolvedValueOnce({ status: 200, data: { id: 99 } })
    await wrapper.vm.commit(newItem)
    expect(newItem.id).toBe(99)

    // Trata erro de API no commit
    item.edicao.nome = 'Erro Teste'
    api.post.mockRejectedValueOnce({ response: { data: 'Erro ao salvar' } })
    await wrapper.vm.commit(item)
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Gravação de Tipo de Produto' }))
  })

  it('exclui tipo de produto via confirmDelete com sucesso e tratamento de erro', async () => {
    const wrapper = mountComponent()
    await nextTick()

    const item = wrapper.vm.data[0]
    wrapper.vm.confirmDelete(item)
    expect(mockConfirmRequire).toHaveBeenCalled()

    const confirmArgs = mockConfirmRequire.mock.calls[0][0]

    // Confirmar Exclusão - Sucesso
    api.delete.mockResolvedValueOnce({})
    await confirmArgs.accept()
    expect(api.delete).toHaveBeenCalledWith('/product-type?id=1')
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ severity: 'success' }))

    // Confirmar Exclusão - Erro
    wrapper.vm.confirmDelete(item)
    api.delete.mockRejectedValueOnce({ response: { data: 'Erro ao deletar' } })
    await mockConfirmRequire.mock.calls[1][0].accept()
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Remoção de Tipo de Produto' }))
  })

  it('executa saveAll e clickAndSaveAll com validação de campos e erro de API', async () => {
    const wrapper = mountComponent()
    await nextTick()

    // Validação de nome obrigatório ao salvar todos em modo edição
    wrapper.vm.addItem()
    const newItem = wrapper.vm.data[wrapper.vm.data.length - 1]
    newItem.edicao.nome = ''
    
    let res = await wrapper.vm.saveAll(true)
    expect(res).toBe(false)
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ detail: 'Nome é obrigatório.' }))

    // Corrige o item adicionado para permitir o saveAll
    newItem.edicao.nome = 'Valido'
    api.post.mockResolvedValueOnce({ status: 200 })
    await wrapper.vm.clickAndSaveAll()
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Sucesso' }))

    // Erro na API ao salvar todos
    newItem.editando = true
    api.post.mockRejectedValueOnce({ response: { data: 'Erro Geral' } })
    res = await wrapper.vm.saveAll(true)
    expect(res).toBe(false)
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Gravação de Produto' }))
  })

  it('interage com todos os botões e formulários do template', async () => {
    api.post.mockResolvedValue({ status: 200, data: { id: 1 } })
    const wrapper = mountComponent()
    await nextTick()

    // Submete e reseta o formulário
    const form = wrapper.find('form')
    await form.trigger('submit')
    await form.trigger('reset')

    // Executa métodos diretamente para cobrir interações de template
    wrapper.vm.addItem()
    const item = wrapper.vm.data[0]
    wrapper.vm.edit(item)
    await wrapper.vm.commit(item)
    wrapper.vm.confirmDelete(item)
    wrapper.vm.cancel(item)

    // Botão Salvar
    const saveBtn = wrapper.findAll('button').find((b) => b.text().includes('Salvar'))
    if (saveBtn) await saveBtn.trigger('click')
  })
})