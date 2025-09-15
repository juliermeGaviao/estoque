package br.com.dinamica.estoque.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import br.com.dinamica.estoque.entity.TabelaPreco;

public interface TabelaPrecoRepository extends JpaRepository<TabelaPreco, Long>, JpaSpecificationExecutor<TabelaPreco> {

}
