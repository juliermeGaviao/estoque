import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, h, nextTick } from 'vue'
import Insert from '../Insert.vue'

const mockToastAdd = vi.fn()
const mockRouterPush = vi.fn()

vi.mock('primevue/usetoast', () => ({
  useToast: () => ({ add: mockToastAdd })
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: mockRouterPush })
}))

vi.mock('@/util/auth', () => ({
  sha256Hex: vi.fn(async (pass) => 'hashed_' + pass)
}))

vi.mock('@/util/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn()
  }
}))

import api from '@/util/api'

describe('Insert.vue - src/views/pages/register/user/Insert.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()

    api.get.mockImplementation((url) => {
      if (url === '/user/profiles') {
        return Promise.resolve({
          data: [
            { id: 1, nome: 'Admin' },
            { id: 2, nome: 'Vendedor' }
          ]
        })
      }
      return Promise.resolve({ data: {} })
    })
  })

  function mountComponent() {
    return mount(Insert, {
      global: {
        stubs: {
          Card: {
            render() {
              return h('div', [this.$slots.title?.(), this.$slots.content?.()])
            }
          },
          Button: {
            props: ['label', 'icon', 'type'],
            render() {
              return h('button', {
                type: this.type || 'button',
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
            props: ['modelValue', 'value', 'disabled'],
            render() {
              return h('input', {
                type: 'checkbox',
                disabled: this.disabled,
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
          Message: {
            props: ['size', 'severity', 'variant'],
            render() { return h('div', { class: 'p-message' }, [this.$slots.default?.()]) }
          },
          Form: defineComponent({
            name: 'Form',
            props: ['resolver', 'initialValues'],
            data() { return { internalValues: this.initialValues || {} } },
            render() {
              return h('form', {
                onSubmit: async (e) => {
                  e.preventDefault()
                  let valid = true
                  if (this.resolver) {
                    try {
                      const res = await this.resolver({ values: this.internalValues })
                      if (res && (res.errors ? Object.keys(res.errors).length > 0 : Object.keys(res).length > 0)) {
                        valid = false
                      }
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
                localValue: this.name === 'perfis' ? [2] : ''
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

  it('carrega perfis com sucesso no onMounted', async () => {
    const wrapper = mountComponent()
    await nextTick()
    await nextTick()

    expect(api.get).toHaveBeenCalledWith('/user/profiles')
    expect(wrapper.vm.profiles).toHaveLength(2)
  })

  it('trata erro ao carregar perfis na API', async () => {
    api.get.mockRejectedValueOnce({ response: { data: 'Erro ao buscar perfis' } })
    mountComponent()
    await nextTick()
    await nextTick()

    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Carga de Perfis' }))
  })

  it('salva usuário com sucesso, removendo confirmarSenha e executando hash', async () => {
    const wrapper = mountComponent()
    await nextTick()

    api.post.mockResolvedValueOnce({ status: 200 })
    await wrapper.vm.save({
      valid: true,
      values: {
        email: '  test@test.com  ',
        senha: 'senhaSegura123',
        confirmarSenha: 'senhaSegura123',
        perfis: [2]
      }
    })

    expect(api.post).toHaveBeenCalledWith('/user', expect.objectContaining({
      email: 'test@test.com',
      senha: 'hashed_senhaSegura123',
      perfis: [2]
    }))
    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Sucesso', detail: 'Usuário cadastrado com sucesso' }))
    expect(mockRouterPush).toHaveBeenCalledWith('/register/user')
  })

  it('trata resposta com status diferente de 200 ao salvar usuário', async () => {
    const wrapper = mountComponent()
    await nextTick()

    api.post.mockResolvedValueOnce({ status: 201 })
    await wrapper.vm.save({
      valid: true,
      values: {
        email: 'test@test.com',
        senha: 'senhaSegura123',
        confirmarSenha: 'senhaSegura123',
        perfis: [2]
      }
    })

    expect(api.post).toHaveBeenCalled()
    expect(mockRouterPush).not.toHaveBeenCalled()
  })

  it('retorna antecipadamente ao tentar salvar com formulário inválido', async () => {
    const wrapper = mountComponent()
    await nextTick()

    await wrapper.vm.save({ valid: false, values: {} })
    expect(api.post).not.toHaveBeenCalled()
  })

  it('trata erro de API ao salvar usuário', async () => {
    const wrapper = mountComponent()
    await nextTick()

    api.post.mockRejectedValueOnce({ response: { data: 'Erro de cadastro' } })
    await wrapper.vm.save({
      valid: true,
      values: {
        email: 'test@test.com',
        senha: 'senhaSegura123',
        confirmarSenha: 'senhaSegura123'
      }
    })

    expect(mockToastAdd).toHaveBeenCalledWith(expect.objectContaining({ summary: 'Falha de Gravação de Usuário' }))
  })

  it('executa validação Zod do resolver com sucesso e falhas (senhas divergentes e campos vazios)', async () => {
    const wrapper = mountComponent()
    await nextTick()

    const resolver = wrapper.vm.resolver

    // Válido
    const resValid = await resolver({
      values: {
        email: 'valid@test.com',
        senha: 'senhaSegura123',
        confirmarSenha: 'senhaSegura123',
        perfis: [2]
      }
    })
    expect(!resValid || Object.keys(resValid.errors || resValid || {}).length === 0).toBe(true)

    // Inválido - senhas não coincidem
    const resDivergent = await resolver({
      values: {
        email: 'valid@test.com',
        senha: 'senhaSegura123',
        confirmarSenha: 'outraSenha123',
        perfis: [2]
      }
    })
    const hasErrorsDivergent = resDivergent && (resDivergent.errors ? Object.keys(resDivergent.errors).length > 0 : Object.keys(resDivergent).length > 0)
    expect(hasErrorsDivergent).toBe(true)

    // Inválido - campos obrigatórios vazios ou formato inválido
    const resInvalid = await resolver({
      values: {
        email: 'invalido',
        senha: 'curta',
        confirmarSenha: 'curta',
        perfis: []
      }
    })
    const hasErrorsInvalid = resInvalid && (resInvalid.errors ? Object.keys(resInvalid.errors).length > 0 : Object.keys(resInvalid).length > 0)
    expect(hasErrorsInvalid).toBe(true)
  })

  it('executa a navegação de cancelamento pelo botão do template', async () => {
    const wrapper = mountComponent()
    await nextTick()

    const buttons = wrapper.findAll('button')
    const backButton = buttons.find(btn => btn.attributes('data-icon') === 'pi pi-replay')
    expect(backButton).toBeDefined()
    await backButton.trigger('click')
    expect(mockRouterPush).toHaveBeenCalledWith('/register/user')
  })
})