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

import br.com.dinamica.estoque.dto.UserSalePointDto;
import br.com.dinamica.estoque.service.UserSalePointService;

@ExtendWith(MockitoExtension.class)
class UserSalePointControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private UserSalePointService service;

    @InjectMocks
    private UserSalePointController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /user-sale-point - Deve buscar por ID com sucesso")
    void get_ShouldReturnOk() throws Exception {
        Long id = 1L;
        when(service.get(id)).thenReturn(new UserSalePointDto());

        mockMvc.perform(get("/user-sale-point").param("id", id.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /user-sale-point - Deve retornar 400 em NoSuchElementException")
    void get_ShouldReturn400_WhenNotFound() throws Exception {
        Long id = 99L;
        when(service.get(id)).thenThrow(new NoSuchElementException());

        mockMvc.perform(get("/user-sale-point").param("id", id.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Usuário no Ponto de venda não encontrado: 99"));
    }

    @Test
    @DisplayName("GET /user-sale-point/list - Deve listar ordenando por desc quando sort.length > 1")
    void list_ShouldReturnOk_Desc() throws Exception {
        when(service.list(any(), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/user-sale-point/list")
                        .param("idUsuario", "1")
                        .param("idPontoVenda", "2")
                        .param("sort", "id", "desc"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /user-sale-point/list - Deve listar ordenando por asc quando sort.length > 1")
    void list_ShouldReturnOk_Asc() throws Exception {
        when(service.list(any(), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/user-sale-point/list")
                        .param("sort", "id", "asc"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /user-sale-point/list - Deve usar direcao padrao 'asc' quando sort.length == 1")
    void list_ShouldReturnOk_SingleSortParam() throws Exception {
        when(service.list(any(), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/user-sale-point/list")
                        .param("sort", "id"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /user-sale-point/list - Deve retornar 500 em erro generico na listagem")
    void list_ShouldReturn500_WhenError() throws Exception {
        when(service.list(any(), any(), any(Pageable.class))).thenThrow(new RuntimeException());

        mockMvc.perform(get("/user-sale-point/list"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao listar usuários no ponto de venda."));
    }

    @Test
    @DisplayName("POST /user-sale-point - Deve salvar com sucesso")
    void save_ShouldReturnOk() throws Exception {
        UserSalePointDto dto = new UserSalePointDto();
        when(service.save(any(UserSalePointDto.class))).thenReturn(dto);

        mockMvc.perform(post("/user-sale-point")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /user-sale-point - Deve retornar 400 em NoSuchElementException")
    void save_ShouldReturn400_WhenNotFound() throws Exception {
        UserSalePointDto dto = new UserSalePointDto();
        dto.setId(99L);
        when(service.save(any(UserSalePointDto.class))).thenThrow(new NoSuchElementException());

        mockMvc.perform(post("/user-sale-point")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Usuário no Ponto de venda não encontrado: 99"));
    }

    @Test
    @DisplayName("POST /user-sale-point - Deve retornar 500 em erro generico ao salvar")
    void save_ShouldReturn500_WhenError() throws Exception {
        UserSalePointDto dto = new UserSalePointDto();
        when(service.save(any(UserSalePointDto.class))).thenThrow(new RuntimeException());

        mockMvc.perform(post("/user-sale-point")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao salvar usuário no ponto de venda."));
    }

    @Test
    @DisplayName("DELETE /user-sale-point - Deve remover com sucesso")
    void delete_ShouldReturnOk() throws Exception {
        Long id = 1L;

        mockMvc.perform(delete("/user-sale-point").param("id", id.toString()))
                .andExpect(status().isOk());

        verify(service).delete(id);
    }

    @Test
    @DisplayName("DELETE /user-sale-point - Deve retornar 400 em NoSuchElementException ao deletar")
    void delete_ShouldReturn400_WhenNotFound() throws Exception {
        Long id = 99L;
        doThrow(new NoSuchElementException()).when(service).delete(id);

        mockMvc.perform(delete("/user-sale-point").param("id", id.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Usuário no Ponto de venda não encontrado: 99"));
    }

    @Test
    @DisplayName("DELETE /user-sale-point - Deve retornar 500 em erro generico ao deletar")
    void delete_ShouldReturn500_WhenError() throws Exception {
        Long id = 1L;
        doThrow(new RuntimeException()).when(service).delete(id);

        mockMvc.perform(delete("/user-sale-point").param("id", id.toString()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao remover usuário no ponto de venda."));
    }

    @Test
    @DisplayName("POST /user-sale-point/save-sale-points - Deve salvar lista de pontos de venda")
    void saveSalePoints_ShouldReturnOk() throws Exception {
        List<UserSalePointDto> dtos = List.of(new UserSalePointDto());

        mockMvc.perform(post("/user-sale-point/save-sale-points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtos)))
                .andExpect(status().isOk())
                .andExpect(content().string("Pontos de Venda registrados"));

        verify(service).saveSalePoints(any());
    }

    @Test
    @DisplayName("POST /user-sale-point/save-sale-points - Deve retornar 400 em NoSuchElementException")
    void saveSalePoints_ShouldReturn400_WhenNotFound() throws Exception {
        List<UserSalePointDto> dtos = List.of(new UserSalePointDto());
        doThrow(new NoSuchElementException()).when(service).saveSalePoints(any());

        mockMvc.perform(post("/user-sale-point/save-sale-points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtos)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Algum ponto de venda ou usuário não encontrado na base"));
    }

    @Test
    @DisplayName("POST /user-sale-point/save-sale-points - Deve retornar 500 em erro generico")
    void saveSalePoints_ShouldReturn500_WhenError() throws Exception {
        List<UserSalePointDto> dtos = List.of(new UserSalePointDto());
        doThrow(new RuntimeException()).when(service).saveSalePoints(any());

        mockMvc.perform(post("/user-sale-point/save-sale-points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtos)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao salvar usuário no ponto de venda."));
    }
}