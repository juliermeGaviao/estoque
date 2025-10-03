package br.com.dinamica.estoque.service.impl;

import java.util.Date;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import br.com.dinamica.estoque.dto.CompanyClientDto;
import br.com.dinamica.estoque.entity.ClienteEmpresa;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.repository.ClienteEmpresaRepository;
import br.com.dinamica.estoque.repository.ContatoClienteEmpresaRepository;
import br.com.dinamica.estoque.service.CompanyClientService;
import br.com.dinamica.estoque.util.DateUtil;

@Service
public class CompanyClientServiceImpl implements CompanyClientService {

	private ClienteEmpresaRepository repository;

	private ContatoClienteEmpresaRepository contatoClienteEmpresaRepository;

	private ModelMapper modelMapper;

	public CompanyClientServiceImpl(ClienteEmpresaRepository repository, ContatoClienteEmpresaRepository contatoClienteEmpresaRepository, ModelMapper modelMapper) {
		this.repository = repository;
		this.contatoClienteEmpresaRepository = contatoClienteEmpresaRepository;
		this.modelMapper = modelMapper;
	}

	@Override
	public CompanyClientDto get(Long id) {
		ClienteEmpresa entity = this.repository.findById(id).orElseThrow();

		return this.modelMapper.map(entity, CompanyClientDto.class);
	}

	@Override
	public Page<CompanyClientDto> list(String razaoSocial, String fantasia, String cnpj, String fone, Pageable pageable) {
        Specification<ClienteEmpresa> specification = (root, query, cb) -> null;

        if (razaoSocial != null && !razaoSocial.isBlank()) {
        	specification = specification.and((root, query, cb) -> cb.like(cb.lower(root.get("razaoSocial")), "%" + razaoSocial.toLowerCase() + "%"));
        }

        if (fantasia != null && !fantasia.isBlank()) {
        	specification = specification.and((root, query, cb) -> cb.like(cb.lower(root.get("fantasia")), "%" + fantasia.toLowerCase() + "%"));
        }

        if (cnpj != null && !cnpj.isBlank()) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("cnpj"), cnpj));
        }

        if (fone != null && !fone.isBlank()) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("fone"), fone));
        }

		return this.repository.findAll(specification, pageable).map(entity -> this.modelMapper.map(entity, CompanyClientDto.class));
	}

	@Override
	public CompanyClientDto save(CompanyClientDto dto, Usuario usuario) {
		ClienteEmpresa entity;
        Date agora = DateUtil.now();

		if (dto.getId() != null) {
			entity = this.repository.findById(dto.getId()).orElseThrow();
		} else {
			entity = new ClienteEmpresa();

			entity.setDataCriacao(agora);
		}

		this.modelMapper.map(dto, entity);

		entity.setUsuario(usuario);
		entity.setDataAlteracao(agora);

		entity = this.repository.save(entity);

		return this.modelMapper.map(entity, CompanyClientDto.class);
	}

	@Override
	public void delete(Long id) {
		this.contatoClienteEmpresaRepository.deleteByClienteEmpresa_Id(id);
		this.repository.deleteById(id);
	}

}
