package br.com.dinamica.estoque.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import br.com.dinamica.estoque.dto.SalePointDto;
import br.com.dinamica.estoque.entity.PontoVenda;

@Mapper(componentModel = "spring") 
public interface SalePointMapper {

    SalePointDto toDto(PontoVenda entity);

	@Mapping(target = "empresa", ignore = true)
	@Mapping(target = "usuario", ignore = true)
	@Mapping(target = "dataCriacao", ignore = true)
	@Mapping(target = "dataAlteracao", ignore = true)
    void updateEntityFromDto(SalePointDto dto, @MappingTarget PontoVenda entity);

}