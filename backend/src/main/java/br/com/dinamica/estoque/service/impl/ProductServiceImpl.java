package br.com.dinamica.estoque.service.impl;

import java.util.Date;

import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import br.com.dinamica.estoque.dto.ProductDto;
import br.com.dinamica.estoque.entity.Produto;
import br.com.dinamica.estoque.entity.TipoProduto;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.repository.ProdutoRepository;
import br.com.dinamica.estoque.repository.TipoProdutoRepository;
import br.com.dinamica.estoque.service.ProductService;
import br.com.dinamica.estoque.util.DateUtil;

@Service
public class ProductServiceImpl implements ProductService {

	private ProdutoRepository repository;

	private TipoProdutoRepository tipoProdutoRepository;

	private ModelMapper modelMapper;

	public ProductServiceImpl(ProdutoRepository repository, TipoProdutoRepository tipoProdutoRepository, ModelMapper modelMapper) {
		this.repository = repository;
		this.tipoProdutoRepository = tipoProdutoRepository;
		this.modelMapper = modelMapper;

		this.modelMapper.addMappings(new PropertyMap<ProductDto, Produto>() {
            @Override
            protected void configure() {
                skip(destination.getTipoProduto());
            }
        });

		this.modelMapper.addMappings(new PropertyMap<Produto, ProductDto>() {
            @Override
            protected void configure() {
                skip(destination.getTipoProduto());
            }
        });
	}

	@Override
	public ProductDto get(Long id) {
		Produto entity = this.repository.findById(id).orElseThrow();

		return this.modelMapper.map(entity, ProductDto.class);
	}

	@Override
	public Page<ProductDto> list(Long idTipoProduto, Pageable pageable) {
        Specification<Produto> specification = (root, query, cb) -> null;

        if (idTipoProduto != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("tipoProduto").get("id"), idTipoProduto));
        }

		return this.repository.findAll(specification, pageable).map(entity -> this.modelMapper.map(entity, ProductDto.class));
	}

	@Override
	public ProductDto save(ProductDto dto, Usuario usuario) {
		Produto entity;
        Date agora = DateUtil.now();

		if (dto.getId() != null) {
			entity = this.repository.findById(dto.getId()).orElseThrow();
		} else {
			entity = new Produto();

			entity.setDataCriacao(agora);
		}

		this.modelMapper.map(dto, entity);

		TipoProduto tipoProduto = this.tipoProdutoRepository.findById(dto.getTipoProduto().getId()).orElseThrow();

		entity.setTipoProduto(tipoProduto);
		entity.setUsuario(usuario);
		entity.setDataAlteracao(agora);

		entity = this.repository.save(entity);

		return this.modelMapper.map(entity, ProductDto.class);
	}

	@Override
	public void delete(Long id) {
		this.repository.deleteById(id);
	}

}
