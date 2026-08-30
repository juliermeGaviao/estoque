package br.com.dinamica.estoque.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import br.com.dinamica.estoque.dto.UserPriceTableDto;
import br.com.dinamica.estoque.entity.UsuarioTabelaPreco;

@Mapper(componentModel = "spring") 
public interface UserPriceTableMapper {

	@Mapping(target = "usuario.perfis", ignore = true)
    UserPriceTableDto toDto(UsuarioTabelaPreco entity);

	@Mapping(target = "tabela.usuario", ignore = true)
    @Mapping(target = "tabela.dataCriacao", ignore = true)
    @Mapping(target = "tabela.dataAlteracao", ignore = true)
	@Mapping(target = "usuario", ignore = true)
    void updateEntityFromDto(UserPriceTableDto dto, @MappingTarget UsuarioTabelaPreco entity);

}