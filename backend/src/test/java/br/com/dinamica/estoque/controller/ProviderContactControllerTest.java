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

import br.com.dinamica.estoque.dto.ProviderContactDto;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.service.ProviderContactService;

@ExtendWith(MockitoExtension.class)
class ProviderContactControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ProviderContactService service;

    @InjectMocks
    private ProviderContactController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /provider-contact - Deve obter contato por id com sucesso")
    void get_ShouldReturnOk() throws Exception {
        Long id = 1L;
        when(service.get(id)).thenReturn(new ProviderContactDto());

        mockMvc.perform(get("/provider-contact").param("id", id.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /provider-contact - Deve retornar 400 em NoSuchElementException")
    void get_ShouldReturn400_WhenNotFound() throws Exception {
        Long id = 99L;
        when(service.get(id)).thenThrow(new NoSuchElementException());

        mockMvc.perform(get("/provider-contact").param("id", id.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Contato de fornecedor não encontrado: 99"));
    }

    @Test
    @DisplayName("GET /provider-contact/list - Deve listar contatos de fornecedor ordenando por padrao (asc)")
    void list_ShouldReturnOk_Asc() throws Exception {
        when(service.list(eq(5L), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/provider-contact/list")
                        .param("idFornecedor", "5")
                        .param("sort", "id"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /provider-contact/list - Deve listar contatos ordenando por desc")
    void list_ShouldReturnOk_Desc() throws Exception {
        when(service.list(any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/provider-contact/list")
                        .param("sort", "id", "desc"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /provider-contact/list - Deve retornar 500 em erro generico")
    void list_ShouldReturn500_WhenError() throws Exception {
        when(service.list(any(), any(Pageable.class))).thenThrow(new RuntimeException());

        mockMvc.perform(get("/provider-contact/list"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao listar contatos de fornecedor."));
    }

    @Test
    @DisplayName("POST /provider-contact - Deve salvar contato")
    void save_ShouldReturnOk() throws Exception {
        ProviderContactDto dto = new ProviderContactDto();
        when(service.save(any(ProviderContactDto.class), any(Usuario.class))).thenReturn(dto);

        mockMvc.perform(post("/provider-contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /provider-contact - Deve retornar 400 quando id nao for encontrado")
    void save_ShouldReturn400_WhenNotFound() throws Exception {
        ProviderContactDto dto = new ProviderContactDto();
        dto.setId(99L);

        when(service.save(any(ProviderContactDto.class), any(Usuario.class))).thenThrow(new NoSuchElementException());

        mockMvc.perform(post("/provider-contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Contato de fornecedor não encontrado: 99"));
    }

    @Test
    @DisplayName("POST /provider-contact - Deve retornar 500 em erro generico")
    void save_ShouldReturn500_WhenError() throws Exception {
        ProviderContactDto dto = new ProviderContactDto();

        when(service.save(any(ProviderContactDto.class), any(Usuario.class))).thenThrow(new RuntimeException());

        mockMvc.perform(post("/provider-contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao salvar contato de fornecedor."));
    }

    @Test
    @DisplayName("DELETE /provider-contact - Deve remover contato")
    void delete_ShouldReturnOk() throws Exception {
        Long id = 1L;

        mockMvc.perform(delete("/provider-contact").param("id", id.toString()))
                .andExpect(status().isOk());

        verify(service).delete(id);
    }

    @Test
    @DisplayName("DELETE /provider-contact - Deve retornar 400 em NoSuchElementException")
    void delete_ShouldReturn400_WhenNotFound() throws Exception {
        Long id = 99L;
        doThrow(new NoSuchElementException()).when(service).delete(id);

        mockMvc.perform(delete("/provider-contact").param("id", id.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Contato de fornecedor não encontrado: 99"));
    }

    @Test
    @DisplayName("DELETE /provider-contact - Deve retornar 500 em erro generico")
    void delete_ShouldReturn500_WhenError() throws Exception {
        Long id = 1L;
        doThrow(new RuntimeException()).when(service).delete(id);

        mockMvc.perform(delete("/provider-contact").param("id", id.toString()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao remover contato de fornecedor."));
    }
}