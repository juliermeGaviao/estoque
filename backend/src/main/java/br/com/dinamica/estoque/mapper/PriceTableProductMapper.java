package br.com.dinamica.estoque.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import br.com.dinamica.estoque.dto.PriceTableProductDto;
import br.com.dinamica.estoque.entity.TabelaPrecoProduto;

@Mapper(componentModel = "spring") 
public interface PriceTableProductMapper {

	@Mapping(target = "produto.estoque", ignore = true)
	@Mapping(target = "produto.tipoProduto", ignore = true)
	@Mapping(target = "produto.fornecedor", ignore = true)
	PriceTableProductDto toDto(TabelaPrecoProduto entity);

	@Mapping(target = "usuario", ignore = true)
	@Mapping(target = "dataCriacao", ignore = true)
	@Mapping(target = "dataAlteracao", ignore = true)
	@Mapping(target = "tabela.usuario", ignore = true)
	@Mapping(target = "tabela.dataCriacao", ignore = true)
	@Mapping(target = "tabela.dataAlteracao", ignore = true)
	@Mapping(target = "produto.usuario", ignore = true)
	@Mapping(target = "produto.dataCriacao", ignore = true)
	@Mapping(target = "produto.dataAlteracao", ignore = true)
	@Mapping(target = "produto.fornecedor", ignore = true)
	@Mapping(target = "produto.tipoProduto", ignore = true)
    void updateEntityFromDto(PriceTableProductDto dto, @MappingTarget TabelaPrecoProduto entity);

}