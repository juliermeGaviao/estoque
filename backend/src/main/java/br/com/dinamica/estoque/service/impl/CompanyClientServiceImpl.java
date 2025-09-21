package br.com.dinamica.estoque.service.impl;

import java.util.Date;

import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import br.com.dinamica.estoque.dto.CompanyClientDto;
import br.com.dinamica.estoque.entity.ClienteEmpresa;
import br.com.dinamica.estoque.repository.ContatoClienteEmpresaRepository;
import br.com.dinamica.estoque.repository.ClienteEmpresaRepository;
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

		this.modelMapper.addMappings(new PropertyMap<CompanyClientDto, ClienteEmpresa>() {
            @Override
            protected void configure() {
                skip(destination.getId());
                skip(destination.getDataCriacao());
                skip(destination.getDataAlteracao());
            }
        });
	}

	@Override
	public CompanyClientDto get(Long id) {
		ClienteEmpresa entity = this.repository.findById(id).orElseThrow();

		return this.modelMapper.map(entity, CompanyClientDto.class);
	}

	@Override
	public Page<CompanyClientDto> list(Pageable pageable) {
        Specification<ClienteEmpresa> specification = (root, query, cb) -> null;

		return this.repository.findAll(specification, pageable).map(entity -> this.modelMapper.map(entity, CompanyClientDto.class));
	}

	@Override
	public CompanyClientDto save(CompanyClientDto dto) {
		ClienteEmpresa entity;
        Date agora = DateUtil.now();

		if (dto.getId() != null) {
			entity = this.repository.findById(dto.getId()).orElseThrow();
		} else {
			entity = new ClienteEmpresa();

			entity.setDataCriacao(agora);
		}

		this.modelMapper.map(dto, entity);
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
