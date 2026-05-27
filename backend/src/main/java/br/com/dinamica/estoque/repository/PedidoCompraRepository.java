package br.com.dinamica.estoque.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import br.com.dinamica.estoque.entity.PedidoCompra;

public interface PedidoCompraRepository extends JpaRepository<PedidoCompra, Long>, JpaSpecificationExecutor<PedidoCompra> {

}
