package br.com.dinamica.estoque.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import br.com.dinamica.estoque.dto.ProfileDto;
import br.com.dinamica.estoque.dto.UserDto;
import br.com.dinamica.estoque.entity.Perfil;
import br.com.dinamica.estoque.entity.Usuario;

@Mapper(componentModel = "spring") 
public interface UserMapper {

	UserDto toDto(Usuario entity);

	@Mapping(target = "senha", ignore = true)
	@Mapping(target = "cadastrante", ignore = true)
	@Mapping(target = "ativo", ignore = true)
	@Mapping(target = "authorities", ignore = true)
	@Mapping(target = "dataCriacao", ignore = true)
	@Mapping(target = "dataAlteracao", ignore = true)
    void updateEntityFromDto(UserDto dto, @MappingTarget Usuario entity);

	@Mapping(target = "usuario", ignore = true)
    @Mapping(target = "dataCriacao", ignore = true)
    @Mapping(target = "dataAlteracao", ignore = true)
    Perfil profileDtoToPerfil(ProfileDto dto);

}