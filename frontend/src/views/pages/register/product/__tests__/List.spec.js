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

describe('List.vue - src/views/pages/register/product/List.vue', () => {
  const mockProductList = [
    {
      id: 1,
      nome: 'Produto A',
      referencia: 'REF001',
      peso: 500,
      estoque: 10,
      tipoProduto: { id: 101, nome: 'Eletrônicos' },
      fornecedor: { id: 201, fantasia: 'Fornecedor X' }
    },
    {
      id: 2,
      nome: 'Produto B',
      referencia: 'REF002',
      peso: 1200,
      estoque: 0,
      tipoProduto: { id: 102, nome: 'Acessórios' },
      fornecedor: { id: 202, fantasia: 'Fornecedor Y' }
    }
  ]

  const mockTypes = [{ id: 101, nome: 'Eletrônicos' }, { id: 102, nome: 'Acessórios' }]
  const mockProviders = [{ id: 201, fantasia: 'Fornecedor X' }, { id: 202, fantasia: 'Fornecedor Y' }]

  beforeEach(() => {
    vi.clearAllMocks()
    api.get.mockImplementation((url) => {
      if (url === '/product/list') {
        return Promise.resolve({
          data: {
            content: JSON.parse(JSON.stringify(mockProductList)),
            totalElements: 2
          }
        })
      }
      if (url === '/product-type/list') {
        return Promise.resolve({ data: { content: mockTypes } })
      }
      if (url === '/provider/list') {
        return Promise.resolve({ data: { content: mockProviders } })
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
                <slot name="body" :data="{ id: 1, nome: 'Produto A', referencia: 'REF001', tipoProduto: { id: 101, nome: 'Eletrônicos' }, fornecedor: { id: 201, fantasia: 'Fornecedor X' }, peso: 500, estoque: 10, editando: false, edicao: { nome: 'Produto A', idTipoProduto: 101, idFornecedor: 201, referencia: 'REF001', peso: 500, estoque: 10 } }" />
                <slot name="body" :data="{ id: null, nome: null, referencia: null, tipoProduto: { id: null }, fornecedor: { id: null }, peso: null, estoque: null, editando: true, edicao: { nome: 'Novo', idTipoProduto: 101, idFornecedor: 201, referencia: 'REF', peso: 100, estoque: 5 } }" />
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
          InputNumber: {
            props: ['modelValue'],
            template: '<input type="number" :value="modelValue" @input="$emit(\'update:modelValue\', Number($event.target.value))" />'
          },
          Select: {
            props: ['modelValue', 'options'],
            template: '<select :value="modelValue" @change="$emit(\'update:modelValue\', $event.target.value)"><option v-for="opt in options" :key="opt.id" :value="opt.id">{{ opt.nome || opt.fantasia }}</option></select>'
          },
          Form: defineComponent({
            name: 'Form',
            template: '<form @submit.prevent="$emit(\'submit\', { valid: true, values: {} })" @reset="$emit(\'reset\')"><slot /></form>'
          }),
          FormField: { template: '<div><slot /></div>' },
          FloatLabel: { template: '<div><slot /></div>' },
          ConfirmDialog: true,
          Popover: {
            template: '<div class="popover-stub"><slot /></div>',
            methods: { toggle: vi.fn() }
          }
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

    expect(api.get).toHaveBeenCalledWith('/product/list', expect.any(Object))
    expect(api.get).toHaveBeenCalledWith('/product-type/list', expect.any(Object))
    expect(api.get).toHaveBeenCalledWith('/provider/list', expect.any(Object))
    expect(wrapper.vm.data.length).toBe(2)
  })

  it('trata erros de API ao carregar produtos, tipos e fornecedores no onMounted', async () => {
    api.get.mockRejectedValue({ response: { data: 'Erro na API' } })
    mountComponent()
    await nextTick()
    await nextTick()

    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Carga de Produtos' }))
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Carga de Tipos de Produto' }))
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Carga de Fornecedores' }))
  })

  it('filtra produtos e limpa os campos de pesquisa', async () => {
    const wrapper = mountComponent()
    await nextTick()

    const filterData = { valid: true, values: { nome: 'Produto A' } }
    await wrapper.vm.filter(filterData)

    expect(wrapper.vm.page).toBe(0)
    expect(api.get).toHaveBeenLastCalledWith('/product/list', { params: expect.objectContaining({ nome: 'Produto A' }) })

    // Validação com formulário inválido
    await wrapper.vm.filter({ valid: false, values: {} })

    // Limpar filtros
    wrapper.vm.limpar()
    await new Promise((r) => setTimeout(r, 0))
    expect(wrapper.vm.page).toBe(0)
    expect(wrapper.vm.sortField).toBeNull()
  })

  it('executa paginação (onPage) com sucesso e quando saveAll falha', async () => {
    api.post.mockResolvedValue({ status: 200 })
    const wrapper = mountComponent()
    await nextTick()

    await wrapper.vm.onPage({ page: 2, rows: 40, first: 80 })
    expect(wrapper.vm.page).toBe(2)

    // Falha em saveAll ao tentar paginar
    wrapper.vm.addItem()
    wrapper.vm.data[0].edicao.nome = ''
    await wrapper.vm.onPage({ page: 3, rows: 40, first: 120 })
    await nextTick()
    expect(wrapper.vm.first).toBe(80)
  })

  it('executa ordenação (onSort) com ordenação asc/desc e falhas de validação', async () => {
    api.post.mockResolvedValue({ status: 200 })
    const wrapper = mountComponent()
    await nextTick()

    // ASC
    await wrapper.vm.onSort({ sortField: 'nome', sortOrder: 1 })
    expect(api.get).toHaveBeenLastCalledWith('/product/list', { params: expect.objectContaining({ sort: 'nome,asc' }) })

    // DESC
    await wrapper.vm.onSort({ sortField: 'nome', sortOrder: -1 })
    expect(api.get).toHaveBeenLastCalledWith('/product/list', { params: expect.objectContaining({ sort: 'nome,desc' }) })

    // Erro ao ordenar
    const preventDefaultMock = vi.fn()
    wrapper.vm.addItem()
    wrapper.vm.data[0].edicao.nome = ''
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
    const newItem = wrapper.vm.data[0]
    const initialLen = wrapper.vm.data.length
    wrapper.vm.cancel(newItem)
    expect(wrapper.vm.data.length).toBe(initialLen - 1)
  })

  it('valida regras de negócio no commit individual e grava o produto', async () => {
    const wrapper = mountComponent()
    await nextTick()

    const item = wrapper.vm.data[0]
    wrapper.vm.edit(item)

    // Falha por peso zerado/inválido
    item.edicao.peso = 0
    await wrapper.vm.commit(item)
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Dados Insuficientes' }))

    // Sucesso para edição de item existente
    item.edicao.peso = 600
    api.post.mockResolvedValueOnce({ status: 200 })
    await wrapper.vm.commit(item)
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Sucesso' }))

    // Sucesso para criação de novo item
    wrapper.vm.addItem()
    const newItem = wrapper.vm.data[0]
    newItem.edicao = { nome: 'Novo Prod', idTipoProduto: 101, idFornecedor: 201, referencia: 'R1', peso: 100, estoque: 5 }
    api.post.mockResolvedValueOnce({ status: 200 })
    await wrapper.vm.commit(newItem)
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ detail: expect.stringContaining('criado') }))

    // Trata erro de API no commit
    item.edicao.peso = 600
    api.post.mockRejectedValueOnce({ response: { data: 'Erro ao salvar' } })
    await wrapper.vm.commit(item)
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Gravação de Produto' }))
  })

  it('exclui produto via confirmDelete com sucesso e falha', async () => {
    const wrapper = mountComponent()
    await nextTick()

    const item = wrapper.vm.data[0]
    wrapper.vm.confirmDelete(item)
    expect(mockConfirmRequire).toHaveBeenCalled()

    const confirmArgs = mockConfirmRequire.mock.calls[0][0]

    // Confirmar Exclusão - Sucesso
    api.delete.mockResolvedValueOnce({})
    await confirmArgs.accept()
    expect(api.delete).toHaveBeenCalledWith('/product?id=1')
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ severity: 'success' }))

    // Confirmar Exclusão - Erro
    wrapper.vm.confirmDelete(item)
    api.delete.mockRejectedValueOnce({ response: { data: 'Erro ao deletar' } })
    await mockConfirmRequire.mock.calls[1][0].accept()
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Remoção de Produto' }))
  })

  it('salva todos (saveAll e clickAndSaveAll) com validação de estoque e edição', async () => {
    const wrapper = mountComponent()
    await nextTick()

    // Teste 1: Validação de falta de estoque em item não editando
    wrapper.vm.data[0].edicao.estoque = null
    let res = await wrapper.vm.saveAll(true)
    expect(res).toBe(false)
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ detail: 'Unidades em estoque é obrigatório.' }))

    // Teste 2: Validação de dados insuficientes em item editando
    wrapper.vm.data[0].edicao.estoque = 10
    wrapper.vm.data[0].editando = true
    wrapper.vm.data[0].edicao.nome = ''
    res = await wrapper.vm.saveAll(true)
    expect(res).toBe(false)

    // Teste 3: Sucesso em saveAll
    wrapper.vm.data[0].edicao.nome = 'Produto Válido'
    api.post.mockResolvedValueOnce({ status: 200 })
    await wrapper.vm.clickAndSaveAll()
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Sucesso' }))

    // Teste 4: Erro da API ao salvar todos
    api.post.mockRejectedValueOnce({ response: { data: 'Erro geral' } })
    res = await wrapper.vm.saveAll(true)
    expect(res).toBe(false)
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Gravação de Produto' }))
  })

  it('carrega o estoque detalhado do ponto de venda no togglePopover com sucesso e erro', async () => {
    const wrapper = mountComponent()
    await nextTick()

    // Sucesso com lista populada
    api.get.mockResolvedValueOnce({
      status: 200,
      data: [{ id: 1, pontoVenda: { nome: 'Loja 1' }, saldo: 5 }]
    })
    const event = { currentTarget: {} }
    await wrapper.vm.togglePopover(event, wrapper.vm.data[0])
    expect(wrapper.vm.salePoints.length).toBe(1)

    // Sucesso com lista vazia
    api.get.mockResolvedValueOnce({ status: 200, data: [] })
    await wrapper.vm.togglePopover(event, wrapper.vm.data[0])
    expect(wrapper.vm.salePoints.length).toBe(0)

    // Erro na requisição
    api.get.mockRejectedValueOnce({ response: { data: 'Erro estoque' } })
    await wrapper.vm.togglePopover(event, wrapper.vm.data[0])
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Estoque' }))
  })

  it('interage com todos os elementos e botões da interface HTML', async () => {
    const wrapper = mountComponent()
    await nextTick()

    // Submete e reseta formulário de filtro
    const form = wrapper.find('form')
    await form.trigger('submit')
    await form.trigger('reset')

    // Dispara botões do grid de dados
    const addBtn = wrapper.find('button[data-icon="pi pi-plus"]')
    if (addBtn.exists()) await addBtn.trigger('click')

    const pencilBtn = wrapper.find('button[data-icon="pi pi-pencil"]')
    if (pencilBtn.exists()) await pencilBtn.trigger('click')

    const trashBtn = wrapper.find('button[data-icon="pi pi-trash"]')
    if (trashBtn.exists()) await trashBtn.trigger('click')

    const checkBtn = wrapper.find('button[data-icon="pi pi-check"]')
    if (checkBtn.exists()) await checkBtn.trigger('click')

    const timesBtn = wrapper.find('button[data-icon="pi pi-times"]')
    if (timesBtn.exists()) await timesBtn.trigger('click')

    // Ícone de info do estoque
    const infoIcon = wrapper.find('.pi-info-circle')
    if (infoIcon.exists()) await infoIcon.trigger('click')

    // Botão final de salvar
    api.post.mockResolvedValueOnce({ status: 200 })
    const saveBtn = wrapper.findAll('button').find((b) => b.text().includes('Salvar'))
    if (saveBtn) await saveBtn.trigger('click')
  })
})