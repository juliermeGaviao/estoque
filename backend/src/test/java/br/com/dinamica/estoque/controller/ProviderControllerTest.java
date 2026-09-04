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

import br.com.dinamica.estoque.dto.ProviderDto;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.service.ProviderService;

@ExtendWith(MockitoExtension.class)
class ProviderControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ProviderService service;

    @InjectMocks
    private ProviderController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /provider - Deve obter fornecedor por id")
    void get_ShouldReturnOk() throws Exception {
        Long id = 1L;
        when(service.get(id)).thenReturn(new ProviderDto());

        mockMvc.perform(get("/provider").param("id", id.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /provider - Deve retornar 400 em NoSuchElementException")
    void get_ShouldReturn400_WhenNotFound() throws Exception {
        Long id = 99L;
        when(service.get(id)).thenThrow(new NoSuchElementException());

        mockMvc.perform(get("/provider").param("id", id.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Fornecedor não encontrado: 99"));
    }

    @Test
    @DisplayName("GET /provider/list - Deve listar fornecedores filtrados ordenando por padrao (asc)")
    void list_ShouldReturnOk_Asc() throws Exception {
        when(service.list(any(), any(), any(), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/provider/list")
                        .param("razaoSocial", "Empresa X")
                        .param("cnpj", "00000000000191")
                        .param("sort", "razaoSocial"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /provider/list - Deve listar fornecedores ordenando por desc")
    void list_ShouldReturnOk_Desc() throws Exception {
        when(service.list(any(), any(), any(), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/provider/list")
                        .param("sort", "razaoSocial", "desc"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /provider/list - Deve retornar 500 em erro generico")
    void list_ShouldReturn500_WhenError() throws Exception {
        when(service.list(any(), any(), any(), any(), any(Pageable.class))).thenThrow(new RuntimeException());

        mockMvc.perform(get("/provider/list"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao listar fornecedores."));
    }

    @Test
    @DisplayName("POST /provider - Deve salvar fornecedor com sucesso")
    void save_ShouldReturnOk() throws Exception {
        ProviderDto dto = new ProviderDto();
        when(service.save(any(ProviderDto.class), any(Usuario.class))).thenReturn(dto);

        mockMvc.perform(post("/provider")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /provider - Deve retornar 400 quando id nao for encontrado")
    void save_ShouldReturn400_WhenNotFound() throws Exception {
        ProviderDto dto = new ProviderDto();
        dto.setId(99L);

        when(service.save(any(ProviderDto.class), any(Usuario.class))).thenThrow(new NoSuchElementException());

        mockMvc.perform(post("/provider")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Fornecedor não encontrado: 99"));
    }

    @Test
    @DisplayName("POST /provider - Deve retornar 500 em erro generico")
    void save_ShouldReturn500_WhenError() throws Exception {
        ProviderDto dto = new ProviderDto();

        when(service.save(any(ProviderDto.class), any(Usuario.class))).thenThrow(new RuntimeException());

        mockMvc.perform(post("/provider")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao salvar fornecedor."));
    }

    @Test
    @DisplayName("DELETE /provider - Deve deletar fornecedor")
    void delete_ShouldReturnOk() throws Exception {
        Long id = 1L;

        mockMvc.perform(delete("/provider").param("id", id.toString()))
                .andExpect(status().isOk());

        verify(service).delete(id);
    }

    @Test
    @DisplayName("DELETE /provider - Deve retornar 400 em NoSuchElementException")
    void delete_ShouldReturn400_WhenNotFound() throws Exception {
        Long id = 99L;
        doThrow(new NoSuchElementException()).when(service).delete(id);

        mockMvc.perform(delete("/provider").param("id", id.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Fornecedor não encontrado: 99"));
    }

    @Test
    @DisplayName("DELETE /provider - Deve retornar 500 em erro generico")
    void delete_ShouldReturn500_WhenError() throws Exception {
        Long id = 1L;
        doThrow(new RuntimeException()).when(service).delete(id);

        mockMvc.perform(delete("/provider").param("id", id.toString()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao remover fornecedor."));
    }
}