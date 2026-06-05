package br.com.dinamica.estoque.service.impl;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import br.com.dinamica.estoque.dto.PersonClientContactDto;
import br.com.dinamica.estoque.entity.Cliente;
import br.com.dinamica.estoque.entity.ContatoClientePessoa;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.mapper.PersonClientContactMapper;
import br.com.dinamica.estoque.repository.ClienteRepository;
import br.com.dinamica.estoque.repository.ContatoClientePessoaRepository;
import br.com.dinamica.estoque.service.PersonClientContactService;
import br.com.dinamica.estoque.util.DateUtil;

@Service
public class PersonClientContactServiceImpl implements PersonClientContactService {

	private ContatoClientePessoaRepository repository;

	private ClienteRepository clienteRepository;

	private PersonClientContactMapper modelMapper;

	public PersonClientContactServiceImpl(ContatoClientePessoaRepository repository, ClienteRepository clienteRepository, PersonClientContactMapper modelMapper) {
		this.repository = repository;
		this.clienteRepository = clienteRepository;
		this.modelMapper = modelMapper;
	}

	@Override
	public PersonClientContactDto get(Long id) {
		ContatoClientePessoa entity = this.repository.findById(id).orElseThrow();

		return this.modelMapper.toDto(entity);
	}

	@Override
	public Page<PersonClientContactDto> list(Long idPessoa, Pageable pageable) {
        Specification<ContatoClientePessoa> specification = (_, _, _) -> null;

        if (idPessoa != null) {
            specification = specification.and((root, _, cb) -> cb.equal(root.get("cliente").get("id"), idPessoa));
        }

		return this.repository.findAll(specification, pageable).map(this.modelMapper::toDto);
	}

	@Override
	public PersonClientContactDto save(PersonClientContactDto dto, Usuario usuario) {
		ContatoClientePessoa entity;
		LocalDateTime agora = DateUtil.now();

		if (dto.getId() != null) {
			entity = this.repository.findById(dto.getId()).orElseThrow();
		} else {
			entity = new ContatoClientePessoa();

			entity.setDataCriacao(agora);
		}

		this.modelMapper.updateEntityFromDto(dto, entity);

		Cliente cliente = this.clienteRepository.findById(dto.getCliente().getId()).orElseThrow();

		entity.setCliente(cliente);
		entity.setUsuario(usuario);
		entity.setDataAlteracao(agora);

		entity = this.repository.save(entity);

		return this.modelMapper.toDto(entity);
	}

	@Override
	public void delete(Long id) {
		this.repository.deleteById(id);
	}

}
