import BarReportComponent from '@/components/BarReportComponent.vue'
import api from '@/util/api'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'
import Home from '../Home.vue'

const mockToastAdd = vi.fn()

vi.mock('primevue/usetoast', () => ({
  useToast: () => ({ add: mockToastAdd })
}))

vi.mock('@/util/api', () => ({
  default: {
    get: vi.fn()
  }
}))

vi.mock('@/util/util', () => ({
  formatNumber: vi.fn((val) => `R$ ${val}`)
}))

describe('Home.vue', () => {
  const mockReportResponse = {
    data: [
      {
        grupo: '2026-01',
        indicadores: {
          quantidadeVendas: 15,
          mediaTotal: 150.5,
          somaTotal: 2257.5
        }
      },
      {
        grupo: '2026-02',
        indicadores: {
          quantidadeVendas: 20,
          mediaTotal: 200,
          somaTotal: 4000
        }
      }
    ]
  }

  beforeEach(() => {
    vi.clearAllMocks()
    api.get.mockResolvedValue(mockReportResponse)
  })

  function mountComponent() {
    return mount(Home, {
      global: {
        stubs: {
          Card: {
            template: '<div><slot name="title" /><slot name="content" /></div>'
          },
          FloatLabel: {
            template: '<div><slot /></div>'
          },
          Select: {
            props: ['modelValue', 'options'],
            template: `
              <select :value="modelValue" @change="$emit('update:modelValue', Number($event.target.value))">
                <option v-for="opt in options" :key="opt.id" :value="opt.id">
                  {{ opt.label }}
                </option>
              </select>
            `
          },
          Chart: {
            props: ['type', 'data', 'options'],
            template: '<div class="chart-stub" />'
          },
          BarReportComponent: {
            name: 'BarReportComponent',
            props: ['title', 'endpoint'],
            template: '<div class="bar-report-stub" />'
          }
        }
      }
    })
  }

  it('carrega o relatório de vendas no onMounted e popula os gráficos', async () => {
    const wrapper = mountComponent()
    await nextTick()

    expect(api.get).toHaveBeenCalledWith('/report/sale-report', {
      params: { frequency: 1 }
    })

    // Valida se os dados foram estruturados nos relatórios de gráfico
    expect(wrapper.vm.salesQuantityReport.labels).toEqual(['2026-01', '2026-02'])
    expect(wrapper.vm.salesQuantityReport.datasets[0].data).toEqual([15, 20])

    expect(wrapper.vm.salesAverageReport.labels).toEqual(['2026-01', '2026-02'])
    expect(wrapper.vm.salesAverageReport.datasets[0].data).toEqual([150.5, 200])

    expect(wrapper.vm.salesTotalReport.labels).toEqual(['2026-01', '2026-02'])
    expect(wrapper.vm.salesTotalReport.datasets[0].data).toEqual([2257.5, 4000])
  })

  it('executa as funções de formatação (formatter) presentes nas opções dos gráficos', async () => {
    const wrapper = mountComponent()
    await nextTick()

    const avgFormatter = wrapper.vm.salesAverageReportOptions.plugins.datalabels.formatter
    const totalFormatter = wrapper.vm.salesTotalReportOptions.plugins.datalabels.formatter

    expect(avgFormatter(100)).toBe('R$ 100')
    expect(totalFormatter(500)).toBe('R$ 500')
  })

  it('recarrega o relatório ao alterar a frequência no Select', async () => {
    const wrapper = mountComponent()
    await nextTick()

    api.get.mockClear()

    const select = wrapper.find('select')
    await select.setValue('3')

    expect(api.get).toHaveBeenCalledWith('/report/sale-report', {
      params: { frequency: 3 }
    })
  })

  it('exibe um toast de erro se a requisição do relatório falhar', async () => {
    api.get.mockRejectedValueOnce({
      response: { data: 'Erro de Banco de Dados' }
    })

    mountComponent()
    await nextTick()

    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'error',
        summary: 'Falha de Carga de Relatório de Vendas',
        detail: 'Requisição de Relatório de Vendas terminou com o erro: Erro de Banco de Dados'
      })
    )
  })

  it('renderiza os componentes BarReportComponent com seus parâmetros corretos', async () => {
    const wrapper = mountComponent()
    await nextTick()

    const barReports = wrapper.findAllComponents(BarReportComponent)
    expect(barReports).toHaveLength(4)

    expect(barReports[0].props()).toEqual({
      title: 'Relatório de Vendedores',
      endpoint: 'salesman-report'
    })
    expect(barReports[1].props()).toEqual({
      title: 'Relatório por Tipo de Produto',
      endpoint: 'product-type-report'
    })
    expect(barReports[2].props()).toEqual({
      title: 'Relatório de Empresas',
      endpoint: 'company-report'
    })
    expect(barReports[3].props()).toEqual({
      title: 'Relatório de Fornecedores',
      endpoint: 'provider-report'
    })
  })
})