package br.com.dinamica.estoque.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import br.com.dinamica.estoque.dto.StockDto;
import br.com.dinamica.estoque.entity.Estoque;
import br.com.dinamica.estoque.entity.TipoOperacao;
import br.com.dinamica.estoque.entity.TransferenciaEstoque;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.mapper.StockMapper;
import br.com.dinamica.estoque.repository.EstoqueRepository;
import br.com.dinamica.estoque.repository.PedidoCompraRepository;
import br.com.dinamica.estoque.repository.PontoVendaRepository;
import br.com.dinamica.estoque.repository.ProdutoRepository;
import br.com.dinamica.estoque.repository.TransferenciaEstoqueRepository;
import br.com.dinamica.estoque.service.StockService;
import br.com.dinamica.estoque.util.DateUtil;

@Service
public class StockServiceImpl implements StockService {

	private EstoqueRepository repository;

	private ProdutoRepository produtoRepository;

	private PontoVendaRepository pontoVendaRepository;

	private PedidoCompraRepository pedidoCompraRepository;

	private TransferenciaEstoqueRepository transferenciaEstoqueRepository;

	private StockMapper modelMapper;

	public StockServiceImpl(EstoqueRepository repository, ProdutoRepository produtoRepository, PontoVendaRepository pontoVendaRepository, PedidoCompraRepository pedidoCompraRepository, TransferenciaEstoqueRepository transferenciaEstoqueRepository, StockMapper modelMapper) {
		this.repository = repository;
		this.produtoRepository = produtoRepository;
		this.pontoVendaRepository = pontoVendaRepository;
		this.pedidoCompraRepository = pedidoCompraRepository;
		this.transferenciaEstoqueRepository = transferenciaEstoqueRepository;
		this.modelMapper = modelMapper;
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
		return this.repository.getStockByPurchaseOrder(idPedidoCompra).stream().map(this.modelMapper::toDto).toList();
	}

	@Override
	public List<StockDto> getStockBySalePoint(Long idPontoVenda) {
		return this.repository.getStockBySalePoint(idPontoVenda).stream().map(this.modelMapper::toDto).toList();
	}

	@Override
	public List<StockDto> getStockTransferProducts(Long idTransferenciaEstoque) {
		return this.repository.getStockByStockTransfer(idTransferenciaEstoque).stream().map(this.modelMapper::toDto).toList();
	}

	@Override
	public void transferStock(Long idProduto, Long idPontoVendaOrigem, Long idPontoVendaDestino, Long idTransferenciaEstoque, Integer amount, Usuario usuario) {
		Estoque origem = this.getNewStock(idProduto, idPontoVendaOrigem, -amount, usuario);
		Estoque destino = this.getNewStock(idProduto, idPontoVendaDestino, amount, usuario);
		TransferenciaEstoque transferencia = this.transferenciaEstoqueRepository.findById(idTransferenciaEstoque).orElseThrow();

		origem.setTransferenciaEstoque(transferencia);
		destino.setTransferenciaEstoque(transferencia);

		this.repository.save(origem);
		this.repository.save(destino);
	}

	@Override
	public List<StockDto> getStockByProductAndSalePoint(Long idProduto) {
		return this.repository.getStockByProductAndSalePoint(idProduto).stream().map(this.modelMapper::toDto).toList();
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
