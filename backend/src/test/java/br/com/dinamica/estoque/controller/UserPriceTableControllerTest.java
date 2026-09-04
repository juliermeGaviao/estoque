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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.dinamica.estoque.dto.UserPriceTableDto;
import br.com.dinamica.estoque.service.UserPriceTableService;

@ExtendWith(MockitoExtension.class)
class UserPriceTableControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private UserPriceTableService service;

    @InjectMocks
    private UserPriceTableController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /user-price-table - Deve buscar por ID com sucesso")
    void get_ShouldReturnOk() throws Exception {
        Long id = 1L;
        when(service.get(id)).thenReturn(new UserPriceTableDto());

        mockMvc.perform(get("/user-price-table").param("id", id.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /user-price-table - Deve retornar 400 em NoSuchElementException")
    void get_ShouldReturn400_WhenNotFound() throws Exception {
        Long id = 99L;
        when(service.get(id)).thenThrow(new NoSuchElementException());

        mockMvc.perform(get("/user-price-table").param("id", id.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Vendedor da Tabela de preços não encontrado: 99"));
    }

    @Test
    @DisplayName("GET /user-price-table/list - Deve listar ordenando por desc quando sort.length > 1")
    void list_ShouldReturnOk_Desc() throws Exception {
        when(service.list(any(), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/user-price-table/list")
                        .param("idTabelaPreco", "1")
                        .param("idVendedor", "2")
                        .param("sort", "id", "desc"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /user-price-table/list - Deve listar ordenando por asc quando sort.length > 1")
    void list_ShouldReturnOk_Asc() throws Exception {
        when(service.list(any(), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/user-price-table/list")
                        .param("sort", "id", "asc"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /user-price-table/list - Deve usar direcao padrao 'asc' quando sort.length == 1")
    void list_ShouldReturnOk_SingleSortParam() throws Exception {
        when(service.list(any(), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/user-price-table/list")
                        .param("sort", "id"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /user-price-table/list - Deve retornar 500 em erro generico na listagem")
    void list_ShouldReturn500_WhenError() throws Exception {
        when(service.list(any(), any(), any(Pageable.class))).thenThrow(new RuntimeException());

        mockMvc.perform(get("/user-price-table/list"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao listar vendedor da tabelas de preços."));
    }

    @Test
    @DisplayName("POST /user-price-table - Deve salvar com sucesso")
    void save_ShouldReturnOk() throws Exception {
        UserPriceTableDto dto = new UserPriceTableDto();
        when(service.save(any(UserPriceTableDto.class))).thenReturn(dto);

        mockMvc.perform(post("/user-price-table")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /user-price-table - Deve retornar 400 em NoSuchElementException")
    void save_ShouldReturn400_WhenNotFound() throws Exception {
        UserPriceTableDto dto = new UserPriceTableDto();
        dto.setId(99L);
        when(service.save(any(UserPriceTableDto.class))).thenThrow(new NoSuchElementException());

        mockMvc.perform(post("/user-price-table")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Vendedor da Tabela de preços não encontrado: 99"));
    }

    @Test
    @DisplayName("POST /user-price-table - Deve retornar 500 em erro generico ao salvar")
    void save_ShouldReturn500_WhenError() throws Exception {
        UserPriceTableDto dto = new UserPriceTableDto();
        when(service.save(any(UserPriceTableDto.class))).thenThrow(new RuntimeException());

        mockMvc.perform(post("/user-price-table")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao salvar vendedor da tabela de preços."));
    }

    @Test
    @DisplayName("DELETE /user-price-table - Deve remover com sucesso")
    void delete_ShouldReturnOk() throws Exception {
        Long id = 1L;

        mockMvc.perform(delete("/user-price-table").param("id", id.toString()))
                .andExpect(status().isOk());

        verify(service).delete(id);
    }

    @Test
    @DisplayName("DELETE /user-price-table - Deve retornar 400 em NoSuchElementException ao deletar")
    void delete_ShouldReturn400_WhenNotFound() throws Exception {
        Long id = 99L;
        doThrow(new NoSuchElementException()).when(service).delete(id);

        mockMvc.perform(delete("/user-price-table").param("id", id.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Vendedor da Tabela de preços não encontrado: 99"));
    }

    @Test
    @DisplayName("DELETE /user-price-table - Deve retornar 500 em erro generico ao deletar")
    void delete_ShouldReturn500_WhenError() throws Exception {
        Long id = 1L;
        doThrow(new RuntimeException()).when(service).delete(id);

        mockMvc.perform(delete("/user-price-table").param("id", id.toString()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao remover vendedor da tabela de preços."));
    }

    @Test
    @DisplayName("POST /user-price-table/save-tables - Deve salvar lista de tabelas com sucesso")
    void savePrices_ShouldReturnOk() throws Exception {
        List<UserPriceTableDto> dtos = List.of(new UserPriceTableDto());

        mockMvc.perform(post("/user-price-table/save-tables")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtos)))
                .andExpect(status().isOk())
                .andExpect(content().string("Tabelas registradas"));

        verify(service).saveTables(any());
    }

    @Test
    @DisplayName("POST /user-price-table/save-tables - Deve retornar 400 em NoSuchElementException")
    void savePrices_ShouldReturn400_WhenNotFound() throws Exception {
        List<UserPriceTableDto> dtos = List.of(new UserPriceTableDto());
        doThrow(new NoSuchElementException()).when(service).saveTables(any());

        mockMvc.perform(post("/user-price-table/save-tables")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtos)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Alguma tabela de preços ou usuário não encontrado na base"));
    }

    @Test
    @DisplayName("POST /user-price-table/save-tables - Deve retornar 500 em erro generico")
    void savePrices_ShouldReturn500_WhenError() throws Exception {
        List<UserPriceTableDto> dtos = List.of(new UserPriceTableDto());
        doThrow(new RuntimeException()).when(service).saveTables(any());

        mockMvc.perform(post("/user-price-table/save-tables")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtos)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao salvar vendedor da tabela de preços."));
    }
}