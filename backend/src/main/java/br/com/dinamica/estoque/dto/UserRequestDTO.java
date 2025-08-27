package br.com.dinamica.estoque.dto;

import java.util.List;

public record UserRequestDTO(Long id, String email, String senha, List<Long> perfis) {}
