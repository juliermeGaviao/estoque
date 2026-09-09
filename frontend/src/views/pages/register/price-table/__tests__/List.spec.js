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

describe('src/views/pages/register/price-table/List.vue', () => {
  const mockTableList = [
    { id: 1, nome: 'Tabela Atacado' },
    { id: 2, nome: 'Tabela Varejo' }
  ]

  beforeEach(() => {
    vi.clearAllMocks()
    api.get.mockResolvedValue({
      data: {
        content: JSON.parse(JSON.stringify(mockTableList)),
        totalElements: 2
      }
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
                <!-- Renderiza slots para item normal e item em edição para cobrir 100% dos templates HTML (linhas 255, 271, 282-286) -->
                <slot name="body" :data="{ id: 1, nome: 'Normal', editando: false, edicao: { nome: 'Normal' } }" />
                <slot name="body" :data="{ id: null, nome: null, editando: true, edicao: { nome: 'Novo' } }" />
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
            template: '<form @submit.prevent="$emit(\'submit\')" @reset="$emit(\'reset\')"><slot /></form>'
          }),
          FloatLabel: { template: '<div><slot /></div>' },
          ConfirmDialog: true,
          Dialog: { template: '<div><slot /></div>' },
          ProductPriceListComponent: defineComponent({
            name: 'ProductPriceListComponent',
            props: ['id', 'nomeTabelaPreco'],
            template: '<div class="product-price-list-stub"><button class="close-btn" @click="$emit(\'close\')">Fechar</button></div>'
          })
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

    expect(api.get).toHaveBeenCalledWith('/price-table/list', { params: { page: 0, size: 20 } })
    expect(wrapper.vm.data.length).toBe(2)
    expect(wrapper.vm.totalRecords).toBe(2)
  })

  it('trata erro na carga inicial de dados em load', async () => {
    api.get.mockRejectedValueOnce({ response: { data: 'Erro de Banco' } })
    mountComponent()
    await nextTick()
    await nextTick()

    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({
      severity: 'error',
      summary: 'Falha de Carga de Tabelas de Preços'
    }))
  })

  it('aplica filtros e limpa pesquisa (onFilter e onClear)', async () => {
    const wrapper = mountComponent()
    await nextTick()

    wrapper.vm.nome = 'Atacado'
    await wrapper.vm.onFilter()

    expect(api.get).toHaveBeenLastCalledWith('/price-table/list', {
      params: { page: 0, size: 20, nome: 'Atacado' }
    })

    await wrapper.vm.onClear()
    expect(wrapper.vm.nome).toBeNull()
    expect(api.get).toHaveBeenLastCalledWith('/price-table/list', {
      params: { page: 0, size: 20 }
    })
  })

  it('executa paginação (onPage) com sucesso e com erro na validação saveAll', async () => {
    api.post.mockResolvedValue({ status: 200 })
    const wrapper = mountComponent()
    await nextTick()

    // Sucesso
    await wrapper.vm.onPage({ page: 2, rows: 40, first: 80 })
    expect(wrapper.vm.page).toBe(2)
    expect(wrapper.vm.size).toBe(40)
    expect(wrapper.vm.first).toBe(80)

    // Falha em saveAll ao tentar paginar com item inválido
    wrapper.vm.addItem()
    await wrapper.vm.onPage({ page: 3, rows: 40, first: 120 })
    
    await nextTick()
    expect(wrapper.vm.first).toBe(80)
  })

  it('executa ordenação (onSort) com sucesso (asc e desc) e com erro de validação', async () => {
    api.post.mockResolvedValue({ status: 200 })
    const wrapper = mountComponent()
    await nextTick()

    // Ordenação ASC
    await wrapper.vm.onSort({ sortField: 'nome', sortOrder: 1 })
    expect(api.get).toHaveBeenLastCalledWith('/price-table/list', {
      params: { page: 0, size: 20, sort: 'nome,asc' }
    })

    // Ordenação DESC
    await wrapper.vm.onSort({ sortField: 'nome', sortOrder: -1 })
    expect(api.get).toHaveBeenLastCalledWith('/price-table/list', {
      params: { page: 0, size: 20, sort: 'nome,desc' }
    })

    // Erro ao ordenar com item inválido
    const preventDefaultMock = vi.fn()
    wrapper.vm.addItem()
    await wrapper.vm.onSort({
      sortField: 'nome',
      sortOrder: 1,
      originalEvent: { preventDefault: preventDefaultMock }
    })

    await nextTick()
    expect(preventDefaultMock).toHaveBeenCalled()
  })

  it('permite alternar estado de edição com edit e cancelar edição/criação', async () => {
    const wrapper = mountComponent()
    await nextTick()

    const item = wrapper.vm.data[0]
    wrapper.vm.edit(item)
    expect(item.editando).toBe(true)
    expect(item.edicao.nome).toBe('Tabela Atacado')

    // Cancela item existente
    wrapper.vm.cancel(item)
    expect(item.editando).toBe(false)

    // Cancela novo item
    wrapper.vm.addItem()
    const newItem = wrapper.vm.data[wrapper.vm.data.length - 1]
    const initialLength = wrapper.vm.data.length

    wrapper.vm.cancel(newItem)
    expect(wrapper.vm.data.length).toBe(initialLength - 1)
  })

  it('consolida item individualmente (commit) com sucesso e validações', async () => {
    const wrapper = mountComponent()
    await nextTick()

    const item = wrapper.vm.data[0]
    wrapper.vm.edit(item)

    // Tenta salvar com nome vazio
    item.edicao.nome = '   '
    await wrapper.vm.commit(item)
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Dados Insuficientes' }))

    // Salva item existente com sucesso
    item.edicao.nome = 'Tabela Editada'
    api.post.mockResolvedValueOnce({ status: 200 })
    await wrapper.vm.commit(item)

    expect(api.post).toHaveBeenCalledWith('/price-table', expect.objectContaining({ nome: 'Tabela Editada' }))
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Sucesso' }))

    // Salva novo item
    wrapper.vm.addItem()
    const newItem = wrapper.vm.data[wrapper.vm.data.length - 1]
    newItem.edicao.nome = 'Nova Tabela'
    api.post.mockResolvedValueOnce({ status: 200 })
    await wrapper.vm.commit(newItem)

    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ detail: expect.stringContaining('criada') }))

    // Erro do backend
    item.edicao.nome = 'Tabela Erro'
    api.post.mockRejectedValueOnce({ response: { data: 'Erro Post' } })
    await wrapper.vm.commit(item)
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Gravação de Produto' }))
  })

  it('remove item (confirmDelete) com aceite e tratamento de erro', async () => {
    const wrapper = mountComponent()
    await nextTick()

    const item = wrapper.vm.data[0]
    wrapper.vm.confirmDelete(item)
    expect(mockConfirmRequire).toHaveBeenCalled()

    const confirmOptions = mockConfirmRequire.mock.calls[0][0]

    // Aceita exclusão com sucesso
    api.delete.mockResolvedValueOnce({})
    await confirmOptions.accept()

    expect(api.delete).toHaveBeenCalledWith('/price-table?id=1')
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ severity: 'success' }))

    // Aceita exclusão com erro
    wrapper.vm.confirmDelete(item)
    api.delete.mockRejectedValueOnce({ response: { data: 'Erro ao Deletar' } })
    await mockConfirmRequire.mock.calls[1][0].accept()

    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Remoção de Tabela de Preços' }))
  })

  it('salva todos (clickAndSaveAll / saveAll) emitindo mensagens e tratando erros', async () => {
    const wrapper = mountComponent()
    await nextTick()

    wrapper.vm.edit(wrapper.vm.data[0])
    wrapper.vm.data[0].edicao.nome = 'Nome Alterado'
    api.post.mockResolvedValueOnce({ status: 200 })

    await wrapper.vm.clickAndSaveAll()
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Sucesso' }))

    api.post.mockRejectedValueOnce({ response: { data: 'Erro no Save All' } })
    const res = await wrapper.vm.saveAll(true)
    expect(res).toBe(false)
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Gravação de Produto' }))
  })

  it('abre o modal de preços (openTable) e fecha evento no componente filho', async () => {
    const wrapper = mountComponent()
    await nextTick()

    const item = { id: 10, nome: 'Tabela Especial' }
    wrapper.vm.openTable(item)

    expect(wrapper.vm.idTabelaPreco).toBe(10)
    expect(wrapper.vm.nomeSelecionado).toBe('Tabela Especial')
    expect(wrapper.vm.visible).toBe(true)

    await nextTick()
    const childComponent = wrapper.findComponent({ name: 'ProductPriceListComponent' })
    expect(childComponent.exists()).toBe(true)

    childComponent.vm.$emit('close')
    expect(wrapper.vm.visible).toBe(false)
  })

  it('dispara todas as ações e eventos dos botões e inputs do template HTML', async () => {
    const wrapper = mountComponent()
    await nextTick()

    // Clicar no botão de adicionar nova tabela (Header da Coluna)
    const addBtn = wrapper.find('button[data-icon="pi pi-plus"]')
    if (addBtn.exists()) {
      await addBtn.trigger('click')
    }

    // Clicar nos botões das linhas (Preencher Preços, Editar, Remover, Consolidar, Cancelar)
    const dollarBtn = wrapper.find('button[data-icon="pi pi-dollar"]')
    if (dollarBtn.exists()) await dollarBtn.trigger('click')

    const pencilBtn = wrapper.find('button[data-icon="pi pi-pencil"]')
    if (pencilBtn.exists()) await pencilBtn.trigger('click')

    const trashBtn = wrapper.find('button[data-icon="pi pi-trash"]')
    if (trashBtn.exists()) await trashBtn.trigger('click')

    const checkBtn = wrapper.find('button[data-icon="pi pi-check"]')
    if (checkBtn.exists()) await checkBtn.trigger('click')

    const timesBtn = wrapper.find('button[data-icon="pi pi-times"]')
    if (timesBtn.exists()) await timesBtn.trigger('click')

    // Submeter e Resetar Formulário
    const form = wrapper.find('form')
    await form.trigger('submit')
    await form.trigger('reset')

    // Botão Salvar Geral
    api.post.mockResolvedValueOnce({ status: 200 })
    const saveAllBtn = wrapper.findAll('button').find((b) => b.text().includes('Salvar'))
    if (saveAllBtn) {
      await saveAllBtn.trigger('click')
    }

    // Fechar Modal pelo evento do filho montado no DOM
    wrapper.vm.visible = true
    await nextTick()
    const closeBtnChild = wrapper.find('.close-btn')
    if (closeBtnChild.exists()) {
      await closeBtnChild.trigger('click')
      expect(wrapper.vm.visible).toBe(false)
    }
  })
})