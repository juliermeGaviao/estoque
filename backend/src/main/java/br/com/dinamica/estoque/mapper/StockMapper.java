package br.com.dinamica.estoque.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import br.com.dinamica.estoque.dto.StockDto;
import br.com.dinamica.estoque.entity.Estoque;

@Mapper(componentModel = "spring") 
public interface StockMapper {

    @Mapping(target = "venda.cliente", ignore = true)
    @Mapping(target = "pedidoCompra.estoque", ignore = true)
    @Mapping(target = "produto.estoque", ignore = true)
    @Mapping(target = "transferenciaEstoque.estoque", ignore = true)
    StockDto toDto(Estoque entity);

	@Mapping(target = "usuario", ignore = true)
	@Mapping(target = "dataCriacao", ignore = true)
    @Mapping(target = "venda.cliente", ignore = true)
    @Mapping(target = "venda.usuario", ignore = true)
    @Mapping(target = "venda.dataCriacao", ignore = true)
    @Mapping(target = "venda.dataAlteracao", ignore = true)
    @Mapping(target = "venda.vendedor", ignore = true)
    @Mapping(target = "venda.pontoVenda", ignore = true)
    @Mapping(target = "venda.tabela", ignore = true)
    @Mapping(target = "pontoVenda.usuario", ignore = true)
    @Mapping(target = "pontoVenda.dataCriacao", ignore = true)
    @Mapping(target = "pontoVenda.dataAlteracao", ignore = true)
    @Mapping(target = "pedidoCompra.usuario", ignore = true)
    @Mapping(target = "pedidoCompra.dataCriacao", ignore = true)
    @Mapping(target = "pedidoCompra.fornecedor", ignore = true)
    @Mapping(target = "produto.usuario", ignore = true)
    @Mapping(target = "produto.dataCriacao", ignore = true)
    @Mapping(target = "produto.dataAlteracao", ignore = true)
    @Mapping(target = "produto.fornecedor", ignore = true)
    @Mapping(target = "produto.tipoProduto", ignore = true)
	@Mapping(target = "transferenciaEstoque.usuario", ignore = true)
	@Mapping(target = "transferenciaEstoque.dataCriacao", ignore = true)
	@Mapping(target = "transferenciaEstoque.pontoVendaOrigem", ignore = true)
	@Mapping(target = "transferenciaEstoque.pontoVendaDestino", ignore = true)
    void updateEntityFromDto(StockDto dto, @MappingTarget Estoque entity);

}