package br.com.dinamica.estoque.controller;

import br.com.dinamica.estoque.dto.CompanyClientContactDto;
import br.com.dinamica.estoque.service.CompanyClientContactService;
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
class CompanyClientContactControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private CompanyClientContactService service;

    @InjectMocks
    private CompanyClientContactController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // --- GET /company-client-contact ---

    @Test
    @DisplayName("GET /company-client-contact - Deve retornar contato por id com status 200 OK")
    void get_ShouldReturnContact_WhenIdExists() throws Exception {
        Long id = 1L;
        CompanyClientContactDto dto = new CompanyClientContactDto();
        dto.setId(id);

        when(service.get(id)).thenReturn(dto);

        mockMvc.perform(get("/company-client-contact").param("id", id.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /company-client-contact - Deve retornar status 400 Bad Request quando contato não for encontrado")
    void get_ShouldReturnBadRequest_WhenNotFound() throws Exception {
        Long id = 99L;
        when(service.get(id)).thenThrow(new NoSuchElementException());

        mockMvc.perform(get("/company-client-contact").param("id", id.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Contato de Empresa Cliente não encontrado: 99"));
    }

    // --- GET /company-client-contact/list ---

    @Test
    @DisplayName("GET /company-client-contact/list - Deve listar contatos com ordenação ascendente padrão")
    void list_ShouldReturnPagedResponse_DefaultAscending() throws Exception {
        Page<CompanyClientContactDto> pageMock = new PageImpl<>(List.of());
        when(service.list(eq(1L), any(Pageable.class))).thenReturn(pageMock);

        mockMvc.perform(get("/company-client-contact/list")
                        .param("idEmpresa", "1")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /company-client-contact/list - Deve listar contatos com ordenação descendente (sort.length > 1)")
    void list_ShouldReturnPagedResponse_DescendingSort() throws Exception {
        Page<CompanyClientContactDto> pageMock = new PageImpl<>(List.of());
        when(service.list(eq(1L), any(Pageable.class))).thenReturn(pageMock);

        mockMvc.perform(get("/company-client-contact/list")
                        .param("idEmpresa", "1")
                        .param("sort", "id,desc"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /company-client-contact/list - Deve aplicar 'asc' quando direção de sort não for informada (sort.length == 1)")
    void list_ShouldDefaultToAsc_WhenSortLengthIsOne() throws Exception {
        Page<CompanyClientContactDto> pageMock = new PageImpl<>(List.of());
        when(service.list(any(), any(Pageable.class))).thenReturn(pageMock);

        mockMvc.perform(get("/company-client-contact/list")
                        .param("sort", "nome"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /company-client-contact/list - Deve retornar status 500 em erro inesperado")
    void list_ShouldReturnInternalServerError_WhenRuntimeException() throws Exception {
        when(service.list(any(), any(Pageable.class))).thenThrow(new RuntimeException());

        mockMvc.perform(get("/company-client-contact/list"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao listar contatos de empresa cliente."));
    }

    // --- POST /company-client-contact ---

    @Test
    @DisplayName("POST /company-client-contact - Deve salvar contato e retornar status 200 OK")
    void save_ShouldReturnOk_WhenSuccess() throws Exception {
        CompanyClientContactDto dto = new CompanyClientContactDto();
        dto.setId(1L);

        when(service.save(any(CompanyClientContactDto.class), any())).thenReturn(dto);

        mockMvc.perform(post("/company-client-contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /company-client-contact - Deve retornar 400 Bad Request em NoSuchElementException")
    void save_ShouldReturnBadRequest_WhenNoSuchElementException() throws Exception {
        CompanyClientContactDto dto = new CompanyClientContactDto();
        dto.setId(10L);

        when(service.save(any(CompanyClientContactDto.class), any())).thenThrow(new NoSuchElementException());

        mockMvc.perform(post("/company-client-contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Contato de Empresa Cliente não encontrado: 10"));
    }

    @Test
    @DisplayName("POST /company-client-contact - Deve retornar status 500 quando ocorrer RuntimeException")
    void save_ShouldReturnInternalServerError_WhenRuntimeException() throws Exception {
        CompanyClientContactDto dto = new CompanyClientContactDto();

        when(service.save(any(CompanyClientContactDto.class), any())).thenThrow(new RuntimeException());

        mockMvc.perform(post("/company-client-contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao salvar contato de empresa cliente."));
    }

    // --- DELETE /company-client-contact ---

    @Test
    @DisplayName("DELETE /company-client-contact - Deve remover contato com sucesso")
    void delete_ShouldReturnOk_WhenSuccess() throws Exception {
        Long id = 1L;

        mockMvc.perform(delete("/company-client-contact").param("id", id.toString()))
                .andExpect(status().isOk());

        verify(service).delete(id);
    }

    @Test
    @DisplayName("DELETE /company-client-contact - Deve retornar status 400 em NoSuchElementException")
    void delete_ShouldReturnBadRequest_WhenNotFound() throws Exception {
        Long id = 99L;
        doThrow(new NoSuchElementException()).when(service).delete(id);

        mockMvc.perform(delete("/company-client-contact").param("id", id.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Contato de Empresa Cliente não encontrado: 99"));
    }

    @Test
    @DisplayName("DELETE /company-client-contact - Deve retornar status 500 em erro genérico")
    void delete_ShouldReturnInternalServerError_WhenRuntimeException() throws Exception {
        Long id = 1L;
        doThrow(new RuntimeException()).when(service).delete(id);

        mockMvc.perform(delete("/company-client-contact").param("id", id.toString()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao remover contato de empresa cliente."));
    }
}