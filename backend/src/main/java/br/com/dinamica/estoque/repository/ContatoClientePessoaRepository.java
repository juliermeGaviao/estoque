package br.com.dinamica.estoque.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.transaction.annotation.Transactional;

import br.com.dinamica.estoque.entity.ContatoClientePessoa;

public interface ContatoClientePessoaRepository extends JpaRepository<ContatoClientePessoa, Long>, JpaSpecificationExecutor<ContatoClientePessoa> {

	@Transactional
    Long deleteByClientePessoa_Id(Long clientePessoaId);

}
