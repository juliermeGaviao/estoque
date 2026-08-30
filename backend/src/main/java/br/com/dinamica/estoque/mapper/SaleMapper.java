package br.com.dinamica.estoque.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import br.com.dinamica.estoque.dto.SaleDto;
import br.com.dinamica.estoque.entity.Venda;

@Mapper(componentModel = "spring") 
public interface SaleMapper {

	@Mapping(target = "cliente.cnpj", ignore = true)
	@Mapping(target = "cliente.cracha", ignore = true)
	@Mapping(target = "cliente.empresa", ignore = true)
	@Mapping(target = "cliente.razaoSocial", ignore = true)
	@Mapping(target = "cliente.dataAniversario", ignore = true)
	@Mapping(target = "cliente.limite", ignore = true)
    SaleDto toDto(Venda entity);

	@Mapping(target = "usuario", ignore = true)
	@Mapping(target = "dataCriacao", ignore = true)
	@Mapping(target = "dataAlteracao", ignore = true)
	@Mapping(target = "pontoVenda", ignore = true)
	@Mapping(target = "cliente", ignore = true)
	@Mapping(target = "vendedor", ignore = true)
	@Mapping(target = "tabela", ignore = true)
    void updateEntityFromDto(SaleDto dto, @MappingTarget Venda entity);

}