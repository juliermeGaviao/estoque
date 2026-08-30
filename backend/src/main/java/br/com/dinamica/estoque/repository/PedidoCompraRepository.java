package br.com.dinamica.estoque.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.dinamica.estoque.entity.PedidoCompra;

public interface PedidoCompraRepository extends JpaRepository<PedidoCompra, Long>, JpaSpecificationExecutor<PedidoCompra> {

	@Query("FROM PedidoCompra pc WHERE pc.numeroPedido = :numeroPedido")
	List<PedidoCompra> findByOrderNumber(@Param("numeroPedido") String numeroPedido);

}
