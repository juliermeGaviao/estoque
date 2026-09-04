package br.com.dinamica.estoque.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import br.com.dinamica.estoque.dto.ProductDto;
import br.com.dinamica.estoque.dto.SalePointDto;
import br.com.dinamica.estoque.dto.StockDto;
import br.com.dinamica.estoque.dto.StockProductDto;
import br.com.dinamica.estoque.dto.StockTransferDto;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.service.StockService;
import br.com.dinamica.estoque.service.StockTransferService;

@ExtendWith(MockitoExtension.class)
class StockTransferControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private StockTransferService service;

    @Mock
    private StockService stockService;

    @InjectMocks
    private StockTransferController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /stock-transfer - Deve obter transferencia por ID")
    void get_ShouldReturnOk() throws Exception {
        Long id = 1L;
        when(service.get(id)).thenReturn(new StockTransferDto());

        mockMvc.perform(get("/stock-transfer").param("id", id.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /stock-transfer - Deve retornar 400 em NoSuchElementException")
    void get_ShouldReturn400_WhenNotFound() throws Exception {
        Long id = 99L;
        when(service.get(id)).thenThrow(new NoSuchElementException());

        mockMvc.perform(get("/stock-transfer").param("id", id.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Transferência de estoque não encontrado: 99"));
    }

    @Test
    @DisplayName("GET /stock-transfer/list - Deve listar transferencias com ordenacao asc padrao")
    void list_ShouldReturnOk_Asc() throws Exception {
        when(service.list(any(), any(), any(), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/stock-transfer/list")
                        .param("idPontoVendaOrigem", "1")
                        .param("idPontoVendaDestino", "2")
                        .param("sort", "id"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /stock-transfer/list - Deve listar transferencias com ordenacao desc explícita")
    void list_ShouldReturnOk_Desc() throws Exception {
        when(service.list(any(), any(), any(), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/stock-transfer/list")
                        .param("sort", "id", "desc"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /stock-transfer/list - Deve retornar 500 em caso de RuntimeException")
    void list_ShouldReturn500_WhenServerError() throws Exception {
        when(service.list(any(), any(), any(), any(), any(Pageable.class))).thenThrow(new RuntimeException());

        mockMvc.perform(get("/stock-transfer/list"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao listar transferências de estoque."));
    }

    @Test
    @DisplayName("POST /stock-transfer - Deve salvar e realizar a transferencia de estoque")
    void save_ShouldReturnOkAndTransferStock() throws Exception {
        StockProductDto itemEstoque = new StockProductDto();
        itemEstoque.setIdProduto(10L);
        itemEstoque.setQuantidade(5);

        StockTransferDto dtoInput = new StockTransferDto();
        dtoInput.setEstoque(List.of(itemEstoque));

        SalePointDto origem = new SalePointDto();
        origem.setId(1L);

        SalePointDto destino = new SalePointDto();
        destino.setId(2L);

        StockTransferDto dtoSaved = new StockTransferDto();
        dtoSaved.setId(100L);
        dtoSaved.setPontoVendaOrigem(origem);
        dtoSaved.setPontoVendaDestino(destino);

        when(service.save(any(StockTransferDto.class), any(Usuario.class))).thenReturn(dtoSaved);

        mockMvc.perform(post("/stock-transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoInput)))
                .andExpect(status().isOk());

        verify(stockService).transferStock(eq(10L), eq(1L), eq(2L), eq(100L), eq(5), any(Usuario.class));
    }

    @Test
    @DisplayName("POST /stock-transfer - Deve retornar 400 quando id nao for encontrado")
    void save_ShouldReturn400_WhenNotFound() throws Exception {
        StockTransferDto dtoInput = new StockTransferDto();
        dtoInput.setId(99L);
        dtoInput.setEstoque(List.of());

        when(service.save(any(StockTransferDto.class), any(Usuario.class))).thenThrow(new NoSuchElementException());

        mockMvc.perform(post("/stock-transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoInput)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Transferência de estoque não encontrado: 99"));
    }

    @Test
    @DisplayName("POST /stock-transfer - Deve retornar 500 em caso de RuntimeException")
    void save_ShouldReturn500_WhenServerError() throws Exception {
        StockTransferDto dtoInput = new StockTransferDto();
        dtoInput.setEstoque(List.of());

        when(service.save(any(StockTransferDto.class), any(Usuario.class))).thenThrow(new RuntimeException());

        mockMvc.perform(post("/stock-transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoInput)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao salvar transferência de estoque."));
    }

    @Test
    @DisplayName("DELETE /stock-transfer - Deve deletar transferencia com sucesso")
    void delete_ShouldReturnOk() throws Exception {
        Long id = 1L;

        mockMvc.perform(delete("/stock-transfer").param("id", id.toString()))
                .andExpect(status().isOk());

        verify(service).delete(id);
    }

    @Test
    @DisplayName("DELETE /stock-transfer - Deve retornar 400 em NoSuchElementException")
    void delete_ShouldReturn400_WhenNotFound() throws Exception {
        Long id = 99L;
        doThrow(new NoSuchElementException()).when(service).delete(id);

        mockMvc.perform(delete("/stock-transfer").param("id", id.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Transferência de estoque não encontrado: 99"));
    }

    @Test
    @DisplayName("DELETE /stock-transfer - Deve retornar 500 em RuntimeException")
    void delete_ShouldReturn500_WhenServerError() throws Exception {
        Long id = 1L;
        doThrow(new RuntimeException()).when(service).delete(id);

        mockMvc.perform(delete("/stock-transfer").param("id", id.toString()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao remover transferência de estoque."));
    }

    @Test
    @DisplayName("GET /stock-transfer/list-products - Deve listar produtos disponiveis para transferencia")
    void listProducts_ShouldReturnOk() throws Exception {
        Long idOrigem = 1L;
        Long idDestino = 2L;

        ProductDto produto = new ProductDto();
        produto.setId(10L);

        StockDto stockDto = new StockDto();
        stockDto.setProduto(produto);
        stockDto.setSaldo(20);

        when(stockService.getStockBySalePoint(idOrigem)).thenReturn(List.of(stockDto));
        when(stockService.getStockSalePoint(10L, idDestino)).thenReturn(5);

        mockMvc.perform(get("/stock-transfer/list-products")
                        .param("idPontoVendaOrigem", idOrigem.toString())
                        .param("idPontoVendaDestingo", idDestino.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /stock-transfer/list-products - Deve retornar 500 em RuntimeException")
    void listProducts_ShouldReturn500_WhenServerError() throws Exception {
        when(stockService.getStockBySalePoint(any())).thenThrow(new RuntimeException());

        mockMvc.perform(get("/stock-transfer/list-products")
                        .param("idPontoVendaOrigem", "1")
                        .param("idPontoVendaDestingo", "2"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao listar produtos."));
    }

    @Test
    @DisplayName("GET /stock-transfer/list-sale-point-products - Deve listar produtos vinculados a transferencia")
    void listPurchaseOrderProducts_ShouldReturnOk() throws Exception {
        Long idTransferencia = 100L;

        ProductDto produto = new ProductDto();
        produto.setId(10L);

        StockDto stockDto = new StockDto();
        stockDto.setProduto(produto);
        stockDto.setQuantidade(10);

        when(stockService.getStockTransferProducts(idTransferencia)).thenReturn(List.of(stockDto));

        mockMvc.perform(get("/stock-transfer/list-sale-point-products")
                        .param("idTransferenciaEstoque", idTransferencia.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /stock-transfer/list-sale-point-products - Deve retornar 500 em RuntimeException")
    void listPurchaseOrderProducts_ShouldReturn500_WhenServerError() throws Exception {
        when(stockService.getStockTransferProducts(any())).thenThrow(new RuntimeException());

        mockMvc.perform(get("/stock-transfer/list-sale-point-products")
                        .param("idTransferenciaEstoque", "1"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao listar produtos."));
    }
}