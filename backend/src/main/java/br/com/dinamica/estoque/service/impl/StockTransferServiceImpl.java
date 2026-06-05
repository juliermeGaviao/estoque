package br.com.dinamica.estoque.service.impl;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import br.com.dinamica.estoque.dto.StockTransferDto;
import br.com.dinamica.estoque.entity.TransferenciaEstoque;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.mapper.StockTransferMapper;
import br.com.dinamica.estoque.repository.PontoVendaRepository;
import br.com.dinamica.estoque.repository.TransferenciaEstoqueRepository;
import br.com.dinamica.estoque.service.StockTransferService;
import br.com.dinamica.estoque.util.DateUtil;

@Service
public class StockTransferServiceImpl implements StockTransferService {

	private TransferenciaEstoqueRepository repository;

	private PontoVendaRepository pontoVendaRepository;

	private StockTransferMapper modelMapper;

	public StockTransferServiceImpl(TransferenciaEstoqueRepository repository, PontoVendaRepository pontoVendaRepository, StockTransferMapper modelMapper) {
		this.repository = repository;
		this.pontoVendaRepository = pontoVendaRepository;
		this.modelMapper = modelMapper;
	}

	@Override
	public StockTransferDto get(Long id) {
		TransferenciaEstoque entity = this.repository.findById(id).orElseThrow();

		return this.modelMapper.toDto(entity);
	}

	@Override
	public Page<StockTransferDto> list(Long idPontoVendaOrigem, Long idPontoVendaDestino, Pageable pageable) {
        Specification<TransferenciaEstoque> specification = (_, _, _) -> null;

        if (idPontoVendaOrigem != null) {
            specification = specification.and((root, _, cb) -> cb.equal(root.get("pontoVendaOrigem").get("id"), idPontoVendaOrigem));
        }

        if (idPontoVendaDestino != null) {
            specification = specification.and((root, _, cb) -> cb.equal(root.get("pontoVendaDestino").get("id"), idPontoVendaDestino));
        }

		return this.repository.findAll(specification, pageable).map(this.modelMapper::toDto);
	}

	@Override
	public StockTransferDto save(StockTransferDto dto, Usuario usuario) {
		TransferenciaEstoque entity;
		LocalDateTime agora = DateUtil.now();

		if (dto.getId() != null) {
			entity = this.repository.findById(dto.getId()).orElseThrow();
		} else {
			entity = new TransferenciaEstoque();

			entity.setDataCriacao(agora);
		}

		this.modelMapper.updateEntityFromDto(dto, entity);

		entity.setPontoVendaOrigem(this.pontoVendaRepository.findById(dto.getPontoVendaOrigem().getId()).orElseThrow());
		entity.setPontoVendaDestino(this.pontoVendaRepository.findById(dto.getPontoVendaDestino().getId()).orElseThrow());
		entity.setUsuario(usuario);

		entity = this.repository.save(entity);

		return this.modelMapper.toDto(entity);
	}

	@Override
	public void delete(Long id) {
		this.repository.deleteById(id);
	}

}
