package br.com.dinamica.estoque.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import br.com.dinamica.estoque.dto.UserSalePointDto;
import br.com.dinamica.estoque.entity.UsuarioPontoVenda;

@Mapper(componentModel = "spring") 
public interface UserSalePointMapper {

    UserSalePointDto toDto(UsuarioPontoVenda entity);

	@Mapping(target = "usuario", ignore = true)
	@Mapping(target = "usuarioCadastro", ignore = true)
	@Mapping(target = "dataCriacao", ignore = true)
	@Mapping(target = "pontoVenda.usuario", ignore = true)
    @Mapping(target = "pontoVenda.dataCriacao", ignore = true)
    @Mapping(target = "pontoVenda.dataAlteracao", ignore = true)
    void updateEntityFromDto(UserSalePointDto dto, @MappingTarget UsuarioPontoVenda entity);

}