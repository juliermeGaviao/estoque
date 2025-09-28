package br.com.dinamica.estoque.service.impl;

import java.util.Date;

import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import br.com.dinamica.estoque.dto.PersonClientContactDto;
import br.com.dinamica.estoque.entity.ClientePessoa;
import br.com.dinamica.estoque.entity.ContatoClientePessoa;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.repository.ClientePessoaRepository;
import br.com.dinamica.estoque.repository.ContatoClientePessoaRepository;
import br.com.dinamica.estoque.service.PersonClientContactService;
import br.com.dinamica.estoque.util.DateUtil;

@Service
public class PersonClientContactServiceImpl implements PersonClientContactService {

	private ContatoClientePessoaRepository repository;

	private ClientePessoaRepository clientePessoaRepository;

	private ModelMapper modelMapper;

	public PersonClientContactServiceImpl(ContatoClientePessoaRepository repository, ClientePessoaRepository clientePessoaRepository, ModelMapper modelMapper) {
		this.repository = repository;
		this.clientePessoaRepository = clientePessoaRepository;
		this.modelMapper = modelMapper;

		this.modelMapper.addMappings(new PropertyMap<ContatoClientePessoa, PersonClientContactDto>() {
            @Override
            protected void configure() {
                skip(destination.getClientePessoa());
            }
        });

		this.modelMapper.addMappings(new PropertyMap<PersonClientContactDto, ContatoClientePessoa>() {
            @Override
            protected void configure() {
                skip(destination.getClientePessoa());
            }
        });
	}

	@Override
	public PersonClientContactDto get(Long id) {
		ContatoClientePessoa entity = this.repository.findById(id).orElseThrow();

		return this.modelMapper.map(entity, PersonClientContactDto.class);
	}

	@Override
	public Page<PersonClientContactDto> list(Long idPessoa, Pageable pageable) {
        Specification<ContatoClientePessoa> specification = (root, query, cb) -> null;

        if (idPessoa != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("clientePessoa").get("id"), idPessoa));
        }

		return this.repository.findAll(specification, pageable).map(entity -> this.modelMapper.map(entity, PersonClientContactDto.class));
	}

	@Override
	public PersonClientContactDto save(PersonClientContactDto dto, Usuario usuario) {
		ContatoClientePessoa entity;
        Date agora = DateUtil.now();

		if (dto.getId() != null) {
			entity = this.repository.findById(dto.getId()).orElseThrow();
		} else {
			entity = new ContatoClientePessoa();

			entity.setDataCriacao(agora);
		}

		this.modelMapper.map(dto, entity);

		ClientePessoa clientePessoa = this.clientePessoaRepository.findById(dto.getClientePessoa().getId()).orElseThrow();

		entity.setClientePessoa(clientePessoa);
		entity.setUsuario(usuario);
		entity.setDataAlteracao(agora);

		entity = this.repository.save(entity);

		return this.modelMapper.map(entity, PersonClientContactDto.class);
	}

	@Override
	public void delete(Long id) {
		this.repository.deleteById(id);
	}

}
