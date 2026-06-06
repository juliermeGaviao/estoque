package br.com.dinamica.estoque.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import br.com.dinamica.estoque.dto.UserSalePointDto;
import br.com.dinamica.estoque.entity.UsuarioPontoVenda;

@Mapper(componentModel = "spring") 
public interface UserSalePointMapper {

    UserSalePointDto toDto(UsuarioPontoVenda entity);

	@Mapping(target = "usuario.senha", ignore = true)
	@Mapping(target = "usuario.cadastrante", ignore = true)
	@Mapping(target = "usuario.dataCriacao", ignore = true)
	@Mapping(target = "usuario.dataAlteracao", ignore = true)
	@Mapping(target = "usuario.ativo", ignore = true)
	@Mapping(target = "usuario.authorities", ignore = true)
	@Mapping(target = "usuario.perfis", ignore = true)
	@Mapping(target = "pontoVenda.usuario", ignore = true)
    @Mapping(target = "pontoVenda.dataCriacao", ignore = true)
    @Mapping(target = "pontoVenda.dataAlteracao", ignore = true)
    void updateEntityFromDto(UserSalePointDto dto, @MappingTarget UsuarioPontoVenda entity);

}