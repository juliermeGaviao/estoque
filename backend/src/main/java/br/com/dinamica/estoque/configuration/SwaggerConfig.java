package br.com.dinamica.estoque.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * From
 * https://medium.com/@berktorun.dev/swagger-like-a-pro-with-spring-boot-3-and-java-17-49eed0ce1d2f
 * and https://www.baeldung.com/spring-rest-openapi-documentation
 */
@Configuration
public class SwaggerConfig {

	@Bean
	GroupedOpenApi publicApi() {
		return GroupedOpenApi.builder().group("public-apis").pathsToMatch("/**").build();
	}

	/**
	 *
	 * @return
	 */
	@Bean
	OpenAPI customOpenAPI() {
		return new OpenAPI().info(new Info().title("API Base").version("1.0"))
				.addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
				.components(new Components().addSecuritySchemes("bearerAuth",
						new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
	}

}