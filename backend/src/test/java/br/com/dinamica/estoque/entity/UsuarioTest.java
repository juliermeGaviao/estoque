package br.com.dinamica.estoque.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UsuarioTest {

    @Test
    @DisplayName("Deve testar getters, setters e metodos do UserDetails")
    void testGettersSettersAndUserDetails() {
        Usuario usuario = new Usuario();

        Long id = 1L;
        String email = "usuario@dinamica.com.br";
        String senha = "password123";
        Boolean ativo = true;
        Usuario cadastrante = new Usuario();
        LocalDateTime agora = LocalDateTime.now();
        Set<Perfil> perfis = new HashSet<>();
        perfis.add(new Perfil(1L, "ROLE_ADMIN", usuario, agora, agora));

        usuario.setId(id);
        usuario.setEmail(email);
        usuario.setSenha(senha);
        usuario.setAtivo(ativo);
        usuario.setCadastrante(cadastrante);
        usuario.setDataCriacao(agora);
        usuario.setDataAlteracao(agora);
        usuario.setPerfis(perfis);

        // Getters da classe
        assertEquals(id, usuario.getId());
        assertEquals(email, usuario.getEmail());
        assertEquals(senha, usuario.getSenha());
        assertEquals(ativo, usuario.getAtivo());
        assertEquals(cadastrante, usuario.getCadastrante());
        assertEquals(agora, usuario.getDataCriacao());
        assertEquals(agora, usuario.getDataAlteracao());
        assertEquals(perfis, usuario.getPerfis());

        // Metodos da interface UserDetails
        assertEquals(perfis, usuario.getAuthorities());
        assertEquals(senha, usuario.getPassword());
        assertEquals(email, usuario.getUsername());
        assertTrue(usuario.isAccountNonExpired());
        assertTrue(usuario.isAccountNonLocked());
        assertTrue(usuario.isCredentialsNonExpired());
        assertEquals(ativo, usuario.isEnabled());
    }

    @Test
    @DisplayName("Deve testar construtor com todos os argumentos")
    void testAllArgsConstructor() {
        Long id = 1L;
        String email = "admin@dinamica.com.br";
        String senha = "secret_password";
        Boolean ativo = true;
        Usuario cadastrante = new Usuario();
        LocalDateTime agora = LocalDateTime.now();
        Set<Perfil> perfis = new HashSet<>();

        Usuario usuario = new Usuario(id, email, senha, ativo, cadastrante, agora, agora, perfis);

        assertNotNull(usuario);
        assertEquals(id, usuario.getId());
        assertEquals(email, usuario.getEmail());
        assertEquals(senha, usuario.getSenha());
        assertEquals(ativo, usuario.getAtivo());
        assertEquals(cadastrante, usuario.getCadastrante());
        assertEquals(agora, usuario.getDataCriacao());
        assertEquals(agora, usuario.getDataAlteracao());
        assertEquals(perfis, usuario.getPerfis());
    }
}