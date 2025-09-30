package br.com.dinamica.estoque.service.impl;

import java.util.Date;

import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import br.com.dinamica.estoque.dto.PriceTableProductDto;
import br.com.dinamica.estoque.entity.Produto;
import br.com.dinamica.estoque.entity.TabelaPreco;
import br.com.dinamica.estoque.entity.TabelaPrecoProduto;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.repository.ProdutoRepository;
import br.com.dinamica.estoque.repository.TabelaPrecoProdutoRepository;
import br.com.dinamica.estoque.repository.TabelaPrecoRepository;
import br.com.dinamica.estoque.service.PriceTableProductService;
import br.com.dinamica.estoque.util.DateUtil;

@Service
public class PriceTableProductServiceImpl implements PriceTableProductService {

	private TabelaPrecoProdutoRepository repository;

	private TabelaPrecoRepository tabelaPrecoRepository;

	private ProdutoRepository produtoRepository;

	private ModelMapper modelMapper;

	public PriceTableProductServiceImpl(TabelaPrecoProdutoRepository repository, TabelaPrecoRepository tabelaPrecoRepository, ProdutoRepository produtoRepository, ModelMapper modelMapper) {
		this.repository = repository;
		this.tabelaPrecoRepository = tabelaPrecoRepository;
		this.produtoRepository = produtoRepository;
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

}
