package br.com.dinamica.estoque.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import br.com.dinamica.estoque.dto.ProductDto;
import br.com.dinamica.estoque.dto.ProductFilterDto;
import br.com.dinamica.estoque.entity.Usuario;

public interface ProductService {

	ProductDto get(Long id);

	Page<ProductDto> list(ProductFilterDto filter, Pageable pageable);

	ProductDto save(ProductDto dto, Usuario usuario);

	void delete(Long id);

}
