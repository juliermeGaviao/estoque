package br.com.dinamica.estoque.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import br.com.dinamica.estoque.dto.StockTransferDto;
import br.com.dinamica.estoque.entity.TransferenciaEstoque;

@Mapper(componentModel = "spring") 
public interface StockTransferMapper {

	@Mapping(target = "estoque", ignore = true)
    StockTransferDto toDto(TransferenciaEstoque entity);

	@Mapping(target = "usuario", ignore = true)
	@Mapping(target = "dataCriacao", ignore = true)
	@Mapping(target = "pontoVendaOrigem.usuario", ignore = true)
    @Mapping(target = "pontoVendaOrigem.dataCriacao", ignore = true)
    @Mapping(target = "pontoVendaOrigem.dataAlteracao", ignore = true)
	@Mapping(target = "pontoVendaDestino.usuario", ignore = true)
    @Mapping(target = "pontoVendaDestino.dataCriacao", ignore = true)
    @Mapping(target = "pontoVendaDestino.dataAlteracao", ignore = true)
    void updateEntityFromDto(StockTransferDto dto, @MappingTarget TransferenciaEstoque entity);

}