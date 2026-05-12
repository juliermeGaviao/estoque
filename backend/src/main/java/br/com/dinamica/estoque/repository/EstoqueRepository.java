package br.com.dinamica.estoque.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.dinamica.estoque.entity.Estoque;

public interface EstoqueRepository extends JpaRepository<Estoque, Long>, JpaSpecificationExecutor<Estoque> {

	@Query("FROM Estoque e WHERE e.produto.id = :idProduto AND e.id = (SELECT MAX(s.id) FROM Estoque s WHERE s.produto.id = :idProduto)")
	Estoque getInventoryByProduct(@Param("idProduto") Long idProduto);

}
