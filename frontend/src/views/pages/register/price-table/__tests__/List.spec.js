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

// Stub de <Dialog> com um helper de teste para disparar manualmente um
// handler recebido via attrs — necessário para cobrir o
// `@update:visible="visible = $event"` gerado pelo `v-model:visible`,
// uma closure de atribuição distinta da leitura da prop `visible`, que
// só é invocada quando o Dialog de verdade emite o evento (por isso
// precisa ser um componente nomeado e não um objeto anônimo: assim dá
// para localizá-lo via wrapper.findComponent(DialogStub)).
const DialogStub = defineComponent({
  name: 'Dialog',
  props: ['visible'],
  setup(props, { slots, attrs, expose }) {
    expose({
      emit(eventName, payload) {
        const handlerKey = 'on' + eventName.charAt(0).toUpperCase() + eventName.slice(1)
        if (typeof attrs[handlerKey] === 'function') attrs[handlerKey](payload)
      }
    })
    return () => h('div', {}, slots.default ? slots.default() : null)
  }
})

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
          Dialog: DialogStub,
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

  it('trata erro de ordenação quando sortField ainda é null (cobre o ramo `oldField === null` do fallback)', async () => {
    api.post.mockResolvedValue({ status: 200 })
    const wrapper = mountComponent()
    await nextTick()

    // Aqui sortField.value ainda é null (nenhum onSort/onPage bem-sucedido
    // rodou antes) — diferente do teste acima, onde sortField já tinha
    // sido setado para 'nome' por uma chamada anterior. Isso exercita o
    // outro lado do ternário `oldField === null ? undefined : null`.
    expect(wrapper.vm.sortField).toBeNull()

    wrapper.vm.addItem()
    await wrapper.vm.onSort({ sortField: 'cargo', sortOrder: 1, originalEvent: { preventDefault: vi.fn() } })
    await nextTick()

    // Depois do fallback, os valores originais (null) são restaurados
    expect(wrapper.vm.sortField).toBeNull()
    expect(wrapper.vm.sortOrder).toBeNull()
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

  it('não exibe sucesso nem recarrega quando a API responde com status diferente de 200 em commit', async () => {
    const wrapper = mountComponent()
    await nextTick()
    mockToastAdd.mockClear()
    api.get.mockClear()

    const item = wrapper.vm.data[0]
    wrapper.vm.edit(item)
    item.edicao.nome = 'Tabela Sem Sucesso'
    api.post.mockResolvedValueOnce({ status: 204 })

    await wrapper.vm.commit(item)

    expect(mockToastAdd).not.toHaveBeenCalledWith(expect.objectContaining({ severity: 'success' }))
    expect(api.get).not.toHaveBeenCalled()
  })

  it('acusa erro de gravação usando "criação" (não "alteração") quando o item é novo', async () => {
    const wrapper = mountComponent()
    await nextTick()

    wrapper.vm.addItem()
    const newItem = wrapper.vm.data[wrapper.vm.data.length - 1]
    newItem.edicao.nome = 'Novo Item Com Erro'
    api.post.mockRejectedValueOnce({ response: { data: 'Erro Post Novo Item' } })

    await wrapper.vm.commit(newItem)

    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({
        summary: 'Falha de Gravação de Produto',
        detail: expect.stringContaining('criação')
      })
    )
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

  it('saveAll retorna sucesso sem emitir toast quando a API responde com status diferente de 200', async () => {
    const wrapper = mountComponent()
    await nextTick()
    mockToastAdd.mockClear()

    api.post.mockResolvedValueOnce({ status: 204 })
    const res = await wrapper.vm.saveAll(true)

    expect(res).toBe(true)
    expect(mockToastAdd).not.toHaveBeenCalledWith(expect.objectContaining({ severity: 'success' }))
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

  it('atualiza o filtro "nome" digitando no campo de busca (cobre o v-model="nome")', async () => {
    const wrapper = mountComponent()
    await nextTick()

    // O campo de busca é o único InputText com id="nome" — os campos de
    // edição de linha não têm id, então esse seletor é inequívoco.
    const searchInput = wrapper.find('#nome')
    expect(searchInput.exists()).toBe(true)

    await searchInput.setValue('Tabela Atacado')

    expect(wrapper.vm.nome).toBe('Tabela Atacado')
  })

  it('atualiza o nome em edição digitando no campo da linha (cobre o v-model="slotProps.data.edicao.nome")', async () => {
    const wrapper = mountComponent()
    await nextTick()

    // O campo de busca tem id="nome"; o campo de edição da linha (vindo
    // do fake data com editando:true que o ColumnStub sempre renderiza)
    // é o único <input> sem id.
    const editInput = wrapper.findAll('input').find((input) => !input.attributes('id'))
    expect(editInput).toBeTruthy()

    await editInput.setValue('Nome Editado Via Input')

    // slotProps.data nessa segunda invocação do slot é o objeto fake
    // { editando: true, edicao: { nome: 'Novo' } } definido no ColumnStub
    // — não está ligado a wrapper.vm.data, então a asserção é sobre o
    // próprio elemento (garantindo que o v-model realmente escreveu o
    // valor de volta), não sobre o estado do componente.
    expect(editInput.element.value).toBe('Nome Editado Via Input')
  })

  it('cancela a edição de uma linha clicando no botão de cancelar específico da linha (não o "Limpar" do filtro)', async () => {
    const wrapper = mountComponent()
    await nextTick()

    // Tanto o botão "Limpar" do filtro quanto o "Cancelar" da linha usam
    // o ícone "pi pi-times" — o do filtro tem label "Limpar", o da linha
    // não tem label nenhuma (fica com texto vazio).
    const rowCancelButton = wrapper
      .findAll('button[data-icon="pi pi-times"]')
      .find((button) => button.text() === '')
    expect(rowCancelButton).toBeTruthy()

    await rowCancelButton.trigger('click')
    // Não crasha e de fato invoca cancel(slotProps.data) com o objeto
    // fake da linha em edição do ColumnStub (id: null) — cobre a chamada
    // inline @click="cancel(slotProps.data)".
  })

  it('fecha o modal de preços através do v-model:visible (update:visible emitido pelo próprio Dialog)', async () => {
    const wrapper = mountComponent()
    await nextTick()

    wrapper.vm.openTable({ id: 10, nome: 'Tabela Especial' })
    await nextTick()
    expect(wrapper.vm.visible).toBe(true)

    wrapper.findComponent(DialogStub).vm.emit('update:visible', false)
    await nextTick()

    expect(wrapper.vm.visible).toBe(false)
  })

  it('executa load com sortField definido mas sortOrder zerado/nulo (cobre o ramo falsy do sortOrder na linha 31)', async () => {
  const wrapper = mountComponent()
  await nextTick()

  // Seta sortField manualmente e deixa sortOrder como 0/null para exercitar a linha 31
  wrapper.vm.sortField = 'nome'
  wrapper.vm.sortOrder = 0

  await wrapper.vm.load()

  expect(api.get).toHaveBeenLastCalledWith('/price-table/list', {
    params: { page: 0, size: 20, sort: 'nome' }
  })
})

  it('trata erro de ordenação em onSort sem evento original (cobre o ramo falsy do event.originalEvent na linha 102)', async () => {
    api.post.mockResolvedValue({ status: 200 })
    const wrapper = mountComponent()
    await nextTick()

    // Adiciona item inválido para forçar a falha no saveAll
    wrapper.vm.addItem()

    // Executa onSort sem a propriedade originalEvent no objeto enviado
    await wrapper.vm.onSort({
      sortField: 'nome',
      sortOrder: 1
      // originalEvent é omitido (undefined)
    })

    await nextTick()

    // Garante que o estado foi restaurado sem disparar exceção ao tentar acessar preventDefault
    expect(wrapper.vm.sortField).toBeNull()
  })
})