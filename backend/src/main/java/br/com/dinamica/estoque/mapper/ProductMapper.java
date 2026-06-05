package br.com.dinamica.estoque.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import br.com.dinamica.estoque.dto.ProductDto;
import br.com.dinamica.estoque.entity.Produto;

@Mapper(componentModel = "spring") 
public interface ProductMapper {

	@Mapping(target = "estoque", ignore = true)
    ProductDto toDto(Produto entity);

	@Mapping(target = "usuario", ignore = true)
	@Mapping(target = "dataCriacao", ignore = true)
	@Mapping(target = "dataAlteracao", ignore = true)
	@Mapping(target = "tipoProduto", ignore = true)
	@Mapping(target = "fornecedor", ignore = true)
    void updateEntityFromDto(ProductDto dto, @MappingTarget Produto entity);

}