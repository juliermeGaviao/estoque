package br.com.dinamica.estoque.service.impl;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import br.com.dinamica.estoque.dto.PriceTableProductDto;
import br.com.dinamica.estoque.dto.PriceTableProductFilterDto;
import br.com.dinamica.estoque.dto.ProductDto;
import br.com.dinamica.estoque.dto.ProductTypeDto;
import br.com.dinamica.estoque.dto.ProviderDto;
import br.com.dinamica.estoque.entity.Produto;
import br.com.dinamica.estoque.entity.TabelaPreco;
import br.com.dinamica.estoque.entity.TabelaPrecoProduto;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.repository.ProdutoRepository;
import br.com.dinamica.estoque.repository.TabelaPrecoProdutoRepository;
import br.com.dinamica.estoque.repository.TabelaPrecoRepository;
import br.com.dinamica.estoque.service.PriceTableProductService;
import br.com.dinamica.estoque.util.DateUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
@Service
public class PriceTableProductServiceImpl implements PriceTableProductService {

	private TabelaPrecoProdutoRepository repository;

	private TabelaPrecoRepository tabelaPrecoRepository;

	private ProdutoRepository produtoRepository;

	private EntityManager entityManager;

	private ModelMapper modelMapper;

	public PriceTableProductServiceImpl(TabelaPrecoProdutoRepository repository, TabelaPrecoRepository tabelaPrecoRepository, ProdutoRepository produtoRepository,
			EntityManager entityManager, ModelMapper modelMapper) {
		this.repository = repository;
		this.tabelaPrecoRepository = tabelaPrecoRepository;
		this.produtoRepository = produtoRepository;
		this.entityManager = entityManager;
		this.modelMapper = modelMapper;

		this.modelMapper.addMappings(new PropertyMap<PriceTableProductDto, TabelaPrecoProduto>() {
            @Override
            protected void configure() {
                skip(destination.getTabela());
                skip(destination.getProduto());
            }
        });
	}

	@Override
	public PriceTableProductDto get(Long id) {
		TabelaPrecoProduto entity = this.repository.findById(id).orElseThrow();

		return this.modelMapper.map(entity, PriceTableProductDto.class);
	}

