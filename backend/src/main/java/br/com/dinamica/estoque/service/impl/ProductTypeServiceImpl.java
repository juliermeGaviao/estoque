package br.com.dinamica.estoque.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import br.com.dinamica.estoque.dto.ProductTypeDto;
import br.com.dinamica.estoque.entity.TipoProduto;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.mapper.ProductTypeMapper;
import br.com.dinamica.estoque.repository.ProdutoRepository;
import br.com.dinamica.estoque.repository.TipoProdutoRepository;
import br.com.dinamica.estoque.service.ProductTypeService;
import br.com.dinamica.estoque.util.DateUtil;

@Service
public class ProductTypeServiceImpl implements ProductTypeService {

	private TipoProdutoRepository repository;

	private ProdutoRepository produtoRepository;

	private ProductTypeMapper modelMapper;

	public ProductTypeServiceImpl(TipoProdutoRepository repository, ProdutoRepository produtoRepository, ProductTypeMapper modelMapper) {
		this.repository = repository;
		this.produtoRepository = produtoRepository;
		this.modelMapper = modelMapper;
	}

	@Override
	public ProductTypeDto get(Long id) {
		TipoProduto entity = this.repository.findById(id).orElseThrow();

		return this.modelMapper.toDto(entity);
	}

	@Override
	public Page<ProductTypeDto> list(String nome, Pageable pageable) {
        Specification<TipoProduto> specification = (_, _, _) -> null;

        if (nome != null && !nome.isBlank()) {
        	specification = specification.and((root, _, cb) -> cb.like(cb.lower(root.get("nome")), "%" + nome.toLowerCase() + "%"));
        }

		return this.repository.findAll(specification, pageable).map(entity -> this.modelMapper.toDto(entity));
	}

	@Override
	public ProductTypeDto save(ProductTypeDto dto, Usuario usuario) {
		TipoProduto entity;
		LocalDateTime agora = DateUtil.now();

		if (dto.getId() != null) {
			entity = this.repository.findById(dto.getId()).orElseThrow();
		} else {
			entity = new TipoProduto();

			entity.setDataCriacao(agora);
		}

		this.modelMapper.updateEntityFromDto(dto, entity);

		entity.setUsuario(usuario);
		entity.setDataAlteracao(agora);

		entity = this.repository.save(entity);

		return this.modelMapper.toDto(entity);
	}

	@Override
	public void delete(Long id) {
		this.produtoRepository.deleteByTipoProduto_Id(id);
		this.repository.deleteById(id);
	}

	@Override
	public void save(List<ProductTypeDto> dtos, Usuario usuario) {
		dtos.forEach(dto -> this.save(dto, usuario));
	}

}
