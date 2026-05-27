package br.com.dinamica.estoque.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import br.com.dinamica.estoque.entity.Produto;
import br.com.dinamica.estoque.entity.UsuarioPontoVenda;

public interface UsuarioPontoVendaRepository extends JpaRepository<UsuarioPontoVenda, Long>, JpaSpecificationExecutor<Produto> {

}
