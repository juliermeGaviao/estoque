package br.com.dinamica.estoque.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import br.com.dinamica.estoque.dto.ProductTypeDto;
import br.com.dinamica.estoque.entity.Usuario;

public interface ProductTypeService {

	ProductTypeDto get(Long id);

	Page<ProductTypeDto> list(String nome, Pageable pageable);

	ProductTypeDto save(ProductTypeDto dto, Usuario usuario);

	void delete(Long id);

}
