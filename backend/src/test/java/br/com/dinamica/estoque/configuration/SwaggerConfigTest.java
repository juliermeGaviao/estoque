package br.com.dinamica.estoque.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;

class SwaggerConfigTest {

    private SwaggerConfig swaggerConfig;

    @BeforeEach
    void setUp() {
        swaggerConfig = new SwaggerConfig();
    }

    @Test
    @DisplayName("publicApi - deve construir e retornar o bean GroupedOpenApi com as configuracoes corretas")
    void publicApi_success() {
        GroupedOpenApi groupedOpenApi = swaggerConfig.publicApi();

        assertNotNull(groupedOpenApi);
        assertEquals("public-apis", groupedOpenApi.getGroup());
    }

    @Test
    @DisplayName("customOpenAPI - deve construir o OpenAPI com titulo, versao e esquema de seguranca Bearer JWT")
    void customOpenAPI_success() {
        OpenAPI openAPI = swaggerConfig.customOpenAPI();

        assertNotNull(openAPI);
        assertNotNull(openAPI.getInfo());
        assertEquals("API Base", openAPI.getInfo().getTitle());
        assertEquals("1.0", openAPI.getInfo().getVersion());

        // Valida as configuracoes de seguranca
        assertNotNull(openAPI.getSecurity());
        assertTrue(openAPI.getSecurity().stream()
                .anyMatch(security -> security.containsKey("bearerAuth")));

        assertNotNull(openAPI.getComponents());
        assertNotNull(openAPI.getComponents().getSecuritySchemes());
        
        SecurityScheme bearerScheme = openAPI.getComponents().getSecuritySchemes().get("bearerAuth");
        assertNotNull(bearerScheme);
        assertEquals(SecurityScheme.Type.HTTP, bearerScheme.getType());
        assertEquals("bearer", bearerScheme.getScheme());
        assertEquals("JWT", bearerScheme.getBearerFormat());
    }
}