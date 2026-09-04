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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.dinamica.estoque.dto.PriceTableDto;
import br.com.dinamica.estoque.service.PriceTableService;

@ExtendWith(MockitoExtension.class)
class PriceTableControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private PriceTableService service;

    @InjectMocks
    private PriceTableController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // --- GET /price-table ---

    @Test
    @DisplayName("GET /price-table - Deve retornar tabela de preços por id com status 200 OK")
    void get_ShouldReturnPriceTable_WhenIdExists() throws Exception {
        Long id = 1L;
        PriceTableDto dto = new PriceTableDto();
        dto.setId(id);

        when(service.get(id)).thenReturn(dto);

        mockMvc.perform(get("/price-table").param("id", id.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /price-table - Deve retornar status 400 Bad Request quando tabela não encontrada")
    void get_ShouldReturnBadRequest_WhenNotFound() throws Exception {
        Long id = 99L;
        when(service.get(id)).thenThrow(new NoSuchElementException());

        mockMvc.perform(get("/price-table").param("id", id.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Tabela de preços não encontrado: 99"));
    }

    // --- GET /price-table/list ---

    @Test
    @DisplayName("GET /price-table/list - Deve listar tabelas com ordenação ascendente padrão")
    void list_ShouldReturnPagedResponse_DefaultAscending() throws Exception {
        Page<PriceTableDto> pageMock = new PageImpl<>(List.of());
        when(service.list(eq("Varejo"), any(Pageable.class))).thenReturn(pageMock);

        mockMvc.perform(get("/price-table/list")
                        .param("nome", "Varejo")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /price-table/list - Deve listar tabelas com ordenação descendente (sort.length > 1)")
    void list_ShouldReturnPagedResponse_DescendingSort() throws Exception {
        Page<PriceTableDto> pageMock = new PageImpl<>(List.of());
        when(service.list(eq("Varejo"), any(Pageable.class))).thenReturn(pageMock);

        mockMvc.perform(get("/price-table/list")
                        .param("nome", "Varejo")
                        .param("sort", "id,desc"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /price-table/list - Deve aplicar 'asc' quando direção de sort não for informada (sort.length == 1)")
    void list_ShouldDefaultToAsc_WhenSortLengthIsOne() throws Exception {
        Page<PriceTableDto> pageMock = new PageImpl<>(List.of());
        when(service.list(any(), any(Pageable.class))).thenReturn(pageMock);

        mockMvc.perform(get("/price-table/list")
                        .param("sort", "nome"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /price-table/list - Deve retornar status 500 em erro inesperado")
    void list_ShouldReturnInternalServerError_WhenRuntimeException() throws Exception {
        when(service.list(any(), any(Pageable.class))).thenThrow(new RuntimeException());

        mockMvc.perform(get("/price-table/list"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao listar tabelas de preços."));
    }

    // --- POST /price-table ---

    @Test
    @DisplayName("POST /price-table - Deve salvar tabela de preço e retornar status 200 OK")
    void save_ShouldReturnOk_WhenSuccess() throws Exception {
        PriceTableDto dto = new PriceTableDto();
        dto.setId(1L);

        when(service.save(any(PriceTableDto.class), any())).thenReturn(dto);

        mockMvc.perform(post("/price-table")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /price-table - Deve retornar 400 Bad Request em NoSuchElementException")
    void save_ShouldReturnBadRequest_WhenNoSuchElementException() throws Exception {
        PriceTableDto dto = new PriceTableDto();
        dto.setId(10L);

        when(service.save(any(PriceTableDto.class), any())).thenThrow(new NoSuchElementException());

        mockMvc.perform(post("/price-table")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Tabela de preços não encontrado: 10"));
    }

    @Test
    @DisplayName("POST /price-table - Deve retornar status 500 quando ocorrer RuntimeException")
    void save_ShouldReturnInternalServerError_WhenRuntimeException() throws Exception {
        PriceTableDto dto = new PriceTableDto();

        when(service.save(any(PriceTableDto.class), any())).thenThrow(new RuntimeException());

        mockMvc.perform(post("/price-table")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao salvar tabela de preços."));
    }

    // --- DELETE /price-table ---

    @Test
    @DisplayName("DELETE /price-table - Deve remover tabela de preço com sucesso")
    void delete_ShouldReturnOk_WhenSuccess() throws Exception {
        Long id = 1L;

        mockMvc.perform(delete("/price-table").param("id", id.toString()))
                .andExpect(status().isOk());

        verify(service).delete(id);
    }

    @Test
    @DisplayName("DELETE /price-table - Deve retornar status 400 em NoSuchElementException")
    void delete_ShouldReturnBadRequest_WhenNotFound() throws Exception {
        Long id = 99L;
        doThrow(new NoSuchElementException()).when(service).delete(id);

        mockMvc.perform(delete("/price-table").param("id", id.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Tabela de preços não encontrado: 99"));
    }

    @Test
    @DisplayName("DELETE /price-table - Deve retornar status 500 em erro genérico")
    void delete_ShouldReturnInternalServerError_WhenRuntimeException() throws Exception {
        Long id = 1L;
        doThrow(new RuntimeException()).when(service).delete(id);

        mockMvc.perform(delete("/price-table").param("id", id.toString()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao remover tabela de preços."));
    }

    // --- POST /price-table/save-all ---

    @SuppressWarnings("unchecked")
	@Test
    @DisplayName("POST /price-table/save-all - Deve salvar lista de tabelas com sucesso")
    void saveAll_ShouldReturnOk_WhenSuccess() throws Exception {
        List<PriceTableDto> dtos = List.of(new PriceTableDto(), new PriceTableDto());

        mockMvc.perform(post("/price-table/save-all")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtos)))
                .andExpect(status().isOk());

        verify(service).save(any(List.class), any());
    }

    @SuppressWarnings("unchecked")
	@Test
    @DisplayName("POST /price-table/save-all - Deve retornar status 500 em caso de RuntimeException")
    void saveAll_ShouldReturnInternalServerError_WhenRuntimeException() throws Exception {
        List<PriceTableDto> dtos = List.of(new PriceTableDto());

        doThrow(new RuntimeException()).when(service).save(any(List.class), any());

        mockMvc.perform(post("/price-table/save-all")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtos)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao salvar tabela de preços."));
    }
}