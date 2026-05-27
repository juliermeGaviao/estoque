package br.com.dinamica.estoque.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import br.com.dinamica.estoque.entity.TransferenciaEstoque;

public interface TransferenciaEstoqueRepository extends JpaRepository<TransferenciaEstoque, Long>, JpaSpecificationExecutor<TransferenciaEstoque> {

}
