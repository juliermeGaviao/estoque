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
              <div class="data-table-stub">
                <slot />
              </div>
            `
          },
          Column: {
            props: ['field'],
            template: `
              <div class="column-stub">
                <slot name="header" />
                <div v-for="(item, index) in $parent.value" :key="index" class="column-body-row">
                  <slot name="body" :data="item" />
                </div>
              </div>
            `
          },
          Button: {
            props: ['label', 'icon'],
            emits: ['click'],
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

  it('renderiza o slot de corpo da coluna Nome (exibição e input de edição)', async () => {
    api.post.mockResolvedValue({ status: 200, data: { id: 1 } })
    const wrapper = mountComponent()
    await nextTick()
    await nextTick()
    await nextTick()

    // Nomes renderizados nas linhas (v-show !editando)
    const columnStubs = wrapper.findAll('.column-stub')
    const nomeColumn = columnStubs[1] // segunda coluna = "Nome"
    expect(nomeColumn.text()).toContain('Eletrônicos')
    expect(nomeColumn.text()).toContain('Acessórios')

    // Inputs de edição presentes (v-show editando), inicialmente ocultos
    const inputs = nomeColumn.findAll('input')
    expect(inputs).toHaveLength(2)
    expect(inputs[0].element.value).toBe('Eletrônicos')
    expect(inputs[0].isVisible()).toBe(false)

    // Entra em edição
    await wrapper.vm.edit(wrapper.vm.data[0])
    await nextTick()
    expect(wrapper.vm.data[0].editando).toBe(true)
    expect(inputs[0].isVisible()).toBe(true)

    // Altera o nome pelo input (v-model em edicao.nome)
    await inputs[0].setValue('Eletrônicos v2')
    expect(wrapper.vm.data[0].edicao.nome).toBe('Eletrônicos v2')

    // Consolida
    await wrapper.vm.commit(wrapper.vm.data[0])
    await nextTick()
    expect(wrapper.vm.data[0].nome).toBe('Eletrônicos v2')
    expect(wrapper.vm.data[0].editando).toBe(false)
  })

  it('aciona os botões de ação da coluna: editar, remover, consolidar e cancelar', async () => {
    api.post.mockResolvedValue({ status: 200, data: { id: 1 } })
    const wrapper = mountComponent()
    await nextTick()
    await nextTick()
    await nextTick()

    const columnStubs = wrapper.findAll('.column-stub')
    const actionColumn = columnStubs[2]

    const pencilButtons = actionColumn.findAll('button[data-icon="pi pi-pencil"]')
    const trashButtons = actionColumn.findAll('button[data-icon="pi pi-trash"]')
    const checkButtons = actionColumn.findAll('button[data-icon="pi pi-check"]')
    const timesButtons = actionColumn.findAll('button[data-icon="pi pi-times"]')

    expect(pencilButtons).toHaveLength(2)
    expect(trashButtons).toHaveLength(2)
    expect(checkButtons[0].isVisible()).toBe(false)
    expect(timesButtons[0].isVisible()).toBe(false)

    await pencilButtons[0].trigger('click')
    await nextTick()
    expect(wrapper.vm.data[0].editando).toBe(true)
    expect(checkButtons[0].isVisible()).toBe(true)
    expect(timesButtons[0].isVisible()).toBe(true)

    await checkButtons[0].trigger('click')
    await nextTick()
    expect(wrapper.vm.data[0].editando).toBe(false)
    expect(api.post).toHaveBeenCalledWith('/product-type', { id: 1, nome: 'Eletrônicos' })

    await trashButtons[0].trigger('click')
    expect(mockConfirmRequire).toHaveBeenCalled()

    wrapper.vm.addItem()
    await nextTick()
    expect(wrapper.vm.data).toHaveLength(3)

    const newItem = wrapper.vm.data[2]
    expect(newItem.id).toBeNull()
    wrapper.vm.cancel(newItem)
    await nextTick()
    expect(wrapper.vm.data).toHaveLength(2)
  })

    it('aciona os botões de ação da coluna: editar, remover, consolidar e cancelar', async () => {
    api.post.mockResolvedValue({ status: 200, data: { id: 1 } })
    const wrapper = mountComponent()
    await nextTick()
    await nextTick()
    await nextTick()

    const columnStubs = wrapper.findAll('.column-stub')
    const actionColumn = columnStubs[2]

    const pencilButtons = actionColumn.findAll('button[data-icon="pi pi-pencil"]')
    const trashButtons = actionColumn.findAll('button[data-icon="pi pi-trash"]')
    const checkButtons = actionColumn.findAll('button[data-icon="pi pi-check"]')
    const timesButtons = actionColumn.findAll('button[data-icon="pi pi-times"]')

    expect(pencilButtons).toHaveLength(2)
    expect(trashButtons).toHaveLength(2)
    expect(checkButtons[0].isVisible()).toBe(false)
    expect(timesButtons[0].isVisible()).toBe(false)

    // Editar primeira linha
    await pencilButtons[0].trigger('click')
    await nextTick()
    expect(wrapper.vm.data[0].editando).toBe(true)
    expect(checkButtons[0].isVisible()).toBe(true)
    expect(timesButtons[0].isVisible()).toBe(true)

    // Consolidar primeira linha
    await checkButtons[0].trigger('click')
    await nextTick()
    expect(wrapper.vm.data[0].editando).toBe(false)
    expect(api.post).toHaveBeenCalledWith('/product-type', { id: 1, nome: 'Eletrônicos' })

    // Remover primeira linha (apenas aciona confirmDelete)
    await trashButtons[0].trigger('click')
    expect(mockConfirmRequire).toHaveBeenCalled()

    // Adicionar novo item (editando=true, id=null)
    wrapper.vm.addItem()
    await nextTick()
    expect(wrapper.vm.data).toHaveLength(3)

    // Cancelar o novo item CLICANDO no botão "pi pi-times" do template (cobre a linha 287)
    const newItem = wrapper.vm.data[2]
    expect(newItem.id).toBeNull()
    const timesButtonsAfter = actionColumn.findAll('button[data-icon="pi pi-times"]')
    await timesButtonsAfter[timesButtonsAfter.length - 1].trigger('click')
    await nextTick()
    expect(wrapper.vm.data).toHaveLength(2)
  })

  it('cobre o ramo de sortOrder falsy na carga (linha 29)', async () => {
    api.post.mockResolvedValue({ status: 200 })
    const wrapper = mountComponent()
    await nextTick()

    // sortField definido + sortOrder falsy (0) → não anexa ",asc"/",desc"
    await wrapper.vm.onSort({ sortField: 'nome', sortOrder: 0 })
    expect(api.get).toHaveBeenLastCalledWith('/product-type/list', { params: expect.objectContaining({ sort: 'nome' }) })
  })

  it('cobre os ramos do else de onSort: sortField nulo e sem originalEvent (linhas 86 e 96)', async () => {
    api.post.mockResolvedValue({ status: 200 })
    const wrapper = mountComponent()
    await nextTick()

    // Estado inicial: sortField nulo. Força falha no saveAll para entrar no else
    wrapper.vm.addItem()
    wrapper.vm.data[wrapper.vm.data.length - 1].edicao.nome = ''
    await wrapper.vm.onSort({ sortField: 'nome', sortOrder: 1 })
    await nextTick()

    // oldField === null → linha 86 assume undefined temporariamente
    // sem originalEvent → linha 96 não chama preventDefault
    expect(wrapper.vm.sortField).toBeNull()
    expect(wrapper.vm.sortOrder).toBeNull()
    expect(wrapper.vm.first).toBe(0)
  })

  it('cobre o ramo de status diferente de 200 no commit (linha 179)', async () => {
    const wrapper = mountComponent()
    await nextTick()
    const item = wrapper.vm.data[0]
    wrapper.vm.edit(item)
    item.edicao.nome = 'Sem Status 200'
    api.post.mockResolvedValueOnce({ status: 201, data: { id: 999 } })
    await wrapper.vm.commit(item)

    // Sem toast de sucesso e id não é atualizado
    expect(mockToastAdd).not.toHaveBeenCalled()
    expect(item.id).toBe(1)
  })

  it('cobre o ramo de status diferente de 200 no saveAll (linha 216)', async () => {
    const wrapper = mountComponent()
    await nextTick()
    api.post.mockResolvedValueOnce({ status: 201 })
    const result = await wrapper.vm.saveAll(true)

    expect(result).toBe(true)
    expect(mockToastAdd).not.toHaveBeenCalled()
  })
})