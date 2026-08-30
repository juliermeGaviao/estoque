package br.com.dinamica.estoque.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import br.com.dinamica.estoque.dto.SaleItemDto;
import br.com.dinamica.estoque.entity.ItemVenda;

@Mapper(componentModel = "spring") 
public interface SaleItemMapper {

	@Mapping(target = "venda.cliente", ignore = true)
	@Mapping(target = "tabelaPrecoProduto.produto", ignore = true)
    SaleItemDto toDto(ItemVenda entity);

	@Mapping(target = "usuario", ignore = true)
	@Mapping(target = "dataCriacao", ignore = true)
	@Mapping(target = "dataAlteracao", ignore = true)
	@Mapping(target = "venda", ignore = true)
	@Mapping(target = "tabelaPrecoProduto", ignore = true)
    void updateEntityFromDto(SaleItemDto dto, @MappingTarget ItemVenda entity);

}