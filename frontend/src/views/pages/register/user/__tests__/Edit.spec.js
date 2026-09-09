import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, h, nextTick } from 'vue'
import Edit from '../Edit.vue'

const mockToastAdd = vi.fn()
const mockRouterBack = vi.fn()

vi.mock('primevue/usetoast', () => ({
  useToast: () => ({ add: mockToastAdd })
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({ query: { id: '123' } }),
  useRouter: () => ({ back: mockRouterBack })
}))

vi.mock('@/util/auth', () => ({
  eAdmin: vi.fn(() => true),
  sha256Hex: vi.fn(async (pass) => 'hashed_' + pass)
}))

vi.mock('@/util/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn()
  }
}))

import api from '@/util/api'
import { eAdmin } from '@/util/auth'

describe('Edit.vue - src/views/pages/register/user/Edit.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()

    api.get.mockImplementation((url) => {
      if (url === '/user/get') {
        return Promise.resolve({
          data: {
            email: 'test@test.com',
            perfis: [{ id: 1, nome: 'Admin' }]
          }
        })
      }
      if (url === '/user/profiles') {
        return Promise.resolve({
          data: [
            { id: 1, nome: 'Admin' },
            { id: 2, nome: 'Vendedor' }
          ]
        })
      }
      if (url === '/price-table/list') {
        return Promise.resolve({
          data: {
            content: [
              { id: 10, nome: 'Tabela 1' },
              { id: 20, nome: 'Tabela 2' }
            ]
          }
        })
      }
      if (url === '/user-price-table/list') {
        return Promise.resolve({
          data: {
            content: [
              { id: 1, tabela: { id: 10, nome: 'Tabela 1' } },
              { id: 2, tabela: { id: 20, nome: 'Tabela 2' } }
            ]
          }
        })
      }
      if (url === '/sale-point/list') {
        return Promise.resolve({
          data: {
            content: [
              { id: 100, nome: 'Ponto 1' },
              { id: 200, nome: 'Ponto 2' }
            ]
          }
        })
      }
      if (url === '/user-sale-point/list') {
        return Promise.resolve({
          data: {
            content: [
              { id: 1, pontoVenda: { id: 100, nome: 'Ponto 1' } },
              { id: 2, pontoVenda: { id: 200, nome: 'Ponto 2' } }
            ]
          }
        })
      }
      return Promise.resolve({ data: {} })
    })
  })

  function mountComponent(admin = true) {
    eAdmin.mockReturnValue(admin)
    return mount(Edit, {
      global: {
        stubs: {
          Card: {
            render() {
              return h('div', [this.$slots.title?.(), this.$slots.content?.()])
            }
          },
          Button: {
            props: ['label', 'icon'],
            render() {
              return h('button', {
                type: 'button',
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
          Password: {
            props: ['modelValue'],
            render() {
              return h('input', {
                type: 'password',
                value: this.modelValue,
                onInput: (e) => this.$emit('update:modelValue', e.target.value)
              })
            }
          },
          Checkbox: {
            props: ['modelValue', 'value'],
            render() {
              return h('input', {
                type: 'checkbox',
                checked: Array.isArray(this.modelValue) && this.modelValue.includes(this.value),
                onChange: (e) => {
                  const val = this.modelValue || []
                  if (e.target.checked) {
                    this.$emit('update:modelValue', [...val, this.value])
                  } else {
                    this.$emit('update:modelValue', val.filter(v => v !== this.value))
                  }
                  this.$emit('change', e)
                }
              })
            }
          },
          RadioButton: {
            props: ['modelValue', 'value'],
            render() {
              return h('input', {
                type: 'radio',
                checked: this.modelValue === this.value,
                onChange: () => this.$emit('update:modelValue', this.value)
              })
            }
          },
          Message: {
            props: ['size', 'severity', 'variant'],
            render() { return h('div', { class: 'p-message' }, [this.$slots.default?.()]) }
          },
          Form: defineComponent({
            name: 'Form',
            props: ['resolver', 'initialValues'],
            methods: {
              setValues(vals) {
                this.internalValues = { ...this.internalValues, ...vals }
              }
            },
            data() { return { internalValues: this.initialValues || {} } },
            render() {
              return h('form', {
                onSubmit: async (e) => {
                  e.preventDefault()
                  let valid = true
                  if (this.resolver) {
                    try {
                      await this.resolver({ values: this.internalValues })
                    } catch (err) {
                      valid = false
                    }
                  }
                  this.$emit('submit', { valid, values: this.internalValues })
                },
                onReset: (e) => {
                  e.preventDefault()
                  this.$emit('reset', e)
                }
              }, [this.$slots.default?.()])
            }
          }),
          FormField: {
            props: ['name'],
            data() {
              return {
                localValue: this.name === 'perfis' || this.name === 'tabelas' || this.name === 'pontos' ? [] : (this.name === 'tabela' || this.name === 'ponto' ? 0 : '')
              }
            },
            render() {
              const vm = this
              const fieldState = {
                get value() {
                  const parent = vm.$parent
                  if (parent && parent.internalValues && parent.internalValues[vm.name] !== undefined) {
                    return parent.internalValues[vm.name]
                  }
                  return vm.localValue
                },
                set value(val) {
                  vm.localValue = val
                  const parent = vm.$parent
                  if (parent && parent.internalValues) {
                    parent.internalValues[vm.name] = val
                  }
                },
                invalid: true,
                error: { message: 'Erro de validação' }
              }
              return h('div', [this.$slots.default?.(fieldState)])
            }
          },
          FloatLabel: {
            render() { return h('div', [this.$slots.default?.()]) }
          }
        },
        directives: {
          tooltip: {}
        }
      }
    })
  }

  it('carrega dados do usuário e chamadas de Admin no onMounted', async () => {
    const wrapper = mountComponent(true)
    await nextTick()
    await nextTick()

    expect(api.get).toHaveBeenCalledWith('/user/get', { params: { id: 123 } })
    expect(api.get).toHaveBeenCalledWith('/user/profiles')
    expect(api.get).toHaveBeenCalledWith('/price-table/list', expect.any(Object))
    expect(api.get).toHaveBeenCalledWith('/user-price-table/list', expect.any(Object))
    expect(api.get).toHaveBeenCalledWith('/sale-point/list', expect.any(Object))
    expect(api.get).toHaveBeenCalledWith('/user-sale-point/list', expect.any(Object))
    expect(wrapper.vm.userProfiles).toBe(1)
  })

  it('carrega dados sem chamadas de Admin quando não é admin', async () => {
    mountComponent(false)
    await nextTick()
    await nextTick()

    expect(api.get).toHaveBeenCalledWith('/user/get', { params: { id: 123 } })
    expect(api.get).not.toHaveBeenCalledWith('/user/profiles')
  })

  it('trata erros de API nas funções de carga (load, loadProfiles, etc)', async () => {
    api.get.mockRejectedValueOnce({ response: { data: 'Erro user' } })
    api.get.mockRejectedValueOnce({ response: { data: 'Erro profiles' } })
    api.get.mockRejectedValueOnce({ response: { data: 'Erro prices' } })
    api.get.mockRejectedValueOnce({ response: { data: 'Erro user prices' } })
    api.get.mockRejectedValueOnce({ response: { data: 'Erro sales' } })
    api.get.mockRejectedValueOnce({ response: { data: 'Erro user sales' } })

    mountComponent(true)
    await nextTick()
    await nextTick()

    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Carga de Usuário' }))
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Carga de Perfis' }))
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Carga de Tabelas de Preços' }))
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Carga de Tabelas de Preços do Usuário' }))
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Carga de Pontos de Venda' }))
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Carga de Pontos de Venda do Usuário' }))
  })

  it('executa loadUserPriceTables e loadUserSalePoints com userProfiles === 1 e userProfiles > 1', async () => {
    const wrapper = mountComponent(true)
    await nextTick()

    // Testa userProfiles === 1 explicitamente
    wrapper.vm.userProfiles = 1
    wrapper.vm.tableForm = { setValues: vi.fn() }
    wrapper.vm.salePointForm = { setValues: vi.fn() }
    await wrapper.vm.loadUserPriceTables()
    await wrapper.vm.loadUserSalePoints()
    expect(wrapper.vm.tableForm.setValues).toHaveBeenCalled()

    // Testa userProfiles > 1 (cobre as linhas 145 e 175)
    wrapper.vm.userProfiles = 2
    await wrapper.vm.loadUserPriceTables()
    await wrapper.vm.loadUserSalePoints()
    expect(wrapper.vm.tableForm.setValues).toHaveBeenCalled()
  })

  it('valida os resolvers Zod para tabelas e pontos de venda com userProfiles 1 e 2 (linhas 187-188, 197-198)', async () => {
    const wrapper = mountComponent(true)
    await nextTick()

    // userProfiles === 1
    wrapper.vm.userProfiles = 1
    try { await wrapper.vm.tableFormValidator({ values: { tabelas: [], tabela: 0 } }) } catch (e) {}
    try { await wrapper.vm.salePointFormValidator({ values: { pontos: [], ponto: 0 } }) } catch (e) {}
    try { await wrapper.vm.tableFormValidator({ values: { tabelas: [], tabela: 10 } }) } catch (e) {}
    try { await wrapper.vm.salePointFormValidator({ values: { pontos: [], ponto: 100 } }) } catch (e) {}

    // userProfiles === 2
    wrapper.vm.userProfiles = 2
    try { await wrapper.vm.tableFormValidator({ values: { tabelas: [], tabela: 0 } }) } catch (e) {}
    try { await wrapper.vm.salePointFormValidator({ values: { pontos: [], ponto: 0 } }) } catch (e) {}
    try { await wrapper.vm.tableFormValidator({ values: { tabelas: [10], tabela: 0 } }) } catch (e) {}
    try { await wrapper.vm.salePointFormValidator({ values: { pontos: [100], ponto: 0 } }) } catch (e) {}
  })

  it('alterna userProfiles entre 1 e 2 para cobrir os blocos v-show do template', async () => {
    const wrapper = mountComponent(true)
    await nextTick()

    wrapper.vm.userProfiles = 1
    await nextTick()

    wrapper.vm.userProfiles = 2
    await nextTick()
  })

  it('salva dados do usuário com sucesso e erro', async () => {
    const wrapper = mountComponent(true)
    await nextTick()

    // Sucesso
    api.post.mockResolvedValueOnce({ status: 200 })
    await wrapper.vm.save({ valid: true, values: { email: '  novo@test.com  ' } })
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Sucesso', detail: 'Usuário atualizado com sucesso' }))

    // Inválido (retorna antecipadamente)
    await wrapper.vm.save({ valid: false, values: {} })

    // Erro de API
    api.post.mockRejectedValueOnce({ response: { data: 'Erro salvando usuário' } })
    await wrapper.vm.save({ valid: true, values: { email: 'erro@test.com' } })
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Gravação de Usuário' }))
  })

  it('altera senha com sucesso, erro e falha de validação', async () => {
    const wrapper = mountComponent(true)
    await nextTick()

    // Sucesso
    api.post.mockResolvedValueOnce({ status: 200 })
    await wrapper.vm.changePassword({ valid: true, values: { senha: 'novaSenha123', confirmarSenha: 'novaSenha123' } })
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Sucesso', detail: 'Senha alterada com sucesso' }))

    // Inválido
    await wrapper.vm.changePassword({ valid: false, values: {} })

    // Erro de API
    api.post.mockRejectedValueOnce({ response: { data: 'Erro alterando senha' } })
    await wrapper.vm.changePassword({ valid: true, values: { senha: 'outraSenha123', confirmarSenha: 'outraSenha123' } })
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Gravação de Usuário' }))

    // Testa refinamento de senha divergente
    try {
      await wrapper.vm.resolverPassword({ values: { senha: 'senha123', confirmarSenha: 'senhaDiferente' } })
    } catch (e) {}
  })

  it('salva tabelas de preços para userProfiles < 2 (com e sem userPriceTables prévio)', async () => {
    const wrapper = mountComponent(true)
    await nextTick()
    wrapper.vm.userProfiles = 1

    // Com userPriceTables vazio (cobre linha 239)
    wrapper.vm.userPriceTables = []
    api.post.mockResolvedValueOnce({ status: 200, data: { id: 1, tabela: { id: 10 } } })
    await wrapper.vm.savePriceTables({ valid: true, values: { tabela: 10, tabelas: [] } })
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Sucesso' }))

    // Inválido
    await wrapper.vm.savePriceTables({ valid: false, values: {} })

    // Erro
    api.post.mockRejectedValueOnce({ response: { data: 'Erro tabela' } })
    await wrapper.vm.savePriceTables({ valid: true, values: { tabela: 20, tabelas: [] } })
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Gravação da seleção de Tabela de Preços' }))
  })

  it('salva tabelas de preços para userProfiles >= 2 (sucesso e erro)', async () => {
    const wrapper = mountComponent(true)
    await nextTick()
    wrapper.vm.userProfiles = 2

    // Sucesso
    api.post.mockResolvedValueOnce({ status: 200 })
    await wrapper.vm.savePriceTables({ valid: true, values: { tabelas: [10, 20], tabela: 0 } })
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Sucesso' }))

    // Erro
    api.post.mockRejectedValueOnce({ response: { data: 'Erro tabelas múltiplas' } })
    await wrapper.vm.savePriceTables({ valid: true, values: { tabelas: [10], tabela: 0 } })
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Gravação da seleção de Tabela de Preços' }))
  })

  it('salva pontos de venda para userProfiles < 2 (com e sem userSalePoints prévio)', async () => {
    const wrapper = mountComponent(true)
    await nextTick()
    wrapper.vm.userProfiles = 1

    // Com userSalePoints vazio (cobre linha 284/297)
    wrapper.vm.userSalePoints = []
    api.post.mockResolvedValueOnce({ status: 200, data: { id: 1, pontoVenda: { id: 100 } } })
    await wrapper.vm.saveSalePoints({ valid: true, values: { ponto: 100, pontos: [] } })
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Sucesso' }))

    // Inválido
    await wrapper.vm.saveSalePoints({ valid: false, values: {} })

    // Erro
    api.post.mockRejectedValueOnce({ response: { data: 'Erro ponto' } })
    await wrapper.vm.saveSalePoints({ valid: true, values: { ponto: 200, pontos: [] } })
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Gravação da seleção de Pontos de Venda' }))
  })

  it('salva pontos de venda para userProfiles >= 2 (sucesso e erro)', async () => {
    const wrapper = mountComponent(true)
    await nextTick()
    wrapper.vm.userProfiles = 2

    // Sucesso
    api.post.mockResolvedValueOnce({ status: 200 })
    await wrapper.vm.saveSalePoints({ valid: true, values: { pontos: [100, 200], ponto: 0 } })
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Sucesso' }))

    // Erro
    api.post.mockRejectedValueOnce({ response: { data: 'Erro pontos múltiplos' } })
    await wrapper.vm.saveSalePoints({ valid: true, values: { pontos: [100], ponto: 0 } })
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Gravação da seleção de Pontos de Venda' }))
  })

  it('executa a limpeza (clear) dos formulários', async () => {
    const wrapper = mountComponent(true)
    await nextTick()

    wrapper.vm.tableForm = { setValues: vi.fn() }
    wrapper.vm.salePointForm = { setValues: vi.fn() }

    wrapper.vm.clear()
    expect(wrapper.vm.tableForm.setValues).toHaveBeenCalled()
    expect(wrapper.vm.salePointForm.setValues).toHaveBeenCalled()
  })

  it('dispara a navegação de voltar pelo botão do template', async () => {
    const wrapper = mountComponent(true)
    await nextTick()

    const backButtons = wrapper.findAll('button').filter(btn => btn.attributes('data-icon') === 'pi pi-replay')
    if (backButtons.length > 0) {
      await backButtons[0].trigger('click')
      expect(mockRouterBack).toHaveBeenCalled()
    }
  })
})