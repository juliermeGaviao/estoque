package br.com.dinamica.estoque.service.impl;

import java.util.Date;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import br.com.dinamica.estoque.dto.ClientDto;
import br.com.dinamica.estoque.entity.Cliente;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.repository.ClienteRepository;
import br.com.dinamica.estoque.repository.ContatoClienteEmpresaRepository;
import br.com.dinamica.estoque.service.ClientService;
import br.com.dinamica.estoque.util.DateUtil;

@Service
public class ClientServiceImpl implements ClientService {

	private ClienteRepository repository;

	private ContatoClienteEmpresaRepository contatoClienteEmpresaRepository;

	private ModelMapper modelMapper;

	public ClientServiceImpl(ClienteRepository repository, ContatoClienteEmpresaRepository contatoClienteEmpresaRepository, ModelMapper modelMapper) {
		this.repository = repository;
		this.contatoClienteEmpresaRepository = contatoClienteEmpresaRepository;
		this.modelMapper = modelMapper;
	}

	@Override
	public ClientDto get(Long id) {
		Cliente entity = this.repository.findById(id).orElseThrow();

		return this.modelMapper.map(entity, ClientDto.class);
	}

	@Override
	public Page<ClientDto> list(String razaoSocial, String fantasia, String cnpj, String fone, Pageable pageable) {
        Specification<Cliente> specification = (root, query, cb) -> null;

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

		return this.repository.findAll(specification, pageable).map(entity -> this.modelMapper.map(entity, ClientDto.class));
	}

	@Override
	public ClientDto save(ClientDto dto, Usuario usuario) {
		Cliente entity;
        Date agora = DateUtil.now();

		if (dto.getId() != null) {
			entity = this.repository.findById(dto.getId()).orElseThrow();
		} else {
			entity = new Cliente();

			entity.setDataCriacao(agora);
		}

		this.modelMapper.map(dto, entity);

		entity.setUsuario(usuario);
		entity.setDataAlteracao(agora);

		entity = this.repository.save(entity);

		return this.modelMapper.map(entity, ClientDto.class);
	}

	@Override
	public void delete(Long id) {
		this.contatoClienteEmpresaRepository.deleteByCliente_Id(id);
		this.repository.deleteById(id);
	}

}
