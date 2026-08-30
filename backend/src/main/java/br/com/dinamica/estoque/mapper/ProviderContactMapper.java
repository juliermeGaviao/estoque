package br.com.dinamica.estoque.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import br.com.dinamica.estoque.dto.ProviderContactDto;
import br.com.dinamica.estoque.entity.ContatoFornecedor;

@Mapper(componentModel = "spring") 
public interface ProviderContactMapper {

	@Mapping(target = "fornecedor", ignore = true)
    ProviderContactDto toDto(ContatoFornecedor entity);

	@Mapping(target = "usuario", ignore = true)
	@Mapping(target = "dataCriacao", ignore = true)
	@Mapping(target = "dataAlteracao", ignore = true)
	@Mapping(target = "fornecedor", ignore = true)
    void updateEntityFromDto(ProviderContactDto dto, @MappingTarget ContatoFornecedor entity);

}