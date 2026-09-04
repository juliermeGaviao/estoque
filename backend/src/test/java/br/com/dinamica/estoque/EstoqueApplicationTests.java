package br.com.dinamica.estoque;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class EstoqueApplicationTests {

    @Test
    @DisplayName("Deve executar o método main da aplicação garantindo cobertura total")
    void main() {
        try (MockedStatic<SpringApplication> springAppMock = Mockito.mockStatic(SpringApplication.class)) {
            assertDoesNotThrow(() -> EstoqueApplication.main(new String[]{}));
            springAppMock.verify(() -> SpringApplication.run(EstoqueApplication.class, new String[]{}));
        }
    }
}