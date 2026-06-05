package br.com.dinamica.estoque.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import br.com.dinamica.estoque.dto.ProfileDto;
import br.com.dinamica.estoque.entity.Perfil;

@Mapper(componentModel = "spring") 
public interface ProfileMapper {

    ProfileDto toDto(Perfil entity);

	@Mapping(target = "usuario", ignore = true)
	@Mapping(target = "dataCriacao", ignore = true)
	@Mapping(target = "dataAlteracao", ignore = true)
    void updateEntityFromDto(ProfileDto dto, @MappingTarget Perfil entity);

}