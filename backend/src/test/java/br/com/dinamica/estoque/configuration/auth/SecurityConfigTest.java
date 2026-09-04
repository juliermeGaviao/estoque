package br.com.dinamica.estoque.configuration.auth;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private SecurityFilter securityFilter;

    @Mock
    private AuthenticationConfiguration authenticationConfiguration;

    private SecurityConfig securityConfig;
    private final String allowedOrigins = "http://localhost:3000";

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig(securityFilter, allowedOrigins);
    }

    @Test
    @DisplayName("filterChain - deve configurar httpSecurity e executar todos os Customizers das lambdas")
    @SuppressWarnings({ "rawtypes", "unchecked" })
    void filterChain_success() throws Exception {
        HttpSecurity http = mock(HttpSecurity.class);
        DefaultSecurityFilterChain expectedChain = mock(DefaultSecurityFilterChain.class);

        org.springframework.security.config.annotation.web.configurers.CsrfConfigurer csrfConfigurer = 
                mock(org.springframework.security.config.annotation.web.configurers.CsrfConfigurer.class);
        org.springframework.security.config.annotation.web.configurers.CorsConfigurer corsConfigurer = 
                mock(org.springframework.security.config.annotation.web.configurers.CorsConfigurer.class);
        org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer sessionConfigurer = 
                mock(org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer.class);
        
        // Uso de DEEP_STUBS para resolver o encadeamento do requestMatchers().permitAll().anyRequest().authenticated()
        org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer.AuthorizationManagerRequestMatcherRegistry authRegistry = 
                mock(org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer.AuthorizationManagerRequestMatcherRegistry.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);

        when(http.csrf(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            org.springframework.security.config.Customizer customizer = invocation.getArgument(0);
            customizer.customize(csrfConfigurer);
            return http;
        });

        when(http.cors(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            org.springframework.security.config.Customizer customizer = invocation.getArgument(0);
            customizer.customize(corsConfigurer);
            return http;
        });

        when(http.sessionManagement(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            org.springframework.security.config.Customizer customizer = invocation.getArgument(0);
            customizer.customize(sessionConfigurer);
            return http;
        });

        when(http.authorizeHttpRequests(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            org.springframework.security.config.Customizer customizer = invocation.getArgument(0);
            customizer.customize(authRegistry);
            return http;
        });

        when(http.addFilterBefore(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(http);
        when(http.build()).thenReturn(expectedChain);

        SecurityFilterChain result = securityConfig.filterChain(http);

        assertNotNull(result);
        verify(csrfConfigurer).disable();
        verify(sessionConfigurer).sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS);
        verify(http).build();
    }

    @Test
    @DisplayName("corsConfigurer - deve registrar as configuracoes de CORS corretamente")
    void corsConfigurer_success() {
        WebMvcConfigurer configurer = securityConfig.corsConfigurer();
        assertNotNull(configurer);

        CorsRegistry registry = mock(CorsRegistry.class);
        CorsRegistration registration = mock(CorsRegistration.class);

        when(registry.addMapping("/**")).thenReturn(registration);
        when(registration.allowedOrigins(allowedOrigins)).thenReturn(registration);
        when(registration.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")).thenReturn(registration);
        when(registration.allowedHeaders("*")).thenReturn(registration);
        when(registration.allowCredentials(true)).thenReturn(registration);

        configurer.addCorsMappings(registry);

        verify(registry).addMapping("/**");
        verify(registration).allowedOrigins(allowedOrigins);
        verify(registration).allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
        verify(registration).allowedHeaders("*");
        verify(registration).allowCredentials(true);
    }

    @Test
    @DisplayName("authManager - deve delegar a obtencao do AuthenticationManager")
    void authManager_success() throws Exception {
        AuthenticationManager expectedManager = mock(AuthenticationManager.class);
        when(authenticationConfiguration.getAuthenticationManager()).thenReturn(expectedManager);

        AuthenticationManager result = securityConfig.authManager(authenticationConfiguration);

        assertNotNull(result);
        verify(authenticationConfiguration).getAuthenticationManager();
    }
}