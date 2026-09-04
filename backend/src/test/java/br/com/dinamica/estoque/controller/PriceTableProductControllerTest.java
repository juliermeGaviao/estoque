package br.com.dinamica.estoque.controller;

import br.com.dinamica.estoque.dto.PriceTableProductDto;
import br.com.dinamica.estoque.dto.PriceTableProductFilterDto;
import br.com.dinamica.estoque.service.PriceTableProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.NoSuchElementException;

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

@ExtendWith(MockitoExtension.class)
class PriceTableProductControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private PriceTableProductService service;

    @InjectMocks
    private PriceTableProductController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // --- GET /price-table-product ---

    @Test
    @DisplayName("GET /price-table-product - Deve retornar produto da tabela por id com status 200 OK")
    void get_ShouldReturnProduct_WhenIdExists() throws Exception {
        Long id = 1L;
        PriceTableProductDto dto = new PriceTableProductDto();
        dto.setId(id);

        when(service.get(id)).thenReturn(dto);

        mockMvc.perform(get("/price-table-product").param("id", id.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /price-table-product - Deve retornar status 400 Bad Request quando não encontrado")
    void get_ShouldReturnBadRequest_WhenNotFound() throws Exception {
        Long id = 99L;
        when(service.get(id)).thenThrow(new NoSuchElementException());

        mockMvc.perform(get("/price-table-product").param("id", id.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Produto na Tabela de preços não encontrado: 99"));
    }

    // --- GET /price-table-product/list ---

    @Test
    @DisplayName("GET /price-table-product/list - Deve listar produtos com ordenação ascendente padrão")
    void list_ShouldReturnPagedResponse_DefaultAscending() throws Exception {
        Page<PriceTableProductDto> pageMock = new PageImpl<>(List.of());
        when(service.list(eq(1L), eq(2L), any(Pageable.class))).thenReturn(pageMock);

        mockMvc.perform(get("/price-table-product/list")
                        .param("idTabelaPreco", "1")
                        .param("idProduto", "2")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /price-table-product/list - Deve listar produtos com ordenação descendente (sort.length > 1)")
    void list_ShouldReturnPagedResponse_DescendingSort() throws Exception {
        Page<PriceTableProductDto> pageMock = new PageImpl<>(List.of());
        when(service.list(eq(1L), eq(2L), any(Pageable.class))).thenReturn(pageMock);

        mockMvc.perform(get("/price-table-product/list")
                        .param("idTabelaPreco", "1")
                        .param("idProduto", "2")
                        .param("sort", "id,desc"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /price-table-product/list - Deve aplicar 'asc' quando direção de sort não for informada (sort.length == 1)")
    void list_ShouldDefaultToAsc_WhenSortLengthIsOne() throws Exception {
        Page<PriceTableProductDto> pageMock = new PageImpl<>(List.of());
        when(service.list(any(), any(), any(Pageable.class))).thenReturn(pageMock);

        mockMvc.perform(get("/price-table-product/list")
                        .param("sort", "id"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /price-table-product/list - Deve retornar status 500 em erro inesperado")
    void list_ShouldReturnInternalServerError_WhenRuntimeException() throws Exception {
        when(service.list(any(), any(), any(Pageable.class))).thenThrow(new RuntimeException());

        mockMvc.perform(get("/price-table-product/list"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao listar produtos nas tabelas de preços."));
    }

    // --- POST /price-table-product ---

    @Test
    @DisplayName("POST /price-table-product - Deve salvar produto na tabela e retornar status 200 OK")
    void save_ShouldReturnOk_WhenSuccess() throws Exception {
        PriceTableProductDto dto = new PriceTableProductDto();
        dto.setId(1L);

        when(service.save(any(PriceTableProductDto.class), any())).thenReturn(dto);

        mockMvc.perform(post("/price-table-product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /price-table-product - Deve retornar 400 Bad Request em NoSuchElementException")
    void save_ShouldReturnBadRequest_WhenNoSuchElementException() throws Exception {
        PriceTableProductDto dto = new PriceTableProductDto();
        dto.setId(10L);

        when(service.save(any(PriceTableProductDto.class), any())).thenThrow(new NoSuchElementException());

        mockMvc.perform(post("/price-table-product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Produto na Tabela de preços não encontrado: 10"));
    }

    @Test
    @DisplayName("POST /price-table-product - Deve retornar status 500 quando ocorrer RuntimeException")
    void save_ShouldReturnInternalServerError_WhenRuntimeException() throws Exception {
        PriceTableProductDto dto = new PriceTableProductDto();

        when(service.save(any(PriceTableProductDto.class), any())).thenThrow(new RuntimeException());

        mockMvc.perform(post("/price-table-product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao salvar produto na tabela de preços."));
    }

    // --- DELETE /price-table-product ---

    @Test
    @DisplayName("DELETE /price-table-product - Deve remover produto com sucesso")
    void delete_ShouldReturnOk_WhenSuccess() throws Exception {
        Long id = 1L;

        mockMvc.perform(delete("/price-table-product").param("id", id.toString()))
                .andExpect(status().isOk());

        verify(service).delete(id);
    }

    @Test
    @DisplayName("DELETE /price-table-product - Deve retornar status 400 em NoSuchElementException")
    void delete_ShouldReturnBadRequest_WhenNotFound() throws Exception {
        Long id = 99L;
        doThrow(new NoSuchElementException()).when(service).delete(id);

        mockMvc.perform(delete("/price-table-product").param("id", id.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Produto na Tabela de preços não encontrado: 99"));
    }

    @Test
    @DisplayName("DELETE /price-table-product - Deve retornar status 500 em erro genérico")
    void delete_ShouldReturnInternalServerError_WhenRuntimeException() throws Exception {
        Long id = 1L;
        doThrow(new RuntimeException()).when(service).delete(id);

        mockMvc.perform(delete("/price-table-product").param("id", id.toString()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao remover produto na tabela de preços."));
    }

    // --- GET /price-table-product/list-product ---

    @Test
    @DisplayName("GET /price-table-product/list-product - Deve listar produtos filtrados com ordenação descendente")
    void listProducts_ShouldReturnPagedResponse_DescendingSort() throws Exception {
        Page<PriceTableProductDto> pageMock = new PageImpl<>(List.of());
        when(service.getProductsByTable(any(PriceTableProductFilterDto.class), any(Pageable.class))).thenReturn(pageMock);

        mockMvc.perform(get("/price-table-product/list-product")
                        .param("idTabelaPreco", "1")
                        .param("nome", "Produto A")
                        .param("sort", "nome,desc"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /price-table-product/list-product - Deve listar produtos filtrados com ordenação ascendente por tamanho de sort == 1")
    void listProducts_ShouldDefaultToAsc_WhenSortLengthIsOne() throws Exception {
        Page<PriceTableProductDto> pageMock = new PageImpl<>(List.of());
        when(service.getProductsByTable(any(PriceTableProductFilterDto.class), any(Pageable.class))).thenReturn(pageMock);

        mockMvc.perform(get("/price-table-product/list-product")
                        .param("sort", "nome"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /price-table-product/list-product - Deve retornar status 500 em erro inesperado")
    void listProducts_ShouldReturnInternalServerError_WhenRuntimeException() throws Exception {
        when(service.getProductsByTable(any(PriceTableProductFilterDto.class), any(Pageable.class))).thenThrow(new RuntimeException());

        mockMvc.perform(get("/price-table-product/list-product"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao listar produtos nas tabelas de preços."));
    }

    // --- POST /price-table-product/save-prices ---

    @SuppressWarnings("unchecked")
	@Test
    @DisplayName("POST /price-table-product/save-prices - Deve registrar preços com sucesso")
    void savePrices_ShouldReturnOk_WhenSuccess() throws Exception {
        List<PriceTableProductDto> dtos = List.of(new PriceTableProductDto());

        mockMvc.perform(post("/price-table-product/save-prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtos)))
                .andExpect(status().isOk())
                .andExpect(content().string("Preços registrados"));

        verify(service).savePrices(any(List.class), any());
    }

    @SuppressWarnings("unchecked")
	@Test
    @DisplayName("POST /price-table-product/save-prices - Deve retornar status 400 se tabela ou produto não for encontrado")
    void savePrices_ShouldReturnBadRequest_WhenNoSuchElementException() throws Exception {
        List<PriceTableProductDto> dtos = List.of(new PriceTableProductDto());

        doThrow(new NoSuchElementException()).when(service).savePrices(any(List.class), any());

        mockMvc.perform(post("/price-table-product/save-prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtos)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Alguma tabela de preços ou produto não encontrado na base"));
    }

    @SuppressWarnings("unchecked")
	@Test
    @DisplayName("POST /price-table-product/save-prices - Deve retornar status 500 em erro inesperado")
    void savePrices_ShouldReturnInternalServerError_WhenRuntimeException() throws Exception {
        List<PriceTableProductDto> dtos = List.of(new PriceTableProductDto());

        doThrow(new RuntimeException()).when(service).savePrices(any(List.class), any());

        mockMvc.perform(post("/price-table-product/save-prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtos)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao salvar produto na tabela de preços."));
    }
}