package br.com.dinamica.estoque.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import br.com.dinamica.estoque.dto.PriceTableDto;
import br.com.dinamica.estoque.entity.Usuario;

public interface PriceTableService {

	PriceTableDto get(Long id);

	Page<PriceTableDto> list(String nome, Pageable pageable);

	PriceTableDto save(PriceTableDto dto, Usuario usuario);

	void delete(Long id);

}
