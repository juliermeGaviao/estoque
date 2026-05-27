package br.com.dinamica.estoque.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import br.com.dinamica.estoque.dto.SalePointDto;
import br.com.dinamica.estoque.entity.Usuario;

public interface SalePointService {

	SalePointDto get(Long id);

	Page<SalePointDto> list(String nome, Pageable pageable);

	SalePointDto save(SalePointDto dto, Usuario usuario);

	void delete(Long id);

}
