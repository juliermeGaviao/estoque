package br.com.dinamica.estoque.service.impl;

import org.springframework.stereotype.Service;

import br.com.dinamica.estoque.entity.Estoque;
import br.com.dinamica.estoque.entity.TipoOperacao;
import br.com.dinamica.estoque.entity.Usuario;
import br.com.dinamica.estoque.repository.EstoqueRepository;
import br.com.dinamica.estoque.repository.PontoVendaRepository;
import br.com.dinamica.estoque.repository.ProdutoRepository;
import br.com.dinamica.estoque.service.StockService;
import br.com.dinamica.estoque.util.DateUtil;

@Service
public class StockServiceImpl implements StockService {

	private EstoqueRepository repository;

	private ProdutoRepository produtoRepository;

	private PontoVendaRepository pontoVendaRepository;

	public StockServiceImpl(EstoqueRepository repository, ProdutoRepository produtoRepository, PontoVendaRepository pontoVendaRepository) {
		this.repository = repository;
		this.produtoRepository = produtoRepository;
		this.pontoVendaRepository = pontoVendaRepository;
	}

	@Override
	public Estoque addStock(Long idProduto, Long idPontoVenda, Integer amount, Usuario usuario) {
		if (amount == null || amount.compareTo(0) == 0) {
			return null;
		}

		Estoque current = this.repository.getStockByProductAndSalePoint(idProduto, idPontoVenda);
		Estoque stock = new Estoque();
		
		stock.setProduto(this.produtoRepository.findById(idProduto).orElseThrow());
		stock.setPontoVenda(this.pontoVendaRepository.findById(idPontoVenda).orElseThrow());
		stock.setQuantidade(Math.abs(amount));
		stock.setTipoOperacao(amount.compareTo(0) > 0 ? TipoOperacao.C : TipoOperacao.D);
		stock.setSaldo((current != null && current.getSaldo() != null ? current.getSaldo() : 0) + amount);
		stock.setUsuario(usuario);
		stock.setDataCriacao(DateUtil.now());

		return this.repository.save(stock);
	}

	@Override
	public Integer getStock(Long idProduto) {
		Integer result = this.repository.getStockByProduct(idProduto);

		return result != null ? result : 0;
	}
}
