package br.com.dinamica.estoque.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.dinamica.estoque.dto.PriceTableProductDto;
import br.com.dinamica.estoque.dto.ProductDto;
import br.com.dinamica.estoque.dto.ProductTypeDto;
import br.com.dinamica.estoque.dto.ProviderDto;
import br.com.dinamica.estoque.dto.SaleDto;
import br.com.dinamica.estoque.dto.SaleItemDto;
import br.com.dinamica.estoque.entity.ItemVenda;
import br.com.dinamica.estoque.entity.TabelaPrecoProduto;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.entity.Venda;
import br.com.dinamica.estoque.mapper.SaleItemMapper;
import br.com.dinamica.estoque.repository.ItemVendaRepository;
import br.com.dinamica.estoque.repository.TabelaPrecoProdutoRepository;
import br.com.dinamica.estoque.repository.VendaRepository;
import br.com.dinamica.estoque.service.SaleItemService;
import br.com.dinamica.estoque.service.StockService;
import br.com.dinamica.estoque.util.DateUtil;

@Service
public class SaleItemServiceImpl implements SaleItemService {

	private ItemVendaRepository repository;

	private VendaRepository vendaRepository;

	private TabelaPrecoProdutoRepository tabelaPrecoProdutoRepository;

	private StockService stockService;

	private SaleItemMapper modelMapper;

	public SaleItemServiceImpl(ItemVendaRepository repository, VendaRepository vendaRepository, TabelaPrecoProdutoRepository tabelaPrecoProdutoRepository, StockService stockService, SaleItemMapper modelMapper) {
		this.repository = repository;
		this.vendaRepository = vendaRepository;
		this.tabelaPrecoProdutoRepository = tabelaPrecoProdutoRepository;
		this.stockService = stockService;
		this.modelMapper = modelMapper;
	}

	@Override
	public SaleItemDto get(Long id) {
		ItemVenda entity = this.repository.findById(id).orElseThrow();
		SaleItemDto result = this.modelMapper.toDto(entity);

		result.getVenda().getVendedor().setPerfis(null);
		result.getTabelaPrecoProduto().getProduto().setTipoProduto(null);
		result.getTabelaPrecoProduto().getProduto().setFornecedor(null);

		return result;
	}

	private static final String DISCOUNT_FIELD = "desconto";

	@Override
	public Page<SaleItemDto> list(Long idVenda, Long idTabelaPrecoProduto, Integer minQuantidade, Integer maxQuantidade, Pageable pageable) {
        Specification<ItemVenda> specification = (_, _, _) -> null;

        if (idVenda != null) {
            specification = specification.and((root, _, cb) -> cb.equal(root.get("venda").get("id"), idVenda));
        }

        if (idTabelaPrecoProduto != null) {
            specification = specification.and((root, _, cb) -> cb.equal(root.get("tabelaPrecoProduto").get("id"), idTabelaPrecoProduto));
        }

        if (minQuantidade != null && maxQuantidade != null) {
            specification = specification.and((root, _, cb) -> cb.between(root.get(DISCOUNT_FIELD), minQuantidade, maxQuantidade));
        } else if (minQuantidade != null) {
            specification = specification.and((root, _, cb) -> cb.greaterThanOrEqualTo(root.get(DISCOUNT_FIELD), minQuantidade));
        } else if (maxQuantidade != null) {
            specification = specification.and((root, _, cb) -> cb.lessThanOrEqualTo(root.get(DISCOUNT_FIELD), maxQuantidade));
        }

		return this.repository.findAll(specification, pageable).map(entity -> {
			SaleItemDto result = this.modelMapper.toDto(entity);

			result.getVenda().getVendedor().setPerfis(null);
			result.getTabelaPrecoProduto().getProduto().setTipoProduto(null);
			result.getTabelaPrecoProduto().getProduto().setFornecedor(null);

			return result;
		});
	}

