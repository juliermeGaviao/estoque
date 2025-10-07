package br.com.dinamica.estoque.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import br.com.dinamica.estoque.dto.SaleDto;
import br.com.dinamica.estoque.entity.Usuario;

public interface SaleService {

	SaleDto get(Long id);

	Page<SaleDto> list(Long vendedorId, Float minDesconto, Float maxDesconto, String observacoes, Pageable pageable);

	SaleDto save(SaleDto dto, Usuario usuario);

	void delete(Long id);

}
