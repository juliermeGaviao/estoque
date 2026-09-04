package br.com.dinamica.estoque.controller;

import br.com.dinamica.estoque.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ReportService service;

    @InjectMocks
    private ReportController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /report/sale-report - Deve retornar relatorio de vendas")
    void getSaleReport_ShouldReturnOk() throws Exception {
        when(service.getSaleReport(30)).thenReturn(List.of());

        mockMvc.perform(get("/report/sale-report").param("frequency", "30"))
                .andExpect(status().isOk());

        verify(service).getSaleReport(30);
    }

    @Test
    @DisplayName("GET /report/salesman-report - Deve retornar relatorio de vendedores")
    void getSalesmanReport_ShouldReturnOk() throws Exception {
        when(service.getSalesmanReport(30)).thenReturn(List.of());

        mockMvc.perform(get("/report/salesman-report").param("frequency", "30"))
                .andExpect(status().isOk());

        verify(service).getSalesmanReport(30);
    }

    @Test
    @DisplayName("GET /report/product-type-report - Deve retornar relatorio por tipo de produto")
    void getProductTypeReport_ShouldReturnOk() throws Exception {
        when(service.getProductTypeReport(30)).thenReturn(List.of());

        mockMvc.perform(get("/report/product-type-report").param("frequency", "30"))
                .andExpect(status().isOk());

        verify(service).getProductTypeReport(30);
    }

    @Test
    @DisplayName("GET /report/company-report - Deve retornar relatorio por empresa")
    void getCompanyReport_ShouldReturnOk() throws Exception {
        when(service.getCompanyReport(30)).thenReturn(List.of());

        mockMvc.perform(get("/report/company-report").param("frequency", "30"))
                .andExpect(status().isOk());

        verify(service).getCompanyReport(30);
    }

    @Test
    @DisplayName("GET /report/provider-report - Deve retornar relatorio por fornecedor")
    void getProviderReport_ShouldReturnOk() throws Exception {
        when(service.getProviderReport(30)).thenReturn(List.of());

        mockMvc.perform(get("/report/provider-report").param("frequency", "30"))
                .andExpect(status().isOk());

        verify(service).getProviderReport(30);
    }
}