package br.com.dinamica.estoque.service.impl;

import java.util.Date;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import br.com.dinamica.estoque.dto.ProductTypeDto;
import br.com.dinamica.estoque.entity.TipoProduto;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.repository.ProdutoRepository;
import br.com.dinamica.estoque.repository.TipoProdutoRepository;
import br.com.dinamica.estoque.service.ProductTypeService;
import br.com.dinamica.estoque.util.DateUtil;

@Service
public class ProductTypeServiceImpl implements ProductTypeService {

	private TipoProdutoRepository repository;

	private ProdutoRepository produtoRepository;

	private ModelMapper modelMapper;

	public ProductTypeServiceImpl(TipoProdutoRepository repository, ProdutoRepository produtoRepository, ModelMapper modelMapper) {
		this.repository = repository;
		this.produtoRepository = produtoRepository;
		this.modelMapper = modelMapper;
	}

	@Override
	public ProductTypeDto get(Long id) {
		TipoProduto entity = this.repository.findById(id).orElseThrow();

		return this.modelMapper.map(entity, ProductTypeDto.class);
	}

	@Override
	public Page<ProductTypeDto> list(Pageable pageable) {
        Specification<TipoProduto> specification = (root, query, cb) -> null;

		return this.repository.findAll(specification, pageable).map(entity -> this.modelMapper.map(entity, ProductTypeDto.class));
	}

	@Override
	public ProductTypeDto save(ProductTypeDto dto, Usuario usuario) {
		TipoProduto entity;
        Date agora = DateUtil.now();

		if (dto.getId() != null) {
			entity = this.repository.findById(dto.getId()).orElseThrow();
		} else {
			entity = new TipoProduto();

			entity.setDataCriacao(agora);
		}

		this.modelMapper.map(dto, entity);

		entity.setUsuario(usuario);
		entity.setDataAlteracao(agora);

		entity = this.repository.save(entity);

		return this.modelMapper.map(entity, ProductTypeDto.class);
	}

	@Override
	public void delete(Long id) {
		this.produtoRepository.deleteByTipoProduto_Id(id);
		this.repository.deleteById(id);
	}

}
