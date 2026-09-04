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

import br.com.dinamica.estoque.dto.ProductTypeDto;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.service.ProductTypeService;

@ExtendWith(MockitoExtension.class)
class ProductTypeControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ProductTypeService service;

    @InjectMocks
    private ProductTypeController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /product-type - Deve buscar tipo de produto por ID")
    void get_ShouldReturnOk() throws Exception {
        Long id = 1L;
        when(service.get(id)).thenReturn(new ProductTypeDto());

        mockMvc.perform(get("/product-type").param("id", id.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /product-type - Deve retornar 400 em NoSuchElementException")
    void get_ShouldReturn400_WhenNotFound() throws Exception {
        Long id = 99L;
        when(service.get(id)).thenThrow(new NoSuchElementException());

        mockMvc.perform(get("/product-type").param("id", id.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Tipo de produto não encontrado: 99"));
    }

    @Test
    @DisplayName("GET /product-type/list - Deve listar tipos de produto ordenando por padrao (asc)")
    void list_ShouldReturnOk_Asc() throws Exception {
        when(service.list(eq("Eletrônicos"), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/product-type/list")
                        .param("nome", "Eletrônicos")
                        .param("sort", "nome"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /product-type/list - Deve listar tipos de produto ordenando por desc")
    void list_ShouldReturnOk_Desc() throws Exception {
        when(service.list(any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/product-type/list")
                        .param("sort", "nome", "desc"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /product-type/list - Deve retornar 500 em erro generico")
    void list_ShouldReturn500_WhenError() throws Exception {
        when(service.list(any(), any(Pageable.class))).thenThrow(new RuntimeException());

        mockMvc.perform(get("/product-type/list"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao listar tipos de produto."));
    }

    @Test
    @DisplayName("POST /product-type - Deve salvar produto")
    void save_ShouldReturnOk() throws Exception {
        ProductTypeDto dto = new ProductTypeDto();
        dto.setId(1L);

        when(service.save(any(ProductTypeDto.class), any(Usuario.class))).thenReturn(dto);

        mockMvc.perform(post("/product-type")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /product-type - Deve retornar 400 quando id nao for encontrado")
    void save_ShouldReturn400_WhenNotFound() throws Exception {
        ProductTypeDto dto = new ProductTypeDto();
        dto.setId(10L);

        when(service.save(any(ProductTypeDto.class), any(Usuario.class)))
                .thenThrow(new NoSuchElementException());

        mockMvc.perform(post("/product-type")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Tipo de produto não encontrado: 10"));
    }

    @Test
    @DisplayName("POST /product-type - Deve retornar 500 em erro generico")
    void save_ShouldReturn500_WhenError() throws Exception {
        ProductTypeDto dto = new ProductTypeDto();

        when(service.save(any(ProductTypeDto.class), any(Usuario.class)))
                .thenThrow(new RuntimeException());

        mockMvc.perform(post("/product-type")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao salvar tipo de produto."));
    }

    @Test
    @DisplayName("DELETE /product-type - Deve excluir tipo de produto")
    void delete_ShouldReturnOk() throws Exception {
        Long id = 1L;

        mockMvc.perform(delete("/product-type").param("id", id.toString()))
                .andExpect(status().isOk());

        verify(service).delete(id);
    }

    @Test
    @DisplayName("DELETE /product-type - Deve retornar 400 em NoSuchElementException")
    void delete_ShouldReturn400_WhenNotFound() throws Exception {
        Long id = 99L;
        doThrow(new NoSuchElementException()).when(service).delete(id);

        mockMvc.perform(delete("/product-type").param("id", id.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Tipo de produto não encontrado: 99"));
    }

    @Test
    @DisplayName("DELETE /product-type - Deve retornar 500 em erro generico")
    void delete_ShouldReturn500_WhenError() throws Exception {
        Long id = 1L;
        doThrow(new RuntimeException()).when(service).delete(id);

        mockMvc.perform(delete("/product-type").param("id", id.toString()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao remover tipo de produto."));
    }

    @Test
    @DisplayName("POST /product-type/save-all - Deve salvar lista de produtos")
    void saveAll_ShouldReturnOk() throws Exception {
        List<ProductTypeDto> dtos = List.of(new ProductTypeDto());

        mockMvc.perform(post("/product-type/save-all")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtos)))
                .andExpect(status().isOk());

        verify(service).save(ArgumentMatchers.<List<ProductTypeDto>>any(), any(Usuario.class));
    }

    @Test
    @DisplayName("POST /product-type/save-all - Deve retornar 500 em erro generico")
    void saveAll_ShouldReturn500_WhenError() throws Exception {
        List<ProductTypeDto> dtos = List.of(new ProductTypeDto());

        doThrow(new RuntimeException()).when(service).save(ArgumentMatchers.<List<ProductTypeDto>>any(), any(Usuario.class));

        mockMvc.perform(post("/product-type/save-all")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtos)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao salvar tipo de produto."));
    }

}