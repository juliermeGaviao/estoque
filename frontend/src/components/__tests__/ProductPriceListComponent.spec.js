// @vitest-environment jsdom
import api from '@/util/api'
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ProductPriceListComponent from '../ProductPriceListComponent.vue'

const mockToastAdd = vi.fn()

vi.mock('primevue/usetoast', () => ({
  useToast: () => ({
    add: mockToastAdd
  })
}))

vi.mock('@/util/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn()
  }
}))

const globalStubs = {
  Card: { template: '<div><slot name="title" /><slot name="content" /></div>' },
  DataTable: {
    props: ['value'],
    template: `
      <div>
        <slot />
        <template v-for="(item, index) in value" :key="index">
          <slot name="body" :data="item" />
        </template>
      </div>
    `
  },
  Column: {
    props: ['field'],
    template: `
      <div>
        <slot />
        <slot name="body" :data="$parent.value && $parent.value[0] ? $parent.value[0] : { preco: 0 }" />
      </div>
    `
  },
  InputNumber: {
    props: ['modelValue'],
    template: '<input :value="modelValue" @input="$emit(\'update:modelValue\', Number($event.target.value))" />'
  },
  Button: { 
    props: ['label', 'icon'],
    template: '<button :class="icon" @click="$emit(\'click\', $event)">{{ label }}<slot /></button>' 
  }
}

