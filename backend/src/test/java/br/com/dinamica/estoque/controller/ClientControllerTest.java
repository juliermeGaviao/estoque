package br.com.dinamica.estoque.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.dinamica.estoque.dto.ClientDto;
import br.com.dinamica.estoque.dto.CommonClientDto;
import br.com.dinamica.estoque.dto.PersonFilterDto;
import br.com.dinamica.estoque.dto.ResultadoCargaEmpregadosDto;
import br.com.dinamica.estoque.service.ClientService;

@ExtendWith(MockitoExtension.class)
class ClientControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ClientService clientService;

    @InjectMocks
    private ClientController clientController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(clientController).build();
    }

    @Test
    @DisplayName("GET /client - Deve retornar cliente por id com status 200 OK")
    void get_ShouldReturnClient_WhenIdExists() throws Exception {
        Long id = 1L;
        ClientDto dto = new ClientDto();
        dto.setId(id);

        when(clientService.get(id)).thenReturn(dto);

        mockMvc.perform(get("/client").param("id", id.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /client - Deve retornar status 400 Bad Request quando cliente não encontrado")
    void get_ShouldReturnBadRequest_WhenNotFound() throws Exception {
        Long id = 99L;
        when(clientService.get(id)).thenThrow(new NoSuchElementException());

        mockMvc.perform(get("/client").param("id", id.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Empresa Cliente não encontrado: 99"));
    }

    // --- GET /client/find-all ---

    @Test
    @DisplayName("GET /client/find-all - Deve retornar lista de clientes com status 200 OK")
    void findAll_ShouldReturnList_WhenSuccess() throws Exception {
        when(clientService.findAll()).thenReturn(List.of(new CommonClientDto(1L, "Empresa A")));

        mockMvc.perform(get("/client/find-all"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /client/find-all - Deve retornar status 500 quando ocorrer exceção")
    void findAll_ShouldReturnInternalServerError_WhenRuntimeException() throws Exception {
        when(clientService.findAll()).thenThrow(new RuntimeException("Erro de banco"));

        mockMvc.perform(get("/client/find-all"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao buscar lista completa de empresas clientes."));
    }

    // --- GET /client/list-companies ---

    @Test
    @DisplayName("GET /client/list-companies - Deve retornar lista paginada de empresas (ASC)")
    void listCompanies_ShouldReturnPagedResponse_Ascending() throws Exception {
        Page<ClientDto> pageMock = new PageImpl<>(List.of());
        when(clientService.list(eq("Razao"), eq("Nome"), eq("123"), eq("9999"), any(Pageable.class))).thenReturn(pageMock);

        mockMvc.perform(get("/client/list-companies")
                        .param("razaoSocial", "Razao")
                        .param("nome", "Nome")
                        .param("cnpj", "123")
                        .param("fone", "9999")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "id,asc"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /client/list-companies - Deve aceitar ordenação descendente e sem segundo parâmetro de sort")
    void listCompanies_ShouldHandleDescendingAndSingleSortParam() throws Exception {
        Page<ClientDto> pageMock = new PageImpl<>(List.of());
        when(clientService.list(any(), any(), any(), any(), any(Pageable.class))).thenReturn(pageMock);

        mockMvc.perform(get("/client/list-companies")
                        .param("sort", "nome,desc"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/client/list-companies")
                        .param("sort", "nome"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /client/list-companies - Deve retornar status 500 em erro inesperado")
    void listCompanies_ShouldReturnInternalServerError_WhenRuntimeException() throws Exception {
        when(clientService.list(any(), any(), any(), any(), any(Pageable.class)))
                .thenThrow(new RuntimeException());

        mockMvc.perform(get("/client/list-companies"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao listar empresas clientes."));
    }

    // --- GET /client/list-people ---

    @Test
    @DisplayName("GET /client/list-people - Deve retornar lista paginada de pessoas")
    void listPeople_ShouldReturnPagedResponse() throws Exception {
        Page<ClientDto> pageMock = new PageImpl<>(List.of());
        when(clientService.list(any(PersonFilterDto.class), any(Pageable.class))).thenReturn(pageMock);

        mockMvc.perform(get("/client/list-people")
                        .param("nome", "João")
                        .param("idEmpresa", "1")
                        .param("sort", "nome,desc"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /client/list-people - Deve retornar status 500 em caso de erro")
    void listPeople_ShouldReturnInternalServerError_WhenRuntimeException() throws Exception {
        when(clientService.list(any(PersonFilterDto.class), any(Pageable.class)))
                .thenThrow(new RuntimeException());

        mockMvc.perform(get("/client/list-people"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao listar empresas clientes."));
    }

    // --- POST /client ---

    @Test
    @DisplayName("POST /client - Deve salvar cliente e retornar status 200 OK")
    void save_ShouldReturnOk_WhenSuccess() throws Exception {
        ClientDto dto = new ClientDto();
        dto.setId(1L);

        when(clientService.save(any(ClientDto.class), any())).thenReturn(dto);

        mockMvc.perform(post("/client")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /client - Deve retornar 400 Bad Request em NoSuchElementException")
    void save_ShouldReturnBadRequest_WhenNoSuchElementException() throws Exception {
        ClientDto dto = new ClientDto();
        dto.setId(10L);

        when(clientService.save(any(ClientDto.class), any())).thenThrow(new NoSuchElementException());

        mockMvc.perform(post("/client")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Empresa Cliente não encontrado: 10"));
    }

    @Test
    @DisplayName("POST /client - Deve retornar status 500 quando ocorrer RuntimeException")
    void save_ShouldReturnInternalServerError_WhenRuntimeException() throws Exception {
        ClientDto dto = new ClientDto();

        when(clientService.save(any(ClientDto.class), any())).thenThrow(new RuntimeException());

        mockMvc.perform(post("/client")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao salvar empresa cliente."));
    }

    // --- DELETE /client ---

    @Test
    @DisplayName("DELETE /client - Deve remover cliente com sucesso")
    void delete_ShouldReturnOk_WhenSuccess() throws Exception {
        Long id = 1L;

        mockMvc.perform(delete("/client").param("id", id.toString()))
                .andExpect(status().isOk());

        verify(clientService).delete(id);
    }

    @Test
    @DisplayName("DELETE /client - Deve retornar status 400 em NoSuchElementException")
    void delete_ShouldReturnBadRequest_WhenNotFound() throws Exception {
        Long id = 99L;
        doThrow(new NoSuchElementException()).when(clientService).delete(id);

        mockMvc.perform(delete("/client").param("id", id.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Empresa Cliente não encontrado: 99"));
    }

    @Test
    @DisplayName("DELETE /client - Deve retornar status 500 em erro genérico")
    void delete_ShouldReturnInternalServerError_WhenRuntimeException() throws Exception {
        Long id = 1L;
        doThrow(new RuntimeException()).when(clientService).delete(id);

        mockMvc.perform(delete("/client").param("id", id.toString()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao remover empresa cliente."));
    }

    @Test
    @DisplayName("POST /client/load-employees - Deve carregar arquivo com sucesso")
    void loadEmployees_ShouldReturnOk_WhenSuccess() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", "dados".getBytes());
        
        // Instancia ou mocka o tipo esperado pelo service
        ResultadoCargaEmpregadosDto resultadoMock = mock(ResultadoCargaEmpregadosDto.class);

        when(clientService.loadEmployees(eq(1L), any(), any())).thenReturn(resultadoMock);

        mockMvc.perform(multipart("/client/load-employees")
                        .file(file)
                        .param("idEmpresa", "1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /client/load-employees - Deve retornar 400 se empresa não for encontrada")
    void loadEmployees_ShouldReturnBadRequest_WhenCompanyNotFound() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", "dados".getBytes());

        when(clientService.loadEmployees(eq(1L), any(), any())).thenThrow(new NoSuchElementException());

        mockMvc.perform(multipart("/client/load-employees")
                        .file(file)
                        .param("idEmpresa", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Empresa não encontrada de id: 1"));
    }

    @Test
    @DisplayName("POST /client/load-employees - Deve retornar status 500 em erro de IO ou RuntimeException")
    void loadEmployees_ShouldReturnInternalServerError_WhenException() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", "dados".getBytes());

        when(clientService.loadEmployees(eq(1L), any(), any())).thenThrow(new RuntimeException("Falha IO"));

        mockMvc.perform(multipart("/client/load-employees")
                        .file(file)
                        .param("idEmpresa", "1"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao carregar empregados da empresa: 1."));
    }

    @Test
    @DisplayName("GET /client/list-people - Deve ordenar por direção informada no parâmetro sort (sort.length > 1)")
    void listPeople_ShouldSortWithDirection_WhenSortLengthIsGreaterThanOne() throws Exception {
        Page<ClientDto> pageMock = new PageImpl<>(List.of());
        when(clientService.list(any(PersonFilterDto.class), any(Pageable.class))).thenReturn(pageMock);

        mockMvc.perform(get("/client/list-people")
                        .param("sort", "nome,desc"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /client/list-people - Deve aplicar ordenação padrão 'asc' quando direção não for informada (sort.length == 1)")
    void listPeople_ShouldDefaultToAsc_WhenSortLengthIsOne() throws Exception {
        Page<ClientDto> pageMock = new PageImpl<>(List.of());
        when(clientService.list(any(PersonFilterDto.class), any(Pageable.class))).thenReturn(pageMock);

        mockMvc.perform(get("/client/list-people")
                        .param("sort", "nome"))
                .andExpect(status().isOk());
    }

}