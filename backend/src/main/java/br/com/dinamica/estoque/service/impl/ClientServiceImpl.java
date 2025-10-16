package br.com.dinamica.estoque.service.impl;

import java.util.Date;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import br.com.dinamica.estoque.dto.ClientDto;
import br.com.dinamica.estoque.dto.CommonClientDto;
import br.com.dinamica.estoque.entity.Cliente;
import br.com.dinamica.estoque.entity.ClienteEmpresa;
import br.com.dinamica.estoque.entity.ClientePessoa;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.repository.ClienteRepository;
import br.com.dinamica.estoque.repository.ContatoClienteEmpresaRepository;
import br.com.dinamica.estoque.repository.ContatoClientePessoaRepository;
import br.com.dinamica.estoque.service.ClientService;
import br.com.dinamica.estoque.util.DateUtil;

@Service
public class ClientServiceImpl implements ClientService {

	private static final String DATA_ANIVERSARIO = "dataAniversario";

	private ClienteRepository repository;

	private ContatoClienteEmpresaRepository contatoClienteEmpresaRepository;

	private ContatoClientePessoaRepository contatoClientePessoaRepository;

	private ModelMapper modelMapper;

	public ClientServiceImpl(ClienteRepository repository, ContatoClienteEmpresaRepository contatoClienteEmpresaRepository, ContatoClientePessoaRepository contatoClientePessoaRepository, ModelMapper modelMapper) {
		this.repository = repository;
		this.contatoClienteEmpresaRepository = contatoClienteEmpresaRepository;
		this.contatoClientePessoaRepository = contatoClientePessoaRepository;
		this.modelMapper = modelMapper;
	}

	@Override
	public ClientDto get(Long id) {
		Cliente entity = this.repository.findById(id).orElseThrow();

		return this.modelMapper.map(entity, ClientDto.class);
	}

	@Override
	public List<CommonClientDto> findAll() {
		List<Cliente> clientes = this.repository.findAll();

		return clientes.stream().map(cliente -> new CommonClientDto(cliente.getId(), cliente.getNome())).toList();
	}

	@Override
	public Page<ClientDto> list(String razaoSocial, String nome, String cnpj, String fone, Pageable pageable) {
        Specification<Cliente> specification = (root, query, cb) -> cb.equal(root.type(), ClienteEmpresa.class);

        if (razaoSocial != null && !razaoSocial.isBlank()) {
        	specification = specification.and((root, query, cb) -> cb.like(cb.lower(root.get("razaoSocial")), "%" + razaoSocial.toLowerCase() + "%"));
        }

        if (nome != null && !nome.isBlank()) {
        	specification = specification.and((root, query, cb) -> cb.like(cb.lower(root.get("nome")), "%" + nome.toLowerCase() + "%"));
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
	public Page<ClientDto> list(String nome, String fone, Date minAniversario, Date maxAniversario, Pageable pageable) {
        Specification<Cliente> specification = (root, query, cb) -> cb.equal(root.type(), ClientePessoa.class);

        if (nome != null && !nome.isBlank()) {
        	specification = specification.and((root, query, cb) -> cb.like(cb.lower(root.get("nome")), "%" + nome.toLowerCase() + "%"));
        }

        if (fone != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("fone"), fone));
        }

        if (minAniversario != null && maxAniversario != null) {
            specification = specification.and((root, query, cb) -> cb.between(root.get(DATA_ANIVERSARIO), minAniversario, maxAniversario));
        } else if (minAniversario != null) {
            specification = specification.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get(DATA_ANIVERSARIO), minAniversario));
        } else if (maxAniversario != null) {
            specification = specification.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get(DATA_ANIVERSARIO), maxAniversario));
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
			entity = dto.getDataAniversario() != null ? new ClientePessoa() : new ClienteEmpresa();

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
		this.contatoClientePessoaRepository.deleteByCliente_Id(id);
		this.repository.deleteById(id);
	}

}
