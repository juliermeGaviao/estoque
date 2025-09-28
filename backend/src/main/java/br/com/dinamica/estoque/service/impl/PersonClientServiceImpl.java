package br.com.dinamica.estoque.service.impl;

import java.util.Date;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import br.com.dinamica.estoque.dto.PersonClientDto;
import br.com.dinamica.estoque.entity.ClientePessoa;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.repository.ClientePessoaRepository;
import br.com.dinamica.estoque.repository.ContatoClientePessoaRepository;
import br.com.dinamica.estoque.service.PersonClientService;
import br.com.dinamica.estoque.util.DateUtil;

@Service
public class PersonClientServiceImpl implements PersonClientService {

	private ClientePessoaRepository repository;

	private ContatoClientePessoaRepository contatoClientePessoaRepository;

	private ModelMapper modelMapper;

	public PersonClientServiceImpl(ClientePessoaRepository repository, ContatoClientePessoaRepository contatoClientePessoaRepository, ModelMapper modelMapper) {
		this.repository = repository;
		this.contatoClientePessoaRepository = contatoClientePessoaRepository;
		this.modelMapper = modelMapper;
	}

	@Override
	public PersonClientDto get(Long id) {
		ClientePessoa entity = this.repository.findById(id).orElseThrow();

		return this.modelMapper.map(entity, PersonClientDto.class);
	}

	@Override
	public Page<PersonClientDto> list(Pageable pageable) {
        Specification<ClientePessoa> specification = (root, query, cb) -> null;

		return this.repository.findAll(specification, pageable).map(entity -> this.modelMapper.map(entity, PersonClientDto.class));
	}

	@Override
	public PersonClientDto save(PersonClientDto dto, Usuario usuario) {
		ClientePessoa entity;
        Date agora = DateUtil.now();

		if (dto.getId() != null) {
			entity = this.repository.findById(dto.getId()).orElseThrow();
		} else {
			entity = new ClientePessoa();

			entity.setDataCriacao(agora);
		}

		this.modelMapper.map(dto, entity);

		entity.setUsuario(usuario);
		entity.setDataAlteracao(agora);

		entity = this.repository.save(entity);

		return this.modelMapper.map(entity, PersonClientDto.class);
	}

	@Override
	public void delete(Long id) {
		this.contatoClientePessoaRepository.deleteByClientePessoa_Id(id);
		this.repository.deleteById(id);
	}

}