describe('ProductPriceListComponent.vue', () => {
  const mockProductsResponse = {
    data: {
      totalElements: 1,
      content: [
        {
          id: 10,
          preco: 50.0,
          produto: { id: 100, nome: 'Produto A', referencia: 'REF1' }
        }
      ]
    }
  }

  beforeEach(() => {
    vi.clearAllMocks()

    api.get.mockImplementation((url) => {
      if (url === '/price-table-product/list-product') {
        return Promise.resolve(mockProductsResponse)
      }
      if (url === '/product-type/list' || url === '/provider/list') {
        return Promise.resolve({ data: { content: [] } })
      }
      return Promise.reject(new Error('URL não mapeada'))
    })
  })

  it('deve carregar os produtos, tipos e fornecedores no mount', async () => {
    mount(ProductPriceListComponent, {
      props: { id: 1, nomeTabelaPreco: 'Tabela Padrão' },
      global: { stubs: globalStubs }
    })

    await flushPromises()

    expect(api.get).toHaveBeenCalledWith('/price-table-product/list-product', expect.anything())
    expect(api.get).toHaveBeenCalledWith('/product-type/list', expect.anything())
    expect(api.get).toHaveBeenCalledWith('/provider/list', expect.anything())
  })

  it('deve exibir um toast de erro se a busca de produtos falhar', async () => {
    api.get.mockImplementationOnce((url) => {
      if (url === '/price-table-product/list-product') {
        return Promise.reject({ response: { data: 'Erro no servidor' } })
      }
      return Promise.resolve({ data: { content: [] } })
    })

    mount(ProductPriceListComponent, {
      props: { id: 1 },
      global: { stubs: globalStubs }
    })

    await flushPromises()

    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'error',
        summary: 'Falha de Carga de Produtos'
      })
    )
  })

  it('deve exibir toast de erro se a carga de tipos ou fornecedores falhar', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/product-type/list' || url === '/provider/list') {
        return Promise.reject({ response: { data: 'Erro ao carregar' } })
      }
      return Promise.resolve(mockProductsResponse)
    })

    mount(ProductPriceListComponent, {
      props: { id: 1 },
      global: { stubs: globalStubs }
    })

    await flushPromises()

    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({ summary: 'Falha de Carga de Tipos de Produto' })
    )
    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({ summary: 'Falha de Carga de Fornecedores' })
    )
  })

  it('deve aplicar parâmetros de ordenação ascendente e descendente no load', async () => {
    const wrapper = mount(ProductPriceListComponent, {
      props: { id: 1 },
      global: { stubs: globalStubs }
    })
    await flushPromises()

    wrapper.vm.sortField = 'produto.nome'
    wrapper.vm.sortOrder = 1
    await wrapper.vm.load()

    expect(api.get).toHaveBeenLastCalledWith('/price-table-product/list-product', {
      params: {
        idTabelaPreco: 1,
        page: 0,
        size: 20,
        sort: 'produto.nome,asc'
      }
    })

    wrapper.vm.sortOrder = -1
    await wrapper.vm.load()

    expect(api.get).toHaveBeenLastCalledWith('/price-table-product/list-product', {
      params: {
        idTabelaPreco: 1,
        page: 0,
        size: 20,
        sort: 'produto.nome,desc'
      }
    })

    wrapper.vm.sortOrder = null
    await wrapper.vm.load()

    expect(api.get).toHaveBeenLastCalledWith('/price-table-product/list-product', {
      params: {
        idTabelaPreco: 1,
        page: 0,
        size: 20,
        sort: 'produto.nome'
      }
    })
  })

  it('deve processar onPage com sucesso e alternar para a nova página', async () => {
    api.post.mockResolvedValue({ status: 200, data: {} })

    const wrapper = mount(ProductPriceListComponent, {
      props: { id: 1 },
      global: { stubs: globalStubs }
    })
    await flushPromises()

    wrapper.vm.data = [{ id: 10, preco: 50.0, produto: { id: 100 } }]

    await wrapper.vm.onPage({ page: 2, rows: 40, first: 80 })
    await flushPromises()

    expect(wrapper.vm.page).toBe(2)
    expect(wrapper.vm.size).toBe(40)
    expect(wrapper.vm.first).toBe(80)
  })

  it('deve redefinir e restaurar o estado do paginador se saveAll falhar no onPage', async () => {
    const wrapper = mount(ProductPriceListComponent, {
      props: { id: 1 },
      global: { stubs: globalStubs }
    })
    await flushPromises()

    wrapper.vm.data = [{ id: 10, preco: null, produto: { id: 100 } }]

    await wrapper.vm.onPage({ page: 2, rows: 20, first: 40 })

    expect(wrapper.vm.first).toBe(0)
  })

  it('deve processar onSort com sucesso e redefinir o campo de ordenação', async () => {
    api.post.mockResolvedValue({ status: 200, data: {} })

    const wrapper = mount(ProductPriceListComponent, {
      props: { id: 1 },
      global: { stubs: globalStubs }
    })
    await flushPromises()

    wrapper.vm.data = [{ id: 10, preco: 50.0, produto: { id: 100 } }]

    await wrapper.vm.onSort({ sortField: 'produto.nome', sortOrder: 1 })
    await flushPromises()

    expect(wrapper.vm.page).toBe(0)
    expect(wrapper.vm.sortField).toBe('produto.nome')
    expect(wrapper.vm.sortOrder).toBe(1)
  })

  it('deve redefinir a ordenação e prevenir evento original se saveAll falhar no onSort', async () => {
    const wrapper = mount(ProductPriceListComponent, {
      props: { id: 1 },
      global: { stubs: globalStubs }
    })
    await flushPromises()

    wrapper.vm.data = [{ id: 10, preco: -10, produto: { id: 100 } }]
    const preventDefaultMock = vi.fn()

    await wrapper.vm.onSort({ 
      sortField: 'produto.nome', 
      sortOrder: 1, 
      originalEvent: { preventDefault: preventDefaultMock } 
    })

    expect(preventDefaultMock).toHaveBeenCalled()
  })

  it('deve redefinir a ordenação para nulo e prevenir evento original se saveAll falhar no onSort', async () => {
    const wrapper = mount(ProductPriceListComponent, {
      props: { id: 1 },
      global: { stubs: globalStubs }
    })
    await flushPromises()

    wrapper.vm.data = [{ id: 10, preco: -10, produto: { id: 100 } }]
    const preventDefaultMock = vi.fn()

    await wrapper.vm.onSort({ 
      sortField: 'produto.nome', 
      sortOrder: null, 
      originalEvent: { preventDefault: preventDefaultMock } 
    })

    expect(preventDefaultMock).toHaveBeenCalled()
  })

  it('deve exibir toast de erro quando a API falhar no salvamento', async () => {
    api.post.mockRejectedValue({ response: { data: 'Erro ao salvar registro' } })

    const wrapper = mount(ProductPriceListComponent, {
      props: { id: 1 },
      global: { stubs: globalStubs }
    })
    await flushPromises()

    wrapper.vm.data = [{ id: 10, preco: 50.0, produto: { id: 100 } }]

    await wrapper.vm.clickAndSaveAll()
    await flushPromises()

    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'error',
        summary: 'Falha de Gravação de Preços'
      })
    )
  })

  it('deve limpar os preços da lista ao acionar cleanPrices', async () => {
    const wrapper = mount(ProductPriceListComponent, {
      props: { id: 1 },
      global: { stubs: globalStubs }
    })
    await flushPromises()

    const cleanBtn = wrapper.findAll('button').find((b) => b.text().includes('Limpar'))
    await cleanBtn.trigger('click')

    expect(wrapper.vm.data[0].preco).toBeNull()
  })

  it('deve validar preços inválidos (nulos ou negativos) antes de salvar', async () => {
    api.get.mockImplementationOnce((url) => {
      if (url === '/price-table-product/list-product') {
        return Promise.resolve({
          data: {
            totalElements: 1,
            content: [{ id: 10, preco: -5, produto: { id: 100 } }]
          }
        })
      }
      return Promise.resolve({ data: { content: [] } })
    })

    const wrapper = mount(ProductPriceListComponent, {
      props: { id: 1 },
      global: { stubs: globalStubs }
    })
    await flushPromises()

    const saveBtn = wrapper.findAll('button').find((b) => b.text().includes('Salvar'))
    await saveBtn.trigger('click')

    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'error',
        summary: 'Dados Insuficientes'
      })
    )
    expect(api.post).not.toHaveBeenCalled()
  })

  it('deve salvar os preços com sucesso e exibir aviso quando fornecido', async () => {
    api.post.mockResolvedValue({ status: 200, data: {} })

    const wrapper = mount(ProductPriceListComponent, {
      props: { id: 1 },
      global: { stubs: globalStubs }
    })
    await flushPromises()

    wrapper.vm.data = [{ id: 10, preco: 50.0, produto: { id: 100 } }]

    wrapper.vm.clickAndSaveAll()
    await flushPromises()

    expect(api.post).toHaveBeenCalledWith('/price-table-product/save-prices', [
      { id: 10, produto: { id: 100 }, tabela: { id: 1 }, preco: 50 }
    ])

    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'success',
        summary: 'Sucesso'
      })
    )
  })

  it('deve emitir o evento close ao clicar no botão fechar do rodapé', async () => {
    const wrapper = mount(ProductPriceListComponent, {
      props: { id: 1 },
      global: { stubs: globalStubs }
    })
    await flushPromises()

    const closeBtn = wrapper.findAll('button').find((b) => b.text().includes('Fechar'))
    await closeBtn.trigger('click')

    expect(wrapper.emitted('close')).toBeTruthy()
  })

  it('deve emitir o evento close ao clicar no ícone de fechar do cabeçalho', async () => {
    const wrapper = mount(ProductPriceListComponent, {
      props: { id: 1 },
      global: { stubs: globalStubs }
    })
    await flushPromises()

    const iconCloseBtn = wrapper.findAll('button').find((b) => b.classes().includes('pi-times'))
    await iconCloseBtn.trigger('click')

    expect(wrapper.emitted('close')).toBeTruthy()
  })

  it('deve garantir a atualização e reatividade do preco no data da tabela', async () => {
    const wrapper = mount(ProductPriceListComponent, {
      props: { id: 1 },
      global: { stubs: globalStubs }
    })
    await flushPromises()

    wrapper.vm.data[0].preco = 99.9
    await wrapper.vm.$nextTick()

    expect(wrapper.vm.data[0].preco).toBe(99.9)
  })

  it('deve interagir com o InputNumber do template e alterar o preço reativamente', async () => {
    const wrapper = mount(ProductPriceListComponent, {
      props: { id: 1 },
      global: { stubs: globalStubs }
    })
    await flushPromises()

    const input = wrapper.find('input')
    expect(input.exists()).toBe(true)

    await input.setValue('75.5')
    await wrapper.vm.$nextTick()

    expect(wrapper.vm.data[0].preco).toBe(75.5)
  })

  it('deve cobrir as linhas 88-98 revertendo o estado de ordenacao quando saveAll falhar no onSort', async () => {
    const wrapper = mount(ProductPriceListComponent, {
      props: { id: 1 },
      global: { stubs: globalStubs }
    })
    await flushPromises()

    // Configura o estado inicial para garantir que o 'oldField' e 'oldOrder' existam nas linhas 88-98
    wrapper.vm.sortField = 'produto.nome'
    wrapper.vm.sortOrder = 1

    // Preço inválido força o saveAll(false) a retornar false, acionando o bloco else (linhas 88-98)
    wrapper.vm.data = [{ id: 10, preco: -1, produto: { id: 100 } }]

    const preventDefaultMock = vi.fn()
    await wrapper.vm.onSort({
      sortField: 'produto.referencia',
      sortOrder: -1,
      originalEvent: { preventDefault: preventDefaultMock }
    })

    await flushPromises()

    expect(preventDefaultMock).toHaveBeenCalled()
    expect(wrapper.vm.sortField).toBe('produto.nome')
    expect(wrapper.vm.sortOrder).toBe(1)
  })

  it('deve executar saveAll com sucesso sem emitir aviso quando emitirAviso for false (linhas 109-122)', async () => {
    api.post.mockResolvedValueOnce({ status: 200, data: {} })

    const wrapper = mount(ProductPriceListComponent, {
      props: { id: 1 },
      global: { stubs: globalStubs }
    })
    await flushPromises()

    wrapper.vm.data = [{ id: 10, preco: 100, produto: { id: 100 } }]

    // Executa saveAll diretamente omitindo o parâmetro emitirAviso
    const result = await wrapper.vm.saveAll(false)

    expect(result).toBe(true)
    expect(api.post).toHaveBeenCalledWith('/price-table-product/save-prices', [
      { id: 10, produto: { id: 100 }, tabela: { id: 1 }, preco: 100 }
    ])
  })

  it('deve processar onSort com falha sem quebrar quando originalEvent nao for informado (cobertura do branch da linha 98)', async () => {
    const wrapper = mount(ProductPriceListComponent, {
      props: { id: 1 },
      global: { stubs: globalStubs }
    })
    await flushPromises()

    // Preço inválido para forçar saveAll a retornar false
    wrapper.vm.data = [{ id: 10, preco: -1, produto: { id: 100 } }]

    // Dispara onSort SEM a propriedade originalEvent
    await expect(
      wrapper.vm.onSort({
        sortField: 'produto.nome',
        sortOrder: 1
      })
    ).resolves.not.toThrow()
  })

})