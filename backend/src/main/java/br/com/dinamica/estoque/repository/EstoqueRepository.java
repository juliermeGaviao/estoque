package br.com.dinamica.estoque.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.dinamica.estoque.entity.Estoque;

public interface EstoqueRepository extends JpaRepository<Estoque, Long>, JpaSpecificationExecutor<Estoque> {

	@Query("SELECT SUM(e.saldo) FROM Estoque e WHERE e.id IN (SELECT MAX(s.id) FROM Estoque s WHERE s.produto.id = :idProduto GROUP BY s.pontoVenda.id)")
	Integer getStockByProduct(@Param("idProduto") Long idProduto);

	@Query("SELECT e.saldo FROM Estoque e WHERE e.id = (SELECT MAX(s.id) FROM Estoque s WHERE s.produto.id = :idProduto AND s.pontoVenda.id = :idPontoVenda)")
	Integer getStockByProductAndSalePoint(@Param("idProduto") Long idProduto, @Param("idPontoVenda") Long idPontoVenda);

	@Query("SELECT e.saldo FROM Estoque e WHERE e.id = (SELECT MAX(s.id) FROM Estoque s WHERE s.produto.id = :idProduto AND s.pedidoCompra.id = :idPedidoCompra)")
	Integer getStockByProductAndPurchaseOrder(@Param("idProduto") Long idProduto, @Param("idPedidoCompra") Long idPedidoCompra);

	@Query("FROM Estoque e WHERE e.pedidoCompra.id = :idPedidoCompra ORDER BY e.produto.nome")
	List<Estoque> getStockByPurchaseOrder(@Param("idPedidoCompra") Long idPedidoCompra);

	@Query("FROM Estoque e WHERE e.id IN (SELECT MAX(s.id) FROM Estoque s WHERE s.pontoVenda.id = :idPontoVenda GROUP BY s.produto.id) AND e.saldo > 0 ORDER BY e.produto.nome")
	List<Estoque> getStockBySalePoint(@Param("idPontoVenda") Long idPontoVenda);

	@Query("FROM Estoque e WHERE e.transferenciaEstoque.id = :idTransferenciaEstoque AND e.tipoOperacao = 'C' ORDER BY e.produto.nome")
	List<Estoque> getStockByStockTransfer(@Param("idTransferenciaEstoque") Long idTransferenciaEstoque);

}
