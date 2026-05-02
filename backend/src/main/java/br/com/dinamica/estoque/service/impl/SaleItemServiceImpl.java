package br.com.dinamica.estoque.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.dinamica.estoque.dto.PriceTableProductDto;
import br.com.dinamica.estoque.dto.ProductDto;
import br.com.dinamica.estoque.dto.SaleDto;
import br.com.dinamica.estoque.dto.SaleItemDto;
import br.com.dinamica.estoque.entity.ItemVenda;
import br.com.dinamica.estoque.entity.TabelaPrecoProduto;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.entity.Venda;
import br.com.dinamica.estoque.repository.ItemVendaRepository;
import br.com.dinamica.estoque.repository.TabelaPrecoProdutoRepository;
import br.com.dinamica.estoque.repository.VendaRepository;
import br.com.dinamica.estoque.service.SaleItemService;
import br.com.dinamica.estoque.util.DateUtil;

@Service
public class SaleItemServiceImpl implements SaleItemService {
	
	private ItemVendaRepository repository;

	private VendaRepository vendaRepository;

	private TabelaPrecoProdutoRepository tabelaPrecoProdutoRepository;

	private ModelMapper modelMapper;

	public SaleItemServiceImpl(ItemVendaRepository repository, VendaRepository vendaRepository, TabelaPrecoProdutoRepository tabelaPrecoProdutoRepository, ModelMapper modelMapper) {
		this.repository = repository;
		this.vendaRepository = vendaRepository;
		this.tabelaPrecoProdutoRepository = tabelaPrecoProdutoRepository;
		this.modelMapper = modelMapper;

		this.modelMapper.addMappings(new PropertyMap<SaleItemDto, ItemVenda>() {
            @Override
            protected void configure() {
                skip(destination.getVenda());
                skip(destination.getTabelaPrecoProduto());
            }
        });
	}

	@Override
	public SaleItemDto get(Long id) {
		ItemVenda entity = this.repository.findById(id).orElseThrow();
		SaleItemDto result = this.modelMapper.map(entity, SaleItemDto.class);

		result.getVenda().getVendedor().setPerfis(null);
		result.getTabelaPrecoProduto().getProduto().setTipoProduto(null);
		result.getTabelaPrecoProduto().getProduto().setFornecedor(null);

		return result;
	}

	private static final String DISCOUNT_FIELD = "desconto";

	@Override
	public Page<SaleItemDto> list(Long idVenda, Long idTabelaPrecoProduto, Integer minQuantidade, Integer maxQuantidade, Pageable pageable) {
        Specification<ItemVenda> specification = (root, query, cb) -> null;

        if (idVenda != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("venda").get("id"), idVenda));
        }

        if (idTabelaPrecoProduto != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("tabelaPrecoProduto").get("id"), idTabelaPrecoProduto));
        }

        if (minQuantidade != null && maxQuantidade != null) {
            specification = specification.and((root, query, cb) -> cb.between(root.get(DISCOUNT_FIELD), minQuantidade, maxQuantidade));
        } else if (minQuantidade != null) {
            specification = specification.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get(DISCOUNT_FIELD), minQuantidade));
        } else if (maxQuantidade != null) {
            specification = specification.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get(DISCOUNT_FIELD), maxQuantidade));
        }

		return this.repository.findAll(specification, pageable).map(entity -> {
			SaleItemDto result = this.modelMapper.map(entity, SaleItemDto.class);

			result.getVenda().getVendedor().setPerfis(null);
			result.getTabelaPrecoProduto().getProduto().setTipoProduto(null);
			result.getTabelaPrecoProduto().getProduto().setFornecedor(null);

			return result;
		});
	}

	@Override
	public SaleItemDto save(SaleItemDto dto, Usuario usuario) {
		ItemVenda entity;
        Date agora = DateUtil.now();

		if (dto.getId() != null) {
			entity = this.repository.findById(dto.getId()).orElseThrow();
		} else {
			entity = new ItemVenda();

			entity.setDataCriacao(agora);
		}

		this.modelMapper.map(dto, entity);

		Venda venda = this.vendaRepository.findById(dto.getVenda().getId()).orElseThrow();
		TabelaPrecoProduto tabelaPrecoProduto = this.tabelaPrecoProdutoRepository.findById(dto.getTabelaPrecoProduto().getId()).orElseThrow();

		entity.setVenda(venda);
		entity.setTabelaPrecoProduto(tabelaPrecoProduto);
		entity.setUsuario(usuario);
		entity.setDataAlteracao(agora);

		entity = this.repository.save(entity);

		return this.modelMapper.map(entity, SaleItemDto.class);
	}

	@Override
	@Transactional
	public List<SaleItemDto> save(List<SaleItemDto> list, Usuario usuario) {
		List<Long> ids = new ArrayList<>();

		List<SaleItemDto> result = list.stream().map(dto -> {
			SaleItemDto entity = this.save(dto, usuario);

			ids.add(entity.getId());

			return entity;
		}).toList();

		this.repository.deleteByVendaIdAndNotInIds(list.getFirst().getVenda().getId(), ids);

		return result;
	}

	@Override
	public void delete(Long id) {
		this.repository.deleteById(id);
	}

	@Override
	public List<SaleItemDto> getItensByPriceTable(Long idTabelaPreco) {
		List<Object[]> result = this.repository.getItensByPriceTable(idTabelaPreco);

		return result.stream().map(linha -> {
			SaleItemDto dto = new SaleItemDto();
			PriceTableProductDto priceTableProductDto = new PriceTableProductDto();
			ProductDto productDto = new ProductDto();

			dto.setTabelaPrecoProduto(priceTableProductDto);
			priceTableProductDto.setProduto(productDto);

			productDto.setId((Long) linha[0]);
			priceTableProductDto.setId((Long) linha[1]);
			productDto.setNome((String) linha[2]);
			productDto.setReferencia((String) linha[3]);
			priceTableProductDto.setPreco(BigDecimal.valueOf(((Number) linha[4]).doubleValue()));
			dto.setPrecoUnitario(BigDecimal.valueOf(((Number) linha[4]).doubleValue()));

			return dto;
		}).toList();
	}

	@Override
	public List<SaleItemDto> getItensBySale(Long idVenda) {
		List<Object[]> result = this.repository.getItensBySale(idVenda);

		return result.stream().map(linha -> {
			SaleItemDto dto = new SaleItemDto();
			SaleDto saleDto = new SaleDto();
			PriceTableProductDto priceTableProductDto = new PriceTableProductDto();
			ProductDto productDto = new ProductDto();

			dto.setTabelaPrecoProduto(priceTableProductDto);
			dto.setVenda(saleDto);
			priceTableProductDto.setProduto(productDto);

			dto.setId((Long) linha[0]);
			saleDto.setId((Long) linha[1]);
			productDto.setId((Long) linha[2]);
			priceTableProductDto.setId((Long) linha[3]);
			productDto.setNome((String) linha[4]);
			productDto.setReferencia((String) linha[5]);
			dto.setQuantidade((Integer) linha[6]);
			priceTableProductDto.setPreco(BigDecimal.valueOf(((Number) linha[7]).doubleValue()));
			dto.setPrecoUnitario(BigDecimal.valueOf(((Number) linha[7]).doubleValue()));
			dto.setTotal(linha[8] != null ? BigDecimal.valueOf(((Number) linha[8]).doubleValue()) : null);

			return dto;
		}).toList();
	}

}
