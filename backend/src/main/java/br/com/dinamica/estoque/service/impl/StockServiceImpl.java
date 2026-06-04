package br.com.dinamica.estoque.service.impl;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.stereotype.Service;

import br.com.dinamica.estoque.dto.StockDto;
import br.com.dinamica.estoque.entity.Estoque;
import br.com.dinamica.estoque.entity.TipoOperacao;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.repository.EstoqueRepository;
import br.com.dinamica.estoque.repository.PedidoCompraRepository;
import br.com.dinamica.estoque.repository.PontoVendaRepository;
import br.com.dinamica.estoque.repository.ProdutoRepository;
import br.com.dinamica.estoque.service.StockService;
import br.com.dinamica.estoque.util.DateUtil;

@Service
public class StockServiceImpl implements StockService {

	private EstoqueRepository repository;

	private ProdutoRepository produtoRepository;

	private PontoVendaRepository pontoVendaRepository;

	private PedidoCompraRepository pedidoCompraRepository;

	private ModelMapper modelMapper;

	public StockServiceImpl(EstoqueRepository repository, ProdutoRepository produtoRepository, PontoVendaRepository pontoVendaRepository, PedidoCompraRepository pedidoCompraRepository, ModelMapper modelMapper) {
		this.repository = repository;
		this.produtoRepository = produtoRepository;
		this.pontoVendaRepository = pontoVendaRepository;
		this.pedidoCompraRepository = pedidoCompraRepository;
		this.modelMapper = modelMapper;

		this.modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
	}

	@Override
	public Estoque addStock(Long idProduto, Long idPontoVenda, Integer amount, Usuario usuario) {
		if (amount == null || amount.compareTo(0) == 0) {
			return null;
		}

		return this.repository.save(this.getNewStock(idProduto, idPontoVenda, amount, usuario));
	}

	@Override
	public Estoque addStock(Long idProduto, Long idPontoVenda, Long idPedidoCompra, Integer amount, Usuario usuario) {
		if (amount == null || amount.compareTo(0) == 0) {
			return null;
		}

		Estoque result = this.getNewStock(idProduto, idPontoVenda, amount, usuario);

		result.setPedidoCompra(this.pedidoCompraRepository.findById(idPedidoCompra).orElseThrow());

		return this.repository.save(result);
	}

	@Override
	public Integer getStock(Long idProduto) {
		Integer result = this.repository.getStockByProduct(idProduto);

		return result != null ? result : 0;
	}

	@Override
	public Integer getStockSalePoint(Long idProduto, Long idPontoVenda) {
		Integer result = this.repository.getStockByProductAndSalePoint(idProduto, idPontoVenda);

		return result != null ? result : 0;
	}

	@Override
	public Integer getStockPurchaseOrder(Long idProduto, Long idPedidoCompra) {
		Integer result = this.repository.getStockByProductAndPurchaseOrder(idProduto, idPedidoCompra);

		return result != null ? result : 0;
	}

	@Override
	public List<StockDto> getPurchaseOrderProducts(Long idPedidoCompra) {
		return this.repository.getStockByPurchaseOrder(idPedidoCompra).stream().map(entity -> this.modelMapper.map(entity, StockDto.class)).toList();
	}

	private Estoque getNewStock(Long idProduto, Long idPontoVenda, Integer amount, Usuario usuario) {
		Integer current = this.repository.getStockByProductAndSalePoint(idProduto, idPontoVenda);
		Estoque result = new Estoque();
		
		result.setProduto(this.produtoRepository.findById(idProduto).orElseThrow());
		result.setPontoVenda(this.pontoVendaRepository.findById(idPontoVenda).orElseThrow());
		result.setQuantidade(Math.abs(amount));
		result.setTipoOperacao(amount.compareTo(0) > 0 ? TipoOperacao.C : TipoOperacao.D);
		result.setSaldo((current != null ? current : 0) + amount);
		result.setUsuario(usuario);
		result.setDataCriacao(DateUtil.now());

		return result;
	}

}
