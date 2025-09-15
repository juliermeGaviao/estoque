package br.com.dinamica.estoque.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import br.com.dinamica.estoque.entity.ClientePessoa;

public interface ContatoClientePessoaRepository extends JpaRepository<ClientePessoa, Long>, JpaSpecificationExecutor<ClientePessoa> {

}
