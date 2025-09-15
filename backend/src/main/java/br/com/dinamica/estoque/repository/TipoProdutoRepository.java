package br.com.dinamica.estoque.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import br.com.dinamica.estoque.entity.TipoProduto;

public interface TipoProdutoRepository extends JpaRepository<TipoProduto, Long>, JpaSpecificationExecutor<TipoProduto> {

}
