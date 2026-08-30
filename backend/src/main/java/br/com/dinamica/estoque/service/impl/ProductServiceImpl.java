package br.com.dinamica.estoque.service.impl;

import java.time.LocalDateTime;
import java.util.List;

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
import br.com.dinamica.estoque.mapper.ProductMapper;
import br.com.dinamica.estoque.repository.FornecedorRepository;
import br.com.dinamica.estoque.repository.ProdutoRepository;
import br.com.dinamica.estoque.repository.TipoProdutoRepository;
import br.com.dinamica.estoque.service.ProductService;
import br.com.dinamica.estoque.service.StockService;
import br.com.dinamica.estoque.util.DateUtil;

@Service
public class ProductServiceImpl implements ProductService {

	private ProdutoRepository repository;

	private TipoProdutoRepository tipoProdutoRepository;

	private FornecedorRepository fornecedorRepository;

	private StockService stockService;

	private ProductMapper modelMapper;

	public ProductServiceImpl(ProdutoRepository repository, TipoProdutoRepository tipoProdutoRepository, FornecedorRepository fornecedorRepository,
			StockService stockService, ProductMapper modelMapper) {
		this.repository = repository;
		this.tipoProdutoRepository = tipoProdutoRepository;
		this.fornecedorRepository = fornecedorRepository;
		this.stockService = stockService;
		this.modelMapper = modelMapper;
	}

	@Override
	public ProductDto get(Long id) {
		Produto entity = this.repository.findById(id).orElseThrow();

		return this.modelMapper.toDto(entity);
	}

	@Override
	public Page<ProductDto> list(ProductFilterDto filter, Pageable pageable) {
        Specification<Produto> specification = (_, _, _) -> null;

        if (filter.getNome() != null && !filter.getNome().isBlank()) {
        	specification = specification.and((root, _, cb) -> cb.like(cb.lower(root.get("nome")), "%" + filter.getNome().toLowerCase() + "%"));
        }

        if (filter.getReferencia() != null && !filter.getReferencia().isBlank()) {
            specification = specification.and((root, _, cb) -> cb.like(cb.lower(root.get("referencia")), "%" + filter.getReferencia().toLowerCase() + "%"));
        }

        if (filter.getIdTipoProduto() != null) {
            specification = specification.and((root, _, cb) -> cb.equal(root.get("tipoProduto").get("id"), filter.getIdTipoProduto()));
        }

        if (filter.getIdFornecedor() != null) {
            specification = specification.and((root, _, cb) -> cb.equal(root.get("fornecedor").get("id"), filter.getIdFornecedor()));
        }

        if (filter.getMinPeso() != null && filter.getMaxPeso() != null) {
            specification = specification.and((root, _, cb) -> cb.between(root.get("peso"), filter.getMinPeso(), filter.getMaxPeso()));
        } else if (filter.getMinPeso() != null) {
            specification = specification.and((root, _, cb) -> cb.greaterThanOrEqualTo(root.get("peso"), filter.getMinPeso()));
        } else if (filter.getMaxPeso() != null) {
            specification = specification.and((root, _, cb) -> cb.lessThanOrEqualTo(root.get("peso"), filter.getMaxPeso()));
        }

		return this.repository.findAll(specification, pageable).map(entity -> {
			ProductDto result = this.modelMapper.toDto(entity);

			result.setEstoque(this.stockService.getStock(entity.getId()));

			return result;
		});
	}

	@Override
	public ProductDto save(ProductDto dto, Usuario usuario) {
		Produto entity;
		LocalDateTime agora = DateUtil.now();

		if (dto.getId() != null) {
			entity = this.repository.findById(dto.getId()).orElseThrow();
		} else {
			entity = new Produto();

			entity.setDataCriacao(agora);
		}

		this.modelMapper.updateEntityFromDto(dto, entity);

		TipoProduto tipoProduto = this.tipoProdutoRepository.findById(dto.getTipoProduto().getId()).orElseThrow();
		Fornecedor fornecedor = this.fornecedorRepository.findById(dto.getFornecedor().getId()).orElseThrow();

		entity.setTipoProduto(tipoProduto);
		entity.setFornecedor(fornecedor);
		entity.setUsuario(usuario);
		entity.setDataAlteracao(agora);

		entity = this.repository.save(entity);

		Integer saldo = this.stockService.getStock(entity.getId());

		ProductDto result = this.modelMapper.toDto(entity);

		result.setEstoque(saldo != null ? saldo : 0);

		return result;
	}

	@Override
	public void delete(Long id) {
		this.repository.deleteById(id);
	}

	@Override
	public void save(List<ProductDto> dtos, Usuario usuario) {
		dtos.forEach(dto -> this.save(dto, usuario));
	}

}
