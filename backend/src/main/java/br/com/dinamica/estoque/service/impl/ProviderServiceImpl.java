package br.com.dinamica.estoque.service.impl;

import java.util.Date;

import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import br.com.dinamica.estoque.dto.ProviderDto;
import br.com.dinamica.estoque.entity.Fornecedor;
import br.com.dinamica.estoque.repository.ContatoFornecedorRepository;
import br.com.dinamica.estoque.repository.FornecedorRepository;
import br.com.dinamica.estoque.service.ProviderService;
import br.com.dinamica.estoque.util.DateUtil;

@Service
public class ProviderServiceImpl implements ProviderService {

	private FornecedorRepository repository;

	private ContatoFornecedorRepository contatoFornecedorRepository;

	private ModelMapper modelMapper;

	public ProviderServiceImpl(FornecedorRepository repository, ContatoFornecedorRepository contatoFornecedorRepository, ModelMapper modelMapper) {
		this.repository = repository;
		this.contatoFornecedorRepository = contatoFornecedorRepository;
		this.modelMapper = modelMapper;

		this.modelMapper.addMappings(new PropertyMap<ProviderDto, Fornecedor>() {
            @Override
            protected void configure() {
                skip(destination.getId());
                skip(destination.getDataCriacao());
                skip(destination.getDataAlteracao());
            }
        });
	}

	@Override
	public ProviderDto get(Long id) {
		Fornecedor entity = this.repository.findById(id).orElseThrow();

		return this.modelMapper.map(entity, ProviderDto.class);
	}

	@Override
	public Page<ProviderDto> list(Pageable pageable) {
        Specification<Fornecedor> specification = (root, query, cb) -> null;

		return this.repository.findAll(specification, pageable).map(entity -> this.modelMapper.map(entity, ProviderDto.class));
	}

	@Override
	public ProviderDto save(ProviderDto dto) {
		Fornecedor entity;
        Date agora = DateUtil.now();

		if (dto.getId() != null) {
			entity = this.repository.findById(dto.getId()).orElseThrow();
		} else {
			entity = new Fornecedor();

			entity.setDataCriacao(agora);
		}

		this.modelMapper.map(dto, entity);
		entity.setDataAlteracao(agora);

		entity = this.repository.save(entity);

		return this.modelMapper.map(entity, ProviderDto.class);
	}

	@Override
	public void delete(Long id) {
		this.contatoFornecedorRepository.deleteByFornecedor_Id(id);
		this.repository.deleteById(id);
	}

}
