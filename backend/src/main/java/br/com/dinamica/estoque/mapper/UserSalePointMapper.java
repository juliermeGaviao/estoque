package br.com.dinamica.estoque.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import br.com.dinamica.estoque.dto.UserSalePointDto;
import br.com.dinamica.estoque.entity.UsuarioPontoVenda;

@Mapper(componentModel = "spring", uses = { SalePointMapper.class }) 
public interface UserSalePointMapper {

    UserSalePointDto toDto(UsuarioPontoVenda entity);

	@Mapping(target = "usuario", ignore = true)
	@Mapping(target = "pontoVenda", ignore = true)
    void updateEntityFromDto(UserSalePointDto dto, @MappingTarget UsuarioPontoVenda entity);

}