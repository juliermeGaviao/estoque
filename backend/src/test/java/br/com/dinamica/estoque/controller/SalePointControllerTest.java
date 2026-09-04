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

import br.com.dinamica.estoque.dto.SalePointDto;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.service.SalePointService;

@ExtendWith(MockitoExtension.class)
class SalePointControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private SalePointService service;

    @InjectMocks
    private SalePointController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /sale-point - Deve buscar ponto de venda por ID")
    void get_ShouldReturnOk() throws Exception {
        Long id = 1L;
        when(service.get(id)).thenReturn(new SalePointDto());

        mockMvc.perform(get("/sale-point").param("id", id.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /sale-point - Deve retornar 400 em NoSuchElementException")
    void get_ShouldReturn400_WhenNotFound() throws Exception {
        Long id = 99L;
        when(service.get(id)).thenThrow(new NoSuchElementException());

        mockMvc.perform(get("/sale-point").param("id", id.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Ponto de venda não encontrado: 99"));
    }

    @Test
    @DisplayName("GET /sale-point/list - Deve listar pontos de venda paginados ordenando por padrao (asc)")
    void list_ShouldReturnOk_Asc() throws Exception {
        when(service.list(eq("Matriz"), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/sale-point/list")
                        .param("nome", "Matriz")
                        .param("sort", "nome"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /sale-point/list - Deve listar pontos de venda paginados ordenando por desc")
    void list_ShouldReturnOk_Desc() throws Exception {
        when(service.list(any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/sale-point/list")
                        .param("sort", "nome", "desc"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /sale-point/list - Deve retornar 500 em erro generico")
    void list_ShouldReturn500_WhenError() throws Exception {
        when(service.list(any(), any(Pageable.class))).thenThrow(new RuntimeException());

        mockMvc.perform(get("/sale-point/list"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao listar pontos de venda."));
    }

    @Test
    @DisplayName("POST /sale-point - Deve salvar ponto de venda")
    void save_ShouldReturnOk() throws Exception {
        SalePointDto dto = new SalePointDto();
        when(service.save(any(SalePointDto.class), any(Usuario.class))).thenReturn(dto);

        mockMvc.perform(post("/sale-point")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /sale-point - Deve retornar 400 quando id nao for encontrado")
    void save_ShouldReturn400_WhenNotFound() throws Exception {
        SalePointDto dto = new SalePointDto();
        dto.setId(99L);

        when(service.save(any(SalePointDto.class), any(Usuario.class))).thenThrow(new NoSuchElementException());

        mockMvc.perform(post("/sale-point")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Ponto de venda não encontrado: 99"));
    }

    @Test
    @DisplayName("POST /sale-point - Deve retornar 500 em erro generico")
    void save_ShouldReturn500_WhenError() throws Exception {
        SalePointDto dto = new SalePointDto();

        when(service.save(any(SalePointDto.class), any(Usuario.class))).thenThrow(new RuntimeException());

        mockMvc.perform(post("/sale-point")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao salvar ponto de venda."));
    }

    @Test
    @DisplayName("DELETE /sale-point - Deve deletar ponto de venda com sucesso")
    void delete_ShouldReturnOk() throws Exception {
        Long id = 1L;

        mockMvc.perform(delete("/sale-point").param("id", id.toString()))
                .andExpect(status().isOk());

        verify(service).delete(id);
    }

    @Test
    @DisplayName("DELETE /sale-point - Deve retornar 400 em NoSuchElementException")
    void delete_ShouldReturn400_WhenNotFound() throws Exception {
        Long id = 99L;
        doThrow(new NoSuchElementException()).when(service).delete(id);

        mockMvc.perform(delete("/sale-point").param("id", id.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Ponto de venda não encontrado: 99"));
    }

    @Test
    @DisplayName("DELETE /sale-point - Deve retornar 500 em erro generico")
    void delete_ShouldReturn500_WhenError() throws Exception {
        Long id = 1L;
        doThrow(new RuntimeException()).when(service).delete(id);

        mockMvc.perform(delete("/sale-point").param("id", id.toString()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao remover ponto de venda."));
    }

    @Test
    @DisplayName("POST /sale-point/save-all - Deve salvar lista de pontos de venda")
    void saveAll_ShouldReturnOk() throws Exception {
        List<SalePointDto> dtos = List.of(new SalePointDto());

        mockMvc.perform(post("/sale-point/save-all")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtos)))
                .andExpect(status().isOk());

        verify(service).save(ArgumentMatchers.<List<SalePointDto>>any(), any(Usuario.class));
    }

    @Test
    @DisplayName("POST /sale-point/save-all - Deve retornar 500 em erro generico")
    void saveAll_ShouldReturn500_WhenError() throws Exception {
        List<SalePointDto> dtos = List.of(new SalePointDto());

        doThrow(new RuntimeException()).when(service).save(ArgumentMatchers.<List<SalePointDto>>any(), any(Usuario.class));

        mockMvc.perform(post("/sale-point/save-all")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtos)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao salvar ponto de venda."));
    }
}