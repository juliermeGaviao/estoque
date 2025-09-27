package br.com.dinamica.estoque.service.impl;

import java.util.Date;

import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import br.com.dinamica.estoque.dto.CompanyClientContactDto;
import br.com.dinamica.estoque.entity.ClienteEmpresa;
import br.com.dinamica.estoque.entity.ContatoClienteEmpresa;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.repository.ClienteEmpresaRepository;
import br.com.dinamica.estoque.repository.ContatoClienteEmpresaRepository;
import br.com.dinamica.estoque.service.CompanyClientContactService;
import br.com.dinamica.estoque.util.DateUtil;

@Service
public class CompanyClientContactServiceImpl implements CompanyClientContactService {

	private ContatoClienteEmpresaRepository repository;

	private ClienteEmpresaRepository clienteEmpresaRepository;

	private ModelMapper modelMapper;

	public CompanyClientContactServiceImpl(ContatoClienteEmpresaRepository repository, ClienteEmpresaRepository clienteEmpresaRepository, ModelMapper modelMapper) {
		this.repository = repository;
		this.clienteEmpresaRepository = clienteEmpresaRepository;
		this.modelMapper = modelMapper;

		this.modelMapper.addMappings(new PropertyMap<CompanyClientContactDto, ContatoClienteEmpresa>() {
            @Override
            protected void configure() {
                skip(destination.getClienteEmpresa());
            }
        });
	}

	@Override
	public CompanyClientContactDto get(Long id) {
		ContatoClienteEmpresa entity = this.repository.findById(id).orElseThrow();

		return this.modelMapper.map(entity, CompanyClientContactDto.class);
	}

	@Override
	public Page<CompanyClientContactDto> list(Pageable pageable) {
        Specification<ContatoClienteEmpresa> specification = (root, query, cb) -> null;

		return this.repository.findAll(specification, pageable).map(entity -> this.modelMapper.map(entity, CompanyClientContactDto.class));
	}

	@Override
	public CompanyClientContactDto save(CompanyClientContactDto dto, Usuario usuario) {
		ContatoClienteEmpresa entity;
        Date agora = DateUtil.now();

		if (dto.getId() != null) {
			entity = this.repository.findById(dto.getId()).orElseThrow();
		} else {
			entity = new ContatoClienteEmpresa();

			entity.setDataCriacao(agora);
		}

		this.modelMapper.map(dto, entity);

		ClienteEmpresa clienteEmpresa = this.clienteEmpresaRepository.findById(dto.getClienteEmpresa().getId()).orElseThrow();

		entity.setClienteEmpresa(clienteEmpresa);
		entity.setUsuario(usuario);
		entity.setDataAlteracao(agora);

		entity = this.repository.save(entity);

		return this.modelMapper.map(entity, CompanyClientContactDto.class);
	}

	@Override
	public void delete(Long id) {
		this.repository.deleteById(id);
	}

}
