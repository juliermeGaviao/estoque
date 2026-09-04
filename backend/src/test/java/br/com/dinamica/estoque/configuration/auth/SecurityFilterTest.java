package br.com.dinamica.estoque.configuration.auth;

import br.com.dinamica.estoque.service.impl.JwtService;
import br.com.dinamica.estoque.service.impl.UserServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserServiceImpl userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private SecurityFilter securityFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Deve ignorar validação e prosseguir a cadeia quando for requisição OPTIONS (CORS)")
    void doFilter_ShouldSkip_WhenMethodIsOptions() throws ServletException, IOException {
        when(request.getMethod()).thenReturn("OPTIONS");

        securityFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService, userDetailsService);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("Deve prosseguir sem autenticar quando cabeçalho Authorization estiver ausente")
    void doFilter_ShouldNotAuthenticate_WhenHeaderIsMissing() throws ServletException, IOException {
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn(null);

        securityFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService, userDetailsService);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("Deve prosseguir sem autenticar quando o token não iniciar com 'Bearer '")
    void doFilter_ShouldNotAuthenticate_WhenHeaderIsNotBearer() throws ServletException, IOException {
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

        securityFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService, userDetailsService);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("Deve prosseguir sem autenticar quando o token for inválido/subject for nulo")
    void doFilter_ShouldNotAuthenticate_WhenSubjectIsNull() throws ServletException, IOException {
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Bearer token_invalido");
        when(jwtService.getSubject("token_invalido")).thenReturn(null);

        securityFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtService).getSubject("token_invalido");
        verifyNoInteractions(userDetailsService);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("Deve autenticar com sucesso no contexto quando o token for válido e não houver autenticação prévia")
    void doFilter_ShouldAuthenticate_WhenTokenIsValid() throws ServletException, IOException {
        String token = "jwt_valido";
        String username = "usuario.teste";

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getAuthorities()).thenReturn(Collections.emptyList());

        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.getSubject(token)).thenReturn(username);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);

        securityFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(userDetails, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    }

    @Test
    @DisplayName("Não deve reautenticar se já houver uma autenticação no SecurityContextHolder")
    void doFilter_ShouldNotReauthenticate_WhenAlreadyAuthenticated() throws ServletException, IOException {
        String token = "jwt_valido";
        String username = "usuario.teste";

        UsernamePasswordAuthenticationToken existingAuth = 
                new UsernamePasswordAuthenticationToken("usuario_existente", null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.getSubject(token)).thenReturn(username);

        securityFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(userDetailsService, never()).loadUserByUsername(username);
        assertEquals(existingAuth, SecurityContextHolder.getContext().getAuthentication());
    }
}