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

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.dinamica.estoque.dto.UserDto;
import br.com.dinamica.estoque.dto.UserRequestDTO;
import br.com.dinamica.estoque.service.ProfileService;
import br.com.dinamica.estoque.service.UserService;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ProfileService profileService;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /user/profiles - Deve retornar lista de perfis com sucesso")
    void getProfiles_ShouldReturnOk() throws Exception {
        when(profileService.getProfiles()).thenReturn(List.of());

        mockMvc.perform(get("/user/profiles"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /user/profiles - Deve retornar 500 em erro generico")
    void getProfiles_ShouldReturn500_WhenError() throws Exception {
        when(profileService.getProfiles()).thenThrow(new RuntimeException());

        mockMvc.perform(get("/user/profiles"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao recuperar lista de perfis."));
    }

    @Test
    @DisplayName("GET /user/get - Deve buscar usuario por ID com sucesso")
    void getUser_ShouldReturnOk() throws Exception {
        Long id = 1L;
        when(userService.getUser(id)).thenReturn(new UserDto());

        mockMvc.perform(get("/user/get").param("id", id.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /user/get - Deve retornar 400 em NoSuchElementException")
    void getUser_ShouldReturn400_WhenNotFound() throws Exception {
        Long id = 99L;
        when(userService.getUser(id)).thenThrow(new NoSuchElementException());

        mockMvc.perform(get("/user/get").param("id", id.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Usuário não encontrado: 99"));
    }

    @Test
    @DisplayName("GET /user/list - Deve listar usuarios ordenando por desc quando sort.length > 1")
    void list_ShouldReturnOk_Desc() throws Exception {
        when(userService.list(any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/user/list")
                        .param("email", "teste@email.com")
                        .param("sort", "id", "desc"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /user/list - Deve listar usuarios ordenando por asc quando sort.length > 1")
    void list_ShouldReturnOk_Asc() throws Exception {
        when(userService.list(any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/user/list")
                        .param("sort", "id", "asc"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /user/list - Deve usar direcao padrao 'asc' quando sort.length == 1")
    void list_ShouldReturnOk_SingleSortParam() throws Exception {
        when(userService.list(any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/user/list")
                        .param("sort", "id"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /user/list - Deve retornar 500 em erro generico na listagem")
    void list_ShouldReturn500_WhenError() throws Exception {
        when(userService.list(any(), any(Pageable.class))).thenThrow(new RuntimeException());

        mockMvc.perform(get("/user/list"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao listar usuários."));
    }

    @Test
    @DisplayName("POST /user - Deve salvar usuario com sucesso")
    void save_ShouldReturnOk() throws Exception {
        UserRequestDTO dto = new UserRequestDTO();
        when(userService.save(any(UserRequestDTO.class), any())).thenReturn(new UserDto());

        mockMvc.perform(post("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /user - Deve retornar 400 em NoSuchElementException")
    void save_ShouldReturn400_WhenNotFound() throws Exception {
        UserRequestDTO dto = new UserRequestDTO();
        dto.setId(99L);
        when(userService.save(any(UserRequestDTO.class), any())).thenThrow(new NoSuchElementException());

        mockMvc.perform(post("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Usuário não encontrado: 99"));
    }

    @Test
    @DisplayName("POST /user - Deve retornar 400 em DataIntegrityViolationException")
    void save_ShouldReturn400_WhenDataIntegrityViolation() throws Exception {
        UserRequestDTO dto = new UserRequestDTO();
        dto.setEmail("existente@email.com");
        when(userService.save(any(UserRequestDTO.class), any())).thenThrow(new DataIntegrityViolationException("Erro de duplicidade"));

        mockMvc.perform(post("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Existe outro usuário com o e-mail existente@email.com"));
    }

    @Test
    @DisplayName("POST /user - Deve retornar 400 em ConstraintViolationException")
    void save_ShouldReturn400_WhenConstraintViolation() throws Exception {
        UserRequestDTO dto = new UserRequestDTO();
        dto.setEmail("existente@email.com");
        when(userService.save(any(UserRequestDTO.class), any())).thenThrow(new ConstraintViolationException("Violacao de constraint", null, "uk_email"));

        mockMvc.perform(post("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Existe outro usuário com o e-mail existente@email.com"));
    }

    @Test
    @DisplayName("POST /user - Deve retornar 500 em erro generico ao salvar usuario")
    void save_ShouldReturn500_WhenError() throws Exception {
        UserRequestDTO dto = new UserRequestDTO();
        when(userService.save(any(UserRequestDTO.class), any())).thenThrow(new RuntimeException());

        mockMvc.perform(post("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao salvar usuário."));
    }

    @Test
    @DisplayName("POST /user/password - Deve alterar senha com sucesso")
    void password_ShouldReturnOk() throws Exception {
        UserRequestDTO dto = new UserRequestDTO();
        when(userService.changePassword(any(UserRequestDTO.class))).thenReturn(new UserDto());

        mockMvc.perform(post("/user/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /user/password - Deve retornar 400 em NoSuchElementException ao trocar senha")
    void password_ShouldReturn400_WhenNotFound() throws Exception {
        UserRequestDTO dto = new UserRequestDTO();
        dto.setId(99L);
        when(userService.changePassword(any(UserRequestDTO.class))).thenThrow(new NoSuchElementException());

        mockMvc.perform(post("/user/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Usuário não encontrado: 99"));
    }

    @Test
    @DisplayName("POST /user/password - Deve retornar 500 em erro generico ao trocar senha")
    void password_ShouldReturn500_WhenError() throws Exception {
        UserRequestDTO dto = new UserRequestDTO();
        when(userService.changePassword(any(UserRequestDTO.class))).thenThrow(new RuntimeException());

        mockMvc.perform(post("/user/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao trocar a senha do usuário."));
    }

    @Test
    @DisplayName("DELETE /user - Deve remover usuario com sucesso")
    void delete_ShouldReturnOk() throws Exception {
        Long id = 1L;

        mockMvc.perform(delete("/user").param("id", id.toString()))
                .andExpect(status().isOk());

        verify(userService).delete(id);
    }

    @Test
    @DisplayName("DELETE /user - Deve retornar 400 em NoSuchElementException ao deletar")
    void delete_ShouldReturn400_WhenNotFound() throws Exception {
        Long id = 99L;
        doThrow(new NoSuchElementException()).when(userService).delete(id);

        mockMvc.perform(delete("/user").param("id", id.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Usuário não encontrado: 99"));
    }

    @Test
    @DisplayName("DELETE /user - Deve retornar 500 em erro generico ao deletar")
    void delete_ShouldReturn500_WhenError() throws Exception {
        Long id = 1L;
        doThrow(new RuntimeException()).when(userService).delete(id);

        mockMvc.perform(delete("/user").param("id", id.toString()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erro ao remover usuário."));
    }
}