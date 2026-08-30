package br.com.dinamica.estoque.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import br.com.dinamica.estoque.dto.PurchaseOrderDto;
import br.com.dinamica.estoque.dto.PurchaseOrderFilterDto;
import br.com.dinamica.estoque.entity.Usuario;

public interface PurchaseOrderService {

	PurchaseOrderDto get(Long id);

	Page<PurchaseOrderDto> list(PurchaseOrderFilterDto filter, Pageable pageable);

	PurchaseOrderDto save(PurchaseOrderDto dto, Usuario usuario);

	void delete(Long id);

	List<PurchaseOrderDto> findByOrderNumber(String numeroPedido);

}
