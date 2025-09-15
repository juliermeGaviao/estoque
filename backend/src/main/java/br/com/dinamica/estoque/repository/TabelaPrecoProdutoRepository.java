package br.com.dinamica.estoque.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import br.com.dinamica.estoque.entity.TabelaPrecoProduto;

public interface TabelaPrecoProdutoRepository extends JpaRepository<TabelaPrecoProduto, Long>, JpaSpecificationExecutor<TabelaPrecoProduto> {

}
