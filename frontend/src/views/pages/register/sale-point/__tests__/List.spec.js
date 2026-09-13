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
            name: 'DataTable',
            props: ['value', 'first', 'sortField', 'sortOrder'],
            emits: ['page', 'sort'],
            render() {
              return h('div', [this.$slots.default?.()])
            }
          },
          Column: {
            props: ['field', 'header'],
            render() {
              const items = this.$parent?.value || []
              return h('div', { class: 'column-stub' }, [
                this.$slots.header?.(),
                ...items.map((item, index) =>
                  h('div', { key: index, class: 'column-body-row' }, [
                    this.$slots.body?.({ data: item })
                  ])
                )
              ])
            }
          },
          Button: {
            props: ['label', 'icon', 'disabled'],
            emits: ['click'],
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

  it('cobre o ramo true de idEmpresa no commit (linhas 217-218)', async () => {
    const wrapper = mountComponent()
    await nextTick()
    const item = wrapper.vm.data[0]
    wrapper.vm.edit(item)
    item.edicao.nome = 'Ponto com Empresa'
    item.edicao.idEmpresa = 10
    api.post.mockResolvedValueOnce({ status: 200, data: { id: 1, empresa: { id: 10, nome: 'Empresa A' } } })

    // Spy no api.post para verificar se params inclui empresa
    await wrapper.vm.commit(item)

    const postCall = api.post.mock.calls.find(c => c[0] === '/sale-point')
    expect(postCall).toBeTruthy()
    expect(postCall[1]).toHaveProperty('empresa', { id: 10 })
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Sucesso' }))
  })

  it('cobre o ramo false de originalEvent no onSort (linha 297)', async () => {
    const wrapper = mountComponent()
    await nextTick()

    // Primeiro: estabelecer sortField não-nulo
    api.post.mockResolvedValueOnce({ status: 200 })
    await wrapper.vm.onSort({ sortField: 'nome', sortOrder: 1 })
    expect(wrapper.vm.sortField).toBe('nome')

    // Agora: bloquear com item em edição, SEM originalEvent
    wrapper.vm.data[0].editando = true
    wrapper.vm.data[0].edicao.nome = ''
    await wrapper.vm.onSort({ sortField: 'celular', sortOrder: -1 })
    // Sem originalEvent → preventDefault não é chamado (ramo false)
    expect(wrapper.vm.sortField).toBe('nome') // restaurado
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Dados Insuficientes' }))
  })

  it('cobre o ramo true de idEmpresa no saveAll (linha 317)', async () => {
    const wrapper = mountComponent()
    await nextTick()

    // Colocar um item em edição com idEmpresa preenchido
    wrapper.vm.data[0].editando = true
    wrapper.vm.data[0].edicao.nome = 'Ponto Editado'
    wrapper.vm.data[0].edicao.idEmpresa = 20

    api.post.mockResolvedValueOnce({ status: 200 })
    await wrapper.vm.saveAll(true)

    const postCall = api.post.mock.calls.find(c => c[0] === '/sale-point/save-all')
    expect(postCall).toBeTruthy()
    // O item em edição deve ter nome atualizado e editando=false
    expect(wrapper.vm.data[0].nome).toBe('Ponto Editado')
    expect(wrapper.vm.data[0].editando).toBe(false)
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Sucesso' }))
  })

  it('aciona os botões de ação da coluna via clique no template (cobre handlers e linha 305)', async () => {
    const wrapper = mountComponent()
    await nextTick()
    await nextTick()
    await nextTick()

    const trashButtons = wrapper.findAll('button[data-icon="pi pi-trash"]')
    expect(trashButtons.length).toBeGreaterThan(0)

    const trashButton = trashButtons[trashButtons.length - 1]
    await trashButton.trigger('click')
    expect(mockConfirmRequire).toHaveBeenCalled()

    const plusButton = wrapper.find('button[data-icon="pi pi-plus"]')
    await plusButton.trigger('click')
    expect(wrapper.vm.data.length).toBe(3)

    const timesButtons = wrapper.findAll('button[data-icon="pi pi-times"]')
    const cancelButton = timesButtons[timesButtons.length - 1]
    await cancelButton.trigger('click')
    expect(wrapper.vm.data.length).toBe(2)

    const pencilButtons = wrapper.findAll('button[data-icon="pi pi-pencil"]')
    const pencilButton = pencilButtons[pencilButtons.length - 1]
    await pencilButton.trigger('click')
    expect(wrapper.vm.data[wrapper.vm.data.length - 1].editando).toBe(true)

    const editItem = wrapper.vm.data.find(item => item.editando)
    editItem.edicao.nome = 'Ponto Consolidado'
    api.post.mockResolvedValueOnce({ status: 200, data: { id: editItem.id || 1, empresa: { id: 10 } } })
    const checkButton = wrapper.find('button[data-icon="pi pi-check"]')
    await checkButton.trigger('click')
    await nextTick()
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Sucesso' }))
  })

  it('emite eventos page e sort via DataTable no template (cobre handlers @page e @sort)', async () => {
    const wrapper = mountComponent()
    await nextTick()
    await nextTick()
    await nextTick()

    api.post.mockResolvedValue({ status: 200 })

    const dataTable = wrapper.findComponent({ name: 'DataTable' })
    expect(dataTable.exists()).toBe(true)

    // @page handler — dispara o wrapper compilado do template
    await dataTable.vm.$emit('page', { page: 1, rows: 20, first: 20 })
    await nextTick()
    expect(wrapper.vm.page).toBe(1)

    // @sort handler — dispara o wrapper compilado do template
    await dataTable.vm.$emit('sort', { sortField: 'nome', sortOrder: 1 })
    await nextTick()
    expect(wrapper.vm.sortField).toBe('nome')
  })

  it('dispara v-model handlers do InputText e Select no Column (cobre linhas 297 e 305)', async () => {
    const wrapper = mountComponent()
    await nextTick()
    await nextTick()
    await nextTick()

    // InputText com v-model="slotProps.data.edicao.nome" no Column "nome"
    const columnInputs = wrapper.findAll('.column-body-row input')
    expect(columnInputs.length).toBeGreaterThan(0)
    for (const input of columnInputs) {
      await input.trigger('input')
    }

    // Select com v-model="slotProps.data.edicao.idEmpresa" no Column "idEmpresa"
    const columnSelects = wrapper.findAll('.column-body-row select')
    expect(columnSelects.length).toBeGreaterThan(0)
    for (const select of columnSelects) {
      await select.trigger('change')
    }
  })

  it('cobre os ramos falsy e truthy dos ternários de empresa no load e no edit (linha 29)', async () => {
    // Carrega um item COM empresa e outro SEM empresa na mesma resposta
    api.get
      .mockResolvedValueOnce({
        data: {
          content: [
            { id: 1, nome: 'Com Empresa', empresa: { id: 10, nome: 'Empresa A' } },
            { id: 2, nome: 'Sem Empresa', empresa: null }
          ],
          totalElements: 2
        }
      })
      .mockResolvedValueOnce({
        data: { content: [{ id: 10, nome: 'Empresa A' }], totalElements: 1 }
      })

    const wrapper = mountComponent()
    await nextTick()
    await nextTick()
    await nextTick()

    // load(): ramo truthy → idEmpresa = 10
    expect(wrapper.vm.data[0].edicao.idEmpresa).toBe(10)
    // load(): ramo falsy → idEmpresa = null (ternário retorna null, NÃO undefined)
    expect(wrapper.vm.data[1].edicao.idEmpresa).toBe(null)

    // edit(): ramo falsy — item sem empresa
    wrapper.vm.edit(wrapper.vm.data[1])
    expect(wrapper.vm.data[1].editando).toBe(true)
    expect(wrapper.vm.data[1].edicao.idEmpresa).toBe(null)

    // edit(): ramo truthy — item com empresa
    wrapper.vm.edit(wrapper.vm.data[0])
    expect(wrapper.vm.data[0].edicao.idEmpresa).toBe(10)
  })

  it('cobre o ramo false de status !== 200 no saveAll (linhas 180-188)', async () => {
    const wrapper = mountComponent()
    await nextTick()
    api.post.mockResolvedValueOnce({ status: 201 })
    const result = await wrapper.vm.saveAll(true)
    expect(result).toBe(true)
  })

  it('cobre o ternário criado e status não-200 no commit (linha 225)', async () => {
    const wrapper = mountComponent()
    await nextTick()

    wrapper.vm.addItem()
    const newItem = wrapper.vm.data[wrapper.vm.data.length - 1]
    newItem.edicao.nome = 'Novo Ponto'
    api.post.mockResolvedValueOnce({ status: 200, data: { id: 99, empresa: { id: 10 } } })
    await wrapper.vm.commit(newItem)
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({
      detail: 'Ponto de venda criado com sucesso'
    }))

    const existingItem = wrapper.vm.data[0]
    wrapper.vm.edit(existingItem)
    existingItem.edicao.nome = 'Teste não-200'
    api.post.mockResolvedValueOnce({ status: 201 })
    await wrapper.vm.commit(existingItem)
  })

  it('cobre o ramo desc do ternario sortOrder no load (linha 24)', async () => {
    const wrapper = mountComponent()
    await nextTick()

    // Estabelece sortField e sortOrder=-1 com saveAll bem-sucedido
    api.post.mockResolvedValueOnce({ status: 200 })
    await wrapper.vm.onSort({ sortField: 'nome', sortOrder: -1 })
    await nextTick()

    // Verifica que o sort foi montado com ",desc"
    expect(api.get).toHaveBeenLastCalledWith(
      '/sale-point/list',
      expect.objectContaining({ params: expect.objectContaining({ sort: 'nome,desc' }) })
    )
  })

  it('cobre o ramo falsy de entity.empresa no edit (linha 29)', async () => {
    api.get
      .mockResolvedValueOnce({
        data: {
          content: [{ id: 3, nome: 'Ponto sem Empresa', empresa: null }],
          totalElements: 1
        }
      })
      .mockResolvedValueOnce({
        data: { content: [{ id: 10, nome: 'Empresa A' }], totalElements: 1 }
      })

    const wrapper = mountComponent()
    await nextTick()
    await nextTick()
    await nextTick()

    const item = wrapper.vm.data[0]
    expect(item.empresa).toBeNull()

    wrapper.vm.edit(item)
    expect(item.editando).toBe(true)
    expect(item.edicao.idEmpresa).toBe(null)
  })

  it('cobre o ramo false de sortOrder.value no load (linha 29)', async () => {
    const wrapper = mountComponent()
    await nextTick()

    // sortField='nome' (truthy) + sortOrder=0 (falsy) → query.sort fica só 'nome', sem sufixo
    api.post.mockResolvedValueOnce({ status: 200 })
    await wrapper.vm.onSort({ sortField: 'nome', sortOrder: 0 })

    expect(api.get).toHaveBeenLastCalledWith(
      '/sale-point/list',
      expect.objectContaining({ params: expect.objectContaining({ sort: 'nome' }) })
    )
  })
})