	@Override
	public Page<PriceTableProductDto> list(Long idTabelaPreco, Long idProduto, Pageable pageable) {
        Specification<TabelaPrecoProduto> specification = (root, query, cb) -> null;

        if (idTabelaPreco != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("tabela").get("id"), idTabelaPreco));
        }

        if (idProduto != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("produto").get("id"), idProduto));
        }

		return this.repository.findAll(specification, pageable).map(entity -> this.modelMapper.map(entity, PriceTableProductDto.class));
	}

	@Override
	public PriceTableProductDto save(PriceTableProductDto dto, Usuario usuario) {
		TabelaPrecoProduto entity;
        Date agora = DateUtil.now();

		if (dto.getId() != null) {
			entity = this.repository.findById(dto.getId()).orElseThrow();
		} else {
			entity = new TabelaPrecoProduto();

			entity.setDataCriacao(agora);
		}

		this.modelMapper.map(dto, entity);

		TabelaPreco tabela = this.tabelaPrecoRepository.findById(dto.getTabela().getId()).orElseThrow();
		Produto produto = this.produtoRepository.findById(dto.getProduto().getId()).orElseThrow();

		entity.setTabela(tabela);
		entity.setProduto(produto);
		entity.setUsuario(usuario);
		entity.setDataAlteracao(agora);

		entity = this.repository.save(entity);

		return this.modelMapper.map(entity, PriceTableProductDto.class);
	}

	@Override
	public void delete(Long id) {
		this.repository.deleteById(id);
	}

	private static final Map<String, String> SORT_MAPPING = Map.of(
	    "id", "p.id",
	    "produto.id", "p.id",
	    "produto.nome", "p.nome",
	    "produto.referencia", "p.referencia",
	    "produto.tipoProduto.nome", "tp.nome",
	    "produto.fornecedor.fantasia", "f.fantasia",
	    "produto.peso", "p.peso",
	    "preco", "tpp.preco"
	);

	private String getQuery(PriceTableProductFilterDto filter) {
		String result = """
				SELECT p.id, tp.nome, f.fantasia, p.referencia, p.nome, p.peso, tpp.id, tpp.preco
				FROM produto p
				JOIN tipo_produto tp ON tp.id = p.id_tipo_produto
				JOIN fornecedor f ON f.id = p.id_fornecedor
				LEFT JOIN tabela_preco_produto tpp ON p.id = tpp.id_produto AND tpp.id_tabela_preco = :idTabelaPreco
				WHERE p.ativo = 1
				""";

		if (filter.getNome() != null) {
			result += " AND lower(p.nome) like :nome";
		}

		if (filter.getReferencia() != null) {
			result += " AND lower(p.referencia) like :referencia";
		}

		if (filter.getIdTipoProduto() != null) {
			result += " AND tp.id = :idTipoProduto";
		}

		if (filter.getIdFornecedor() != null) {
			result += " AND f.id = :idFornecedor";
		}

		if (filter.getMinPeso() != null && filter.getMaxPeso() != null) {
			result += " AND p.peso between :minPeso and :maxPeso";
		} else if (filter.getMinPeso() != null) {
			result += " AND p.peso >= :minPeso";
		} else if (filter.getMaxPeso() != null) {
			result += " AND p.peso <= :maxPeso";
		}

		if (filter.getMinPreco() != null && filter.getMaxPreco() != null) {
			result += " AND tpp.preco between :minPreco and :maxPreco";
		} else if (filter.getMinPreco() != null) {
			result += " AND tpp.preco >= :minPreco";
		} else if (filter.getMaxPreco() != null) {
			result += " AND tpp.preco <= :maxPreco";
		}

		return result;
	}

	private void fillParameters(PriceTableProductFilterDto filter, Query query) {

	    query.setParameter("idTabelaPreco", filter.getIdTabelaPreco());

		if (filter.getNome() != null) {
		    query.setParameter("nome", "%" + filter.getNome().toLowerCase() + "%");
		}
		
		if (filter.getReferencia() != null) {
		    query.setParameter("referencia", "%" + filter.getReferencia().toLowerCase() + "%");
		}

		if (filter.getIdTipoProduto() != null) {
		    query.setParameter("idTipoProduto", filter.getIdTipoProduto());
		}

		if (filter.getIdFornecedor() != null) {
		    query.setParameter("idFornecedor", filter.getIdFornecedor());
		}

		if (filter.getMinPeso() != null) {
		    query.setParameter("minPeso", filter.getMinPeso());
		}

		if (filter.getMaxPeso() != null) {
		    query.setParameter("maxPeso", filter.getMaxPeso());
		}

		if (filter.getMinPreco() != null) {
		    query.setParameter("minPreco", filter.getMinPreco());
		}

		if (filter.getMaxPreco() != null) {
		    query.setParameter("maxPreco", filter.getMaxPreco());
		}
	}

	@Override
	public Page<PriceTableProductDto> getProductsByTable(PriceTableProductFilterDto filter, Pageable pageable) {
		String baseQuery = this.getQuery(filter);

		String orderBy = pageable.getSort().stream().map(order -> SORT_MAPPING.get(order.getProperty()) + " " + order.getDirection()).collect(Collectors.joining(", "));

	    if (!orderBy.isEmpty()) {
	        baseQuery += "\nORDER BY " + orderBy;
	    }

	    Query query = this.entityManager.createNativeQuery(baseQuery);

	    this.fillParameters(filter, query);

		query.setFirstResult((int) pageable.getOffset());
	    query.setMaxResults(pageable.getPageSize());

	    @SuppressWarnings("unchecked")
		List<Object[]> content = query.getResultList();

	    Query countQuery = entityManager.createNativeQuery("SELECT count(*) FROM produto p WHERE p.ativo = 1");
	    long total = ((Number) countQuery.getSingleResult()).longValue();
		
	    List<PriceTableProductDto> result = content.stream().map(linha -> {
            	ProductDto produto = new ProductDto();
            	ProductTypeDto tipoProduto = new ProductTypeDto();
            	ProviderDto fornecedor = new ProviderDto();
            	PriceTableProductDto preco = new PriceTableProductDto();

            	produto.setTipoProduto(tipoProduto);
            	produto.setFornecedor(fornecedor);
            	produto.setId((Long) linha[0]);
            	tipoProduto.setNome((String) linha[1]);
            	fornecedor.setFantasia((String) linha[2]);
            	produto.setReferencia((String) linha[3]);
            	produto.setNome((String) linha[4]);
            	produto.setPeso((Integer) linha[5]);

            	preco.setProduto(produto);
            	preco.setId((Long) linha[6]);
            	if (linha[7] != null) {
	            	preco.setPreco(BigDecimal.valueOf(((Number) linha[7]).doubleValue()));
            	}

                return preco;
            }).toList();

	    return new PageImpl<>(result, pageable, total);
	}

	@Override
	public void savePrices(List<PriceTableProductDto> prices, Usuario usuario) {
		prices.forEach(dto -> {
			if (dto.getPreco() != null) {
				TabelaPrecoProduto entity;
				Date agora = DateUtil.now();

				if (dto.getId() != null) {
					entity = this.repository.findById(dto.getId()).orElseThrow();
				} else {
					entity = new TabelaPrecoProduto();

					entity.setDataCriacao(agora);
				}

				TabelaPreco tabela = this.tabelaPrecoRepository.findById(dto.getTabela().getId()).orElseThrow();
				Produto produto = this.produtoRepository.findById(dto.getProduto().getId()).orElseThrow();

				entity.setTabela(tabela);
				entity.setProduto(produto);
				entity.setUsuario(usuario);
				entity.setDataAlteracao(agora);
				entity.setPreco(dto.getPreco());

				this.repository.save(entity);
			} else {
				if (dto.getId() != null) {
					this.repository.deleteById(dto.getId());
				}
			}
		});
	}
}
