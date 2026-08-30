package br.com.dinamica.estoque.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import br.com.dinamica.estoque.dto.PriceTableDto;
import br.com.dinamica.estoque.entity.TabelaPreco;

@Mapper(componentModel = "spring") 
public interface PriceTableMapper {

	PriceTableDto toDto(TabelaPreco entity);

	@Mapping(target = "usuario", ignore = true)
	@Mapping(target = "dataCriacao", ignore = true)
	@Mapping(target = "dataAlteracao", ignore = true)
    void updateEntityFromDto(PriceTableDto dto, @MappingTarget TabelaPreco entity);

}