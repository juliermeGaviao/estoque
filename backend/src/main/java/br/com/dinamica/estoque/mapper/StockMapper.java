package br.com.dinamica.estoque.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import br.com.dinamica.estoque.dto.StockDto;
import br.com.dinamica.estoque.entity.Estoque;

@Mapper(componentModel = "spring") 
public interface StockMapper {

    @Mapping(target = "venda.cliente", ignore = true)
    @Mapping(target = "pedidoCompra.estoque", ignore = true)
    @Mapping(target = "produto.estoque", ignore = true)
    @Mapping(target = "transferenciaEstoque.estoque", ignore = true)
    StockDto toDto(Estoque entity);

}