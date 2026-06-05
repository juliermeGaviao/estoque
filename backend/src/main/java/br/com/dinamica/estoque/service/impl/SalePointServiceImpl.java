package br.com.dinamica.estoque.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import br.com.dinamica.estoque.dto.SalePointDto;
import br.com.dinamica.estoque.entity.PontoVenda;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.mapper.SalePointMapper;
import br.com.dinamica.estoque.repository.PontoVendaRepository;
import br.com.dinamica.estoque.service.SalePointService;
import br.com.dinamica.estoque.util.DateUtil;

@Service
public class SalePointServiceImpl implements SalePointService {

	private PontoVendaRepository repository;

	private SalePointMapper modelMapper;

	public SalePointServiceImpl(PontoVendaRepository repository, SalePointMapper modelMapper) {
		this.repository = repository;
		this.modelMapper = modelMapper;
	}

	@Override
	public SalePointDto get(Long id) {
		PontoVenda entity = this.repository.findById(id).orElseThrow();

		return this.modelMapper.toDto(entity);
	}

	@Override
	public Page<SalePointDto> list(String nome, Pageable pageable) {
        Specification<PontoVenda> specification = (_, _, _) -> null;

        if (nome != null && !nome.isBlank()) {
        	specification = specification.and((root, _, cb) -> cb.like(cb.lower(root.get("nome")), "%" + nome.toLowerCase() + "%"));
        }

		return this.repository.findAll(specification, pageable).map(this.modelMapper::toDto);
	}

	@Override
	public SalePointDto save(SalePointDto dto, Usuario usuario) {
		PontoVenda entity;
		LocalDateTime agora = DateUtil.now();

		if (dto.getId() != null) {
			entity = this.repository.findById(dto.getId()).orElseThrow();
			entity.setDataAlteracao(agora);
		} else {
			entity = new PontoVenda();

			entity.setDataCriacao(agora);
		}

		this.modelMapper.updateEntityFromDto(dto, entity);

		entity.setUsuario(usuario);
		entity.setDataAlteracao(agora);

		entity = this.repository.save(entity);

		return this.modelMapper.toDto(entity);
	}

	@Override
	public void delete(Long id) {
		this.repository.deleteById(id);
	}

	@Override
	public void save(List<SalePointDto> dtos, Usuario usuario) {
		dtos.forEach(dto -> this.save(dto, usuario));
	}

}
