package br.com.dinamica.estoque.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.dinamica.estoque.dto.AuthRequest;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.repository.UsuarioRepository;
import br.com.dinamica.estoque.service.impl.JwtService;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuthenticationManager authManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder encoder;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    @Test
    @DisplayName("Deve realizar login com sucesso e retornar token JWT com perfis")
    void login_ShouldReturnAuthResponse_WhenCredentialsAreValid() throws Exception {
        AuthRequest request = new AuthRequest("usuario@teste.com", "senha123");
        String jwtToken = "jwt.token.valido";

        Usuario mockUsuario = mock(Usuario.class);
        @SuppressWarnings("rawtypes")
		Collection authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));

        when(mockUsuario.getId()).thenReturn(1L);
        doReturn(authorities).when(mockUsuario).getAuthorities();

        when(jwtService.gerarToken(request.email())).thenReturn(jwtToken);
        when(userDetailsService.loadUserByUsername(request.email())).thenReturn(mockUsuario);

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.token").value(jwtToken))
                .andExpect(jsonPath("$.perfis[0]").value("ROLE_ADMIN"));

        verify(authManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("Deve falhar no login quando as credenciais forem inválidas")
    void login_ShouldReturnUnauthorized_WhenCredentialsAreInvalid() throws Exception {
        AuthRequest request = new AuthRequest("usuario@teste.com", "senhaIncorreta");

        doThrow(new BadCredentialsException("Credenciais inválidas"))
                .when(authManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

        // Valida diretamente que a chamada ao controller lança a exceção esperada sem a necessidade do try-catch manual
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> 
            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
        ).hasCauseInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("Deve cadastrar novo usuário com sucesso")
    void register_ShouldSaveUsuario_WhenRequestIsValid() throws Exception {
        AuthRequest request = new AuthRequest("novo@teste.com", "senha123");
        new Usuario(1L, "admin@teste.com", "encodedSenha", true, null, LocalDateTime.now(), LocalDateTime.now(), Set.of());

        when(encoder.encode(request.senha())).thenReturn("senhaCodificada");

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(usuarioRepository).save(any(Usuario.class));
    }
}