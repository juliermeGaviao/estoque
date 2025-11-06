package br.com.dinamica.estoque.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import br.com.dinamica.estoque.entity.ArquivoClientePessoa;

public interface ArquivoClientePessoaRepository extends JpaRepository<ArquivoClientePessoa, Long>, JpaSpecificationExecutor<ArquivoClientePessoa> {

}
