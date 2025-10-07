package br.com.dinamica.estoque.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import br.com.dinamica.estoque.dto.SaleItemDto;
import br.com.dinamica.estoque.entity.Usuario;

public interface SaleItemService {

	SaleItemDto get(Long id);

	Page<SaleItemDto> list(Long idVenda, Long idTabelaPrecoProduto, Integer minQuantidade, Integer maxQuantidade, Pageable pageable);

	SaleItemDto save(SaleItemDto dto, Usuario usuario);

	void delete(Long id);

}
