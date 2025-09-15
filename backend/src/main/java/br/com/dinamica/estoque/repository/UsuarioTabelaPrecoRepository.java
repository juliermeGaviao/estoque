package br.com.dinamica.estoque.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import br.com.dinamica.estoque.entity.UsuarioTabelaPreco;

public interface UsuarioTabelaPrecoRepository extends JpaRepository<UsuarioTabelaPreco, Long>, JpaSpecificationExecutor<UsuarioTabelaPreco> {

}
