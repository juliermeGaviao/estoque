package br.com.dinamica.estoque.service.impl;

import java.util.Date;

import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import br.com.dinamica.estoque.dto.ProductDto;
import br.com.dinamica.estoque.dto.ProductFilterDto;
import br.com.dinamica.estoque.entity.Fornecedor;
import br.com.dinamica.estoque.entity.Produto;
import br.com.dinamica.estoque.entity.TipoProduto;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.repository.FornecedorRepository;
import br.com.dinamica.estoque.repository.ProdutoRepository;
import br.com.dinamica.estoque.repository.TipoProdutoRepository;
import br.com.dinamica.estoque.service.ProductService;
import br.com.dinamica.estoque.util.DateUtil;

@Service
public class ProductServiceImpl implements ProductService {

	private ProdutoRepository repository;

	private TipoProdutoRepository tipoProdutoRepository;

	private FornecedorRepository fornecedorRepository;

	private ModelMapper modelMapper;

	public ProductServiceImpl(ProdutoRepository repository, TipoProdutoRepository tipoProdutoRepository, FornecedorRepository fornecedorRepository, ModelMapper modelMapper) {
		this.repository = repository;
		this.tipoProdutoRepository = tipoProdutoRepository;
		this.fornecedorRepository = fornecedorRepository;
		this.modelMapper = modelMapper;

		this.modelMapper.addMappings(new PropertyMap<ProductDto, Produto>() {
            @Override
            protected void configure() {
                skip(destination.getTipoProduto());
                skip(destination.getFornecedor());
            }
        });
	}

	@Override
	public ProductDto get(Long id) {
		Produto entity = this.repository.findById(id).orElseThrow();

		return this.modelMapper.map(entity, ProductDto.class);
	}

	@Override
	public Page<ProductDto> list(ProductFilterDto filter, Pageable pageable) {
        Specification<Produto> specification = (root, query, cb) -> null;

        if (filter.getNome() != null && !filter.getNome().isBlank()) {
        	specification = specification.and((root, query, cb) -> cb.like(cb.lower(root.get("nome")), "%" + filter.getNome().toLowerCase() + "%"));
        }

        if (filter.getReferencia() != null && !filter.getReferencia().isBlank()) {
            specification = specification.and((root, query, cb) -> cb.like(cb.lower(root.get("referencia")), "%" + filter.getReferencia().toLowerCase() + "%"));
        }

        if (filter.getIdTipoProduto() != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("tipoProduto").get("id"), filter.getIdTipoProduto()));
        }

        if (filter.getIdFornecedor() != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("fornecedor").get("id"), filter.getIdFornecedor()));
        }

        if (filter.getMinPeso() != null && filter.getMaxPeso() != null) {
            specification = specification.and((root, query, cb) -> cb.between(root.get("peso"), filter.getMinPeso(), filter.getMaxPeso()));
        } else if (filter.getMinPeso() != null) {
            specification = specification.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("peso"), filter.getMinPeso()));
        } else if (filter.getMaxPeso() != null) {
            specification = specification.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("peso"), filter.getMaxPeso()));
        }

        if (filter.getAtivo() != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("ativo"), filter.getAtivo()));
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
		Fornecedor fornecedor = this.fornecedorRepository.findById(dto.getFornecedor().getId()).orElseThrow();

		entity.setTipoProduto(tipoProduto);
		entity.setFornecedor(fornecedor);
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
