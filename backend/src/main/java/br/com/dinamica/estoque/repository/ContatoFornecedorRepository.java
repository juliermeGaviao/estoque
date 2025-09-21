package br.com.dinamica.estoque.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.transaction.annotation.Transactional;

import br.com.dinamica.estoque.entity.ContatoFornecedor;

public interface ContatoFornecedorRepository extends JpaRepository<ContatoFornecedor, Long>, JpaSpecificationExecutor<ContatoFornecedor> {

	@Transactional
    Long deleteByFornecedor_Id(Long fornecedorId);

}
