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
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.dinamica.estoque.dto.SaleItemDto;
import br.com.dinamica.estoque.service.SaleItemService;

@ExtendWith(MockitoExtension.class)
class SaleItemControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private SaleItemService service;

    @InjectMocks
    private SaleItemController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /sale-item - Deve buscar item de venda por ID com sucesso")
    void get_ShouldReturnOk() throws Exception {
        Long id = 1L;
        when(service.get(id)).thenReturn(new SaleItemDto());

        mockMvc.perform(get("/sale-item").param("id", id.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /sale-item - Deve retornar 400 quando item de venda nao for encontrado")
    void get_ShouldReturn400_WhenNotFound() throws Exception {
        Long id = 99L;
        when(service.get(id)).thenThrow(new NoSuchElementException());

        mockMvc.perform(get("/sale-item").param("id", id.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Item de venda não encontrado: 99"));
    }

    @Test
    @DisplayName("GET /sale-item/list - Deve listar itens ordenando por desc quando sort.length > 1")
    void list_ShouldReturnOk_Desc() throws Exception {
        when(service.list(any(), any(), any(), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/sale-item/list")
                        .param("idVenda", "10")
                        .param("sort", "id", "desc"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /sale-item/list - Deve listar itens ordenando por asc quando sort.length > 1")
    void list_ShouldReturnOk_Asc() throws Exception {
        when(service.list(any(), any(), any(), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/sale-item/list")
                        .param("sort", "id", "asc"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /sale-item/list - Deve usar direcao padrao 'asc' quando sort.length == 1")
    void list_ShouldReturnOk_SingleSortParam() throws Exception {
        when(service.list(any(), any(), any(), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/sale-item/list")
                        .param("sort", "id"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /sale-item/list - Deve retornar 500 em erro generico na listagem")
    void list_ShouldReturn500_WhenError() throws Exception {
        when(service.list(any(), any(), any(), any(), any(Pageable.class))).thenThrow(new RuntimeException());

        mockMvc.perform(get("/sale-item/list"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao listar itens de venda."));
    }

    @Test
    @DisplayName("POST /sale-item - Deve salvar um item de venda com sucesso")
    void save_ShouldReturnOk() throws Exception {
        SaleItemDto dto = new SaleItemDto();
        when(service.save(any(SaleItemDto.class), any())).thenReturn(dto);

        mockMvc.perform(post("/sale-item")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /sale-item - Deve retornar 400 quando id do item de venda nao for encontrado")
    void save_ShouldReturn400_WhenNotFound() throws Exception {
        SaleItemDto dto = new SaleItemDto();
        dto.setId(99L);
        when(service.save(any(SaleItemDto.class), any())).thenThrow(new NoSuchElementException());

        mockMvc.perform(post("/sale-item")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Item de venda não encontrado: 99"));
    }

    @Test
    @DisplayName("POST /sale-item - Deve retornar 500 em erro generico ao salvar item")
    void save_ShouldReturn500_WhenError() throws Exception {
        SaleItemDto dto = new SaleItemDto();
        when(service.save(any(SaleItemDto.class), any())).thenThrow(new RuntimeException());

        mockMvc.perform(post("/sale-item")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao salvar item de venda."));
    }

    @Test
    @DisplayName("POST /sale-item/save-items - Deve salvar lista de itens de venda com sucesso")
    void saveItems_ShouldReturnOk() throws Exception {
        List<SaleItemDto> items = List.of(new SaleItemDto());
        when(service.save(ArgumentMatchers.<List<SaleItemDto>>any(), any())).thenReturn(items);

        mockMvc.perform(post("/sale-item/save-items")
                        .param("idVenda", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(items)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /sale-item/save-items - Deve retornar 400 em NoSuchElementException")
    void saveItems_ShouldReturn400_WhenNotFound() throws Exception {
        List<SaleItemDto> items = List.of(new SaleItemDto());
        when(service.save(ArgumentMatchers.<List<SaleItemDto>>any(), any()))
                .thenThrow(new NoSuchElementException("Item invalido"));

        mockMvc.perform(post("/sale-item/save-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(items)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Item de venda não encontrado: Item invalido"));
    }

    @Test
    @DisplayName("POST /sale-item/save-items - Deve retornar 500 em erro generico")
    void saveItems_ShouldReturn500_WhenError() throws Exception {
        List<SaleItemDto> items = List.of(new SaleItemDto());
        when(service.save(ArgumentMatchers.<List<SaleItemDto>>any(), any()))
                .thenThrow(new RuntimeException());

        mockMvc.perform(post("/sale-item/save-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(items)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao salvar item de venda."));
    }

    @Test
    @DisplayName("DELETE /sale-item - Deve remover item de venda com sucesso")
    void delete_ShouldReturnOk() throws Exception {
        Long id = 1L;

        mockMvc.perform(delete("/sale-item").param("id", id.toString()))
                .andExpect(status().isOk());

        verify(service).delete(eq(id), any());
    }

    @Test
    @DisplayName("DELETE /sale-item - Deve retornar 400 quando item nao for encontrado ao deletar")
    void delete_ShouldReturn400_WhenNotFound() throws Exception {
        Long id = 99L;
        doThrow(new NoSuchElementException()).when(service).delete(eq(id), any());

        mockMvc.perform(delete("/sale-item").param("id", id.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Item de venda não encontrado: 99"));
    }

    @Test
    @DisplayName("DELETE /sale-item - Deve retornar 500 em erro generico ao deletar")
    void delete_ShouldReturn500_WhenError() throws Exception {
        Long id = 1L;
        doThrow(new RuntimeException()).when(service).delete(eq(id), any());

        mockMvc.perform(delete("/sale-item").param("id", id.toString()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao remover item de venda."));
    }

    @Test
    @DisplayName("GET /sale-item/list-by-price-table - Deve listar por tabela de preco e ponto de venda")
    void getItensByPriceTableAndSalePoint_ShouldReturnOk() throws Exception {
        when(service.getItensByPriceTableAndSalePoint(1L, 2L)).thenReturn(List.of());

        mockMvc.perform(get("/sale-item/list-by-price-table")
                        .param("idTabelaPreco", "1")
                        .param("idPontoVenda", "2"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /sale-item/list-by-price-table - Deve retornar 500 em erro generico")
    void getItensByPriceTableAndSalePoint_ShouldReturn500_WhenError() throws Exception {
        when(service.getItensByPriceTableAndSalePoint(1L, 2L)).thenThrow(new RuntimeException());

        mockMvc.perform(get("/sale-item/list-by-price-table")
                        .param("idTabelaPreco", "1")
                        .param("idPontoVenda", "2"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao listar por tabela de preços itens de venda."));
    }

    @Test
    @DisplayName("GET /sale-item/list-by-sale - Deve listar por ID da venda")
    void listBySale_ShouldReturnOk() throws Exception {
        when(service.getItensBySale(10L)).thenReturn(List.of());

        mockMvc.perform(get("/sale-item/list-by-sale").param("idVenda", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /sale-item/list-by-sale - Deve retornar 500 em erro generico")
    void listBySale_ShouldReturn500_WhenError() throws Exception {
        when(service.getItensBySale(10L)).thenThrow(new RuntimeException());

        mockMvc.perform(get("/sale-item/list-by-sale").param("idVenda", "10"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao listar por venda itens de venda."));
    }
}