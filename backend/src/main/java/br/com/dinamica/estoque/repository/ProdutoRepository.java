package br.com.dinamica.estoque.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.transaction.annotation.Transactional;

import br.com.dinamica.estoque.entity.Produto;

public interface ProdutoRepository extends JpaRepository<Produto, Long>, JpaSpecificationExecutor<Produto> {

	@Transactional
    Long deleteByTipoProduto_Id(Long tipoProdutoId);

}
