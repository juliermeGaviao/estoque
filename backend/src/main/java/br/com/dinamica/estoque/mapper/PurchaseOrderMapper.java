package br.com.dinamica.estoque.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import br.com.dinamica.estoque.dto.PurchaseOrderDto;
import br.com.dinamica.estoque.entity.PedidoCompra;

@Mapper(componentModel = "spring") 
public interface PurchaseOrderMapper {

	@Mapping(target = "estoque", ignore = true)
    PurchaseOrderDto toDto(PedidoCompra entity);

	@Mapping(target = "usuario", ignore = true)
	@Mapping(target = "dataCriacao", ignore = true)
    @Mapping(target = "fornecedor.usuario", ignore = true)
    @Mapping(target = "fornecedor.dataCriacao", ignore = true)
    @Mapping(target = "fornecedor.dataAlteracao", ignore = true)
    void updateEntityFromDto(PurchaseOrderDto dto, @MappingTarget PedidoCompra entity);

}