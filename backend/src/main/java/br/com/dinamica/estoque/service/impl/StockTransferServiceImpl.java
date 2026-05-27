package br.com.dinamica.estoque.service.impl;

import java.util.Date;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import br.com.dinamica.estoque.dto.StockTransferDto;
import br.com.dinamica.estoque.entity.TransferenciaEstoque;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.repository.PontoVendaRepository;
import br.com.dinamica.estoque.repository.TransferenciaEstoqueRepository;
import br.com.dinamica.estoque.service.StockTransferService;
import br.com.dinamica.estoque.util.DateUtil;

@Service
public class StockTransferServiceImpl implements StockTransferService {

	private TransferenciaEstoqueRepository repository;

	private PontoVendaRepository pontoVendaRepository;

	private ModelMapper modelMapper;

	public StockTransferServiceImpl(TransferenciaEstoqueRepository repository, PontoVendaRepository pontoVendaRepository, ModelMapper modelMapper) {
		this.repository = repository;
		this.pontoVendaRepository = pontoVendaRepository;
		this.modelMapper = modelMapper;
	}

	@Override
	public StockTransferDto get(Long id) {
		TransferenciaEstoque entity = this.repository.findById(id).orElseThrow();

		return this.modelMapper.map(entity, StockTransferDto.class);
	}

	@Override
	public Page<StockTransferDto> list(Long idPontoVendaOrigem, Long idPontoVendaDestino, Pageable pageable) {
        Specification<TransferenciaEstoque> specification = (root, query, cb) -> null;

        if (idPontoVendaOrigem != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("pontoVendaOrigem").get("id"), idPontoVendaOrigem));
        }

        if (idPontoVendaDestino != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("pontoVendaDestino").get("id"), idPontoVendaDestino));
        }

		return this.repository.findAll(specification, pageable).map(entity -> this.modelMapper.map(entity, StockTransferDto.class));
	}

	@Override
	public StockTransferDto save(StockTransferDto dto, Usuario usuario) {
		TransferenciaEstoque entity;
        Date agora = DateUtil.now();

		if (dto.getId() != null) {
			entity = this.repository.findById(dto.getId()).orElseThrow();
		} else {
			entity = new TransferenciaEstoque();

			entity.setDataCriacao(agora);
		}

		this.modelMapper.map(dto, entity);

		entity.setPontoVendaOrigem(this.pontoVendaRepository.findById(dto.getPontoVendaOrigem().getId()).orElseThrow());
		entity.setPontoVendaDestino(this.pontoVendaRepository.findById(dto.getPontoVendaDestino().getId()).orElseThrow());
		entity.setUsuario(usuario);

		entity = this.repository.save(entity);

		return this.modelMapper.map(entity, StockTransferDto.class);
	}

	@Override
	public void delete(Long id) {
		this.repository.deleteById(id);
	}

}
