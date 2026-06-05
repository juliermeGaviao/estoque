package br.com.dinamica.estoque.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import br.com.dinamica.estoque.dto.PersonClientContactDto;
import br.com.dinamica.estoque.entity.ContatoClientePessoa;

@Mapper(componentModel = "spring") 
public interface PersonClientContactMapper {

	@Mapping(target = "cliente", ignore = true)
    PersonClientContactDto toDto(ContatoClientePessoa entity);

	@Mapping(target = "usuario", ignore = true)
	@Mapping(target = "dataCriacao", ignore = true)
	@Mapping(target = "dataAlteracao", ignore = true)
	@Mapping(target = "cliente", ignore = true)
    void updateEntityFromDto(PersonClientContactDto dto, @MappingTarget ContatoClientePessoa entity);

}