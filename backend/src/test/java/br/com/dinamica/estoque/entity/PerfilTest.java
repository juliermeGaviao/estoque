package br.com.dinamica.estoque.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PerfilTest {

    @Test
    @DisplayName("Deve testar construtor padrao, setters, getters e getAuthority")
    void testSettersGettersAndAuthority() {
        Perfil perfil = new Perfil();
        
        Long id = 1L;
        String nome = "ROLE_ADMIN";
        Usuario usuario = new Usuario();
        LocalDateTime agora = LocalDateTime.now();

        perfil.setId(id);
        perfil.setNome(nome);
        perfil.setUsuario(usuario);
        perfil.setDataCriacao(agora);
        perfil.setDataAlteracao(agora);

        assertEquals(id, perfil.getId());
        assertEquals(nome, perfil.getNome());
        assertEquals(usuario, perfil.getUsuario());
        assertEquals(agora, perfil.getDataCriacao());
        assertEquals(agora, perfil.getDataAlteracao());
        
        // Valida implementacao da interface GrantedAuthority
        assertEquals(nome, perfil.getAuthority());
    }

    @Test
    @DisplayName("Deve testar construtor com todos os argumentos")
    void testAllArgsConstructor() {
        Long id = 1L;
        String nome = "ROLE_USER";
        Usuario usuario = new Usuario();
        LocalDateTime agora = LocalDateTime.now();

        Perfil perfil = new Perfil(id, nome, usuario, agora, agora);

        assertEquals(id, perfil.getId());
        assertEquals(nome, perfil.getNome());
        assertEquals(usuario, perfil.getUsuario());
        assertEquals(agora, perfil.getDataCriacao());
        assertEquals(agora, perfil.getDataAlteracao());
    }

    @Test
    @DisplayName("Deve testar equals, hashCode e toString gerados pelo Lombok")
    void testEqualsHashCodeAndToString() {
        LocalDateTime agora = LocalDateTime.now();
        Usuario usuario = new Usuario();

        Perfil perfil1 = new Perfil(1L, "ROLE_ADMIN", usuario, agora, agora);
        Perfil perfil2 = new Perfil(1L, "ROLE_ADMIN", usuario, agora, agora);
        Perfil perfilDiferente = new Perfil(2L, "ROLE_USER", usuario, agora, agora);

        // Equals e HashCode
        assertEquals(perfil1, perfil2);
        assertEquals(perfil1.hashCode(), perfil2.hashCode());
        assertNotEquals(perfil1, perfilDiferente);
        assertNotEquals(perfil1, null);
        assertNotEquals(perfil1, new Object());

        // ToString
        assertNotNull(perfil1.toString());
    }
}