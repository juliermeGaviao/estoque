package br.com.dinamica.estoque.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.dinamica.estoque.dto.ReportGroupDTO;
import br.com.dinamica.estoque.repository.VendaRepository;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock
    private VendaRepository vendaRepository;

    private ReportServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReportServiceImpl(vendaRepository);
    }

    // -------------------------------------------------------------------------
    // getSaleReport(Integer frequency)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getSaleReport - deve processar relatorio diario (frequencia 1) com múltiplos registros no mesmo periodo")
    void getSaleReport_daily() {
        LocalDate data = LocalDate.of(2026, 3, 1);
        Object[] reg1 = new Object[] { data, 15L, new BigDecimal("200.00"), new BigDecimal("20.00") };
        Object[] reg2 = new Object[] { data, 5L, new BigDecimal("100.00"), new BigDecimal("10.00") };
        List<Object[]> mockRecords = List.<Object[]>of(reg1, reg2);

        when(vendaRepository.findRelatorioVendaDiario()).thenReturn(mockRecords);

        List<ReportGroupDTO> result = service.getSaleReport(1);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("01/03/2026", result.get(0).getGrupo());
        assertEquals(5L, result.get(0).getIndicadores().getQuantidadeVendas());
        verify(vendaRepository).findRelatorioVendaDiario();
    }

    @Test
    @DisplayName("getSaleReport - deve processar relatorio semanal (frequencia 2)")
    void getSaleReport_weekly() {
        LocalDate data = LocalDate.of(2026, 3, 2);
        Object[] registro = new Object[] { data, 5L, new BigDecimal("500.00"), new BigDecimal("50.00") };
        List<Object[]> mockRecords = List.<Object[]>of(registro);

        when(vendaRepository.findRelatorioVendaSemanal()).thenReturn(mockRecords);

        List<ReportGroupDTO> result = service.getSaleReport(2);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("02/03/2026", result.get(0).getGrupo());
        verify(vendaRepository).findRelatorioVendaSemanal();
    }

    @Test
    @DisplayName("getSaleReport - deve processar relatorio mensal (frequencia 3)")
    void getSaleReport_monthly() {
        Object[] registro = new Object[] { "03/2026", 20L, new BigDecimal("1000.00"), new BigDecimal("100.00") };
        List<Object[]> mockRecords = List.<Object[]>of(registro);

        when(vendaRepository.findRelatorioVendaMensal()).thenReturn(mockRecords);

        List<ReportGroupDTO> result = service.getSaleReport(3);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("03/2026", result.get(0).getGrupo());
        verify(vendaRepository).findRelatorioVendaMensal();
    }

    @Test
    @DisplayName("getSaleReport - deve retornar lista vazia para frequencia invalida")
    void getSaleReport_invalidFrequency() {
        List<ReportGroupDTO> result = service.getSaleReport(99);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // -------------------------------------------------------------------------
    // getSalesmanReport(Integer frequency)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getSalesmanReport - deve processar relatorio diario (frequencia 1)")
    void getSalesmanReport_daily() {
        LocalDate data = LocalDate.of(2026, 3, 1);
        Object[] reg = new Object[] { data, "vendedor@test.com", 2L, new BigDecimal("50.00"), new BigDecimal("5.00") };
        when(vendaRepository.findRelatorioVendedorDiario()).thenReturn(List.<Object[]>of(reg));

        List<ReportGroupDTO> res = service.getSalesmanReport(1);

        assertEquals(1, res.size());
        assertEquals("01/03/2026", res.get(0).getGrupo());
        assertEquals("vendedor@test.com", res.get(0).getSubGrupos().get(0).getGrupo());
    }

    @Test
    @DisplayName("getSalesmanReport - deve processar relatorio semanal (frequencia 2)")
    void getSalesmanReport_weekly() {
        LocalDate data = LocalDate.of(2026, 3, 1);
        Object[] reg = new Object[] { data, "vendedor@test.com", 2L, new BigDecimal("50.00"), new BigDecimal("5.00") };
        when(vendaRepository.findRelatorioVendedorSemanal()).thenReturn(List.<Object[]>of(reg));

        List<ReportGroupDTO> res = service.getSalesmanReport(2);

        assertEquals(1, res.size());
    }

    @Test
    @DisplayName("getSalesmanReport - deve processar relatorio mensal (frequencia 3)")
    void getSalesmanReport_monthly() {
        Object[] reg = new Object[] { "03/2026", "vendedor@test.com", 8L, new BigDecimal("200.00"), new BigDecimal("20.00") };
        when(vendaRepository.findRelatorioVendedorMensal()).thenReturn(List.<Object[]>of(reg));

        List<ReportGroupDTO> res = service.getSalesmanReport(3);

        assertEquals(1, res.size());
        assertEquals("03/2026", res.get(0).getGrupo());
    }

    @Test
    @DisplayName("getSalesmanReport - deve retornar lista vazia para frequencia invalida")
    void getSalesmanReport_invalidFrequency() {
        List<ReportGroupDTO> res = service.getSalesmanReport(99);

        assertNotNull(res);
        assertTrue(res.isEmpty());
    }

    // -------------------------------------------------------------------------
    // getProductTypeReport(Integer frequency)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getProductTypeReport - deve processar relatorio diario (frequencia 1)")
    void getProductTypeReport_daily() {
        LocalDate data = LocalDate.of(2026, 3, 1);
        Object[] reg1 = new Object[] { data, "Eletronicos", 3L, new BigDecimal("300.00"), new BigDecimal("30.00") };
        Object[] reg2 = new Object[] { data, "Vestuario", 1L, new BigDecimal("50.00"), new BigDecimal("5.00") };

        when(vendaRepository.findRelatorioTipoProdutoDiario()).thenReturn(List.<Object[]>of(reg1, reg2));

        List<ReportGroupDTO> res = service.getProductTypeReport(1);

        assertEquals(1, res.size());
        assertEquals(2, res.get(0).getSubGrupos().size());
    }

    @Test
    @DisplayName("getProductTypeReport - deve processar relatorio semanal (frequencia 2)")
    void getProductTypeReport_weekly() {
        LocalDate data = LocalDate.of(2026, 3, 1);
        Object[] reg1 = new Object[] { data, "Eletronicos", 3L, new BigDecimal("300.00"), new BigDecimal("30.00") };

        when(vendaRepository.findRelatorioTipoProdutoSemanal()).thenReturn(List.<Object[]>of(reg1));

        List<ReportGroupDTO> res = service.getProductTypeReport(2);

        assertEquals(1, res.size());
    }

    @Test
    @DisplayName("getProductTypeReport - deve processar relatorio mensal (frequencia 3)")
    void getProductTypeReport_monthly() {
        Object[] reg = new Object[] { "03/2026", "Eletronicos", 3L, new BigDecimal("300.00"), new BigDecimal("30.00") };

        when(vendaRepository.findRelatorioTipoProdutoMensal()).thenReturn(List.<Object[]>of(reg));

        List<ReportGroupDTO> res = service.getProductTypeReport(3);

        assertEquals(1, res.size());
    }

    @Test
    @DisplayName("getProductTypeReport - deve retornar lista vazia para frequencia invalida")
    void getProductTypeReport_invalidFrequency() {
        List<ReportGroupDTO> res = service.getProductTypeReport(99);

        assertNotNull(res);
        assertTrue(res.isEmpty());
    }

    // -------------------------------------------------------------------------
    // getCompanyReport(Integer frequency)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getCompanyReport - deve processar relatorio diario (frequencia 1)")
    void getCompanyReport_daily() {
        LocalDate data = LocalDate.of(2026, 3, 1);
        Object[] reg = new Object[] { data, "Empresa X", 10L, new BigDecimal("1000.00"), new BigDecimal("100.00") };

        when(vendaRepository.findRelatorioEmpresaDiario()).thenReturn(List.<Object[]>of(reg));

        List<ReportGroupDTO> res = service.getCompanyReport(1);

        assertEquals(1, res.size());
    }

    @Test
    @DisplayName("getCompanyReport - deve processar relatorio semanal (frequencia 2)")
    void getCompanyReport_weekly() {
        LocalDate data = LocalDate.of(2026, 3, 1);
        Object[] reg = new Object[] { data, "Empresa X", 10L, new BigDecimal("1000.00"), new BigDecimal("100.00") };

        when(vendaRepository.findRelatorioEmpresaSemanal()).thenReturn(List.<Object[]>of(reg));

        List<ReportGroupDTO> res = service.getCompanyReport(2);

        assertEquals(1, res.size());
    }

    @Test
    @DisplayName("getCompanyReport - deve processar relatorio mensal (frequencia 3)")
    void getCompanyReport_monthly() {
        Object[] reg = new Object[] { "03/2026", "Empresa X", 10L, new BigDecimal("1000.00"), new BigDecimal("100.00") };

        when(vendaRepository.findRelatorioEmpresaMensal()).thenReturn(List.<Object[]>of(reg));

        List<ReportGroupDTO> res = service.getCompanyReport(3);

        assertEquals(1, res.size());
    }

    @Test
    @DisplayName("getCompanyReport - deve retornar lista vazia para frequencia invalida")
    void getCompanyReport_invalidFrequency() {
        List<ReportGroupDTO> res = service.getCompanyReport(99);

        assertNotNull(res);
        assertTrue(res.isEmpty());
    }

    // -------------------------------------------------------------------------
    // getProviderReport(Integer frequency)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getProviderReport - deve processar relatorio diario (frequencia 1)")
    void getProviderReport_daily() {
        LocalDate data = LocalDate.of(2026, 3, 1);
        Object[] reg = new Object[] { data, "Fornecedor Y", 4L, new BigDecimal("400.00"), new BigDecimal("40.00") };

        when(vendaRepository.findRelatorioFornecedorDiario()).thenReturn(List.<Object[]>of(reg));

        List<ReportGroupDTO> res = service.getProviderReport(1);

        assertEquals(1, res.size());
    }

    @Test
    @DisplayName("getProviderReport - deve processar relatorio semanal (frequencia 2)")
    void getProviderReport_weekly() {
        LocalDate data = LocalDate.of(2026, 3, 1);
        Object[] reg = new Object[] { data, "Fornecedor Y", 4L, new BigDecimal("400.00"), new BigDecimal("40.00") };

        when(vendaRepository.findRelatorioFornecedorSemanal()).thenReturn(List.<Object[]>of(reg));

        List<ReportGroupDTO> res = service.getProviderReport(2);

        assertEquals(1, res.size());
    }

    @Test
    @DisplayName("getProviderReport - deve processar relatorio mensal (frequencia 3)")
    void getProviderReport_monthly() {
        Object[] reg = new Object[] { "03/2026", "Fornecedor Y", 4L, new BigDecimal("400.00"), new BigDecimal("40.00") };

        when(vendaRepository.findRelatorioFornecedorMensal()).thenReturn(List.<Object[]>of(reg));

        List<ReportGroupDTO> res = service.getProviderReport(3);

        assertEquals(1, res.size());
    }

    @Test
    @DisplayName("getProviderReport - deve retornar lista vazia para frequencia invalida")
    void getProviderReport_invalidFrequency() {
        List<ReportGroupDTO> res = service.getProviderReport(99);

        assertNotNull(res);
        assertTrue(res.isEmpty());
    }
}