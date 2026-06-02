package br.com.dinamica.estoque.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.dinamica.estoque.entity.Estoque;

public interface EstoqueRepository extends JpaRepository<Estoque, Long>, JpaSpecificationExecutor<Estoque> {

	@Query("SELECT SUM(e.saldo) FROM Estoque e WHERE e.id IN (SELECT MAX(s.id) FROM Estoque s WHERE s.produto.id = :idProduto GROUP BY s.pontoVenda.id)")
	Integer getStockByProduct(@Param("idProduto") Long idProduto);

	@Query("FROM Estoque e WHERE e.id = (SELECT MAX(s.id) FROM Estoque s WHERE s.produto.id = :idProduto AND s.pontoVenda.id = :idPontoVenda)")
	Estoque getStockByProductAndSalePoint(@Param("idProduto") Long idProduto, @Param("idPontoVenda") Long idPontoVenda);

}
