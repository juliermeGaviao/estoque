package br.com.dinamica.estoque.service.impl;

import java.util.Date;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import br.com.dinamica.estoque.dto.PurchaseOrderDto;
import br.com.dinamica.estoque.dto.PurchaseOrderFilterDto;
import br.com.dinamica.estoque.entity.PedidoCompra;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.repository.PedidoCompraRepository;
import br.com.dinamica.estoque.service.PurchaseOrderService;
import br.com.dinamica.estoque.util.DateUtil;

@Service
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

	private static final String DATA_PEDIDO = "dataPedido";

	private PedidoCompraRepository repository;

	private ModelMapper modelMapper;

	public PurchaseOrderServiceImpl(PedidoCompraRepository repository, ModelMapper modelMapper) {
		this.repository = repository;
		this.modelMapper = modelMapper;
	}

	@Override
	public PurchaseOrderDto get(Long id) {
		PedidoCompra entity = this.repository.findById(id).orElseThrow();

		return this.modelMapper.map(entity, PurchaseOrderDto.class);
	}

	@Override
	public Page<PurchaseOrderDto> list(PurchaseOrderFilterDto filter, Pageable pageable) {
        Specification<PedidoCompra> specification = (root, query, cb) -> null;

        if (filter.getNumeroPedido() != null && !filter.getNumeroPedido().isBlank()) {
        	specification = specification.and((root, query, cb) -> cb.like(cb.lower(root.get("numeroPedido")), "%" + filter.getNumeroPedido().toLowerCase() + "%"));
        }

        if (filter.getIdFornecedor() != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("fornecedor").get("id"), filter.getIdFornecedor()));
        }

        if (filter.getMinDataPedido() != null && filter.getMaxDataPedido() != null) {
            specification = specification.and((root, query, cb) -> cb.between(root.get(DATA_PEDIDO), filter.getMinDataPedido(), filter.getMaxDataPedido()));
        } else if (filter.getMinDataPedido() != null) {
            specification = specification.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get(DATA_PEDIDO), filter.getMinDataPedido()));
        } else if (filter.getMaxDataPedido() != null) {
            specification = specification.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get(DATA_PEDIDO), filter.getMaxDataPedido()));
        }

		return this.repository.findAll(specification, pageable).map(entity -> this.modelMapper.map(entity, PurchaseOrderDto.class));
	}

	@Override
	public PurchaseOrderDto save(PurchaseOrderDto dto, Usuario usuario) {
		PedidoCompra entity;
        Date agora = DateUtil.now();

		if (dto.getId() != null) {
			entity = this.repository.findById(dto.getId()).orElseThrow();
		} else {
			entity = new PedidoCompra();

			entity.setDataCriacao(agora);
		}

		this.modelMapper.map(dto, entity);

		entity.setUsuario(usuario);

		entity = this.repository.save(entity);

		return this.modelMapper.map(entity, PurchaseOrderDto.class);
	}

	@Override
	public void delete(Long id) {
		this.repository.deleteById(id);
	}

}