	@Override
	public SaleItemDto save(SaleItemDto dto, Usuario usuario) {
		ItemVenda entity;
		LocalDateTime agora = DateUtil.now();

		if (dto.getId() != null) {
			entity = this.repository.findById(dto.getId()).orElseThrow();
		} else {
			entity = new ItemVenda();

			entity.setDataCriacao(agora);
		}

		this.modelMapper.updateEntityFromDto(dto, entity);

		Venda venda = this.vendaRepository.findById(dto.getVenda().getId()).orElseThrow();
		TabelaPrecoProduto tabelaPrecoProduto = this.tabelaPrecoProdutoRepository.findById(dto.getTabelaPrecoProduto().getId()).orElseThrow();

		entity.setVenda(venda);
		entity.setTabelaPrecoProduto(tabelaPrecoProduto);
		entity.setUsuario(usuario);
		entity.setDataAlteracao(agora);

		entity = this.repository.save(entity);

		return this.modelMapper.toDto(entity);
	}

	@Override
	@Transactional
	public List<SaleItemDto> save(List<SaleItemDto> list, Usuario usuario) {
		List<Long> ids = new ArrayList<>();

		List<SaleItemDto> result = list.stream().map(dto -> {
			Long idProduto = dto.getTabelaPrecoProduto().getProduto().getId();

			if (dto.getId() != null) {
				ItemVenda itemVenda = this.repository.findById(dto.getId()).orElseThrow();

				this.stockService.saleStock(idProduto, itemVenda.getVenda().getPontoVenda().getId(), itemVenda.getVenda().getId(), itemVenda.getQuantidade() - dto.getQuantidade(), usuario);
			} else {
				this.stockService.saleStock(idProduto, dto.getVenda().getPontoVenda().getId(), dto.getVenda().getId(), -dto.getQuantidade(), usuario);
			}

			SaleItemDto entity = this.save(dto, usuario);
			
			ids.add(entity.getId());

			return entity;
		}).toList();

		List<ItemVenda> toBeDeleted = this.repository.getItensByVendaIdAndNotInIds(list.getFirst().getVenda().getId(), ids);

		toBeDeleted.stream().forEach(item -> {
			this.stockService.saleStock(item.getTabelaPrecoProduto().getProduto().getId(), item.getVenda().getPontoVenda().getId(), item.getVenda().getId(), item.getQuantidade(), usuario);
			this.repository.delete(item);
		});

		return result;
	}

	@Override
	public void delete(Long id, Usuario usuario) {
		ItemVenda item = this.repository.findById(id).orElseThrow();

		this.stockService.saleStock(item.getTabelaPrecoProduto().getProduto().getId(), item.getVenda().getPontoVenda().getId(), item.getVenda().getId(), item.getQuantidade(), usuario);

		this.repository.deleteById(id);
	}

	@Override
	public List<SaleItemDto> getItensByPriceTableAndSalePoint(Long idTabelaPreco,  Long idPontoVenda) {
		List<Object[]> result = this.repository.getItensByPriceTable(idTabelaPreco, idPontoVenda);

		return result.stream().map(linha -> {
			SaleItemDto dto = new SaleItemDto();
			PriceTableProductDto priceTableProductDto = new PriceTableProductDto();
			ProductDto productDto = new ProductDto();

			dto.setTabelaPrecoProduto(priceTableProductDto);
			priceTableProductDto.setProduto(productDto);

			productDto.setId((Long) linha[0]);
			priceTableProductDto.setId((Long) linha[1]);
			productDto.setNome((String) linha[2]);
			productDto.setTipoProduto(new ProductTypeDto(null, (String) linha[3]));
			productDto.setFornecedor(new ProviderDto((String) linha[4]));
			productDto.setReferencia((String) linha[5]);
			priceTableProductDto.setPreco(BigDecimal.valueOf(((Number) linha[6]).doubleValue()));
			dto.setPrecoUnitario(BigDecimal.valueOf(((Number) linha[6]).doubleValue()));
			productDto.setEstoque((Integer) linha[7]);

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
			productDto.setTipoProduto(new ProductTypeDto(null, (String) linha[6]));
			productDto.setFornecedor(new ProviderDto((String) linha[7]));
			dto.setQuantidade((Integer) linha[8]);
			priceTableProductDto.setPreco(BigDecimal.valueOf(((Number) linha[9]).doubleValue()));
			dto.setPrecoUnitario(BigDecimal.valueOf(((Number) linha[9]).doubleValue()));
			dto.setTotal(linha[10] != null ? BigDecimal.valueOf(((Number) linha[10]).doubleValue()) : null);
			productDto.setEstoque((Integer) linha[11]);

			return dto;
		}).toList();
	}

}
