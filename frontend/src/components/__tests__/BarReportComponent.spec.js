// @vitest-environment jsdom
import api from '@/util/api'
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import BarReportComponent from '../BarReportComponent.vue'

const mockToastAdd = vi.fn()

vi.mock('primevue/usetoast', () => ({
  useToast: () => ({
    add: mockToastAdd
  })
}))

vi.mock('@/util/api', () => ({
  default: {
    get: vi.fn()
  }
}))

vi.mock('@/util/util', () => ({
  formatNumber: vi.fn((val) => `R$ ${val}`)
}))

const globalStubs = {
  Card: { template: '<div><slot name="title" /><slot name="content" /></div>' },
  FloatLabel: { template: '<div><slot /></div>' },
  Select: {
    props: ['modelValue', 'options'],
    template: `
      <select 
        :value="modelValue" 
        @change="$emit('update:modelValue', Number($event.target.value))"
      >
        <option v-for="opt in options" :key="opt.id" :value="opt.id">
          {{ opt.label }}
        </option>
      </select>
    `
  },
  Chart: {
    props: ['type', 'data', 'options'],
    template: '<div class="p-chart" />'
  }
}

describe('BarReportComponent.vue', () => {
  const mockApiData = [
    {
      grupo: 'Jan/2026',
      subGrupos: [
        {
          grupo: 'Vendedor A',
          indicadores: { quantidadeVendas: 10, mediaTotal: 100, somaTotal: 1000 }
        },
        {
          grupo: 'Vendedor B',
          indicadores: { quantidadeVendas: 5, mediaTotal: 50, somaTotal: 250 }
        }
      ]
    },
    {
      grupo: 'Fev/2026',
      subGrupos: [
        {
          grupo: 'Vendedor A',
          indicadores: { quantidadeVendas: 12, mediaTotal: 110, somaTotal: 1320 }
        }
        // Vendedor B omitido intencionalmente para testar preenchimento com null
      ]
    }
  ]

  beforeEach(() => {
    vi.clearAllMocks()
    api.get.mockResolvedValue({ data: mockApiData })
  })

  it('deve carregar os dados do relatório no mount com props padrão e frequência inicial', async () => {
    const wrapper = mount(BarReportComponent, {
      props: { title: 'Relatório Geral', endpoint: 'vendedores' },
      global: { stubs: globalStubs }
    })

    await flushPromises()

    expect(api.get).toHaveBeenCalledWith('/report/vendedores', {
      params: { frequency: 1 }
    })
    expect(wrapper.find('h3').text()).toBe('Relatório Geral')
  })

  it('deve formatar valores corretamente através dos formatters das opções dos gráficos', async () => {
    const wrapper = mount(BarReportComponent, {
      props: { title: 'Test Formatter', endpoint: 'test' },
      global: { stubs: globalStubs }
    })

    await flushPromises()

    const averageFormatter = wrapper.vm.averageReportOptions.plugins.datalabels.formatter
    const totalFormatter = wrapper.vm.totalReportOptions.plugins.datalabels.formatter

    expect(averageFormatter(150)).toBe('R$ 150')
    expect(totalFormatter(3000)).toBe('R$ 3000')
  })

  it('deve preencher valores nulos para subgrupos ausentes em determinados períodos e mapear dataset corretamente', async () => {
    const wrapper = mount(BarReportComponent, {
      props: { title: 'Test Datasets', endpoint: 'test' },
      global: { stubs: globalStubs }
    })

    await flushPromises()

    const quantityData = wrapper.vm.quantityReport
    expect(quantityData.labels).toEqual(['Jan/2026', 'Fev/2026'])
    expect(quantityData.datasets.length).toBe(2)

    const vendedorBDataset = quantityData.datasets.find((ds) => ds.label === 'Vendedor B')
    expect(vendedorBDataset.data).toEqual([5, null])
  })

  it('deve recarregar o relatório ao alterar a periodicidade no Select', async () => {
    const wrapper = mount(BarReportComponent, {
      props: { title: 'Vendas', endpoint: 'vendas' },
      global: { stubs: globalStubs }
    })

    await flushPromises()

    const select = wrapper.find('select')
    await select.setValue('2')

    await flushPromises()

    expect(wrapper.vm.frequency).toBe(2)
    expect(api.get).toHaveBeenLastCalledWith('/report/vendas', {
      params: { frequency: 2 }
    })
  })

  it('deve exibir toast de erro quando a chamada da API falhar', async () => {
    api.get.mockRejectedValueOnce({
      response: { data: 'Erro de banco de dados' }
    })

    mount(BarReportComponent, {
      props: { title: 'Erro Test', endpoint: 'erro' },
      global: { stubs: globalStubs }
    })

    await flushPromises()

    expect(mockToastAdd).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'error',
        summary: 'Falha de Carga de Relatório de Vendedores',
        detail: 'Requisição de Relatório de Vendedores terminou com o erro: Erro de banco de dados'
      })
    )
  })

  it('deve cobrir valores default de props quando não informados', async () => {
    const wrapper = mount(BarReportComponent, {
      global: { stubs: globalStubs }
    })

    await flushPromises()

    expect(wrapper.props().title).toBeNull()
    expect(wrapper.props().endpoint).toBeNull()
    expect(api.get).toHaveBeenCalledWith('/report/null', { params: { frequency: 1 } })
  })
})