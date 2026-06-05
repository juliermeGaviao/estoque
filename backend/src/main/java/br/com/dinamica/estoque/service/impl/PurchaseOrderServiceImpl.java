package br.com.dinamica.estoque.service.impl;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import br.com.dinamica.estoque.dto.PurchaseOrderDto;
import br.com.dinamica.estoque.dto.PurchaseOrderFilterDto;
import br.com.dinamica.estoque.entity.PedidoCompra;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.mapper.PurchaseOrderMapper;
import br.com.dinamica.estoque.repository.FornecedorRepository;
import br.com.dinamica.estoque.repository.PedidoCompraRepository;
import br.com.dinamica.estoque.service.PurchaseOrderService;
import br.com.dinamica.estoque.util.DateUtil;

@Service
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

	private static final String DATA_PEDIDO = "dataPedido";

	private PedidoCompraRepository repository;

	private FornecedorRepository providerRepository;

	private PurchaseOrderMapper modelMapper;

	public PurchaseOrderServiceImpl(PedidoCompraRepository repository, FornecedorRepository providerRepository, PurchaseOrderMapper modelMapper) {
		this.repository = repository;
		this.providerRepository = providerRepository;
		this.modelMapper = modelMapper;
	}

	@Override
	public PurchaseOrderDto get(Long id) {
		PedidoCompra entity = this.repository.findById(id).orElseThrow();

		return this.modelMapper.toDto(entity);
	}

	@Override
	public Page<PurchaseOrderDto> list(PurchaseOrderFilterDto filter, Pageable pageable) {
        Specification<PedidoCompra> specification = (_, _, _) -> null;

        if (filter.getNumeroPedido() != null && !filter.getNumeroPedido().isBlank()) {
        	specification = specification.and((root, _, cb) -> cb.like(cb.lower(root.get("numeroPedido")), "%" + filter.getNumeroPedido().toLowerCase() + "%"));
        }

        if (filter.getIdFornecedor() != null) {
            specification = specification.and((root, _, cb) -> cb.equal(root.get("fornecedor").get("id"), filter.getIdFornecedor()));
        }

        if (filter.getMinDataPedido() != null && filter.getMaxDataPedido() != null) {
            specification = specification.and((root, _, cb) -> cb.between(root.get(DATA_PEDIDO), filter.getMinDataPedido(), filter.getMaxDataPedido()));
        } else if (filter.getMinDataPedido() != null) {
            specification = specification.and((root, _, cb) -> cb.greaterThanOrEqualTo(root.get(DATA_PEDIDO), filter.getMinDataPedido()));
        } else if (filter.getMaxDataPedido() != null) {
            specification = specification.and((root, _, cb) -> cb.lessThanOrEqualTo(root.get(DATA_PEDIDO), filter.getMaxDataPedido()));
        }

		return this.repository.findAll(specification, pageable).map(this.modelMapper::toDto);
	}

	@Override
	public PurchaseOrderDto save(PurchaseOrderDto dto, Usuario usuario) {
		PedidoCompra entity;
		LocalDateTime agora = DateUtil.now();

		if (dto.getId() != null) {
			entity = this.repository.findById(dto.getId()).orElseThrow();
		} else {
			entity = new PedidoCompra();

			entity.setDataCriacao(agora);
		}

		this.modelMapper.updateEntityFromDto(dto, entity);

		entity.setFornecedor(this.providerRepository.findById(dto.getFornecedor().getId()).orElseThrow());
		entity.setDataCriacao(agora);
		entity.setUsuario(usuario);

		entity = this.repository.save(entity);

		return this.modelMapper.toDto(entity);
	}

	@Override
	public void delete(Long id) {
		this.repository.deleteById(id);
	}

}
