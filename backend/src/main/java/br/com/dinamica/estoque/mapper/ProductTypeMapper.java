package br.com.dinamica.estoque.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import br.com.dinamica.estoque.dto.ProductTypeDto;
import br.com.dinamica.estoque.entity.TipoProduto;

@Mapper(componentModel = "spring") 
public interface ProductTypeMapper {

    ProductTypeDto toDto(TipoProduto entity);

	@Mapping(target = "usuario", ignore = true)
	@Mapping(target = "dataCriacao", ignore = true)
	@Mapping(target = "dataAlteracao", ignore = true)
    void updateEntityFromDto(ProductTypeDto dto, @MappingTarget TipoProduto entity);

}