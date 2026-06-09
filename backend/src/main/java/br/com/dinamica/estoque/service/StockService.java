package br.com.dinamica.estoque.service;

import java.util.List;

import br.com.dinamica.estoque.dto.StockDto;
import br.com.dinamica.estoque.entity.Estoque;
import br.com.dinamica.estoque.entity.Usuario;

public interface StockService {

	Estoque saleStock(Long idProduto, Long idPontoVenda, Long idVenda, Integer amount, Usuario usuario);

	Estoque addStock(Long idProduto, Long idPontoVenda, Long idPedidoCompra, Integer amount, Usuario usuario);

	Integer getStock(Long idProduto);

	Integer getStockSalePoint(Long idProduto, Long idPontoVenda);

	Integer getStockPurchaseOrder(Long idProduto, Long idPedidoCompra);

	List<StockDto> getPurchaseOrderProducts(Long idPedidoCompra);

	List<StockDto> getStockBySalePoint(Long idPontoVenda);

	List<StockDto> getStockTransferProducts(Long idTransferenciaEstoque);

	void transferStock(Long idProduto, Long idPontoVendaOrigem, Long idPontoVendaDestino, Long idTransferenciaEstoque, Integer amount, Usuario usuario);

	List<StockDto> getStockByProductAndSalePoint(Long idProduto);

	void deleteBySale(Long idVenda);

}
