package br.com.dinamica.estoque;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class EstoqueApplicationTests {

    @Test
    @DisplayName("Deve executar o método main da aplicação garantindo cobertura total")
    void main() {
        assertDoesNotThrow(() -> EstoqueApplication.main(new String[] { "--spring.profiles.active=test" })
        );
    }
}