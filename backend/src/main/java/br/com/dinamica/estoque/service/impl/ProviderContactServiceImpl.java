package br.com.dinamica.estoque.service.impl;

import java.util.Date;

import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import br.com.dinamica.estoque.dto.ProviderContactDto;
import br.com.dinamica.estoque.entity.ContatoFornecedor;
import br.com.dinamica.estoque.entity.Fornecedor;
import br.com.dinamica.estoque.repository.ContatoFornecedorRepository;
import br.com.dinamica.estoque.repository.FornecedorRepository;
import br.com.dinamica.estoque.service.ProviderContactService;
import br.com.dinamica.estoque.util.DateUtil;

@Service
public class ProviderContactServiceImpl implements ProviderContactService {

	private ContatoFornecedorRepository repository;

	private FornecedorRepository fornecedorRepository;

	private ModelMapper modelMapper;

	public ProviderContactServiceImpl(ContatoFornecedorRepository repository, FornecedorRepository fornecedorRepository, ModelMapper modelMapper) {
		this.repository = repository;
		this.fornecedorRepository = fornecedorRepository;
		this.modelMapper = modelMapper;

		this.modelMapper.addMappings(new PropertyMap<ProviderContactDto, ContatoFornecedor>() {
            @Override
            protected void configure() {
                skip(destination.getId());
                skip(destination.getFornecedor());
                skip(destination.getDataCriacao());
                skip(destination.getDataAlteracao());
            }
        });
	}

	@Override
	public ProviderContactDto get(Long id) {
		ContatoFornecedor entity = this.repository.findById(id).orElseThrow();

		return this.modelMapper.map(entity, ProviderContactDto.class);
	}

	@Override
	public Page<ProviderContactDto> list(Integer idFornecedor, Pageable pageable) {
        Specification<ContatoFornecedor> specification = (root, query, cb) -> null;

        if (idFornecedor != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("fornecedor").get("id"), idFornecedor));
        }

		return this.repository.findAll(specification, pageable).map(entity -> this.modelMapper.map(entity, ProviderContactDto.class));
	}

	@Override
	public ProviderContactDto save(ProviderContactDto dto) {
		ContatoFornecedor entity;
        Date agora = DateUtil.now();

		if (dto.getId() != null) {
			entity = this.repository.findById(dto.getId()).orElseThrow();
		} else {
			entity = new ContatoFornecedor();

			entity.setDataCriacao(agora);
		}

		this.modelMapper.map(dto, entity);

		Fornecedor fornecedor = this.fornecedorRepository.findById(dto.getFornecedor().getId()).orElseThrow();
		
		entity.setFornecedor(fornecedor);
		entity.setDataAlteracao(agora);

		entity = this.repository.save(entity);

		return this.modelMapper.map(entity, ProviderContactDto.class);
	}

	@Override
	public void delete(Long id) {
		this.repository.deleteById(id);
	}

}
