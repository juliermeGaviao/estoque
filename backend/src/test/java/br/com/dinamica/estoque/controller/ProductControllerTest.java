package br.com.dinamica.estoque.controller;

import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.dinamica.estoque.dto.ProductDto;
import br.com.dinamica.estoque.dto.ProductFilterDto;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.service.ProductService;
import br.com.dinamica.estoque.service.StockService;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ProductService service;

    @Mock
    private StockService stockService;

    @InjectMocks
    private ProductController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /product - Deve retornar produto quando id existe")
    void get_ShouldReturnProduct_WhenExists() throws Exception {
        Long id = 1L;
        ProductDto dto = new ProductDto();
        dto.setId(id);

        when(service.get(id)).thenReturn(dto);

        mockMvc.perform(get("/product").param("id", id.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /product - Deve retornar 400 em NoSuchElementException")
    void get_ShouldReturnBadRequest_WhenNotFound() throws Exception {
        Long id = 99L;
        when(service.get(id)).thenThrow(new NoSuchElementException());

        mockMvc.perform(get("/product").param("id", id.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Produto não encontrado: 99"));
    }

    @Test
    @DisplayName("GET /product/list - Deve listar produtos paginados e ordenados desc")
    void list_ShouldReturnPagedResponse_Desc() throws Exception {
        Page<ProductDto> pageMock = new PageImpl<>(List.of());
        when(service.list(any(ProductFilterDto.class), any(Pageable.class))).thenReturn(pageMock);

        mockMvc.perform(get("/product/list")
                        .param("nome", "Camisa")
                        .param("sort", "nome,desc"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /product/list - Deve listar produtos paginados e ordenados asc (parametro simples)")
    void list_ShouldReturnPagedResponse_Asc() throws Exception {
        Page<ProductDto> pageMock = new PageImpl<>(List.of());
        when(service.list(any(ProductFilterDto.class), any(Pageable.class))).thenReturn(pageMock);

        mockMvc.perform(get("/product/list")
                        .param("nome", "Camisa")
                        .param("sort", "nome"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /product/list - Deve retornar 500 em erro generico")
    void list_ShouldReturn500_WhenError() throws Exception {
        when(service.list(any(ProductFilterDto.class), any(Pageable.class))).thenThrow(new RuntimeException());

        mockMvc.perform(get("/product/list"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao listar produtos."));
    }

    @Test
    @DisplayName("POST /product - Deve salvar produto")
    void save_ShouldReturnOk() throws Exception {
        ProductDto dto = new ProductDto();
        dto.setId(1L);

        when(service.save(any(ProductDto.class), any(Usuario.class))).thenReturn(dto);

        mockMvc.perform(post("/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /product - Deve retornar 400 quando id nao for encontrado")
    void save_ShouldReturn400_WhenNotFound() throws Exception {
        ProductDto dto = new ProductDto();
        dto.setId(10L);

        when(service.save(any(ProductDto.class), any(Usuario.class))).thenThrow(new NoSuchElementException());

        mockMvc.perform(post("/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Produto não encontrado: 10"));
    }

    @Test
    @DisplayName("POST /product - Deve retornar 500 em erro generico")
    void save_ShouldReturn500_WhenError() throws Exception {
        ProductDto dto = new ProductDto();

        when(service.save(any(ProductDto.class), any(Usuario.class))).thenThrow(new RuntimeException());

        mockMvc.perform(post("/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao salvar produto."));
    }

    @Test
    @DisplayName("POST /product/save-all - Deve salvar lista de produtos")
    void saveAll_ShouldReturnOk() throws Exception {
        List<ProductDto> dtos = List.of(new ProductDto());

        mockMvc.perform(post("/product/save-all")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtos)))
                .andExpect(status().isOk());

        verify(service).save(ArgumentMatchers.<List<ProductDto>>any(), any(Usuario.class));
    }

    @Test
    @DisplayName("POST /product/save-all - Deve retornar 500 em erro generico")
    void saveAll_ShouldReturn500_WhenError() throws Exception {
        List<ProductDto> dtos = List.of(new ProductDto());

        doThrow(new RuntimeException()).when(service).save(ArgumentMatchers.<List<ProductDto>>any(), any(Usuario.class));

        mockMvc.perform(post("/product/save-all")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtos)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao salvar produto."));
    }

    @Test
    @DisplayName("DELETE /product - Deve deletar produto com sucesso")
    void delete_ShouldReturnOk() throws Exception {
        Long id = 1L;

        mockMvc.perform(delete("/product").param("id", id.toString()))
                .andExpect(status().isOk());

        verify(service).delete(id);
    }

    @Test
    @DisplayName("DELETE /product - Deve retornar 400 quando id nao for encontrado")
    void delete_ShouldReturn400_WhenNotFound() throws Exception {
        Long id = 99L;
        doThrow(new NoSuchElementException()).when(service).delete(id);

        mockMvc.perform(delete("/product").param("id", id.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Produto não encontrado: 99"));
    }

    @Test
    @DisplayName("DELETE /product - Deve retornar 500 em erro generico")
    void delete_ShouldReturn500_WhenError() throws Exception {
        Long id = 1L;
        doThrow(new RuntimeException()).when(service).delete(id);

        mockMvc.perform(delete("/product").param("id", id.toString()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao remover produto."));
    }

    @Test
    @DisplayName("GET /product/stock-product-sale-point - Deve buscar estoque por produto e ponto de venda")
    void getStockByProductAndSalePoint_ShouldReturnOk() throws Exception {
        Long idProduto = 1L;
        when(stockService.getStockByProductAndSalePoint(idProduto)).thenReturn(List.of());

        mockMvc.perform(get("/product/stock-product-sale-point").param("idProduto", idProduto.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /product/stock-product-sale-point - Deve retornar 400 em NoSuchElementException")
    void getStockByProductAndSalePoint_ShouldReturn400_WhenNotFound() throws Exception {
        Long idProduto = 99L;
        when(stockService.getStockByProductAndSalePoint(idProduto)).thenThrow(new NoSuchElementException());

        mockMvc.perform(get("/product/stock-product-sale-point").param("idProduto", idProduto.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Produto não encontrado: 99"));
    }
}